package au.com.gaiaresources.bdrs.model.record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class RecordDAOImplFilterTest extends AbstractTransactionalTest {

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
    @Autowired
    private GroupDAO groupDAO;

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
    
    private Record r6;
    private Record r7;
    private Record r8;
    private Record r9;

    private Location loc1;
    private Location loc2;
    private Location loc3;
    private Location loc4;
    private Location loc5;

    private User adminUser;
    private User normalUser;
    private User moderatorUser;
    private User groupUser;
    
    private Group group;

    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    private Logger log = Logger.getLogger(getClass());

    @Before
    public void setup() {

        adminUser = userDAO.getUser("admin");

        normalUser = userDAO.createUser("normal", "normalfirst", "normallast", "normal@normal.com", "password", "regkey", new String[] { Role.USER });
        
        // see User.isModerator()
        moderatorUser = userDAO.createUser("mod", "modfirst", "modlast", "mod@mod.com", "password", "regkey", new String[] { Role.SUPERVISOR });
        
        groupUser = userDAO.createUser("group", "groupfirst", "grouplast", "group@group.com", "password", "regkey", new String[] { Role.USER });
        
        group = new Group();
        group.setName("user group 1");
        Set<User> groupUserSet = new HashSet<User>();
        groupUserSet.add(groupUser);
        group.setUsers(groupUserSet);
        groupDAO.save(group);
        
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
        addGroupToSurvey(survey3, group);
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
        
        r6 = new Record();
        r6.setSurvey(survey4); // everyone can see
        r6.setUser(adminUser);
        r6.setRecordVisibility(RecordVisibility.OWNER_ONLY); // but owner only!
        recordDAO.save(r6);
        
        r7 = new Record();
        r7.setSurvey(survey4); // everyone can see
        r7.setUser(adminUser);
        r7.setRecordVisibility(RecordVisibility.CONTROLLED);  // everyone should be able to see with limitations on data
        recordDAO.save(r7);
        
        r8 = new Record();
        r8.setSurvey(survey5); // only admin has priveledges here
        r8.setUser(normalUser); // normal user should always be able to see their own record
        r8.setRecordVisibility(RecordVisibility.CONTROLLED);  // everyone should be able to see with limitations on data
        recordDAO.save(r8);
        
        r9 = new Record();
        r9.setSurvey(survey4); // everyone can see
        r9.setUser(normalUser);
        r9.setRecordVisibility(RecordVisibility.OWNER_ONLY); // only admin and owner can see.
        r9.setHeld(true); // only admin or mod can see.
        recordDAO.save(r9);

        getSession().flush();
    }
    
    @Test
    public void testAnonRecordFilter() {
        FilterManager.enableRecordFilter(getSession(), null);
        
        List<Record> result = recordDAO.getRecords(100, 0);
        
        Assert.assertEquals("wrong size", 4, result.size());
        assertRecord(result, r1);
        assertRecord(result, r2);
        assertRecord(result, r4);
        assertRecord(result, r7);
    }
    
    @Test
    public void testGroupUserRecordFilter() {
        FilterManager.enableRecordFilter(getSession(), groupUser);
        
        List<Record> result = recordDAO.getRecords(100, 0);
        
        Assert.assertEquals("wrong size", 5, result.size());
        assertRecord(result, r1);
        assertRecord(result, r2);
        assertRecord(result, r3);
        assertRecord(result, r4);
        assertRecord(result, r7);
    }
    
    @Test
    public void testNormalUserRecordFilter() {
        FilterManager.enableRecordFilter(getSession(), normalUser);
        
        List<Record> result = recordDAO.getRecords(100, 0);
        
        Assert.assertEquals("wrong size", 7, result.size());
        assertRecord(result, r1);
        assertRecord(result, r2);
        assertRecord(result, r3);
        assertRecord(result, r4);
        assertRecord(result, r7);
        assertRecord(result, r8);
        assertRecord(result, r9);
    }
    
    @Test
    public void testUserModRecordFilter() {
        FilterManager.enableRecordFilter(getSession(), moderatorUser);
        
        List<Record> result = recordDAO.getRecords(100, 0);
        
        Assert.assertEquals("wrong size", 7, result.size());
        assertRecord(result, r1);
        assertRecord(result, r2);
        assertRecord(result, r3);
        assertRecord(result, r4);
        assertRecord(result, r5);
        assertRecord(result, r7);
        assertRecord(result, r8);
    }
    
    @Test
    public void testAdminRecordFilter() {
        FilterManager.enableRecordFilter(getSession(), adminUser);
        
        List<Record> result = recordDAO.getRecords(100, 0);
        
        Assert.assertEquals("wrong size", 9, result.size());
        assertRecord(result, r1);
        assertRecord(result, r2);
        assertRecord(result, r3);
        assertRecord(result, r4);
        assertRecord(result, r5);
        assertRecord(result, r6);
        assertRecord(result, r7);
        assertRecord(result, r8);
        assertRecord(result, r9);
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
    
    private void addGroupToSurvey(Survey survey, Group group) {
        Set<Group> groupSet = new HashSet<Group>();
        groupSet.addAll(survey.getGroups());
        groupSet.add(group);
        survey.setGroups(groupSet);
    }
    
    private void assertRecord(List<Record> list, Record expected) {
        Assert.assertTrue("expected record wasn't in list", list.contains(expected));
    }
}
