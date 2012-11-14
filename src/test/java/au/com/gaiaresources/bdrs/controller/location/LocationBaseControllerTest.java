package au.com.gaiaresources.bdrs.controller.location;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractFormField;
import au.com.gaiaresources.bdrs.controller.map.WebMap;
import au.com.gaiaresources.bdrs.controller.record.RecordWebFormContext;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class LocationBaseControllerTest extends AbstractControllerTest {
    
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    
    private Survey simpleSurvey;
    private Survey locAttSurvey;
    private List<Attribute> locationAttributes = new ArrayList<Attribute>();
    
    private Logger log = Logger.getLogger(getClass());
    
    private IndicatorSpecies species1;
    
    @Before
    public void setUp() throws Exception {
    	
    	TaxonGroup taxonGroup = new TaxonGroup();
    	taxonGroup.setName("test taxon group");
    	taxaDAO.save(taxonGroup);
    	
    	species1 = new IndicatorSpecies();
    	species1.setTaxonGroup(taxonGroup);
    	species1.setScientificName("species one");
    	species1.setCommonName("common one");
    	species1.setTaxonRank(TaxonRank.SPECIALFORM);
    	taxaDAO.save(species1);
    	
        simpleSurvey = new Survey();
        simpleSurvey.setName("Simple Survey");
        simpleSurvey.setActive(true);
        simpleSurvey.setStartDate(new Date());
        simpleSurvey.setDescription("Simple Test Survey");
        Metadata md2 = simpleSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md2);
        simpleSurvey = surveyDAO.save(simpleSurvey);
        
        locAttSurvey = new Survey();
        locAttSurvey.setName("Location Attributes Survey");
        locAttSurvey.setActive(true);
        locAttSurvey.setStartDate(new Date());
        locAttSurvey.setDescription("Location Attributes Test Survey");
        Metadata md3 = locAttSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md3);
        
        // create a census method to use for census method attribute types
        createCensusMethodForAttributes();
        
        // create location attributes for the survey
        locAttSurvey.setAttributes(createAttrList("", true, AttributeScope.LOCATION, true, false));
        
        locAttSurvey = surveyDAO.save(locAttSurvey);
    }

    @Test
    public void testListSurveyLocations() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/locationListing.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(simpleSurvey.getId()));

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "locationListing");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
    }

    @Test
    public void testAddLocation() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/editLocation.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(simpleSurvey.getId()));
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "surveyEditLocation");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeValue(mv, RecordWebFormContext.MODEL_EDIT, true);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.MV_WEB_MAP);
        Assert.assertTrue("wrong class", mv.getModel().get(BdrsWebConstants.MV_WEB_MAP) instanceof WebMap);
    }

    /**
     * Tests the basic use case of creating a new location and clicking save.
     */
    @Test
    public void testAddLocationSubmitWgs84() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/editLocation.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(simpleSurvey.getId()));
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("survey", String.valueOf(simpleSurvey.getId()));
        params.put("locationName", "Test Location 1234");
        params.put("locationDescription", "This is a test location");
        params.put("location_WKT", "POINT(115.77240371813 -31.945572001881)");
        params.put("latitude", "-31.945572001881");
        params.put("longitude", "115.77240371813");
        params.put(BdrsWebConstants.PARAM_SRID, "4326");

        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        assertUrlEquals("/bdrs/admin/survey/locationListing.htm", redirect.getUrl());
        
        Location location = locationDAO.getLocationByName(simpleSurvey.getName(), params.get("locationName"));
        Assert.assertEquals(location.getName(), params.get("locationName"));
        Assert.assertEquals(location.getDescription(), params.get("locationDescription"));
        // ignore the wkt right now because going in it is POINT[](x,y), but coming out it is POINT[ ](x,y)
        //Assert.assertEquals(location.getLocation().toText(), params.get("location_WKT"));
        Assert.assertEquals(String.valueOf(location.getY()), params.get("latitude"));
        Assert.assertEquals(String.valueOf(location.getX()), params.get("longitude"));
        Assert.assertEquals("wrong srid", 4326, location.getLocation().getSRID());
    }
    
    /**
     * Tests the basic use case of creating a new location and clicking save.
     */
    @Test
    public void testAddLocationSubmitMga50() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/editLocation.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(simpleSurvey.getId()));
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("survey", String.valueOf(simpleSurvey.getId()));
        params.put("locationName", "Test Location 1234");
        params.put("locationDescription", "This is a test location");
        params.put("location_WKT", "POINT(550000.123456 7500000.123456)");
        params.put("latitude", "7500000.123456");
        params.put("longitude", "550000.123456");
        params.put(BdrsWebConstants.PARAM_SRID, "28350");

        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        assertUrlEquals("/bdrs/admin/survey/locationListing.htm", redirect.getUrl());
        
        Location location = locationDAO.getLocationByName(simpleSurvey.getName(), params.get("locationName"));
        Assert.assertEquals(location.getName(), params.get("locationName"));
        Assert.assertEquals(location.getDescription(), params.get("locationDescription"));
        // ignore the wkt right now because going in it is POINT[](x,y), but coming out it is POINT[ ](x,y)
        //Assert.assertEquals(location.getLocation().toText(), params.get("location_WKT"));
        Assert.assertEquals(String.valueOf(location.getY()), params.get("latitude"));
        Assert.assertEquals(String.valueOf(location.getX()), params.get("longitude"));
        Assert.assertEquals("wrong srid", 28350, location.getLocation().getSRID());
    }
    
    @Test
    public void testAddLocationAttributes() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/editLocation.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(locAttSurvey.getId()));
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "surveyEditLocation");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "locationFormFieldList");

        int curWeight = Integer.MIN_VALUE;
        Assert.assertEquals(locAttSurvey, mv.getModelMap().get("survey"));
        List<AbstractFormField> formFieldList = (List<AbstractFormField>) mv.getModelMap().get("locationFormFieldList");
        Assert.assertEquals(locAttSurvey.getAttributes().size(), formFieldList.size());
        for (AbstractFormField formField : formFieldList) {
            // Test attributes are sorted by weight
            Assert.assertTrue(formField.getWeight() >= curWeight);
            curWeight = formField.getWeight();
            Assert.assertTrue(formField.isAttributeFormField());
        }
        ModelAndViewAssert.assertModelAttributeValue(mv, RecordWebFormContext.MODEL_EDIT, true);
    }
    
    /**
     * Tests the basic use case of creating a new location and clicking save.
     */
    @Test
    public void testAddLocationAttributesSubmit() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/editLocation.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(locAttSurvey.getId()));
        request.setParameter(BdrsWebConstants.PARAM_SRID, "4326");
        
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2000, 1, 1);
        Date testDate = cal.getTime();
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("survey", String.valueOf(locAttSurvey.getId()));
        params.put("locationName", "Test Location 1234");
        params.put("locationDescription", "This is a test location");
        params.put("location_WKT", "POINT(115.77240371813 -31.945572001881)");
        params.put("latitude", "-31.945572001881");
        params.put("longitude", "115.77240371813");
        params.putAll(createAttributes(locAttSurvey, testDate));
        
        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        assertUrlEquals("/bdrs/admin/survey/locationListing.htm", redirect.getUrl());
        
        Location location = locationDAO.getLocationByName(locAttSurvey.getName(), params.get("locationName"));
        Assert.assertEquals(location.getName(), params.get("locationName"));
        Assert.assertEquals(location.getDescription(), params.get("locationDescription"));
        // ignore the wkt right now because going in it is POINT[](x,y), but coming out it is POINT[ ](x,y)
        //Assert.assertEquals(location.getLocation().toText(), params.get("location_WKT"));
        Assert.assertEquals(String.valueOf(location.getY()), params.get("latitude"));
        Assert.assertEquals(String.valueOf(location.getX()), params.get("longitude"));

        String key;
        for (TypedAttributeValue recAttr : location.getAttributes()) {
            Attribute attr = recAttr.getAttribute();
            if (locAttSurvey.getAttributes().contains(recAttr.getAttribute())) {
                key = WebFormAttributeParser.getParamKey("", attr);
            } else {
                Assert.assertFalse(true);
                key = null;
            }
            assertAttributes(recAttr, params, key);
        }
    }
    
    private Map<String, String> createAttributes(Survey survey, Date testDate) {
        return createLocationAttributeValues(survey, "123", new SimpleDateFormat("dd MMM yyyy"), testDate);
    }
    
    private Map<String, String> createLocationAttributeValues(Survey survey, String intWithRangeValue, DateFormat dateFormat, Date testDate) {
        Map<String, String> params = new HashMap<String,String>();
        int seed = 0;
        for (Attribute attr : survey.getAttributes()) {
            if(AttributeScope.LOCATION.equals(attr.getScope())) {
                genRandomAttributeValue(attr, seed++, null, "", params);
            }
        }
        return params;
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
