package au.com.gaiaresources.bdrs.model.location.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
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
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class LocationDAOImplFilterTest extends AbstractTransactionalTest {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private LocationDAO locDAO;
    @Autowired
    private GroupDAO groupDAO;

    private Survey survey1;
    private Survey survey2;
    private Survey survey3;
    private Survey survey4;
    private Survey survey5;

    private Location loc1;
    private Location loc2;
    private Location loc3;
    private Location loc4;
    private Location loc5;
    private Location loc6;
    private Location loc7;

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
        
        loc6 = new Location();
        loc6.setLocation(spatialUtil.createPoint(1, 1));
        loc6.setName("loc 6");
        loc6.setUser(adminUser);
        locDAO.save(loc6);
        
        loc7 = new Location();
        loc7.setLocation(spatialUtil.createPoint(1, 1));
        loc7.setName("loc 7");
        loc7.setUser(normalUser);
        locDAO.save(loc7);

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

        getSession().flush();
    }
    
    @Test
    public void testAnonFilter() {
        FilterManager.enableLocationFilter(getSession(), null);
        
        List<Location> result = getAllLocations();
        
        for (Location l : result) {
            log.debug("loc name : " + l.getName());
        }
        
        Assert.assertEquals("wrong size", 3, result.size());
        
        assertLocation(result, loc1);
        assertLocation(result, loc2);
        assertLocation(result, loc4);
    }
    
    @Test
    public void testGroupFilter() {
        FilterManager.enableLocationFilter(getSession(), groupUser);
        
        List<Location> result = getAllLocations();
        
        Assert.assertEquals("wrong size", 4, result.size());
        
        assertLocation(result, loc1);
        assertLocation(result, loc2);
        assertLocation(result, loc3);
        assertLocation(result, loc4);
    }
    
    @Test
    public void testUserFilter() {
        FilterManager.enableLocationFilter(getSession(), normalUser);
        
        List<Location> result = getAllLocations();
        
        Assert.assertEquals("wrong size", 5, result.size());
        
        assertLocation(result, loc1);
        assertLocation(result, loc2);
        assertLocation(result, loc3);
        assertLocation(result, loc4);
        assertLocation(result, loc7);
    }
    
    @Test
    public void testAdminFilter() {
        FilterManager.enableLocationFilter(getSession(), adminUser);
        
        List<Location> result = getAllLocations();
        
        Assert.assertEquals("wrong size", 7, result.size());
        
        assertLocation(result, loc1);
        assertLocation(result, loc2);
        assertLocation(result, loc3);
        assertLocation(result, loc4);
        assertLocation(result, loc5);
        assertLocation(result, loc6);
        assertLocation(result, loc7);
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
    
    private void assertLocation(List<Location> list, Location expected) {
        Assert.assertTrue("Could not find expected Location in list", list.contains(expected));
    }
    
    private List<Location> getAllLocations() {
        return getSession().createQuery("from Location").list();
    }
    
    private void addGroupToSurvey(Survey survey, Group group) {
        Set<Group> groupSet = new HashSet<Group>();
        groupSet.addAll(survey.getGroups());
        groupSet.add(group);
        survey.setGroups(groupSet);
    }
}
