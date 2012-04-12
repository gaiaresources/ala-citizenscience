package au.com.gaiaresources.bdrs.service.facet;

import java.util.Map;

import org.apache.log4j.Logger;

import edu.emory.mathcs.backport.java.util.Arrays;

import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.LocationFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.json.JSONObject;

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
    public static final String QUERY_PARAM_NAME = "location";
    
    /**
     * Limits the number of options to show in the facet.
     */
    public static final int OPTIONS_LIMIT = 10;
    
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
        
        setContainsSelected(parameterMap.containsKey(getInputName()));
        
        String[] selectedOptions = parameterMap.get(getInputName());
        Integer[] selectedIds = null;
        if(selectedOptions == null) {
            selectedOptions = new String[]{};
        } else if (selectedOptions.length > 0) {
            // for some reason, this doesn't work
            //selectedIds = Arrays.copyOf(selectedOptions, selectedOptions.length, Integer.class);
            // so doing it manually
            selectedIds = new Integer[selectedOptions.length];
            for (int i = 0; i < selectedOptions.length; i++) {
                selectedIds[i] = Integer.valueOf(selectedOptions[i]);
            }
        }
        Arrays.sort(selectedOptions);
        for(Pair<Location, Long> pair : recordDAO.getDistinctLocations(null, OPTIONS_LIMIT, (Integer[]) selectedIds)) {
            super.addFacetOption(new LocationFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
        }
    }

}
