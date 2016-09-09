package cloud.benchflow.experimentsmanager.resources.lifecycle;

import cloud.benchflow.experimentsmanager.db.ExperimentsDAO;
import cloud.benchflow.experimentsmanager.db.DbManager;
import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.db.entities.Trial;
import cloud.benchflow.experimentsmanager.exceptions.ExperimentRunException;
import cloud.benchflow.experimentsmanager.responses.lifecycle.ExperimentIdResponse;
import cloud.benchflow.experimentsmanager.utils.BenchFlowExperimentArchiveExtractor;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import cloud.benchflow.experimentsmanager.utils.ExperimentConfiguration;
import cloud.benchflow.minio.BenchFlowMinioClient;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.exceptions.FabanClientException;
import cloud.benchflow.faban.client.responses.RunId;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 05/03/16.
 */
@Path("run")
public class RunExperimentResource {

    private static Logger logger = LoggerFactory.getLogger(RunExperimentResource.class.getName());

    private BenchFlowMinioClient minio;
    private DbManager db;
    private FabanClient faban;
    private DriversMaker driversMaker;
    private ExecutorService runBenchmarkPool;
    private ExecutorService submitRunsPool;
    private Integer submitRetries;

    @Inject
    public RunExperimentResource(@Named("minio") BenchFlowMinioClient minio,
                                 @Named("db") DbManager db,
                                 @Named("faban") FabanClient faban,
                                 @Named("retries") Integer submitRetries,
                                 @Named("drivers-maker") DriversMaker driversMaker,
                                 @Named("runBenchmarkExecutorService") ExecutorService runExperimentsPool,
                                 @Named("submitRunExecutorService") ExecutorService submitRunsPool) {
        this.minio = minio;
        this.db = db;
        this.faban = faban;
        this.driversMaker = driversMaker;
        this.runBenchmarkPool = runExperimentsPool;
        this.submitRetries = submitRetries;
        this.submitRunsPool = submitRunsPool;
    }

    private void cleanUpMinio(Experiment e) {
        minio.removeTestConfigurationForExperiment(e.getTestId(), e.getExperimentNumber());
    }


    private class AsyncRunExperiment implements Runnable {

        private Experiment experiment;
        private ExperimentsDAO experimentsDAO;

        public AsyncRunExperiment(Experiment experiment,
                                  ExperimentsDAO experimentsDAO) {
            this.experiment = experiment;
            this.experimentsDAO = experimentsDAO;
        }

        @Override
        public void run() {

            try {
                String testName = experiment.getExperimentName();
                String testId = experiment.getTestId();
                //String minioTestId = experiment.getUsername() + "/" + testName;
                String minioTestId = (experiment.getUsername() + "." + testName).replace('.', '/');
                long experimentNumber = experiment.getExperimentNumber();

                driversMaker.generateBenchmark(testName, experiment.getExperimentNumber(), experiment.getTrials().size());
                logger.debug("Generated Faban benchmark");

                experiment.setQueued();

                InputStream fabanBenchmark = minio.getGeneratedBenchmark(minioTestId, experimentNumber);

                //because Faban doesn't accept deployment of benchmarks with dots in the name
                String compatibleBenchmarkName = experiment.getExperimentId().replace('.', '_');

                java.nio.file.Path benchmarkPath =
                        Paths.get("./tmp").resolve(experiment.getExperimentId())
//                                          .resolve("benchflow-benchmark.jar");
                                          .resolve(compatibleBenchmarkName + ".jar");

                FileUtils.copyInputStreamToFile(fabanBenchmark, benchmarkPath.toFile());

                //faban.deploy(fabanBenchmark, experiment.getExperimentId());
                System.out.println(faban.deploy(benchmarkPath.toFile()).getCode());

                logger.debug("Benchmark successfully deployed");

                FileUtils.forceDelete(benchmarkPath.toFile());

                //send the runs to faban
                CompletionService<Trial> cs = new ExecutorCompletionService<>(submitRunsPool);

                //make concurrent run requests to faban
                for (Trial t : experiment.getTrials()) {

                    cs.submit(() -> {
                        int retries = submitRetries;
                        String config = minio.getFabanConfiguration(minioTestId, experimentNumber, t.getTrialNumber());
                        java.nio.file.Path fabanConfigPath = Paths.get("./tmp")
                                .resolve(experiment.getExperimentId())
                                .resolve(String.valueOf(t.getTrialNumber()))
                                .resolve("run.xml");
                        FileUtils.writeStringToFile(fabanConfigPath.toFile(), config, Charset.forName("UTF-8"));

                        RunId runId = null;
                        while (runId == null) {
                            try {
//                                runId = new RunId(benchmarkName,"foo");
                                runId = faban.submit(compatibleBenchmarkName, t.getTrialId().replace('.', '-'),
                                                      fabanConfigPath.toFile());
//                                runId = faban.submit("benchflow-benchmark", "benchflow-benchmark",
//                                        fabanConfigPath.toFile());
                            } catch (FabanClientException e) {
                                if (retries > 0) retries--;
                                else {
                                    throw e;
                                }
                            }
                        }
                        t.setFabanRunId(runId.toString());
                        t.setSubmitted();
                        return t;
                    });
                }

                int received = 0;
                while (received < experiment.getTrials().size()) {
                    //TODO: handle the case in which some trials fails to submit?
                    Future<Trial> updatedTrialResponse = cs.take();
                    Trial updatedTrial = updatedTrialResponse.get();
                    updatedTrial.setSubmitted();
                    experimentsDAO.update(updatedTrial);
                    logger.debug("Received trial " + updatedTrial.getTrialNumber() +
                                 "with run ID: " + updatedTrial.getFabanRunId());
                    received++;
                }

                experiment.setRunning();

            } catch(Exception e) {
                experiment.setAborted();
                //TODO: set all trials as aborted, if any
                //check if any of them was queued on faban, to kill it
                experimentsDAO.update(experiment);
                logger.debug("Exception", e.getMessage());
                throw new ExperimentRunException(e.getMessage(), e);
            } finally {
                experimentsDAO.close();
            }
        }
    }


    //TODO: in the future, instead of the archive, this API
    //will receive only configuration and deployment descriptor for experiment
    //for now it receives the full archive and saves stuff on minio (to be moved to orchestrator)
    @POST
    @Path("{testName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public ExperimentIdResponse runAsync(@PathParam("testName") String testName,
                                         @FormDataParam("experiment") InputStream expArchive,
                                         @FormDataParam("experiment") FormDataContentDisposition expArchiveDisp)
    throws IOException {

        String user = "BenchFlow";
        String minioTestId = (user + "." + testName).replace('.', '/');

        ExperimentsDAO experimentsDAO = db.getExperimentsDAO();


        ExperimentConfiguration config = new BenchFlowExperimentArchiveExtractor(minio, minioTestId)
                .extract(expArchive);

        String expConfig = config.getExpConfig();
        String deploymentDescriptor = config.getDeploymentDescriptor();

        Map<String, Object> parsedExpConfig = (Map<String, Object>) new Yaml().load(expConfig);
        int trials = (Integer) parsedExpConfig.get("trials");

        Experiment experiment = new Experiment(user, testName);
        String testId = experiment.getTestId();

        logger.debug("Retrieved number of trials for experiment " + testId + ": " + trials);

        for (int i = 1; i <= trials; i++) {
            Trial trial = new Trial(i);
            experiment.addTrial(trial);
        }

        experimentsDAO.saveExperiment(experiment);
        logger.debug("Stored experiment id in database");

        try {
           minio.saveTestConfigurationForExperiment(minioTestId, experiment.getExperimentNumber(), expConfig);
           minio.saveDeploymentDescriptorForExperiment(minioTestId, experiment.getExperimentNumber(), deploymentDescriptor);
           runBenchmarkPool.submit(new AsyncRunExperiment(experiment, experimentsDAO));
        } catch(Exception e) {
            //leaves the database in a consistent state
            //and reports the exception so that we can investigate
            experimentsDAO.cleanUp(experiment);
            cleanUpMinio(experiment);
            throw new WebApplicationException(e.getMessage(), e);
        }

        return new ExperimentIdResponse(experiment.getExperimentId(), experiment.getTrials().size());
    }

}