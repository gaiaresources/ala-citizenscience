package au.com.gaiaresources.bdrs.controller.record;

import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceUtil;

/**
 * Base controller for viewing/editing records.  Contains common code for 
 * determining view type.
 * 
 * @author stephanie
 *
 */
public class RecordController extends AbstractController {
    
    public static final String MAP_TAB = "map";
    public static final String TABLE_TAB = "table";
    public static final String DOWNLOAD_TAB = "download";
    public static final String DEFAULT_TAB = MAP_TAB;
    
    /** Required to get the default view to pass to the messages */
    @Autowired
    private PreferenceDAO preferenceDAO;
    /**
     * Returns the default tab (map or table) to display if it has not been specified in the request.
     * The default is determined by the value of the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     * @return the default tab to display (MAP_TAB or TABLE_TAB).
     */
    protected String defaultTab() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(preferenceDAO);
        boolean useMap = preferenceUtil.getBooleanPreference(Preference.MY_SIGHTINGS_DEFAULT_VIEW_KEY);
        return useMap ? MAP_TAB : TABLE_TAB;
    }
}
