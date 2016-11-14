package cloud.benchflow.experimentsmanager.modules;

import cloud.benchflow.experimentsmanager.configurations.DriversMakerConfiguration;
import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;
import org.apache.http.client.HttpClient;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 10/01/16.
 */
public class DriversMakerModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    @Named("drivers-maker")
    @Inject
    public DriversMaker provideDriversMaker(/*@Named("http") HttpClient httpClient,*/
                                            ExperimentsManagerConfiguration config,
                                            Environment environment) {

        System.out.println(config.getHttpClientConfiguration().getTimeout());

        final HttpClient httpClient = new HttpClientBuilder(environment)
                .using(config.getHttpClientConfiguration())
                .build(environment.getName());
        DriversMakerConfiguration dmConfig = config.getDriversMakerConfiguration();
//        HttpClient httpClient = HttpClients.createDefault();
        return new DriversMaker(dmConfig.getAddress(), httpClient);
    }

}
