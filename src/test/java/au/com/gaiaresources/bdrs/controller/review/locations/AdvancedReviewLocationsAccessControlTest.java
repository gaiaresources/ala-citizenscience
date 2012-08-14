package au.com.gaiaresources.bdrs.controller.review.locations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.json.JSONArray;
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
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class AdvancedReviewLocationsAccessControlTest extends
        AbstractControllerTest {

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

    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;

    private Location loc1;
    private Location loc2;
    private Location loc3;
    private Location loc4;
    private Location loc5;
    private Location loc6;

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
        loc5.setUser(adminUser);
        locDAO.save(loc5);
        
        loc6 = new Location();
        loc6.setLocation(spatialUtil.createPoint(1, 1));
        loc6.setName("loc6");
        loc6.setUser(normalUser);
        locDAO.save(loc6);

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
        survey2.setPublic(false);
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
        r1.setRecordVisibility(RecordVisibility.OWNER_ONLY);
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

        getSession().flush();
    }
    
    
    @Test
    public void testLocationFilterUser() throws Exception {
        login(normalUser.getName(), "password", new String[] { Role.USER });
        
        request.setMethod("GET");
        request.setRequestURI(AdvancedReviewLocationsController.GET_LOCATIONS_JSON_URL);
        
        handle(request, response);
        
        JSONArray locJsonArray = JSONArray.fromString(response.getContentAsString());
        
        Assert.assertEquals("wrong size", 4, locJsonArray.size());
        
        assertId(locJsonArray, loc1.getId());
        assertId(locJsonArray, loc3.getId());
        assertId(locJsonArray, loc4.getId());
        assertId(locJsonArray, loc6.getId());
    }
    
    @Test
    public void testLocationFilterAdmin() throws Exception {
        login(adminUser.getName(), "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(AdvancedReviewLocationsController.GET_LOCATIONS_JSON_URL);
        
        this.handle(request, response);
        
        JSONArray locJsonArray = JSONArray.fromString(response.getContentAsString());
        
        Assert.assertEquals("wrong size", 6, locJsonArray.size());
        assertId(locJsonArray, loc1.getId());
        assertId(locJsonArray, loc2.getId());
        assertId(locJsonArray, loc3.getId());
        assertId(locJsonArray, loc4.getId());
        assertId(locJsonArray, loc5.getId());
        assertId(locJsonArray, loc6.getId());
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
    
    private void assertId(JSONArray array, int id) {
        // return if found else throw assertion.
        for (int i=0; i<array.size(); ++i) {
            JSONObject obj = array.getJSONObject(i);
            if (obj.getInt("id") == id) {
                return;
            }
        }
        Assert.fail("could not find id : " + id);
    }
}
