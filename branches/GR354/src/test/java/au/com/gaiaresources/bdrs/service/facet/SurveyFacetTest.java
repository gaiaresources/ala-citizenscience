package au.com.gaiaresources.bdrs.service.facet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.review.sightings.AdvancedReviewSightingsController;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.location.LocationSurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.record.RecordSurveyFacet;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Tests the survey facets in conjunction with the Record filters
 *
 */
public class SurveyFacetTest extends AbstractControllerTest {

    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private LocationDAO locDAO;

    private TaxonGroup g1;
    private TaxonGroup g2;
    private TaxonGroup g3;

    private IndicatorSpecies s1;
    private IndicatorSpecies s2;
    private IndicatorSpecies s3;

    private Survey survey1;
    private Survey survey2;
    private Survey survey3;
    private Survey survey4;
    private Survey survey5;

    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;
    private Record r5;

    private Location loc1;
    private Location loc2;
    private Location loc3;
    private Location loc4;
    private Location loc5;

    private User adminUser;
    private User normalUser;

    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    private Logger log = Logger.getLogger(getClass());

    @Before
    public void setup() {

        adminUser = userDAO.getUser("admin");

        normalUser = userDAO.createUser("normal", "normalfirst", "normallast", "normal@normal.com", "password", "regkey", new String[] { Role.USER });

        loc1 = new Location();
        loc1.setLocation(spatialUtil.createPoint(1, 1));
        loc1.setName("loc 1");
        locDAO.save(loc1);

        loc2 = new Location();
        loc2.setLocation(spatialUtil.createPoint(1, 1));
        loc2.setName("loc 2");
        locDAO.save(loc2);

        loc3 = new Location();
        loc3.setLocation(spatialUtil.createPoint(1, 1));
        loc3.setName("loc 3");
        locDAO.save(loc3);

        loc4 = new Location();
        loc4.setLocation(spatialUtil.createPoint(1, 1));
        loc4.setName("loc 4");
        locDAO.save(loc4);
        
        loc5 = new Location();
        loc5.setLocation(spatialUtil.createPoint(1, 1));
        loc5.setName("loc 5");
        locDAO.save(loc5);

        survey1 = new Survey();
        survey1.setName("survey one");
        survey1.setPublicReadAccess(true);
        survey1.setPublic(false);
        addLocationToSurvey(survey1, loc1);
        addUserToSurvey(survey1, adminUser);
        addUserToSurvey(survey1, normalUser);
        surveyDAO.save(survey1);

        survey2 = new Survey();
        survey2.setName("survey two");
        survey2.setPublicReadAccess(false);
        survey2.setPublic(true);
        addLocationToSurvey(survey2, loc2);
        surveyDAO.save(survey2);

        survey3 = new Survey();
        survey3.setName("survey three");
        survey3.setPublicReadAccess(false);
        survey3.setPublic(false);
        addUserToSurvey(survey3, normalUser);
        addLocationToSurvey(survey3, loc3);
        surveyDAO.save(survey3);

        survey4 = new Survey();
        survey4.setName("survey four");
        survey4.setPublicReadAccess(true);
        survey4.setPublic(true);
        addLocationToSurvey(survey4, loc4);
        surveyDAO.save(survey4);
        
        survey5 = new Survey();
        survey5.setName("survey five");
        survey5.setPublicReadAccess(false);
        survey5.setPublic(false);
        addLocationToSurvey(survey5, loc5);
        addUserToSurvey(survey5, adminUser);
        surveyDAO.save(survey5);

        g1 = new TaxonGroup();
        g1.setName("group one");
        taxaDAO.save(g1);
        g2 = new TaxonGroup();
        g2.setName("group two");
        taxaDAO.save(g2);
        g3 = new TaxonGroup();
        g3.setName("group three");
        taxaDAO.save(g3);

        s1 = new IndicatorSpecies();
        s1.setTaxonGroup(g1);
        s1.setScientificName("species one");
        s1.setCommonName("common one");
        taxaDAO.save(s1);

        s2 = new IndicatorSpecies();
        s2.setTaxonGroup(g2);
        s2.setScientificName("species two");
        s2.setCommonName("common two");
        taxaDAO.save(s2);

        s3 = new IndicatorSpecies();
        s3.setTaxonGroup(g3);
        s3.setScientificName("species three");
        s3.setCommonName("common three");
        taxaDAO.save(s3);

        r1 = new Record();
        r1.setSpecies(s1);
        r1.setSurvey(survey1);
        r1.setUser(adminUser);
        r1.setRecordVisibility(RecordVisibility.PUBLIC);
        recordDAO.save(r1);

        r2 = new Record();
        r2.setSpecies(s2);
        r2.setSurvey(survey2);
        r2.setUser(adminUser);
        r2.setRecordVisibility(RecordVisibility.PUBLIC);
        recordDAO.save(r2);

        r3 = new Record();
        r3.setSurvey(survey3);
        r3.setUser(adminUser);
        r3.setRecordVisibility(RecordVisibility.PUBLIC);
        recordDAO.save(r3);

        r4 = new Record();
        r4.setSurvey(survey4);
        r4.setUser(adminUser);
        r4.setRecordVisibility(RecordVisibility.PUBLIC);
        recordDAO.save(r4);
        
        r5 = new Record();
        r5.setSurvey(survey5);
        r5.setUser(adminUser);
        r5.setRecordVisibility(RecordVisibility.PUBLIC);
        recordDAO.save(r5);

        getSession().flush();
    }

    @After
    public void teardown() {
        // we should get a new session at the start of every test.
    }

    @Test
    public void testAnonRecordSurveyFacet() {
        FilterManager.enableRecordFilter(getSession(), null);
        
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        RecordSurveyFacet facet = new RecordSurveyFacet("survey",
                recordDAO, paramMap, null, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 3, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey4);
        
        // each survey should have 1 record.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }
    }
    
    @Test
    public void testNormalLoginRecordSurveyFacet() {
        FilterManager.enableRecordFilter(getSession(), normalUser);
        
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        RecordSurveyFacet facet = new RecordSurveyFacet("survey",
                recordDAO, paramMap, normalUser, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 4, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey3);
        assertSurveyName(facet.getFacetOptions(), survey4);
        
        // each survey should have 1 record.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }
    }
    
    @Test
    public void testAdminLoginRecordSurveyFacet() {
        FilterManager.enableRecordFilter(getSession(), adminUser);
        
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        RecordSurveyFacet facet = new RecordSurveyFacet("survey",
                recordDAO, paramMap, adminUser, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 5, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey3);
        assertSurveyName(facet.getFacetOptions(), survey4);
        assertSurveyName(facet.getFacetOptions(), survey5);
        
        // each survey should have 1 record.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }
    }

    @Test
    public void testAnonLocationRecordFacet() {
        FilterManager.enableLocationFilter(getSession(), null);
        // no login
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        LocationSurveyFacet facet = new LocationSurveyFacet("survey",
                locDAO, paramMap, null, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 3, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey4);
        
        // each survey should have 1 location.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }
    }

    @Test
    public void testAdminLoginLocationRecordFacet() {
        FilterManager.enableLocationFilter(getSession(), adminUser);
        // no login
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        LocationSurveyFacet facet = new LocationSurveyFacet("survey",
                locDAO, paramMap, adminUser, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 5, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey3);
        assertSurveyName(facet.getFacetOptions(), survey4);
        assertSurveyName(facet.getFacetOptions(), survey5);
        
        // each survey should have 1 location.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }
    }

    @Test
    public void testFacetNormalLoginLocationRecord() {
        FilterManager.enableLocationFilter(getSession(), normalUser);
        // no login
        Map<String, String[]> paramMap = new HashMap<String, String[]>();
        LocationSurveyFacet facet = new LocationSurveyFacet("survey",
                locDAO, paramMap, normalUser, new JSONObject());

        Assert.assertEquals("wrong number of returned surveys", 4, facet.getFacetOptions().size());
        
        assertSurveyName(facet.getFacetOptions(), survey1);
        assertSurveyName(facet.getFacetOptions(), survey2);
        assertSurveyName(facet.getFacetOptions(), survey3);
        assertSurveyName(facet.getFacetOptions(), survey4);

        // each survey should have 1 location.
        for (FacetOption opt : facet.getFacetOptions()) {
            Assert.assertEquals("wrong record count", 1, opt.getCount().intValue());
        }

    }

    private void addLocationToSurvey(Survey survey, Location loc) {
        List<Location> locList = new ArrayList<Location>();
        locList.addAll(survey.getLocations());
        locList.add(loc);
        survey.setLocations(locList);
    }

    private void addUserToSurvey(Survey survey, User user) {
        Set<User> userSet = new HashSet<User>();
        userSet.addAll(survey.getUsers());
        userSet.add(user);
        survey.setUsers(userSet);
    }
    
    private void assertSurveyName(List<FacetOption> optionList, Survey expectedSurvey) {
        for (FacetOption fo : optionList) {
            if (fo.getDisplayName().equals(expectedSurvey.getName())) {
                return;
            }
        }
        Assert.fail("Did not find survey name in facet option list : " + expectedSurvey.getName());
    }
}
