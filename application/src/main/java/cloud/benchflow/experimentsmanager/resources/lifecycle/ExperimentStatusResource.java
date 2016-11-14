package cloud.benchflow.experimentsmanager.resources.lifecycle;

import cloud.benchflow.experimentsmanager.db.DbManager;
import cloud.benchflow.experimentsmanager.db.ExperimentsDAO;
import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.db.entities.Trial;
import cloud.benchflow.experimentsmanager.exceptions.NoSuchExperimentIdException;
import cloud.benchflow.experimentsmanager.exceptions.NoSuchTrialIdException;
import cloud.benchflow.experimentsmanager.responses.lifecycle.ExperimentStatusResponse;
import cloud.benchflow.experimentsmanager.responses.lifecycle.TrialStatusResponse;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.exceptions.RunIdNotFoundException;
import cloud.benchflow.faban.client.responses.RunId;
import cloud.benchflow.faban.client.responses.RunStatus;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 06/04/16.
 */
@Path("status")
public class ExperimentStatusResource {

    private static Logger logger = LoggerFactory.getLogger(ExperimentStatusResource.class.getName());

    private FabanClient faban;
    private DbManager db;

    @Inject
    public ExperimentStatusResource(@Named("faban") FabanClient faban,
                                    @Named("db") DbManager db) {
        this.faban = faban;
        this.db = db;
    }


    @GET
    @Path("{testName}/{experimentNumber}/{trialNumber}")
    @Produces("application/vnd.experiments-manager.v2+json")
    public TrialStatusResponse getTrialStatus(@PathParam("testName") String testName,
                                              @PathParam("experimentNumber") long experimentNumber,
                                              @PathParam("trialNumber") int trialNumber) {

        String userId = "BenchFlow";

        try (ExperimentsDAO session = db.getExperimentsDAO()) {

            Trial trial = session.getTrial(userId, testName, experimentNumber, trialNumber);

            if (trial == null)
                throw new NoSuchTrialIdException(new Trial(userId,
                        testName,
                        experimentNumber,
                        trialNumber).getTrialId());
            if (trial.isSubmitted()) {

                String runId = trial.getFabanRunId();
                try {

                    RunStatus.Code status = faban.status(new RunId(runId)).getStatus();
                    if (status == RunStatus.Code.COMPLETED) {
                        trial.setCompleted();
                        session.update(trial);
                    } else if (status == RunStatus.Code.FAILED) {
                        trial.setFailed();
                        session.update(trial);
                    }
                    return new TrialStatusResponse(trial.getTrialId(), status.name());

                } catch (RunIdNotFoundException e) {
                    //this should never happen
                    throw new NoSuchTrialIdException(trial.getTrialId());
                }

            } else {
                return new TrialStatusResponse(trial.getTrialId(), trial.getStatus());
            }
        }
    }

    @GET
    @Path("{testName}/{experimentNumber}")
    @Produces("application/vnd.experiments-manager.v2+json")
    public ExperimentStatusResponse getExperimentStatus(@PathParam("testName") String testName,
                                                        @PathParam("experimentNumber") long experimentNumber) {

        String userId = "BenchFlow";

        try (ExperimentsDAO session = db.getExperimentsDAO()) {

            Experiment experiment = session.getExperiment(userId, testName, experimentNumber);

            if (experiment == null) {
                Experiment exp = new Experiment(userId, testName);
                exp.setExperimentNumber(experimentNumber);
                throw new NoSuchExperimentIdException(exp.getExperimentId());
            }

            ExperimentStatusResponse response =
                    new ExperimentStatusResponse(experiment.getExperimentId(),
                            experiment.getStatus());

            for (Trial t : experiment.getTrials()) {
                response.addTrialStatus(getTrialStatus(testName, experimentNumber, t.getTrialNumber()));
            }

            if (!(experiment.isCompleted() || experiment.isAborted()) &&
                    experiment.getTrials().stream().filter(Trial::isCompleted).count() == experiment.getTrials().size()) {
                experiment.setCompleted();
                response.setExperimentStatus(experiment.getStatus());
                session.update(experiment);
            }

            return response;

        }
    }

}
