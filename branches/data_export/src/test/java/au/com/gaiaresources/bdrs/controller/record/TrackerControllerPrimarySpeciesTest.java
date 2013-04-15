package au.com.gaiaresources.bdrs.controller.record;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class TrackerControllerPrimarySpeciesTest extends AbstractGridControllerTest {

	@Autowired
	private SurveyDAO surveyDAO;
	@Autowired
	private MetadataDAO mdDAO;
	
	private Survey survey;
	
	@Before
	public void setup() {
		survey = new Survey();
		survey.setName("my survey");
		for (RecordPropertyType type : RecordPropertyType.values()) {
			RecordProperty p = new RecordProperty(survey, type, mdDAO);
			p.setRequired(false);
		}
		
		surveyDAO.save(survey);
	}
	
	@Test
	public void testPrimarySpeciesAttributeValueByName() throws Exception {
		
		login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(TrackerController.PARAM_SPECIES_NAME, this.dropBear.getScientificName());
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        
        if (errorMap != null) {
        	for (Entry<String, String> e : errorMap.entrySet()) {
        		log.debug(e.getKey() + " : " + e.getValue());
        	}
        }
        
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertNotNull("record species should not be null", record.getSpecies());
        Assert.assertEquals("wrong species id", dropBear.getId(), record.getSpecies().getId());
    }
	
	@Test
	public void testPrimarySpeciesAttributeValueByNameBlankId() throws Exception {
		
		login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(TrackerController.PARAM_SPECIES_NAME, this.dropBear.getScientificName());
        request.addParameter(TrackerController.PARAM_SPECIES_ID, " ");
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertNotNull("record species should not be null", record.getSpecies());
        Assert.assertEquals("wrong species id", dropBear.getId(), record.getSpecies().getId());
    }
	
	@Test
	public void testPrimarySpeciesAttributeValueById() throws Exception {
		
		login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        // We are using an incorrect name but adding the id as a second parameter.
        // The ID should override the bad name.
        request.setParameter(TrackerController.PARAM_SPECIES_NAME, "dummy name");
        request.addParameter(TrackerController.PARAM_SPECIES_ID, this.dropBear.getId().toString());
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertNotNull("record species should not be null", record.getSpecies());
        Assert.assertEquals("wrong species id", dropBear.getId(), record.getSpecies().getId());
    }
	
	@Test
	public void testPrimarySpeciesBadName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        // We are using an incorrect name but adding the id as a second parameter.
        // The ID should override the bad name.
        request.setParameter(TrackerController.PARAM_SPECIES_NAME, "dummy name");
                
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        Assert.assertNotNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNull("rec id should not be null", recId);
	}
	
	
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
