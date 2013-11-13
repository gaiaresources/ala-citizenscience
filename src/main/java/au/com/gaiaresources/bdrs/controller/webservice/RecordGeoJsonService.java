package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.geojson.RecordGroupLineMfFeature;
import au.com.gaiaresources.bdrs.geojson.RecordMinimalMfFeature;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeoJSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 1/10/13
 * Time: 10:15 AM
 * To change this template use File | Settings | File Templates.
 */

@Controller
public class RecordGeoJsonService extends AbstractController {

    public static final String GET_RECORD_GEOJSON_URL = "/webservice/record/geojson.htm";

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_LIMIT = "limit";
    public static final String PARAM_START_DATE = "startDate";
    public static final String PARAM_END_DATE = "endDate";
    public static final String PARAM_SURVEY_IDS = "surveyId";
    public static final String PARAM_GROUP_NAME = "groupName";
    public static final String PARAM_SPECIES_NAME = "speciesName";
    public static final String PARAM_GROUPED = "grouped";

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private TaxaDAO taxaDAO;

    /**
     * Returns a geo json object of records that meet the passed parameters
     *
     * @param username     username to limit returned records. If null returns all records
     * @param limit        limit the number of returned records. Optional - default 5.
     * @param startDateStr the earliest date of returned records. If null, no limit.
     * @param endDateStr   the latest date of returned records . If null, no limit.
     * @param surveyIds    filter by survey IDs. If empty, return all surveys.
     * @param grouped      when true, only displays records with record groups.
     *                     Draws these records as a line, ordered by record 'when' date.
     *                     when false, will draw ignore groups even if they exist
     *                     and draw all records as points.
     * @param request      http request
     * @param response     http response
     * @throws IOException
     */
    // suppress the xss warning since we are supporting jsonp.
    // the jsonp callback argument is html sanitized.
    @SuppressWarnings({"XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER"})
    @RequestMapping(value = GET_RECORD_GEOJSON_URL, method = RequestMethod.GET)
    public void getGeoJson(
            @RequestParam(value = PARAM_USERNAME, defaultValue = "") String username,
            @RequestParam(value = PARAM_LIMIT, defaultValue = "5") int limit,
            @RequestParam(value = PARAM_START_DATE, required = false)
            String startDateStr,
            @RequestParam(value = PARAM_END_DATE, required = false)
            String endDateStr,
            @RequestParam(value = PARAM_SURVEY_IDS, required = false) int[] surveyIds,
            @RequestParam(value = PARAM_GROUP_NAME, required = false) String groupName,
            @RequestParam(value = PARAM_SPECIES_NAME, required = false) String speciesName,
            @RequestParam(value = PARAM_GROUPED, required = false, defaultValue = "false")
            boolean grouped,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

            Date startDate = null;
            if (StringUtils.notEmpty(startDateStr)) {
                try {
                    startDate = dateFormat.parse(startDateStr);
                } catch (ParseException e) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Bad start date : "
                            + startDateStr + ". Must be in following format : "
                            + DATE_FORMAT);
                    return;
                }
            }

            Date endDate = null;
            if (StringUtils.notEmpty(endDateStr)) {
                try {
                    endDate = dateFormat.parse(endDateStr);
                } catch (ParseException e) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Bad end date. : "
                            + endDateStr + " Must be in following format : "
                            + DATE_FORMAT);
                    return;
                }
            }

            List<Integer> speciesList;
            if (StringUtils.notEmpty(speciesName) || StringUtils.notEmpty(groupName)) {

                speciesList = taxaDAO.searchIndicatorSpeciesPk(groupName, speciesName, true);
                if (speciesList.isEmpty()) {
                    // return with error
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("No species returned for species search string : "
                            + HtmlUtils.htmlEscape(speciesName) + " and group search string : "
                            + HtmlUtils.htmlEscape(groupName));
                    return;
                }
            } else {
                speciesList = Collections.EMPTY_LIST;
            }

            User u = userDAO.getUser(username);
            List<Survey> surveys = new ArrayList<Survey>();
            if (surveyIds != null) {
                for (int surveyId : surveyIds) {
                    Survey s = surveyDAO.get(surveyId);
                    if (s != null) {
                        surveys.add(s);
                    }
                }
            }

            ScrollableRecords records = recordDAO.getScrollableRecords(u, surveys,
                    speciesList, startDate, endDate, 0, limit);

            String jsonpCallback = request.getParameter(BdrsWebConstants.JSONP_CALLBACK_PARAM);
            boolean jsonp = StringUtils.notEmpty(jsonpCallback);

            if (jsonp) {
                jsonpCallback = HtmlUtils.htmlEscape(jsonpCallback);
                response.getWriter().write(jsonpCallback + "(");
            }

            writeGeoJson(getRequestContext().getHibernate(),
                    response.getWriter(), records, grouped);

            if (jsonp) {
                response.getWriter().write(");");
                response.setContentType("application/javascript");
            } else {
                response.setContentType("application/json");
            }
        } catch (JSONException ex) {
            log.error("Error creating GeoJSON feed");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error creating GeoJSON feed : " + ex.getMessage());
            response.setContentType("text/html");
        }
    }

    /**
     * @param sesh hibernate session
     * @param w writer
     * @param records records to serialize
     * @param grouped when true, only displays records with record groups.
     *                Draws these records as a line, ordered by record 'when' date.
     *                when false, will draw ignore groups even if they exist
     *                and draw all records as points.
     */
    public void writeGeoJson(Session sesh, Writer w, ScrollableRecords records, boolean grouped) throws JSONException {

        JSONWriter jsonWriter = new JSONWriter(w);
        MfGeoJSONWriter geoJSONWriter = new MfGeoJSONWriter(jsonWriter);

        // Use json stream writing methods for more efficient writing...
        jsonWriter.object();
        jsonWriter.key("type");
        jsonWriter.value("FeatureCollection");

        jsonWriter.key("features");
        jsonWriter.array();

        // defaults to srid = 4326
        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();
        SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil();

        Set<Integer> includedGroupIds = new HashSet<Integer>();
        while (records.hasMoreElements()) {
            Record r = records.nextElement();

            if (!grouped) {
                if (r.getGeometry() != null) {
                    MfFeature feature = new RecordMinimalMfFeature(r, spatialUtil);
                    geoJSONWriter.encodeFeature(feature);
                }
            } else {
                // record must have a group
                RecordGroup recordGroup = r.getRecordGroup();
                if (recordGroup != null) {
                    if (!includedGroupIds.contains(recordGroup.getId())) {
                        includedGroupIds.add(recordGroup.getId());
                        ScrollableRecords sr = recordDAO.getRecordByGroup(recordGroup);

                        MfFeature lineFeature =
                                new RecordGroupLineMfFeature(r.getRecordGroup(), sr,
                                        spatialUtil, sesh);
                        geoJSONWriter.encodeFeature(lineFeature);

                        /*
                        List<Record> recList = new ArrayList<Record>();

                        while (sr.hasMoreElements()) {
                            recList.add(sr.nextElement());
                            sesh.clear();
                        }

                        // must have 2 points for track
                        if (recList.size() > 1) {
                            MfFeature lineFeature =
                                    new RecordGroupLineMfFeature(r.getRecordGroup(), recList);
                            geoJSONWriter.encodeFeature(lineFeature);
                        } // else we cant draw a line. ignore this record group.
                        */
                    }
                } // else we ignore the record
            }

            sesh.clear();
        }

        jsonWriter.endArray();
        jsonWriter.endObject();
    }
}


