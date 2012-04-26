package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.option.SurveyFacetOption;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents records on a per survey basis.
 */
public abstract class SurveyFacet extends AbstractFacet {
    public static final String SURVEY_ID_QUERY_PARAM_NAME = BdrsWebConstants.PARAM_SURVEY_ID;
    
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "survey";

    /**
     * Creates a new instance.
     *
     * @param defaultDisplayName the default human readable name of this facet.
     * @param facetDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public SurveyFacet(String defaultDisplayName, FacetDAO facetDAO,  Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);

        if(parameterMap.get(SURVEY_ID_QUERY_PARAM_NAME) != null && 
            parameterMap.get(SURVEY_ID_QUERY_PARAM_NAME).length == 1) {
            
            try {
                Integer.parseInt(parameterMap.get(SURVEY_ID_QUERY_PARAM_NAME)[0]);
                setActive(false);
            } catch(NumberFormatException nfe) {
                setActive(true);
            }
        }
        
        if(isActive()) {
            String[] selectedOptions = processParameters(parameterMap);
            
            for(Pair<Survey, Long> pair : facetDAO.getDistinctSurveys(null)) {
                super.addFacetOption(getSurveyFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions));
            }
        }
    }
    
    protected abstract FacetOption getSurveyFacetOption(Survey first, Long second,
            String[] selectedOptions);

    /**
     * 
     * @return A list of surveys that have been selected in the facet
     */
    public List<Survey> getSelectedSurveys() {
        List<Survey> result = new ArrayList<Survey>();
        for (FacetOption facetOpt : getFacetOptions()) {
            // we know the internal implementation must be a SurveyFacetOption
            if (facetOpt instanceof SurveyFacetOption) {
                SurveyFacetOption sfo = (SurveyFacetOption)facetOpt;
                if (sfo.isSelected()) {
                    result.add(sfo.getSurvey());
                }
            }
        }    
        return result;
    }
}
