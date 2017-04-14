package cloud.benchflow.experimentmanager.api.request;

import cloud.benchflow.experimentmanager.models.BenchFlowExperimentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 27.02.17.
 */
public class BenchFlowExperimentStateRequest {

    @NotEmpty
    @JsonProperty
    private BenchFlowExperimentModel.BenchFlowExperimentState state;

    public BenchFlowExperimentStateRequest(BenchFlowExperimentModel.BenchFlowExperimentState state) {
        this.state = state;
    }

    public BenchFlowExperimentModel.BenchFlowExperimentState getState() {
        return state;
    }
}
