package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;

/**
 * Factory for creating a TaxonLib session with parameters from a property file.
 */
public class FileTaxonLibSessionFactory extends AbstractTaxonLibSessionFactory {

	private Logger log = Logger.getLogger(getClass());
	
	@Override
	public ITaxonLibSession getSession() throws IllegalArgumentException, BdrsTaxonLibException, TaxonLibException {
		Properties props = new Properties();
		
		InputStream propStream = null;
		try {
			propStream = getClass().getResourceAsStream("taxonlib.properties"); 
			props.load(propStream);	
		} catch (Exception e) {
			throw new BdrsTaxonLibException("Could not load taxonlib properties", e);
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
		if (DB_CONN_STRING == null) {
			throw new BdrsTaxonLibException("Database url is missing from property file");
		}
		String USER_NAME = props.getProperty("db.username");
		if (USER_NAME == null) {
			throw new BdrsTaxonLibException("Database user name is missing from property file");
		}
		String PASSWORD = props.getProperty("db.password");
		if (PASSWORD == null) {
			throw new BdrsTaxonLibException("Database password is missing from property file");
		}
        return getSession(DB_CONN_STRING, USER_NAME, PASSWORD);
	}
}
