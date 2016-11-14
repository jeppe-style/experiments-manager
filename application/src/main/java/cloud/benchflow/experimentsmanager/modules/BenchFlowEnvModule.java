package cloud.benchflow.experimentsmanager.modules;

import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import cloud.benchflow.experimentsmanager.utils.env.BenchFlowEnv;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;

import java.io.FileNotFoundException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 03/03/16.
 */
public class BenchFlowEnvModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    @Named("bfEnv")
    public BenchFlowEnv provideBenchFlowEnv(ExperimentsManagerConfiguration config) throws FileNotFoundException {
        return new BenchFlowEnv(config.getBenchFlowEnvConfiguration().getConfigPath());
    }
}
