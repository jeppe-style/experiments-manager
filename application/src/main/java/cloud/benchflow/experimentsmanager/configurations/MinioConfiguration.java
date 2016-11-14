package cloud.benchflow.experimentsmanager.configurations;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 26/11/15.
 */
public class MinioConfiguration {

    @NotEmpty
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
