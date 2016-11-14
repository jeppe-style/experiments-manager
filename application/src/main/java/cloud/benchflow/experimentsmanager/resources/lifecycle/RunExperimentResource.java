package cloud.benchflow.experimentsmanager.resources.lifecycle;

import cloud.benchflow.experimentsmanager.db.ExperimentsDAO;
import cloud.benchflow.experimentsmanager.db.DbManager;
import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.db.entities.Trial;
import cloud.benchflow.experimentsmanager.manager.AsyncRunExperiment;
import cloud.benchflow.experimentsmanager.responses.lifecycle.ExperimentIdResponse;
import cloud.benchflow.experimentsmanager.utils.BenchFlowExperimentArchiveExtractor;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import cloud.benchflow.experimentsmanager.utils.ExperimentConfiguration;
import cloud.benchflow.experimentsmanager.utils.exceptions.ArchiveExtractionException;
import cloud.benchflow.minio.BenchFlowMinioClient;
import cloud.benchflow.faban.client.FabanClient;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.ZipInputStream;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 05/03/16.
 */
@Path("run")
public class RunExperimentResource {

    private static Logger logger = LoggerFactory.getLogger(RunExperimentResource.class.getName());

    public static final String USER = "BenchFlow";

    private BenchFlowMinioClient minio;
    private DbManager db;
    private FabanClient faban;
    private DriversMaker driversMaker;
    private ExecutorService runExperimentsPool;
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
        this.runExperimentsPool = runExperimentsPool;
        this.submitRetries = submitRetries;
        this.submitRunsPool = submitRunsPool;
    }

    //TODO: in the future, instead of the archive, this API
    //will receive only configuration and deployment descriptor for experiment
    //for now it receives the full archive and saves stuff on minio (to be moved to orchestrator)
    @POST
    @Path("{experimentName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
//    public ExperimentIdResponse runAsync(@PathParam("experimentName") String experimentName,
//                                         @FormDataParam("experiment") InputStream expArchive,
//                                         @FormDataParam("experiment") FormDataContentDisposition expArchiveDisp)
    public ExperimentIdResponse runAsync(@PathParam("experimentName") String experimentName,
                                         @FormDataParam("experiment") InputStream expArchive)

    throws IOException {


        // check that the arguments are valid
        // TODO - we will move much of the code to the orchestrator, there we will add a validator
        if (experimentName == null || expArchive == null) {
            // TODO - check which http status to return
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Experiment experiment = new Experiment(USER, experimentName);

        ExperimentsDAO experimentsDAO = db.getExperimentsDAO();

        ZipInputStream zipInputStream = new ZipInputStream(expArchive);

        ExperimentConfiguration config = null;

        try {
            config = new BenchFlowExperimentArchiveExtractor(minio, experiment.getMinioExperimentId())
                    .extractExperimentArchive(zipInputStream);
        } catch (ArchiveExtractionException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        String expConfig = config.getExpConfig();
        String deploymentDescriptor = config.getDeploymentDescriptor();

        Map<String, Object> parsedExpConfig = (Map<String, Object>) new Yaml().load(expConfig);

        int trials = (Integer) parsedExpConfig.get("trials");

        String testId = experiment.getTestId();

        logger.debug("Retrieved number of trials for experiment " + testId + ": " + trials);

        for (int i = 1; i <= trials; i++) {
            Trial trial = new Trial(i);
            experiment.addTrial(trial);
        }

        experimentsDAO.saveExperiment(experiment);
        logger.debug("Stored experiment id in database");

        try {

           minio.saveTestConfigurationForExperiment(experiment.getMinioExperimentId(), experiment.getExperimentNumber(), expConfig);
           minio.saveDeploymentDescriptorForExperiment(experiment.getMinioExperimentId(), experiment.getExperimentNumber(), deploymentDescriptor);
           runExperimentsPool.submit(new AsyncRunExperiment(experiment, experimentsDAO, minio, faban, driversMaker, submitRunsPool, submitRetries));

        } catch(Exception e) {

            // TODO - make this exception more specific

            //leaves the database in a consistent state
            //and reports the exception so that we can investigate
            experimentsDAO.cleanUp(experiment);
            cleanUpMinio(experiment);
            throw new WebApplicationException(e.getMessage(), e);
        }

        return new ExperimentIdResponse(experiment.getExperimentId(), experiment.getTrials().size());
    }


    private void cleanUpMinio(Experiment e) {
        minio.removeTestConfigurationForExperiment(e.getTestId(), e.getExperimentNumber());
    }

}