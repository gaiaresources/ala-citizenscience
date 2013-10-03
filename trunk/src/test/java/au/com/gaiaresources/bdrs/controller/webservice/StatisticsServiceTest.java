package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.test.RecordFactory;
import au.com.gaiaresources.bdrs.test.TestUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 2/10/13
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatisticsServiceTest extends AbstractControllerTest {

    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private AttributeDAO attrDAO;

    @Autowired
    private AttributeValueDAO avDAO;

    @Autowired
    private TaxaDAO taxaDAO;

    private User mainUser;
    private User otherUser;

    private Survey s1;
    private Survey s2;

    private Attribute a1;
    private Attribute a2;

    private IndicatorSpecies species1;
    private IndicatorSpecies species2;

    private TaxonGroup tg;

    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;


    @Before
    public void setup() {

        RecordFactory recordFactory = new RecordFactory(recordDAO, avDAO);

        tg = new TaxonGroup();
        tg.setName("taxon group");
        taxaDAO.save(tg);

        species1 = new IndicatorSpecies();
        species1.setScientificName("sci name 1");
        species1.setCommonName("common name 1");
        species1.setTaxonGroup(tg);
        taxaDAO.save(species1);

        species2 = new IndicatorSpecies();
        species2.setScientificName("sci name 2");
        species2.setCommonName("common name 2");
        species2.setTaxonGroup(tg);
        taxaDAO.save(species2);

        mainUser = userDAO.getUser("admin");
        otherUser = userDAO.getUser("root");

        s1 = new Survey();
        s1.setName("survey 1");
        s1.setDescription("survey 1 desc");

        a1 = new Attribute();
        a1.setTypeCode(AttributeType.STRING.getCode());
        a1.setDescription("a1 desc");
        a1.setName("testvalue1");

        s1.getAttributes().add(a1);

        attrDAO.save(a1);
        surveyDAO.save(s1);

        s2 = new Survey();
        s2.setName("survey 2");
        s2.setDescription("survey 2 desc");

        a2 = new Attribute();
        a2.setTypeCode(AttributeType.STRING.getCode());
        a2.setDescription("a2 desc");
        a2.setName("testvalue2");

        s2.getAttributes().add(a2);

        attrDAO.save(a2);
        surveyDAO.save(s2);

        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();
        SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil();
        GeometryFactory geometryFactory = spatialUtil.getGeometryFactory();

        r1 = recordFactory.create(s1, mainUser);
        r1.setWhen(TestUtil.getDate(2013, 2, 2));
        r1.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r1.setSpecies(species1);

        r2 = recordFactory.create(s2, mainUser);
        r2.setWhen(TestUtil.getDate(2013, 2, 4));
        r2.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r2.setSpecies(species1);

        r3 = recordFactory.create(s1, otherUser);
        r3.setWhen(TestUtil.getDate(2013, 2, 6));
        r3.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r3.setSpecies(species1);

        r4 = recordFactory.create(s2, otherUser);
        r4.setWhen(TestUtil.getDate(2013, 2, 8));
        r4.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r4.setSpecies(species2);

        r1.setRecordVisibility(RecordVisibility.PUBLIC);
        r2.setRecordVisibility(RecordVisibility.PUBLIC);
        r3.setRecordVisibility(RecordVisibility.PUBLIC);
        r4.setRecordVisibility(RecordVisibility.PUBLIC);

        recordDAO.save(r1);
        recordDAO.save(r2);
        recordDAO.save(r3);
        recordDAO.save(r4);
    }

    @Test
    public void testStatistics() throws Exception {

        request.setRequestURI(StatisticsService.LATEST_STATS_URL);
        request.setMethod("GET");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Integer recordCount = json.getInt("recordCount");
        Integer uniqueSpeciesCount = json.getInt("uniqueSpeciesCount");
        Integer userCount = json.getInt("userCount");

        JSONObject latestRecord = json.getJSONObject("latestRecord");
        Integer recordId = latestRecord.getInt("recordId");

        Assert.assertEquals("wrong record count", 4, recordCount.intValue());
        Assert.assertEquals("wrong unique species count", 2, uniqueSpeciesCount.intValue());
        Assert.assertEquals("wrong user cont", 2, userCount.intValue());

        Record lr = recordDAO.getLatestRecord();

        Assert.assertEquals("wrong latest record id", lr.getId().intValue(), recordId.intValue());

        String sciName = latestRecord.getString("species");

        Assert.assertEquals("wrong sci name", lr.getSpecies().getScientificName(), sciName);
    }
}
