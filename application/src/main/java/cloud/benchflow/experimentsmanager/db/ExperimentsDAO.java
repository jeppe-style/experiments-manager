package cloud.benchflow.experimentsmanager.db;

import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.db.entities.Trial;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 29/01/16.
 */
public class ExperimentsDAO implements AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(ExperimentsDAO.class.getName());
    private SessionFactory sessionFactory;

    public ExperimentsDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void saveExperiment(Experiment e) {
        ExperimentsDAOCommand<Void> cmd = new ExperimentsDAOCommand<>((sf) -> {
            sessionFactory.getCurrentSession().save(e);
        }, "saveExperiment");

        loggedTransaction(cmd);
    }

    private <T> T loggedTransaction(ExperimentsDAOCommand<T> command) {
        Transaction tx = null;

        try {

            tx = sessionFactory.getCurrentSession().beginTransaction();
            T result = command.execute(sessionFactory);
            tx.commit();
            return result;

        } catch (RuntimeException e) {
            try {
                assert tx != null;
                tx.rollback();
            } catch (RuntimeException rbEx) {
                logger.error("Couldn’t roll back transaction: " + command.getQueryDescription(), rbEx);
            }
            throw e;
        }

    }

    public String getFabanRunId(String username, String benchmarkName, long experimentNumber, int trialNumber) {

        ExperimentsDAOCommand<String> cmd = new ExperimentsDAOCommand<>((sf) -> {

            String query = "select t.fabanRunId from Trial t where " +
                    "t.experiment.username = :uname and " +
                    "t.experiment.benchmarkName = :bname and " +
                    "t.experiment.experimentNumber = :expNum and " +
                    "t.trialNumber = :trialNum";

            return (String) sessionFactory.getCurrentSession().createQuery(query)
                    .setParameter("uname", username)
                    .setParameter("bname", benchmarkName)
                    .setParameter("expNum", experimentNumber)
                    .setParameter("trialNum", trialNumber)
                    .uniqueResult();

        }, "getFabanRunId");

        return loggedTransaction(cmd);
    }

    public void update(Object row) {

        ExperimentsDAOCommand<Void> cmd = new ExperimentsDAOCommand<>((sf) -> {
            sf.getCurrentSession().update(row);
        }, "update");

        loggedTransaction(cmd);
    }

    public Trial getTrial(String userId, String benchmarkName, long experimentNumber, int trialNumber) {

        ExperimentsDAOCommand<Trial> cmd = new ExperimentsDAOCommand<>((sf) -> {

            String query = "select t from Trial t where " +
                    "t.experiment.username = :uname and " +
                    "t.experiment.benchmarkName = :bname and " +
                    "t.experiment.experimentNumber = :expNum and " +
                    "t.trialNumber = :trialNum";

            return (Trial) sessionFactory.getCurrentSession()
                    .createQuery(query)
                    .setParameter("uname", userId)
                    .setParameter("bname", benchmarkName)
                    .setParameter("expNum", experimentNumber)
                    .setParameter("trialNum", trialNumber)
                    .uniqueResult();
        }, "getTrial");

        return loggedTransaction(cmd);
    }

    public Experiment getExperiment(String userId, String benchmarkName, long experimentNumber) {

        ExperimentsDAOCommand<Experiment> cmd = new ExperimentsDAOCommand<>((sf) -> {

            String query = "select e from Experiment e where " +
                    "e.username = :uname and " +
                    "e.benchmarkName = :bname and " +
                    "e.experimentNumber = :expNum";

            return (Experiment) sessionFactory.getCurrentSession()
                    .createQuery(query)
                    .setParameter("uname", userId)
                    .setParameter("bname", benchmarkName)
                    .setParameter("expNum", experimentNumber)
                    .uniqueResult();
        }, "getExperiment");

        return loggedTransaction(cmd);
    }

    public void cleanUp(Experiment e) {

        ExperimentsDAOCommand<Void> cmd = new ExperimentsDAOCommand<>((sf) -> {
            sf.getCurrentSession().delete(e);
        }, "cleanUp");

        loggedTransaction(cmd);
    }

    @Override
    public void close() {
        sessionFactory.getCurrentSession().close();
    }

    private static class ExperimentsDAOCommand<T> {

        private Function<SessionFactory, T> function;
        private Consumer<SessionFactory> consumer;

        private String queryDescription;

        public ExperimentsDAOCommand(Function<SessionFactory, T> command,
                                     String queryDescription) {
            this.function = command;
            this.queryDescription = queryDescription;
        }

        public ExperimentsDAOCommand(Consumer<SessionFactory> command,
                                     String queryDescription) {
            this.consumer = command;
            this.queryDescription = queryDescription;
        }

        public T execute(SessionFactory sf) {
            //execute either the function, or the consumer (returning null)
            if (function != null)
                return function.apply(sf);
            else {
                consumer.accept(sf);
                return null; //ugly, because java
            }

        }

        public String getQueryDescription() {
            return queryDescription;
        }

    }
}