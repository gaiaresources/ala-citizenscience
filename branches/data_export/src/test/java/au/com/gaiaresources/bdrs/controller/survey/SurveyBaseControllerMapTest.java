package au.com.gaiaresources.bdrs.controller.survey;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.map.AbstractEditMapControllerTest;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class SurveyBaseControllerMapTest extends AbstractEditMapControllerTest {

    @Autowired
    private GeoMapDAO geoMapDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    
    private Survey survey1;
    
	/**
     * Test that the survey map editing interface can be correctly opened.
     * @throws Exception
     */
    @Test
    public void testEditMap() throws Exception {
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(survey1.getId()));
        ModelAndView mv = this.testEditMap("/bdrs/admin/survey/editMap.htm");
        ModelAndViewAssert.assertViewName(mv, SurveyBaseController.EDIT_MAP_VIEW_NAME);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
    }
    
    /**
     * Test that survey map settings can be saved and that they show on the record entry form.
     * @throws Exception
     */
    @Test
    public void testSaveMapSettings() throws Exception {
    	request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(survey1.getId()));
    	request.setParameter(SurveyBaseController.PARAM_SAVE_AND_CONTINUE, "true");
    	ModelAndView mv = this.testSaveMapSettings("/bdrs/admin/survey/editMap.htm");
    	assertRedirect(mv, "/bdrs/admin/survey/locationListing.htm");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "surveyId");
    }

	@Override
	protected GeoMap createMap() {
		// we need a survey so create that as well.
    	survey1 = new Survey();
    	survey1.setName("survey 1");
    	survey1.setDescription("survey 1 description");
    	surveyDAO.save(survey1);
    	
    	GeoMap geoMap = new GeoMap();
        geoMap.setOwner(MapOwner.SURVEY);
        geoMap.setSurvey(survey1);
        return geoMapDAO.save(geoMap);
	}

	@Override
	protected GeoMap getMap() {
		return geoMapDAO.getForSurvey(null, survey1);
	}
}
