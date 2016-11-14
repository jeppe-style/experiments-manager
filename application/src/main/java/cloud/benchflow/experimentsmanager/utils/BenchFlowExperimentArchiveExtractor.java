package cloud.benchflow.experimentsmanager.utils;

import cloud.benchflow.experimentsmanager.utils.exceptions.ArchiveExtractionException;
import cloud.benchflow.minio.BenchFlowMinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         <p>
 *         Created on 28/07/16.
 */
public class BenchFlowExperimentArchiveExtractor {

    private static final Charset charset = StandardCharsets.UTF_8;
    private static Logger logger = LoggerFactory.getLogger(BenchFlowExperimentArchiveExtractor.class.getName());
    private static BiPredicate<ZipEntry, String> isExpConfigEntry = (e, s) -> e.getName().endsWith(s);
    private static Function<ZipEntry, String> getEntryFileName = e -> e.getName().replaceFirst(".*/([^/?]+).*", "$1");

    // TODO - will be extracted into configuration file in the future
    private static Predicate<ZipEntry> isExpConfig = e -> isExpConfigEntry.test(e, "/benchflow-test.yml");
    private static Predicate<ZipEntry> isDeploymentDescriptor = e -> isExpConfigEntry.test(e, "/docker-compose.yml");
    //        public static Predicate<ZipEntry> isModel = e -> e.getName().contains("models") &&
//                !(getEntryFileName.apply(e).endsWith("models/"));
    private static Predicate<ZipEntry> isModel = e -> e.getName().contains("models") &&
            !(e.getName().endsWith("models/"));

    private BenchFlowMinioClient minio;
    private String minioExperimentId;


    public BenchFlowExperimentArchiveExtractor(BenchFlowMinioClient minio, String minioExperimentId) {
        this.minio = minio;
        this.minioExperimentId = minioExperimentId;
    }

    /**
     * Extracts an experiment archive (zip) into an ExperimentConfiguration instance.
     *
     * @param archive
     * @return
     * @throws ArchiveExtractionException
     */
    public ExperimentConfiguration extractExperimentArchive(ZipInputStream archive) throws ArchiveExtractionException {

        ExperimentConfiguration config = new ExperimentConfiguration();

        try {

            ZipEntry zipEntry;

            while ((zipEntry = archive.getNextEntry()) != null) {

                String zipEntryData = readZipEntry(archive);

                if (isDeploymentDescriptor.test(zipEntry)) {

                    config.setDeploymentDescriptor(zipEntryData);
                    minio.saveOriginalDeploymentDescriptor(minioExperimentId, zipEntryData);
                    logger.debug("Saved experiment configuration");

                } else if (isExpConfig.test(zipEntry)) {

                    config.setExpConfig(zipEntryData);
                    minio.saveOriginalTestConfiguration(minioExperimentId, zipEntryData);
                    logger.debug("Saved deployment descriptor");

                } else if (isModel.test(zipEntry)) {

                    String modelName = getEntryFileName.apply(zipEntry);

                    // TODO - will be changed to not do IO inside the minio client
                    //                    String model = readZipEntry(archive);
                    minio.saveModel(minioExperimentId, modelName, archive);
                    logger.debug("Saved model " + modelName);

                }

            }
        } catch (IOException e) {
            cleanUp();
            throw new ArchiveExtractionException(minioExperimentId, e);
        }

        return config;

    }

    private void cleanUp() {
        minio.removeModels(minioExperimentId);
        //TODO: add
        //minio.removeOriginalBenchFlowBenchmark(benchmarkId);
        //minio.removeOriginalDeploymentDescriptor(benchmarkId);
    }


    /**
     * Reads the data from the current ZipEntry in a ZipInputStream to a String.
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    protected String readZipEntry(ZipInputStream inputStream) throws IOException {

        byte[] buffer = new byte[1024];

        int len;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while ((len = inputStream.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }

        return out.toString(charset.name());

    }

}
