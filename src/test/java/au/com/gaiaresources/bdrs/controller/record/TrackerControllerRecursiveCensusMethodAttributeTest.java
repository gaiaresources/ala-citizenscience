package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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

public class TrackerControllerRecursiveCensusMethodAttributeTest extends
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
    private Attribute surveyAttr;
    private AttributeValue parentAv;
    private AttributeValue childAv;
    private AttributeValue childChildAv;
    private User user;
    private CensusMethod childCm;
    private CensusMethod parentCm;
    private Attribute childCmStringAttr;
    private Record childRec;
    private Record childChildRec;
    
    private Attribute parentCmAttr;
    
    @Before
    public void setup() {
        user = userDAO.getUser("admin");
        
        childCmStringAttr = new Attribute();
        childCmStringAttr.setName("child cm str attr");
        childCmStringAttr.setDescription("child cm str attr desc");
        childCmStringAttr.setTypeCode(AttributeType.STRING.getCode());
        childCmStringAttr = attrDAO.save(childCmStringAttr);
        
        childCm = new CensusMethod();
        childCm.setName("cm row");
        childCm.setDescription("cm desc");
        
        List<Attribute> childCmAttrList = new ArrayList<Attribute>();
        childCmAttrList.add(childCmStringAttr);
        childCm.setAttributes(childCmAttrList);
        childCm = cmDAO.save(childCm);
        
        parentCm = new CensusMethod();
        parentCm.setName("parent cm");
        parentCm.setDescription("parent cm desc");
         
        parentCmAttr = new Attribute();
        parentCmAttr.setName("parent cm attr");
        parentCmAttr.setDescription("parent cm attr desc");
        parentCmAttr.setTypeCode(AttributeType.CENSUS_METHOD_ROW.getCode());
        parentCmAttr.setCensusMethod(childCm);
        parentCmAttr = attrDAO.save(parentCmAttr);
        
        List<Attribute> parentCmAttrList = new ArrayList<Attribute>();
        parentCmAttrList.add(parentCmAttr);
        
        parentCm.setAttributes(parentCmAttrList);
        parentCm = cmDAO.save(parentCm);
        
        surveyAttr = new Attribute();
        surveyAttr.setName("attr");
        surveyAttr.setDescription("attr desc");
        surveyAttr.setTypeCode(AttributeType.CENSUS_METHOD_ROW.getCode());
        surveyAttr.setCensusMethod(parentCm);
        surveyAttr = attrDAO.save(surveyAttr);
        
        survey = new Survey();
        survey.setName("survey name");
        survey.setDescription("survey desc");
        List<Attribute> surveyAttrList = new ArrayList<Attribute>();
        surveyAttrList.add(surveyAttr);
        survey.setAttributes(surveyAttrList);
        survey = surveyDAO.save(survey);
        
        // make everything not required for easier setup.
        for (RecordPropertyType rpt : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(survey, rpt, mdDAO);
            rp.setRequired(false);
        }
        
        childChildAv = new AttributeValue();
        childChildAv.setAttribute(this.childCmStringAttr);
        childChildAv.setStringValue("helloworld");
        childChildAv = avDAO.save(childChildAv);
        Set<AttributeValue> childChildAvSet = new HashSet<AttributeValue>();
        childChildAvSet.add(childChildAv);
        
        childAv = new AttributeValue();
        childAv.setAttribute(this.parentCmAttr);
        Set<AttributeValue> childAvSet = new HashSet<AttributeValue>();
        childAvSet.add(childAv);
        childAv = avDAO.save(childAv);
        
        parentAv = new AttributeValue();
        parentAv.setAttribute(surveyAttr);
        parentAv = avDAO.save(parentAv);
        
        Set<AttributeValue> parentAvSet = new HashSet<AttributeValue>();
        parentAvSet.add(parentAv);
        
        childChildRec = new Record();
        childChildRec.setSurvey(survey);
        childChildRec.setUser(user);
        childChildRec.setAttributes(childChildAvSet);
        childChildRec.setAttributeValue(childAv);
        childChildRec = recordDAO.saveRecord(childChildRec);
        
        childRec = new Record();
        childRec.setSurvey(survey);
        childRec.setUser(user);
        childRec.setAttributes(childAvSet);
        childRec.setAttributeValue(parentAv);
        childRec = recordDAO.saveRecord(childRec);
        
        rec = new Record();
        rec.setSurvey(survey);
        rec.setUser(user);
        rec.setAttributes(parentAvSet);
        rec = recordDAO.save(rec);
        
        getSession().flush();
        recordDAO.refresh(childRec);
        recordDAO.refresh(childChildRec);
        avDAO.refresh(parentAv);
        avDAO.refresh(childAv);
    }
    
    /**
     * Create a new record with census method attribute rows inside census method attribute rows.
     * @throws Exception
     */
    @Test
    public void testRows() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        int recCountStart = recordDAO.countAllRecords();
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(null, null, surveyAttr, null);
        ParamEntry subEntry = new ParamEntry(null, null, parentCmAttr, null);
        baseEntry.addChild(subEntry);
        subEntry.addChild(new ParamEntry(null, null, childCmStringAttr, "asdf"));
        
        this.addParams(baseEntry, request);
        
        ModelAndView mv = handle(request, response);
        
        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart + 1, recCountEnd);
        
        Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);
        
        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Record createdSubChildRec = createdChildAv.getRecords().iterator().next();
        AttributeValue createdSubChildAv = createdSubChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "asdf", createdSubChildAv.getStringValue());
    }
    
    /**
     * Create a new record with census method attr cols inside census method attr cols.
     * 
     * @throws Exception
     */
    @Test
    public void testCols() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        // adjust the attr type...
        surveyAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        parentCmAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        
        attrDAO.save(surveyAttr);
        attrDAO.save(parentCmAttr);
        
        int recCountStart = recordDAO.countAllRecords();
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(0, null, surveyAttr, null);
        ParamEntry subEntry = new ParamEntry(0, null, parentCmAttr, null);
        baseEntry.addChild(subEntry);
        subEntry.addChild(new ParamEntry(null, null, childCmStringAttr, "asdf"));
        
        this.addParams(baseEntry, request);
        
        ModelAndView mv = handle(request, response);
        
        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart + 1, recCountEnd);
        
        Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);
        
        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Record createdSubChildRec = createdChildAv.getRecords().iterator().next();
        AttributeValue createdSubChildAv = createdSubChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "asdf", createdSubChildAv.getStringValue());
    }
    
    /**
     * Create a new record with census method attr cols and 2 entries in the child
     * sub census method attribute col.
     * @throws Exception
     */
    @Test
    public void testColsMulti() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        // adjust the attr type...
        surveyAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        parentCmAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        
        attrDAO.save(surveyAttr);
        attrDAO.save(parentCmAttr);
        
        int recCountStart = recordDAO.countAllRecords();
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        
        ParamEntry baseEntry1 = new ParamEntry(0, null, surveyAttr, null);
        ParamEntry subEntry1 = new ParamEntry(0, null, parentCmAttr, null);
        baseEntry1.addChild(subEntry1);
        subEntry1.addChild(new ParamEntry(null, null, childCmStringAttr, "asdf"));
        
        ParamEntry subEntry2 = new ParamEntry(1, null, parentCmAttr, null);
        subEntry2.addChild(new ParamEntry(null, null, childCmStringAttr, "asdf"));
        baseEntry1.addChild(subEntry2);
        
        this.addParams(baseEntry1, request);
        
        this.logMap(request);
        
        ModelAndView mv = handle(request, response);
        
        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart + 1, recCountEnd);
        
        Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);
        
        Assert.assertEquals("Wrong av set size", 1, createdRec.getAttributes().size());
        // iterate through the top level attribute values
        Iterator<AttributeValue> avIter = createdRec.getAttributes().iterator();
        AttributeValue createdParentAv = avIter.next();
        
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        
        Assert.assertEquals("wrong sub sub record count", 2, createdChildAv.getRecords().size());
        
        Iterator<Record> childChildRecIter = createdChildAv.getRecords().iterator();
        while(childChildRecIter.hasNext()) {
            Record r = childChildRecIter.next();
            Assert.assertEquals("wrong child child av attr set size", 1, r.getAttributes().size());
            AttributeValue av = r.getAttributes().iterator().next();
            Assert.assertEquals("wrong child av value", "asdf", av.getStringValue());
        }
    }
    
    /**
     * Edit and existing record with census method attr cols inside census method attr cols.
     * @throws Exception
     */
    @Test
    public void testColsExisting() throws Exception {
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        // adjust the attr type...
        surveyAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        parentCmAttr.setTypeCode(AttributeType.CENSUS_METHOD_COL.getCode());
        
        attrDAO.save(surveyAttr);
        attrDAO.save(parentCmAttr);
        
        getSession().flush();
        attrDAO.refresh(surveyAttr);
        attrDAO.refresh(parentCmAttr);
        
        // assert preconditions...
        {
            Record createdRec = recordDAO.getRecord(rec.getId());
            AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
            Record createdChildRec = createdParentAv.getRecords().iterator().next();
            AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
            Record createdSubChildRec = createdChildAv.getRecords().iterator().next();
            AttributeValue createdSubChildAv = createdSubChildRec.getAttributes().iterator().next();
            Assert.assertEquals("wrong child av value", "helloworld", createdSubChildAv.getStringValue());
        }
        
        int recCountStart = recordDAO.countAllRecords();
        
        request.setRequestURI(TrackerController.EDIT_URL);
        request.setMethod(RequestMethod.POST.toString());
        request.setParameter(TrackerController.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(TrackerController.PARAM_RECORD_ID, rec.getId().toString());
        
        ParamEntry baseEntry = new ParamEntry(0, childRec, surveyAttr, null);
        ParamEntry subEntry = new ParamEntry(0, childChildRec, parentCmAttr, null);
        baseEntry.addChild(subEntry);
        subEntry.addChild(new ParamEntry(null, null, childCmStringAttr, "asdf"));
        
        this.addParams(baseEntry, request);
        
        ModelAndView mv = handle(request, response);
        
        int recCountEnd = recordDAO.countAllRecords();
        Assert.assertEquals("wrong rec count", recCountStart, recCountEnd);
        
        Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
        Assert.assertNotNull("rec id should not be null", recId);
        Record createdRec = recordDAO.getRecord(recId);
        Assert.assertNotNull("created rec should not be null", createdRec);
        
        // any of these could throw a null pointer ex
        AttributeValue createdParentAv = createdRec.getAttributes().iterator().next();
        Record createdChildRec = createdParentAv.getRecords().iterator().next();
        AttributeValue createdChildAv = createdChildRec.getAttributes().iterator().next();
        Record createdSubChildRec = createdChildAv.getRecords().iterator().next();
        Assert.assertEquals("wrong num of attribute values", 1, createdSubChildRec.getAttributes().size());
        AttributeValue createdSubChildAv = createdSubChildRec.getAttributes().iterator().next();
        Assert.assertEquals("wrong child av value", "asdf", createdSubChildAv.getStringValue());
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return createUploadRequest();
    }
    
}
