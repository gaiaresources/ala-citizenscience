package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.record.impl.CountRecordFilter;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.ModerationFacetOption;

import java.util.Map;

/**
 * Restricts records by moderation status.
 * @author stephanie
 */
public class ModerationFacet extends AbstractFacet {
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "h";

    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public ModerationFacet(String defaultDisplayName, FacetDAO recordDAO, Map<String, String[]> parameterMap, User user, JSONObject userParams) {
          super(QUERY_PARAM_NAME, defaultDisplayName, userParams);
          
          if (user == null || !user.isModerator()) {
              setActive(false);
          }
          
          String[] selectedOptions = processParameters(parameterMap);
          
          RecordFilter filter = new CountRecordFilter();
          filter.setAccessor(user);
          filter.setHeld(true);
          int count = recordDAO.countRecords(filter);
          super.addFacetOption(new ModerationFacetOption(QUERY_PARAM_NAME, Long.valueOf(count), selectedOptions.length > 0));
    }
}
