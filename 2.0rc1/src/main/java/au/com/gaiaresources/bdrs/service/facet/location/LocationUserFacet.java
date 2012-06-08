package au.com.gaiaresources.bdrs.service.facet.location;

import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.UserFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;

/**
 * A facet that allows filtering of Locations based on User.
 * 
 * @author stephanie
 *
 */
public class LocationUserFacet extends UserFacet {

    public LocationUserFacet(String defaultDisplayName, FacetDAO facetDAO,
            Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(defaultDisplayName, facetDAO.getDistinctUsers(null), parameterMap, user, userParams);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.UserFacet#getUserFacetOption(au.com.gaiaresources.bdrs.model.user.User, java.lang.Long, java.lang.String[])
     */
    @Override
    protected FacetOption getUserFacetOption(User user, Long count,
            String[] selectedOptions) {
        return new LocationUserFacetOption(user, count, selectedOptions);
    }

}
