package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TestUtils;

public abstract class TaxonomyImportTest extends AbstractTransactionalTest {

    protected ITaxonLibSession taxonLibSession;
    
    private Logger log = Logger.getLogger(TaxonomyImportTest.class);

    @Before
    public void taxonomyImportTestSetup() throws Exception {
    	
    	Properties props = new Properties();
		
		InputStream propStream = null;
		try {
			propStream = TaxonomyImportTest.class.getResourceAsStream("taxonlib.properties"); 
			props.load(propStream);	
		} catch (Exception e) {
			log.error("Could not load taxonlib properties", e);
			throw e;
		} finally {
			if (propStream != null) {
				try {
					propStream.close();
				} catch (IOException ioe) {
					log.error("Failed to close stream", ioe);
				}
			}
		}
		String DB_CONN_STRING = props.getProperty("db.url");
		String USER_NAME = props.getProperty("db.username");
		String PASSWORD = props.getProperty("db.password");
        taxonLibSession = TaxonLibSessionFactory.getSession(DB_CONN_STRING, USER_NAME, PASSWORD);
    }

    @After
    public void taxonomyImportTestTeardown() throws SQLException, IOException {
    	
    	if (dropDatabase) {
			InputStream sqlStream = null;
			try {
				sqlStream = ITaxonLibSession.class.getResourceAsStream("taxonlib.sql");
				importSQL(taxonLibSession.getConnection(), sqlStream);
			} finally {
				if (sqlStream != null) {
					sqlStream.close();
				}
			}
			taxonLibSession.commit();
		} else {
			taxonLibSession.rollback();
		}
    	taxonLibSession.close();
    }

    protected Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }
    
    private boolean dropDatabase = false;
    
    /**
     * Drops the taxonlib AND the bdrs database
     */
    protected void requestTaxonomyImportTestDropDatabase() {
    	dropDatabase = true;
    	requestDropDatabase();
    }
    
    private static void importSQL(Connection conn, InputStream in)
			throws SQLException {
		Scanner s = new Scanner(in);
		s.useDelimiter("(;(\r)?\n)|(--\n)");
		Statement st = null;
		try {
			st = conn.createStatement();
			while (s.hasNext()) {
				String line = s.next();
				if (line.startsWith("/*!") && line.endsWith("*/")) {
					int i = line.indexOf(' ');
					line = line
							.substring(i + 1, line.length() - " */".length());
				}

				if (line.trim().length() > 0) {
					st.execute(line);
				}
			}
		} finally {
			if (st != null)
				st.close();
		}
	}
}
