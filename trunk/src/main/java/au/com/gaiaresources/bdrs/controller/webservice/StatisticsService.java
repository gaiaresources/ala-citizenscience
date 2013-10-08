package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 2/10/13
 * Time: 1:22 PM
 * To change this template use File | Settings | File Templates.
 */

@Controller
public class StatisticsService extends AbstractController {

    public static final String LATEST_STATS_URL = "/webservice/statistics/latest_statistics.htm";

    public static final String PARAM_USER_NAME = "username";

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private JsonService jsonService;

    private Logger log = Logger.getLogger(getClass());

    @RequestMapping(value = LATEST_STATS_URL, method = RequestMethod.GET)
    public void latestStatistics(
            @RequestParam(value = PARAM_USER_NAME, defaultValue = "") String username,
            HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {

        Record latestRecord = recordDAO.getLatestRecord();

        User user = userDAO.getUser(username);

        JSONObject obj = new JSONObject();

        obj.put("recordCount", recordDAO.countAllRecords());
        obj.put("uniqueSpeciesCount", recordDAO.countUniqueSpecies());
        obj.put("userCount", userDAO.countUsers());
        if (latestRecord != null) {
            obj.put("latestRecord", getRecordJson(latestRecord, request));
        }

        if (user != null) {
            String fullName = String.format("%s %s",
                    user.getFirstName() == null ? "" : user.getFirstName(),
                    user.getLastName() == null ? "" : user.getLastName()).trim();
            obj.put("userFullName", fullName);
            obj.put("userRecordCount", recordDAO.countRecords(user));
            obj.put("userUniqueSpeciesCount", recordDAO.countSpecies(user));

            Record userLatestRecord = recordDAO.getLatestRecord(user);
            if (userLatestRecord != null) {
                obj.put("userLatestRecord", getRecordJson(userLatestRecord, request));
            }
        }
        writeJson(request, response, obj.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private JSONObject getRecordJson(Record r, HttpServletRequest request) {
        AccessControlledRecordAdapter ar = new AccessControlledRecordAdapter(r, null);
        return jsonService.toJson(ar, request.getContextPath(), new SpatialUtilFactory(), true);
    }
}
