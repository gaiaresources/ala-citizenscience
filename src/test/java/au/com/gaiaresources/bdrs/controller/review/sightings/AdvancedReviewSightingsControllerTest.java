package au.com.gaiaresources.bdrs.controller.review.sightings;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.LocationAttributeSurveyCreator;
import au.com.gaiaresources.bdrs.controller.map.RecordDownloadFormat;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.*;
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

import java.util.*;

/**
 * Tests all aspects of the <code>AdvancedReviewSightingsController</code>.
 */
public class AdvancedReviewSightingsControllerTest extends
        AbstractControllerTest {

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
    private AdvancedReviewSightingsController controller;
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

        testAllPublicRecordsUserFacetSelection();
        resetRequest();

        testAnonymousView();
        resetRequest();

        testCensusMethodObservationType();
        resetRequest();

        testCensusMethodOneType();
        resetRequest();

        testCensusMethodTwoTypes();
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

        testOneMonthFacet();
        resetRequest();

        testOneTaxonGroupFacet();
        resetRequest();

        testSightingsSearchText();
        resetRequest();

        testSightingsSorting();
        resetRequest();

        testSurveyFacetNoneSelected();
        resetRequest();

        testSurveyFacetTwoSelected();
        resetRequest();

        testTableSightings();
        resetRequest();

        testTwoMonthFacet();
        resetRequest();

        testTwoTaxonGroupFacet();
        resetRequest();

        testUserUserFacet();
        resetRequest();

        testUserViewUserFacet();
        resetRequest();

        // These next two tests add OWNER_ONLY records
        testPublicRecordAccess();
        resetRequest();

        testPrivateRecordAccess();
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

        testSurveyFacetSelection(surveyList, surveyCreator.getSurveyRecordCount());
    }

    //@Test
    public void testSurveyFacetTwoSelected() throws Exception {
        List<Survey> allSurveys = surveyDAO.getSurveys(surveyCreator.getAdmin());
        List<Survey> surveyList = new ArrayList<Survey>();
        surveyList.add(allSurveys.get(0));
        surveyList.add(allSurveys.get(1));

        testSurveyFacetSelection(surveyList, 2 * surveyCreator.getSurveyRecordCount());
    }

    //@Test
    public void testNoSurveyFacetWithSurveyID() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        Survey survey = surveyDAO.getSurveys(surveyCreator.getAdmin()).get(0);

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        request.addParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, survey.getId().toString());
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

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
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, surveyId, null, null, null, null, null);
        Assert.assertEquals(surveyCreator.getSurveyRecordCount(), recordList.size());
    }

    //@Test
    public void testMapSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        request.addParameter("viewType", "map");

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

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
        request.setRequestURI("/review/sightings/advancedReview.htm");
        request.addParameter("viewType", "table");

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeValue(mv, AdvancedReviewSightingsController.MODEL_TABLE_VIEW_SELECTED, true);
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
        request.setRequestURI("/review/sightings/advancedReviewKMLSightings.htm");
        request.addParameter("viewType", "map");

        handle(request, response);
        Assert.assertEquals(KMLUtils.KML_CONTENT_TYPE, response.getContentType());
        Assert.assertTrue(response.getContentAsString().length() > 0);
    }

    //@Test
    public void testJSONSightings() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewJSONSightings.htm");
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
        request.setRequestURI("/review/sightings/advancedReviewDownload.htm");
        request.addParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, survey.getId().toString());
        request.addParameter(AdvancedReviewSightingsController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.KML.toString());
        request.addParameter(AdvancedReviewSightingsController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.XLS.toString());
        request.addParameter(AdvancedReviewSightingsController.QUERY_PARAM_DOWNLOAD_FORMAT, RecordDownloadFormat.SHAPEFILE.toString());

        handle(request, response);
        Assert.assertEquals(AdvancedReviewSightingsController.SIGHTINGS_DOWNLOAD_CONTENT_TYPE, response.getContentType());
        Assert.assertTrue(response.getContentAsByteArray().length > 0);
    }

    //@Test
    public void testSightingsSorting() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");

        for (String sortProperty : AdvancedReviewSightingsController.VALID_SORT_PROPERTIES) {
            for (SortOrder sortOrder : SortOrder.values()) {
                List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, sortProperty, sortOrder.toString(), null, null, null);
                Assert.assertEquals(surveyCreator.getRecordCount(), recordList.size());
            }
        }
    }

    //@Test
    public void testSightingsSearchText() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        request.addParameter(AdvancedReviewSightingsController.SEARCH_QUERY_PARAM_NAME, surveyCreator.getSpeciesA().getScientificName());
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");

        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, request.getParameter(AdvancedReviewSightingsController.SEARCH_QUERY_PARAM_NAME), null, null);
        // Some of the records are non taxonomic
        Assert.assertEquals((surveyCreator.getMethodRecordCount() * 3) / 3, recordList.size());
    }

    //@Test
    public void testCensusMethodOneType() throws Exception {
        List<CensusMethod> methodList = new ArrayList<CensusMethod>();
        methodList.add(surveyCreator.getMethodA());
        methodList.add(surveyCreator.getMethodB());

        testCensusMethodSelection(methodList, 2 * surveyCreator.getMethodRecordCount());
    }

    //@Test
    public void testCensusMethodTwoTypes() throws Exception {
        List<CensusMethod> methodList = new ArrayList<CensusMethod>();
        methodList.add(surveyCreator.getMethodA());
        methodList.add(surveyCreator.getMethodB());
        methodList.add(surveyCreator.getMethodC());

        testCensusMethodSelection(methodList, 3 * surveyCreator.getMethodRecordCount());
    }

    //@Test
    public void testCensusMethodObservationType() throws Exception {
        List<CensusMethod> methodList = new ArrayList<CensusMethod>();
        methodList.add(surveyCreator.getMethodA());
        methodList.add(surveyCreator.getMethodB());
        methodList.add(surveyCreator.getMethodC());
        methodList.add(null);

        testCensusMethodSelection(methodList, 4 * surveyCreator.getMethodRecordCount());
    }

    //@Test
    public void testOneTaxonGroupFacet() throws Exception {

        List<TaxonGroup> groupList = new ArrayList<TaxonGroup>();
        groupList.add(surveyCreator.getTaxonGroupBirds());

        testTaxonGroupFacetSelection(groupList, 2 * surveyCreator.getTaxonRecordCount());
    }

    //@Test
    public void testTwoTaxonGroupFacet() throws Exception {

        List<TaxonGroup> groupList = new ArrayList<TaxonGroup>();
        groupList.add(surveyCreator.getTaxonGroupBirds());
        groupList.add(surveyCreator.getTaxonGroupFrogs());

        testTaxonGroupFacetSelection(groupList, 3 * surveyCreator.getTaxonRecordCount());
    }

    //@Test
    public void testOneMonthFacet() throws Exception {
        List<Long> dateList = new ArrayList<Long>();
        Calendar cal = new GregorianCalendar();
        for (Date d : new Date[]{surveyCreator.getDateA()}) {
            cal.setTime(d);
            dateList.add(Integer.valueOf(cal.get(Calendar.MONTH)).longValue() + 1);
        }

        testMonthFacetSelection(dateList, surveyCreator.getRecordCount() / 2);
    }

    //@Test
    public void testTwoMonthFacet() throws Exception {

        List<Long> dateList = new ArrayList<Long>();
        Calendar cal = new GregorianCalendar();
        for (Date d : new Date[]{surveyCreator.getDateA(), surveyCreator.getDateB()}) {
            cal.setTime(d);
            dateList.add(Integer.valueOf(cal.get(Calendar.MONTH)).longValue() + 1);
        }

        testMonthFacetSelection(dateList, surveyCreator.getRecordCount());
    }

    //@Test
    public void testAdminUserFacet() throws Exception {

        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getAdmin());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), surveyCreator.getRecordCount() / 2);
    }

    //@Test
    public void testUserUserFacet() throws Exception {

        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), surveyCreator.getUserRecordCount().get(surveyCreator.getUser()));
    }

    //@Test
    public void testMultipleUserFacet() throws Exception {

        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());
        userList.add(surveyCreator.getAdmin());

        testUserFacetSelection(userList, surveyCreator.getAdmin(), surveyCreator.getUserRecordCount().get(surveyCreator.getUser())
                + surveyCreator.getUserRecordCount().get(surveyCreator.getAdmin()));
    }

    //@Test
    public void testUserViewUserFacet() throws Exception {
        List<User> userList = new ArrayList<User>();
        userList.add(surveyCreator.getUser());

        testUserFacetSelection(userList, surveyCreator.getUser(), surveyCreator.getUserRecordCount().get(surveyCreator.getUser()));
    }

    private void testCensusMethodSelection(List<CensusMethod> methodList,
                                           int expectedCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        Set<String> methodTypes = new HashSet<String>();
        for (CensusMethod method : methodList) {
            if (method == null) {
                methodTypes.add(CensusMethodTypeFacetOption.NOCENSUSMETHODVALUE);
            } else {
                methodTypes.add(method.getType());
            }
        }

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        List<Facet> facets = getFacetInstancesByType(CensusMethodTypeFacet.class);
        for (Facet facet : facets) {
            for (String type : methodTypes) {
                request.addParameter(facet.getInputName(), type);
            }
        }
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedCount, recordList.size());

        for (Record rec : recordList) {
            if (rec.getCensusMethod() != null) {
                Assert.assertTrue(methodTypes.contains(rec.getCensusMethod().getType()));
            }
        }
    }

    private void testSurveyFacetSelection(List<Survey> surveyList,
                                          int expectedRecordCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        List<Facet> facets = getFacetInstancesByType(SurveyFacet.class);
        for (Facet facet : facets) {
            for (Survey survey : surveyList) {
                request.addParameter(facet.getInputName(), survey.getId().toString());
            }
        }
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Record rec : recordList) {
            Assert.assertTrue(surveyList.contains(rec.getSurvey()));
        }
    }

    private void deselectMyRecordsOnly() {
        List<Facet> facets = getFacetInstancesByType(UserFacet.class);
        for (Facet facet : facets) {
            request.addParameter(facet.getInputName(), String.valueOf(-1));
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
        request.setRequestURI("/review/sightings/advancedReview.htm");

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
                request.addParameter(facet.getInputName(), attrVal.getStringValue());
                attrNames.add(attrVal.getStringValue());
            }
        }

        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);

        for (Record rec : recordList) {
            Location loc = rec.getLocation();
            Assert.assertNotNull(loc);

            boolean isFound = false;
            for (AttributeValue attrVal : loc.getAttributes()) {
                isFound = isFound || attrNames.contains(attrVal.getStringValue());
            }
            Assert.assertTrue(isFound);
        }
    }

    private void testTaxonGroupFacetSelection(List<TaxonGroup> groupList,
                                              int expectedRecordCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        List<Facet> facets = getFacetInstancesByType(TaxonGroupFacet.class);
        for (Facet facet : facets) {
            for (TaxonGroup group : groupList) {
                request.addParameter(facet.getInputName(), group.getId().toString());
            }
        }
        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Record rec : recordList) {
            if (rec.getSpecies() != null) {
                Assert.assertTrue(groupList.contains(rec.getSpecies().getTaxonGroup()));
            }
        }
    }

    private void testUserFacetSelection(List<User> userList, User loginUser,
                                        int expectedRecordCount) throws Exception {
        login(loginUser.getName(), loginUser.getPassword(), loginUser.getRoles());

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        List<Facet> facets = getFacetInstancesByType(UserFacet.class);
        for (Facet facet : facets) {
            for (User user : userList) {
                request.addParameter(facet.getInputName(), user.getId().toString());
            }
        }

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Record rec : recordList) {
            Assert.assertTrue(userList.contains(rec.getUser()));
        }
    }

    private void testMonthFacetSelection(List<Long> monthlist,
                                         int expectedRecordCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        Calendar cal = new GregorianCalendar();

        Set<Integer> monthSet = new HashSet<Integer>(monthlist.size());
        List<Facet> facets = getFacetInstancesByType(MonthFacet.class);
        for (Facet facet : facets) {
            for (Long date : monthlist) {
                request.addParameter(facet.getInputName(), date.toString());
                monthSet.add(date.intValue());
            }
        }

        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(expectedRecordCount, recordList.size());

        for (Record rec : recordList) {
            cal.setTime(rec.getWhen());
            Integer monthInt = cal.get(Calendar.MONTH);
            Assert.assertTrue(monthSet.contains(Integer.valueOf(monthInt + 1)));
        }
    }

    //@Test
    public void testPublicRecord() throws Exception {
        List<Long> dateList = new ArrayList<Long>();
        Calendar cal = new GregorianCalendar();
        for (Date d : new Date[]{surveyCreator.getDateA()}) {
            cal.setTime(d);
            dateList.add(Integer.valueOf(cal.get(Calendar.MONTH)).longValue() + 1);
        }

        testMonthFacetSelection(dateList, surveyCreator.getRecordCount() / 2);
    }


    /**
     * Inserts an OWNER_ONLY record and ensures it and only it is returned when the VisibilityFacet is configured
     * to return OWNER_ONLY records.
     *
     * @throws Exception if there is an error running the test.
     */
    public void testPrivateRecordAccess() throws Exception {

        Survey survey = surveyDAO.getSurveyByName("Survey 1");
        Record record = surveyCreator.createRecord(survey, surveyCreator.getMethodA(), survey.getLocations().get(0), surveyCreator.getSpeciesA(), surveyCreator.getAdmin(), RecordVisibility.OWNER_ONLY);

        getRequestContext().getHibernate().flush();

        // The reason there are 2 owner_only records is the testPublicRecordAccess test also inserts one.
        testVisibilityFacetSelection(RecordVisibility.OWNER_ONLY, 2);
    }

    /**
     * Inserts an OWNER_ONLY record and ensures it is not returned when the VisibilityFacet is configured
     * to return OWNER_ONLY records.
     *
     * @throws Exception if there is an error running the test.
     */
    public void testPublicRecordAccess() throws Exception {

        Survey survey = surveyDAO.getSurveyByName("Survey 1");
        Record record = surveyCreator.createRecord(survey, surveyCreator.getMethodA(), survey.getLocations().get(0), surveyCreator.getSpeciesA(), surveyCreator.getAdmin(), RecordVisibility.OWNER_ONLY);

        getRequestContext().getHibernate().flush();

        testVisibilityFacetSelection(RecordVisibility.PUBLIC, surveyCreator.getRecordCount());
    }

    /**
     * Tests that the Preference that sets the default view is honoured by the controller.
     * The map view as default is tested in the other unit tests.
     */
    public void testDefaultView() throws Exception {

        login("admin", "password", new String[]{Role.ADMIN});
        updateDefaultViewPreference(false);

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        ModelAndView mv = handle(request, response);

        ModelAndViewAssert.assertModelAttributeValue(mv, AdvancedReviewSightingsController.MODEL_TABLE_VIEW_SELECTED, true);
    }

    private void testVisibilityFacetSelection(RecordVisibility visibility, int expectedCount) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        Facet facet = getFacetInstancesByType(VisibilityFacet.class).get(0);
        request.addParameter(facet.getInputName(), Integer.toString(visibility.ordinal()));

        deselectMyRecordsOnly();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);

        Assert.assertEquals(expectedCount, recordList.size());
        for (Record rec : recordList) {
            Assert.assertEquals(visibility, rec.getRecordVisibility());
        }
    }

    private List<Facet> getFacetInstancesByType(Class<? extends Facet> klass) {
        List<Facet> facetList = new ArrayList<Facet>();
        User user = userDAO.getUser("admin");
        for (Facet facet : facetService.getFacetList(surveyCreator.getUser(), new HashMap<String, String[]>())) {
            if (facet.getClass().equals(klass)) {
                facetList.add(facet);
            }
        }

        return facetList;
    }

    //@Test
    public void testAnonymousView() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");
        deselectMyRecordsOnly();

        Calendar cal = new GregorianCalendar();

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(surveyCreator.getRecordCount(), recordList.size());
    }

    //@Test
    public void testAllPublicRecordsUserFacetSelection() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReview.htm");

        List<Facet> facets = getFacetInstancesByType(UserFacet.class);
        for (Facet facet : facets) {
            request.addParameter(facet.getInputName(), String.valueOf(-1));
        }

        ModelAndView mv = handle(request, response);
        getRequestContext().getHibernate().flush();
        getRequestContext().getHibernate().setFlushMode(FlushMode.AUTO);
        ModelAndViewAssert.assertViewName(mv, "advancedReview");

        ModelAndViewAssert.assertModelAttributeAvailable(mv, "mapViewSelected");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, BdrsWebConstants.PARAM_SURVEY_ID);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortBy");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "sortOrder");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "searchText");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "facetList");

        List<Facet> facetList = (List<Facet>) mv.getModel().get("facetList");
        List<Record> recordList = controller.getMatchingRecordsAsList(facetList, null, null, null, null, null, null);
        Assert.assertEquals(surveyCreator.getRecordCount(), recordList.size());
    }

    private void updateDefaultViewPreference(boolean useMap) {
        Preference mapViewDefault = preferenceDAO.getPreferenceByKey(Preference.ADVANCED_REVIEW_DEFAULT_VIEW_KEY);
        mapViewDefault.setValue(Boolean.toString(useMap));
    }
}
