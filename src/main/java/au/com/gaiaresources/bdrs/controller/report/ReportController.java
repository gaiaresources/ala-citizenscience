package au.com.gaiaresources.bdrs.controller.report;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.file.DownloadFileController;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSON;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportCapability;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.model.report.impl.ReportView;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.python.report.ReportService;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.FileUtils;
import au.com.gaiaresources.bdrs.util.ImageUtil;
import au.com.gaiaresources.bdrs.util.StringUtils;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

/**
 * This controller handles all requests for adding, listing, deleting and
 * generation of reports.
 */
@Controller
public class ReportController extends AbstractController {

    /**
     * Tile definition name for listing reports.
     */
    public static final String REPORT_LISTING_VIEW = "reportListing";
    /**
     * The URL to list all active reports.
     */
    public static final String REPORT_LISTING_URL = "/report/listing.htm";
    /**
     * The URL that accepts POST requests to add reports.
     */
    public static final String REPORT_ADD_URL = "/report/add.htm";
    /**
     * The URL to that accepts POST requests to remove reports.
     */
    public static final String REPORT_DELETE_URL = "/report/delete.htm";
    /**
     * The URL that GETs rendered reports.
     */
    public static final String REPORT_RENDER_URL = "/report/{reportId}/render.htm";
    /**
     * The URL to GET static report files.
     */
    public static final String REPORT_STATIC_URL = "/report/{reportId}/static.htm";
    
    public static final String POST_KEY_ADD_REPORT_FILE = "report_file";
    /**
     * The (mandatory) name of the configuration file containing the report
     * name and description.
     */
    public static final String REPORT_CONFIG_FILENAME = "config.json";  
    /**
     * The JSON configuration attribute for the report name.
     */
    public static final String JSON_CONFIG_NAME = "name";
    /**
     * The JSON configuration attribute for the report description.
     */
    public static final String JSON_CONFIG_DESCRIPTION = "description";
    /**
     * The JSON configuration attribute for the report icon file path.
     */
    public static final String JSON_CONFIG_ICON = "icon";
    /**
     * The JSON configuration attribute for the report directory.
     */
    public static final String JSON_CONFIG_REPORT = "report";
    /**
     * The JSON configuration attribute for the ability of the report to consume data.
     */
    public static final String JSON_CONFIG_CAPABILITY = "capabilities";
    /**
     * The JSON configuration attribute for the pages where the report may be accessed.
     */
    public static final String JSON_CONFIG_VIEWS = "views";
    /**
     * The minimum level of user role required to access the report.
     */
    public static final String JSON_CONFIG_USER_ROLE = "user_role";
    
    /**
     * The resized width of the report icon. This is the icon that is 
     * displayed on the report listing screen.
     */
    public static final int ICON_WIDTH = 128;
    /**
     * The resized height of the report icon. This is the icon that is 
     * displayed on the report listing screen.
     */
    public static final int ICON_HEIGHT = 128;
    /**
     * The target image format of the resized report icon.
     */
    public static final String ICON_FORMAT = "png";
    /**
     * The path variable name used to extract the primary key of the 
     * current report.
     */
    public static final String REPORT_ID_PATH_VAR = "reportId";
    /**
     * The query parameter name containing the file path of a static report file.
     */
    public static final String FILENAME_QUERY_PARAM = "fileName";

    
    @Autowired
    private ReportDAO reportDAO;
    @Autowired
    private FileService fileService;
    @Autowired
    private ReportService reportService;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Lists all reports currently in the system.
     * 
     * @param request the browser request.
     * @param response the server response.
     * @return a list of all reports currently in the system.
     */
    @RequestMapping(value = REPORT_LISTING_URL, method = RequestMethod.GET)
    public ModelAndView listReports(HttpServletRequest request,
            HttpServletResponse response) {

        ModelAndView mv = new ModelAndView(REPORT_LISTING_VIEW);
        mv.addObject("reports", reportDAO.getReports());
        return mv;
    }
    
    /**
     * Adds a report to the system sourcing the report name, description and 
     * icon from the configuration file.
     * 
     * @param request the browser request
     * @param response the server response
     * @return redirects to the report listing page.
     */
    @RolesAllowed({  Role.ADMIN })
    @RequestMapping(value = REPORT_ADD_URL, method = RequestMethod.POST)
    public ModelAndView addReports(MultipartHttpServletRequest request,
            HttpServletResponse response) {
        
        ZipInputStream zis = null;
        File tempReportDir = null;
        InputStream configInputStream = null;
        try {
            tempReportDir = extractUploadedReport(request.getFile(POST_KEY_ADD_REPORT_FILE));

            // Begin extraction of the report.
            File configFile = new File(tempReportDir, REPORT_CONFIG_FILENAME);
            if(configFile.exists()) {
                configInputStream = new FileInputStream(configFile);
                JSON json = FileUtils.readJsonStream(configInputStream);
                if(!json.isArray()) {
                    JSONObject config = (JSONObject) json;
                    
                    String reportName = config.optString(JSON_CONFIG_NAME, null);
                    String reportDescription = config.optString(JSON_CONFIG_DESCRIPTION, null);
                    String iconFilename = config.optString(JSON_CONFIG_ICON);
                    String reportDirName = config.optString(JSON_CONFIG_REPORT);
                    String reportUserRole = config.optString(JSON_CONFIG_USER_ROLE);
                    JSONArray capabilityArray = config.optJSONArray(JSON_CONFIG_CAPABILITY);
                    JSONArray viewArray = config.optJSONArray(JSON_CONFIG_VIEWS);
                    if(viewArray == null) {
                        viewArray = new JSONArray();
                    }
                    if(capabilityArray == null) {
                        capabilityArray = new JSONArray();
                    }

                    boolean validUserRole = true;
                    if(StringUtils.nullOrEmpty(reportUserRole)) {
                        // default
                        reportUserRole = Role.USER;
                    } else {
                        List roleList = Arrays.asList(Role.getAllRoles());
                        if (!roleList.contains(reportUserRole)) {
                            log.error("Attempted to assign an invalid user role to a report : " + reportUserRole);
                            validUserRole = false;
                        }
                    }
                    
                    File iconFile = new File(tempReportDir, iconFilename);
                    File reportDir = new File(tempReportDir, reportDirName);
                    File reportPy = new File(reportDir, ReportService.PYTHON_REPORT);
                    
                    boolean isReportValid = reportName != null && !reportName.isEmpty() && 
                                            reportDescription != null &&
                            !reportDescription.isEmpty() && !viewArray.isEmpty() && validUserRole;
                    boolean isFilesValid = iconFile.exists() && reportDir.exists() && reportPy.exists(); 
                    
                    if(isReportValid && isFilesValid) {
                        
                        String targetIconFilename = String.format("%s.%s", FilenameUtils.removeExtension(iconFilename), ICON_FORMAT);
                        Report report = new Report(reportName, reportDescription, targetIconFilename, true);
                        report.setUserRole(reportUserRole);

                        // Report capability
                        Set<ReportCapability> capabilitySet = new HashSet<ReportCapability>();
                        for(int i=0; i<capabilityArray.size(); i++) {
                            capabilitySet.add(ReportCapability.valueOf(capabilityArray.getString(i)));
                        }
                        report.setCapabilities(capabilitySet);

                        // Report views
                        Set<ReportView> viewSet = new HashSet<ReportView>();
                        for(int i=0; i<viewArray.size(); i++) {
                            viewSet.add(ReportView.valueOf(viewArray.getString(i)));
                        }
                        report.setViews(viewSet);

                        report = reportDAO.save(report);
                        
                        // Scale the icon file.
                        scaleReportIcon(report, iconFile);
                        boolean deleteSuccess = iconFile.delete();
                        if(!deleteSuccess) {
                            log.warn("Failed to delete icon file at: "+iconFile.getAbsolutePath());
                        }
                        
                        // Move the python report code to the target directory.
                        File targetReportDir = fileService.getTargetDirectory(report, Report.REPORT_DIR, true);
                        if (targetReportDir.exists()) {
                            log.warn("Target directory exists, attempting to delete : " + targetReportDir.getAbsolutePath());
                            try {
                                org.apache.commons.io.FileUtils.deleteDirectory(targetReportDir);    
                            } catch (IOException ioe) {
                                log.error("Failed to delete target report directory", ioe);
                            }
                        }

                        // Using apache commons rather than File.renameTo because rename to cannot move the file
                        // between two different file systems.
                        org.apache.commons.io.FileUtils.copyDirectory(reportDir, targetReportDir);
                        org.apache.commons.io.FileUtils.deleteDirectory(reportDir);

                        // Success!
                        getRequestContext().addMessage("bdrs.report.add.success", new Object[]{ report.getName() });
                        
                    } else {
                        // Invalid Report Config or Content
                        log.error("Failed to add report because the name or description is null or the report or icon cannot be found or the user role is invalid.");
                        getRequestContext().addMessage("bdrs.report.add.error");
                    }
                } else {
                    // Malformed JSON
                    getRequestContext().addMessage("bdrs.report.add.malformed_config");
                }
            } else {
                // Missing config file
                getRequestContext().addMessage("bdrs.report.add.missing_config");
            }
        } catch(SecurityException se) {
            // This cannot happen because we should have the rights to remove
            // a file that we created.
            getRequestContext().addMessage("bdrs.report.add.error");
            log.error("Unable to read the report file.", se);
        } catch(IOException ioe) {
            getRequestContext().addMessage("bdrs.report.add.error");
            log.error("Unable to read the report file.", ioe);
        } catch(JSONException je) {
            getRequestContext().addMessage("bdrs.report.add.malformed_config");
            log.error("Unable to parse the report config file.", je);
        } finally {
            try {
                if(zis != null) {
                    zis.close();
                }
                if(configInputStream != null) {
                    configInputStream.close();
                }
                if(tempReportDir != null) {
                    org.apache.commons.io.FileUtils.deleteDirectory(tempReportDir);
                }
            } catch(IOException ioe) {
                log.error(ioe.getMessage(), ioe);
            }
        }
        
        return new ModelAndView(new PortalRedirectView(REPORT_LISTING_URL, true));
    }
    
    /**
     * Deletes the specified report from the database.
     * 
     * @param request the browser request
     * @param response the server response
     * @param reportId the primary key of the report to be deleted
     * @return redirects to the report listing page
     */
    @RolesAllowed({  Role.ADMIN })
    @RequestMapping(value = REPORT_DELETE_URL, method = RequestMethod.POST)
    public ModelAndView deleteReport(HttpServletRequest request,
                                     HttpServletResponse response,
                                     @RequestParam(required = true, value=REPORT_ID_PATH_VAR) int reportId) {
        
        Report report = reportDAO.getReport(reportId);
        if(report != null) {
            reportDAO.delete(report);
            getRequestContext().addMessage("bdrs.report.delete.success", new Object[]{ report.getName() });
        } else {
            getRequestContext().addMessage("bdrs.report.delete.not_found");
        }
        
        return new ModelAndView(new PortalRedirectView(REPORT_LISTING_URL, true));
    }
    
    /**
     * Renders the specified report.
     * 
     * @param request the browser request.
     * @param response the server response.
     * @param reportId the primary key of the report to be rendered.
     */
    @RequestMapping(value = REPORT_RENDER_URL)
    public ModelAndView renderReport(HttpServletRequest request,
                                     HttpServletResponse response,
                                     @PathVariable(REPORT_ID_PATH_VAR) int reportId) {
        Report report = reportDAO.getReport(reportId);
        User loggedInUser = getRequestContext().getUser();
        boolean auth;
        String reportUserRole = report.getUserRole();
        if (loggedInUser == null) {
            auth = reportUserRole.equals(Role.ANONYMOUS);
        } else {
            String ur = Role.getHighestRole(loggedInUser.getRoles());
            auth = Role.isRoleHigherThanOrEqualTo(ur, reportUserRole);
        }
        if (auth) {
            return reportService.renderReport(request, response, report);
        } else {
            getRequestContext().addMessage("bdrs.report.render.auth_fail");
            return new ModelAndView(new PortalRedirectView(REPORT_LISTING_URL, true));
        }
    }
    
    /**
     * Allows reports to provide static files such as media or javascript
     * without the need to create a Python interpreter. This handler will
     * rewrite the URL and delegate the servicing of the request to the 
     * {@link DownloadFileController}.
     * 
     * @param request the browser request.
     * @param response the server response.
     * @param reportId the primary key of the report containing the static file.
     * @param fileName the relative path of the file to retrieve.
     */
    @RequestMapping(value = REPORT_STATIC_URL, method = RequestMethod.GET)
    public ModelAndView downloadStaticReportFile(HttpServletRequest request,
                                     HttpServletResponse response,
                                     @PathVariable(REPORT_ID_PATH_VAR) int reportId,
                                     @RequestParam(required = true, value=FILENAME_QUERY_PARAM) String fileName) {
        ModelAndView mv = null;
        try {
            Report report = reportDAO.getReport(reportId);
            mv = reportService.downloadStaticFile(report, fileName);
        } catch(NullPointerException npe) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return mv;
    }

    /**
     * Scales the specified icon to the size required by the report listing view.
     * 
     * @param report the report instance where the file will be saved.
     * @param iconFile the file to be resized.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void scaleReportIcon(Report report, File iconFile) throws IOException {
        InputStream iconInputStream = new FileInputStream(iconFile);
        BufferedImage resizedIcon = ImageUtil.resizeImage(iconInputStream, ICON_WIDTH, ICON_HEIGHT);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedIcon, ICON_FORMAT, baos);
        baos.flush();
        fileService.createFile(report.getClass(), report.getId(), report.getIconFilename(), baos.toByteArray());
    }
    
    /**
     * Saves and decompresses the uploaded report to a temporary directory.
     * 
     * @param uploadedReport the uploaded report.
     * @return the directory containing the decompressed report
     * @throws IOException thrown if there is a problem reading or writing the files.
     */
    private File extractUploadedReport(MultipartFile uploadedReport) throws IOException {

        File tempReportZip = File.createTempFile("report", "zip");
        tempReportZip.deleteOnExit();
        FileUtils.writeBytesToFile(uploadedReport.getBytes(), tempReportZip);
        
        // Decompress the report to a temporary location
        File tempReportDir = FileUtils.createTempDirectory(String.valueOf(System.currentTimeMillis()));
        tempReportDir.deleteOnExit();
        ZipUtils.decompressToDir(tempReportZip, tempReportDir);
        
        boolean deleteSuccess = tempReportZip.delete();
        if(!deleteSuccess) {
            log.warn("Failed to delete temporary report zip file: " + tempReportZip.getAbsolutePath());
        }

        return tempReportDir;
    }
}
