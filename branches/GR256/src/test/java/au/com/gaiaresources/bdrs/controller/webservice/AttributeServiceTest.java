package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.json.JSONSerializer;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests the AttributeService.
 */
public class AttributeServiceTest extends AbstractControllerTest {

    private String ident = "testIdentity";
    private Mockery context = new Mockery();
    private RecordDAO recordDAO = context.mock(RecordDAO.class);
    private UserDAO userDAO = context.mock(UserDAO.class);


    private void setUpMocks() {
        AttributeService toTest = applicationContext.getBean(AttributeService.class);
        // Mock out the RecordDAO.
        ReflectionTestUtils.setField(toTest, "recordDAO", recordDAO);
        ReflectionTestUtils.setField(toTest, "userDAO", userDAO);


    }

    /**
     * Tests the controller responds appropriately to valid input.
     */
    @Test
    public void testFindRecordsByAttributeValue() throws Exception {

        setUpMocks();

        context.checking(new Expectations() {{
            oneOf(userDAO).getUserByRegistrationKey(ident);
            will(returnValue(new User()));
        }});

        final int surveyId = 1;
        final String attributeName = "attribute";
        final String attributeValue = "val";
        final List<Record> records = new ArrayList<Record>();
        Record r1 = new Record();
        Date now = new Date();
        r1.setWhen(now);
        r1.setNotes("Test");
        records.add(r1);

        context.checking(new Expectations() {{
            oneOf(recordDAO).getRecordByAttributeValue(null, surveyId, attributeName, attributeValue);
            will(returnValue(records));
        }});

        request.setMethod("GET");
        request.setRequestURI("/webservice/attribute/recordsByAttributeValue.htm");
        request.setParameter("ident", ident);
        request.setParameter("surveyId", Integer.toString(surveyId));
        request.setParameter("attributeName", attributeName);
        request.setParameter("attributeValue", attributeValue);


        this.handle(request, response);


        JSONObject json = (JSONObject) JSONSerializer.toJSON(response.getContentAsString());
        JSONArray rowArray = json.getJSONArray("records");
        Assert.assertEquals(1, rowArray.size());

        JSONObject record = (JSONObject)rowArray.get(0);

        Assert.assertEquals(now.getTime(), record.getLong("when"));
        Assert.assertEquals("Test", record.getString("notes"));
    }

    @Test
    public void testRequestIsAuthenticated() throws Exception {
        setUpMocks();

        context.checking(new Expectations() {{
            oneOf(userDAO).getUserByRegistrationKey(ident);
            will(returnValue(null));
        }});

        request.setMethod("GET");
        request.setRequestURI("/webservice/attribute/recordsByAttributeValue.htm");
        request.setParameter("ident", ident);

        this.handle(request, response);

        Assert.assertEquals(401, response.getStatus());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(response.getContentAsString());
        Assert.assertEquals(401, json.getInt("status"));

    }

    @Test
    public void testInvalidParams() throws Exception {
        setUpMocks();

        context.checking(new Expectations() {{
            oneOf(userDAO).getUserByRegistrationKey(ident);
            will(returnValue(new User()));
        }});context.checking(new Expectations() {{
            oneOf(recordDAO).getRecordByAttributeValue(null, 0, "", "");
            will(throwException(new IllegalArgumentException("invalid parameter")));
        }});

        request.setMethod("GET");
        request.setRequestURI("/webservice/attribute/recordsByAttributeValue.htm");
        request.setParameter("ident", ident);

        this.handle(request, response);

        Assert.assertEquals(400, response.getStatus());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(response.getContentAsString());
        Assert.assertEquals(400, json.getInt("status"));
        Assert.assertEquals("invalid parameter", json.getString("message"));
    }

}
