package au.com.gaiaresources.bdrs.service.python.report;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.gaiaresources.bdrs.model.record.RecordGroupDAO;
import au.com.gaiaresources.bdrs.model.taxa.*;
import jep.Jep;
import jep.JepException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.report.ReportController;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportCapability;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.python.PyBDRS;
import au.com.gaiaresources.bdrs.python.PyResponse;
import au.com.gaiaresources.bdrs.python.model.PyScrollableRecords;
import au.com.gaiaresources.bdrs.service.python.PythonService;
import au.com.gaiaresources.bdrs.service.taxonomy.BdrsTaxonLibException;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * The <code>ReportService</code> handles the rendering of reports from candidate views (such as the
 * {@link au.com.gaiaresources.bdrs.controller.report.ReportController} or
 * {@link au.com.gaiaresources.bdrs.controller.review.sightings.AdvancedReviewSightingsController}.
 */
@Service
public class ReportService extends PythonService {
    /**
     * The (mandatory) filename of the python report.
     */
    public static final String PYTHON_REPORT = "report.py";
    /**
     * A Python snippet that executes a report and catches any errors and logging
     * them appropriately.
     */
    public static final String REPORT_EXEC_TMPL;
    /**
     * Tile definition name for rendering reports.
     */
    public static final String REPORT_RENDER_VIEW = "pythonRender";

    static {
        StringBuilder builder = new StringBuilder();
        builder.append("try:\n");
        builder.append("    Report().content(\"\"\"%s\"\"\", **__bdrs_kwargs__)\n");
        builder.append("except Exception, e:\n");
        builder.append("    import sys, traceback\n");
        builder.append("    response = bdrs.getResponse()\n");
        builder.append("    response.setError(True)\n");
        builder.append("    response.setErrorMsg(str(e))\n");
        builder.append("    response.setContent(traceback.format_exc())\n");
        builder.append("finally:\n");
        builder.append("    from django.db import connections\n");
        builder.append("    for conn in connections.all():\n");
        builder.append("        conn.close()\n");

        REPORT_EXEC_TMPL = builder.toString();
    }

    private Logger log = Logger.getLogger(this.getClass());
    @Autowired
    private FileService fileService;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private CensusMethodDAO censusMethodDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private PortalDAO portalDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private AttributeOptionDAO attributeOptionDAO;
    @Autowired
    private AttributeValueDAO attributeValueDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;
    @Autowired
    private RecordGroupDAO recordGroupDAO;
    
    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    /**
     * Renders the specified report.
     *
     * @param request  the client request.
     * @param response the server response.
     * @param report   the report to be executed.
     * @return the rendered report content.
     */
    public ModelAndView renderReport(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Report report) {
        return this.renderReport(request, response, report, null);
    }

    /**
     * Renders the specified report.
     *
     * @param request  the client request.
     * @param response the server response.
     * @param report   the report to be executed.
     * @param sc       the scrollable records to be passed to the report as key word arguments to the content function.
     * @return the rendered report content.
     */
    public ModelAndView renderReport(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Report report,
                                     ScrollableResults<Record> sc) {
        PyBDRS bdrs = null;
        try {
            // Find the Python report code.
            File reportDir = fileService.getTargetDirectory(report, Report.REPORT_DIR, true);

            // Setup the parameters to send to the Python report.
            bdrs = new PyBDRS(request, spatialUtil,
                    fileService, report, RequestContextHolder.getContext().getUser(),
                    surveyDAO, censusMethodDAO,
                    taxaDAO, recordDAO, portalDAO, attributeDAO, attributeOptionDAO, attributeValueDAO,
                    metadataDAO, locationDAO, speciesProfileDAO, recordGroupDAO);
            JSONObject jsonParams = toJSONParams(request);
            // Fire up a new Python interpreter
            StringBuilder pythonPath = new StringBuilder();
            for(String providedContentDir : getProvidedPythonContentDirs()) {
                pythonPath.append(providedContentDir);
                pythonPath.append(File.pathSeparatorChar);
            }

            // If there are no content dirs, then the pythonpath is empty and you do not need a path separator,
            // otherwise, it is not empty and it already ends with a path separator.
            pythonPath.append(reportDir.getAbsolutePath());
            log.info("Python Path = "+pythonPath);

            Jep jep = new Jep(false, pythonPath.toString(), Thread.currentThread().getContextClassLoader());
            // Set the Python bdrs global variable
            jep.set("bdrs", bdrs);

            // Configure and import django into the python interpreter
           super.loadDjango(jep, bdrs);

            // Create the map of named parameters that will be passed to the report.
            jep.eval("__bdrs_kwargs__ = {}");
            if (sc != null) {
                addKeyWordArgument(jep, ReportCapability.SCROLLABLE_RECORDS,
                        new PyScrollableRecords(sc, true, true, true));
            }

            // Load and execute the report
            jep.runScript(new File(reportDir, PYTHON_REPORT).getAbsolutePath());

            jep.eval(String.format(REPORT_EXEC_TMPL, jsonParams.toString()));
            // Terminate the interpreter
            jep.close();

            // Examine the report response
            PyResponse pyResponse = bdrs.getResponse();
            if (pyResponse.isError()) {
                // The report had some sort of error.
                log.error(pyResponse.getContentAsString());
                // Let the user know that an error has occurred.
                String errorMsg = pyResponse.getErrorMsg();
                // Work out what kind of error it is....Note we throw away the
                // exception message at the moment.
                // This is necessary to work out what java exceptions are thrown
                // (such as from pyBDRS) within the JEP runtime. When passing
                // through the JEP runtime all exceptions are serialized into a string
                // with the format:
                // classname : message
                // e.g:
                // au.com.gaiaresources.bdrs.service.taxonomy.BdrsTaxonLibException : An error has occured
                if (errorMsg != null && errorMsg.startsWith(BdrsTaxonLibException.class.getCanonicalName())) {
                    RequestContextHolder.getContext().addMessage("bdrs.report.taxonlib.initError");
                } else {
                    RequestContextHolder.getContext().addMessage("bdrs.report.render.error");
                }
                // We can't render the page, so redirect back to the listing page.
                return new ModelAndView(new PortalRedirectView(ReportController.REPORT_LISTING_URL, true));
            } else {
                // Set the header of the Python report if there is one.
                // This allows the python report to provide file downloads if
                // necessary.
                updateHeader(response, pyResponse);
                // Set the content type of the python report. This is HTML 
                // by default
                response.setContentType(pyResponse.getContentType());

                // If the content type is HTML then we can treat it as text,
                // otherwise we simply treat it as raw bytes.
                if (PyResponse.HTML_CONTENT_TYPE.equals(pyResponse.getContentType())) {
                    // If the report is standalone, then do not render the
                    // report with the usual header and footer.
                    if (pyResponse.isStandalone()) {
                        response.getWriter().write(pyResponse.getContentAsString());
                    } else {
                        // Embed the report in a model and view so that it will
                        // receive the usual header, menu and footer.
                        ModelAndView mv = new ModelAndView(REPORT_RENDER_VIEW);
                        mv.addObject("renderContent", pyResponse.getContentAsString());
                        return mv;
                    }
                } else {
                    // Treat the data as a byte array and simply squirt them
                    // down the pipe.
                    byte[] content = pyResponse.getContent();
                    response.setContentLength(content.length);
                    ServletOutputStream outputStream = response.getOutputStream();
                    outputStream.write(content);
                    outputStream.flush();
                }
            }
            return null;

        } catch (JepException je) {
            // Jep Exceptions will occur if there has been a problem on the
            // Python side of the fence.
            log.error("Unable to render report with PK: " + report.getId(), je);
            RequestContextHolder.getContext().addMessage("bdrs.report.render.error");
            return redirectToListing();
        } catch (IOException e) {
            // Occurs when there has been a problem reading/writing files.
            log.error("Unable to render report with PK: " + report.getId(), e);
            RequestContextHolder.getContext().addMessage("bdrs.report.render.error");
            return redirectToListing();
        } catch (Exception e) {
        	log.error("Exception thrown when attempting to render report", e);
        	RequestContextHolder.getContext().addMessage(e.getMessage());
        	return redirectToListing();
        } finally {
        	if (bdrs != null) {
        		bdrs.close();
        	}
        }
    }

    private ModelAndView redirectToListing() {
    	return new ModelAndView(new PortalRedirectView(ReportController.REPORT_LISTING_URL, true));
    }

    /**
     * Adds a named parameter to the invocation of the report.
     *
     * @param jep        The Python runtime where the argument will be inserted.
     * @param capability The type of parameter being provided. The parameter will be inserted using the
     *                   lower cased toString of this enum.
     * @param parameter  The data to be passed to the report
     * @throws JepException Thrown if there was an error communicating to the Python runtime.
     */
    private void addKeyWordArgument(Jep jep, ReportCapability capability, Object parameter) throws JepException {
        String key = capability.toString().toLowerCase();
        // Place the data in a temporary variable
        jep.set("__bdrs_temp__", parameter);
        // Insert the data into the dictionary using Python
        jep.eval(String.format("__bdrs_kwargs__['%s'] = __bdrs_temp__", key));
        // Delete the reference to prevent cluttering up the global namespace
        jep.eval("del __bdrs_temp__");
    }
}
