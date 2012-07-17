package au.com.gaiaresources.bdrs.model.record;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerSource;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;

import com.vividsolutions.jts.geom.Geometry;

public class RecordDAOImplSpatialTest extends AbstractControllerTest {
    
    private GeometryBuilder geometryBuilder = new GeometryBuilder();
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private GeoMapLayerDAO layerDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private LocationDAO locDAO;
    
    User admin;
    User root;
    Survey survey1;
    Survey survey2;
    GeoMapLayer layer1;
    GeoMapLayer layer2;
    
    Record r1;
    Record r2;
    Record r3;
    Record r4;
    Record r5;
    
    Geometry g1;
    Geometry g2;
    Geometry g3;
    Geometry g4;
    Geometry g5;
        
    @Before
    public void setup() {
        Calendar cal = Calendar.getInstance();
        cal.set(2000, 10, 10);
        Date now = cal.getTime();
        
        admin = userDAO.getUser("admin");
        root = userDAO.getUser("root");
        
        survey1 = surveyDAO.createSurvey("my survey");
        survey2 = surveyDAO.createSurvey("second survey");
        
        GeometryBuilder mgaBuilder = new GeometryBuilder(28350);
        
        g1 = geometryBuilder.createSquare(0, 0, 10);
        g2 = geometryBuilder.createSquare(5, 0, 10);
        g3 = geometryBuilder.createSquare(0, 5, 10);
        g4 = geometryBuilder.createSquare(5, 5, 10);
        g5 = mgaBuilder.createSquare(0, 0, 10);
        
        r1 = createTestRecord(now, g1, survey1, RecordVisibility.OWNER_ONLY, admin);
        r2 = createTestRecord(now, g2, survey1, RecordVisibility.OWNER_ONLY, admin);
        r3 = createTestRecord(now, g3, survey2, RecordVisibility.CONTROLLED, root);
        r4 = createTestRecord(now, g4, survey2, RecordVisibility.CONTROLLED, root);
        r5 = createTestRecord(now, g5, survey1, RecordVisibility.PUBLIC, admin);
                      
        layer1 = new GeoMapLayer();
        layer1.setName("aaaa");
        layer1.setDescription("zzzz");
        layer1.setSurvey(survey1);
        layer1.setLayerSource(GeoMapLayerSource.SURVEY_KML);

        layer2 = new GeoMapLayer();
        layer2.setName("cccc");
        layer2.setDescription("xxxx");
        layer2.setLayerSource(GeoMapLayerSource.SHAPEFILE);
        layer2.setSurvey(survey2);
                
        layerDAO.save(layer1);
        layerDAO.save(layer2);
        
        recordDAO.saveRecord(r1);
        recordDAO.saveRecord(r2);
        recordDAO.saveRecord(r3);
        recordDAO.saveRecord(r4);
        recordDAO.saveRecord(r5);
        
        surveyDAO.updateSurvey(survey1);
    }
    
    @Test
    public void testPointIntersect() {
        Integer[] layerIds = new Integer[] { layer1.getId(), layer2.getId() };
        
        {
            Geometry point = geometryBuilder.createPoint(2.5, 2.5);
            List<Record> result = recordDAO.find(layerIds, point, null, null);
            Assert.assertEquals(1, result.size());
        }
        
        {
            Geometry point = geometryBuilder.createPoint(2.5, 7.5);
            List<Record> result = recordDAO.find(layerIds, point, null, null);
            Assert.assertEquals(2, result.size());
        }
        
        {
            Geometry point = geometryBuilder.createPoint(7.5, 7.5);
            List<Record> result = recordDAO.find(layerIds, point, null, null);
            Assert.assertEquals(4, result.size());
        }
    }
    
    @Test
    public void testPointIntersectPublicPrivate() {
        Integer[] layerIds = new Integer[] { layer1.getId(), layer2.getId() };
        
        Geometry point = geometryBuilder.createPoint(2.5, 2.5);
        {
            
            List<Record> result = recordDAO.find(layerIds, point, null, null);
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, true, null);
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, false, null);
            Assert.assertEquals(0, result.size());
        }
    }
    
    @Test
    public void testPointIntersectByUser() {
        Integer[] layerIds = new Integer[] { layer1.getId(), layer2.getId() };
        
        Geometry point = geometryBuilder.createPoint(2.5, 2.5);
        {
            List<Record> result = recordDAO.find(layerIds, point, null, null);
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, null, admin.getId());
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, null, root.getId());
            Assert.assertEquals(0, result.size());
        }
    }
    
    // user/owner overrides everything. we can always see our own records.
    @Test
    public void testPointIntersectPrivateAndUser() {
        Integer[] layerIds = new Integer[] { layer1.getId(), layer2.getId() };
        
        Geometry point = geometryBuilder.createPoint(2.5, 2.5);
        {
            List<Record> result = recordDAO.find(layerIds, point, false, admin.getId());
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, true, admin.getId());
            Assert.assertEquals(1, result.size());
        }
        
        {
            List<Record> result = recordDAO.find(layerIds, point, null, admin.getId());
            Assert.assertEquals(1, result.size());
        }
    }
    
    @Test
    public void testReturnAll() {
        Integer[] layerIds = new Integer[] { layer1.getId(), layer2.getId() };
        {
            List<Record> result = recordDAO.find(layerIds, null, null, null);
            Assert.assertEquals(5, result.size());
        }
    }
    
    @Test
    public void testGetWithin() {
    	// create a srid = 4326 rectangle.
    	Geometry geom = geometryBuilder.createRectangle(-1, -1, 30, 30);
    	// this rectangle should contain all of the points.
    	List<Record> result = recordDAO.getRecords(geom);
    	Assert.assertEquals("wrong list size", 4, result.size());
    }
    
    private Record createTestRecord(Date now, Geometry geom, Survey survey, RecordVisibility publish, User owner) {
        Record rec = new Record();
        rec.setUser(owner);
        rec.setCensusMethod(null);
        rec.setSurvey(survey);
        rec.setSpecies(null);
        rec.setWhen(now);
        rec.setLastDate(now);
        rec.setGeometry(geom);   
        rec.setRecordVisibility(publish);
        
        Location loc = new Location();
        loc.setName("lname");
        loc.setDescription("ldesc");
        loc.setLocation(geom);
        rec.setLocation(loc);
        locDAO.save(loc);
        
        return rec;
    }
}
