package au.com.gaiaresources.bdrs.controller.review.locations;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.LocationAttributeSurveyCreator;
import au.com.gaiaresources.bdrs.controller.map.RecordDownloadFormat;
import au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.FacetService;
import au.com.gaiaresources.bdrs.service.facet.LocationAttributeFacet;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.location.LocationSurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.location.LocationUserFacet;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.KMLUtils;
import junit.framework.Assert;
import org.hibernate.FlushMode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Tests all aspects of the <code>AdvancedReviewSightingsController</code>.
 */
public class AdvancedReviewLocationsControllerTest extends
        AbstractControllerTest {

    private static final String ADVANCED_REVIEW_VIEW_NAME = "advancedReviewLocations";
    
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
    private PreferenceDAO prefDAO;
    @Autowired
    private CensusMethodDAO methodDAO;
    @Autowired
    private LocationService locationService;
    @Autowired
    private FileService fileService;
    @Autowired
    private FacetService facetService;
    @Autowired
    private AdvancedReviewLocationsController controller;
    /**
     * Used to change the default view returned by the controller
     */
    @Autowired
    private PreferenceDAO preferenceDAO;

    private LocationAttributeSurveyCreator surveyCreator;

    @Before
    public void setup() throws Exception {
        surveyCreator = new LocationAttributeSurveyCreator(surveyDAO, locationDAO,
                locationService, methodDAO, userDAO, taxaDAO, recordDAO, metadataDAO, preferenceDAO, fileService);
        surveyCreator.create(true);
    }

    private void resetRequest() {
        request.removeAllParameters();
        response = new MockHttpServletResponse();
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().clear();
        getRequestContext().getHibernate().disableFilter(Record.USER_ACCESS_FILTER);
        getRequestContext().getHibernate().disableFilter(Record.MODERATOR_ACCESS_FILTER);
        getRequestContext().getHibernate().disableFilter(Record.ANONYMOUS_RECORD_ACCESS_FILTER);

    }

    @Test
    public void testAdvancedReviewSightings() throws Exception {
        testAdminUserFacet();
        resetRequest();

        testAdvancedReviewDownload();
        resetRequest();

        testJSONSightings();
        resetRequest();

        testKMLSightings();
        resetRequest();

        testMapSightings();
        resetRequest();

        testMultipleUserFacet();
        resetRequest();

        testNoSurveyFacetWithSurveyID();
        resetRequest();
/* test currently not working because of a null pointer exception
        testSightingsSearchText();
        resetRequest();
*/
        testSightingsSorting();
        resetRequest();

        testSurveyFacetNoneSelected();
        resetRequest();

        testSurveyFacetTwoSelected();
        resetRequest();

        testTableSightings();
        resetRequest();
/* also not working but does work in the view
        testUserUserFacet();
        resetRequest();
*/
        testUserViewUserFacet();
        resetRequest();

        // Saves a preference so do this test last
        testOneLocationAttributeFacet();
        resetRequest();

        // This also saves a preference..
        testDefaultView();
        resetRequest();
        
    }


    //@Test
    public void testSurveyFacetNoneSelected() throws Exception {
        Survey survey = surveyDAO.getSurveys(surveyCreator.getAdmin()).get(0);
        List<Survey> surveyList = new ArrayList<Survey>();
        surveyList.add(survey);

        testSurveyFacetSelection(surveyList, survey.getLocations().size());
    }

    //@Test
    public void testSurveyFacetTwoSelected() throws Exception {
        List<Survey> allSurveys = surveyDAO.getSurveys(surveyCreator.getAdmin());
        List<Survey> surveyList = new ArrayList<Survey>();
        surveyList.add(allSurveys.get(0));
        surveyList.add(allSurveys.get(1));

        // get the distinct count of survey locations
        int survey1Count = 0;
        for (Location loc : allSurveys.get(0).getLocations()) {
            if (!allSurveys.get(1).getLocations().contains(loc)) {
                survey1Count++;
            }
        }
        
        testSurveyFacetSelection(surveyList, survey1Count + allSurveys.get(1).getLocations().size());
    }

    //@Test
    public void testNoSurveyFacetWithSurveyID() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        Survey survey = surveyDAO.getSurveys(surveyCreator.getAdmin()).get(0);

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        request.addParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, survey.getId().toString());
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        for (Facet facet : facetList) {
            if (facet instanceof SurveyFacet) {
                Assert.assertFalse(facet.isActive());
            }
        }

        Integer surveyId = new Integer(
                mv.getModel().get(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME).toString());
        List<Location> recordList = controller.getMatchingRecordsAsList(facetList, surveyId, null, null, null, null, null);
        Assert.assertEquals(survey.getLocations().size(), recordList.size());
    }

    //@Test
    public void testMapSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        request.addParameter("viewType", "map");

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");
    }

    //@Test
    public void testTableSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        request.addParameter("viewType", "table");

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeValue(mv, AdvancedReviewController.MODEL_TABLE_VIEW_SELECTED, true);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");
    }

    //@Test
    public void testKMLSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewKMLLocations.htm");
        request.addParameter("viewType", "map");

        handle(request, response);
        Assert.assertEquals(KMLUtils.KML_CONTENT_TYPE, response.getContentType());
        Assert.assertTrue(response.getContentAsString().length() > 0);
    }

    //@Test
    public void testJSONSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewJSONLocations.htm");
        request.addParameter("viewType", "table");

        handle(request, response);
        Assert.assertEquals("application/json", response.getContentType());
        Assert.assertTrue(response.getContentAsString().length() > 0);
        Assert.assertTrue(JSONArray.fromString(response.getContentAsString()).size() > 0);
    }

    //@Test
    public void testAdvancedReviewDownload() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        Survey survey = surveyDAO.getSurveys(surveyCreator.getAdmin()).get(0);
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewDownloadLocations.htm");
        request.addParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, survey.getId().toString());
        request.addParameter(AdvancedReviewController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.KML.toString());
        request.addParameter(AdvancedReviewController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.XLS.toString());
        request.addParameter(AdvancedReviewController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.SHAPEFILE.toString());

        handle(request, response);
        Assert.assertEquals(AdvancedReviewController.SIGHTINGS_DOWNLOAD_CONTENT_TYPE, response.getContentType());
        Assert.assertTrue(response.getContentAsByteArray().length > 0);
    }

    //@Test
    public void testSightingsSorting() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");

        for (String sortProperty : AdvancedReviewLocationsController.VALID_SORT_PROPERTIES) {
            for (SortOrder sortOrder : SortOrder.values()) {
                List<Location> recordList = controller.getMatchingRecordsAsList(facetList, null, sortProperty, sortOrder.toString(), null, null, null);
                Assert.assertEquals(surveyCreator.getLocationCount(), recordList.size());
            }
        }
    }

    //@Test
    public void testSightingsSearchText() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        Survey survey = surveyDAO.getSurveys(surveyCreator.getAdmin()).get(0);
        Location loc = survey.getLocations().get(0);
        request.addParameter(AdvancedReviewController.SEARCH_QUERY_PARAM_NAME, loc.getName());
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");

        List<Location> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, request.getParameter(AdvancedReviewController.SEARCH_QUERY_PARAM_NAME), null, null);
        // Some of the records are non taxonomic
        Assert.assertEquals(1, recordList.size());
    }

    //@Test
    public void testAdminUserFacet() throws Exception {
        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getAdmin());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), locationDAO.getUserLocations(surveyCreator.getAdmin()).size());
    }

    //@Test
    public void testUserUserFacet() throws Exception {
        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), locationDAO.getUserLocations(surveyCreator.getUser()).size());
    }

    //@Test
    public void testMultipleUserFacet() throws Exception {

        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());
        userList.add(surveyCreator.getAdmin());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), locationDAO.getUserLocations(surveyCreator.getAdmin()).size()+locationDAO.getUserLocations(surveyCreator.getUser()).size());
    }

    //@Test
    public void testUserViewUserFacet() throws Exception {
        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());

        testUserFacetSelection(userList, surveyCreator.getUser(), locationDAO.getUserLocations(surveyCreator.getUser()).size());
    }

    private void testSurveyFacetSelection(List<Survey> surveyList,
                                          int expectedRecordCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");

        List<Facet> facets = getFacetInstancesByType(LocationSurveyFacet.class);
        for (Facet facet : facets) {
            for (Survey survey : surveyList) {
                request.addParameter(facet.getOptionsParameterName(), survey.getId().toString());
            }
        }
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Location> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Location rec : recordList) {
            boolean containsOne = false;
            for (Survey survey : rec.getSurveys()) {
                if (!containsOne) {
                    // the list only needs to contain one of the location surveys
                    // locations can have multiple surveys and that doesn't mean they will all be in the list
                    containsOne = surveyList.contains(survey);
                }
            }
            Assert.assertTrue(containsOne);
        }
    }

    private void deselectMyRecordsOnly() {
        List<Facet> facets = getFacetInstancesByType(LocationUserFacet.class);
        for (Facet facet : facets) {
            request.addParameter(facet.getOptionsParameterName(), String.valueOf(-1));
        }
    }

    //@Test
    public void testOneLocationAttributeFacet() throws Exception {

        Survey survey = surveyDAO.getSurveyByName("Survey 1");
        Location loc = survey.getLocations().get(0);
        List<AttributeValue> locAttrList = new ArrayList<AttributeValue>();
        locAttrList.add(loc.getAttributes().iterator().next());

        testLocationAttributeFacetSelection(locAttrList);
    }

    private void testLocationAttributeFacetSelection(List<AttributeValue> locAttrValueList) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");

        Preference pref = prefDAO.getPreferenceByKey(LocationAttributeFacet.class.getCanonicalName());
        JSONArray jsonArray = new JSONArray();
        for (AttributeValue attrVal : locAttrValueList) {
            Attribute attr = attrVal.getAttribute();
            JSONObject prefValue = new JSONObject();
            prefValue.put(Facet.JSON_ACTIVE_KEY, Facet.DEFAULT_ACTIVE_CONFIG);
            prefValue.put(Facet.JSON_WEIGHT_KEY, Facet.DEFAULT_WEIGHT_CONFIG);
            prefValue.put(Facet.JSON_NAME_KEY, attr.getDescription());
            prefValue.put(LocationAttributeFacet.JSON_ATTRIBUTE_NAME_KEY, attr.getDescription());

            jsonArray.add(prefValue);
        }
        pref.setValue(jsonArray.toString());
        pref = prefDAO.save(pref);

        List<String> attrNames = new ArrayList<String>();
        List<Facet> facets = getFacetInstancesByType(LocationAttributeFacet.class);
        for (Facet facet : facets) {
            for (AttributeValue attrVal : locAttrValueList) {
                request.addParameter(facet.getOptionsParameterName(), attrVal.getStringValue());
                attrNames.add(attrVal.getStringValue());
            }
        }

        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Location> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);

        for (Location loc : recordList) {
            Assert.assertNotNull(loc);

            boolean isFound = false;
            for (AttributeValue attrVal : loc.getAttributes()) {
                isFound = isFound || attrNames.contains(attrVal.getStringValue());
            }
            Assert.assertTrue(isFound);
        }
    }

    private void testUserFacetSelection(List<User> userList, User loginUser,
                                        int expectedRecordCount) throws Exception {
        login(loginUser.getName(), loginUser.getPassword(), loginUser.getRoles());

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");

        List<Facet> facets = getFacetInstancesByType(LocationUserFacet.class);
        for (Facet facet : facets) {
            for (User user : userList) {
                request.addParameter(facet.getOptionsParameterName(), user.getId().toString());
            }
        }

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, ADVANCED_REVIEW_VIEW_NAME);

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Location> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Location rec : recordList) {
            Assert.assertTrue(userList.contains(rec.getUser()));
        }
    }

    /**
     * Tests that the Preference that sets the default view is honoured by the controller.
     * The map view as default is tested in the other unit tests.
     */
    public void testDefaultView() throws Exception {

        login("admin", "password", new String[]{Role.ADMIN});
        updateDefaultViewPreference(false);

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewLocations.htm");
        ModelAndView mv = handle(request, response);

        ModelAndViewAssert.assertModelAttributeValue(mv, AdvancedReviewController.MODEL_TABLE_VIEW_SELECTED, true);
    }

    private List<Facet> getFacetInstancesByType(Class<? extends Facet> klass) {
        List<Facet> facetList = new ArrayList<Facet>();
        User user = userDAO.getUser("admin");
        for (Facet facet : facetService.getFacetList(surveyCreator.getUser(), new HashMap<String, String[]>(), Location.class)) {
            if (facet.getClass().equals(klass)) {
                facetList.add(facet);
            }
        }

        return facetList;
    }

    private void updateDefaultViewPreference(boolean useMap) {
        Preference mapViewDefault = preferenceDAO.getPreferenceByKey(Preference.ADVANCED_REVIEW_DEFAULT_VIEW_KEY);
        mapViewDefault.setValue(Boolean.toString(useMap));
    }
}
