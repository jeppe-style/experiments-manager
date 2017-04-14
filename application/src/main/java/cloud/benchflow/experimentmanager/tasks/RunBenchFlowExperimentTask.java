package cloud.benchflow.experimentmanager.tasks;

import cloud.benchflow.dsl.BenchFlowDSL;
import cloud.benchflow.dsl.DemoConverter;
import cloud.benchflow.dsl.definition.BenchFlowExperiment;
import cloud.benchflow.dsl.definition.workload.Workload;
import cloud.benchflow.experimentmanager.constants.BenchFlowConstants;
import cloud.benchflow.experimentmanager.exceptions.web.ExperimentRunException;
import cloud.benchflow.experimentmanager.resources.BenchFlowExperimentResource;
import cloud.benchflow.experimentmanager.services.external.BenchFlowTestManagerService;
import cloud.benchflow.experimentmanager.services.external.DriversMakerService;
import cloud.benchflow.experimentmanager.services.external.MinioService;
import cloud.benchflow.experimentmanager.services.internal.dao.BenchFlowExperimentModelDAO;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.exceptions.FabanClientException;
import cloud.benchflow.faban.client.responses.RunId;
import cloud.benchflow.faban.client.responses.RunStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static cloud.benchflow.experimentmanager.constants.BenchFlowConstants.MODEL_ID_DELIMITER;
import static cloud.benchflow.faban.client.responses.RunStatus.Code.*;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         <p>
 *         Created on 07.11.16.
 */
public class RunBenchFlowExperimentTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RunBenchFlowExperimentTask.class.getSimpleName());

    private static String TEMP_DIR = "./tmp";

    private String experimentID;

    private BenchFlowExperimentModelDAO experimentDAO;

    private MinioService minioService;
    private FabanClient fabanClient;
    private DriversMakerService driversMakerService;
    private BenchFlowTestManagerService testManagerService;
    //    private ExecutorService submitRunsPool;
    private int submitRetries = 3;


    public RunBenchFlowExperimentTask(String experimentID, BenchFlowExperimentModelDAO experimentDAO, MinioService minioService, FabanClient fabanClient, DriversMakerService driversMakerService, BenchFlowTestManagerService testManagerService, int submitRetries) {

        this.experimentID = experimentID;
        this.experimentDAO = experimentDAO;
        this.minioService = minioService;
        this.fabanClient = fabanClient;
        this.driversMakerService = driversMakerService;
        this.testManagerService = testManagerService;
        this.submitRetries = submitRetries;

    }

    @Override
    public void run() {


        try {

            // save experiment model in DB
            experimentDAO.addExperiment(experimentID);

            // get the BenchFlowExperimentDefinition from minioService
            String experimentYamlString = IOUtils.toString(minioService.getExperimentDefinition(experimentID), StandardCharsets.UTF_8);

            BenchFlowExperiment experiment = BenchFlowDSL.experimentFromExperimentYaml(experimentYamlString).get();

            int nTrials = experiment.configuration().terminationCriteria().get().experiment().number();

            // convert to old version and save to minio, and also a new experimentID to send to DriversMaker
            // generate DriversMaker compatible files on minio
            DriversMakerCompatibleID driversMakerCompatibleID = new DriversMakerCompatibleID().invoke(experimentID);
            String driversMakerExperimentID = driversMakerCompatibleID.getDriversMakerExperimentID();
            String experimentName = driversMakerCompatibleID.getExperimentName();
            long experimentNumber = driversMakerCompatibleID.getExperimentNumber();

            saveDriversMakerCompatibleFilesOnMinio(experiment, driversMakerExperimentID, experimentNumber);

            // generate benchmark from DriversMaker (one per experiment)
            // TODO - is driversMakerService responsible to generate the trialIDs, otherwise I think we should just pass the trialID to the driversMakerService

            driversMakerService.generateBenchmark(experimentName, experimentNumber, nTrials);

            // DEPLOY TO FABAN
            // get the generated benchflow-benchmark.jar from minioService and save to disk so that it can be sent
            // TODO - is this per experiment/trial? adjust below accordingly
            InputStream fabanBenchmark = minioService.getDriversMakerGeneratedBenchmark(driversMakerExperimentID, experimentNumber);

            // TODO - should this be a method (part of Faban Client?)
            String fabanExperimentId = experimentID.replace(MODEL_ID_DELIMITER, BenchFlowConstants.FABAN_ID_DELIMITER);

            // store on disk because there are issues sending InputStream directly
            java.nio.file.Path benchmarkPath =
                    Paths.get(TEMP_DIR)
                            .resolve(experimentID)
                            .resolve(fabanExperimentId + ".jar");

            FileUtils.copyInputStreamToFile(fabanBenchmark, benchmarkPath.toFile());

            // deploy experiment to Faban
            fabanClient.deploy(benchmarkPath.toFile());
            logger.info("deployed benchmark to Faban: " + fabanExperimentId); // TODO - move this into fabanClient

            // remove file that was sent to fabanClient
            FileUtils.forceDelete(benchmarkPath.toFile());

            for (int trialNumber = 1; trialNumber <= nTrials; trialNumber++) {

                // add trial to experiment
                String trialID = experimentDAO.addTrial(experimentID, trialNumber);

                // A) submit to fabanClient
                int retries = submitRetries;

                java.nio.file.Path fabanConfigPath = Paths.get(TEMP_DIR)
                        .resolve(experimentID)
                        .resolve(String.valueOf(trialNumber))
                        .resolve("run.xml");

                InputStream configInputStream = minioService.getDriversMakerGeneratedFabanConfiguration(driversMakerExperimentID, experimentNumber, trialNumber);
                String config = IOUtils.toString(configInputStream, StandardCharsets.UTF_8);

                FileUtils.writeStringToFile(fabanConfigPath.toFile(), config, StandardCharsets.UTF_8);

                RunId runId = null;
                while (runId == null) {
                    try {
//                                runId = new RunId(benchmarkName,"foo");
                        runId = fabanClient.submit(fabanExperimentId, trialID.replace('.', '-'),
                                fabanConfigPath.toFile());
//                                runId = fabanClient.submit("benchflow-benchmark", "benchflow-benchmark",
//                                        fabanConfigPath.toFile());
                    } catch (FabanClientException e) {
                        if (retries > 0) retries--;
                        else {
                            throw e;
                        }
                    }
                }

                experimentDAO.setFabanTrialID(experimentID, trialNumber, runId.toString());
                experimentDAO.setTrialModelAsStarted(experimentID, trialNumber);

                // B) wait/poll for trial to complete and store the trial result in the DB
                RunStatus status = fabanClient.status(runId); // TODO - is this the status we want to use? No it is a subset, should also include metrics computation status

                while (status.getStatus().equals(QUEUED) || status.getStatus().equals(RECEIVED) || status.getStatus().equals(STARTED)) {
                    Thread.sleep(1000);
                    status = fabanClient.status(runId);
                }

                experimentDAO.setTrialStatus(experimentID, trialNumber, status.getStatus());
                testManagerService.submitTrialStatus(trialID, status.getStatus());

            }

        } catch (IOException e) {
            logger.error("could not read experiment definition: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO - we should inform the test-manager? Yes, have an API for that
            experimentDAO.setExperimentModelAsAborted(experimentID);
            //TODO: set all trials as aborted, if any
            //TODO - check if any of them was queued on fabanClient, to kill it ??
            logger.error("Exception", e.getMessage());
            throw new ExperimentRunException(e.getMessage(), e);
        } finally {
            // remove file that was sent to fabanClient
            try {
                FileUtils.forceDelete(Paths.get(TEMP_DIR).toFile());
            } catch (IOException e) {
                // if folder doesn't exist then it is OK anyway
            }
        }
    }

    private void saveDriversMakerCompatibleFilesOnMinio(BenchFlowExperiment experiment, String driversMakerExperimentID, long experimentNumber) {

        //        InputStream definitionInputStream = minioService.getExperimentDefinition(experimentID);

        // convert to previous version
        String oldExperimentDefinitionYaml = DemoConverter.convertExperimentToPreviousYamlString(experiment);
        InputStream definitionInputStream = IOUtils.toInputStream(oldExperimentDefinitionYaml, StandardCharsets.UTF_8);

        minioService.saveExperimentDefintionForDriversMaker(driversMakerExperimentID, experimentNumber, definitionInputStream);

        InputStream deploymentDescriptorInputStream = minioService.getExperimentDeploymentDescriptor(experimentID);
        minioService.saveDeploymentDescriptorForDriversMaker(driversMakerExperimentID, experimentNumber, deploymentDescriptorInputStream);

        // convert to Java compatible type
        Collection<Workload> workloadCollection = JavaConverters.asJavaCollectionConverter(experiment.workload().values()).asJavaCollection();

        for (Workload workload : workloadCollection) {

            for (String operationName : JavaConverters.asJavaCollectionConverter(workload.operations()).asJavaCollection()) {

                InputStream modelInputStream = minioService.getExperimentBPMNModel(experimentID, operationName);
                minioService.saveExperimentBPMNModelForDriversMaker(driversMakerExperimentID, operationName, modelInputStream);

            }
        }
    }

    public static class DriversMakerCompatibleID {
        private String experimentName;
        private long experimentNumber;
        private String driversMakerExperimentID;

        public String getExperimentName() {
            return experimentName;
        }

        public long getExperimentNumber() {
            return experimentNumber;
        }

        public String getDriversMakerExperimentID() {
            return driversMakerExperimentID;
        }

        public DriversMakerCompatibleID invoke(String experimentID) {
            // userID = "BenchFlow"
            // ExperimentID := userId.experimentName.experimentNumber

            String[] experimentIDArray = experimentID.split("\\.");

            experimentName = experimentIDArray[1] + MODEL_ID_DELIMITER + experimentIDArray[2];
            experimentNumber = Long.parseLong(experimentIDArray[3]);
            driversMakerExperimentID = "BenchFlow." + experimentName;
            return this;
        }
    }
}
