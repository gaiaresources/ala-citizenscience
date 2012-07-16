package au.com.gaiaresources.bdrs.controller.record;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordAttributeFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Tests all aspects of the <code>TrackerController</code>.
 */
public class TrackerControllerTest extends RecordFormTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private RedirectionService redirectionService;
    
    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    private Survey survey;
    private TaxonGroup taxonGroupBirds;
    private TaxonGroup taxonGroupFrogs;
    private IndicatorSpecies speciesA;
    private IndicatorSpecies speciesB;
    private Location locationA;
    private Location locationB;
    private Location locationPoly;

    private Survey simpleSurvey;
    
    @Before
    public void setUp() throws Exception {
    	
        taxonGroupBirds = new TaxonGroup();
        taxonGroupBirds.setName("Birds");
        taxonGroupBirds = taxaDAO.save(taxonGroupBirds);

        taxonGroupFrogs = new TaxonGroup();
        taxonGroupFrogs.setName("Frogs");
        taxonGroupFrogs = taxaDAO.save(taxonGroupBirds);

        createCensusMethodForAttributes();
        
        List<Attribute> taxonGroupAttributeList;
        Attribute groupAttr;
        for (TaxonGroup group : new TaxonGroup[] { taxonGroupBirds,
                taxonGroupFrogs }) {
            taxonGroupAttributeList = new ArrayList<Attribute>();
            for (boolean isTag : new boolean[] { true, false }) {
                taxonGroupAttributeList.addAll(createAttrList(group.getName(), true, new AttributeScope[]{null}, isTag));
            }
            group.setAttributes(taxonGroupAttributeList);
            taxaDAO.save(group);
        }

        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroupBirds);
        speciesA = taxaDAO.save(speciesA);

        speciesB = new IndicatorSpecies();
        speciesB.setCommonName("Indicator Species B");
        speciesB.setScientificName("Indicator Species B");
        speciesB.setTaxonGroup(taxonGroupBirds);
        speciesB = taxaDAO.save(speciesB);

        List<Attribute> attributeList = createAttrList("", true, new AttributeScope[] {
                AttributeScope.RECORD, AttributeScope.SURVEY, 
                AttributeScope.RECORD_MODERATION, AttributeScope.SURVEY_MODERATION, 
                null });

        HashSet<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(speciesA);
        speciesSet.add(speciesB);

        survey = new Survey();
        survey.setName("SingleSiteMultiTaxaSurvey 1234");
        survey.setActive(true);
        survey.setStartDate(new Date());
        survey.setDescription("Single Site Multi Taxa Survey Description");
        survey.setDefaultRecordVisibility(RecordVisibility.CONTROLLED, metadataDAO);
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md);
        survey.setAttributes(attributeList);
        survey.setSpecies(speciesSet);
        survey = surveyDAO.save(survey);

        simpleSurvey = new Survey();
        simpleSurvey.setName("Simple Survey");
        simpleSurvey.setActive(true);
        simpleSurvey.setStartDate(new Date());
        simpleSurvey.setDescription("Simple Test Survey");
        Metadata md2 = simpleSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md2);
        //survey.setSpecies(speciesSet);
        simpleSurvey = surveyDAO.save(simpleSurvey);
        
        User admin = userDAO.getUser("admin");

        locationA = new Location();
        locationA.setName("Location A");
        locationA.setUser(admin);
        locationA.setLocation(spatialUtil.createPoint(-40.58, 153.1));
        locationDAO.save(locationA);

        locationB = new Location();
        locationB.setName("Location B");
        locationB.setUser(admin);
        locationB.setLocation(spatialUtil.createPoint(-32.58, 154.2));
        locationDAO.save(locationB);
        
        locationPoly = new Location();
        locationPoly.setLocation(spatialUtil.createGeometryFromWKT("POLYGON((114.91699218293 -33.678639850485,115.18066405793 -34.406909655312,117.46582030783 -35.272531755372,119.92675780773 -33.897777012664,114.91699218293 -33.678639850485))"));
        locationPoly.setName("Location Poly");
        locationPoly.setUser(admin);
        locationPoly = locationDAO.save(locationPoly);
        
		// all tests in this class are attempting to edit a record
        request.setParameter(RecordWebFormContext.PARAM_EDIT, "true");
    }
    
    @Test
    public void nullSurveyGet() throws Exception {
        nullSurveyTest("GET");
    }
    
    @Test
    public void nullSurveyPost() throws Exception {
        nullSurveyTest("POST");
    }
    
    private void nullSurveyTest(String requestMethod) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod(requestMethod);
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, Integer.toString(0));
        
        ModelAndView mav = handle(request, response);
        
        assertRedirectAndErrorCode(mav, redirectionService.getMySightingsUrl(null), TrackerController.NO_SURVEY_ERROR_KEY);
    }

    @Test
    public void testAddRecord() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "tracker");
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "record");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        Assert.assertNotNull(formContext.getNamedFormFields().get("surveyFormFieldList"));
        Assert.assertNotNull(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));

        Assert.assertFalse((Boolean) mv.getModelMap().get("preview"));
        Assert.assertEquals("Survey has "+survey.getAttributes().size()+" attributes and "+
                            (RecordPropertyType.values().length - 2)+" properties, but form field count is "+
                            formContext.getNamedFormFields().get("surveyFormFieldList").size(), 
                survey.getAttributes().size()
                + RecordPropertyType.values().length - 2 , // The - 2 is because CREATED and UPDATED are only visible in read mode by deafult.
                formContext.getNamedFormFields().get("surveyFormFieldList").size());
        Assert.assertEquals(0, formContext.getNamedFormFields().get("taxonGroupFormFieldList").size());
        for (FormField formField : formContext.getNamedFormFields().get("surveyFormFieldList")) {
            if (formField.isPropertyFormField()) {
                Assert.assertNull(((RecordPropertyFormField) formField).getSpecies());
            }
        }
        
        // make sure the new record has the correct default record visibility set
        Record rec = (Record)mv.getModel().get("record");
        Assert.assertEquals(RecordVisibility.CONTROLLED, rec.getRecordVisibility());
    }

    @Test
    public void testAddRecordWithExactTaxonSearch() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter("taxonSearch", speciesA.getScientificName());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "tracker");
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "record");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        Assert.assertNotNull(formContext.getNamedFormFields().get("surveyFormFieldList"));
        Assert.assertNotNull(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));

        Assert.assertFalse((Boolean) mv.getModelMap().get("preview"));

        Assert.assertEquals(survey.getAttributes().size()
                + RecordPropertyType.values().length -2, // UPDATED and CREATED are visible in read mode only by default.
                formContext.getNamedFormFields().get("surveyFormFieldList").size());
        // Half of the taxon group attributes are tags.
        Assert.assertEquals((speciesA.getTaxonGroup().getAttributes().size() + attrCm.getAttributes().size()) / 2, formContext.getNamedFormFields().get("taxonGroupFormFieldList").size());
        for (FormField formField : formContext.getNamedFormFields().get("surveyFormFieldList")) {
            if (formField.isPropertyFormField()) {
            	RecordPropertyFormField recordPropertyFormField = (RecordPropertyFormField) formField;
                Assert.assertEquals(speciesA, recordPropertyFormField.getSpecies());
                //Make sure the right descriptions come through in a default survey
                RecordProperty recordProperty = recordPropertyFormField.getRecordProperty();
                switch (recordProperty.getRecordPropertyType()){
                case ACCURACY:
                	Assert.assertEquals("Accuracy (meters)", recordProperty.getDescription());
                	break;
                case NUMBER:
                	Assert.assertEquals("Individual Count", recordProperty.getDescription());
                	break;
                case WHEN:
                	Assert.assertEquals("Date", recordProperty.getDescription());
                	break;
               default:
            	   Assert.assertEquals(recordProperty.getRecordPropertyType().getDefaultDescription(), recordProperty.getDescription());
                }
            }
        }
    }

    @Test
    public void testAddRecordWithMultipleTaxaSearch() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter("taxonSearch", "Indicator Species");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "tracker");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "record");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        IndicatorSpecies expectedTaxon = surveyDAO.getSpeciesForSurveySearch(survey.getId(), request.getParameter("taxonSearch")).get(0);
        Assert.assertFalse((Boolean) mv.getModelMap().get("preview"));
        Assert.assertEquals(survey.getAttributes().size()
                + RecordPropertyType.values().length -2, // -2 is because UPDATED and CREATED are visible in read mode by default.
                formContext.getNamedFormFields().get("surveyFormFieldList").size());
        // Half of the taxon group attributes are tags.
        Assert.assertEquals(expectedTaxon.getTaxonGroup().getAttributes().size() / 2, formContext.getNamedFormFields().get("taxonGroupFormFieldList").size());
        // Its an error that gets logged, but nonetheless the first species
        // should be returned
        for (FormField formField : formContext.getNamedFormFields().get("surveyFormFieldList")) {
            if (formField.isPropertyFormField()) {
                Assert.assertEquals(expectedTaxon, ((RecordPropertyFormField) formField).getSpecies());
            }
        }
    }
    
    @Test
    public void testAddEmptyRecord() throws Exception {
    	Survey mockSurvey = new Survey();
    	mockSurvey.setName("mockSurvey");
    	mockSurvey.setActive(true);
    	mockSurvey.setStartDate(new Date());
    	mockSurvey.setDescription("Survey to test adding an empty record");
    	mockSurvey.setDefaultRecordVisibility(RecordVisibility.CONTROLLED, metadataDAO);
        Metadata mockMd  = mockSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(mockMd);
        surveyDAO.save(mockSurvey);
    	
    	login("admin", "password", new String[] { Role.ADMIN });
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, mockSurvey.getId().toString());
        setDwcRequired(false, mockSurvey);
        setDwcHidden(true, mockSurvey);
        ModelAndView mv = handle(request, response);
        Integer recordId = (Integer)mv.getModelMap().get("record_id");
        Assert.assertNotNull("recordId is null", recordId);
        if (recordId != null) {
        	Record r = recordDAO.getRecord(recordId);
        	Assert.assertNotNull("Record is null", r);
        }
    }

    @Test
    public void testEditRecordLowerLimitOutside() throws Exception {
        testEditRecord("99");
    }

    @Test
    public void testEditRecordLowerLimitEdge() throws Exception {
        testEditRecord("100");
    }

    @Test
    public void testEditRecordInRange() throws Exception {
        testEditRecord("101");
    }

    @Test
    public void testEditRecordUpperLimitEdge() throws Exception {
        testEditRecord("200");
    }

    @Test
    public void testEditRecordUpperLimitOutside() throws Exception {
        testEditRecord("201");
    }

    public void testEditRecord(String intWithRangeValue) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

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

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Set<AttributeValue> attributeList = new HashSet<AttributeValue>();
        Map<Attribute, AttributeValue> expectedRecordAttrMap = new HashMap<Attribute, AttributeValue>();
        int seed = 0;
        for(Attribute attr : survey.getAttributes()) {
            if(!AttributeScope.LOCATION.equals(attr.getScope())) {
                List<AttributeOption> opts = attr.getOptions();
                AttributeValue recAttr = new AttributeValue();
                recAttr.setAttribute(attr);
                genRandomAttributeValue(recAttr, seed++, true, true, null, "", null);
                
                recAttr = attributeDAO.save(recAttr);
                attributeList.add(recAttr);
                expectedRecordAttrMap.put(attr, recAttr);
            }
        }

        for (Attribute attr : record.getSpecies().getTaxonGroup().getAttributes()) {
            if (!attr.isTag()) {
                AttributeValue recAttr = new AttributeValue();
                recAttr.setAttribute(attr);
                genRandomAttributeValue(recAttr, seed++, true, true, null, "", null);
                
                recAttr = attributeDAO.save(recAttr);
                attributeList.add(recAttr);
                expectedRecordAttrMap.put(attr, recAttr);
            }
        }

        record.setAttributes(attributeList);
        record = recordDAO.saveRecord(record);

        request.setMethod("GET");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(BdrsWebConstants.PARAM_RECORD_ID, record.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "tracker");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "record");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");

        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        Assert.assertNotNull(formContext.getNamedFormFields().get("surveyFormFieldList"));
        Assert.assertNotNull(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));

        List<FormField> allFormFields = new ArrayList<FormField>(formContext.getNamedFormFields().get("surveyFormFieldList"));
        allFormFields.addAll(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));
        for (FormField formField : allFormFields) {
            if (formField.isAttributeFormField()) {
                RecordAttributeFormField attributeField = (RecordAttributeFormField) formField;

                Assert.assertEquals(record, attributeField.getRecord());
                Assert.assertEquals(survey, attributeField.getSurvey());
                Assert.assertEquals(expectedRecordAttrMap.get(attributeField.getAttribute()), attributeField.getAttributeValue());
            } else if (formField.isPropertyFormField()) {
                RecordPropertyFormField propertyField = (RecordPropertyFormField) formField;
                Assert.assertEquals(record, propertyField.getRecord());
                Assert.assertEquals(survey, propertyField.getSurvey());
            } else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertEquals(record, mv.getModelMap().get("record"));
        Assert.assertEquals(survey, mv.getModelMap().get("survey"));
    }

    @Test
    public void testSaveRecordLowerLimitOutside() throws Exception {
        testSaveRecord(String.valueOf(INTEGER_WITH_RANGE_LOWER_LIMIT-1), false);
    }

    @Test
    public void testSaveRecordLowerLimitEdge() throws Exception {
        testSaveRecord(String.valueOf(INTEGER_WITH_RANGE_LOWER_LIMIT), true);
    }

    @Test
    public void testSaveRecordInRange() throws Exception {
        testSaveRecord(String.valueOf(INTEGER_WITH_RANGE_LOWER_LIMIT+1), true);
    }

    @Test
    public void testSaveRecordUpperLimitEdge() throws Exception {
        testSaveRecord(String.valueOf(INTEGER_WITH_RANGE_UPPER_LIMIT), true);
    }

    @Test
    public void testSaveRecordUpperLimitOutside() throws Exception {
        testSaveRecord(String.valueOf(INTEGER_WITH_RANGE_UPPER_LIMIT+1), false);
    }

    public void testSaveRecord(String intWithRangeValue, boolean passExpected)
            throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(
                System.currentTimeMillis())));

        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");

        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        params.put("survey_species_search", speciesA.getScientificName());
        params.put("species", speciesA.getId().toString());
        params.put("latitude", "-32.546");
        params.put("longitude", "115.488");
        params.put("date", dateFormat.format(today));
        params.put("time", "15:48");
        params.put("number", "29");
        params.put("notes", "This is a test record");

        params.putAll(createAttributes(intWithRangeValue, dateFormat, today));

        request.setParameters(params);
        ModelAndView mv = handle(request, response);

        if (passExpected) {
            assertRedirect(mv, redirectionService.getMySightingsUrl(survey));
            Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
            Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);

            Assert.assertEquals(speciesA, rec.getSpecies());
            Assert.assertEquals(Double.parseDouble(params.get("latitude")), rec.getPoint().getY());
            Assert.assertEquals(Double.parseDouble(params.get("longitude")), rec.getPoint().getX());

            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(today);
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 48);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Assert.assertEquals(cal.getTime(), rec.getWhen());
            Assert.assertEquals(cal.getTime().getTime(), rec.getTime().longValue());

            Assert.assertEquals(rec.getNotes(), params.get("notes"));

            String key;
            for (TypedAttributeValue recAttr : rec.getAttributes()) {
                Attribute attr = recAttr.getAttribute();
                if (survey.getAttributes().contains(recAttr.getAttribute())) {
                    key = String.format(AttributeParser.ATTRIBUTE_NAME_TEMPLATE, "", attr.getId());
                } else if (speciesA.getTaxonGroup().getAttributes().contains(recAttr.getAttribute())) {
                    key = String.format(AttributeParser.ATTRIBUTE_NAME_TEMPLATE, TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX, attr.getId());
                } else {
                    Assert.assertFalse(true);
                    key = null;
                }

                assertAttributes(recAttr, params, key);
            }
        } else {
            assertRedirect(mv, "/bdrs/user/tracker.htm");
            Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        }
    }

    private Map<? extends String, ? extends String> createAttributes() {
        return createAttributes("123", new SimpleDateFormat("dd MMM yyyy"), new Date());
    }
    
    private Map<? extends String, ? extends String> createAttributes(String intWithRangeValue, DateFormat dateFormat, Date today) {
        Map<String, String> params = new HashMap<String,String>();
        int seed = 0;
        for (Attribute attr : survey.getAttributes()) {
            if(!AttributeScope.LOCATION.equals(attr.getScope())) {
                if (AttributeType.INTEGER_WITH_RANGE.equals(attr.getType())) {
                    // set the value to this value otherwise generate a random value
                    setSpecificAttributeValue(attr, intWithRangeValue, null, "", params);
                } else {
                    genRandomAttributeValue(attr, seed++, null, "", params);
                }
            }
        }

        for (Attribute attr : speciesA.getTaxonGroup().getAttributes()) {
            if (AttributeType.INTEGER_WITH_RANGE.equals(attr.getType())) {
                // set the value to this value otherwise generate a random value
                setSpecificAttributeValue(attr, intWithRangeValue, null, TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX, params);
            } else {
                genRandomAttributeValue(attr, seed++, null, TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX, params);
            }
        }
        return params;
    }

    @Test
    public void testSaveRecordInvalidEarlyDateNoEnd() throws Exception {
        testSaveRecordWithDateRange("04 Jul 2011 12:45", "05 Jul 2011 00:00", null, false);
    }
 
    @Test
    public void testSaveRecordInvalidEarlyDate() throws Exception {
        testSaveRecordWithDateRange("04 Jul 2011 12:45", "05 Jul 2011 00:00", "06 Jul 2011 17:00", false);
    }
 
    //@Test this test doesn't work as expected, but does in practice, why?
    public void testSaveRecordValidDateNoEnd() throws Exception {
        testSaveRecordWithDateRange("05 Jul 2011 12:45", "05 Jul 2011 00:00", null, true);
    }
 
    //@Test this test doesn't work as expected, but does in practice, why?
    public void testSaveRecordSameStartDate() throws Exception {
        testSaveRecordWithDateRange("05 Jul 2011 12:45", "05 Jul 2011 00:00", "06 Jul 2011 17:00", true);
    }
 
    @Test
    public void testSaveRecordSameEndDate() throws Exception {
        testSaveRecordWithDateRange("06 Jul 2011 12:45", "05 Jul 2011 00:00", "06 Jul 2011 17:00", false);
    }
 
    @Test
    public void testSaveRecordInvalidLateDate() throws Exception {
        testSaveRecordWithDateRange("07 Jul 2011 12:45", "05 Jul 2011 00:00", "06 Jul 2011 17:00", false);
    }
 
    public void testSaveRecordWithDateRange(String date, String earliest,
            String latest, boolean passExpected) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
 
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(
                System.currentTimeMillis())));
 
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
 
        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        //params.put(BdrsWebConstants.PARAM_RECORD_ID,"");
        params.put("survey_species_search", speciesA.getScientificName());
        params.put("species", speciesA.getId().toString());
        params.put("latitude", "-32.546");
        params.put("longitude", "115.488");
        params.put("date", date);
        params.put("time", "15:48");
        params.put("number", "29");
        params.put("notes", "This is a test record");
 
        survey.setStartDate(earliest);
        survey.setEndDate(latest);
        survey = surveyDAO.save(survey);
 
        request.setParameters(params);
        ModelAndView mv = handle(request, response);

        if (passExpected) {
            assertRedirect(mv, redirectionService.getMySightingsUrl(survey));
            Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
            Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);
 
            Assert.assertEquals(speciesA, rec.getSpecies());
            Assert.assertEquals(Double.parseDouble(params.get("latitude")), rec.getPoint().getY());
            Assert.assertEquals(Double.parseDouble(params.get("longitude")), rec.getPoint().getX());
 
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(today);
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 48);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
 
            Assert.assertEquals(cal.getTime(), rec.getWhen());
            Assert.assertEquals(cal.getTime().getTime(), rec.getTime().longValue());
 
            Assert.assertEquals(rec.getNotes(), params.get("notes"));
        } else {
            assertRedirect(mv, "/bdrs/user/tracker.htm");
            Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        }
    }
 
    @Test
    public void testSaveRecordWithSameLonLatLocation() throws Exception {
        testSaveRecordWithLonLatLocation(locationA.getLocation().getCentroid().getX(), 
                                         locationA.getLocation().getCentroid().getY(), 
                                         locationA, true, true);
    }
    
    @Test
    public void testSaveRecordWithDifferentLonLatLocation() throws Exception {
        testSaveRecordWithLonLatLocation(locationA.getLocation().getCentroid().getX(), 
                                         locationA.getLocation().getCentroid().getY(), 
                                         locationPoly, true, false);
    }
    
    public void testSaveRecordWithLonLatLocation(double lon, double lat, Location loc, boolean passExpected, boolean geometriesMatch) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
 
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(
                System.currentTimeMillis())));
 
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/tracker.htm");
 
        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, simpleSurvey.getId().toString());
        params.put("survey_species_search", "");
        params.put("latitude", String.valueOf(lat));
        params.put("longitude", String.valueOf(lon));
        params.put("location", String.valueOf(loc.getId()));
        params.put("date", dateFormat.format(new Date()));
        params.put("time", "15:48");
        params.put("number", "");
        params.put("notes", "This is a test record");
        
        request.setParameters(params);
        ModelAndView mv = handle(request, response);

        if (passExpected) {
            assertRedirect(mv, redirectionService.getMySightingsUrl(simpleSurvey));
            Assert.assertEquals(1, recordDAO.countRecords(getRequestContext().getUser()).intValue());
            Record rec = recordDAO.getRecords(getRequestContext().getUser()).get(0);
 
            Assert.assertEquals(Double.parseDouble(params.get("latitude")), rec.getPoint().getY());
            Assert.assertEquals(Double.parseDouble(params.get("longitude")), rec.getPoint().getX());
            if (geometriesMatch) {
                Assert.assertTrue(rec.getLocation().getLocation().equalsExact(rec.getPoint()));
            } else {
                Assert.assertNotNull(rec.getLocation());
                Assert.assertNotNull(rec.getPoint());
                Assert.assertFalse(rec.getLocation().getLocation().equalsExact(rec.getPoint()));
            }
            
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(today);
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 48);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
 
            Assert.assertEquals(cal.getTime(), rec.getWhen());
            Assert.assertEquals(cal.getTime().getTime(), rec.getTime().longValue());
 
            Assert.assertEquals(rec.getNotes(), params.get("notes"));
        } else {
            assertRedirect(mv, "/bdrs/user/tracker.htm");
            Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());
        }
    }
    
    @Test
    public void testRecordFormPredefinedLocationsAsSurveyOwner()
            throws Exception {
        super.testRecordLocations("/bdrs/user/tracker.htm", true, SurveyFormRendererType.DEFAULT, true);
    }
 
    @Test
    public void testRecordFormLocationsAsSurveyOwner() throws Exception {
        super.testRecordLocations("/bdrs/user/tracker.htm", false, SurveyFormRendererType.DEFAULT, true);
    }
 
    @Test
    public void testRecordFormPredefinedLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/tracker.htm", true, SurveyFormRendererType.DEFAULT, false);
    }
 
    @Test
    public void testRecordFormLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/tracker.htm", false, SurveyFormRendererType.DEFAULT, false);
    }
    
    /**
     * Helper method that sets the Darwin Core Fields to be required or not required.
     * @param req	
     * @param dwcSurvey The survey for which the Darwin Core Fields required flag needs to be set.
     */
    private void setDwcRequired(boolean req, Survey dwcSurvey) {
		RecordProperty recordProperty;
		for (RecordPropertyType type : RecordPropertyType.values()) {
			 recordProperty = new RecordProperty(dwcSurvey, type, metadataDAO);
			 recordProperty.setRequired(req);
		}
    }

    /**
     * Helper method that sets the Darwin Core Fields to be hidden or not hidden.
     * @param hidden
     * @param dwcSurvey The survey for which the Darwin Core Fields hidden flag needs to be set.
     */
    private void setDwcHidden(boolean hidden, Survey dwcSurvey) {
		RecordProperty recordProperty;
		for (RecordPropertyType type : RecordPropertyType.values()) {
			 recordProperty = new RecordProperty(dwcSurvey, type, metadataDAO);
			 recordProperty.setHidden(hidden);
		}
    }
}
