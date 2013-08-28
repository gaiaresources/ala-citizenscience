package au.com.gaiaresources.bdrs.controller.survey;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.activation.FileDataSource;
import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.map.AbstractEditMapController;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.survey.SurveyImportExportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.FileUtils;
import au.com.gaiaresources.bdrs.util.ImageUtil;
import au.com.gaiaresources.bdrs.util.ZipUtils;

@Controller
public class SurveyBaseController extends AbstractEditMapController {
    
    /**
     * URL of the first page of survey editing
     */
    public static final String SURVEY_EDIT_URL_INITIAL_PAGE = "/bdrs/admin/survey/edit.htm";
    /**
     * The URL of the handler to retrieve an exported survey.
     */
    public static final String SURVEY_EXPORT_URL = "/bdrs/admin/survey/export.htm";
    /**
     * The URL of the handler to parse an imported survey.
     */
    public static final String SURVEY_IMPORT_URL = "/bdrs/admin/survey/import.htm";
    /**
     * Edit the CSS layout for the survey form
     */
    public static final String SURVEY_EDIT_CSS_LAYOUT_URL = "/bdrs/admin/survey/editCssLayout.htm";
    /**
     * Edit the custom JS for the survey form
     */
    public static final String SURVEY_EDIT_JS_URL = "/bdrs/admin/survey/editJs.htm";

    /**
     * The POST dictionary key name of the survey import file.
     */
    public static final String POST_KEY_SURVEY_IMPORT_FILE = "survey_file";

    /**
     * The name of the JSON file containing the imported/exported survey data.
     */
    public static final String SURVEY_JSON_IMPORT_EXPORT_FILENAME = "survey.json";

    public static final String SURVEY_DOES_NOT_EXIST_ERROR_KEY = "bdrs.survey.doesNotExist";

    /**
     * The target survey logo dimension.
     */
    public static final Dimension TARGET_LOGO_DIMENSION = new Dimension(250, 187);
    /**
     * The target survey logo image format.
     */
    public static final String TARGET_LOGO_IMAGE_FORMAT = "PNG";
    /**
     * Query parameter name for the records default visibility.
     */
    public static final String PARAM_DEFAULT_RECORD_VISIBILITY = "defaultRecordVis";
    /**
     * Query parameter name for the value indicating if the record visibility is modifiable by standard users.
     */
    public static final String PARAM_RECORD_VISIBILITY_MODIFIABLE = "recordVisModifiable";

    /**
     * Request parameter, the form submit action for the survey.
     */
    public static final String PARAM_FORM_SUBMIT_ACTION = "formSubmitAction";

    /** The parameter that configures whether records may be commented on */
    public static final String PARAM_RECORD_COMMENTS_ENABLED = "recordCommentsEnabled";

    /**
     * The URL of the page displaying a list of all surveys.
     */
    public static final String SURVEY_LISTING_URL = "/bdrs/admin/survey/listing.htm";
    /**
     * The Query parameter name of the survey primary key passed into the survey exporter.
     */
    public static final String QUERY_PARAM_SURVEY_ID = "surveyId";
    /**
     * The query parameter to save and continue to the next form
     */
    public static final String PARAM_SAVE_AND_CONTINUE = "saveAndContinue";
    /**
     * The query parameter for the coordinate reference system
     */
    public static final String PARAM_CRS = "crs";
    
    /**
     * Query param for whether survey has public read access
     */
    public static final String PARAM_SURVEY_PUBLIC_READ_ACCESS = "public_read_access";
    /**
     * Query param for post - posting action (i.e. action to take after posting)
     */
    public static final String PARAM_SAVE_AND_CONTINUE_EDITING = "saveAndContinueEditing";
    /**
     * Query param for text to save when editing a CSS layout
     */
    public static final String PARAM_TEXT = "text_to_save";
    
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_CSS_FILE_MISSING = "bdrs.survey.cssLayout.missing";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_CSS_FILE_READ_ERROR = "bdrs.survey.cssLayout.fileReadError";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_CSS_FILE_WRITE_ERROR = "bdrs.survey.cssLayout.fileWriteError";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_CSS_FILE_WRITE_SUCCESS = "bdrs.survey.cssLayout.textFileSaved";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_CSS_NO_FILE = "bdrs.survey.cssLayout.noFileSaved";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_BASE_FORM_SAVE_CSS_ERROR = "bdrs.survey.save.errorCssLayoutSave";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_JS_WRITE_ERROR = "bdrs-survey.js.fileWriteError";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_JS_FILE_MISSING = "bdrs.survey.js.missing";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_JS_FILE_WRITE_SUCCESS = "bdrs.survey.js.textFileSaved";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_JS_FILE_READ_ERROR = "bdrs.survey.js.fileReadError";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_BASE_FORM_SAVE_JS_ERROR = "bdrs.survey.save.errorJsSave";
    /**
     * see bdrs-errors.properties
     */
    public static final String MSG_KEY_SURVEY_JS_NO_FILE = "bdrs.survey.js.noFileSaved";
    
    /**
     * Model and view key
     */
    public static final String MV_TEXT = "text";
    
    private static final String TEXT_ENCODING = "UTF-8";

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private MetadataDAO metadataDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private TaxaDAO taxaDAO;

    /**
     *  Performs database operations on Custom Form objects.
     */
    @Autowired
    private CustomFormDAO formDAO;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private GeoMapService geoMapService;
    
    /**
     * Provides access to hibernate sessions. This is used for survey import where the entire import executes
     * in a single transaction.
     */
    @Autowired
    private SessionFactory sessionFactory;

    /**
     * The import/export service that takes JSON data and creates BDRS objects.
     */
    @Autowired
    private SurveyImportExportService surveyImportExportService;

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_LISTING_URL, method = RequestMethod.GET)
    public ModelAndView listSurveys(HttpServletRequest request, HttpServletResponse response) {

        ModelAndView mv = new ModelAndView("surveyListing");
        mv.addObject("surveyList", surveyDAO.getSurveys(getRequestContext().getUser()));
        return mv;
    }

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_URL_INITIAL_PAGE, method = RequestMethod.GET)
    public ModelAndView editSurvey(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, required = false) Integer surveyId,
            @RequestParam(value = "publish", required = false) String publish) {

        Survey survey;
        if (surveyId == null) {
            survey = new Survey();
        } else {
            survey = surveyDAO.getSurvey(surveyId);
            if (survey == null) {
                return nullSurveyRedirect(getRequestContext());
            }
        }

        boolean toPublish = publish != null;
        if (toPublish) {
            getRequestContext().addMessage("bdrs.survey.publish");
        }
        
        ModelAndView mv = new ModelAndView("surveyEdit");
        mv.addObject("survey", survey);
        mv.addObject("publish", toPublish);
        mv.addObject("customforms", formDAO.getCustomForms());
        return mv;
    }

    /**
     * Handler for posting the survey setup form
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param surveyId Survey ID
     * @param name Name of the survey
     * @param description Description of the survey
     * @param active Is the survey active?
     * @param rendererType either the SurveyFormRendererType or the primary key to a CustomForm.
     * @param surveyDate Start date for the survey
     * @param surveyEndDate End date for the survey
     * @param defaultRecordVis Default record visibility for the survey
     * @param recordVisMod Is the record visibility modifiable by users 
     * @param formSubmitAction The form submit action for the survey
     * @return ModelAndView
     * @throws IOException
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_URL_INITIAL_PAGE, method = RequestMethod.POST)
    public ModelAndView submitSurveyEdit(
            MultipartHttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, required = false) Integer surveyId,
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "description", required = true) String description,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            @RequestParam(value = "rendererType", defaultValue="DEFAULT") String rendererType,
            @RequestParam(value = "surveyDate", required = true) Date surveyDate,
            @RequestParam(value = "surveyEndDate", required = false) String surveyEndDate,
            @RequestParam(value = PARAM_DEFAULT_RECORD_VISIBILITY, required = true) String defaultRecordVis,
            @RequestParam(value = PARAM_RECORD_VISIBILITY_MODIFIABLE, defaultValue="false") boolean recordVisMod,
            @RequestParam(value = PARAM_RECORD_COMMENTS_ENABLED, defaultValue="false") boolean recordCommentsEnabled,
            @RequestParam(value = PARAM_FORM_SUBMIT_ACTION, required = true) String formSubmitAction,
            @RequestParam(value = PARAM_CRS, required = true) String crs) 
        throws IOException {

        Survey survey;
        boolean create;
        if (surveyId == null) {
            survey = new Survey();
            create = true;
        } else {
            survey = surveyDAO.getSurvey(surveyId);
            if (survey == null) {
                return nullSurveyRedirect(getRequestContext());
            }
            create = false;
        }

        boolean origPublishStatus = survey.isActive();

        survey.setName(name);
        survey.setDescription(description);
        survey.setStartDate(surveyDate);
        survey.setEndDate(surveyEndDate);
        survey.setActive(active);
        
        survey.setDefaultRecordVisibility(RecordVisibility.parse(defaultRecordVis), metadataDAO);
        survey.setRecordVisibilityModifiable(recordVisMod, metadataDAO);
        survey.setFormSubmitAction(SurveyFormSubmitAction.valueOf(formSubmitAction), metadataDAO);
        survey.setRecordCommentsEnabled(recordCommentsEnabled, metadataDAO);
        
        // A list of metadata to delete. To maintain referential integrity,
        // the link between survey and metadata must be broken before the 
        // metadata can be deleted.
        List<Metadata> metadataToDelete = new ArrayList<Metadata>();
        
        // get the css file if it exists...
        readCssFile(request, survey, metadataToDelete);
        
        // get the js file if it exists...
        readJsFile(request, survey, metadataToDelete);
        
        // ---- Form Renderer Type
        // Initially assume the renderer type is a CustomForm primary key.
        CustomForm form = null;
        try {
            int formPk = Integer.parseInt(rendererType); 
            form = formDAO.getCustomForm(formPk);
        } catch(NumberFormatException nfe) {
        }

        survey.setCustomForm(form);

        // If we have failed to link to a custom form,
        if(form == null) {
            Metadata md;
            SurveyFormRendererType formRenderType = SurveyFormRendererType.valueOf(rendererType);
            if(formRenderType.isEligible(survey)) {
                md = survey.setFormRendererType(formRenderType);
            } else {
                md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            }
            metadataDAO.save(md);
        } else {
            // Remove any existing form renderer types
            Metadata md = survey.getMetadataByKey(Metadata.FORM_RENDERER_TYPE);
            if(md != null) {
                survey.getMetadata().remove(md);
            }
        }
        // ----------------------
        
        // Survey Logo
        readLogoFile(request, survey, metadataToDelete);
        
        surveyDAO.save(survey);
        
        geoMapService.getForSurvey(survey).setCrs(BdrsCoordReferenceSystem.valueOf(crs));

        // Work around for the Survey DAO. The DAO automatically sets the
        // survey to active when creating the survey. This forces the survey
        // to update with the correct active state.
        if (create) {
            survey.setActive(active);
        }
        
        for(Metadata delMd : metadataToDelete) {
            metadataDAO.delete(delMd);
        }

        String messageKey;
        
        if (!origPublishStatus && survey.isActive()) {
            messageKey = "bdrs.survey.publish.success";
        } else {
            messageKey = "bdrs.survey.save.success";
        }
        getRequestContext().addMessage(messageKey,
                new Object[] { survey.getName() });

        ModelAndView mv;
        if (request.getParameter(PARAM_SAVE_AND_CONTINUE) != null) {
            mv = new ModelAndView(new PortalRedirectView(
                    "/bdrs/admin/survey/editTaxonomy.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else {
            mv = new ModelAndView(new PortalRedirectView(
                    SURVEY_LISTING_URL, true));
        }
        return mv;
    }

    // -------------------------------
    //  Users
    // -------------------------------

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editUsers.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyUsers(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        ModelAndView mv = new ModelAndView("surveyEditUsers");
        mv.addObject("listType", survey.isPublic() ? UserSelectionType.ALL_USERS : UserSelectionType.SELECTED_USERS);
        mv.addObject("survey", survey);
        return mv;
    }

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editUsers.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyUsers(HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(value=PARAM_SURVEY_PUBLIC_READ_ACCESS, defaultValue="false") boolean publicReadAccess) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        survey.setPublicReadAccess(publicReadAccess);

        UserSelectionType selectionType =
            UserSelectionType.valueOf(request.getParameter("userSelectionType"));

        if(UserSelectionType.SELECTED_USERS.equals(selectionType)) {
            survey.setPublic(false);
            // Add the users
            Set<User> users = new HashSet<User>();
            if(request.getParameterValues("users") != null) {
                List<Integer> pks = new ArrayList<Integer>();
                for(String rawPk : request.getParameterValues("users")) {
                    pks.add(Integer.parseInt(rawPk));
                }
                users.addAll(userDAO.get(pks.toArray(new Integer[]{})));
            }
            survey.setUsers(users);

            // Add the groups
            Set<Group> groups = new HashSet<Group>();
            if(request.getParameterValues("groups") != null) {
                List<Integer> pks = new ArrayList<Integer>();
                for(String rawPk : request.getParameterValues("groups")) {
                    pks.add(Integer.parseInt(rawPk));
                }
                groups.addAll(groupDAO.get(pks.toArray(new Integer[]{})));
            }
            survey.setGroups(groups);

        } else if(UserSelectionType.ALL_USERS.equals(selectionType)) {
            survey.setPublic(true);
        } else {
            log.error("Unknown User Selection Type: "+selectionType.toString());
            throw new HTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // Update the form rendering type given the new criteria
        SurveyFormRendererType formRenderType = survey.getFormRendererType();
        if(formRenderType == null || (formRenderType != null && !formRenderType.isEligible(survey))) {
            Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            metadataDAO.save(md);
        }
        surveyDAO.save(survey);

        getRequestContext().addMessage("bdrs.survey.users.success", new Object[]{survey.getName()});

        ModelAndView mv;
        if(request.getParameter(PARAM_SAVE_AND_CONTINUE) != null) {
            mv = new ModelAndView(new PortalRedirectView(SURVEY_EDIT_URL_INITIAL_PAGE, true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
            mv.addObject("publish", "publish");
        }
        else {
            mv = new ModelAndView(new PortalRedirectView(SURVEY_LISTING_URL, true));
        }
        return mv;
    }

    // --------------------------------------
    //  Taxonomy
    // --------------------------------------

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editTaxonomy.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyTaxonomy(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        Set<IndicatorSpecies> speciesSet = survey.getSpecies();
        SpeciesListType listType = null;
        Set<TaxonGroup> taxonGroupSet = new HashSet<TaxonGroup>();
        TaxonGroup taxonGroup = null;

        if(speciesSet.isEmpty()) {
            listType = SpeciesListType.ALL_SPECIES;
        } else if(speciesSet.size() == 1) {
            listType = SpeciesListType.ONE_SPECIES;
            taxonGroupSet.add(speciesSet.iterator().next().getTaxonGroup());
        } else {
            //TODO fixme, coz i don't work properly
            // Need to work out if all the species are in the same group.
            listType = SpeciesListType.SPECIES_GROUP;
            for(IndicatorSpecies species: speciesSet) {
                taxonGroupSet.add(species.getTaxonGroup());
                if(listType == null) {
                    if(taxonGroup == null) {
                        taxonGroup = species.getTaxonGroup();
                    }
                    if(!taxonGroup.equals(species.getTaxonGroup())) {
                        listType = SpeciesListType.MANY_SPECIES;
                        taxonGroup = null;
                    }
                }
            }
        }

        ModelAndView mv = new ModelAndView("surveyEditTaxonomy");
        mv.addObject("survey", survey);
        mv.addObject("taxonGroupSet", taxonGroupSet);
        mv.addObject("listType", listType);

        return mv;
    }

    
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editTaxonomy.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyTaxonomy(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();

        SpeciesListType listType = SpeciesListType.valueOf(request.getParameter("speciesListType"));
        switch(listType) {
            case ONE_SPECIES:
            case MANY_SPECIES:
                if(request.getParameterValues("species") != null) {
                    Integer[] pks = new Integer[request.getParameterValues("species").length];
                    int i=0;
                    for(String rawPk : request.getParameterValues("species")) {
                        pks[i] = Integer.parseInt(rawPk);
                        i+=1;
                    }
                    speciesSet.addAll(taxaDAO.getIndicatorSpeciesById(pks));
                }
                break;
            case SPECIES_GROUP:
                if(request.getParameterValues("speciesGroup") != null) {
                    Integer[] pks = new Integer[request.getParameterValues("speciesGroup").length];
                    int i=0;
                    for(String rawPk : request.getParameterValues("speciesGroup")) {
                        pks[i] = Integer.parseInt(rawPk);
                        i+=1;
                    }
                    
                    //TODO unhardwire this constant.
                    int count = taxaDAO.countIndicatorSpecies(pks);
                    log.debug("Counted " + count + " species in groups");
                    if (count < 10000) {
                    	speciesSet.addAll(taxaDAO.getIndicatorSpecies(pks));
                    }
                    else
                    {
                    	getRequestContext().addMessage("bdrs.survey.taxonomy.tooManyTaxa", new Object[]{survey.getName()});
                    }
                }
                break;
            case ALL_SPECIES:
                break;
            default:
                log.error("Unknown Species List Type: "+listType);
                break;
        }
        survey.setSpecies(speciesSet);

        // Update the form rendering type given the new criteria
        SurveyFormRendererType formRenderType = survey.getFormRendererType();
        if(formRenderType == null || (formRenderType != null && !formRenderType.isEligible(survey))) {
            Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            metadataDAO.save(md);
        }
        surveyDAO.save(survey);

        getRequestContext().addMessage("bdrs.survey.taxonomy.success", new Object[]{survey.getName()});

        ModelAndView mv;
        if(request.getParameter(PARAM_SAVE_AND_CONTINUE) != null) {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/editAttributes.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        }
        else {
            mv = new ModelAndView(new PortalRedirectView(SURVEY_LISTING_URL, true));
        }
        return mv;
    }

    // --------------------------------------
    //  Survey Export
    // --------------------------------------

    /**
     * JSON encodes and compresses the survey, censusmethod, metadata and associated attributes and attribute options.
     *
     * @param request  the client request
     * @param response the server response
     * @param surveyId the primary key of the survey to be exported.
     * @return compressed JSON representation of the survey, census method, metadata and attributes.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EXPORT_URL, method = RequestMethod.GET)
    public void exportSurvey(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(required = true, value = QUERY_PARAM_SURVEY_ID) int surveyId) throws IOException {
        // turn off the partial record filter to allow census method attributes to be exported
        FilterManager.disablePartialRecordCountFilter(sessionFactory.getCurrentSession());
        try {
            Survey survey = surveyDAO.get(surveyId);
            JSONObject jsonSurvey = surveyImportExportService.exportObject(survey);

            response.setContentType(ZipUtils.ZIP_CONTENT_TYPE);
            response.setHeader("Content-Disposition", "attachment;filename=survey_export_"
                    + survey.getId() + ".zip");

            ZipEntry entry = new ZipEntry(SURVEY_JSON_IMPORT_EXPORT_FILENAME);
            ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
            out.putNextEntry(entry);
            out.write(jsonSurvey.toJSONString().getBytes(Charset.defaultCharset()));

            out.flush();
            out.close();
        } finally {
            FilterManager.setPartialRecordCountFilter(sessionFactory.getCurrentSession());
        }
    }

    /**
     * Creates a new survey based upon an uploaded file.
     *
     * @param request  the client request.
     * @param response the server response.
     * @return The survey listing page with a message indicating success or failure.
     */
    @RolesAllowed({Role.ADMIN})
    @RequestMapping(value = SURVEY_IMPORT_URL, method = RequestMethod.POST)
    public ModelAndView importSurvey(MultipartHttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        try {
            JSONObject importData = null;
            {
                MultipartFile file = request.getFile(POST_KEY_SURVEY_IMPORT_FILE);
                ZipInputStream zis = new ZipInputStream(file.getInputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                    if (SURVEY_JSON_IMPORT_EXPORT_FILENAME.equals(entry.getName())) {
                        int read = zis.read(buffer, 0, buffer.length);
                        while (read > -1) {
                            baos.write(buffer, 0, read);
                            read = zis.read(buffer, 0, buffer.length);
                        }
                    }
                }
                zis.close();
                importData = JSONObject.fromStringToJSONObject(new String(baos.toByteArray(), Charset.defaultCharset()));
            }

            if (importData != null) {
                Session sesh = null;
                Transaction tx = null;
                try {
                    sesh = sessionFactory.openSession();

                    tx = sesh.beginTransaction();
                    surveyImportExportService.importObject(sesh, importData);
                    tx.commit();
                    getRequestContext().addMessage("bdrs.survey.import.success");
                } catch (Throwable t) {
                    if (tx != null) {
                        tx.rollback();
                    }
                    log.error(t.getMessage(), t);
                    getRequestContext().addMessage("bdrs.survey.import.error.unknown");
                } finally {
                    if (sesh != null) {
                        sesh.close();
                    }
                }
            } else {
                getRequestContext().addMessage("bdrs.survey.import.error.missingFile");
            }
        } catch (JSONException je) {
            log.error(je.getMessage(), je);
            getRequestContext().addMessage("bdrs.survey.import.error.json");
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            getRequestContext().addMessage("bdrs.survey.import.error.io");
        }

        return new ModelAndView(new PortalRedirectView(SurveyBaseController.SURVEY_LISTING_URL, true));
    }

    /**
     * Get the survey represented by the string.
     * 
     * @param rawSurveyId Survey ID as a string.
     * @return Survey matching criteria.
     */
    private Survey getSurvey(String rawSurveyId) {
        Survey survey = null;
        try {
            if(rawSurveyId != null) {
                survey = surveyDAO.getSurvey(Integer.parseInt(rawSurveyId));
            }
        } catch(NumberFormatException nfe) {
            log.warn(String.format("Raw survey ID %s is not a valid integer", rawSurveyId), nfe);

            // Leave survey as null.
        }
        return survey;
    }
    
    /**
     * Helper for handling null surveys
     * @param requestContext RequestContext for request.
     * @return ModelAndView, sends the user back to the survey listing.
     */
    public static ModelAndView nullSurveyRedirect(RequestContext requestContext) {
        requestContext.addMessage(SurveyBaseController.SURVEY_DOES_NOT_EXIST_ERROR_KEY);
        return new ModelAndView(new PortalRedirectView(SurveyBaseController.SURVEY_LISTING_URL, true));
    }
    
    /**
     * View for editing survey map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @return ModelAndView.
     */
    @SuppressWarnings("unchecked")
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editMap.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyMap(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        GeoMap geoMap = geoMapService.getForSurvey(survey);
        if (geoMap == null) {
        	throw new IllegalStateException("Map should be created at survey creation.");
        }
        ModelAndView mv = this.editMap(request, geoMap);
        mv.addObject("survey", survey);
        return mv;
    }
    
    /**
     * View for saving survey map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @return ModelAndView.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editMap.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyMap(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        GeoMap geoMap = geoMapService.getForSurvey(survey);
        this.submitMap(request, geoMap);
        ModelAndView mv;
        if(request.getParameter(PARAM_SAVE_AND_CONTINUE) != null) {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/locationListing.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else {
            mv = new ModelAndView(new PortalRedirectView(SURVEY_LISTING_URL, true));
        }
        return mv;
    }
    
    /**
     * Edit the CSS layout for the survey.
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @return ModelAndView.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_CSS_LAYOUT_URL, method = RequestMethod.GET)
    public ModelAndView editCssFile(HttpServletRequest request,
            HttpServletResponse response) {

        return editTextFile(request, response, "editSurveyCssLayout", Metadata.SURVEY_CSS, 
                            MSG_KEY_SURVEY_CSS_FILE_MISSING, 
                            MSG_KEY_SURVEY_CSS_FILE_READ_ERROR,
                            MSG_KEY_SURVEY_CSS_NO_FILE);
    }
    
    /**
     * Edit the custom javascript for the survey.
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @return ModelAndView.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_JS_URL, method = RequestMethod.GET)
    public ModelAndView editSurveyJs(HttpServletRequest request,
            HttpServletResponse response) {

        return editTextFile(request, response, "editSurveyJs", Metadata.SURVEY_JS, 
                            MSG_KEY_SURVEY_JS_FILE_MISSING, 
                            MSG_KEY_SURVEY_JS_FILE_READ_ERROR,
                            MSG_KEY_SURVEY_JS_NO_FILE);
    }
    
    /**
     * Edit a text file attached to a Metadata object.
     * 
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @param viewName Name of view.
     * @param metaKey Metadata key.
     * @param msgKeyFileMissing Msg key for missing file.
     * @param msgKeyReadError Msg key for read error.
     * @return ModelAndView
     */
    private ModelAndView editTextFile(HttpServletRequest request,
            HttpServletResponse response, String viewName, String metaKey, String msgKeyFileMissing, String msgKeyReadError, String msgKeyNoFile) {
        
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        OpenFileResult ofr = openMetadataFile(survey, metaKey, msgKeyFileMissing, msgKeyNoFile);
        if (!ofr.isValid()) {
            return ofr.modelAndView;
        }

        // see secure-admin.xml
        ModelAndView mv = new ModelAndView(viewName);
        mv.addObject("survey", survey);
        mv.addObject("filename", ofr.file.getName());

        // get the contents of the text file.
        String text = null;
        try {
            text = FileUtils.readFile(ofr.file.getAbsolutePath());
        } catch (FileNotFoundException fnfe) {
            getRequestContext().addMessage(msgKeyFileMissing, new Object[] { ofr.file.getName() });
            return redirectToEdit(survey);
        } catch (IOException ioe) {
            getRequestContext().addMessage(msgKeyReadError);
            log.error("Could not read bytes from file. uuid = " + ofr.file.getAbsolutePath(), ioe);
            return redirectToEdit(survey);
        }
        mv.addObject(MV_TEXT, text);
        return mv;
    }
    
    /**
     * Save the CSS file that the user has been editing.
     * 
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @param text Text to save.
     * @return ModelAndView
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_CSS_LAYOUT_URL, method = RequestMethod.POST)
    public ModelAndView saveCssFile(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_TEXT) String text) {
        
        return saveTextFile(request, response, text, Metadata.SURVEY_CSS, MSG_KEY_SURVEY_CSS_FILE_WRITE_ERROR,
                            MSG_KEY_SURVEY_CSS_FILE_MISSING, MSG_KEY_SURVEY_CSS_FILE_WRITE_SUCCESS, 
                            MSG_KEY_SURVEY_CSS_NO_FILE, SURVEY_EDIT_CSS_LAYOUT_URL);
    }
    
    /**
     * Save the JS file that the user has been editing.
     * 
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @param text Text to save.
     * @return ModelAndView.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = SURVEY_EDIT_JS_URL, method = RequestMethod.POST)
    public ModelAndView saveJsFile(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_TEXT) String text) {
        
        return saveTextFile(request, response, text, Metadata.SURVEY_JS, MSG_KEY_SURVEY_JS_WRITE_ERROR,
                            MSG_KEY_SURVEY_JS_FILE_MISSING, MSG_KEY_SURVEY_JS_FILE_WRITE_SUCCESS, 
                            MSG_KEY_SURVEY_JS_NO_FILE, SURVEY_EDIT_JS_URL);
    }
    
    /**
     * Saves the text file that the use has been editing.
     * 
     * @param request HttpServletRequest.
     * @param response HttpServletResponse.
     * @param text Text to save.
     * @param metadataKey Metadata key.
     * @param msgKeyFileWriteError Message key for file write error.
     * @param msgKeyFileMissing Message key for file missing error.
     * @param msgKeySuccess Message key for successful save.
     * @param editFileUrl URL to redirect to to continue editing the text after saving.
     * @return ModelAndView.
     */
    private ModelAndView saveTextFile(HttpServletRequest request, HttpServletResponse response,
            String text, String metadataKey, String msgKeyFileWriteError, 
            String msgKeyFileMissing, String msgKeySuccess, String msgKeyNoFileSaved, String editFileUrl) {
        
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        OpenFileResult ofr = openMetadataFile(survey, metadataKey, msgKeyFileMissing, msgKeyNoFileSaved);
        if (!ofr.isValid()) {
            return ofr.modelAndView;
        }
        if (ofr.file.exists()) {
            if (!ofr.file.delete()) {
                log.error("failed to delete file - potential file locking issue : " + ofr.file.getAbsolutePath());
                getRequestContext().addMessage(msgKeyFileWriteError);
                return redirectToEdit(survey);
            }
        }
        try {
            if (!ofr.file.createNewFile()) {
                // really this shouldn't happen as we have already checked for an deleted the file
                log.error("could not create new file - " + ofr.file.getAbsolutePath());
                getRequestContext().addMessage(msgKeyFileWriteError);
                return redirectToEdit(survey);
            }
        } catch (IOException e1) {
            log.error("could not create new file, IOException - " + ofr.file.getAbsolutePath(), e1);
            getRequestContext().addMessage(msgKeyFileWriteError);
            return redirectToEdit(survey);
        }
        
        FileOutputStream stream = null; 
        OutputStreamWriter writer = null; 
        
        try {
            stream = new FileOutputStream(ofr.file);
            writer = new OutputStreamWriter(stream, TEXT_ENCODING);
            
            writer.write(text);
            writer.flush();
            
        } catch (FileNotFoundException fnfe) {
            getRequestContext().addMessage(msgKeyFileMissing, new Object[] { ofr.file.getName() });
            return redirectToEdit(survey);
        } catch (UnsupportedEncodingException uee) {
            log.error("Unsupported encoding", uee);
            getRequestContext().addMessage(msgKeyFileWriteError);
            return redirectToEdit(survey);
        } catch (IOException ioe) {
            log.error("Error writing managed file, uuid " + ofr.file.getAbsolutePath(), ioe);
            getRequestContext().addMessage(msgKeyFileWriteError);
            return redirectToEdit(survey);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("failed to close writer", e);
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("failed to close stream", e);
                    getRequestContext().addMessage(msgKeyFileWriteError);
                    return redirectToEdit(survey);
                }
            }
        }

        getRequestContext().addMessage(msgKeySuccess);
        
        ModelAndView mv;
        if (request.getParameter(PARAM_SAVE_AND_CONTINUE_EDITING) != null) {
            // back to CSS layout editing screen
            mv = new ModelAndView(new PortalRedirectView(editFileUrl, true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else if (request.getParameter(PARAM_SAVE_AND_CONTINUE) != null) {
            // back to first survey editing page
            mv = new ModelAndView(new PortalRedirectView(SURVEY_EDIT_URL_INITIAL_PAGE, true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else {
            // back to URL listing
            mv = new ModelAndView(new PortalRedirectView(
                    SURVEY_LISTING_URL, true));
        }
        return mv;
    }
    
    /**
     * Helper for opening a file attached to survey metadata.
     * @param survey Survey to use.
     * @param metaKey Metadata key to look for.
     * @return OpenFileResult
     */
    private OpenFileResult openMetadataFile(Survey survey, String metaKey, String msgKeyFileMissing, String msgKeyNoFileSaved) {
        Metadata md = survey.getMetadataByKey(metaKey);
        OpenFileResult result = new OpenFileResult();
        if (md == null) {
            getRequestContext().addMessage(msgKeyNoFileSaved);
            result.modelAndView = redirectToEdit(survey);
            return result;
        }
        FileDataSource fds = fileService.getFile(md, md.getValue());
        
        if (fds == null) {
            getRequestContext().addMessage(msgKeyFileMissing, new Object[] { md.getValue() });
            result.modelAndView = redirectToEdit(survey);
            return result;
        }
        
        File file = fds.getFile();
        result.file = file;
        
        return result;
    }
    
    /**
     * Gets the required parameter name for a file in the file param map.
     * 
     * @param metaKey Metadata key that the file will belong to.
     * @return Parameter name for file.
     */
    public static final String getFileParamName(String metaKey) {
        if (!StringUtils.hasLength(metaKey)) {
            throw new IllegalArgumentException("String must be non empty");
        }
        return String.format("%s_file", metaKey);
    }
    
    /**
     * Has the logic for working out whether a file has been updated or not.
     * 
     * @param request Http request.
     * @param survey Survey to update.
     * @param metaKey Metadata key to update.
     * @param metadataToDelete If the metadata file has been removed, add the metadata object to this list.
     * @return MultipartFile to be saved or null if no action needs to be taken.
     */
    private MultipartFile readMetaFile(MultipartHttpServletRequest request, Survey survey, String metaKey, List<Metadata> metadataToDelete) {
        // Survey Logo
        String fileStr = request.getParameter(metaKey);
        MultipartFile file = request.getFile(getFileParamName(metaKey));
        // file will always have size zero unless the file
        // is changed. If there is already a file, but the
        // project is updated, without changing the file input,
        // fileStr will not be empty but file will
        // have size zero.
        Metadata metadata = survey.getMetadataByKey(metaKey);
        log.debug("1 - " + metaKey + " - survey id : " + survey.getId() + " fileStr : " + fileStr);
        if (fileStr != null) {
            log.debug("2");
            log.debug("metadata is not null : " + Boolean.toString(metadata != null));
            log.debug("filestr is empty : " + Boolean.toString(fileStr.isEmpty()));
            if(fileStr.isEmpty() && metadata != null) {
                // The file was intentionally cleared.
                Set<Metadata> surveyMetadata = new HashSet<Metadata>(survey.getMetadata());
                surveyMetadata.remove(metadata);
                survey.setMetadata(surveyMetadata);
                
                metadataToDelete.add(metadata);
                log.debug("deleting meta data");
            } else if(!fileStr.isEmpty() && file != null && file.getSize() > 0) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * On the base survey editing form, saves the uploaded javascript file.
     * 
     * @param request HttpServletRequest.
     * @param survey Survey being edited.
     * @param metadataToDelete List to add metadata objects to.
     */
    private void readJsFile(MultipartHttpServletRequest request, Survey survey, List<Metadata> metadataToDelete) {
        baseFormSaveTextFile(request, survey, metadataToDelete, Metadata.SURVEY_JS, MSG_KEY_BASE_FORM_SAVE_JS_ERROR);
    }
    
    /**
     * On the base survey editing form, saves the uploaded CSS file.
     * @param request HttpServletRequest.
     * @param survey Survey being edited.
     * @param metadataToDelete List to add metadata objects to.
     */
    private void readCssFile(MultipartHttpServletRequest request, Survey survey, List<Metadata> metadataToDelete) {
        baseFormSaveTextFile(request, survey, metadataToDelete, Metadata.SURVEY_CSS, MSG_KEY_BASE_FORM_SAVE_CSS_ERROR);
    }
    
    /**
     * On the base survey editing form. saves the uploaded text file.
     * 
     * @param request HttpServletRequest.
     * @param survey Survey being edited.
     * @param metadataToDelete List to add metadata objects to.
     * @param metaKey Metadata key of metadata to save file.
     * @param msgKeyError Message key for saving error.
     */
    private void baseFormSaveTextFile(MultipartHttpServletRequest request, Survey survey, List<Metadata> metadataToDelete,
            String metaKey, String msgKeyError) {
        MultipartFile file = readMetaFile(request, survey, metaKey, metadataToDelete);
        if (file != null) {
            String targetFilename = file.getOriginalFilename();
            Metadata fileMetadata = survey.getMetadataByKey(metaKey);
            if(fileMetadata == null) {
                fileMetadata = new Metadata();
                fileMetadata.setKey(metaKey);
            }
            fileMetadata.setValue(targetFilename);
            metadataDAO.save(fileMetadata);
            survey.getMetadata().add(fileMetadata);
            
            try {
                fileService.createFile(fileMetadata, file);
            } catch (IOException ioe) {
                getRequestContext().addMessage(msgKeyError);
                log.error("Error saving base form text file - " + msgKeyError, ioe);
            }
        }
    }
    
    /**
     * On the base survey form, save the survey logo.
     * 
     * @param request HttpServletRequest.
     * @param survey Survey being edited.
     * @param metadataToDelete List to add metadata objects to.
     */
    private void readLogoFile(MultipartHttpServletRequest request, Survey survey, List<Metadata> metadataToDelete) {
        String metaKey = Metadata.SURVEY_LOGO;
        MultipartFile logoFile = readMetaFile(request, survey, metaKey, metadataToDelete);
        if (logoFile != null) {
            if (ImageUtil.isMimetypeSupported(logoFile.getContentType())) {
                String targetBasename = FilenameUtils.getBaseName(logoFile.getOriginalFilename());
                
                String targetFilename = String.format("%s%s%s", targetBasename, FilenameUtils.EXTENSION_SEPARATOR, TARGET_LOGO_IMAGE_FORMAT);
                
                Metadata logo = survey.getMetadataByKey(metaKey);
                if(logo == null) {
                    logo = new Metadata();
                    logo.setKey(metaKey);
                }
                logo.setValue(targetFilename);
                metadataDAO.save(logo);
                survey.getMetadata().add(logo);
                
                try {
                    BufferedImage scaledImage = ImageUtil.resizeImage(logoFile.getInputStream(), TARGET_LOGO_DIMENSION.width, TARGET_LOGO_DIMENSION.height);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(scaledImage, TARGET_LOGO_IMAGE_FORMAT, baos);
                    baos.flush();
                    fileService.createFile(logo.getClass(), logo.getId(), targetFilename, baos.toByteArray());                    
                } catch (IOException ioe) {
                    getRequestContext().addMessage("bdrs.survey.save.errorLogoSave");
                    log.error("Error saving logo", ioe);
                }
            } else {
                getRequestContext().addMessage("bdrs.survey.save.errorLogoNotAnImage");
                log.warn("Unable to resize logo image with content type "+logoFile.getContentType());
            }
        }
    }
    
    /**
     * Redirects to the first survey editing page in the edit survey wizard.
     * @param survey Survey to edit.
     * @return redirect view.
     */
    private ModelAndView redirectToEdit(Survey survey) {
        ModelAndView mv = this.redirect(SURVEY_EDIT_URL_INITIAL_PAGE);
        mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        return mv;
    }

    /**
     * Holder object for open file operation.
     */
    private static class OpenFileResult {
        // will be null if successful.
        public ModelAndView modelAndView = null;
        // will be non null if successful
        public File file = null;
        
        public boolean isValid() {
            return modelAndView == null && file != null;
        }
    }
}
