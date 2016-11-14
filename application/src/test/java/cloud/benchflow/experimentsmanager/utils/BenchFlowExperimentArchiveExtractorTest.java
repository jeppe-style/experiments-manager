package cloud.benchflow.experimentsmanager.utils;

import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.resources.lifecycle.RunExperimentResource;
import cloud.benchflow.experimentsmanager.testUtils.SetUpMocks;
import cloud.benchflow.experimentsmanager.testUtils.TestArchive;
import cloud.benchflow.experimentsmanager.utils.exceptions.ArchiveExtractionException;
import cloud.benchflow.minio.BenchFlowMinioClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by jesper on 02.11.16.
 */
public class BenchFlowExperimentArchiveExtractorTest {


    @Mock
    private BenchFlowMinioClient minioClient = Mockito.mock(BenchFlowMinioClient.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private BenchFlowExperimentArchiveExtractor extractor;
    private Experiment experiment;
    private ZipInputStream input;


    @Before
    public void setUp() throws Exception {

        experiment = new Experiment(RunExperimentResource.USER, TestArchive.EXPERIMENT_NAME);

        extractor = new BenchFlowExperimentArchiveExtractor(minioClient, experiment.getMinioExperimentId());

        InputStream expArchive = new FileInputStream(TestArchive.TEST_ARCHIVE_FILENAME);

        input = new ZipInputStream(expArchive);

        SetUpMocks.setUpMinioClientMocksWithExperiment(minioClient, experiment);

    }


    @Test
    public void extractExperimentArchiveToConfiguration() throws Exception {

        ExperimentConfiguration configuration = extractor.extractExperimentArchive(input);

        Assert.assertNotNull(configuration);

        Assert.assertTrue(configuration.getDeploymentDescriptor().contains("version:"));

        Assert.assertTrue(configuration.getExpConfig().contains("sut:"));


    }

    @Test
    public void extractExperimentArchiveException() throws Exception {

        exception.expect(ArchiveExtractionException.class);
        exception.expectMessage(ArchiveExtractionException.MESSAGE + experiment.getMinioExperimentId());

        BenchFlowExperimentArchiveExtractor spyExtractor = Mockito.spy(extractor);

        Mockito.doThrow(IOException.class).when(spyExtractor).readZipEntry(Mockito.any(ZipInputStream.class));

        spyExtractor.extractExperimentArchive(input);

    }

}