package au.com.gaiaresources.bdrs.controller.record;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class TrackerControllerOrphanAttributeSaveTest extends
        AbstractControllerTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private AttributeDAO attrDAO;
    @Autowired
    private AttributeValueDAO avDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private MetadataDAO mdDAO;
    
    private Survey survey;
    private Record rec;
    private Attribute attr;
    private AttributeValue av;
    private User user;
    
    @Before
    public void setup() {
        user = userDAO.getUser("admin");
        
        survey = new Survey();
        survey.setName("survey name");
        survey.setDescription("survey desc");
        
        survey = surveyDAO.save(survey);
        // make everything not required for easier setup.
        for (RecordPropertyType rpt : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(survey, rpt, mdDAO);
            rp.setRequired(false);
        }
        
        attr = new Attribute();
        attr.setName("attr");
        attr.setDescription("attr desc");
        attr.setTypeCode(AttributeType.STRING.getCode());
        attr = attrDAO.save(attr);
        
        av = new AttributeValue();
        av.setAttribute(attr);
        av.setStringValue("hello");
        av = avDAO.save(av);
        
        Set<AttributeValue> avSet = new HashSet<AttributeValue>();
        avSet.add(av);
        
        rec = new Record();
        rec.setSurvey(survey);
        rec.setUser(user);
        rec.setAttributes(avSet);
        rec = recordDAO.save(rec);
    }
    
    @Test
    public void testOrphanAttributeValueDisplayed() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.GET.toString());
        request.setParameter(TrackerController.PARAM_RECORD_ID, rec.getId().toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        RecordWebFormContext webFormContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        
        // make sure the orphaned attribute value appears ok.
        Assert.assertEquals("wrong size", 1, webFormContext.getNamedFormFields().get("orphanFormFieldList").size());
    }
    
    @Test
    public void testSaveOrphanAttributeValue() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_RECORD_ID, rec.getId().toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        String paramName = WebFormAttributeParser.getParamKey(AttributeParser.DEFAULT_PREFIX, attr);
        request.setParameter(paramName, "goodbye");
        
        this.handle(request, response);
        
        recordDAO.refresh(rec);
        
        Assert.assertEquals("wrong size", 1, rec.getAttributes().size());
        AttributeValue avUnderTest = rec.getAttributes().iterator().next();
        Assert.assertEquals("wrong attr id", attr.getId(), avUnderTest.getAttribute().getId());
        Assert.assertEquals("wrong attr value id", av.getId(), avUnderTest.getId());
        Assert.assertEquals("wrong attr val value", "goodbye", avUnderTest.getStringValue());
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return createUploadRequest();
    }
}
