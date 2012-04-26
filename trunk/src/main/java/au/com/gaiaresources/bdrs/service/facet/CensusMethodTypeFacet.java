package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.CensusMethodTypeFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;

import java.util.Map;

/**
 * The <code>CensusMethodTypeFacet</code> restricts records to the type of the
 * associated census method or a null census method. 
 */
public class CensusMethodTypeFacet extends AbstractFacet {
    
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "censusMethod";

    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link Preference)}.
     */
    public CensusMethodTypeFacet(String defaultDisplayName, FacetDAO recordDAO,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);

        String[] selectedOptions = processParameters(parameterMap);
        
        // Special entry for null census methods. (Observation Type)
        Long count = Long.valueOf(recordDAO.countNullCensusMethodRecords());
        super.addFacetOption(new CensusMethodTypeFacetOption(count, selectedOptions));
        
        // All other situations
        for(Pair<String, Long> pair : recordDAO.getDistinctCensusMethodTypes(null)) {
            super.addFacetOption(new CensusMethodTypeFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
        }
    }
}
