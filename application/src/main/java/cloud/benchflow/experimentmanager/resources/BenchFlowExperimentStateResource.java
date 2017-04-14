package cloud.benchflow.experimentmanager.resources;

import cloud.benchflow.experimentmanager.api.request.BenchFlowExperimentStateRequest;
import cloud.benchflow.experimentmanager.api.response.BenchFlowExperimentStateResponse;
import cloud.benchflow.experimentmanager.constants.BenchFlowConstants;
import cloud.benchflow.experimentmanager.exceptions.BenchFlowExperimentIDDoesNotExistException;
import cloud.benchflow.experimentmanager.models.BenchFlowExperimentModel;
import cloud.benchflow.experimentmanager.services.internal.dao.BenchFlowExperimentModelDAO;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 27.02.17.
 */

@Path("/{benchFlowExperimentID}/state")
@Api(value = "benchflow-experiment")
public class BenchFlowExperimentStateResource {

    public static final String ACTION_PATH = "/state";

    private static Logger logger = LoggerFactory.getLogger(BenchFlowExperimentStateResource.class.getName());

    @PathParam("benchFlowExperimentID")
    String experimentID;

    private BenchFlowExperimentModelDAO experimentModelDAO;

    public BenchFlowExperimentStateResource(BenchFlowExperimentModelDAO experimentModelDAO) {
        this.experimentModelDAO = experimentModelDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BenchFlowExperimentStateResponse getExperimentState() {

        logger.info("GET /" + experimentID + ACTION_PATH);

        try {

            BenchFlowExperimentModel.BenchFlowExperimentState state = experimentModelDAO.getExperimentModelState(experimentID);

            return new BenchFlowExperimentStateResponse(state);

        } catch (BenchFlowExperimentIDDoesNotExistException e) {
            logger.error(BenchFlowConstants.INVALID_EXPERIMENT_ID_MESSAGE + ": " + experimentID + ": " + e.getMessage());
            throw new WebApplicationException(BenchFlowConstants.INVALID_EXPERIMENT_ID_MESSAGE);
        }

    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public BenchFlowExperimentStateResponse changeExperimentState(@NotNull @Valid BenchFlowExperimentStateRequest stateRequest) {

        logger.info("PUT /" + experimentID + ACTION_PATH);


        BenchFlowExperimentModel.BenchFlowExperimentState state = experimentModelDAO.setExperimentModelState(
                experimentID,
                stateRequest.getState()
        );

        if (state == null) {
            logger.info(BenchFlowConstants.INVALID_EXPERIMENT_ID_MESSAGE + ": " + experimentID);
            throw new WebApplicationException(BenchFlowConstants.INVALID_EXPERIMENT_ID_MESSAGE);
        }

        return new BenchFlowExperimentStateResponse(state);

    }

}
