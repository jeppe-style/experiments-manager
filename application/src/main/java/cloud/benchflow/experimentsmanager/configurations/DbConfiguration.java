package cloud.benchflow.experimentsmanager.configurations;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 29/01/16.
 */
public class DbConfiguration {

    private String address;
    private String user;
    private String name;
    private String password;
    private String hibernateConfig; // relative path to hibernate.cfg.xml
    private String createExperimentsTableQueryPath; // relative path to create-experiments-table.sql
    private String createTrialsTableQueryPath; // relative path to create-trials-table.sql

    public String getName() {
        return name;
    }

    public void setName(String dbName) {
        this.name = dbName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHibernateConfig() {
        return hibernateConfig;
    }

    public void setHibernateConfig(String hibernateConfig) {
        this.hibernateConfig = hibernateConfig;
    }

    public String getCreateExperimentsTableQueryPath() {
        return createExperimentsTableQueryPath;
    }

    public void setCreateExperimentsTableQueryPath(String createExperimentsTableQueryPath) {
        this.createExperimentsTableQueryPath = createExperimentsTableQueryPath;
    }

    public String getCreateTrialsTableQueryPath() {
        return createTrialsTableQueryPath;
    }

    public void setCreateTrialsTableQueryPath(String createTrialsTableQueryPath) {
        this.createTrialsTableQueryPath = createTrialsTableQueryPath;
    }
}
