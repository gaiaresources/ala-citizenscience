package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.controller.RenderController;
import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyGeoMapLayer;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper for holding constants used across the board for record forms.
 *
 * The RecordWebFormContext also holds the set of FormFields that make up the form and can filter them based on
 * the fields defined visibility.
 *
 * It is also responsible for determining the context in which the Record/Survey is being viewed.
 * 
 * @author aaron
 *
 */
public class RecordWebFormContext {
    
    /**
     * Request param - requests for an editable form
     */
    public static final String PARAM_EDIT = "editForm";
    /**
     * Model key - is the form editable
     */
    public static final String MODEL_EDIT = "editEnabled";
    /**
     * Model key - for the RecordWebFormContext
     */
    public static final String MODEL_WEB_FORM_CONTEXT = "recordWebFormContext";
    /**
     * Request param - is the form in preview mode
     */
    public static final String PARAM_PREVIEW = "preview";
    /**
     * Request param - the survey to open the form with.
     */
    public static final String PARAM_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    /**
     * Msg code - cannot edit form due to auth failure
     */
    public static final String MSG_CODE_EDIT_AUTHFAIL = "bdrs.record.edit.authfail";
    /**
     * Msg code - cannot view form due to auth failure
     */
    public static final String MSG_CODE_VIEW_AUTHFAIL = "bdrs.record.view.authfail";
    /**
     * Msg code - cannot create/edit records in the survey due to auth failure
     */
    protected static final String MSG_SURVEY_AUTHFAIL = "bdrs.record.survey.authfail";
    /**
     * Request param - passed when the POST should redirect to a blank version of the same
     * form that has just been posted
     */
    public static final String PARAM_SUBMIT_AND_ADD_ANOTHER = "submitAndAddAnother";
    /**
     * Request param - redirect url to use after record form post.
     */
    public static final String PARAM_REDIRECT_URL = "redirecturl";
        
    /**
     * Url for survey redirect, see RenderController. This needs refactoring !
     * This actually causes a compile time circular dependency but java can compile anyway so...
     * it's ok for now ?
     */
    public static final String SURVEY_RENDER_REDIRECT_URL = RenderController.SURVEY_RENDER_REDIRECT_URL;
    
    /**
     * Request param - census method ID used to open the form.
     */
    public static final String PARAM_CENSUS_METHOD_ID = BdrsWebConstants.PARAM_CENSUS_METHOD_ID;
    
    // From MySightingsController - the query parameter record ID.
    // Not refering to MySightings directly here since we may introduce
    // cyclic dependencies.
    public static final String MODEL_RECORD_ID = "record_id";

    private boolean existingRecord;
    private boolean unlockable;
    private Integer recordId;
    private Integer surveyId;
    private boolean moderateOnly;

    /** True if the current User can comment on the Record being accessed */
    private boolean commentable;
    
    /** True if the Record is being accessed by an anonymous user */
    private boolean anonymous;
    
    private JSONArray mapBaseLayers = new JSONArray();
    private JSONArray geoMapLayers = new JSONArray();

    /**
     * The collection of FormFields that make up the form data to be displayed,  grouped by a (String) identifier
     * determined by the controller that created this WebFormRecordContext
     */
    private Map<String, List<FormField>> namedFormFields = new HashMap<String, List<FormField>>();

    /**
     * The collection of RecordFormFieldCollections that make up the per Record form data to be displayed in single
     * site multi/all taxa forms,  grouped by a (String) identifier determined by the controller that created this
     * RecordWebFormContext.
     */
    private Map<String, List<RecordFormFieldCollection>> namedCollections = new HashMap<String, List<RecordFormFieldCollection>>();

    /** The context in which the record form will be rendered */
    private DisplayContext context;
    
    /**
     * Create a new web form context for use with the form GET handler
     * 
     * Will throw AccessDeniedExceptions if appropriate.
     * 
     * @param request - the HttpServletRequest used when requesting the form
     * @param recordToLoad - The record requested to load, can be null
     * @param accessingUser - the User attempting to access the web form, can be null
     * @param survey - the survey that for the record. passing it in as a separate parameter as sometimes there is no
     * record to edit e.g. when previewing a form
     */
    public RecordWebFormContext(HttpServletRequest request, Record recordToLoad, User accessingUser, Survey survey) {
        determineContext(request, recordToLoad);
        
        // if the record is non persisted, we always want to edit.
        if (recordId != null) {

            // set if the edit is moderation fields only
            // this is only true if the form is editable, it is not an anonymous user
            // the user is not the owner and the user is a moderator but not admin
            moderateOnly = isEditable() && accessingUser != null && !accessingUser.equals(recordToLoad.getUser()) && accessingUser.isModerator() && !accessingUser.isAdmin();
        }

        surveyId = survey != null ? survey.getId() : null;
        existingRecord = recordId != null;
        unlockable = existingRecord && recordToLoad.canWrite(accessingUser);
        commentable = (context == DisplayContext.VIEW) && recordToLoad.canComment(accessingUser);
        anonymous = (accessingUser == null);
        if (isEditable()) {
            surveyAccessSecurityCheck(survey, accessingUser);
        }
        recordAccessSecurityCheck(recordToLoad, accessingUser, isEditable());
        
        // add the flattened layers to the page so they can be referenced 
        // through the javascript on the map pages
        if (survey != null) {
            List<BaseMapLayer> sortedLayers = survey.getBaseMapLayers();
            Collections.sort(sortedLayers);
            for (BaseMapLayer layer : sortedLayers) {
                mapBaseLayers.add(layer.flatten());
            }
            
            List<SurveyGeoMapLayer> sortedGeoLayers = survey.getGeoMapLayers();
            Collections.sort(sortedGeoLayers);
            for (SurveyGeoMapLayer layer : sortedGeoLayers) {
                geoMapLayers.add(layer.flatten(2));
            }
        }
        

    }

    private void determineContext(HttpServletRequest request, Record record) {
        recordId = record != null ? record.getId() : null;
        if (request.getParameter(PARAM_PREVIEW) != null) {
            context = DisplayContext.PREVIEW;
        }
        else if (recordId == null) {
            context = DisplayContext.CREATE;
        }
        else if (checkParameterValue(request, PARAM_EDIT)) {
            context = DisplayContext.EDIT;
        }
        else {
            context = DisplayContext.VIEW;
        }
    }

    /**
     * Helper method for dealing with boolean parameters.  A missing parameter is treated as false.
     * @param request the HTTP request to retrieve the paramter from.
     * @param parameterName the name of the parameter to retrieve.
     * @return false if the parameter is missing or does not have the value "true".
     */
    private boolean checkParameterValue(HttpServletRequest request, String parameterName) {
        String parameter = request.getParameter(parameterName);
        return parameter != null ? Boolean.parseBoolean(parameter) : false;
    }
    
    /**
     * Create a new web form context for use with the form POST handler
     * 
     * Will throw AccessDeniedExceptions if appropriate
     * 
     * @param recordToSave - The record requested to save, can be null
     * @param writingUser - the User attempting to save the web form, can be null
     */
    public RecordWebFormContext(Record recordToSave, User writingUser) {
        // we are posting so editable must always be true.
        context = DisplayContext.EDIT;
        recordAccessSecurityCheck(recordToSave, writingUser, isEditable());
    }
    
    /**
     * Helper method to throw AccessDeniedExceptions if the record cannot be edited or viewed.
     * The exceptions are handled in HandlerExceptionResolver.java
     * 
     * @param record - the record we are attempting to access. Can be nullable
     * @param loggedInUser - the user that is attempting to access the record. can be null
     * @param editEnabled - whether the record is being edited or viewed
     */
    private static void recordAccessSecurityCheck(Record record, User loggedInUser, boolean editEnabled) {
        if (record == null) {
            record = new Record();
        }
        // Check whether we can write or not...
        if (editEnabled) {
            if (!record.canWrite(loggedInUser)) {
                throw new AccessDeniedException(MSG_CODE_EDIT_AUTHFAIL);
            }   
        } else {
            if (!record.canView(loggedInUser)) {
                throw new AccessDeniedException(MSG_CODE_VIEW_AUTHFAIL);
            }
        }
        // if security pass is successful no exceptions will be thrown
    }
    
    /**
     * Helper method to throw AccessDeniedExceptions if the user does not have permission to write
     * to a survey and they have requested an edit.
     * @param survey the survey we are trying to write to
     * @param user the user we are trying to write as
     */
    private static void surveyAccessSecurityCheck(Survey survey, User user) {
        if (!survey.canWriteSurvey(user)) {
            throw new AccessDeniedException(MSG_SURVEY_AUTHFAIL);
        }
    }
    
    /**
     * Is this web form editable (vs in view mode)
     * 
     * @return true if edit mode enabled, false otherwise
     */
    public boolean isEditable() {
        return context == DisplayContext.CREATE || context == DisplayContext.EDIT || context == DisplayContext.PREVIEW;
    }
    
    /**
     * Is the web form editable for moderation only (i.e. only moderation fields are editable)
     * @return true if only moderation fields are editable, false if all fields are editable
     */
    public boolean isModerateOnly() {
        return this.moderateOnly;
    }
    
    /**
     * Is the accessing user capable of unlocking the requested record
     * @return boolean
     */
    public boolean isUnlockable() {
        return this.unlockable;
    }
    
    /**
     * Whether the record requested was an existing record or not
     * @return boolean
     */
    public boolean isExistingRecord() {
        return this.existingRecord;
    }
    
    /**
     * The record ID requested to be opened in this form
     * @return Integer record ID
     */
    public Integer getRecordId() {
        return this.recordId;
    }
    
    /**
     * The survey id to be opened for this form
     * @return Integer survey ID
     */
    public Integer getSurveyId() {
        return this.surveyId;
    }
    
    /**
     * Is the form being opened for preview
     * @return boolean
     */
    public boolean isPreview() {
        return context == DisplayContext.PREVIEW;
    }

    /**
     * @return true if the current User can comment on the Record being accessed.
     */
    public boolean isCommentable() {
        return this.commentable;
    }

    /**
     * @return true if the Record is being accessed by an anonymous user.
     */
    public boolean isAnonymous() {
        return this.anonymous;
    }
    
    /**
     * hacky helper method. Used to add the record ID onto the model and view
     * for highlighting after the redirect.
     * 
     * @param mv ModelAndView
     * @param r Record
     */
    public static void addRecordHighlightId(ModelAndView mv, Record r) {
        if (r != null) {
            mv.addObject(MODEL_RECORD_ID, r.getId());
        }
    }
        
    /**
     * Returns the Redirect model and view
     * 
     * @param request - HttpServletRequest from record form post
     * @param r - the persisted record
     * @return ModelAndView
     */
    public static ModelAndView getSubmitRedirect(HttpServletRequest request, Record r) {
        
        Survey survey = r.getSurvey();
        CensusMethod cm = r.getCensusMethod();
        RedirectionService redirectionService = AppContext.getBean(RedirectionService.class);
        
        ModelAndView mv;
        if (request.getParameter(PARAM_SUBMIT_AND_ADD_ANOTHER) != null) {
            mv = new ModelAndView(new RedirectView(
                    SURVEY_RENDER_REDIRECT_URL, true));
            mv.addObject(PARAM_SURVEY_ID, survey.getId());
            if (cm != null) {
                mv.addObject(PARAM_CENSUS_METHOD_ID, cm.getId());   
            }
        } else {
            // Normal submit case:
            if (request.getSession().getAttribute(PARAM_REDIRECT_URL) != null) {
                mv = new ModelAndView("redirect:"
                        + request.getSession().getAttribute(PARAM_REDIRECT_URL));
            } else if (request.getParameter(PARAM_REDIRECT_URL) != null) { 
                mv = new ModelAndView("redirect:"
                        + request.getParameter(PARAM_REDIRECT_URL));
            } else {
                switch (survey.getFormSubmitAction()) {
                case STAY_ON_FORM:
                    mv = new ModelAndView(new RedirectView(redirectionService.getViewRecordUrl(r), true));   
                    break;
                case MY_SIGHTINGS:
                default:
                    mv = new ModelAndView(new RedirectView(redirectionService.getMySightingsUrl(survey), true));
                    // highlight the record that has been created...
                    RecordWebFormContext.addRecordHighlightId(mv, r);
                    break;
                }
            }
        }
        return mv;
    }
    
    
    /**
     * Gets the base layers that should be shown on the map.
     * @return the mapBaseLayers
     */
    public String getMapBaseLayers() {
        return mapBaseLayers.toString();
    }
    
    /**
     * Gets the geo map layers that should be shown on the map.
     * @return the geoMapLayers
     */
    public String getGeoMapLayers() {
        return geoMapLayers.toString();
    }

    /**
     * Adds a List of FormField objects to this record form, identified by a specific name.
     * @param name the name under which the FormFields can be retrieved.
     * @param fields the FormFields to add.
     */
    public void addFormFields(String name, List<FormField> fields) {
        filterOnVisibility(fields);
        namedFormFields.put(name, fields);
    }

    /**
     * Exposes the FormFields owned by this record form as a Map.  This has been done to allow easy access to the
     * FormFields using JSTL.
     * Note that FormFields that are not visible in the current displayContext will not be returned by this
     * method.
     * @return a Map of String (name) to List&lt;FormField&gt;
     */
    public Map<String, List<FormField>> getNamedFormFields() {
        return namedFormFields;
    }

    /**
     * Adds a List of RecordFormFieldCollection objects to this record form, identified by a specific name.
     * Note that FormFields contained in each RecordFormFieldCollection that are not visible in the current
     * displayContext will not be returned by this method.
     * @param name the name under which the RecordFormFieldCollection can be retrieved.
     * @param fields the RecordFormFieldCollections to add.
     */
    public void addRecordCollection(String name, List<RecordFormFieldCollection> fields) {
       
        for (RecordFormFieldCollection collection : fields) {
            filterOnVisibility(collection.getFormFields());
        }
        namedCollections.put(name, fields);
    }

    /**
     * Exposes the RecordFormFieldCollections owned by this record form as a Map.  This has been done to allow easy
     * access to the RecordFormFieldCollections using JSTL.
     * @return a Map of String (name) to List&lt;RecordFormFieldCollection&gt;
     */
    public Map<String, List<RecordFormFieldCollection>> getNamedCollections() {
        return namedCollections;
    }

    /**
     * Removes FormFields that are not visible in the current display context from the supplied list.
     * @param fields a List of FormFields to filter.
     */
    private void filterOnVisibility(List<FormField> fields) {
        Iterator<FormField> fieldIterator = fields.iterator();
        while (fieldIterator.hasNext()) {
            if (!fieldIterator.next().isVisible(context)) {
                fieldIterator.remove();
            }
        }
    }


}
