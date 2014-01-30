package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.option.WithinAreaFacetOption;
import au.com.gaiaresources.bdrs.util.StringUtils;

import java.util.Map;

/**
 * Creates a {@link Facet} to search records over an area drawn on the map.
 * This facet is not visible on the UI.
 * User: serge
 * Creation Date: 20/12/13
 * Time: 3:28 PM
 */
public class WithinAreaFacet extends AbstractFacet {

    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "within";

    public WithinAreaFacet(String defaultDisplayName, Map<String, String[]> parameterMap, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);
        String[] selectedOptions = processParameters(parameterMap);
        FacetOption option;
        if (selectedOptions.length > 0 && StringUtils.notEmpty(selectedOptions[0])) {
            option = new WithinAreaFacetOption("Drawn Area", selectedOptions[0]);
            option.setSelected(true);
        } else {
            option = WithinAreaFacetOption.NO_AREA;
        }
        super.addFacetOption(option);

    }
}
