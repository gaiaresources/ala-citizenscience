package au.com.gaiaresources.bdrs.service.facet;

import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.util.Pair;
import edu.emory.mathcs.backport.java.util.Arrays;

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
     * @param userParams user configurable parameters provided in via the {@link Preference)}.
     */
    public UserFacet(String defaultDisplayName, List<Pair<User, Long>> list,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);
        setContainsSelected(parameterMap.containsKey(getInputName()));
        
        String[] selectedOptions = parameterMap.get(getInputName());
        if(selectedOptions == null) {
            selectedOptions = new String[0];
        }
        Arrays.sort(selectedOptions);
        
        int userCount = 0;
        for(Pair<User, Long> pair : list) {
            if (pair.getFirst().equals(user)) {
                super.insertFacetOption(getUserFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions), 0);
            } else if (userCount < OPTIONS_LIMIT) {
                super.addFacetOption(getUserFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
                userCount++;
            }
        }
        
        // if the user is not null (anonymous view) and
        // if the options are not the min of the limit or the count + 2 for my records and all public records
        // the user has no records and has not been added, so add an option for their 0 records now
        if (user != null && getFacetOptions().size() < Math.min(OPTIONS_LIMIT, userCount) + 1) {
            super.insertFacetOption(getUserFacetOption(user, Long.valueOf(0), selectedOptions), 0);
        }
    }

    protected abstract FacetOption getUserFacetOption(User first, Long second,
            String[] selectedOptions);
}
