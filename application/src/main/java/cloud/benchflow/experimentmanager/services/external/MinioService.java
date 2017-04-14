package cloud.benchflow.experimentmanager.services.external;

import io.minio.MinioClient;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static cloud.benchflow.experimentmanager.constants.BenchFlowConstants.*;
import static cloud.benchflow.experimentmanager.constants.BenchFlowConstants.FABAN_CONFIG_FILENAME;
import static cloud.benchflow.experimentmanager.constants.BenchFlowConstants.MODEL_ID_DELIMITER;
import static cloud.benchflow.experimentmanager.demo.Hashing.hashKey;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 05.03.17.
 */
public class MinioService {

    // http://www.iana.org/assignments/media-types/application/octet-stream
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static Logger logger = LoggerFactory.getLogger(MinioService.class.getSimpleName());
    private MinioClient minioClient;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void initializeBuckets() {

        try {
            if (!minioClient.bucketExists(TESTS_BUCKET)) {
                minioClient.makeBucket(TESTS_BUCKET);
            }

        } catch (InvalidBucketNameException | NoSuchAlgorithmException | IOException | InsufficientDataException | InvalidKeyException | NoResponseException | XmlPullParserException | ErrorResponseException | InternalException e) {
            // TODO - handle exception
            logger.error("Exception in initializeBuckets ", e);
        }

    }

    public boolean isValidExperimentID(String experimentID) {

        logger.info("isValidExperimentID: " + experimentID);

        String objectName = minioCompatibleID(experimentID) + MINIO_ID_DELIMITER + PT_PE_DEFINITION_FILE_NAME;

        try {
            boolean valid = minioClient.bucketExists(TESTS_BUCKET);

            if (valid) {

                valid = minioClient.listObjects(TESTS_BUCKET, objectName).iterator().hasNext();
            }

            return valid;

        } catch (MinioException | NoSuchAlgorithmException | XmlPullParserException | IOException | InvalidKeyException e) {
            logger.error(e.getMessage());
            return false;
        }

    }

    public void saveExperimentDefinition(String experimentID, InputStream definitionInputStream) {

        String objectName = minioCompatibleID(experimentID) + MINIO_ID_DELIMITER + PT_PE_DEFINITION_FILE_NAME;
        putInputStreamObject(definitionInputStream, objectName);

    }

    public InputStream getExperimentDefinition(String experimentID) {

        logger.info("getExperimentDefinition: " + experimentID);

        String objectName = minioCompatibleID(experimentID) + MINIO_ID_DELIMITER + PT_PE_DEFINITION_FILE_NAME;

        return getInputStreamObject(objectName);
    }

    public void saveExperimentDefintionForDriversMaker(String experimentID, long experimentNumber, InputStream definitionInputStream) {

        // TODO - change/remove this method when DriversMaker changes

        logger.info("saveExperimentDefintionForDriversMaker: " + experimentID + MODEL_ID_DELIMITER + experimentNumber);

        String objectName = minioCompatibleID(experimentID);

        try {

            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + PT_PE_DEFINITION_FILE_NAME;
            putInputStreamObject(definitionInputStream, hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }


    }

    public void saveExperimentDeploymentDescriptor(String experimentID, InputStream deploymentDescriptor) {

        String objectName = minioCompatibleID(experimentID) + MINIO_ID_DELIMITER + DEPLOYMENT_DESCRIPTOR_FILE_NAME;
        putInputStreamObject(deploymentDescriptor, objectName);

    }

    public InputStream getExperimentDeploymentDescriptor(String experimentID) {

        // TODO - change/remove this method when DriversMaker changes

        logger.info("getExperimentDeploymentDescriptor: " + experimentID);

        String objectName = minioCompatibleID(experimentID) + MINIO_ID_DELIMITER + DEPLOYMENT_DESCRIPTOR_FILE_NAME;

        return getInputStreamObject(objectName);

    }

    public void saveDeploymentDescriptorForDriversMaker(String experimentID, long experimentNumber, InputStream deploymentDescriptorInputStream) {

        // TODO - change/remove this method when DriversMaker changes

        logger.info("saveExperimentDefintionForDriversMaker: " + experimentID + MODEL_ID_DELIMITER + experimentNumber);

        String objectName = minioCompatibleID(experimentID);

        try {

            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + DEPLOYMENT_DESCRIPTOR_FILE_NAME;
            putInputStreamObject(deploymentDescriptorInputStream, hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }

    }

    public void saveExperimentBPMNModel(String experimentID, String modelName, InputStream modelInputStream) {

        logger.info("saveExperimentBPMNModel: " + experimentID + MINIO_ID_DELIMITER + modelName);

        String testID = experimentID.substring(0, experimentID.lastIndexOf("."));

        String objectName = minioCompatibleID(testID) + MINIO_ID_DELIMITER + BPMN_MODELS_FOLDER_NAME + MINIO_ID_DELIMITER + modelName;

        putInputStreamObject(modelInputStream, objectName);

    }

    public InputStream getExperimentBPMNModel(String experimentID, String modelName) {

        // TODO - change/remove this method when DriversMaker changes

        logger.info("getExperimentBPMNModel: " + experimentID + MINIO_ID_DELIMITER + modelName);

        String testID = experimentID.substring(0, experimentID.lastIndexOf("."));

        String objectName = minioCompatibleID(testID) + MINIO_ID_DELIMITER + BPMN_MODELS_FOLDER_NAME + MINIO_ID_DELIMITER + modelName;

        return getInputStreamObject(objectName);

    }

    public void saveExperimentBPMNModelForDriversMaker(String experimentID, String modelName, InputStream modelInputStream) {

        // TODO - change/remove this method when DriversMaker changes

        logger.info("saveExperimentBPMNModelForDriversMaker: " + experimentID + MINIO_ID_DELIMITER + modelName);

        String objectName = minioCompatibleID(experimentID);

        try {
            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + BPMN_MODELS_FOLDER_NAME + MINIO_ID_DELIMITER + modelName;
            putInputStreamObject(modelInputStream, hashedObjectName);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }

    }

    public void saveDriversMakerGeneratedBenchmark(String experimentID, long experimentNumber, InputStream generatedBenchmarkInputStream) {

        // TODO - change this method when DriversMaker changes

        logger.info("saveDriversMakerGeneratedBenchmark: " + experimentID + MODEL_ID_DELIMITER + experimentNumber);

        String objectName = minioCompatibleID(experimentID);

        try {

            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + GENERATED_BENCHMARK_FILENAME;
            putInputStreamObject(generatedBenchmarkInputStream, hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }

    }

    public InputStream getDriversMakerGeneratedBenchmark(String experimentID, long experimentNumber) {

        // TODO - change this method when DriversMaker changes

        logger.info("getDriversMakerGeneratedBenchmark: " + experimentID + MODEL_ID_DELIMITER + experimentNumber);

        String objectName = minioCompatibleID(experimentID);

        try {

            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + GENERATED_BENCHMARK_FILENAME;
            return getInputStreamObject(hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }

        return null;

    }

    public void saveDriversMakerGeneratedFabanConfiguration(String experimentID, long experimentNumber, long trialNumber, InputStream fabanConfigurationInputStream) {

        // TODO - change this method when DriversMaker changes

        logger.info("saveDriversMakerGeneratedFabanConfiguration: " + experimentID + MODEL_ID_DELIMITER + experimentNumber + MODEL_ID_DELIMITER + trialNumber);

        String objectName = minioCompatibleID(experimentID);

        try {
            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + trialNumber + MINIO_ID_DELIMITER + FABAN_CONFIG_FILENAME;
            putInputStreamObject(fabanConfigurationInputStream, hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    public InputStream getDriversMakerGeneratedFabanConfiguration(String experimentID, long experimentNumber, long trialNumber) {

        // TODO - change this method when DriversMaker changes

        logger.info("getDriversMakerGeneratedFabanConfiguration: " + experimentID + MODEL_ID_DELIMITER + experimentNumber + MODEL_ID_DELIMITER + trialNumber);

        String objectName = minioCompatibleID(experimentID);

        try {
            String hashedObjectName = hashKey(objectName) + MINIO_ID_DELIMITER + experimentNumber + MINIO_ID_DELIMITER + trialNumber + MINIO_ID_DELIMITER + FABAN_CONFIG_FILENAME;
            return getInputStreamObject(hashedObjectName);

        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     *
     * @param inputStream
     * @param objectName
     */
    private void putInputStreamObject(InputStream inputStream, String objectName) {

        logger.info("putInputStreamObject: " + objectName);

        try {

            minioClient.putObject(TESTS_BUCKET,
                    objectName,
                    inputStream,
                    inputStream.available(),
                    CONTENT_TYPE);

        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | NoResponseException | InvalidKeyException | ErrorResponseException | XmlPullParserException | InvalidArgumentException | InternalException e) {
            // TODO - handle exception
            logger.error("Exception in putInputStreamObject: " + objectName, e);
        }
    }

    /**
     * @param objectName
     *
     * @return
     */
    private InputStream getInputStreamObject(String objectName) {

        logger.info("getInputStreamObject: " + objectName);

        try {

            return minioClient.getObject(TESTS_BUCKET, objectName);

        } catch (InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | IOException | InvalidKeyException | NoResponseException | XmlPullParserException | InternalException | InvalidArgumentException e) {
            // TODO - handle exception
            logger.error("Exception in getInputStreamObject: " + objectName, e);
            return null;

        } catch (ErrorResponseException e) {
           /* happens if the object doesn't exist*/
            return null;
        }
    }



    private String minioCompatibleID(String id) {
        return id.replace(MODEL_ID_DELIMITER, MINIO_ID_DELIMITER);
    }

}
