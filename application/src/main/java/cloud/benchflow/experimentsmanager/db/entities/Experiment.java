package cloud.benchflow.experimentsmanager.db.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         <p>
 *         Created on 03/02/16.
 */
@Entity
@Table(name = "EXPERIMENTS")
@IdClass(Experiment.ExperimentId.class)
public class Experiment {

    @Id
    @Column(name = "USERNAME")
    private String username;
    @Id
    @Column(name = "EXPERIMENT_NAME")
    private String experimentName;
    @Id
    @Column(name = "EXPERIMENT_NUMBER")
    @GenericGenerator(name = "expNumberGenerator",
            strategy = "cloud.benchflow.experimentsmanager.db.generators.ExperimentNumberGenerator")
    @GeneratedValue(generator = "expNumberGenerator")
    private long experimentNumber;
    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL)
    private Set<Trial> trials = new HashSet<>();
    @Column(name = "PERFORMED_ON")
    private LocalDateTime performedOn;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private Status status;

    Experiment() {
    }

    public Experiment(String user, String experimentName) {
        this.experimentName = experimentName;
        this.performedOn = LocalDateTime.now();
        this.username = user;
        this.status = Status.GENERATING;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

//    @Formula("concat(USERNAME, '.', BENCHMARK_NAME, '.', EXP_NUMBER)")
//    private String experimentId;

    public long getExperimentNumber() {
        return experimentNumber;
    }

    public void setExperimentNumber(long experimentNumber) {
        this.experimentNumber = experimentNumber;
    }

    public void addTrial(Trial trial) {
        trials.add(trial);
        trial.setExperiment(this);
    }

    public Set<Trial> getTrials() {
        return trials;
    }

    public void setTrials(Set<Trial> trials) {
        this.trials = trials;
    }

    public LocalDateTime getPerformedOn() {
        return performedOn;
    }

    public void setPerformedOn(LocalDateTime performedOn) {
        this.performedOn = performedOn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExperimentId() {
        return username + "." + experimentName + "." + experimentNumber;
    }

    public String getTestId() {
        return username + "." + experimentName;
    }

    public String getStatus() {
        return status.name();
    }

    public void setQueued() {
        status = Status.QUEUED;
    }

    public void setRunning() {
        status = Status.RUNNING;
    }

    public void setCompleted() {
        status = Status.COMPLETED;
    }

    public boolean isQueued() {
        return status == Status.QUEUED;
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isAborted() {
        return status == Status.ABORTED;
    }

    public void setAborted() {
        status = Status.ABORTED;
    }

    public String getMinioExperimentId() {
        return (username + "." + experimentName).replace('.', '/');
    }

    /**
     * because Faban doesn't accept deployment of benchmarks with dots in the name
     *
     * @return
     */
    public String getFabanExperimentId() {
        return getExperimentId().replace('.', '_');
    }

    //TODO: maybe only RUNNING and COMPLETED are necessary?
    private enum Status {
        GENERATING, QUEUED, RUNNING, COMPLETED, ABORTED
    }

    @Embeddable
    public static class ExperimentId implements Serializable {

        private String username;
        private String experimentName;
        private long experimentNumber;

        public String getExperimentName() {
            return experimentName;
        }

        public void setExperimentName(String experimentName) {
            this.experimentName = experimentName;
        }

        public long getExperimentNumber() {
            return experimentNumber;
        }

        public void setExperimentNumber(long experimentNumber) {
            this.experimentNumber = experimentNumber;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

}