package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
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

/**
 * Tests the tracker controller record/attribute deserialization and persisting to 
 * database for the census method attribute case.
 *
 */
public class TrackerControllerCensusMethodAttributeTest extends
        AbstractCensusMethodAttributeTest {

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
    @Autowired
    private CensusMethodDAO cmDAO;

    private Survey survey;
    private Record rec;
    private Attribute attrRow;
    private AttributeValue parentAv;
    private AttributeValue childAv;
    private User user;
    private CensusMethod cmRow;
    private Attribute cmStringAttr;
    private Record childRec;

    @Before
    public void setup() {
        user = userDAO.getUser("admin");

        cmStringAttr = new Attribute();
        cmStringAttr.setName("cmstrattr");
        cmStringAttr.setDescription("cm string attribute desc");
        cmStringAttr.setTypeCode(AttributeType.STRING.getCode());
        cmStringAttr = attrDAO.save(cmStringAttr);

        cmRow = new CensusMethod();
        cmRow.setName("cm row");
        cmRow.setDescription("cm desc");

        List<Attribute> cmRowAttrList = new ArrayList<Attribute>();
        cmRowAttrList.add(cmStringAttr);
        cmRow.setAttributes(cmRowAttrList);
        cmRow = cmDAO.save(cmRow);

        attrRow = new Attribute();
        attrRow.setName("attr");
        attrRow.setDescription("attr desc");
        attrRow.setTypeCode(AttributeType.CENSUS_METHOD_ROW.getCode());
        attrRow.setCensusMethod(cmRow);
        attrRow = attrDAO.save(attrRow);

        survey = new Survey();
        survey.setName("survey name");
        survey.setDescription("survey desc");
        List<Attribute> surveyAttrList = new ArrayList<Attribute>();
        surveyAttrList.add(attrRow);
        survey.setAttributes(surveyAttrList);
        survey = surveyDAO.save(survey);

        // make everything not required for easier setup.
        for (RecordPropertyType rpt : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(survey, rpt, mdDAO);
            rp.setRequired(false);
        }

        childAv = new AttributeValue();
        childAv.setAttribute(this.cmStringAttr);
        childAv.setStringValue("helloworld");
        childAv = avDAO.save(childAv);
        Set<AttributeValue> childAvSet = new HashSet<AttributeValue>();
        childAvSet.add(childAv);

        parentAv = new AttributeValue();
        parentAv.setAttribute(attrRow);
        
        parentAv = avDAO.save(parentAv);

        Set<AttributeValue> parentAvSet = new HashSet<AttributeValue>();
        parentAvSet.add(parentAv);

        childRec = new Record();
        childRec.setSurvey(survey);
        childRec.setUser(user);
        childRec.setAttributes(childAvSet);
        childRec.setAttributeValue(parentAv);
        childRec = recordDAO.saveRecord(childRec);
        
        // update the set instance
        attributeDAO.refresh(parentAv);

        rec = new Record();
        rec.setSurvey(survey);
        rec.setUser(user);
        rec.setAttributes(parentAvSet);
        rec = recordDAO.save(rec);
        
        getSession().flush();
    }
    
    /**
     * Create new record with census method attribute rows.
     * @throws Exception
     */
    @Test
    public void testCreateNewRecordRowsCmAttr() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });

        int recCountStart = recordDAO.countAllRecords();

        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(null, null, attrRow, null);
        baseEntry.addChild(new ParamEntry(null, null, cmStringAttr, "asdf"));
        
        this.addParams(baseEntry, request);

        ModelAndView mv = handle(request, response);

        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart + 1, recCountEnd);

        Integer recId = (Integer) mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);

        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "asdf", createdChildAv.getStringValue());
    }

    /**
     * Create new record with census method attribute cols
     * @throws Exception
     */
    @Test
    public void testCreateNewRecordColsCmAttr() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });

        // adjust the attr type...
        attrRow.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        attrDAO.save(attrRow);

        int recCountStart = recordDAO.countAllRecords();

        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(0, null, attrRow, null);
        baseEntry.addChild(new ParamEntry(null, null, cmStringAttr, "asdf"));
        
        this.addParams(baseEntry, request);

        ModelAndView mv = handle(request, response);

        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart + 1, recCountEnd);

        Integer recId = (Integer) mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);

        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "asdf", createdChildAv.getStringValue());
    }
    
    /**
     * Edit an existing record with census method attribute row.
     * @throws Exception
     */
    @Test
    public void testEditingExistingCmRows() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        // check some preconditions..
        {
            Assert.assertNotNull("record set should be non null", parentAv.getRecords());
            Assert.assertEquals("wrong record set size", 1, parentAv.getRecords().size());
            Record checkChildRec = parentAv.getRecords().iterator().next();
            AttributeValue checkChildAv = checkChildRec.getAttributes().iterator().next();
            Assert.assertEquals("child av value not right", "helloworld", checkChildAv.getStringValue());
        }
        
        int recCountStart = recordDAO.countAllRecords();

        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(TrackerController.PARAM_RECORD_ID, rec.getId().toString());
                
        ParamEntry baseEntry = new ParamEntry(null, childRec, attrRow, null);
        baseEntry.addChild(new ParamEntry(null, null, cmStringAttr, "zzzz"));
        
        this.addParams(baseEntry, request);
        
        ModelAndView mv = handle(request, response);
        
        getSession().flush();

        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart, recCountEnd);

        Integer recId = (Integer) mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        
        recordDAO.refresh(createdRec);
        
        Assert.assertNotNull("created rec should not be null", createdRec);
        Assert.assertEquals("wrong record id", rec.getId(), createdRec.getId());
        
        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Assert.assertEquals("parent av id has changed", parentAv.getId(), createdParentAv.getId());
        Assert.assertNotNull("created parent av should not be null", createdParentAv);
        Assert.assertNotNull("records list should not be null", createdParentAv.getRecords());
        Assert.assertEquals("wrong parent av record set size", 1, createdParentAv.getRecords().size());
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        Assert.assertEquals("wrong child rec id", childRec.getId(), createdChildRec.getId());
        Assert.assertNotNull("created child rec should be non null", createdChildRec);
        Assert.assertNotNull("created child attr set should be non null", createdChildRec.getAttributes());
        Assert.assertEquals("wrong rec attr set size", 1, createdChildRec.getAttributes().size());
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "zzzz", createdChildAv.getStringValue());
    }
    
    /**
     * Edit an existing record with orphaned census method attribute rows.
     * @throws Exception
     */
    @Test
    public void testEditExistingOrphanedRows() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        survey.getAttributes().clear();
        surveyDAO.save(survey);
        getSession().flush();
        
        // check some preconditions..
        {
            Assert.assertNotNull("record set should be non null", parentAv.getRecords());
            Assert.assertEquals("wrong record set size", 1, parentAv.getRecords().size());
            Record checkChildRec = parentAv.getRecords().iterator().next();
            AttributeValue checkChildAv = checkChildRec.getAttributes().iterator().next();
            Assert.assertEquals("child av value not right", "helloworld", checkChildAv.getStringValue());
        }
        
        int recCountStart = recordDAO.countAllRecords();

        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(TrackerController.PARAM_RECORD_ID, rec.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(null, childRec, attrRow, null);
        baseEntry.addChild(new ParamEntry(null, null, cmStringAttr, "zzzz"));
        
        this.addParams(baseEntry, request);
        
        ModelAndView mv = handle(request, response);
        
        getSession().flush();

        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart, recCountEnd);

        Integer recId = (Integer) mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        
        recordDAO.refresh(createdRec);
        
        Assert.assertNotNull("created rec should not be null", createdRec);
        Assert.assertEquals("wrong record id", rec.getId(), createdRec.getId());
        
        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Assert.assertEquals("parent av id has changed", parentAv.getId(), createdParentAv.getId());
        Assert.assertNotNull("created parent av should not be null", createdParentAv);
        Assert.assertNotNull("records list should not be null", createdParentAv.getRecords());
        Assert.assertEquals("wrong parent av record set size", 1, createdParentAv.getRecords().size());
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        
        Assert.assertNotNull("created child rec should be non null", createdChildRec);
        Assert.assertEquals("wrong child rec id", childRec.getId(), createdChildRec.getId());
        
        recordDAO.refresh(createdChildRec);
        
        Assert.assertNotNull("created child attr set should be non null", createdChildRec.getAttributes());
        Assert.assertEquals("wrong rec attr set size", 1, createdChildRec.getAttributes().size());
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        
        recordDAO.refresh(createdChildAv);
        
        Assert.assertEquals("wrong child av value", "zzzz", createdChildAv.getStringValue());
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return createUploadRequest();
    }
}
