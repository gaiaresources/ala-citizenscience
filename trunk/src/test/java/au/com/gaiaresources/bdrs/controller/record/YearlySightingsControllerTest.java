package au.com.gaiaresources.bdrs.controller.record;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordAttributeFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Tests all aspects of the <code>YearlySightingsControllerTest</code>.
 */
public class YearlySightingsControllerTest extends RecordFormTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired 
    private MetadataDAO metadataDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private LocationService locationService;
    @Autowired
    private RedirectionService redirectionService;

    private Survey survey;
    private TaxonGroup taxonGroup;
    private IndicatorSpecies speciesA;
    private IndicatorSpecies speciesB;
    private Location locationA;
    private Location locationB;
    
    @Before
    public void setUp() throws Exception {
        super.doSetup();
        taxonGroup = new TaxonGroup();
        taxonGroup.setName("Birds");
        taxonGroup = taxaDAO.save(taxonGroup);
        
        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroup);
        speciesA = taxaDAO.save(speciesA);
        
        speciesB = new IndicatorSpecies();
        speciesB.setCommonName("Indicator Species B");
        speciesB.setScientificName("Indicator Species B");
        speciesB.setTaxonGroup(taxonGroup);
        speciesB = taxaDAO.save(speciesB);
        
        List<Attribute> attributeList = createAttrList("", true, new AttributeScope[] { 
                    AttributeScope.RECORD, AttributeScope.SURVEY,
                    AttributeScope.RECORD_MODERATION, AttributeScope.SURVEY_MODERATION, null });
        
        HashSet<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(speciesA);

        survey = new Survey();
        survey.setName("SingleSiteMultiTaxaSurvey 1234");
        survey.setActive(true);
        survey.setStartDate(new Date());
        survey.setDescription("Single Site Multi Taxa Survey Description");
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
        metadataDAO.save(md);
        survey.setAttributes(attributeList);
        survey.setSpecies(speciesSet);
        survey = surveyDAO.save(survey);
        
        User admin = userDAO.getUser("admin");
        
        locationA = new Location();
        locationA.setName("Location A");        
        locationA.setUser(admin);
        locationA.setLocation(locationService.createPoint(-40.58, 153.1));
        locationDAO.save(locationA);
        
        locationB = new Location();
        locationB.setName("Location B");        
        locationB.setUser(admin);
        locationB.setLocation(locationService.createPoint(-32.58, 154.2));
        locationDAO.save(locationB);
    }
    
    @Test
    public void testAddRecord() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/user/yearlySightings.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "yearlySightings");
        
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "species");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "locations");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "dateMatrix");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "today");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "location");
        Assert.assertNull(mv.getModelMap().get("location"));
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        Assert.assertNotNull(formContext.getNamedFormFields().get("formFieldList"));

    }
    
    
    @Test 
    public void testAddRecordWithRecordIdLowerLimitOutside() throws Exception{
    	testAddRecordWithRecordId("99");
    }
    
    @Test 
    public void testAddRecordWithRecordIdLowerLimitEdge() throws Exception{
    	testAddRecordWithRecordId("100");
    }
    
    @Test 
    public void testAddRecordWithRecordIdInRange() throws Exception{
    	testAddRecordWithRecordId("101");
    }
    
    @Test 
    public void testAddRecordWithRecordIdUpperLimitEdge() throws Exception{
    	testAddRecordWithRecordId("200");
    }
    
    @Test 
    public void testAddRecordWithRecordIdUpperLimitOutside() throws Exception{
    	testAddRecordWithRecordId("201");
    }
    
    public void testAddRecordWithRecordId(String intWithRangeValue) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Set<AttributeValue> attributeSet = new HashSet<AttributeValue>();
        Map<Attribute, AttributeValue> expectedRecordAttrMap = new HashMap<Attribute, AttributeValue>();
        int seed = 0;
        for(Attribute attr : survey.getAttributes()) {
            if(!AttributeScope.LOCATION.equals(attr.getScope())) {
                AttributeValue recAttr = new AttributeValue();
                recAttr.setAttribute(attr);
                genRandomAttributeValue(recAttr, seed++, true, true, null, "", null);
                recAttr = attributeDAO.save(recAttr);
                attributeSet.add(recAttr);
                expectedRecordAttrMap.put(attr, recAttr);
            }
        }
        
        // Add a record to the Survey
        Record record = new Record();
        record.setSurvey(survey);
        record.setSpecies(speciesA);
        record.setUser(getRequestContext().getUser());
        record.setLocation(locationA);
        record.setHeld(false);
        Date now = new Date();
        record.setWhen(now);
        record.setTime(now.getTime());
        record.setLastDate(now);
        record.setLastTime(now.getTime());
        record.setNotes("This is a test record");
        record.setFirstAppearance(false);
        record.setLastAppearance(false);
        record.setBehaviour("Eating a muffin");
        record.setHabitat("By my foot");
        record.setNumber(1);
        record.setAttributes(attributeSet);
        // record.setReviewRequests()
        // record.setMetadata()
        record = recordDAO.saveRecord(record);

        request.setMethod("GET");
        request.setRequestURI("/bdrs/user/yearlySightings.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_RECORD_ID, record.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "yearlySightings");
        
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "species");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "locations");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "dateMatrix");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "today");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "location");
        Assert.assertEquals(record.getLocation(), mv.getModelMap().get("location"));
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        Assert.assertNotNull(formContext.getNamedFormFields().get("formFieldList"));
        
        for(FormField formField : (formContext.getNamedFormFields().get("formFieldList"))) {
            if(formField.isAttributeFormField()) {
                RecordAttributeFormField attributeField = (RecordAttributeFormField)formField;
                
                Assert.assertEquals(record, attributeField.getRecord());
                Assert.assertEquals(survey, attributeField.getSurvey());
            }
            else if(formField.isPropertyFormField()) {
                RecordPropertyFormField propertyField = (RecordPropertyFormField)formField;
                Assert.assertEquals(record, propertyField.getRecord());
                Assert.assertEquals(survey, propertyField.getSurvey());
            }
            else {
                Assert.assertTrue(false);
            }
        }
    }
    
    @Test 
    public void testSubmitRecordLowerLimitOutside() throws Exception{
    	testSubmitRecord("99");
    }
    
    @Test 
    public void testSubmitRecordLowerLimitEdge() throws Exception{
    	testSubmitRecord("100");
    }
    
    @Test 
    public void testSubmitRecordInRange() throws Exception{
    	testSubmitRecord("101");
    }
    
    @Test 
    public void testSubmitRecordUpperLimitEdge() throws Exception{
    	testSubmitRecord("200");
    }
    
    @Test 
    public void testSubmitRecordUpperLimitOutside() throws Exception{
    	testSubmitRecord("201");
    }
    

    public void testSubmitRecord(String intWithRangeValue) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/user/yearlySightings.htm");
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        params.put(BdrsWebConstants.PARAM_LOCATION_ID, locationA.getId().toString());
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        
        int seed = 0;
        for (Attribute attr : survey.getAttributes()) {
            if(AttributeScope.SURVEY.equals(attr.getScope()) || AttributeScope.SURVEY_MODERATION.equals(attr.getScope())) {
                genRandomAttributeValue(attr, seed++, null, "", params);
            }
        }
        
        Date startDate = SurveyFormRendererType.YEARLY_SIGHTINGS.getStartDateForSightings(survey);
        Date endDate = SurveyFormRendererType.YEARLY_SIGHTINGS.getEndDateForSightings(survey);

        // Fill in 300 out of the possible 365/366 days of the year with the
        // index of that day in the year.
        Calendar cal = new GregorianCalendar();
        cal.setTime(startDate);  // start at the start date for the survey...
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Set<Integer> sightingSet = new HashSet<Integer>();
        for(int i=0; i<300; i++) {
            Integer sighting = Integer.valueOf(cal.get(Calendar.DAY_OF_YEAR));
            sightingSet.add(sighting);
            params.put(String.format("date_%d", cal.getTimeInMillis()), sighting.toString());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        request.setParameters(params);
        ModelAndView mv = handle(request, response);
        
        assertRedirect(mv, redirectionService.getMySightingsUrl(survey));
        
        Assert.assertEquals(300, recordDAO.countAllRecords().intValue());
        
        for(Record rec : recordDAO.getRecords(getRequestContext().getUser(), survey, locationA, startDate, endDate)) {
            // Assert that this was a sighting we added
            Assert.assertTrue(sightingSet.remove(rec.getNumber()));
            cal.setTime(rec.getWhen());
            // Test that the sighting was for the correct day.
            Assert.assertEquals(rec.getNumber().intValue(), cal.get(Calendar.DAY_OF_YEAR));
            
            for(TypedAttributeValue recAttr: rec.getAttributes()) {
                String key = String.format(AttributeParser.ATTRIBUTE_NAME_TEMPLATE, "", recAttr.getAttribute().getId());
                assertAttributes(recAttr, params, key);
            }
        }

        // All records accounted for.
        Assert.assertTrue(sightingSet.isEmpty());
    }
    
    @Test
    public void testRecordFormPredefinedLocationsAsSurveyOwner() throws Exception {
        super.testRecordLocations("/bdrs/user/yearlySightings.htm", true, SurveyFormRendererType.YEARLY_SIGHTINGS, true);
    }
    
    @Test
    public void testRecordFormLocationsAsSurveyOwner() throws Exception {
        super.testRecordLocations("/bdrs/user/yearlySightings.htm", false, SurveyFormRendererType.YEARLY_SIGHTINGS, true);
    }
    
    @Test
    public void testRecordFormPredefinedLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/yearlySightings.htm", true, SurveyFormRendererType.YEARLY_SIGHTINGS, false);
    }
    
    @Test
    public void testRecordFormLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/yearlySightings.htm", false, SurveyFormRendererType.YEARLY_SIGHTINGS, false);
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
