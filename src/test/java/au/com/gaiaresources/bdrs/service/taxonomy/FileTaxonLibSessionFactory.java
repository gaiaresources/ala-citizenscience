package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import au.com.gaiaresources.taxonlib.ITaxonLibSession;

public class FileTaxonLibSessionFactory extends AbstractTaxonLibSessionFactory {

	private Logger log = Logger.getLogger(getClass());
	
	@Override
	public ITaxonLibSession getSession() throws Exception {
		Properties props = new Properties();
		
		InputStream propStream = null;
		try {
			propStream = getClass().getResourceAsStream("taxonlib.properties"); 
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
        return getSession(DB_CONN_STRING, USER_NAME, PASSWORD);
	}
}
