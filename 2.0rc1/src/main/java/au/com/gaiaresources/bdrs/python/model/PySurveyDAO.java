package au.com.gaiaresources.bdrs.python.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;

/**
 * Represents a facade over the {@link SurveyDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PySurveyDAO extends AbstractPyDAO {

    private User user;
    private SurveyDAO surveyDAO;

    /**
     * Creates a new instance.
     * 
     * @param user the user accessing data.
     * @param surveyDAO retrieves survey related data.
     */
    public PySurveyDAO(User user, SurveyDAO surveyDAO) {
        this.user = user;
        this.surveyDAO = surveyDAO;
    }

    /**
     * Retrieves a list of active surveys for the accessing user.
     * @return a JSON encoded string representing all active surveys for the
     * accessing user.
     */
    public String getActiveSurveys() {
        return PyDAOUtil.toJSON(surveyDAO.getActiveSurveysForUser(this.user)).toString();
    }
    
    /**
     * Returns a JSON serialized survey with the specified primary key. 
     * 
     * @param surveyId the primary key of the survey to be returned.
     * @return a JSON serialized survey with the specified primary key. 
     */
    public String getSurveyById(int surveyId) {
        return getSurveyById(surveyId, false, false);
    }

    @Override
    public String getById(int pk) {
        return super.getById(surveyDAO, Survey.class, pk);
    }

    /**
     * Returns a JSON serialized survey with the specified primary key. 
     * 
     * @param surveyId the primary key of the survey to be returned.
     * @return a JSON serialized survey with the specified primary key. 
     */
    public String getSurveyById(int surveyId, boolean includeAttributes, boolean includeLocations) {
        return getSurveyById(surveyId, includeAttributes, includeLocations, false);
    }
    
    /**
     * Returns a JSON serialized survey with the specified primary key. 
     * 
     * @param surveyId the primary key of the survey to be returned.
     * @param includeAttributes true if {@link AttributeValue}s for the
     * returned locations should be included (as opposed to simple primary keys).
     * @return a JSON serialized survey with the specified primary key. 
     */
    public String getSurveyById(int surveyId, boolean includeAttributes, boolean includeLocations, boolean includeLocationAttributeValues) {
        Survey survey = surveyDAO.getSurvey(surveyId);
        Map<String, Object> flatSurvey = survey.flatten();
        
        if(includeAttributes) {
            List<Map<String, Object>> flatAttrList = new ArrayList<Map<String, Object>>();
            for(Attribute attr : survey.getAttributes()) {
                flatAttrList.add(attr.flatten());
            }
            
            flatSurvey.put("attributes", flatAttrList);
        }
        
        if(includeLocations) {
            List<Map<String, Object>> flatLocList = new ArrayList<Map<String, Object>>();
            for(Location loc : survey.getLocations()) {
                
                Map<String, Object> flatLoc = loc.flatten();
                if(includeLocationAttributeValues) {
                    List<Map<String, Object>> flatAttrValList = new ArrayList<Map<String, Object>>();
                    for(AttributeValue attrVal : loc.getAttributes()) {
                        flatAttrValList.add(attrVal.flatten());
                    }
                    
                    flatLoc.put("attributes", flatAttrValList);
                }
                flatLocList.add(flatLoc);
            }
            
            flatSurvey.put("locations", flatLocList);
        }
        
        return JSONObject.fromMapToString(flatSurvey);
    }
}
