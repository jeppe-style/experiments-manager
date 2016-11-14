package cloud.benchflow.experimentsmanager.db;

import cloud.benchflow.experimentsmanager.configurations.DbConfiguration;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *
 * Created on 07/03/16.
 */
public class DbManager {

    private SessionFactory sessionFactory;

    public DbManager(final DbConfiguration dbConfig) {

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();

        System.out.println("jdbc:mysql://" + dbConfig.getAddress() + "/" + dbConfig.getName() + "?createDatabaseIfNotExist=true");

        builder.configure(new File(dbConfig.getHibernateConfig()));
//                .applySetting("hibernate.connection.url", "jdbc:mysql://" + url +
//                              "/" + dbName + "?createDatabaseIfNotExist=true")
//                .applySetting("hibernate.connection.username", username)
//                .applySetting("hibernate.connection.password", password);

        // TODO - replace line 30 with this
        System.out.println(builder.getAggregatedCfgXml().getConfigurationValues().get("connection.url"));

        final StandardServiceRegistry registry = builder.build();
        try {

            this.sessionFactory = new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();

            checkDatabaseSchema(dbConfig);

        }
        catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw new RuntimeException("Encountered a problem connecting to database " + dbConfig.getName(), e);
        }
    }

    /***
     * Checks existence of expected tables, and creates them if they
     * don't exist
     */
    private void checkDatabaseSchema(final DbConfiguration dbConfig) throws IOException {

        Session session = sessionFactory.openSession();
        session.beginTransaction();

        Path createExperimentsTableQuery =
                Paths.get(dbConfig.getCreateExperimentsTableQueryPath());
        final String createExperimentsTable =
                FileUtils.readFileToString(createExperimentsTableQuery.toFile(), Charsets.UTF_8);

        Path createTrialsTableQuery =
                Paths.get(dbConfig.getCreateTrialsTableQueryPath());
        final String createTrialsTable =
                FileUtils.readFileToString(createTrialsTableQuery.toFile(), Charsets.UTF_8);

        session.createSQLQuery(createExperimentsTable).executeUpdate();

        session.createSQLQuery(createTrialsTable).executeUpdate();

        session.getTransaction().commit();
        session.close();
    }

    public ExperimentsDAO getExperimentsDAO() {
        return new ExperimentsDAO(this.sessionFactory);
    }

}
