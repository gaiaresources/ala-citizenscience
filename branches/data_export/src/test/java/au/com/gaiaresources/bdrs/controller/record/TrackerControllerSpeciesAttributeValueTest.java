package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Tests the new species attribute type via the tracker controller.
 * 
 * Some whitebox testing here - we know that all webforms use RecordDeserializer
 * which uses WebFormAttributeParser, TaxonValidator etc - we are assuming
 * that since it works via tracker controller the other webforms _should_ be ok
 */
public class TrackerControllerSpeciesAttributeValueTest extends
        AbstractGridControllerTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private MetadataDAO mdDAO;
    
    private Survey survey;
    private Attribute attr;
    
    @Before
    public void setup() {
        survey = new Survey();
        survey.setName("my survey");
        for (RecordPropertyType type : RecordPropertyType.values()) {
            RecordProperty p = new RecordProperty(survey, type, mdDAO);
            p.setRequired(false);
        }
        
        List<Attribute> attrList = new ArrayList<Attribute>();
        
        attr = new Attribute();
        attr.setName("speciesattr");
        attr.setDescription("species attribute");
        attr.setTypeCode(AttributeType.SPECIES.getCode());
        attr.setRequired(true);
        
        attrList.add(attr);
        
        taxaDAO.save(attr);
        
        survey.setAttributes(attrList);
        
        surveyDAO.save(survey);
    }
    
    @Test
    public void testSpeciesAttributeValueByName() throws Exception {
        
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(WebFormAttributeParser.getParamKey("", attr), this.dropBear.getScientificName());
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertEquals("wrong attribute value count", 1, record.getAttributes().size());
        
        AttributeValue av = (AttributeValue)record.getAttributes().toArray()[0];
        
        Assert.assertNotNull("species should not be null", av.getSpecies());
        
        Assert.assertEquals("wrong species id", av.getSpecies().getId(), this.dropBear.getId());
    }
    
	/**
	 * Tricky test where there are 2 elements in the post map but the ID field is whitespace
	 * @throws Exception
	 */
	@Test
	public void testSpeciesAttributeValueByName2() throws Exception {
		
		login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(WebFormAttributeParser.getParamKey("", attr), this.dropBear.getScientificName());
        request.addParameter(WebFormAttributeParser.getParamKey("", attr), "     ");
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertEquals("wrong attribute value count", 1, record.getAttributes().size());
        
        AttributeValue av = (AttributeValue)record.getAttributes().toArray()[0];
        
        Assert.assertNotNull("species should not be null", av.getSpecies());
        
        Assert.assertEquals("wrong species id", av.getSpecies().getId(), this.dropBear.getId());
    }
	
    @Test
    public void testSpeciesAttributeValueById() throws Exception {
        
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+TrackerController.EDIT_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        // We are using an incorrect name but adding the id as a second parameter.
        // The ID should override the bad name.
        request.setParameter(WebFormAttributeParser.getParamKey("", attr), "dummy name");
        request.addParameter(WebFormAttributeParser.getParamKey("", attr), this.dropBear.getId().toString());
        
        ModelAndView mav = handle(request, response);
        
        Map<String, String> errorMap = (Map<String, String>)mav.getModel().get(TrackerController.MV_ERROR_MAP);
        Assert.assertNull("error map should be null", errorMap);
        
        Integer recId = (Integer)mav.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        
        Record record = recordDAO.getRecord(recId);
        Assert.assertNotNull("record should not be null", record);
        
        Assert.assertEquals("wrong attribute value count", 1, record.getAttributes().size());
        
        AttributeValue av = (AttributeValue)record.getAttributes().toArray()[0];
        
        Assert.assertNotNull("species should not be null", av.getSpecies());
        
        Assert.assertEquals("wrong species id", av.getSpecies().getId(), this.dropBear.getId());
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
