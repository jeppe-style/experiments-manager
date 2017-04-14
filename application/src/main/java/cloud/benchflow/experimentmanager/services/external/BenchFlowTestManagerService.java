package cloud.benchflow.experimentmanager.services.external;

import cloud.benchflow.experimentmanager.exceptions.web.BenchmarkGenerationException;
import cloud.benchflow.faban.client.responses.RunStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 05.03.17.
 */
public class BenchFlowTestManagerService {

    public static final String EXPERIMENT_TRIAL_PATH = "/benchflow-experiment-trial/";
    public static final String STATUS_PATH = "/status";

    private Logger logger = LoggerFactory.getLogger(BenchFlowTestManagerService.class.getSimpleName());

    private WebTarget testManagerTarget;

    public BenchFlowTestManagerService(Client httpClient, String testManagerAddress) {

        this.testManagerTarget = httpClient.target("http://" + testManagerAddress);
    }

    public void submitTrialStatus(String trialID, RunStatus.Code statusCode) {

        logger.info("submitTrialStatus for " + trialID + " with status " + statusCode.name());

        SubmitTrialStatusRequest trialStatusRequest = new SubmitTrialStatusRequest();
        trialStatusRequest.setStatus(statusCode);

        Response response = testManagerTarget
                .path(EXPERIMENT_TRIAL_PATH)
                .path(trialID)
                .path(STATUS_PATH)
                .request()
                .post(Entity.entity(trialStatusRequest, MediaType.APPLICATION_JSON));

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {

            logger.error("submitTrialStatus: error connecting - " + response.getStatus());
            throw new BenchmarkGenerationException("Error in benchmark generation",
                    response.getStatus());
        }

        logger.info("submitTrialStatus: response: " + response.readEntity(String.class));

    }

    private class SubmitTrialStatusRequest {

        @NotNull
        @JsonProperty
        private RunStatus.Code status;

        public SubmitTrialStatusRequest() {
        }

        public SubmitTrialStatusRequest(RunStatus.Code status) {
            this.status = status;
        }

        public RunStatus.Code getStatus() {
            return status;
        }

        public void setStatus(RunStatus.Code status) {
            this.status = status;
        }
    }
}
