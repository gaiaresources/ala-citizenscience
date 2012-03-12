/**
 * 
 */
package au.com.gaiaresources.bdrs.controller.report;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.mock.web.MockMultipartFile;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.util.ZipUtils;

public class ReportTestUtil {
    
    private ReportTestUtil() {
        // Do nothing. Cannot instantiate a utility class.
    }
        
    public static String getReportRenderURL(Report report) {
        return ReportController.REPORT_RENDER_URL.replace("{reportId}", String.valueOf(report.getId()));
    }

    public static Report getReportByName(ReportDAO reportDAO, String reportName) {
        for(Report report : reportDAO.getReports()) {
            if(report.getName().equals(reportName)) {
                return report;
            }
        }
        return null;
    }
    
    public static MockMultipartFile getTestReport(File dir, String reportName) throws URISyntaxException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipUtils.compressToStream(dir.listFiles(), baos);
        
        return new MockMultipartFile(ReportController.POST_KEY_ADD_REPORT_FILE, 
                                     String.format("%s.zip", reportName), 
                                     "application/zip", 
                                     baos.toByteArray());
    }
    
    public static JSONObject getConfigFile(File reportDir) throws IOException, URISyntaxException {
        File config = new File(reportDir, ReportController.REPORT_CONFIG_FILENAME);
        return JSONObject.fromStringToJSONObject(readFileAsString(config.getAbsolutePath()));
    }
    
    public static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null)
                try {
                    f.close();
                } catch (IOException ignored) {
                }
        }
        return new String(buffer);
    }
}
