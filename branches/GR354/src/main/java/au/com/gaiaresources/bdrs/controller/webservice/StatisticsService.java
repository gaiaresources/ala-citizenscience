package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private JsonService jsonService;

    private Logger log = Logger.getLogger(getClass());

    @RequestMapping(value = LATEST_STATS_URL, method = RequestMethod.GET)
    public void latestStatistics(HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {

        Record latestRecord = recordDAO.getLatestRecord();

        JSONObject obj = new JSONObject();

        obj.put("recordCount", recordDAO.countAllRecords());
        obj.put("uniqueSpeciesCount", recordDAO.countUniqueSpecies());
        obj.put("userCount", userDAO.countUsers());

        if (latestRecord != null) {
            AccessControlledRecordAdapter ar = new AccessControlledRecordAdapter(latestRecord, null);
            JSONObject recJson = jsonService.toJson(ar, request.getContextPath(), new SpatialUtilFactory(), true);
            obj.put("latestRecord", recJson);
        }
        writeJson(request, response, obj.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
