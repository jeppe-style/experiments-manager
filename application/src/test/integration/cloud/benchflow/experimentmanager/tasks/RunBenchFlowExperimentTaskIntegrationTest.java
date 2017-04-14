package cloud.benchflow.experimentmanager.tasks;

import cloud.benchflow.experimentmanager.BenchFlowExperimentManagerApplication;
import cloud.benchflow.experimentmanager.DockerComposeTest;
import cloud.benchflow.experimentmanager.configurations.BenchFlowExperimentManagerConfiguration;
import cloud.benchflow.experimentmanager.constants.BenchFlowConstants;
import cloud.benchflow.experimentmanager.helpers.MinioTestData;
import cloud.benchflow.experimentmanager.helpers.TestConstants;
import cloud.benchflow.experimentmanager.services.external.BenchFlowTestManagerService;
import cloud.benchflow.experimentmanager.services.external.DriversMakerService;
import cloud.benchflow.experimentmanager.services.external.MinioService;
import cloud.benchflow.experimentmanager.services.internal.dao.BenchFlowExperimentModelDAO;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.responses.RunId;
import cloud.benchflow.faban.client.responses.RunStatus;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 2017-04-14
 */
public class RunBenchFlowExperimentTaskIntegrationTest extends DockerComposeTest {

    private static final int TEST_PORT = 8090;
    private static final String TEST_ADDRESS = "localhost:" + TEST_PORT;

    @Rule
    public final DropwizardAppRule<BenchFlowExperimentManagerConfiguration> RULE = new DropwizardAppRule<>(
            BenchFlowExperimentManagerApplication.class,
            "../configuration.yml",
            ConfigOverride.config("mongoDB.hostname", MONGO_CONTAINER.getIp()),
            ConfigOverride.config("mongoDB.port", String.valueOf(MONGO_CONTAINER.getExternalPort())),
            ConfigOverride.config("minio.address", "http://" + MINIO_CONTAINER.getIp() + ":" + MINIO_CONTAINER.getExternalPort()),
            ConfigOverride.config("minio.accessKey", MINIO_ACCESS_KEY),
            ConfigOverride.config("minio.secretKey", MINIO_SECRET_KEY),
            ConfigOverride.config("driversMaker.address", TEST_ADDRESS),
            ConfigOverride.config("testManager.address", TEST_ADDRESS),
            ConfigOverride.config("faban.address", TEST_ADDRESS)
    );

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TEST_PORT);

    private String experimentID = TestConstants.BENCHFLOW_EXPERIMENT_ID;
    private RunBenchFlowExperimentTask runExperimentTask;
    private FabanClient fabanClientMock;

    @Before
    public void setUp() throws Exception {

        BenchFlowExperimentManagerConfiguration configuration = RULE.getConfiguration();
        Environment environment = RULE.getEnvironment();

        Client client = new JerseyClientBuilder(environment)
                .using(configuration.getJerseyClientConfiguration())
                .build("experiment-manager-test");

        BenchFlowExperimentModelDAO experimentModelDAO = new BenchFlowExperimentModelDAO(configuration.getMongoDBFactory().build());
        MinioService minioService = configuration.getMinioServiceFactory().build();
        fabanClientMock = Mockito.mock(FabanClient.class);
        DriversMakerService driversMakerService = configuration.getDriversMakerServiceFactory().build(client);
        BenchFlowTestManagerService testManagerService = configuration.getTestManagerServiceFactory().build(client);

        int submitRetries = configuration.getFabanServiceFactory().getSubmitRetries();

        runExperimentTask = new RunBenchFlowExperimentTask(
                experimentID,
                experimentModelDAO,
                minioService,
                fabanClientMock,
                driversMakerService,
                testManagerService,
                submitRetries
        );

        RunBenchFlowExperimentTask.DriversMakerCompatibleID driversMakerCompatibleID = new RunBenchFlowExperimentTask.DriversMakerCompatibleID().invoke(experimentID);

        minioService.saveExperimentDefinition(experimentID, MinioTestData.getExperimentDefinition());
        minioService.saveExperimentDeploymentDescriptor(experimentID, MinioTestData.getDeploymentDescriptor());
        minioService.saveExperimentBPMNModel(experimentID, MinioTestData.BPM_MODEL_11_PARALLEL_NAME, MinioTestData.get11ParallelStructuredModel());
        minioService.saveDriversMakerGeneratedBenchmark(driversMakerCompatibleID.getDriversMakerExperimentID(), driversMakerCompatibleID.getExperimentNumber(), MinioTestData.getGeneratedBenchmark());
        minioService.saveDriversMakerGeneratedFabanConfiguration(driversMakerCompatibleID.getDriversMakerExperimentID(), driversMakerCompatibleID.getExperimentNumber(), 1, MinioTestData.getFabanConfiguration());

    }

    @Test
    public void run() throws Exception {

        String fabanID = "test_faban_id";
        RunId runId = new RunId(fabanID, "1");
        RunStatus status = new RunStatus("COMPLETED", runId);
        String trialID = experimentID + BenchFlowConstants.MODEL_ID_DELIMITER + 1;

        // Drivers Maker Stub
        stubFor(post(urlEqualTo(DriversMakerService.GENERATE_BENCHMARK_PATH))
                .willReturn(aResponse().withStatus(Response.Status.OK.getStatusCode())
                )
        );

        // Test Manager Stub
        stubFor(post(urlEqualTo(BenchFlowTestManagerService.EXPERIMENT_TRIAL_PATH + trialID + BenchFlowTestManagerService.STATUS_PATH))
                .willReturn(aResponse().withStatus(Response.Status.OK.getStatusCode())
                )
        );

        Mockito.doReturn(runId)
                .when(fabanClientMock)
                .submit(Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class));

        Mockito.doReturn(status)
                .when(fabanClientMock)
                .status(runId);


        runExperimentTask.run();

    }
}