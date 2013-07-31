package au.com.gaiaresources.bdrs.service.facet.location;

import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;

/**
 * A Facet that restricts locations by survey.
 * 
 * @author stephanie
 *
 */
public class LocationSurveyFacet extends SurveyFacet {

    public LocationSurveyFacet(String defaultDisplayName,
            FacetDAO transDAO, Map<String, String[]> parameterMap,
            User user, JSONObject userParams) {
        super(defaultDisplayName, transDAO, parameterMap, user, userParams);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.SurveyFacet#getSurveyFacetOption(au.com.gaiaresources.bdrs.model.survey.Survey, java.lang.Long, java.lang.String[])
     */
    @Override
    protected FacetOption getSurveyFacetOption(Survey survey, Long count,
            String[] selectedOptions) {
        return new LocationSurveyFacetOption(survey, count, selectedOptions);
    }

}
