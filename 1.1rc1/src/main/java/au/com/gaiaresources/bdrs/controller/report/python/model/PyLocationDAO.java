/**
 * 
 */
package au.com.gaiaresources.bdrs.controller.report.python.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;

/**
 * Represents a facade over the {@link LocationDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyLocationDAO {

    private SurveyDAO surveyDAO;

    /**
     * Creates a new instance. 
     * @param surveyDAO retrieves survey related data.
     */
    public PyLocationDAO(SurveyDAO surveyDAO) {
        this.surveyDAO = surveyDAO;
    }
    
    /**
     * Returns a JSON serialized array of locations belonging to the survey 
     * with the specified primary key. 
     * 
     * @param id the primary key of the survey containing the locations 
     * to be returned.
     * @param includeAttributeValues true if {@link AttributeValue}s for the
     * returned locations should be included (as opposed to simple primary keys).
     * @return a JSON serialized array of locations belonging to the survey 
     * with the specified primary key. 
     */
    public String getLocationsForSurvey(int surveyId, boolean includeAttributeValues) {
        Survey survey = surveyDAO.getSurvey(surveyId);
        
        JSONArray jLocArray = new JSONArray();
        for(Location loc : survey.getLocations()) {
            Map<String, Object> flatLoc = loc.flatten();
            
            if(includeAttributeValues) {
                List<Map<String, Object>> flatAttrValList = new ArrayList<Map<String, Object>>();
                for(AttributeValue attrVal : loc.getAttributes()) {
                    flatAttrValList.add(attrVal.flatten());
                }
                
                flatLoc.put("attributes", flatAttrValList);
            }
            
            jLocArray.add(flatLoc);
        }
        
        return jLocArray.toString();
    }
}
