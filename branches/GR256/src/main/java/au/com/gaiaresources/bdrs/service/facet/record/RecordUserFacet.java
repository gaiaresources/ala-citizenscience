package au.com.gaiaresources.bdrs.service.facet.record;

import java.util.Map;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.UserFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;

/**
 * The <code>RecordUserFacet</code> restricts records to the selected set of users. 
 */
public class RecordUserFacet extends UserFacet {

    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param transDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link Preference)}.
     */
    public RecordUserFacet(String defaultDisplayName, FacetDAO facetDAO,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(defaultDisplayName, facetDAO.getDistinctUsers(null), parameterMap, user, userParams);
    }

    @Override
    protected FacetOption getUserFacetOption(User user, Long count, String[] selectedOptions) {
        return new RecordUserFacetOption(user, count, selectedOptions);
    }
}
