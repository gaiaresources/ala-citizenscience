package au.com.gaiaresources.bdrs.service.python.customform;

import au.com.gaiaresources.bdrs.controller.RenderController;
import au.com.gaiaresources.bdrs.controller.customform.CustomFormController;
import au.com.gaiaresources.bdrs.controller.record.TrackerController;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOptionDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.python.PyBDRS;
import au.com.gaiaresources.bdrs.python.PyResponse;
import au.com.gaiaresources.bdrs.service.python.PythonService;
import au.com.gaiaresources.bdrs.service.taxonomy.BdrsTaxonLibException;
import au.com.gaiaresources.bdrs.service.taxonomy.TaxonLibSessionFactory;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import jep.Jep;
import jep.JepException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * The <code>CustomFormService</code> handles the rendering of custom forms from candidate views (such as the
 * {@link CustomFormController}
 */
@Service
public class CustomFormService extends PythonService {
    /**
     * The (mandatory) filename of the python custom form.
     */
    public static final String PYTHON_FORM = "form.py";

    /**
     * A Python snippet that executes a custom form and catches any errors and logging
     * them appropriately.
     */
    public static final String EXEC_TMPL;
    static {
        StringBuilder builder = new StringBuilder();
        builder.append("try:\n");
        builder.append("    Form().render(\"\"\"%s\"\"\", **__bdrs_kwargs__)\n");
        builder.append("except Exception, e:\n");
        builder.append("    import sys, traceback\n");
        builder.append("    response = bdrs.getResponse()\n");
        builder.append("    response.setError(True)\n");
        builder.append("    response.setErrorMsg(str(e))\n");
        builder.append("    response.setContent(traceback.format_exc())\n");

        EXEC_TMPL = builder.toString();
    }

    private Logger log = Logger.getLogger(this.getClass());
    @Autowired
    private FileService fileService;
    @Autowired
    private LocationService locationService;
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
    private TaxonLibSessionFactory taxonLibSessionFactory;

    /**
     * Renders the specified custom form.
     *
     * @param request  the client request.
     * @param response the server response.
     * @param form   the custom form to be executed.
     * @return the rendered custom form content.
     */
    public ModelAndView renderForm(HttpServletRequest request,
                                     HttpServletResponse response,
                                     CustomForm form) {
        PyBDRS bdrs = null;
        try {
            // Find the Python custom form code.
            File formDir = fileService.getTargetDirectory(form, CustomForm.CUSTOM_FORM_DIR, true);

            // Setup the parameters to send to the Python custom form.
            bdrs = new PyBDRS(request, locationService,
                    fileService, form, RequestContextHolder.getContext().getUser(),
                    surveyDAO, censusMethodDAO,
                    taxaDAO, recordDAO, portalDAO, attributeDAO, attributeOptionDAO, attributeValueDAO,
                    metadataDAO, locationDAO, taxonLibSessionFactory);
            JSONObject jsonParams = toJSONParams(request);

            // Fire up a new Python interpreter
            StringBuilder pythonPath = new StringBuilder();
            pythonPath.append(getProvidedPythonContentDir());
            pythonPath.append(File.pathSeparatorChar);
            pythonPath.append(formDir.getAbsolutePath());

            Jep jep = new Jep(false, pythonPath.toString(), Thread.currentThread().getContextClassLoader());
            // Set the Python bdrs global variable
            jep.set("bdrs", bdrs);

            // Create the map of named parameters that will be passed to the custom form.
            jep.eval("__bdrs_kwargs__ = {}");

            // Load and execute the custom form
            jep.runScript(new File(formDir, PYTHON_FORM).getAbsolutePath());

            jep.eval(String.format(EXEC_TMPL, jsonParams.toString()));
            // Terminate the interpreter
            jep.close();

            // Examine the custom form response
            PyResponse pyResponse = bdrs.getResponse();
            if (pyResponse.isError()) {
                // The custom form had some sort of error.
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
                    RequestContextHolder.getContext().addMessage("bdrs.customform.taxonlib.initError");
                } else {
                    RequestContextHolder.getContext().addMessage("bdrs.customform.render.error");
                }
                // We can't render the page, so redirect back to the listing page.
                return redirectToDefaultRenderer(request);
            } else if(pyResponse.isRedirect()) {
                return new ModelAndView(new PortalRedirectView(pyResponse.getRedirectURL(), true), pyResponse.getRedirectParams());
            } else {
                // Set the header of the Python custom form if there is one.
                // This allows the python custom form to provide file downloads if
                // necessary.
                updateHeader(response, pyResponse);
                // Set the content type of the python custom form. This is HTML
                // by default
                response.setContentType(pyResponse.getContentType());

                // If the content type is HTML then we can treat it as text,
                // otherwise we simply treat it as raw bytes.
                if (PyResponse.HTML_CONTENT_TYPE.equals(pyResponse.getContentType())) {
                    // If the custom form is standalone, then do not render the
                    // custom form with the usual header and footer.
                    if (pyResponse.isStandalone()) {
                        response.getWriter().write(pyResponse.getContentAsString());
                    } else {
                        // Embed the custom form in a model and view so that it will
                        // receive the usual header, menu and footer.
                        ModelAndView mv = new ModelAndView(RENDER_VIEW);
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
            log.error("Unable to render custom form with PK: " + form.getId(), je);
            RequestContextHolder.getContext().addMessage("bdrs.customform.render.error");
            return redirectToDefaultRenderer(request);
        } catch (IOException e) {
            // Occurs when there has been a problem reading/writing files.
            log.error("Unable to render custom form with PK: " + form.getId(), e);
            RequestContextHolder.getContext().addMessage("bdrs.customform.render.error");
            return redirectToDefaultRenderer(request);
        } catch(UnsatisfiedLinkError ule) {
            // Occurs if Jep is not set up correctly. e.g if jep is not on the java.library.path
            log.error("Unable to render custom form with PK: " + form.getId(), ule);
            RequestContextHolder.getContext().addMessage("bdrs.customform.render.error");
            return redirectToDefaultRenderer(request);
        } catch(NoClassDefFoundError ncdefe) {
            // Occurs if Jep is not set up correctly. e.g if jep is not on the java.library.path
            log.error("Unable to render custom form with PK: " + form.getId(), ncdefe);
            RequestContextHolder.getContext().addMessage("bdrs.customform.render.error");
            return redirectToDefaultRenderer(request);
        } catch (Exception e) {
            log.error("Exception thrown when attempting to render report", e);
            RequestContextHolder.getContext().addMessage(e.getMessage());
            return redirectToDefaultRenderer(request);
        } finally {
            if (bdrs != null) {
                bdrs.close();
            }
        }
    }

    /***
     * Used when there is an error rendering a custom form, this method returns a model and view to redirect
     * the browser to the default renderer for the survey.
     * @param request the client request
     * @return a model and view redirect to the default survey renderer.
     */
    private ModelAndView redirectToDefaultRenderer(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView(new PortalRedirectView(RenderController.SURVEY_RENDER_REDIRECT_URL, true));
        mv.addObject(RenderController.PARAM_REDIRECT_URL, TrackerController.EDIT_URL);
        mv.addAllObjects(request.getParameterMap());
        return mv;
    }
}
