package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
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
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 1/10/13
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordGeoJsonServiceTest extends AbstractControllerTest {

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

    private TaxonGroup primaryGroup;
    private TaxonGroup secondaryGroup;
    private IndicatorSpecies species1;
    private IndicatorSpecies species2;

    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;


    @Before
    public void setup() {

        primaryGroup = new TaxonGroup();
        primaryGroup.setName("primary group");
        taxaDAO.save(primaryGroup);

        secondaryGroup = new TaxonGroup();
        secondaryGroup.setName("secondary group");
        taxaDAO.save(secondaryGroup);

        species1 = new IndicatorSpecies();
        species1.setScientificName("sp sci one");
        species1.setCommonName("sp common one");
        species1.setTaxonGroup(primaryGroup);
        taxaDAO.save(species1);

        species2 = new IndicatorSpecies();
        species2.setScientificName("sp sci two");
        species2.setCommonName("sp common two");
        species2.setTaxonGroup(primaryGroup);
        species2.addSecondaryGroup(secondaryGroup);
        taxaDAO.save(species2);


        RecordFactory recordFactory = new RecordFactory(recordDAO, avDAO);

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
        r2.setSpecies(species2);

        r3 = recordFactory.create(s1, otherUser);
        r3.setWhen(TestUtil.getDate(2013, 2, 6));
        r3.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r3.setSpecies(species1);

        r4 = recordFactory.create(s2, otherUser);
        r4.setWhen(TestUtil.getDate(2013, 2, 8));
        r4.setGeometry(geometryFactory.createPoint(new Coordinate(1,2)));
        r4.setSpecies(species2);
    }

    @Test
    public void testGeoJson() throws Exception {

        request.setParameter(RecordGeoJsonService.PARAM_USERNAME, mainUser.getName());
        request.addParameter(RecordGeoJsonService.PARAM_SURVEY_IDS, s1.getId().toString());
        request.addParameter(RecordGeoJsonService.PARAM_SURVEY_IDS, s2.getId().toString());
        request.addParameter(RecordGeoJsonService.PARAM_LIMIT, "2");
        request.setRequestURI(RecordGeoJsonService.GET_RECORD_GEOJSON_URL);
        request.setMethod("GET");

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
            this.assertHasRecord(featureJsonArray, r2);
        }

        request.setParameter(RecordGeoJsonService.PARAM_LIMIT, "1");

        response = new MockHttpServletResponse();

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r2);
        }
    }

    @Test
    public void testSpeciesFilter() throws Exception {

        request.setParameter(RecordGeoJsonService.PARAM_USERNAME, mainUser.getName());
        request.setParameter(RecordGeoJsonService.PARAM_SPECIES_NAME, species1.getScientificName());
        request.addParameter(RecordGeoJsonService.PARAM_LIMIT, "100");
        request.setRequestURI(RecordGeoJsonService.GET_RECORD_GEOJSON_URL);
        request.setMethod("GET");

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
        }

        request.setParameter(RecordGeoJsonService.PARAM_SPECIES_NAME, species1.getCommonName());
        response = new MockHttpServletResponse();

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
        }
    }

    @Test
    public void testTaxonGroupFilter() throws Exception {
        request.setParameter(RecordGeoJsonService.PARAM_GROUP_NAME, primaryGroup.getName());
        request.addParameter(RecordGeoJsonService.PARAM_LIMIT, "100");
        request.setRequestURI(RecordGeoJsonService.GET_RECORD_GEOJSON_URL);
        request.setMethod("GET");

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 4, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
            this.assertHasRecord(featureJsonArray, r2);
            this.assertHasRecord(featureJsonArray, r3);
            this.assertHasRecord(featureJsonArray, r4);
        }

        request.setParameter(RecordGeoJsonService.PARAM_GROUP_NAME, secondaryGroup.getName());
        response = new MockHttpServletResponse();

        this.handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r2);
            this.assertHasRecord(featureJsonArray, r4);
        }
    }

    private JSONArray getFeatureArray(MockHttpServletResponse response) throws UnsupportedEncodingException {
        Assert.assertEquals("wrong http return code. msg : " + response.getContentAsString(),
                HttpServletResponse.SC_OK, response.getStatus());
        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());
        Assert.assertEquals("wrong type" ,"FeatureCollection", json.getString("type"));
        return json.getJSONArray("features");
    }

    @Test
    public void testSurveySelection() throws Exception {

        request.setParameter(RecordGeoJsonService.PARAM_USERNAME, mainUser.getName());
        request.addParameter(RecordGeoJsonService.PARAM_SURVEY_IDS, s1.getId().toString());

        request.addParameter(RecordGeoJsonService.PARAM_LIMIT, "10");
        request.setRequestURI(RecordGeoJsonService.GET_RECORD_GEOJSON_URL);
        request.setMethod("GET");

        this.handle(request, response);

        JSONArray featureJsonArray = getFeatureArray(response);

        Assert.assertEquals("wrong count", 1, featureJsonArray.size());

        this.assertHasRecord(featureJsonArray, r1);
    }

    @Test
    public void testDateSelection() throws Exception {

        request.setParameter(RecordGeoJsonService.PARAM_USERNAME, mainUser.getName());
        request.addParameter(RecordGeoJsonService.PARAM_SURVEY_IDS, s1.getId().toString());
        request.addParameter(RecordGeoJsonService.PARAM_SURVEY_IDS, s2.getId().toString());

        request.addParameter(RecordGeoJsonService.PARAM_LIMIT, "10");

        request.setRequestURI(RecordGeoJsonService.GET_RECORD_GEOJSON_URL);
        request.setMethod("GET");

        request.setParameter(RecordGeoJsonService.PARAM_START_DATE, "2013-03-1");
        request.setParameter(RecordGeoJsonService.PARAM_END_DATE, "2013-03-3");
        this.handle(request, response);
        {
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
        }

        response = new MockHttpServletResponse();
        request.setParameter(RecordGeoJsonService.PARAM_START_DATE, "2013-03-03");
        request.setParameter(RecordGeoJsonService.PARAM_END_DATE, "2013-03-3");
        this.handle(request, response);
        {
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 0, featureJsonArray.size());
        }

        response = new MockHttpServletResponse();
        request.removeParameter(RecordGeoJsonService.PARAM_START_DATE);
        request.setParameter(RecordGeoJsonService.PARAM_END_DATE, "2013-03-3");
        this.handle(request, response);
        {
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r1);
        }

        response = new MockHttpServletResponse();
        request.removeParameter(RecordGeoJsonService.PARAM_START_DATE);
        request.removeParameter(RecordGeoJsonService.PARAM_END_DATE);
        this.handle(request, response);
        {
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
        }

        response = new MockHttpServletResponse();
        request.setParameter(RecordGeoJsonService.PARAM_START_DATE, "2013-03-03");
        request.removeParameter(RecordGeoJsonService.PARAM_END_DATE);
        this.handle(request, response);
        {
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            this.assertHasRecord(featureJsonArray, r2);
        }
    }

    private void assertHasRecord(JSONArray featureArray, Record record) {
        for (int i=0; i<featureArray.size(); ++i) {
            JSONObject obj = featureArray.getJSONObject(i);
            String id = obj.getString("id");
            Integer idAsInt = Integer.valueOf(id);
            if (idAsInt != null && (idAsInt.intValue() == record.getId().intValue())) {
                // record found. assert stuff in the record.

                JSONObject geomJson = obj.getJSONObject("geometry");
                JSONArray coords = geomJson.getJSONArray("coordinates");

                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);

                Assert.assertEquals("wrong longitude", record.getPoint().getX(), lon, 0.0001);
                Assert.assertEquals("wrong latitude", record.getPoint().getY(), lat, 0001);

                return;
            }
        }
        // got to the end, record has not been found.
        Assert.fail("Did not find record in feature array");
    }
}