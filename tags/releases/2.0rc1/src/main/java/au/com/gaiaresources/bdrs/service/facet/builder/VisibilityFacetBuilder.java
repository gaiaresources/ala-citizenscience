package au.com.gaiaresources.bdrs.service.facet.builder;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.VisibilityFacet;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The VisibilityFacetBuilder is responsible for creating instances of the {@link au.com.gaiaresources.bdrs.service.facet.VisibilityFacet}.
 * It provides sensible defaults for the facet name, description and configuration.
 */
public class VisibilityFacetBuilder extends AbstractFacetBuilder<VisibilityFacet> {

    /**
     * Describes the function of this facet that will be used in the preference description.
     */
    public static final String FACET_DESCRIPTION = "Restricts records based on the records visibility.";

    /**
     * The human readable name of this facet.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Visibility";

    /**
     *  Help text for the JSON_ENABLE_FOR_ROLES configuration parameter
     */
    public static final String ENABLE_FOR_ROLES_CONFIG_DESCRIPTION = "<dd><code>"+VisibilityFacet.JSON_ENABLE_FOR_ROLES_KEY+
            "</code> - comma separated list of roles for which this facet is displayed.  Valid values are: "+
            "ROLE_USER, ROLE_POWER_USER, ROLE_SUPERVISOR and ROLE_ADMIN.  Default=ROLE_ADMIN</dd>";


    /**
     * Creates a new instance of the VisibilityFacetBuilder.
     */
    public VisibilityFacetBuilder() {
        super(VisibilityFacet.class);
    }

    /**
     * Overrides the default behaviour to add a description of the enableForRoles parameter.
     * @return description text for this facet.
     */
    @Override
    public String getPreferenceDescription() {
        List<String> params = getFacetParameterDescription();
        params.add(ENABLE_FOR_ROLES_CONFIG_DESCRIPTION);
        return buildPreferenceDescription(FACET_DESCRIPTION, params);
    }

    @Override
    public Facet createFacet(FacetDAO recordDAO,
            Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        
        return new VisibilityFacet(DEFAULT_DISPLAY_NAME, recordDAO, parameterMap, user, userParams);
    }

    @Override
    public String getDefaultDisplayName() {
        return DEFAULT_DISPLAY_NAME;
    }


    /**
     * Overrides the parent class method to add sensible defaults for the enableForRoles parameter.
     * @return a JSONArray containing the preferences for this Facet.
     */
    @Override
    protected JSONArray createDefaultPreferenceObject() {
        JSONArray preference = super.createDefaultPreferenceObject();
        for (Object preferenceObject : preference) {
            // Defaults to admin only.
            ((JSONObject)preferenceObject).put(VisibilityFacet.JSON_ENABLE_FOR_ROLES_KEY, Role.ADMIN);
        }

        return preference;
    }
}