package au.com.gaiaresources.bdrs.controller.webservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.taxa.*;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.BadWebParameterException;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.bulkdata.AbstractBulkDataService;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

@Controller
public class RecordService extends AbstractController {

    public static final String SEARCH_RECORDS_URL = "/webservice/record/searchRecords.htm";

    /**
     * URL for jqgrid compliant ajax request for retrieving child records
     */
    public static final String AJAX_GET_CHILD_RECORD_URL = "/webservice/record/getChildRecords.htm"; 
    
    public static final String GET_RECORD_BY_ID_V2_URL = "/webservice/record/getRecordById_v2.htm";
    
    /**
     * Request parameter for parent record ID
     */
    public static final String PARAM_PARENT_RECORD_ID = "parentRecordId";
    
    /**
     * Ident of user
     */
    public static final String PARAM_REGKEY = "regkey";
    /**
     * Ident of user again - different param name.
     * Leaving both in code to avoid breaking code that
     * uses these webservices.
     */
    public static final String PARAM_IDENT = "ident";
    public static final String PARAM_SPECIES = "species";
    public static final String PARAM_USER = "user";
    public static final String PARAM_GROUP = "group";
    public static final String PARAM_SURVEY = "survey";
    public static final String PARAM_TAXON_GROUP = "taxon_group";
    public static final String PARAM_DATE_START = "date_start";
    public static final String PARAM_DATE_END = "date_end";
    public static final String PARAM_LIMIT = "limit";
    public static final String PARAM_ONLY_MY_RECORDS = "only_my_records";
    public static final String PARAM_DEEP_FLATTEN = "deep_flatten";
    public static final String PARAM_ROUND_DATE_RANGE = "round_date_range";
    public static final String PARAM_PAGE_NUMBER = "page_number";

    public static final String ERROR_FORMAT_PARAM_FORMAT = "%s parameter must be of the format %s";
    
    /**
     * Request parameter for census method ID
     */
    public static final String PARAM_CENSUS_METHOD_ID = BdrsWebConstants.PARAM_CENSUS_METHOD_ID;

    private Logger log = Logger.getLogger(getClass());
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private AttributeValueDAO attrValueDAO;
    @Autowired
    private TaxaService taxaService;
    @Autowired
    private AbstractBulkDataService bulkDataService;
    @Autowired
    private PreferenceDAO prefDAO;
    
    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    private SimpleDateFormat dateFormatter = new SimpleDateFormat(BdrsWebConstants.DATE_FORMAT);

    @RequestMapping(value = "/webservice/record/lastRecords.htm", method = RequestMethod.GET)
    public void getLatestRecords(
            @RequestParam(value = "species", defaultValue = "") String species,
            @RequestParam(value = "user", defaultValue = "0") int userPk,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            HttpServletResponse response)
            throws IOException {
        // RequestParam user - the user
        // RequestParam limit - the number of records to return
        // RequestParam species - the species to search for optional
        User user = userDAO.getUser(userPk);
        List<Record> recordList = recordDAO.getRecord(user, 0, 0, 0, null,
                null, species, limit);

        JSONArray array = new JSONArray();
        for (Record r : recordList) {
            array.add(r.flatten());
        }

        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    @RequestMapping(value = "/webservice/record/lastSpecies.htm", method = RequestMethod.GET)
    public void getLastSpecies(
            @RequestParam(value = "user", defaultValue = "0") int userPk,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // RequestParam user - the user
        // RequestParam limit - number of last species to return
        // [ { species : {species}, record : {record} }, ... ]
        List<IndicatorSpecies> species = recordDAO
                .getLastSpecies(userPk, limit);
        JSONArray array = new JSONArray();
        User user = userDAO.getUser(userPk);
        for (IndicatorSpecies s : species) {
            List<Record> recordList = recordDAO.getRecord(user, 0, 0, 0,
                    null, null, s.getScientificName(), 1);
            if (recordList.size() > 0) {
                JSONObject ob = new JSONObject();
                ob.put("species", s.flatten());
                ob.put("record", recordList.get(0).flatten());
                array.add(ob);
            }
        }

        response.setContentType("application/json");
        response.getWriter().write(array.toString());

    }

    /**
     * Search records
     *
     * @param ident ident of the logged in user.
     * @param species filters records by species name.
     * @param userPk filters records by owning user. Ignored if onlyMyRecords is true.
     * @param groupPk filters records by owning user's group.
     * @param surveyPk filters records by survey.
     * @param taxonGroupPk filters records by taxon group.
     * @param startDate filters records by 'when' field.
     * @param endDate filters records by 'when' field.
     * @param limit limits the number of records returned.
     * @param onlyMyRecords if true will only return records owned by logged in user. If false
     *                      will allow the userPk field to come into effect. If the logged
     *                      in user is not an admin, this field will always be read as true.
     * @param deepFlatten if true flattens more of the record - will make the query slower.
     * @param roundDateRange if true start / end dates will be rounded to the nearest start and
     *                       end of days respectively. If false the start and end dates will not
     *                       be rounded.
     * @param pageNumber page number to return. starts at 1
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws IOException on error writing to output stream
     */
    @RequestMapping(value = SEARCH_RECORDS_URL, method = RequestMethod.GET)
    public void searchRecords(
            @RequestParam(value = PARAM_IDENT, defaultValue = "") String ident,
            @RequestParam(value = PARAM_SPECIES, defaultValue = "") String species,
            @RequestParam(value = PARAM_USER, defaultValue = "0") int userPk,
            @RequestParam(value = PARAM_GROUP, defaultValue = "0") int groupPk,
            @RequestParam(value = PARAM_SURVEY, defaultValue = "0") int surveyPk,
            @RequestParam(value = PARAM_TAXON_GROUP, defaultValue = "0") int taxonGroupPk,
            @RequestParam(value = PARAM_DATE_START, defaultValue = "01 Jan 1970") Date startDate,
            @RequestParam(value = PARAM_DATE_END, defaultValue = "01 Jan 9999") Date endDate,
            @RequestParam(value = PARAM_LIMIT, defaultValue = "5000") int limit,
            @RequestParam(value = PARAM_ONLY_MY_RECORDS, defaultValue = "true") boolean onlyMyRecords,
            @RequestParam(value = PARAM_DEEP_FLATTEN, defaultValue = "false") boolean deepFlatten,
            @RequestParam(value = PARAM_ROUND_DATE_RANGE, defaultValue = "true") boolean roundDateRange,
            @RequestParam(value = PARAM_PAGE_NUMBER, defaultValue = "1") int pageNumber,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        User loggedInUser;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            loggedInUser = userDAO.getUserByRegistrationKey(ident);
            if (loggedInUser == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        if (!roundDateRange) {
            // reassign start and end dates
            // Needed to be done this way as spring RequestParam refused to
            // parse the time component of any passed string - not sure why.
            // Tried using explicit @DateTimeFormat provided by spring and still
            // had no luck.
            Map paramMap = request.getParameterMap();
            try {
                startDate = parseDate(paramMap, PARAM_DATE_START, new Date(0));
                endDate = parseDate(paramMap, PARAM_DATE_END, new Date(Long.MAX_VALUE));
            } catch (BadWebParameterException e) {
                response.getWriter().write(e.getLocalizedMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        Session sesh = getRequestContext().getHibernate();
        sesh.setFlushMode(FlushMode.MANUAL);

        // non admin users will only be able to search their own records
        onlyMyRecords = !loggedInUser.isAdmin() || onlyMyRecords;

        // The default userPk = 0 will return a null user which will
        // search through all users.
        User requestedUser = loggedInUser;
        if (!onlyMyRecords) {
            requestedUser = userDAO.getUser(userPk);
        }

        ScrollableRecords sr = recordDAO.getScrollableRecords(requestedUser,
                groupPk, surveyPk, taxonGroupPk, startDate, endDate, species, pageNumber, limit, loggedInUser,
                roundDateRange);
        
        int recordCount = 0;
        JSONArray array = new JSONArray();
        while(sr.hasMoreElements()) {
            array.add(sr.nextElement().flatten(deepFlatten ? 4 : 0));
            if (++recordCount % ScrollableRecords.RESULTS_BATCH_SIZE == 0) {
                sesh.clear();
            }
        }

        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    /**
     * Saves the records of a particular survey in the database.
     * 
     * @param surveyId
     *            The id of the survey of which records are to be stored.
     * @param request
     *            HttpServletRequest
     * @param response
     *            HttpServletResponse
     * @throws IOException
     * @throws ParseException
     */
    @RequestMapping(value = "/webservice/record/uploadRecords.htm", method = RequestMethod.POST)
    public void uploadRecords(
            @RequestParam(value = "survey", defaultValue = "") Integer surveyId,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParseException {
        // variables
        Map<String, Object> jsonResponse = new HashMap<String, Object>();
        User user;
        JSONObject jsonRecordObject = JSONObject.fromStringToJSONObject(request
                .getParameter("JSONrecords"));

        Survey survey = surveyDAO.getSurvey(surveyId);
        List<Attribute> surveyatts = survey.getAttributes();
        String string_codes = "TASVSTIMFISA"; //
        String numeric_codes = "INDE"; //
        Map<String, Integer> onlineRecordIds = new HashMap<String, Integer>();
        JSONObject jsonRecord;

        // authorisation
        user = userDAO.getUserByRegistrationKey(request.getParameter("ident"));
        if (user == null) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // convert JSON records in to Record objects
        for (Object entry : jsonRecordObject.entrySet()) {
            Record record = new Record();
            Set<AttributeValue> newAttributes = new HashSet<AttributeValue>();
            jsonRecord = (JSONObject) entry;
            // standard attributes
            double latitude = jsonRecord.getDouble("latitude");
            double longitude = jsonRecord.getDouble("longitude");
            record.setPoint(spatialUtil.createPoint(latitude, longitude));
            record.setNotes(jsonRecord.getString("notes"));
            String number_string = request.getParameter("numberseen");
            if (!"".equals(number_string) & (number_string != null)) {
                // set number seen if there are any
                record.setNumber(Integer.valueOf(number_string));
            }
            record.setFirstAppearance(true);
            record.setLastAppearance(true);
            record.setUser(user);
            record.setSpecies(taxaService.getIndicatorSpecies(jsonRecord
                    .getInt("fkindicatorspeciesid")));
            record.setWhen(new Date(new Long(jsonRecord.getString("when"))));
            record.setTime(new Long(jsonRecord.getString("time")));
            record
                    .setLastDate(new Date(
                            new Long(jsonRecord.getString("when"))));
            record.setLastTime(new Long(jsonRecord.getString("time")));
            record.setHeld(true);
            record.setSurvey(survey);
            // custom attributes
            JSONArray jsonRecordAttributeList = jsonRecord
                    .getJSONArray("attributes");

            for (Attribute att : surveyatts) {
                if(!AttributeScope.LOCATION.equals(att.getScope())) {
                    String rec_att_value = jsonRecordAttributeList.getString(att
                            .getId());
                    AttributeValue recAttr = new AttributeValue();
                    recAttr.setAttribute(att);
                    if (string_codes.contains(att.getType().getCode())) {
                        recAttr.setStringValue(rec_att_value);
                    } else if (numeric_codes.contains(att.getType().getCode())) {
                        recAttr.setNumericValue(new BigDecimal(rec_att_value));
                    } else {
                        Date date_custom = new SimpleDateFormat("dd MMM yyyy")
                                .parse(rec_att_value);
                        Calendar cal_custom = new GregorianCalendar();
                        cal_custom.setTime(date_custom);
                        date_custom = cal_custom.getTime();
                        recAttr.setDateValue(date_custom);
                    }
                    recAttr = attrValueDAO.save(recAttr);
                    // save attribute and store in Set
                    newAttributes.add(recAttr);
                }
            }
            // store custom attributes in record
            record.setAttributes(newAttributes);
            // save record
            record = recordDAO.saveRecord(record);
            // add mapping for online and offline record ids to onlineRecordIds
            onlineRecordIds.put(jsonRecord.getString("id"), record.getId());
        }
        jsonResponse.put("succes", "true");
        jsonResponse.put("recordIdsMapping", onlineRecordIds);
        response.getWriter().write(JSONObject.fromMapToString(jsonResponse));
    }

    @RequestMapping(value = "/webservice/record/updateRecords.htm", method = RequestMethod.POST)
    public void updateRecords(
            @RequestParam(value = "survey", defaultValue = "") Integer surveyId,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParseException {
        // variables
        Map<String, Object> jsonResponse = new HashMap<String, Object>();
        User user;
        JSONObject jsonRecordObject = JSONObject.fromStringToJSONObject(request
                .getParameter("JSONrecords"));

        Survey survey = surveyDAO.getSurvey(surveyId);
        List<Attribute> surveyatts = survey.getAttributes();
        String string_codes = "TASVSTIMFI"; //
        String numeric_codes = "INDE"; //
        Map<String, Integer> onlineRecordIds = new HashMap<String, Integer>();
        JSONObject jsonRecord;

        // authorisation
        user = userDAO.getUserByRegistrationKey(request.getParameter("ident"));
        if (user == null) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // convert JSON records in to Record objects
        for (Object entry : jsonRecordObject.entrySet()) {
            jsonRecord = (JSONObject) entry;
            Record record = recordDAO.getRecord(jsonRecord
                    .getInt("online_recordid"));
            Set<AttributeValue> newAttributes = new HashSet<AttributeValue>();

            // set standard fields
            Date date = new Date(new Long(jsonRecord.getString("when")));
            date.setTime(new Long(jsonRecord.getString("time")));
            record.setSpecies(taxaService.getIndicatorSpecies(jsonRecord
                    .getInt("fkindicatorspeciesid")));
            double latitude = jsonRecord.getDouble("latitude");
            double longitude = jsonRecord.getDouble("longitude");
            record.setPoint(spatialUtil.createPoint(latitude, longitude));
            record.setTime(date.getTime());
            record.setWhen(date);
            // record.setNumber(new Integer(request.getParameter("number")));
            record.setNotes(jsonRecord.getString("notes"));
            record.setLastDate(date);
            record.setLastTime(date.getTime());
            record.setSurvey(survey);
            record.setUser(user);
            record.setHeld(true);
            // custom attributes
            JSONArray jsonRecordAttributeList = jsonRecord
                    .getJSONArray("attributes");

            // set custom fields
            Map<Integer, AttributeValue> recordAttributesMap = new HashMap<Integer, AttributeValue>();
            Set<AttributeValue> recordAttributes = record.getAttributes();
            // convert Set in to Map
            for (AttributeValue ra : recordAttributes) {
                recordAttributesMap.put(ra.getAttribute().getId(), ra);
            }
            // empty set
            recordAttributes.clear();

            for (Attribute att : surveyatts) {
                if(!AttributeScope.LOCATION.equals(att.getScope())) {
                    String rec_att_value = jsonRecordAttributeList.getString(att
                            .getId());
                    AttributeValue recAttr = new AttributeValue();
                    recAttr.setAttribute(att);
                    if (string_codes.contains(att.getType().getCode())) {
                        recAttr.setStringValue(rec_att_value);
                    } else if (numeric_codes.contains(att.getType().getCode())) {
                        recAttr.setNumericValue(new BigDecimal(rec_att_value));
                    } else {
                        Date date_custom = new SimpleDateFormat("dd MMM yyyy")
                                .parse(rec_att_value);
                        Calendar cal_custom = new GregorianCalendar();
                        cal_custom.setTime(date_custom);
                        date_custom = cal_custom.getTime();
                        recAttr.setDateValue(date_custom);
                    }
                    recAttr = recordDAO.saveAttributeValue(recAttr);
                    // save attribute and store in Set
                    newAttributes.add(recAttr);
                }
            }
            // add the updated RecordAttributes to the record
            record.setAttributes(recordAttributes);
            // update record
            record = recordDAO.updateRecord(record);
            // add mapping for online and offline record ids to onlineRecordIds
            onlineRecordIds.put(jsonRecord.getString("id"), record.getId());
        }
        jsonResponse.put("succes", "true");
        jsonResponse.put("recordIdsMapping", onlineRecordIds);
        response.getWriter().write(JSONObject.fromMapToString(jsonResponse));
    }

    @RequestMapping(value = "/webservice/record/downloadRecords.htm", method = RequestMethod.GET)
    public void downloadRecords(
            @RequestParam(value = "ident", defaultValue = "") String ident,
            @RequestParam(value = "species", defaultValue = "") String species,
            @RequestParam(value = "user", defaultValue = "0") int userPk,
            @RequestParam(value = "group", defaultValue = "0") int groupPk,
            @RequestParam(value = "survey", defaultValue = "1") int surveyPk,
            @RequestParam(value = "taxon_group", defaultValue = "0") int taxonGroupPk,
            @RequestParam(value = "date_start", defaultValue = "01 Jan 1970") Date startDate,
            @RequestParam(value = "date_end", defaultValue = "01 Jan 9999") Date endDate,
            @RequestParam(value = "limit", defaultValue = "5000") long limit,
            HttpServletResponse response) throws IOException {

        // We are changing the flush mode here to prevent checking for dirty
        // objects in the session cache. Normally this is desireable so that
        // you will not receive stale objects however in this situation
        // the controller will only be performing reads and the objects cannot
        // be stale. We are explicitly setting the flush mode here because
        // we are potentially loading a lot of objects into the session cache
        // and continually checking if it is dirty is prohibitively expensive.
        // https://forum.hibernate.org/viewtopic.php?f=1&t=936174&view=next
        getRequestContext().getHibernate().setFlushMode(FlushMode.MANUAL);

        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        ScrollableRecords sc = recordDAO.getScrollableRecords(user, groupPk,
                surveyPk, taxonGroupPk, startDate, endDate, species);

        Survey survey = surveyDAO.getSurvey(surveyPk);

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition",
                "attachment;filename=records_"
                        + System.currentTimeMillis() + ".xls");
        bulkDataService.exportSurveyRecords(getRequestContext().getHibernate(), survey, sc, limit, response
                .getOutputStream());
    }

    /**
     * Returns the records of a particular user and survey
     * 
     *
     * @param response
     *            HttpServletResponse
     * @param regkey
     *            Registration key of the user
     * @param surveyId
     *            Id of the survey of which records are requested
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/record/recordsForSurvey.htm", method = RequestMethod.GET)
    public void recordsForSurvey(HttpServletResponse response,
                                 @RequestParam(value = PARAM_REGKEY, defaultValue = "0") String regkey,
                                 @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, defaultValue = "0") int surveyId)
            throws IOException {

        // check authorisation
        if (regkey.equals("0")) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }
        User user = userDAO.getUserByRegistrationKey(regkey);
        if (user == null) {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        // get Records
        Survey survey = surveyDAO.get(surveyId);
        Set<User> users = new HashSet<User>();
        users.add(user);
        List<Record> records = recordDAO.getRecords(survey, users);
        // parse to JSON
        JSONArray array = new JSONArray();
        for (Record r : records) {
            Map<String, Object> flattendRecord = r.flatten();
            if (r.getSpecies() != null) {
                    flattendRecord.put("commonName", r.getSpecies().getCommonName());
                    flattendRecord.put("scientificName", r.getSpecies().getScientificName());
            }
            array.add(flattendRecord);
        }

        // return JSON
        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    @RequestMapping(value = "/webservice/record/getRecordsForLocation.htm", method = RequestMethod.GET)
    public void getRecordsForLocation(
            @RequestParam(value = "ident", required = true) String ident,
            @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, defaultValue = "0", required = true) int surveyPk,
            @RequestParam(value = BdrsWebConstants.PARAM_LOCATION_ID, defaultValue = "0", required = true) int locationPk,
            HttpServletResponse response) throws IOException {

        List<Record> recordList = recordDAO.getRecords(ident, surveyPk,
                locationPk);

        JSONArray array = new JSONArray();
        for (Record r : recordList) {
            array.add(r.flatten());
        }
        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    /**
     * Delete a record of a particular user.
     * @param ident - The registration key assigned to the user.
     * @param recordPk - Id of the record that needs to be deleted.
     * @param response - HttpServletResponse
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/record/deleteRecord.htm", method = RequestMethod.POST)
    public void deleteRecord(
            @RequestParam(value = "ident", required = true) String ident,
            @RequestParam(value = BdrsWebConstants.PARAM_RECORD_ID, required = true) int recordPk,
            HttpServletResponse response) throws IOException {

        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        
        recordDAO.deleteById(recordPk);
        // return true if succesfull
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("succeeded", true);
        response.setContentType("application/json");
        response.getWriter().write(jsonObject.toString());
    }

    /**
     * Deletes records from the database with the given record ids.
     *
     * @param ident
     *            The users registration key.
     * @param recordIds
     *            An array of primary keys of the records to be deleted.
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/record/bulkDeleteRecords.htm", method = RequestMethod.POST)
    public void bulkDeleteRecords(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "ident", required = true) String ident,
            @RequestParam(value = "recordIds[]", required=true) int[] recordIds) throws IOException {

        User user;
        if (ident.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        JSONArray deletedRecordIds = new JSONArray();
        for(int pk : recordIds) {
            recordDAO.deleteById(pk);
            deletedRecordIds.add(pk);
        }

        super.writeJson(request, response, deletedRecordIds.toString());
    }

    /**
     * Deletes records from the database with the given record ids.
     *
     * @param ident
     *            The users registration key.
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/record/deleteRecords.htm", method = RequestMethod.POST)
    public void deleteRecords(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "ident", required = true) String ident)
            throws IOException {

        JSONObject jsonRecordIdsMap = JSONObject.fromStringToJSONObject(request
                .getParameter("JSONrecords"));
        // Authenticate the user
        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        // Delete the records
        for (Object entry : jsonRecordIdsMap.entrySet()) {
            try {
                Integer id = (Integer) entry;
                recordDAO.deleteById(id);
            } catch (Exception e) {
                log.error("The id in the jsonRecordIdsMap is not an Integer.");
            }
        }

        // return true if succesfull
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("succeeded", true);
        response.setContentType("application/json");
        response.getWriter().write(jsonObject.toString());
    }

    @RequestMapping(value = "/webservice/record/getRecordById.htm", method = RequestMethod.GET)
    public void getRecordById(
            @RequestParam(value = PARAM_REGKEY, required = true) String ident,
            @RequestParam(value = BdrsWebConstants.PARAM_RECORD_ID, defaultValue = "0", required = true) int recordPk,
            HttpServletResponse response) throws IOException {
        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        Record record = recordDAO.getRecord(recordPk);

        Map<String, Object> flattendRecord = record.flatten();
        if (record.getSpecies() != null) {
                flattendRecord.put("commonName", record.getSpecies().getCommonName());
                flattendRecord.put("scientificName", record.getSpecies().getScientificName());
        }
        Map<String, String> attVals = new HashMap<String, String>();
        for (TypedAttributeValue ra : record.getAttributes()) {
            String attId = ra.getAttribute().getId().toString();
            String attVal = ra.getStringValue();
            attVals.put(attId, attVal);
        }
        flattendRecord.put("attributes", attVals);

        JSONObject rec = new JSONObject();
        rec.accumulateAll(flattendRecord);

        // return JSON
        response.setContentType("application/json");
        response.getWriter().write(rec.toString());

    }
    
    /**
     * Outputs JSON suitable for displaying on BDRS maps.
     *
     * @param ident ident of user.
     * @param recordPk record PK.
     * @param serializeLazyLoadedAttributes whether to serialize attribute values or not. defaults to true.
     * @param request HttpServletRequest.
     * @param response HttpServletReponse.
     * @throws IOException Error writing to output stream.
     */
    @RequestMapping(value = GET_RECORD_BY_ID_V2_URL, method = RequestMethod.GET)
    public void getRecordByIdV2(
            @RequestParam(value = PARAM_REGKEY, required = false) String ident,
            @RequestParam(value = BdrsWebConstants.PARAM_RECORD_ID, defaultValue = "0", required = true) int recordPk,
            @RequestParam(value = "serializeAv", required = false, defaultValue = "true") boolean serializeLazyLoadedAttributes,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = userDAO.getUserByRegistrationKey(ident);

        Record record = recordDAO.getRecord(recordPk);
        
        // Filter data based on user permissions. Null user is OK
        AccessControlledRecordAdapter ar = new AccessControlledRecordAdapter(record, user);

        JsonService jsonService = new JsonService(prefDAO,
                getRequestContext().getServerURL());

        JSONObject recJson = jsonService.toJson(ar, new SpatialUtilFactory(),
                serializeLazyLoadedAttributes);

        // return JSON
        writeJson(request, response, recJson.toString());
    }

    @RequestMapping(value = "/webservice/record/getRecordAttributeById.htm", method = RequestMethod.GET)
    public void getRecordAttributeById(
            @RequestParam(value = "ident", required = true) String ident,
            @RequestParam(value = "recordAttributeId", defaultValue = "0", required = true) int recordAttributePk,
            HttpServletResponse response) throws IOException {

        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        AttributeValue recAttr = recordDAO
                .getAttributeValue(recordAttributePk);
        JSONObject rec = new JSONObject();
        if (recAttr != null) {
            // flatten with depth of 1 so we can see the underlying attribute type
            rec.accumulateAll(recAttr.flatten(1));
        }
        response.setContentType("application/json");
        response.getWriter().write(rec.toString());
    }

    @InitBinder
    public void initBinder(HttpServletRequest request,
            ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }

    /**
     * Deletes records from the database with the given record ids.
     * @param request - HttpServletRequest
     * @param response - HttpServletResponse
     * @param ident - The users registration key.
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/record/syncToServer.htm", method = RequestMethod.POST)
    public void sync(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "ident", required = true) String ident,
            @RequestParam(value="JSONrecords", required = true) JSONArray allRecordsObject)
            throws IOException, ParseException {
        
        User user;
        // Authenticate the user
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        
        Survey survey;
        HashMap<Integer, Survey> surveyMap = new HashMap<Integer, Survey>();
        String string_codes = "TASVSTIMFISA"; //
        String numeric_codes = "INDE"; //
        Map<String, Integer> onlineRecordIds = new HashMap<String, Integer>();
        Map<String, Integer> updatedRecordIds = new HashMap<String, Integer>();
        Map<String, String> deletedRecordIds = new HashMap<String, String>();
        
        // Delete records : gets the JSONArray with records that need to be deleted from the allRecords Object 
        JSONArray deleteRecordsArray = allRecordsObject.getJSONArray(0);
        for (Object delete_r : deleteRecordsArray) {
            try {
                // Type cast record object to JSONObject
                JSONObject jsonDeleteRecord = (JSONObject) delete_r;
                // Get survey from map if exists in the map otherwise get it from the database
                int surveyId = jsonDeleteRecord.getInt("surveyid");
                if(surveyMap.containsKey(surveyId)){
                    survey = surveyMap.get(surveyId);
                }else{
                    survey = surveyDAO.getSurvey(surveyId);
                    surveyMap.put(survey.getId(), survey);
                }
                //delete the actual record
                
                if(!jsonDeleteRecord.getString("online_recordid").isEmpty()){
                    //there is an online record id
                    recordDAO.deleteById(jsonDeleteRecord.getInt("online_recordid"));
                }
                deletedRecordIds.put(jsonDeleteRecord.getString("id"), jsonDeleteRecord.getString("online_recordid"));
            } catch (Exception e) {
                log.error("Something went wrong while trying to delete a record in the Record sync method.");
            }
        }
        
        // Update records : gets the JSONArray with records that need to be updated from the allRecords Object 
        JSONArray updateRecordsArray = allRecordsObject.getJSONArray(1);
        for (Object update_r : updateRecordsArray) {
            // Type cast record object to JSONObject
            JSONObject jsonUpdateRecord = (JSONObject) update_r;
            // Get the original record from the database
            Record updateRecord = recordDAO.getRecord(jsonUpdateRecord.getInt("online_recordid"));
            Set<AttributeValue> newUpdateAttributes = new HashSet<AttributeValue>();
            // Get survey from map if exists in the map otherwise get it from the database
            int surveyId = jsonUpdateRecord.getInt("surveyid");
            if(surveyMap.containsKey(surveyId)){
                survey = surveyMap.get(surveyId);
            }else{
                survey = surveyDAO.getSurvey(surveyId);
                surveyMap.put(survey.getId(), survey);
            }
            //get attributes from the survey related to the current record
            List<Attribute> surveyatts = survey.getAttributes();
            // set standard fields
            Date date = new Date(new Long(jsonUpdateRecord.getString("when")));
            date.setTime(new Long(jsonUpdateRecord.getString("time")));
            updateRecord.setSpecies(taxaService.getIndicatorSpecies(jsonUpdateRecord.getInt("fkindicatorspeciesid")));
            double latitude = jsonUpdateRecord.getDouble("latitude");
            double longitude = jsonUpdateRecord.getDouble("longitude");
            updateRecord.setPoint(spatialUtil.createPoint(latitude, longitude));
            updateRecord.setTime(date.getTime());
            updateRecord.setWhen(date);
            updateRecord.setNotes(jsonUpdateRecord.getString("notes"));
            updateRecord.setLastDate(date);
            updateRecord.setLastTime(date.getTime());
            updateRecord.setSurvey(survey);
            updateRecord.setUser(user);
            updateRecord.setHeld(true);
            // custom attributes
            Map<String, String> jsonUpdateRecordAttributeList = jsonUpdateRecord.getJSONObject("attributes");
            for (Attribute att : surveyatts) {
                if(!AttributeScope.LOCATION.equals(att.getScope())) {
                    String rec_att_value = jsonUpdateRecordAttributeList.get(att.getId().toString());
                    AttributeValue recAttr = new AttributeValue();
                    recAttr.setAttribute(att);
                    if (string_codes.contains(att.getType().getCode())) {
                        recAttr.setStringValue(rec_att_value);
                    } else if (numeric_codes.contains(att.getType().getCode())) {
                        recAttr.setNumericValue(new BigDecimal(rec_att_value));
                    } else {
                        Date date_custom = new SimpleDateFormat("dd MMM yyyy")
                                .parse(rec_att_value);
                        Calendar cal_custom = new GregorianCalendar();
                        cal_custom.setTime(date_custom);
                        date_custom = cal_custom.getTime();
                        recAttr.setDateValue(date_custom);
                    }
                    recAttr = recordDAO.saveAttributeValue(recAttr);
                    // save attribute and store in Set
                    newUpdateAttributes.add(recAttr);
                }
            }
            // store custom attributes in record
            updateRecord.setAttributes(newUpdateAttributes);
            // update record
            updateRecord = recordDAO.updateRecord(updateRecord);
            // add mapping for online and off-line record ids to
            // updatedRecordIds
            updatedRecordIds.put(jsonUpdateRecord.getString("id"), updateRecord
                    .getId());
        }
        
        // Save records : gets the JSONArray with records that are new from the allRecords Object 
        JSONArray uploadRecordsArray = allRecordsObject.getJSONArray(2);
        for (Object upload_r : uploadRecordsArray) {
            JSONObject jsonUploadRecord = (JSONObject) upload_r;
            // Get survey from map if exists in the map otherwise get it from the database
            if(surveyMap.containsKey(jsonUploadRecord.getInt("surveyid"))){
                survey = surveyMap.get(jsonUploadRecord.getInt("surveyid"));
            }else{
                survey = surveyDAO.getSurvey(jsonUploadRecord.getInt("surveyid"));
                surveyMap.put(survey.getId(), survey);
            }
            Record uploadRecord = new Record();
            Set<AttributeValue> newUploadAttributes = new HashSet<AttributeValue>();
            // standard attributes
            double uploadLatitude = jsonUploadRecord.getDouble("latitude");
            double uploadLongitude = jsonUploadRecord.getDouble("longitude");
            uploadRecord.setPoint(spatialUtil.createPoint(uploadLatitude,uploadLongitude));
            uploadRecord.setNotes(jsonUploadRecord.getString("notes"));
            String number_string = jsonUploadRecord.getString("numberseen");
            if (number_string != "" & (number_string != null)) {
                // set number seen if there are any
                uploadRecord.setNumber(Integer.valueOf(number_string));
            }
            uploadRecord.setFirstAppearance(true);
            uploadRecord.setLastAppearance(true);
            uploadRecord.setUser(user);
            uploadRecord.setSpecies(taxaService
                    .getIndicatorSpecies(jsonUploadRecord
                            .getInt("fkindicatorspeciesid")));
            uploadRecord.setWhen(new Date(new Long(jsonUploadRecord.getString("when"))));
            uploadRecord.setTime(new Long(jsonUploadRecord.getString("time")));
            uploadRecord.setLastDate(new Date(new Long(jsonUploadRecord
                    .getString("when"))));
            uploadRecord.setLastTime(new Long(jsonUploadRecord
                    .getString("time")));
            uploadRecord.setHeld(true);
            uploadRecord.setSurvey(survey);
            // custom attributes
            Map<String, String> jsonUploadRecordAttributeList = jsonUploadRecord.getJSONObject("attributes");
            //get attributes from the survey related to the current record
            List<Attribute> surveyatts = survey.getAttributes();
            for (Attribute att : surveyatts) {
                if(!AttributeScope.LOCATION.equals(att.getScope())) {
                    String rec_att_value = jsonUploadRecordAttributeList.get(att
                            .getId().toString());
                    AttributeValue recAttr = new AttributeValue();
                    recAttr.setAttribute(att);
                    if (string_codes.contains(att.getType().getCode())) {
                        recAttr.setStringValue(rec_att_value);
                    } else if (numeric_codes.contains(att.getType().getCode())) {
                        recAttr.setNumericValue(new BigDecimal(rec_att_value));
                    } else {
                        Date date_custom = new SimpleDateFormat("dd MMM yyyy")
                                .parse(rec_att_value);
                        Calendar cal_custom = new GregorianCalendar();
                        cal_custom.setTime(date_custom);
                        date_custom = cal_custom.getTime();
                        recAttr.setDateValue(date_custom);
                    }
                    recAttr = recordDAO.saveAttributeValue(recAttr);
                    // save attribute and store in Set
                    newUploadAttributes.add(recAttr);
                }
            }
            // store custom attributes in record
            uploadRecord.setAttributes(newUploadAttributes);
            // save record
            uploadRecord = recordDAO.saveRecord(uploadRecord);
            // add mapping for online and off-line record ids to
            // onlineRecordIds
            onlineRecordIds.put(jsonUploadRecord.getString("id"), uploadRecord
                    .getId());
        }

        JSONObject jsonReturnObject = new JSONObject();
        jsonReturnObject.put("succeeded", true);
        jsonReturnObject.put("deleteResponse", deletedRecordIds);
        jsonReturnObject.put("updateResponse", updatedRecordIds);
        jsonReturnObject.put("uploadResponse", onlineRecordIds);
        response.setContentType("application/json");
        response.getWriter().write(jsonReturnObject.toString());
    }
    
    /**
     * JqGrid compliant service for retrieving child records
     * 
     * @param request - HttpServletRequest
     * @param response - HttpServletResponse
     * @param parentRecordId - parent to retrieve children for
     * @param censusMethodId - census method to search for inside the children
     * @throws Exception
     */
    @RequestMapping(value = AJAX_GET_CHILD_RECORD_URL, method = RequestMethod.GET)
    public void getChildRecords(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_PARENT_RECORD_ID, required=false) Integer parentRecordId,
            @RequestParam(value=PARAM_CENSUS_METHOD_ID, required=false) Integer censusMethodId) throws Exception {
        
        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);
        PaginationFilter filter = jqGridHelper.createFilter(request);
        
        User accessingUser = getRequestContext().getUser();
        
        PagedQueryResult<Record> queryResult = recordDAO.getChildRecords(filter, parentRecordId, censusMethodId, accessingUser);
        
        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());

        if (queryResult.getCount() > 0) {
            for (Record rec : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(rec.getId());
                // Set flatten depth to 1 so we can access the species common name
                row.addValues(rec.flatten(1));
                builder.addRow(row);
            }
        }
        this.writeJson(request, response, builder.toJson());
    }

    /**
     * Helper to parse dates from HttpServletRequest.
     *
     * @param paramMap http parameter map
     * @param paramName Name of date parameter
     * @param defaultValue Default value to use if the parameter name is missing
     *                     from the parameter map.
     * @return Date if successful, null if not successful. The controller should
     * check this result of this method and return immediately if the result is null.
     * @throws au.com.gaiaresources.bdrs.controller.BadWebParameterException when
     * the string value is incorrect
     */
    private Date parseDate(Map<String, String[]> paramMap,
                           String paramName, Date defaultValue) throws BadWebParameterException {
        if (paramName == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        String[] valueArray = paramMap.get(paramName);
        if (valueArray != null && valueArray.length > 0) {
            String dateString = paramMap.get(paramName)[0];
            if (dateString == null) {
                return defaultValue;
            } else {
                try {
                    return dateFormatter.parse(dateString);
                } catch (ParseException e) {
                    throw new BadWebParameterException(
                            String.format(ERROR_FORMAT_PARAM_FORMAT, paramName, BdrsWebConstants.DATE_FORMAT), e);
                } catch (IllegalArgumentException e) {
                    throw new BadWebParameterException(
                            String.format(ERROR_FORMAT_PARAM_FORMAT, paramName, BdrsWebConstants.DATE_FORMAT), e);
                }
            }
        } else {
            return null;
        }
    }
}
