package au.com.gaiaresources.bdrs.service.taxonomy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import au.com.gaiaresources.taxonlib.TaxonLib;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;

public class TaxonLibSessionFactory {

    private static Logger log = Logger.getLogger(TaxonLibSessionFactory.class);
    
    public static ITaxonLibSession getSession(String url, String username, String password) throws Exception {
        return TaxonLib.openSession(getConnection(url, username, password));
    }

    private static Connection getConnection(String url, String username, String password) throws Exception {

        // hardcode connection for now...
        String DB_CONN_STRING = "jdbc:postgresql://" + url;
        String DRIVER_CLASS_NAME = "org.postgresql.Driver";
        String USER_NAME = username;
        String PASSWORD = password;

        Connection result = null;
        try {
            Class.forName(DRIVER_CLASS_NAME).newInstance();
        } catch (Exception ex) {
            log.error("Check classpath. Cannot load db driver: " + DRIVER_CLASS_NAME);
            throw ex;
        }
        try {
            result = DriverManager.getConnection(DB_CONN_STRING, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            log.error("Driver loaded, but cannot connect to db: " + DB_CONN_STRING);
            throw e;
        }
        return result;
    }
}