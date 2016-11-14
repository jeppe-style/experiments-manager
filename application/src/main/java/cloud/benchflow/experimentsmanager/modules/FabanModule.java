package cloud.benchflow.experimentsmanager.modules;

import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import cloud.benchflow.experimentsmanager.configurations.FabanConfiguration;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.configurations.FabanClientConfigImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 10/01/16.
 */
public class FabanModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @Named("faban")
    public FabanClient provideFaban(ExperimentsManagerConfiguration config) throws URISyntaxException {
        FabanConfiguration fabanConfig = config.getFabanConfiguration();
        return new FabanClient().withConfig(
                new FabanClientConfigImpl(fabanConfig.getUser(),
                        fabanConfig.getPassword(),
                        new URI(fabanConfig.getAddress())));
    }

    @Provides
    @Named("retries")
    public Integer provideRetries(ExperimentsManagerConfiguration config) {
        return config.getFabanConfiguration().getSubmitRetries();
    }

}
