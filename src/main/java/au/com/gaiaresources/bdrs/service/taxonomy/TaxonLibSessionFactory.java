package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;

/**
 * Entry point for taxon lib functionality from BDRS.
 * Creates a TaxonLibSession with a database connection, ready for use by client classes.
 *
 */
public interface TaxonLibSessionFactory {
	
	/**
	 * Get a TaxonLib session.
	 * 
	 * @return A TaxonLib session.
	 * @throws IllegalArgumentException Error in one of the arguments for connecting to TaxonLib database.
	 * @throws BdrsTaxonLibException Error initialising TaxonLib session.
	 * @throws TaxonLibException Error initialising TaxonLib session.
	 */
    public abstract ITaxonLibSession getSession() throws IllegalArgumentException, BdrsTaxonLibException, TaxonLibException;
}
