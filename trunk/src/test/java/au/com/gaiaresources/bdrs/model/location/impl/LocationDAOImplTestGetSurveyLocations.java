package au.com.gaiaresources.bdrs.model.location.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

public class LocationDAOImplTestGetSurveyLocations extends
		AbstractTransactionalTest {

	private Survey s1;
	private Survey s2;
	private Survey s3;
	
	private Location l1;
	private Location l2;
	private Location l3;
	
	private User myuser;
	
	@Autowired
	private SurveyDAO surveyDAO;
	
	@Autowired 
	private LocationDAO locationDAO;
	
	@Autowired
	private UserDAO userDAO;
	
	@Before
	public void setup() {
		myuser = userDAO.createUser("myuser", "userfirst", "userlast", "user@user.com", "password", "regkey", Role.ADMIN);
		s1 = createTestSurvey("1", 1, 1, 4326, Collections.EMPTY_LIST);
		s2 = createTestSurvey("2", 2, 2, 28350, Collections.EMPTY_LIST);
		s3 = createTestSurvey("3", 3, 3, 28351, s2.getLocations());
	}
	
	@Test
	public void testGetSurveyLocations() {
		PagedQueryResult<Location> result = locationDAO.getSurveylocations(null, myuser, s1.getId());
		Assert.assertEquals("wrong size", 2, result.getList().size());
	}
	
	private Survey createTestSurvey(String name, double y, double x, int srid, List<Location> locations) {
		GeometryBuilder gb = new GeometryBuilder(srid);
		
		Survey survey = new Survey();
		survey.setName(name);
		survey.setDescription(name + " desc");
		
		List<Location> locList = new ArrayList<Location>();
		locList.addAll(locations);
		Location loc = new Location();
		loc.setName(name + " loc");
		loc.setDescription(name + "loc desc");
		loc.setLocation(gb.createPoint(x, y));
		
		survey.setLocations(locList);
		
		locationDAO.save(loc);
		return surveyDAO.save(survey);
	}
}
