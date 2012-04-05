package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.taxonlib.ITaxonLibSession;

/**
 * Entry point for taxon lib functionality from BDRS.
 * Creates a TaxonLibSession with a database connection, ready for use by client classes.
 *
 */
public interface TaxonLibSessionFactory {
	/**
	 * Creates a new TaxonLibSession
	 * 
	 * @return a new session
	 * @throws Exception
	 */
    public abstract ITaxonLibSession getSession() throws Exception;
}
