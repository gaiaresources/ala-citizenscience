package au.com.gaiaresources.bdrs.service.taxonomy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.ibatis.executor.ExecutorException;
import org.apache.log4j.Logger;

import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLib;
import au.com.gaiaresources.taxonlib.TaxonLibException;

/**
 * Abstract factory for creating a TaxonLibSession
 *
 */
public abstract class AbstractTaxonLibSessionFactory implements TaxonLibSessionFactory {

    private Logger log = Logger.getLogger(getClass());
    private ITaxonLibSession currentSession = null;

    /**
     * Get a TaxonLib session
     * 
     * @param url URL of the taxonLib database.
     * @param username user name to log into the database.
     * @param password password to log into the database.
     * @return TaxonLib session.
     * @throws BdrsTaxonLibException 
     * @throws TaxonLibException Error creating the session.
     * @throws SQLException 
     * @throws Exception
     */
    protected ITaxonLibSession getSession(String url, String username, String password) throws SQLException, TaxonLibException, BdrsTaxonLibException {
        boolean create = false;
        if(currentSession == null){
            create = true;
        }else {
            try {
                if(currentSession.getConnection().isClosed()){
                    create = true;
                }
            } catch (ExecutorException e){
                // calling getConnection on a connection that is closed leads to an exception, but 
                // we need to be able to roll back transactions that require dropdatabase
                log.warn("Failed to get connection. Message was" + e.getMessage());
                create = true;
            }
        }
        if(create){
            currentSession = TaxonLib.openSession(getConnection(url, username, password));
        }
        return currentSession;
    }

    @Override
    public ITaxonLibSession getSessionOrNull() {
        try {
            return this.getSession();
        } catch (SQLException e) {
            log.info("Could not load the taxonLib library. This is ok if you don't have TaxonLib enabled for your portal" + 
                        "Error message was " + e.getMessage());
        }catch (TaxonLibException e) {
            log.info("Could not load the taxonLib library. This is ok if you don't have TaxonLib enabled for your portal" + 
                    "Error message was " + e.getMessage());
        }catch (BdrsTaxonLibException e) {
            log.info("Could not load the taxonLib library. This is ok if you don't have TaxonLib enabled for your portal" + 
                    "Error message was " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets a JDBC connection
     * 
     * @param url url of the database.
     * @param username username to log into database.
     * @param password password to log into database.
     * @return JDBC connection.
     * @throws BdrsTaxonLibException Error creating the connection.
     */
    private Connection getConnection(String url, String username, String password) throws BdrsTaxonLibException {

        // hardcode connection for now...
        String DB_CONN_STRING = "jdbc:postgresql://" + url;
        String DRIVER_CLASS_NAME = "org.postgresql.Driver";
        String USER_NAME = username;
        String PASSWORD = password;

        Connection result = null;
        try {
            Class.forName(DRIVER_CLASS_NAME).newInstance();
        } catch (Exception ex) {
            throw new BdrsTaxonLibException("Check classpath. Cannot load db driver: " + DRIVER_CLASS_NAME, ex);
        }
        try {
            result = DriverManager.getConnection(DB_CONN_STRING, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new BdrsTaxonLibException("Driver loaded, but cannot connect to db: " + DB_CONN_STRING, e);
        }
        return result;
    }   
}
