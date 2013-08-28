package au.com.gaiaresources.bdrs.service.taxonomy;

import java.sql.SQLException;

import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;

/**
 * Entry point for taxon lib functionality from BDRS.
 * Creates a TaxonLibSession with a database connection, ready for use by client classes.
 * Typically the {@link RequestContext} contains a TaxonLibSessionFactory and also provides a taxonlib session. 
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
	 * @throws SQLException 
	 */
    public abstract ITaxonLibSession getSession() throws IllegalArgumentException, BdrsTaxonLibException, TaxonLibException, SQLException;
    

    /**
     * Get a TaxonLib session or if something goes wrong fail gracefully and return a null
     * @return {@link ITaxonLibSession} or null if something went wrong 
     */
    public abstract ITaxonLibSession getSessionOrNull();
}
