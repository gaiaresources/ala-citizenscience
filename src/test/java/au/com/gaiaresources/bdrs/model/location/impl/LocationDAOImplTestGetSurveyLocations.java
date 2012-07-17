package au.com.gaiaresources.bdrs.model.location.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.type.CustomType;
import org.hibernatespatial.GeometryUserType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

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
	
	private Logger log = Logger.getLogger(getClass());
	
	@Before
	public void setup() {
		myuser = userDAO.createUser("myuser", "userfirst", "userlast", "user@user.com", "password", "regkey", Role.ADMIN);
		// all points are on perth.
		s1 = createTestSurvey("1", -31.9554, 115.8585, 4326, myuser, Collections.EMPTY_LIST);
		s2 = createTestSurvey("2", 6464012.292, 391931.696, 28350, myuser, Collections.EMPTY_LIST);
		s3 = createTestSurvey("3", 6442213.442, -175849.476, 28351, myuser, s2.getLocations());
	}
	
	@Test
	public void testGetSurveyLocations() {
		PagedQueryResult<Location> result = locationDAO.getSurveylocations(null, myuser, s1.getId());
		Assert.assertEquals("wrong size", 2, result.getList().size());
	}
	
	@Test
	public void testGetUserLocations() {
		SpatialUtilFactory suFactory = new SpatialUtilFactory();
		Region region = new Region();
		int srid = 4326;
		GeometryBuilder gb = new GeometryBuilder(srid);
		SpatialUtil spatialUtil = suFactory.getLocationUtil(srid);
		region.setBoundary((MultiPolygon)spatialUtil.convertToMultiGeom(gb.createRectangle(115, -32, 2, 2)));
		
		List<Location> result =  locationDAO.getUserLocations(myuser, region);
		Assert.assertEquals("wrong list size", 3, result.size());
	}
	
	private Survey createTestSurvey(String name, double y, double x, int srid, User u, List<Location> locations) {
		GeometryBuilder gb = new GeometryBuilder(srid);
		
		Survey survey = new Survey();
		survey.setName(name);
		survey.setDescription(name + " desc");
		survey.setPublic(true);
		
		List<Location> locList = new ArrayList<Location>();
		locList.addAll(locations);
		Location loc = new Location();
		loc.setName(name + " loc");
		loc.setUser(u);
		loc.setDescription(name + "loc desc");
		loc.setLocation(gb.createPoint(x, y));
		
		locList.add(loc);
		
		survey.setLocations(locList);
		
		locationDAO.save(loc);
		return surveyDAO.save(survey);
	}
}
