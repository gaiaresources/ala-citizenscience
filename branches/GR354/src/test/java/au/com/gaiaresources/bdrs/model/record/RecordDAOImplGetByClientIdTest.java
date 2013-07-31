package au.com.gaiaresources.bdrs.model.record;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerSource;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

import com.vividsolutions.jts.geom.Geometry;

public class RecordDAOImplGetByClientIdTest extends AbstractTransactionalTest {

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
    private MetadataDAO mdDAO;
    
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
        
        r1 = createTestRecord(now, g1, survey1, RecordVisibility.OWNER_ONLY, admin, "1");
        r2 = createTestRecord(now, g2, survey1, RecordVisibility.OWNER_ONLY, admin, "2");
        r3 = createTestRecord(now, g3, survey2, RecordVisibility.CONTROLLED, root, "3");
        r4 = createTestRecord(now, g4, survey2, RecordVisibility.CONTROLLED, root, "4");
        r5 = createTestRecord(now, g5, survey1, RecordVisibility.PUBLIC, admin, "1");
                      
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
        
        surveyDAO.updateSurvey(survey1);
    }
    
    @Test
    public void testGetByClientId() {
    	 Record r = recordDAO.getRecordByClientID("1");
    	 Assert.assertNotNull("record should not be null", r);
    	 Assert.assertEquals("wrong id", r1.getId(), r.getId());
    }
    
    private Record createTestRecord(Date now, Geometry geom, Survey survey, RecordVisibility publish, User owner, String clientId) {
        Record rec = new Record();
        rec.setUser(owner);
        rec.setCensusMethod(null);
        rec.setSurvey(survey);
        rec.setSpecies(null);
        rec.setWhen(now);
        rec.setLastDate(now);
        rec.setGeometry(geom);   
        rec.setRecordVisibility(publish);
        
        Metadata md = new Metadata();
        md.setKey(Metadata.RECORD_CLIENT_ID_KEY);
        md.setValue(clientId);
        
        Metadata md2 = new Metadata();
        md2.setKey("dummykey");
        md2.setValue("dummyvalue");
        
        Set<Metadata> mdSet = new HashSet<Metadata>();
        mdSet.add(md);
        mdSet.add(md2);
        rec.setMetadata(mdSet);
        
        mdDAO.save(md);
        mdDAO.save(md2);
        recordDAO.save(rec);
        
        return rec;
    }
}
