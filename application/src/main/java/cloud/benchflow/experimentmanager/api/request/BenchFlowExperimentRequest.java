package cloud.benchflow.experimentmanager.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 27.02.17.
 */
public class BenchFlowExperimentRequest {

    @NotEmpty
    @JsonProperty
    String experimentID;

    public BenchFlowExperimentRequest() {
    }

    public BenchFlowExperimentRequest(String experimentID) {
        this.experimentID = experimentID;
    }

    public String getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(String experimentID) {
        this.experimentID = experimentID;
    }
}
