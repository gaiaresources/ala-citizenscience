package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.RecordGroupDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.test.TestUtil;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 9/10/13
 * Time: 10:37 AM
 *
 * Minimal test case for uploading record groups.
 */
public class ApplicationServiceUploadRecordGroupTest extends AbstractControllerTest {

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private RecordGroupDAO recordGroupDAO;

    @Autowired
    private MetadataDAO mdDAO;

    @Autowired
    private UserDAO userDAO;

    private Survey survey;

    private Record record;

    private RecordGroup recordGroup;

    private User user;

    private static final String TEST_EXISTING_RECORD_CLIENT_ID = "asdfasfasfasdfasd";
    private static final String TEST_EXISTING_RECORD_GROUP_CLIENT_ID = "ssdjfkhk34rh48fh34tkja";

    @Before
    public void setup() {
        user = userDAO.getUser("admin");
        survey = new Survey();
        survey.setActive(true);
        // set all record properties to false
        for (RecordPropertyType rpType : RecordPropertyType.values()) {
            new RecordProperty(survey, rpType, mdDAO).setRequired(false);
        }

        surveyDAO.save(survey);

        record = new Record();
        record.setUser(user);
        record.setSurvey(survey);

        Metadata md = recordDAO.getRecordMetadataForKey(record, Metadata.RECORD_CLIENT_ID_KEY);
        md.setValue(TEST_EXISTING_RECORD_CLIENT_ID);
        mdDAO.save(md);

        recordDAO.save(record);

        recordGroup = new RecordGroup();
        recordGroupDAO.save(recordGroup);

        Metadata groupMd = recordGroupDAO.getRecordGroupMetadataForKey(recordGroup,
                Metadata.RECORD_GROUP_CLIENT_ID_KEY);
        groupMd.setValue(TEST_EXISTING_RECORD_GROUP_CLIENT_ID);
        mdDAO.save(groupMd);

        getSession().flush();
    }

    @Test
    public void testUploadNewRecordGroupToNewRecord() throws Exception {

        login("admin", "password", new String[] { Role.ADMIN });

        JSONObject recJson = new JSONObject();
        recJson.put(ApplicationService.UPLOAD_JSON_KEY_SURVEY_ID, survey.getId());
        recJson.put("server_id", 0);
        recJson.put("id", "test record client id");
        recJson.put("survey_id", survey.getId());
        recJson.put("attributeValues", new JSONArray());

        JSONObject groupJson = new JSONObject();
        groupJson.put("id", "test record group client id");
        groupJson.put("server_id", 0);

        recJson.put("recordGroup", groupJson);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(recJson);

        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", jsonArray.toJSONString());

        JSONArray recordGroupArray = new JSONArray();

        JSONObject g1 = new JSONObject();
        g1.put("id", groupJson.getString("id"));
        g1.put("type", "hello world type");
        g1.put("server_id", 0);
        g1.put("survey_id", survey.getId());
        Date startDate = TestUtil.getDate(2013, 1, 1);
        Date endDate = TestUtil.getDate(2014,1,2);
        g1.put("startDate", startDate.getTime());
        g1.put("endDate", endDate.getTime());

        recordGroupArray.add(g1);

        request.setParameter("recordGroupData", recordGroupArray.toJSONString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");

        RequestContext context = RequestContextHolder.getContext();
        Assert.assertEquals("admin", context.getUser().getName());

        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(200, json.getInt("status"));

        JSONObject data = json.getJSONObject("200");
        JSONArray syncResult = data.getJSONArray("sync_result");

        // we expect one sync result for the record and one for the record group
        Assert.assertEquals("wrong size", 2, syncResult.size());

        Assert.assertEquals("wrong number of records", 2,
                recordDAO.countAllRecords().intValue());
        Assert.assertEquals("wrong number of record groups", 2,
                recordGroupDAO.count(RecordGroup.class).intValue());

        assertSyncResult(syncResult);

        RecordGroup gg = recordGroupDAO.getRecordGroupByClientID(g1.getString("id"));
        Assert.assertNotNull("Record group should be non null", gg);

        recordGroupDAO.refresh(gg);
        Assert.assertEquals("wrong record group set size", 1, gg.getRecords().size());

        Record r = gg.getRecords().iterator().next();

        Assert.assertEquals("wrong rec client id", recJson.getString("id"),
                r.getMetadataValue(Metadata.RECORD_CLIENT_ID_KEY));

        Assert.assertEquals("wrong group type", g1.getString("type"), gg.getType());
        Assert.assertEquals("wrong survey id", g1.getInt("survey_id"),
                gg.getSurvey().getId().intValue());
        Assert.assertEquals("wrong start date", startDate, gg.getStartDate());
        Assert.assertEquals("Wrong end date", endDate, gg.getEndDate());
    }

    @Test
    public void testUploadExistingRecordGroupToExistingRecord() throws Exception {

        login("admin", "password", new String[] { Role.ADMIN });

        JSONObject recJson = new JSONObject();
        recJson.put(ApplicationService.UPLOAD_JSON_KEY_SURVEY_ID, survey.getId());
        recJson.put("server_id", record.getId());
        recJson.put("id", TEST_EXISTING_RECORD_CLIENT_ID);
        recJson.put("survey_id", survey.getId());
        recJson.put("attributeValues", new JSONArray());

        JSONObject groupJson = new JSONObject();
        groupJson.put("id", TEST_EXISTING_RECORD_GROUP_CLIENT_ID);
        groupJson.put("server_id", recordGroup.getId());

        recJson.put("recordGroup", groupJson);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(recJson);

        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", jsonArray.toJSONString());

        JSONArray recordGroupArray = new JSONArray();

        JSONObject g1 = new JSONObject();
        g1.put("id", TEST_EXISTING_RECORD_GROUP_CLIENT_ID);
        g1.put("type", "hello world type");
        g1.put("server_id", recordGroup.getId());
        g1.put("survey_id", survey.getId());
        Date startDate = TestUtil.getDate(2013, 1, 1);
        Date endDate = TestUtil.getDate(2014,1,2);
        g1.put("startDate", startDate.getTime());
        g1.put("endDate", endDate.getTime());

        recordGroupArray.add(g1);

        request.setParameter("recordGroupData", recordGroupArray.toJSONString());


        ModelAndView mv = handle(request, response);

        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");

        RequestContext context = RequestContextHolder.getContext();
        Assert.assertEquals("admin", context.getUser().getName());

        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(200, json.getInt("status"));

        JSONObject data = json.getJSONObject("200");
        JSONArray syncResult = data.getJSONArray("sync_result");

        // we expect one sync result for the record and one for the record group
        Assert.assertEquals("wrong size", 2, syncResult.size());

        Assert.assertEquals("wrong number of records", 1,
                recordDAO.countAllRecords().intValue());
        Assert.assertEquals("wrong number of record groups", 1,
                recordGroupDAO.count(RecordGroup.class).intValue());

        assertSyncResult(syncResult);

        RecordGroup gg = recordGroupDAO.getRecordGroupByClientID(g1.getString("id"));
        Assert.assertNotNull("Record group should be non null", gg);

        recordGroupDAO.refresh(gg);
        Assert.assertEquals("wrong record group set size", 1, gg.getRecords().size());

        Record r = gg.getRecords().iterator().next();
        recordDAO.refresh(r);

        Assert.assertEquals("wrong rec client id", recJson.getString("id"),
                r.getMetadataValue(Metadata.RECORD_CLIENT_ID_KEY));

        Assert.assertEquals("wrong group type", g1.getString("type"), gg.getType());
        Assert.assertEquals("wrong survey id", g1.getInt("survey_id"),
                gg.getSurvey().getId().intValue());
        Assert.assertEquals("wrong start date", startDate, gg.getStartDate());
        Assert.assertEquals("Wrong end date", endDate, gg.getEndDate());
    }

    @Test
    public void testUploadExistingRecordGroupToNewRecord() throws Exception {

        login("admin", "password", new String[] { Role.ADMIN });

        JSONObject recJson = new JSONObject();
        recJson.put(ApplicationService.UPLOAD_JSON_KEY_SURVEY_ID, survey.getId());
        recJson.put("server_id", 0);
        recJson.put("id", "test record client id");
        recJson.put("survey_id", survey.getId());
        recJson.put("attributeValues", new JSONArray());

        JSONObject groupJson = new JSONObject();
        groupJson.put("id", TEST_EXISTING_RECORD_GROUP_CLIENT_ID);
        groupJson.put("server_id", recordGroup.getId());

        recJson.put("recordGroup", groupJson);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(recJson);

        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", jsonArray.toJSONString());

        JSONArray recordGroupArray = new JSONArray();

        JSONObject g1 = new JSONObject();
        g1.put("id", TEST_EXISTING_RECORD_GROUP_CLIENT_ID);
        g1.put("type", "hello world type");
        g1.put("server_id", recordGroup.getId());
        g1.put("survey_id", survey.getId());
        Date startDate = TestUtil.getDate(2013, 1, 1);
        Date endDate = TestUtil.getDate(2014,1,2);
        g1.put("startDate", startDate.getTime());
        g1.put("endDate", endDate.getTime());

        recordGroupArray.add(g1);

        request.setParameter("recordGroupData", recordGroupArray.toJSONString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");

        RequestContext context = RequestContextHolder.getContext();
        Assert.assertEquals("admin", context.getUser().getName());

        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(200, json.getInt("status"));

        JSONObject data = json.getJSONObject("200");
        JSONArray syncResult = data.getJSONArray("sync_result");

        // we expect one sync result for the record and one for the record group
        Assert.assertEquals("wrong size", 2, syncResult.size());

        Assert.assertEquals("wrong number of records", 2,
                recordDAO.countAllRecords().intValue());
        Assert.assertEquals("wrong number of record groups", 1,
                recordGroupDAO.count(RecordGroup.class).intValue());

        assertSyncResult(syncResult);

        RecordGroup gg = recordGroupDAO.getRecordGroupByClientID(g1.getString("id"));
        Assert.assertNotNull("Record group should be non null", gg);

        recordGroupDAO.refresh(gg);
        Assert.assertEquals("wrong record group set size", 1, gg.getRecords().size());

        Record r = gg.getRecords().iterator().next();

        Assert.assertEquals("wrong rec client id", recJson.getString("id"),
                r.getMetadataValue(Metadata.RECORD_CLIENT_ID_KEY));

        Assert.assertEquals("wrong group type", g1.getString("type"), gg.getType());
        Assert.assertEquals("wrong survey id", g1.getInt("survey_id"),
                gg.getSurvey().getId().intValue());
        Assert.assertEquals("wrong start date", startDate, gg.getStartDate());
        Assert.assertEquals("Wrong end date", endDate, gg.getEndDate());
    }

    private void assertSyncResult(JSONArray array) {
        for (int i=0;i<array.size();++i) {
            JSONObject obj = array.getJSONObject(i);
            String klass = obj.getString("klass");
            Integer serverId = obj.getInt("server_id");
            if (klass.equals("Record")) {
                Record r = recordDAO.getRecord(serverId);
                Assert.assertNotNull("Could not find record for pk " + serverId, r);
            } else if (klass.equals("RecordGroup")) {
                RecordGroup rg = recordGroupDAO.getRecordGroup(serverId);
                Assert.assertNotNull("Could not find record group for pk " + serverId, rg);
            }
        }
    }
}
