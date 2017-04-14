package cloud.benchflow.experimentmanager.configurations.factory;

import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.configurations.FabanClientConfigImpl;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 26/11/15.
 */
public class FabanServiceFactory {

    private String user;

    @JsonProperty
    public String getUser() {
        return user;
    }

    @JsonProperty
    public void setUser(String user) {
        this.user = user;
    }

    private String password;

    @JsonProperty
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    @NotEmpty
    private String address;

    @JsonProperty
    public String getAddress() {
        return address;
    }

    @JsonProperty
    public void setAddress(String address) {
        this.address = address;
    }

    private int submitRetries;

    @JsonProperty
    public int getSubmitRetries() {
        return submitRetries;
    }

    @JsonProperty
    public void setSubmitRetries(int submitRetries) {
        this.submitRetries = submitRetries;
    }

    public FabanClient build() throws URISyntaxException {

        return new FabanClient().withConfig(
                new FabanClientConfigImpl(getUser(),
                                          getPassword(), new URI(getAddress())));

    }


}
