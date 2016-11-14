package cloud.benchflow.experimentsmanager;

import cloud.benchflow.experimentsmanager.modules.*;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class ExperimentsManagerApplication extends Application<ExperimentsManagerConfiguration> {
    public static void main(String[] args) throws Exception {
        new ExperimentsManagerApplication().run(args);
    }

    @Override
    public String getName() {
        return "experiments-manager";
    }

    @Override
    public void initialize(Bootstrap<ExperimentsManagerConfiguration> bootstrap) {

        bootstrap.addBundle(new TemplateConfigBundle());

        GuiceBundle<ExperimentsManagerConfiguration> guiceBundle =
                GuiceBundle.<ExperimentsManagerConfiguration>builder()
                .enableAutoConfig("cloud.benchflow.experimentsmanager")
                .modules(new BenchFlowEnvModule(),
                         new FabanModule(),
                         new MinioModule(),
                         new DriversMakerModule(),
                         new DbModule(),
                         new ExecutorServiceModule())
                .build();

        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public void run(ExperimentsManagerConfiguration configuration, Environment environment) {
        environment.jersey().register(MultiPartFeature.class);
    }

}
