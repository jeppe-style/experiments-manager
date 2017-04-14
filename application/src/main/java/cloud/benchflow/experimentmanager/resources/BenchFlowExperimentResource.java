package cloud.benchflow.experimentmanager.resources;

import cloud.benchflow.experimentmanager.constants.BenchFlowConstants;
import cloud.benchflow.experimentmanager.services.external.BenchFlowTestManagerService;
import cloud.benchflow.experimentmanager.services.external.DriversMakerService;
import cloud.benchflow.experimentmanager.services.external.MinioService;
import cloud.benchflow.experimentmanager.services.internal.dao.BenchFlowExperimentModelDAO;
import cloud.benchflow.experimentmanager.tasks.RunBenchFlowExperimentTask;
import cloud.benchflow.faban.client.FabanClient;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 05/03/16.
 */
@Path("/{benchFlowExperimentID}/run")
@Api(value = "benchflow-experiment")
public class BenchFlowExperimentResource {

    public static final String ACTION_PATH = "/run";

    private static Logger logger = LoggerFactory.getLogger(BenchFlowExperimentResource.class.getSimpleName());

    private MinioService minio;
    private BenchFlowExperimentModelDAO experimentDAO;
    private FabanClient faban;
    private DriversMakerService driversMaker;
    private BenchFlowTestManagerService testManagerService;
    private ExecutorService taskExecutorService;

    private int submitRetries;

    public BenchFlowExperimentResource(
            MinioService minio,
            BenchFlowExperimentModelDAO experimentDAO,
            FabanClient faban,
            DriversMakerService driversMaker,
            ExecutorService taskExecutorService,
            BenchFlowTestManagerService testManagerService,
            int submitRetries
    ) {
        this.minio = minio;
        this.experimentDAO = experimentDAO;
        this.faban = faban;
        this.driversMaker = driversMaker;
        this.taskExecutorService = taskExecutorService;
        this.testManagerService = testManagerService;
        this.submitRetries = submitRetries;
    }

    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
    public void submitPerformanceExperiment(@PathParam("benchFlowExperimentID") String experimentID) {

        logger.info("request received: /" + experimentID + ACTION_PATH);

        // check that the experiment exists
        if (!minio.isValidExperimentID(experimentID)) {
            logger.info("invalid experimentID: " + experimentID);
            throw new WebApplicationException(BenchFlowConstants.INVALID_EXPERIMENT_ID_MESSAGE, Response.Status.PRECONDITION_FAILED);
        }

        RunBenchFlowExperimentTask task = new RunBenchFlowExperimentTask(
                experimentID,
                experimentDAO,
                minio,
                faban,
                driversMaker,
                testManagerService,
                submitRetries
        );

        // TODO - should go into a stateless queue (so that we can recover)
        // (for now) only allows one experiment at a time (poolSize == 1)
        taskExecutorService.submit(task);

    }

}
