package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.commons.lang.ArrayUtils;


import java.util.List;
import java.util.Map;

/**
 * The <code>VisibilityFacet</code> filters records based on their visibility.
 */
public class VisibilityFacet extends AbstractFacet {
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "visibility";

    /**
     * The configuration attribute (specified in the Preferences) that specifies the user roles that enable this facet.
     */
    public static final String JSON_ENABLE_FOR_ROLES_KEY = "enableForRoles";

    /**
     * Creates a new instance.
     *
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param facetConfiguration user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public VisibilityFacet(String defaultDisplayName, RecordDAO recordDAO, Map<String, String[]> parameterMap, User user, JSONObject facetConfiguration) {
        super(QUERY_PARAM_NAME, defaultDisplayName, facetConfiguration);
        if (!checkUserRole(user, facetConfiguration))   {
            setActive(false);
            return;
        }
        setContainsSelected(parameterMap.containsKey(getInputName()));
        
        String[] selectedOptions = parameterMap.get(getInputName());
        if(selectedOptions == null) {
            selectedOptions = new String[0];
        }

        List<Pair<RecordVisibility, Long>> visibilities = recordDAO.getDistinctRecordVisibilities();

        for (Pair<RecordVisibility, Long> visibilityPair : visibilities) {

            boolean selected = ArrayUtils.contains(selectedOptions, Integer.toString(visibilityPair.getFirst().ordinal()));
            super.addFacetOption(new VisibilityFacetOption(visibilityPair.getFirst(), visibilityPair.getSecond(), selected));
        }

    }

    /**
     * Checks if the current User has a role that has been specified in the facet configuration.  If no
     * role configuration has been made for the visibility facet by default it will be enabled only for admins.
     * @param user the currently logged in user.
     * @param facetConfiguration the configuration specified for this facet.
     * @return true if this facet is being viewed by someone with a configured role.
     */
    private boolean checkUserRole(User user, JSONObject facetConfiguration) {
        // Don't display for anonymous users, there is no point as they can only see PUBLIC records anyway.
        if (user == null) {
            return false;
        }
        String enabledForRoles = facetConfiguration.optString(JSON_ENABLE_FOR_ROLES_KEY);
        if (StringUtils.nullOrEmpty(enabledForRoles)) {
            // If the per-role visibility attribute has not been set, default to only displaying this
            // facet to administrators.
            return user.isAdmin();
        }

        String[] rolesArray = enabledForRoles.split(",");
        String[] roles = user.getRoles();
        for (String role : roles) {
            for (String enabledRole : rolesArray) {
                if (role.equals(enabledRole.trim())) {
                    return true;
                }
            }
        }
        return false;
        
    }
}
