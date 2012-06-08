package au.com.gaiaresources.bdrs.controller.customform;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSON;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.python.customform.CustomFormService;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.FileUtils;
import au.com.gaiaresources.bdrs.util.ZipUtils;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * This controller handles all requests for adding, listing, deleting and
 * generation of custom forms.
 */
@Controller
public class CustomFormController extends AbstractController {

    /**
     * Tile definition name for listing custom forms.
     */
    public static final String FORM_LISTING_VIEW = "customFormListing";
    /**
     * The URL to list all active custom forms.
     */
    public static final String FORM_LISTING_URL = "/customform/listing.htm";
    /**
     * The URL that accepts POST requests to add custom forms.
     */
    public static final String FORM_ADD_URL = "/customform/add.htm";
    /**
     * The URL to that accepts POST requests to remove custom forms.
     */
    public static final String FORM_DELETE_URL = "/customform/delete.htm";
    /**
     * The URL that GETs rendered custom forms.
     */
    public static final String FORM_RENDER_URL = "/customform/{formId}/render.htm";
    /**
     * The URL to GET static custom form files.
     */
    public static final String FORM_STATIC_URL = "/customform/{formId}/static.htm";
    /**
     * The key to retrieve the form file from the POST dictionary.
     */
    public static final String POST_KEY_ADD_FORM_FILE = "form_file";
    /**
     * The (mandatory) name of the configuration file containing the custom form
     * name and description.
     */
    public static final String FORM_CONFIG_FILENAME = "config.json";
    /**
     * The JSON configuration attribute for the custom form name.
     */
    public static final String JSON_CONFIG_NAME = "name";
    /**
     * The JSON configuration attribute for the custom form description.
     */
    public static final String JSON_CONFIG_DESCRIPTION = "description";
    /**
     * The JSON configuration attribute for the custom form directory.
     */
    public static final String JSON_CONFIG_FORM = "form";
    /**
     * The path variable name used to extract the primary key of the
     * current custom form.
     */
    public static final String FORM_ID_PATH_VAR = "formId";
    /**
     * The query parameter name containing the file path of a static custom form file.
     */
    public static final String FILENAME_QUERY_PARAM = "fileName";


    @Autowired
    private CustomFormDAO formDAO;
    @Autowired
    private FileService fileService;
    @Autowired
    private CustomFormService formService;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Lists all custom forms currently in the system.
     *
     * @param request  the browser request.
     * @param response the server response.
     * @return a list of all custom forms currently in the system.
     */
    @RolesAllowed({Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN})
    @RequestMapping(value = FORM_LISTING_URL, method = RequestMethod.GET)
    public ModelAndView listCustomForms(HttpServletRequest request,
                                        HttpServletResponse response) {

        ModelAndView mv = new ModelAndView(FORM_LISTING_VIEW);
        mv.addObject("customforms", formDAO.getCustomForms());
        return mv;
    }

    /**
     * Adds a custom form to the system sourcing the name and description from the configuration file.
     *
     * @param request  the browser request
     * @param response the server response
     * @return redirects to the custom form listing page.
     */
    @RolesAllowed({Role.ADMIN})
    @RequestMapping(value = FORM_ADD_URL, method = RequestMethod.POST)
    public ModelAndView addCustomForms(MultipartHttpServletRequest request,
                                       HttpServletResponse response) {

        ZipInputStream zis = null;
        File tempReportDir = null;
        InputStream configInputStream = null;
        try {
            tempReportDir = extractUploadedForm(request.getFile(POST_KEY_ADD_FORM_FILE));

            // Begin extraction of the form.
            File configFile = new File(tempReportDir, FORM_CONFIG_FILENAME);
            if (configFile.exists()) {
                configInputStream = new FileInputStream(configFile);
                JSON json = FileUtils.readJsonStream(configInputStream);
                if (!json.isArray()) {
                    JSONObject config = (JSONObject) json;

                    String formName = config.optString(JSON_CONFIG_NAME, null);
                    String formDescription = config.optString(JSON_CONFIG_DESCRIPTION, null);
                    String formDirName = config.optString(JSON_CONFIG_FORM);

                    File formDir = new File(tempReportDir, formDirName);
                    File formPy = new File(formDir, CustomFormService.PYTHON_FORM);

                    boolean isValid = formName != null && !formName.isEmpty() && formDescription != null && !formDescription.isEmpty();
                    boolean isFilesValid = formDir.exists() && formPy.exists();

                    if (isValid && isFilesValid) {
                        CustomForm form = new CustomForm();
                        form.setName(formName);
                        form.setDescription(formDescription);
                        form = formDAO.save(form);

                        // Move the python form code to the target directory.
                        File targetFormDir = fileService.getTargetDirectory(form, CustomForm.CUSTOM_FORM_DIR, true);
                        if (targetFormDir.exists()) {
                            log.warn("Target directory exists, attempting to delete : " + targetFormDir.getAbsolutePath());
                            try {
                                org.apache.commons.io.FileUtils.deleteDirectory(targetFormDir);
                            } catch (IOException ioe) {
                                log.error("Failed to delete target custom form directory", ioe);
                            }
                        }

                        // Using apache commons rather than File.renameTo because rename to cannot move the file
                        // between two different file systems.
                        org.apache.commons.io.FileUtils.copyDirectory(formDir, targetFormDir);
                        org.apache.commons.io.FileUtils.deleteDirectory(formDir);

                        // Success!
                        getRequestContext().addMessage("bdrs.customform.add.success", new Object[]{form.getName()});

                    } else {
                        // Invalid Report Config or Content
                        log.error("Failed to add Custom Form because the name is empty or null.");
                        getRequestContext().addMessage("bdrs.customform.add.error");
                    }
                } else {
                    // Malformed JSON
                    getRequestContext().addMessage("bdrs.customform.add.malformed_config");
                }
            } else {
                // Missing config file
                getRequestContext().addMessage("bdrs.customform.add.missing_config");
            }
        } catch (SecurityException se) {
            // This cannot happen because we should have the rights to remove
            // a file that we created.
            getRequestContext().addMessage("bdrs.customform.add.error");
            log.error("Unable to read the Custom Form file.", se);
        } catch (IOException ioe) {
            getRequestContext().addMessage("bdrs.customform.add.error");
            log.error("Unable to read the Custom Form file.", ioe);
        } catch (JSONException je) {
            getRequestContext().addMessage("bdrs.customform.add.malformed_config");
            log.error("Unable to parse the Custom Form config file.", je);
        } finally {
            try {
                if (zis != null) {
                    zis.close();
                }
                if (configInputStream != null) {
                    configInputStream.close();
                }
                if (tempReportDir != null) {
                    org.apache.commons.io.FileUtils.deleteDirectory(tempReportDir);
                }
            } catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe);
            }
        }

        return new ModelAndView(new PortalRedirectView(FORM_LISTING_URL, true));
    }

    /**
     * Deletes the specified custom form from the database.
     *
     * @param request  the browser request
     * @param response the server response
     * @param formId   the primary key of the form to be deleted
     * @return redirects to the form listing page
     */
    @RolesAllowed({Role.ADMIN})
    @RequestMapping(value = FORM_DELETE_URL, method = RequestMethod.POST)
    public ModelAndView deleteCustomForm(HttpServletRequest request,
                                         HttpServletResponse response,
                                         @RequestParam(required = true, value = FORM_ID_PATH_VAR) int formId) {

        CustomForm form = formDAO.getCustomForm(formId);
        if (form != null) {
            formDAO.delete(form);
            getRequestContext().addMessage("bdrs.customform.delete.success", new Object[]{form.getName()});
        } else {
            getRequestContext().addMessage("bdrs.customform.delete.not_found");
        }

        return new ModelAndView(new PortalRedirectView(FORM_LISTING_URL, true));
    }

    /**
     * Renders the specified custom form.
     *
     * @param request  the browser request.
     * @param response the server response.
     * @param formId   the primary key of the custom form to be rendered.
     */
    @RolesAllowed({Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN})
    @RequestMapping(value = FORM_RENDER_URL)
    public ModelAndView renderForm(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @PathVariable(FORM_ID_PATH_VAR) int formId) {
        if (!RequestMethod.POST.toString().equals(request.getMethod())) {
            super.requestRollback(request);
        }
        return formService.renderForm(request, response, formDAO.getCustomForm(formId));
    }


    /**
     * Allows custom forms to provide static files such as media or javascript
     * without the need to create a Python interpreter. This handler will
     * rewrite the URL and delegate the servicing of the request to the
     * {@link au.com.gaiaresources.bdrs.controller.file.DownloadFileController}.
     *
     * @param request  the browser request.
     * @param response the server response.
     * @param formId   the primary key of the custom form containing the static file.
     * @param fileName the relative path of the file to retrieve.
     */
    @RolesAllowed({Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN})
    @RequestMapping(value = FORM_STATIC_URL, method = RequestMethod.GET)
    public ModelAndView downloadStaticReportFile(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 @PathVariable(FORM_ID_PATH_VAR) int formId,
                                                 @RequestParam(required = true, value = FILENAME_QUERY_PARAM) String fileName) {
        ModelAndView mv = null;
        try {
            CustomForm form = formDAO.getCustomForm(formId);
            mv = formService.downloadStaticFile(form, fileName);
        } catch (NullPointerException npe) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return mv;
    }

    /**
     * Saves and decompresses the uploaded form to a temporary directory.
     *
     * @param uploadedForm the uploaded form.
     * @return the directory containing the decompressed form
     * @throws java.io.IOException thrown if there is a problem reading or writing the files.
     */
    private File extractUploadedForm(MultipartFile uploadedForm) throws IOException {

        File tempFormZip = File.createTempFile("form", "zip");
        tempFormZip.deleteOnExit();
        FileUtils.writeBytesToFile(uploadedForm.getBytes(), tempFormZip);

        // Decompress the form to a temporary location
        File tempFormDir = FileUtils.createTempDirectory(String.valueOf(System.currentTimeMillis()));
        tempFormDir.deleteOnExit();
        ZipUtils.decompressToDir(tempFormZip, tempFormDir);

        boolean deleteSuccess = tempFormZip.delete();
        if (!deleteSuccess) {
            log.warn("Failed to delete temporary form zip file: " + tempFormZip.getAbsolutePath());
        }
        return tempFormDir;
    }
}
