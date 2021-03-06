package cloud.benchflow.experimentsmanager.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 13/02/16.
 */
public class BenchFlowEnvConfiguration {

    @NotEmpty
    private String configPath;

    @JsonProperty("config.yml")
    public String getConfigPath() {
        return configPath;
    }

    @JsonProperty("config.yml")
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
