package au.com.gaiaresources.bdrs.controller.survey;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.map.*;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.*;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.survey.SurveyImportExportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.util.ImageUtil;
import au.com.gaiaresources.bdrs.util.StringUtils;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Controller
public class SurveyBaseController extends AbstractController {
    /**
     * The URL of the handler to retrieve an exported survey.
     */
    public static final String SURVEY_EXPORT_URL = "/bdrs/admin/survey/export.htm";

    /**
     * The URL of the handler to parse an imported survey.
     */
    public static final String SURVEY_IMPORT_URL = "/bdrs/admin/survey/import.htm";

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
    private GeoMapLayerDAO geoMapLayerDAO;
    @Autowired
    private BaseMapLayerDAO baseMapLayerDAO;
    
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
    @RequestMapping(value = "/bdrs/admin/survey/edit.htm", method = RequestMethod.GET)
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
    @RequestMapping(value = "/bdrs/admin/survey/edit.htm", method = RequestMethod.POST)
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
            @RequestParam(value = PARAM_FORM_SUBMIT_ACTION, required = true) String formSubmitAction) 
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
        String logoFileStr = request.getParameter(Metadata.SURVEY_LOGO);
        MultipartFile logoFile = request.getFile(Metadata.SURVEY_LOGO+"_file");
        
        // logoFile will always have size zero unless the file
        // is changed. If there is already a file, but the
        // record is updated, without changing the file input,
        // logoFileStr will not be empty but logoFile will
        // have size zero.
        Metadata logo = survey.getMetadataByKey(Metadata.SURVEY_LOGO);
        if(logoFileStr.isEmpty() && logo != null) {
            // The file was intentionally cleared.
            Set<Metadata> surveyMetadata = new HashSet<Metadata>(survey.getMetadata());
            surveyMetadata.remove(logo);
            survey.setMetadata(surveyMetadata);
            
            metadataToDelete.add(logo);
        }
        else if(!logoFileStr.isEmpty() && logoFile != null && logoFile.getSize() > 0) {
            if (ImageUtil.isMimetypeSupported(logoFile.getContentType())) {
                String targetBasename = FilenameUtils.getBaseName(logoFile.getOriginalFilename());
                
                String targetFilename = String.format("%s%s%s", targetBasename, FilenameUtils.EXTENSION_SEPARATOR, TARGET_LOGO_IMAGE_FORMAT);
                
                if(logo == null) {
                    logo = new Metadata();
                    logo.setKey(Metadata.SURVEY_LOGO);
                }
                logo.setValue(targetFilename);
                metadataDAO.save(logo);
                survey.getMetadata().add(logo);
                
                BufferedImage scaledImage = ImageUtil.resizeImage(logoFile.getInputStream(), TARGET_LOGO_DIMENSION.width, TARGET_LOGO_DIMENSION.height);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaledImage, TARGET_LOGO_IMAGE_FORMAT, baos);
                baos.flush();
                fileService.createFile(logo.getClass(), logo.getId(), targetFilename, baos.toByteArray());
            }
            else {
                log.warn("Unable to resize logo image with content type "+logoFile.getContentType());
            }
        } 
        
        surveyDAO.save(survey);

        // Work around for the Survey DAO. The DAO automatically sets the
        // survey to active when creating the survey. This forces the survey
        // to update with the correct active state.
        if (create) {
            survey.setActive(active);
            createDefaultBaseLayers(survey);
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
        if (request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new RedirectView(
                    "/bdrs/admin/survey/editTaxonomy.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else {
            mv = new ModelAndView(new RedirectView(
                    SURVEY_LISTING_URL, true));
        }
        return mv;
    }

    /**
     * Creates an initial set of {@link BaseMapLayer} for a {@link Survey}.
     * @param survey The survey to set the layers on
     */
    private void createDefaultBaseLayers(Survey survey) {
        // create the default base layers
        for (BaseMapLayerSource baseMapLayerSource : BaseMapLayerSource.values()) {
            boolean isGoogleDefault = BaseMapLayerSource.G_HYBRID_MAP.equals(baseMapLayerSource);
            // set the layer to visible if no values have been saved and it is a Google layer
            // or if there is no default and it is G_HYBRID_MAP (for the default)
            BaseMapLayer layer = new BaseMapLayer(survey, baseMapLayerSource, isGoogleDefault, BaseMapLayerSource.isGoogleLayerSource(baseMapLayerSource) || isGoogleDefault);
            layer.setSurvey(survey);
            layer = baseMapLayerDAO.save(layer);
            survey.addBaseMapLayer(layer);
        }
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
    public ModelAndView submitSurveyUsers(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }

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
        if(request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new RedirectView("/bdrs/admin/survey/edit.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
            mv.addObject("publish", "publish");
        }
        else {
            mv = new ModelAndView(new RedirectView(SURVEY_LISTING_URL, true));
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
        if(request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new RedirectView("/bdrs/admin/survey/editAttributes.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        }
        else {
            mv = new ModelAndView(new RedirectView(SURVEY_LISTING_URL, true));
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

        return new ModelAndView(new RedirectView(SurveyBaseController.SURVEY_LISTING_URL, true));
    }

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
    
    public static ModelAndView nullSurveyRedirect(RequestContext requestContext) {
        requestContext.addMessage(SurveyBaseController.SURVEY_DOES_NOT_EXIST_ERROR_KEY);
        return new ModelAndView(new RedirectView(SurveyBaseController.SURVEY_LISTING_URL, true));
    }
    
    /**
     * View for editing survey map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unchecked")
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editMap.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyMap(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        ModelAndView mv = new ModelAndView("surveyEditMap");
        mv.addObject("survey", survey);
        
        // get the base layers that have been set for the survey
        List<BaseMapLayerSource> enumKeys = new ArrayList<BaseMapLayerSource>(BaseMapLayerSource.values().length);
        enumKeys.addAll((List<BaseMapLayerSource>) Arrays.asList(BaseMapLayerSource.values()));
        List<BaseMapLayer> surveyLayers = survey.getBaseMapLayers();
        List<BaseMapLayer> baseLayers = new ArrayList<BaseMapLayer>(BaseMapLayerSource.values().length);
        BaseMapLayer defaultLayer = null;
        boolean showDefaults = surveyLayers.size() < 1;
        for (BaseMapLayer baseMapLayer : surveyLayers) {
            enumKeys.remove(baseMapLayer.getLayerSource());
            if (baseMapLayer.isDefault()) {
                defaultLayer = baseMapLayer;
            }
            baseMapLayer.setShowOnMap(true);
            baseLayers.add(baseMapLayer);
        }
        
        // create fake base layers for any remaining enum values
        for (BaseMapLayerSource baseMapLayerSource : enumKeys) {
            boolean isGoogleDefault = defaultLayer == null && BaseMapLayerSource.G_HYBRID_MAP.equals(baseMapLayerSource);
            boolean isDefault = defaultLayer != null && baseMapLayerSource.equals(defaultLayer.getLayerSource());
            // set the layer to visible if no values have been saved and it is a Google layer
            // or if there is no default and it is G_HYBRID_MAP (for the default)
            BaseMapLayer layer = new BaseMapLayer(null, baseMapLayerSource, isGoogleDefault || isDefault, (showDefaults && BaseMapLayerSource.isGoogleLayerSource(baseMapLayerSource)) || isGoogleDefault || isDefault);
            baseLayers.add(layer);
        }
        
        // get the bdrs layers for the survey
        List<GeoMapLayer> bdrsLayers = geoMapLayerDAO.getAllLayers();
        List<SurveyGeoMapLayer> selectedBdrsLayers = survey.getGeoMapLayers();
        List<SurveyGeoMapLayer> allBdrsLayers = new ArrayList<SurveyGeoMapLayer>(bdrsLayers.size());
        for (GeoMapLayer geoMapLayer : bdrsLayers) {
            SurveyGeoMapLayer thisLayer = new SurveyGeoMapLayer(survey, geoMapLayer);
            if (selectedBdrsLayers.contains(thisLayer)) {
                thisLayer = selectedBdrsLayers.get(selectedBdrsLayers.indexOf(thisLayer));
            }
            thisLayer.setShowOnMap(selectedBdrsLayers.contains(thisLayer));
            allBdrsLayers.add(thisLayer);
        }
        
        // sort the lists before adding to the view
        Collections.sort(baseLayers);
        Collections.sort(allBdrsLayers);
        
        log.debug("after sorting, base layers are: ");
        for (BaseMapLayer baseLayer : baseLayers) {
            log.debug("        "+baseLayer.getLayerSource().getName()+" with weight "+baseLayer.getWeight());
        }
        log.debug("after sorting, bdrs layers are: ");
        for (SurveyGeoMapLayer baseLayer : allBdrsLayers) {
            log.debug("        "+baseLayer.getLayer().getName()+" with weight "+baseLayer.getWeight());
        }
        
        mv.addObject("baseLayers", baseLayers);
        mv.addObject("bdrsLayers", allBdrsLayers);
        return mv;
    }
    
    /**
     * View for saving survey map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request
     * @param response
     * @return
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/bdrs/admin/survey/editMap.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyMap(HttpServletRequest request, HttpServletResponse response) {
        Survey survey = getSurvey(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        // save the zoom and center defaults
        String zoom = request.getParameter("zoomLevel");
        Metadata md = survey.addMetadata(Metadata.MAP_DEFAULT_ZOOM, zoom);
        metadataDAO.save(md);
        String center = request.getParameter("mapCenter");
        md = survey.addMetadata(Metadata.MAP_DEFAULT_CENTER, center);
        metadataDAO.save(md);
        surveyDAO.save(survey);
        
        String defaultLayerSource = request.getParameter("default");
        // save any changes to exisitng base layers and remove any that are no longer selected
        for (BaseMapLayerSource layerSource : BaseMapLayerSource.values()) {
            String layerId = request.getParameter("id_"+layerSource);
            BaseMapLayer layer;
            if(layerId != null && !layerId.isEmpty()) {
                layer = baseMapLayerDAO.getBaseMapLayer(Integer.valueOf(layerId));
                if (layer == null) {
                    throw new NullPointerException("Null object returned for layer with id "+layerId);
                }
            } else {
                layer = new BaseMapLayer(survey, layerSource);
            }
            
            String selected = request.getParameter("selected_"+layerSource);
            if (!StringUtils.nullOrEmpty(selected) || layerSource.toString().equals(defaultLayerSource)) {
                // the layer is selected, update the default setting
                layer.setDefault(layerSource.toString().equals(defaultLayerSource));
                layer.setWeight(Integer.valueOf(request.getParameter("weight_"+layerSource)));
                baseMapLayerDAO.save(layer);
                survey.addBaseMapLayer(layer);
            } else {
                // the layer is not selected, delete it from the database
                if (layer.getId() != null) {
                    baseMapLayerDAO.delete(layer);
                    survey.removeBaseMapLayer(layer);
                }
            }
        }
        
        // save any bdrs layers that have been selected/deselected
        List<SurveyGeoMapLayer> surveyLayers = survey.getGeoMapLayers();
        if(request.getParameterValues("bdrsLayer") != null) {
            for(String layerId : request.getParameterValues("bdrsLayer")) {
                if(layerId != null && !layerId.isEmpty()) {
                    GeoMapLayer layer = geoMapLayerDAO.get(Integer.valueOf(layerId));
                    if (layer != null) {
                        String selected = request.getParameter("bdrs_selected_"+layerId);
                        SurveyGeoMapLayer thisLayer = new SurveyGeoMapLayer(survey, layer);
                        if (surveyLayers.contains(thisLayer)) {
                            thisLayer = surveyLayers.get(surveyLayers.indexOf(thisLayer));
                        }
                        if (!StringUtils.nullOrEmpty(selected)) {
                            // save the new layer or updated old one
                            int weight = Integer.valueOf(request.getParameter("weight_"+layerId));
                            log.debug("saving layer "+layerId+" with weight "+weight);
                            thisLayer.setWeight(weight);
                            if (surveyLayers.contains(thisLayer)) {
                                survey.addGeoMapLayer(thisLayer);
                            }
                            surveyDAO.save(thisLayer);
                        } else {
                            // remove the layer if it exists
                            if (surveyLayers.contains(thisLayer)) {
                                survey.removeGeoMapLayer(thisLayer);
                                surveyDAO.delete(thisLayer);
                            }
                        }
                    }
                }
            }
            // save the survey at the end of all the changes
            survey = surveyDAO.save(survey);
        }
        
        ModelAndView mv;
        if(request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new RedirectView("/bdrs/admin/survey/locationListing.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        } else {
            mv = new ModelAndView(new RedirectView(SURVEY_LISTING_URL, true));
        }
        return mv;
    }
}