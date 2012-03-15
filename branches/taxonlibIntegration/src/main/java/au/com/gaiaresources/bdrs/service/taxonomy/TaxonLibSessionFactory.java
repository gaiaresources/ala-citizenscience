package au.com.gaiaresources.bdrs.service.taxonomy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import au.com.gaiaresources.taxonlib.TaxonLib;
import au.com.gaiaresources.taxonlib.TaxonLibSession;

public class TaxonLibSessionFactory {

    private static Logger log = Logger.getLogger(TaxonLibSessionFactory.class);
    
    public static TaxonLibSession getSession() throws Exception {
        return TaxonLib.openSession(getConnection());
    }

    private static Connection getConnection() throws Exception {

        // hardcode connection for now...
        String DB_CONN_STRING = "jdbc:postgresql://localhost:5432/taxonlib";
        String DRIVER_CLASS_NAME = "org.postgresql.Driver";
        String USER_NAME = "postgres";
        String PASSWORD = "postgres";

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
