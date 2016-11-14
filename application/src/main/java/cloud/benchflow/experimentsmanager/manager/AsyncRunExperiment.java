package cloud.benchflow.experimentsmanager.manager;

import cloud.benchflow.experimentsmanager.db.ExperimentsDAO;
import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.db.entities.Trial;
import cloud.benchflow.experimentsmanager.exceptions.ExperimentRunException;
import cloud.benchflow.experimentsmanager.resources.lifecycle.RunExperimentResource;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.exceptions.FabanClientException;
import cloud.benchflow.faban.client.responses.RunId;
import cloud.benchflow.minio.BenchFlowMinioClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         <p>
 *         Created on 07.11.16.
 */
public class AsyncRunExperiment implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RunExperimentResource.class.getName());

    private Experiment experiment;
    private ExperimentsDAO experimentsDAO;

    private BenchFlowMinioClient minio;
    private FabanClient faban;
    private DriversMaker driversMaker;
    private ExecutorService submitRunsPool;
    private Integer submitRetries;


    public AsyncRunExperiment(Experiment experiment, ExperimentsDAO experimentsDAO, BenchFlowMinioClient minio, FabanClient faban, DriversMaker driversMaker, ExecutorService submitRunsPool, Integer submitRetries) {
        this.experiment = experiment;
        this.experimentsDAO = experimentsDAO;
        this.minio = minio;
        this.faban = faban;
        this.driversMaker = driversMaker;
        this.submitRunsPool = submitRunsPool;
        this.submitRetries = submitRetries;
    }

    @Override
    public void run() {

        try {
            String testName = experiment.getExperimentName();
            String minioTestId = experiment.getMinioExperimentId();
            long experimentNumber = experiment.getExperimentNumber();
            int trials = experiment.getTrials().size();

            driversMaker.generateBenchmark(testName, experimentNumber, trials);
            logger.debug("Generated Faban benchmark");

            experiment.setQueued();

            InputStream fabanBenchmark = minio.getGeneratedBenchmark(minioTestId, experimentNumber);

            String fabanExperimentId = experiment.getFabanExperimentId();

            java.nio.file.Path benchmarkPath =
                    Paths.get("./tmp").resolve(experiment.getExperimentId())
//                                          .resolve("benchflow-benchmark.jar");
                            .resolve(fabanExperimentId + ".jar");

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
                            runId = faban.submit(fabanExperimentId, t.getTrialId().replace('.', '-'),
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
            while (received < trials) {
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

        } catch (Exception e) {
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
