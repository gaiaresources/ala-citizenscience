package au.com.gaiaresources.bdrs.controller.record;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class TrackerControllerCensusMethodTest extends AbstractControllerTest {
    @Autowired 
    CensusMethodDAO cmDAO;
    @Autowired
    AttributeDAO attrDAO;
    @Autowired 
    MetadataDAO metadataDAO;
    @Autowired
    SurveyDAO surveyDAO;
    @Autowired
    RecordDAO recordDAO;
    @Autowired
    TaxaDAO taxaDAO;
    
    private CensusMethod m1;
    private CensusMethod m2;
    private CensusMethod m3;
    private Survey survey;
    
    private TaxonGroup taxonGroupBirds;
//    private TaxonGroup taxonGroupFrogs;    
    private IndicatorSpecies speciesA;
//    private IndicatorSpecies speciesB;
    
    @Before
    public void setup() {
    	taxonGroupBirds = new TaxonGroup();
        taxonGroupBirds.setName("Birds");
        taxonGroupBirds = taxaDAO.save(taxonGroupBirds);
        
//        taxonGroupFrogs = new TaxonGroup();
//        taxonGroupFrogs.setName("Frogs");
//        taxonGroupFrogs = taxaDAO.save(taxonGroupBirds);
        
        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroupBirds);
        speciesA = taxaDAO.save(speciesA);
//        
//        speciesB = new IndicatorSpecies();
//        speciesB.setCommonName("Indicator Species B");
//        speciesB.setScientificName("Indicator Species B");
//        speciesB.setTaxonGroup(taxonGroupBirds);
//        speciesB = taxaDAO.save(speciesB);
    	
     // worst test data generation ever!
        m1 = new CensusMethod();
        m1.setName("apple");
        m1.setTaxonomic(Taxonomic.NONTAXONOMIC);
        Attribute testAttr1 = new Attribute();
        testAttr1.setName("an_attribute");
        testAttr1.setDescription("attribute description");
        testAttr1.setRequired(true);
        testAttr1.setScope(AttributeScope.RECORD);
        testAttr1.setTag(false);
        testAttr1.setTypeCode("IN");
        attrDAO.save(testAttr1);
        
        Attribute testAttr2 = new Attribute();
        testAttr2.setName("anotherattr");
        testAttr2.setDescription("attribsdfsute desgfdsdfcription");
        testAttr2.setRequired(true);
        testAttr2.setScope(AttributeScope.RECORD);
        testAttr2.setTag(false);
        testAttr2.setTypeCode("ST");
        attrDAO.save(testAttr2);
        
        m1.getAttributes().add(testAttr1);
        m1.getAttributes().add(testAttr2);
        m1 = cmDAO.save(m1);
        
        
        m2 = new CensusMethod();
        m2.setName("banana");
        m2.setTaxonomic(Taxonomic.TAXONOMIC);
        Attribute testAttr3 = new Attribute();
        testAttr3.setName("an_attribute_22");
        testAttr3.setDescription("attribute description 22");
        testAttr3.setRequired(true);
        testAttr3.setScope(AttributeScope.RECORD);
        testAttr3.setTag(false);
        testAttr3.setTypeCode("IN");
        m2.getAttributes().add(testAttr3);
        m2 = cmDAO.save(m2);
        attrDAO.save(testAttr3);
        
        m3 = new CensusMethod();
        m3.setName("chicken");
        m3.setTaxonomic(Taxonomic.OPTIONALLYTAXONOMIC);
        Attribute testAttr4 = new Attribute();
        testAttr4.setName("an_attribute_33");
        testAttr4.setDescription("attribute description 33");
        testAttr4.setRequired(true);
        testAttr4.setScope(AttributeScope.RECORD);
        testAttr4.setTag(false);
        testAttr4.setTypeCode("IN");
        m3.getAttributes().add(testAttr4);
        m3 = cmDAO.save(m3);
        attrDAO.save(testAttr4);
        
        survey = new Survey();
        survey.setName("SingleSiteMultiTaxaSurvey 1234");
        survey.setActive(true);
        survey.setStartDate(new Date());
        survey.setDescription("Single Site Multi Taxa Survey Description");
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md);
        // no attributes
        //survey.setAttributes(attributeList);
        // no species which infact means all species
        //survey.setSpecies(speciesSet);
        survey.getCensusMethods().add(m1);
        survey.getCensusMethods().add(m2);
        survey.getCensusMethods().add(m3);
        survey = surveyDAO.save(survey);
        
        // all tests in this class are attempting to edit a record
        request.setParameter(RecordWebFormContext.PARAM_EDIT, Boolean.TRUE.toString());
        
    }
    
    @Test
    public void testOptionallyTaxonCensusMethodGet() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m3.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        List<FormField> cmFormFields = formContext.getNamedFormFields().get("censusMethodFormFieldList");
        // m1 should have 2 test attributes so....
        Assert.assertEquals(1, cmFormFields.size());
        Assert.assertTrue(Taxonomic.OPTIONALLYTAXONOMIC.equals(mv.getModel().get("taxonomic")));
        Assert.assertEquals(null, mv.getModel().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
    }
    
    @Test
    public void testNonTaxonCensusMethodGet() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m1.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<FormField> cmFormFields = formContext.getNamedFormFields().get("censusMethodFormFieldList");
        // m1 should have 2 test attributes so....
        Assert.assertEquals(2, cmFormFields.size());
        Assert.assertTrue(Taxonomic.NONTAXONOMIC.equals(mv.getModel().get("taxonomic")));
        Assert.assertEquals(null, mv.getModel().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
    }
    
    @Test
    public void testTaxonCensusMethodGet() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m2.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<FormField> cmFormFields = formContext.getNamedFormFields().get("censusMethodFormFieldList");
        // m1 should have 1 test attributes so....
        Assert.assertEquals(1, cmFormFields.size());
        Assert.assertTrue(Taxonomic.TAXONOMIC.equals(mv.getModel().get("taxonomic")));
        Assert.assertEquals(null, mv.getModel().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
    }
    
    @Test
    public void testNoCensusMethodGet() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, "0");
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<FormField> cmFormFields = formContext.getNamedFormFields().get("censusMethodFormFieldList");

        Assert.assertEquals(0, cmFormFields.size());
        Assert.assertEquals(null, mv.getModel().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
        Assert.assertEquals(null, mv.getModel().get("censusMethod"));
    }
    
    @Test
    public void testTaxonCensusMethodPostValidateFail() throws Exception {
        login("admin", "password", new String[] { Role.USER });
        
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m1.getId().toString());
        request.setParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER, "Submit and Add Another");
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, TrackerController.EDIT_URL);
        
        Assert.assertEquals(survey.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_SURVEY_ID));
        Assert.assertEquals(m1.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
        Assert.assertNotNull(getRequestContext().getSessionAttribute("errorMap"));
    }
    
    @Test
    public void testTaxonCensusMethodPostValidatePass() throws Exception {
        login("admin", "password", new String[] { Role.USER });
        
        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));
        
        Map<String, String> params = generateTrackerFormData(m1.getAttributes(), AttributeParser.DEFAULT_PREFIX, today);
        params.put("latitude", "-32.546");
        params.put("longitude", "115.488");
        params.put("date", dateFormat.format(today));
        params.put(TrackerController.PARAM_TIME, "15:48");
        params.put("notes", "");
        request.setParameters(params);
        
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m1.getId().toString());
        request.setParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER, "Submit and Add Another");
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, RecordWebFormContext.SURVEY_RENDER_REDIRECT_URL);
        
        Assert.assertEquals(survey.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_SURVEY_ID));
        Assert.assertEquals(m1.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
        Assert.assertNull(getRequestContext().getSessionAttribute("errorMap"));
        
        Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);
        
        this.assertTrackerSaveToRecord(rec, params, today);
    }
    
    @Test
    public void testOptionallyTaxonCensusMethodPostWithoutTaxon() throws Exception {
        login("admin", "password", new String[] { Role.USER });
        
        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));
        
        Map<String, String> params = generateTrackerFormData(m3.getAttributes(), AttributeParser.DEFAULT_PREFIX, today);

        params.put("survey_species_search", "");
        params.put("number", "");
        params.put("latitude", "-32.546");
        params.put("longitude", "115.488");
        params.put("date", dateFormat.format(today));
        params.put(TrackerController.PARAM_TIME, "15:48");
        params.put("notes", "");
        request.setParameters(params);
        
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m3.getId().toString());
        request.setParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER, "Submit and Add Another");
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, RecordWebFormContext.SURVEY_RENDER_REDIRECT_URL);
        
        Assert.assertEquals(survey.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_SURVEY_ID));
        Assert.assertEquals(m3.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
        Assert.assertNull(getRequestContext().getSessionAttribute("errorMap"));
        
        Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);
        
        this.assertTrackerSaveToRecord(rec, params, today);
    }
    
    @Test
    public void testOptionallyTaxonCensusMethodPostWithTaxon() throws Exception {
        login("admin", "password", new String[] { Role.USER });
        
        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));
        
        Map<String, String> params = generateTrackerFormData(m3.getAttributes(), AttributeParser.DEFAULT_PREFIX, today);

        params.put("survey_species_search", speciesA.getScientificName());
        params.put("number", "1");
        params.put("latitude", "-32.546");
        params.put("longitude", "115.488");
        params.put("date", dateFormat.format(today));
        params.put(TrackerController.PARAM_TIME, "15:48");
        params.put("notes", "");
        request.setParameters(params);
        
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, m3.getId().toString());
        request.setParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER, "Submit and Add Another");
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, RecordWebFormContext.SURVEY_RENDER_REDIRECT_URL);
        
        Assert.assertEquals(survey.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_SURVEY_ID));
        Assert.assertEquals(m3.getId(), mv.getModelMap().get(BdrsWebConstants.PARAM_CENSUS_METHOD_ID));
        Assert.assertNull(getRequestContext().getSessionAttribute("errorMap"));
        
        Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);
        
        this.assertTrackerSaveToRecord(rec, params, today);
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
    
    private Map<String, String> generateTrackerFormData(List<Attribute> attrList, String attributePrefix, Date today) throws ParseException {
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        int seed = 0;
        Map<String, String> params = new HashMap<String, String>();
        for (Attribute attr : attrList) {
            genRandomAttributeValue(attr, seed++, null, attributePrefix, params);
        }
        return params;
    }
    
    private void assertTrackerSaveToRecord(Record record, Map<String, String> params, Date today) {
        IndicatorSpecies species = record.getSpecies();
        CensusMethod censusMethod = record.getCensusMethod();
        
        for(TypedAttributeValue recAttr: record.getAttributes()) {
            String key = "";
            Attribute attr = recAttr.getAttribute();
            if(survey.getAttributes().contains(recAttr.getAttribute())) {
                key = WebFormAttributeParser.getParamKey("", attr);
            } else if(species != null && species.getTaxonGroup().getAttributes().contains(recAttr.getAttribute())) {
                key = WebFormAttributeParser.getParamKey(AttributeParser.DEFAULT_PREFIX, attr);
            } else if (censusMethod != null && censusMethod.getAttributes().contains(recAttr.getAttribute())) {
                key = WebFormAttributeParser.getParamKey(AttributeParser.DEFAULT_PREFIX, attr);
            } else {
                Assert.assertFalse(true);
                key = null;
            }
            assertAttributes(recAttr, params, key);
        }
    }
}
