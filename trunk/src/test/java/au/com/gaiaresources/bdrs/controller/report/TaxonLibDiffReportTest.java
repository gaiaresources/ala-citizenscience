package au.com.gaiaresources.bdrs.controller.report;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.taxonomy.AbstractBdrsMaxImporterTest;

/**
 * Test the taxon lib diff report
 *
 */
public class TaxonLibDiffReportTest extends AbstractBdrsMaxImporterTest {

    /**
     * Relative path to directory containing report
     */
    public static final String TAXON_DIFF_REPORT_DIR = "reports/TaxonLibDiff/";
    
    @Autowired
    private ReportDAO reportDAO;
	
	/**
     * Tests the report runs without errors however there are no assertions.
     * 
     * @throws Exception
     */
    @Test
    public void testTaxonLibDiffReport() throws Exception {
    	runDefaultImport();
    	
    	login("admin", "password", new String[] { Role.ADMIN });

        String testReportName = "TaxonLibDiffReport";

        // Upload the Report
        request.setMethod("POST");
        request.setRequestURI(ReportController.REPORT_ADD_URL);

        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        File reportDir = new File(TAXON_DIFF_REPORT_DIR);
        req.addFile(ReportTestUtil.getTestReport(reportDir, testReportName));

        handle(request, response);
        Assert.assertFalse(reportDAO.getReports().isEmpty());
        Assert.assertEquals(1, getRequestContext().getMessageContents().size());
        
        JSONObject config = ReportTestUtil.getConfigFile(reportDir);
        String reportName = config.getString(ReportController.JSON_CONFIG_NAME);
        Report report = ReportTestUtil.getReportByName(reportDAO, reportName);
        
        Assert.assertNotNull("Report cant be null", report);
        
        String renderURL = ReportTestUtil.getReportRenderURL(report);
        
        // Render the report start page
        request.setMethod("GET");
        request.setParameter("startDate", "20 Mar 2012");
        request.setParameter("endDate", "21 Mar 2012");
        request.setParameter("submitReportParams", "true");
        request.setParameter("itemsPerPage", "10");
        request.setParameter("pageNumber", "1");
        request.setRequestURI(renderURL);
        handle(request, response);
        // If everything works as desired, there should be no messages
        Assert.assertTrue(getRequestContext().getMessageContents().isEmpty());
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
