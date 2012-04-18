/**
 *
 */
package au.com.gaiaresources.bdrs.controller.report;

import au.com.gaiaresources.bdrs.python.PythonTestUtil;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Performs test utility functions that assist report testing.
 */
public class ReportTestUtil {

    private ReportTestUtil() {
        // Do nothing. Cannot instantiate a utility class.
    }

    /**
     * @see PythonTestUtil#getRenderURL(String, String, au.com.gaiaresources.bdrs.db.impl.PersistentImpl)
     */
    public static String getReportRenderURL(Report report) {
        return PythonTestUtil.getRenderURL(ReportController.REPORT_RENDER_URL, "{reportId}", report);
    }

    /**
     * Returns the first report with the specified name.
     * @param reportDAO performs database retrieval of reports.
     * @param reportName the name of the report to be returned.
     * @return the first report with the specified name.
     */
    public static Report getReportByName(ReportDAO reportDAO, String reportName) {
        for (Report report : reportDAO.getReports()) {
            if (report.getName().equals(reportName)) {
                return report;
            }
        }
        return null;
    }

    /**
     * @see PythonTestUtil#getTestFile(java.io.File, String, String)
     */
    public static MockMultipartFile getTestReport(File dir, String reportName) throws URISyntaxException, IOException {
        return PythonTestUtil.getTestFile(dir, reportName, ReportController.POST_KEY_ADD_REPORT_FILE);
    }


    /**
     * @see PythonTestUtil#getConfigFile(java.io.File, String)
     */
    public static JSONObject getConfigFile(File reportDir) throws IOException, URISyntaxException {
        return PythonTestUtil.getConfigFile(reportDir, ReportController.REPORT_CONFIG_FILENAME);
    }

    /**
     * @see au.com.gaiaresources.bdrs.python.PythonTestUtil#readFileAsString(String)
     */
    public static String readFileAsString(String filePath) throws IOException {
        return PythonTestUtil.readFileAsString(filePath);
    }
}
