package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * The <code>UserFacet</code> restricts records to the selected set of users. 
 */
public abstract class UserFacet extends AbstractFacet {
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "user";
    
    /**
     * A limit for the number of options to show in the facet.
     */
    private static final Integer OPTIONS_LIMIT = 8;
    
    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param list used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public UserFacet(String defaultDisplayName, List<Pair<User, Long>> list,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);

        String[] selectedOptions = processParameters(parameterMap);
        
        boolean foundUser = false;
        for(Pair<User, Long> pair : list) {
            if (pair.getFirst().equals(user)) {
                super.insertFacetOption(getUserFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions), 0);
                foundUser = true;
            } else {
                super.addFacetOption(getUserFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
            }
        }
        
        // if the user is not null (anonymous view) and
        // the user has no records and has not been added then add an option for their 0 records now
        if (user != null && !foundUser) {
            super.insertFacetOption(getUserFacetOption(user, Long.valueOf(0), selectedOptions), 0);
        }
    }

    protected abstract FacetOption getUserFacetOption(User first, Long second,
            String[] selectedOptions);
}
