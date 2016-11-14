package cloud.benchflow.experimentsmanager.db.generators;

import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 03/02/16.
 */
public class ExperimentNumberGenerator implements IdentifierGenerator {

    @Override
    public synchronized Serializable generate(SessionImplementor session, Object o) throws HibernateException {

        Connection c = session.connection();
        Experiment exp = (Experiment) o;
        String experimentName = exp.getExperimentName();
        String username = exp.getUsername();
        try {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT IFNULL(MAX(EXPERIMENT_NUMBER)+1, 1) as nextExpId FROM EXPERIMENTS WHERE USERNAME = '" + username +
                            "' AND EXPERIMENT_NAME = '" + experimentName + "'");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long count = rs.getLong("nextExpId");
                return count;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new HibernateException("Error while retrieving count of experiments for user " + username +
                    "and experiment " + experimentName);
        }
        return null;
    }
}
