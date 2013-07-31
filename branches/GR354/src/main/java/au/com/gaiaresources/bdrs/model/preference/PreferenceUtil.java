package au.com.gaiaresources.bdrs.model.preference;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.util.StringUtils;

public class PreferenceUtil {
    
    private PreferenceDAO prefDAO;
    
    private Logger log = Logger.getLogger(getClass());
    
    public PreferenceUtil(PreferenceDAO prefDAO) {
        if (prefDAO == null) {
            throw new IllegalArgumentException("PreferenceDAO, prefDAO, cannot be null");
        }
        this.prefDAO = prefDAO;
    }
    
    public boolean getBooleanPreference(String key) {
        Preference pref = prefDAO.getPreferenceByKey(key);
        if (pref == null) {
            throw new NullPointerException("Get preference by key returned null : " + key);
        }
        return getBooleanValue(pref);
    }

    /**
     * Returns the value of the Preference object identified by the supplied key, treating it as a boolean.
     * @param session the Hibernate Session to use for the query.
     * @param key identifies the Preference of interest.
     * @param defaultValue the value to return if no Preference exists with the supplied key.
     * @return true if the value of the Preference identified by <code>key</code> is "True", <code>defaultValue</code>
     * if the Preference does not exist, false otherwise.
     */
    public boolean getBooleanPreference(Session session, String key, boolean defaultValue) {
        Preference pref = prefDAO.getPreferenceByKey(session, key);
        if (pref == null) {
            return defaultValue;
        }
        return getBooleanValue(pref);
    }

    /**
     * Returns the value of the supplied Preference object as a boolean.  Boolean.parseBoolean is used to convert
     * the value into a boolean.
     * @param pref the Preference we are treating as a boolean preference.
     * @return true if the value of the supplied Preference is "True".
     */
    private boolean getBooleanValue(Preference pref) {
        String value = pref.getValue();
        if (StringUtils.hasLength(value)) {
            return Boolean.parseBoolean(value);
        } else {
            return false;
        }
    }
}
