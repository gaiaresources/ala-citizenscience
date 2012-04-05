package au.com.gaiaresources.bdrs.service.taxonomy;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;

public class PreferenceTaxonLibSessionFactory extends AbstractTaxonLibSessionFactory implements ApplicationContextAware {

	 /**
     * Preference key
     */
    private static final String TAXON_LIB_DB_URL_KEY = "taxonlib.database.url";
    /**
     * Preference key
     */
	private static final String TAXON_LIB_DB_USER_KEY = "taxonlib.database.username";
	/**
     * Preference key
     */
	private static final String TAXON_LIB_DB_PASS_KEY = "taxonlib.database.password";
	
	private ApplicationContext appContext;
	
	/**
	 * Gets a taxon lib session using database properties found in the preference table.
	 * Use this when providing a TaxonLibSession to client classes in the BDRS.
	 * 
	 * @param prefDAO PreferenceDAO for retrieving database details.
	 * @return The created TaxonLibSession.
	 * @throws Exception
	 */
	@Override
    public ITaxonLibSession getSession() throws Exception {
		PreferenceDAO prefDAO = appContext.getBean(PreferenceDAO.class);
		if (prefDAO == null) {
			throw new Exception("PreferenceDAO cannot be null");
		}
		
    	Preference urlPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_URL_KEY);
		Preference userPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_USER_KEY);
		Preference passPref = prefDAO.getPreferenceByKey(TAXON_LIB_DB_PASS_KEY);
		
		if (urlPref == null || userPref == null || passPref == null) {
			throw new Exception("taxonlib.badTaxonLibConfig");
		}
		if (!StringUtils.hasLength(urlPref.getValue().trim()) || !StringUtils.hasLength(userPref.getValue().trim()) 
				|| !StringUtils.hasLength(passPref.getValue().trim())) {
			throw new Exception("taxonlib.badTaxonLibConfig");
		}
		// looks ok, lets go!
		
		String url = urlPref.getValue().trim();
		String username = userPref.getValue().trim();
		String password = userPref.getValue().trim();
		
		return getSession(url, username, password);
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		appContext = applicationContext;
	}
}
