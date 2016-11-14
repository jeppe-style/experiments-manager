package cloud.benchflow.experimentsmanager.modules;

import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import cloud.benchflow.experimentsmanager.utils.env.BenchFlowEnv;
import cloud.benchflow.minio.BenchFlowMinioClient;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 03/03/16.
 */
public class MinioModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @Named("minio")
    @Inject
    public BenchFlowMinioClient provideMinio(ExperimentsManagerConfiguration ec, @Named("bfEnv") BenchFlowEnv benv)
            throws InvalidPortException, InvalidEndpointException {
        String minioAddress = ec.getMinioConfiguration().getAddress();
        String accessKey = benv.<String>getVariable("MINIO_ACCESS_KEY");
        String secretKey = benv.<String>getVariable("MINIO_SECRET_KEY");
        return new BenchFlowMinioClient(minioAddress, accessKey, secretKey);
    }

}
