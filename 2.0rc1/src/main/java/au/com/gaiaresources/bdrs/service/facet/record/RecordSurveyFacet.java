package au.com.gaiaresources.bdrs.service.facet.record;

import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;

/**
 * Represents records on a per survey basis.
 */
public class RecordSurveyFacet extends SurveyFacet {
    /**
     * Creates a new instance.
     *
     * @param defaultDisplayName the default human readable name of this facet.
     * @param ((RecordDAO)transDAO) used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link Preference)}.
     */
    public RecordSurveyFacet(String defaultDisplayName, FacetDAO transDAO,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(defaultDisplayName, transDAO, parameterMap, user, userParams);
    }

    @Override
    protected FacetOption getSurveyFacetOption(Survey survey, Long count,
            String[] selectedOptions) {
        return new RecordSurveyFacetOption(survey, count, selectedOptions);
    }
}
