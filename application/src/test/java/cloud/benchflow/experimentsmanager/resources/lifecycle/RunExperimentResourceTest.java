package cloud.benchflow.experimentsmanager.resources.lifecycle;

import cloud.benchflow.experimentsmanager.db.DbManager;
import cloud.benchflow.experimentsmanager.testutils.SetUpMocks;
import cloud.benchflow.experimentsmanager.testutils.TestArchive;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.minio.BenchFlowMinioClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.WebApplicationException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by jesper on 26.10.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class RunExperimentResourceTest {

    // TODO - also add test data for systems different than a WfMS

    // TODO - NOW: make unit tests starting resources with mocks (db, minio client etc.)

    @Rule
    public ExpectedException exception = ExpectedException.none();

    // TODO - move this so that we only have to start the DB once before all tests in all test classes are run (integration testing)
//    @Rule
//    public GuiceyAppRule<ExperimentsManagerConfiguration> RULE = new GuiceyAppRule<>(ExperimentsManagerApplication.class, "../configuration.yml",
//            ConfigOverride.config("database.address", "mem"),
//            ConfigOverride.config("database.hibernateConfig", "src/test/resources/hibernate.cfg.xml"),
//            ConfigOverride.config("database.createExperimentsTableQueryPath", "src/main/resources/db/create-experiments-table.sql"),
//            ConfigOverride.config("database.createTrialsTableQueryPath", "src/main/resources/db/create-trials-table.sql"),
//            ConfigOverride.config("drivers-maker.address", "localhost"),
//            ConfigOverride.config("faban.address", "localhost"),
//            ConfigOverride.config("minio.address", "localhost"),
//            ConfigOverride.config("benchflowEnv.config.yml", "src/test/resources/app/config.yml")
//            );
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule(); // initialize the mocks according to @Mock annotation
    // TODO - mock the services below
    @Mock
    private BenchFlowMinioClient minioMock;
    @Mock
    private DbManager dbMock;
    @Mock
    private FabanClient fabanMock;
    @Mock
    private DriversMaker driversMakerMock;
    @Mock(name = "runExperimentsPool")
    private ExecutorService runExperimentsPoolMock;
    @Mock
    private ExecutorService submitRunsPool;
    @InjectMocks
    private RunExperimentResource experimentResource;

    @Before
    public void setUp() throws Exception {

        SetUpMocks.setUpMinioClientMocks(minioMock);
        SetUpMocks.setUpDbManagerMocks(dbMock);
        SetUpMocks.setUpRunExperimentsPoolMock(runExperimentsPoolMock);

    }

    @Test
    public void emptyRequest() throws Exception {


        exception.expect(WebApplicationException.class);
        // TODO - change string below
        exception.expectMessage("400");


        experimentResource.runAsync(null, null);

    }

    @Test
    public void runAsync() throws Exception {

        InputStream expArchive = new FileInputStream(TestArchive.TEST_ARCHIVE_FILENAME);

        experimentResource.runAsync(TestArchive.EXPERIMENT_NAME, expArchive);

    }
}