package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.MultimediaFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;

import java.util.Map;

/**
 * The <code>MultimediaFacet</code> restricts records depending if
 * it contains a non-empty file or image record attribute.
 */
public class MultimediaFacet extends AbstractFacet {
    
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "multimedia";

    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public MultimediaFacet(String defaultDisplayName, FacetDAO recordDAO,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);

        String[] selectedOptions = processParameters(parameterMap);
        
        for(Pair<String, Long> pair : recordDAO.getDistinctAttributeTypes(null, new AttributeType[]{AttributeType.FILE, AttributeType.IMAGE, AttributeType.AUDIO})) {
            super.addFacetOption(new MultimediaFacetOption(AttributeType.find(pair.getFirst(), AttributeType.values()), pair.getSecond(), selectedOptions));
        }
    }
}
