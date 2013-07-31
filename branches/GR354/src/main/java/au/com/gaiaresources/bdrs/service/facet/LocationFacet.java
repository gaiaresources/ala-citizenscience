package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.LocationFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Creates a {@link Facet} for showing records by location.  This will allow only 
 * records for a given location to be shown.  
 * @author stephanie
 */
public class LocationFacet extends AbstractFacet {
    private Logger log = Logger.getLogger(getClass());
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "l";

    /**
     * Creates a Location Facet.
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference )}.
     */
    public LocationFacet(String defaultDisplayName, FacetDAO recordDAO, Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);

        String[] selectedOptions = processParameters(parameterMap);

        Integer[] selectedIds = new Integer[selectedOptions.length];
        for (int i = 0; i < selectedOptions.length; i++) {
            selectedIds[i] = Integer.valueOf(selectedOptions[i]);
        }

        final int NO_LIMIT = 0;
        for(Pair<Location, Long> pair : recordDAO.getDistinctLocations(null, NO_LIMIT, selectedIds)) {
            super.addFacetOption(new LocationFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
        }
    }

}
