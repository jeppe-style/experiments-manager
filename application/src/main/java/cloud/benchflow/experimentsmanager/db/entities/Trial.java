package cloud.benchflow.experimentsmanager.db.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;

import javax.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 03/02/16.
 */
@Entity
@Table(name = "TRIALS")
@IdClass(Trial.TrialId.class)
@SelectBeforeUpdate
@DynamicUpdate
public class Trial {

    @Id
    @Column(name = "TRIAL_NUMBER")
    private int trialNumber;
    @Id
    @JoinColumns({
            @JoinColumn(name = "USERNAME", referencedColumnName = "USERNAME"),
            @JoinColumn(name = "EXPERIMENT_NAME", referencedColumnName = "EXPERIMENT_NAME"),
            @JoinColumn(name = "EXPERIMENT_NUMBER", referencedColumnName = "EXPERIMENT_NUMBER")
    })
    @ManyToOne
    private Experiment experiment;
    @Column(name = "FABAN_RUN_ID")
    private String fabanRunId;
    @Column(name = "PERFORMED_ON")
    private LocalDateTime performedOn;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private Status status;

    Trial() {
    }

    public Trial(int trialNumber) {
        this.trialNumber = trialNumber;
        this.performedOn = LocalDateTime.now();
        this.status = Status.SUBMITTED;
    }


    public Trial(String userId, String experimentName, long experimentNumber, int trialNumber) {
        this.experiment = new Experiment(userId, experimentName);
        this.experiment.setExperimentNumber(experimentNumber);
        this.trialNumber = trialNumber;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public int getTrialNumber() {
        return trialNumber;
    }

    public void setTrialNumber(int trialNumber) {
        this.trialNumber = trialNumber;
    }

    public String getFabanRunId() {
        return fabanRunId;
    }

    public void setFabanRunId(String fabanRunId) {
        this.fabanRunId = fabanRunId;
    }

    public String getTrialId() {
        return experiment.getExperimentId() + "." + trialNumber;
    }

    public LocalDateTime getPerformedOn() {
        return performedOn;
    }

    public void setPerformedOn(LocalDateTime performedOn) {
        this.performedOn = performedOn;
    }

    public String getStatus() {
        return status.name();
    }

    public void setSubmitted() {
        this.status = Status.SUBMITTED;
    }

    public void setCompleted() {
        this.status = Status.COMPLETED;
    }

    public void setFailed() {
        this.status = Status.FAILED;
    }

    public boolean isQueued() {
        return status == Status.QUEUED;
    }

    public boolean isSubmitted() {
        return status == Status.SUBMITTED;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isAborted() {
        return status == Status.ABORTED;
    }

    public void setAborted() {
        this.status = Status.ABORTED;
    }

    private enum Status {
        QUEUED, SUBMITTED, COMPLETED, FAILED, ABORTED
    }

    @Embeddable
    public static class TrialId implements Serializable {

        private Experiment experiment;
        private int trialNumber;

        public Experiment getExperiment() {
            return experiment;
        }

        public void setExperiment(Experiment experiment) {
            this.experiment = experiment;
        }

        public int getTrialNumber() {
            return trialNumber;
        }

        public void setTrialNumber(int trialNumber) {
            this.trialNumber = trialNumber;
        }
    }

}
