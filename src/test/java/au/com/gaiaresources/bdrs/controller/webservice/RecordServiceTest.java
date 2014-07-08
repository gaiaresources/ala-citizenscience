package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.test.RecordFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by aaron on 27/03/2014.
 */
public class RecordServiceTest extends AbstractControllerTest {

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

    private User adminUser;
    private User nonAdminUser;

    private Survey s1;
    private Survey s2;

    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;

    private SimpleDateFormat dateFormatter;

    @Before
    public void setup() {

        dateFormatter = new SimpleDateFormat(BdrsWebConstants.DATE_FORMAT);

        adminUser = userDAO.getUser("admin");
        nonAdminUser = userDAO.createUser("u", "w", "a",
                "email@email.com", "password", "regkey", Role.USER);

        s1 = new Survey();
        s1.setName("survey 1");
        s1.setDescription("survey 1 desc");
        surveyDAO.save(s1);

        s2 = new Survey();
        s2.setName("survey 2");
        s2.setDescription("survey 2 desc");
        surveyDAO.save(s2);

        RecordFactory recordFactory = new RecordFactory(recordDAO, avDAO);

        r1 = recordFactory.create(s1, adminUser);
        r1.setWhen(new Date(10000));
        r2 = recordFactory.create(s1, nonAdminUser);
        r2.setWhen(new Date(20000));
        // Offset next 2 dates to make it past noon to test 24 hour time
        r3 = recordFactory.create(s2, adminUser);
        r3.setWhen(new Date(30000+14400000));
        r4 = recordFactory.create(s2, nonAdminUser);
        r4.setWhen(new Date(40000+14400000));

        recDAO.update(r1);
        recDAO.update(r2);
        recDAO.update(r3);
        recDAO.update(r4);
    }

    @Test
    public void testSearchByNonAdminUser() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, nonAdminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r4);
        }
    }

    @Test
    public void testSearchBySurvey() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_SURVEY, s1.getId().toString());
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r2);
        }
    }

    @Test
    public void testSearchByDate() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_DATE_START, dateFormatter.format(new Date(r1.getWhen().getTime()+1000)));
        request.setParameter(RecordService.PARAM_DATE_END, dateFormatter.format(new Date(r3.getWhen().getTime()-1000)));
        request.setParameter(RecordService.PARAM_ROUND_DATE_RANGE, "false");
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 1, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r2);
        }
    }

    @Test
     public void testSearchByDateInclusive() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_DATE_START, dateFormatter.format(new Date(r1.getWhen().getTime())));
        request.setParameter(RecordService.PARAM_DATE_END, dateFormatter.format(new Date(r3.getWhen().getTime())));
        request.setParameter(RecordService.PARAM_ROUND_DATE_RANGE, "false");
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 3, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r3);
        }
    }

    @Test
    public void testPagination() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_PAGE_NUMBER, "1");
        request.setParameter(RecordService.PARAM_LIMIT, "4");
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 4, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r3);
            assertHasRecord(featureJsonArray, r4);
        }

        request.setParameter(RecordService.PARAM_PAGE_NUMBER, "2");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 0, featureJsonArray.size());
        }
    }

    /**
     * This test returns all records as they all lie within the same calendar day.
     * By default this web service rounds the start and end dates to the nearest
     * start and end days respectively.
     * @throws Exception
     */
    @Test
    public void testSearchByDateRoundDateRange() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_DATE_START, dateFormatter.format(new Date(r1.getWhen().getTime())));
        request.setParameter(RecordService.PARAM_DATE_END, dateFormatter.format(new Date(r3.getWhen().getTime())));
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 4, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r3);
            assertHasRecord(featureJsonArray, r4);
        }
    }

    @Test
    public void testSearchByAdminUser() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "true");
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r3);
        }

        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_USER, nonAdminUser.getId().toString());
        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 2, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r4);
        }

        request.removeParameter(RecordService.PARAM_USER);
        request.removeParameter(RecordService.PARAM_SURVEY);

        response = new MockHttpServletResponse();

        handle(request, response);
        {
            // assertions
            JSONArray featureJsonArray = getFeatureArray(response);
            Assert.assertEquals("wrong count", 4, featureJsonArray.size());
            assertHasRecord(featureJsonArray, r1);
            assertHasRecord(featureJsonArray, r2);
            assertHasRecord(featureJsonArray, r3);
            assertHasRecord(featureJsonArray, r4);
        }
    }

    @Test
    public void testDateParsing() throws Exception {
        request.setParameter(RecordService.PARAM_IDENT, adminUser.getRegistrationKey());
        request.setParameter(RecordService.PARAM_ONLY_MY_RECORDS, "false");
        request.setParameter(RecordService.PARAM_DATE_START, dateFormatter.format(new Date(r1.getWhen().getTime())));
        request.setParameter(RecordService.PARAM_DATE_END, "24 Mar 2013 aa:bb"); //junk time
        request.setParameter(RecordService.PARAM_ROUND_DATE_RANGE, "false"); // force time parsing
        request.setRequestURI(RecordService.SEARCH_RECORDS_URL);
        request.setMethod("GET");
        response = new MockHttpServletResponse();

        handle(request, response);

        Assert.assertEquals("wrong status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assert.assertEquals("wrong message", RecordService.PARAM_DATE_END + " parameter must be of the format "
                + BdrsWebConstants.DATE_FORMAT, response.getContentAsString());
    }

    private JSONArray getFeatureArray(MockHttpServletResponse response) throws UnsupportedEncodingException {
        Assert.assertEquals("wrong http return code. msg : " + response.getContentAsString(),
                HttpServletResponse.SC_OK, response.getStatus());
        return JSONArray.fromString(response.getContentAsString());
    }

    private void assertHasRecord(JSONArray featureArray, Record record) {
        for (int i=0; i<featureArray.size(); ++i) {
            JSONObject obj = featureArray.getJSONObject(i);
            //String id = obj.getString("id");
            //Integer idAsInt = Integer.valueOf(id);
            Integer idAsInt = obj.getInt("id");
            if (idAsInt != null && (idAsInt.intValue() == record.getId().intValue())) {
                // record found. assert stuff in the record.
                return;
            }
        }
        // got to the end, record has not been found.
        Assert.fail("Did not find record in feature array");
    }
}
