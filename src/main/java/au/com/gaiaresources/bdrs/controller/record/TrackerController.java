package au.com.gaiaresources.bdrs.controller.record;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.RenderController;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.RecordDeserializer;
import au.com.gaiaresources.bdrs.deserialization.record.RecordDeserializerResult;
import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;
import au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup;
import au.com.gaiaresources.bdrs.message.Message;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordService;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeUtil;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.content.ContentService;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;

/**
 * Controller for the default 'tracker' form.
 * 
 */
@Controller
public class TrackerController extends RecordController {

    private Logger log = Logger.getLogger(getClass());
    
    /**
     * Edit URL for the tracker form
     */
    public static final String EDIT_URL = "/bdrs/user/tracker.htm";
    /**
     * Prefix expected in parameter map entries for taxon group attributes
     */
    public static final String TAXON_GROUP_ATTRIBUTE_PREFIX = "taxonGroupAttr_";
    /**
     * Prefix expected in parameter map entries for census method attributes
     */
    public static final String CENSUS_METHOD_ATTRIBUTE_PREFIX = "censusMethodAttr_";
    /**
     * Msg code - taxon and number required together
     */
    public static final String TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE_KEY = "Tracker.TaxonAndNumberRequiredTogether";
    
    /**
     * Default message - taxon and number required together
     */
    public static final String TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE = "Species and number must both be blank, or both filled in.";
    /**
     * Request param, survey ID
     */
    public static final String PARAM_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    /**
     * Request param, census method ID
     */
    public static final String PARAM_CENSUS_METHOD_ID = BdrsWebConstants.PARAM_CENSUS_METHOD_ID;
    /**
     * Request param, species ID
     */
    public static final String PARAM_SPECIES_ID = "species";
    /**
     * Request param, string of species to search for
     */
    public static final String PARAM_SPECIES_NAME = "survey_species_search";
    /**
     * Request param, individual count
     */
    public static final String PARAM_INDIVIDUAL_COUNT = "number";
    /**
     * Request param, existing record ID
     */
    public static final String PARAM_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    /**
     * Request param, notes
     */
    public static final String PARAM_NOTES = "notes";
    /**
     * Request param, hour part of time
     */
    public static final String PARAM_TIME_HOUR = "time_hour";
    /**
     * Request param, minute part of time
     */
    public static final String PARAM_TIME_MINUTE = "time_minute";
    /**
     * Request param, date of record
     */
    public static final String PARAM_DATE = "date";
    /**
     * Request param, latitude of record
     */
    public static final String PARAM_LATITUDE = "latitude";
    /**
     * Request param, longitude of record
     */
    public static final String PARAM_LONGITUDE = "longitude";
    /**
     * Request param, pre-canned location of record.
     */
    public static final String PARAM_LOCATION = "location";
    /**
     * Request param, accuracy of the lat/lon of record
     */
    public static final String PARAM_ACCURACY = "accuracyInMeters";
    /**
     * Request param, name of the pre-canned location of record
     */
    public static final String PARAM_LOCATION_NAME = "locationName";
    /**
     * Request param, time the record was taken.
     */
    public static final String PARAM_TIME = "time";
    /**
     * Request param, the wkt string representing the geographical location
     * of the record
     */
    public static final String PARAM_WKT = BdrsWebConstants.PARAM_WKT;
    /**
     * Request param - the visibility of the record.
     */
    public static final String PARAM_RECORD_VISIBILITY = "recordVisibility";
    
    /**
     * Msg code to be used when the tracker form saves changes to an existing record
     * and redirects to the my sightings form.
     */
    public static final String MSG_CODE_SAVE_EXISTING_SUCCESS_MY_SIGHTINGS = "bdrs.record.saveExisting.success.mySightings";
    
    /**
     * Msg code to be used when the tracker form saves changes to an existing record
     * and redirects to the editing form.
     */
    public static final String MSG_CODE_SAVE_EXISTING_SUCCESS_STAY_ON_FORM = "bdrs.record.saveExisting.success.stayOnForm";
    
    /**
     * Msg code to be used when the tracker form saves a new record and redirects
     * to the my sightings form.
     */
    public static final String MSG_CODE_SAVE_NEW_SUCCESS_MY_SIGHTINGS = "bdrs.record.save.success.mySightings";
    
    /**
     * Msg code to be used when the tracker form saves a new record and redirects
     * to the editing form.
     */
    public static final String MSG_CODE_SAVE_NEW_SUCCESS_STAY_ON_FORM = "bdrs.record.save.success.stayOnForm";
    
    /**
     * Msg code to be used when the tracker form saves an existing record and redirects
     * back to a new, empty, editing form
     */
    public static final String MSG_CODE_SAVE_EXISTING_SUCCESS_ADD_ANOTHER = "bdrs.record.saveExisting.success.addAnother";
    
    /**
     * Msg code to be used when the tracker form saves a new record and redirects back
     * to a new, empty, editing form
     */
    public static final String MSG_CODE_SAVE_SUCCESS_ADD_ANOTHER = "bdrs.record.save.success.addAnother";
    
    /**
     * Selects the record tab
     */
    public static final String SELECT_TAB_RECORD = "record";
    /**
     * Selects the sub record tab
     */
    public static final String SELECT_TAB_SUB_RECORD = "subRecord";
    /**
     * Request param : used when we are creating a new record, assign the record
     * with this ID as the parent record.
     */
    public static final String PARAM_PARENT_RECORD_ID = "parentRecordId";
    /**
     * Request param : which tab to show on the tracker form. Only has an effect 
     * if the tabs are shown.
     */
    public static final String PARAM_SELECTED_TAB = "selectedTab";
    /**
     * Request param : when true sumbut opens a new form for the same survey.
     */
    public static final String PARAM_SUBMIT_AND_SWITCH_TO_SUB_RECORD_TAB = "submitAndSwitchToSubRecordTab";
    /**
     * Model key - WKT string of the record geographical location
     */
    public static final String MV_WKT = "wkt";
    /**
     * Model key - error map of the record data that was just posted.
     */
    public static final String MV_ERROR_MAP = "errorMap";
    /**
     * Model key - value map of the record data that was just posted.
     */
    public static final String MV_VALUE_MAP = "valueMap";
    /**
     * Model key - parent record ID
     */
    public static final String MV_PARENT_RECORD_ID = "parentRecordId";
    /**
     * Model key - list of parent records, 0 index is the most senior.
     */
    public static final String MV_PARENT_RECORD_LIST = "parentRecordList";
    /**
     * Msg code - no survey error
     */
    public static final String NO_SURVEY_ERROR_KEY = "bdrs.record.noSurveyError";
    /**
     * View name - tracker view
     */
    public static final String TRACKER_VIEW_NAME = "tracker";

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private CensusMethodDAO cmDAO;
    @Autowired
    private RedirectionService redirectionService;
    @Autowired
    private MetadataDAO metadataDAO;
    /** Required to get the user who last updated the Record */
    @Autowired
    private RecordService recordService;
    @Autowired
    private GeoMapService geoMapService;
    @Autowired
    private LocationService locationService;

    /**
     * Handler to get the tracker form. Will populate the form with values if an existing
     * record ID is passed.
     * 
     * @param request - the http request object
     * @param response - the http response object
     * @param surveyId - the ID for the survey to create a new record for. Is still required when requesting
     * an existing record
     * @param taxonSearch - a taxon scientific name to search for to populate the species field of a record
     * @param recordId - the ID for the record to retrieve and populate form values with
     * @param guid - a guid for a species to search for to populate the species field of a record
     * @param censusMethodId - the census method id to use when creating a new record
     * @return a ModelAndView for rendering the tracker form
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = EDIT_URL, method = RequestMethod.GET)
    public ModelAndView addRecord(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = PARAM_SURVEY_ID, required = true) int surveyId,
            @RequestParam(value = "taxonSearch", required = false) String taxonSearch,
            @RequestParam(value = PARAM_RECORD_ID, required = false, defaultValue = "0") int recordId,
            @RequestParam(value = "guid", required = false) String guid,
            @RequestParam(value = PARAM_CENSUS_METHOD_ID, required = false, defaultValue = "0") Integer censusMethodId,
            @RequestParam(value = "speciesId", required = false, defaultValue="0") Integer speciesId,
            @RequestParam(value = PARAM_PARENT_RECORD_ID, required = false) Integer parentRecordId,
            @RequestParam(value = PARAM_SELECTED_TAB, required = false) String selectedTab) {
        Survey survey = surveyDAO.getSurvey(surveyId);
        if (survey == null) {
            return nullSurveyError();
        }
        
        Record record = recordDAO.getRecord(recordId);
        CensusMethod censusMethod = record == null? cmDAO.get(censusMethodId) : record.getCensusMethod();
        
        record = record == null ? new Record() : record;
        
        User loggedInUser = getRequestContext().getUser();
        RecordWebFormContext context = new RecordWebFormContext(request, record, loggedInUser, survey, geoMapService);
        
        // if this is a new record...
        if (record.getId() == null) {
            // set survey specific record defaults
            record.setRecordVisibility(survey.getDefaultRecordVisibility());
        }

        IndicatorSpecies species = determineSpecies(surveyId, taxonSearch, guid, speciesId, record);
        Map<String, String> valueMap = (Map<String, String>)getRequestContext().getSessionAttribute(MV_VALUE_MAP);
        getRequestContext().removeSessionAttribute(MV_VALUE_MAP);
        
        if (valueMap != null) {
            // mock save record with value map results to populate form on server error
            RecordKeyLookup lookup = new TrackerFormRecordKeyLookup();
            TrackerFormToRecordEntryTransformer transformer = new TrackerFormToRecordEntryTransformer();
            TrackerFormAttributeDictionaryFactory adf = new TrackerFormAttributeDictionaryFactory();
            AttributeParser parser = new WebFormAttributeParser(taxaDAO);
            
            RecordDeserializer rds = new RecordDeserializer(lookup, adf, parser);
            Map<String,String[]> convertedMap = convertMap(valueMap);
            List<RecordEntry> entries = transformer.httpRequestParamToRecordMap(convertedMap, Collections.EMPTY_MAP);
            List<RecordDeserializerResult> results;
            try {
                results = rds.deserialize(getRequestContext().getUser(), entries, false, false);
                if (results.size() == 1) {
                    record = results.get(0).getRecord();
                } else {
                    log.error("Error loading record from value map: Too many records returned: got "+results.size()+" but expected 1");
                }
            } catch (Exception e) {
                log.error("Error loading record from value map", e);
            }
        }
        
        // Add all attribute form fields
        FormField formField;
        Attribute attr;
        List<FormField> surveyFormFieldList = new ArrayList<FormField>();
        List<FormField> taxonGroupFormFieldList = new ArrayList<FormField>();
        List<FormField> censusMethodFormFieldList = new ArrayList<FormField>();
        List<Attribute> surveyAttributeList = new ArrayList<Attribute>(survey.getAttributes());
        List<Attribute> taxonGroupAttributeList = new ArrayList<Attribute>();
        List<Attribute> censusMethodAttributeList = censusMethod != null ? new ArrayList<Attribute>(censusMethod.getAttributes()) : new ArrayList<Attribute>();
        
        if(species != null) {
            for(Attribute taxonGroupAttribute : species.getTaxonGroup().getAttributes()) {
                if(!taxonGroupAttribute.isTag()) {
                    taxonGroupAttributeList.add(taxonGroupAttribute);
                }
            }
        }
        
        for (TypedAttributeValue recAttr : record.getAttributes()) {
            attr = recAttr.getAttribute();
            // If you are a survey attribute, add to the survey form fields
            // otherwise add to the group form fields. This is done because
            // group form fields are sorted separately with survey form fields
            // displayed above group form fields.
            if(surveyAttributeList.remove(attr)) {
                // only add moderation attributes if the user is a moderator
                // or if the loggedInUser is the owner of the record
                if (AttributeUtil.isVisibleByScopeAndUser(attr, loggedInUser, recAttr) ||
                        (loggedInUser != null && loggedInUser.equals(record.getUser())) ||
                        !AttributeScope.isModerationScope(attr.getScope())) {
                    if (AttributeType.isCensusMethodType(attr.getType())) {
                        formField = createCensusMethodFormField(survey, record, attr, loggedInUser, AttributeParser.DEFAULT_PREFIX, context);
                    } else {
                        formField = formFieldFactory.createRecordFormField(survey, record, attr, recAttr);
                    }
                    if (formField != null) {
                        surveyFormFieldList.add(formField);
                    }
                }
            } else if(taxonGroupAttributeList.remove(attr)) {
                if (AttributeType.isCensusMethodType(attr.getType())) {
                    formField = createCensusMethodFormField(survey, record, attr, loggedInUser, TAXON_GROUP_ATTRIBUTE_PREFIX, context);
                } else {
                    formField = formFieldFactory.createRecordFormField(survey, record, attr, recAttr, TAXON_GROUP_ATTRIBUTE_PREFIX);
                }
                if (formField != null) {
                    taxonGroupFormFieldList.add(formField);
                }
            } else if (censusMethodAttributeList.remove(attr)) {
                if (AttributeType.isCensusMethodType(attr.getType())) {
                    formField = createCensusMethodFormField(survey, record, attr, loggedInUser, CENSUS_METHOD_ATTRIBUTE_PREFIX, context);
                } else {
                    formField = formFieldFactory.createRecordFormField(survey, record, attr, recAttr, CENSUS_METHOD_ATTRIBUTE_PREFIX);
                }
                if (formField != null) {
                    censusMethodFormFieldList.add(formField);
                }
            }
        }
        
        // If there were no pre-existing values for the attributes, add 
        // the blank fields now.
        for (Attribute surveyAttr : surveyAttributeList) {
            if(!AttributeScope.LOCATION.equals(surveyAttr.getScope())) {
                AttributeValue attrVal = AttributeValueUtil.getAttributeValue(surveyAttr, record);
                // only add moderation attributes if the user is a moderator or if it is the owner of the record
                if (AttributeUtil.isVisibleByScopeAndUser(surveyAttr, loggedInUser, attrVal) ||
                        (loggedInUser != null && loggedInUser.equals(record.getUser())) ||
                        !AttributeScope.isModerationScope(surveyAttr.getScope())) {
                    if (AttributeType.isCensusMethodType(surveyAttr.getType())) {
                        FormField ff = createCensusMethodFormField(survey, record, surveyAttr, loggedInUser, AttributeParser.DEFAULT_PREFIX, context);
                        if (ff != null) {
                            surveyFormFieldList.add(ff);
                        }
                    } else {
                        surveyFormFieldList.add(formFieldFactory.createRecordFormField(survey, record, surveyAttr));
                    }
                }
            }
        }
        for (Attribute taxonGroupAttr : taxonGroupAttributeList) {
            if (AttributeType.isCensusMethodType(taxonGroupAttr.getType())) {
                FormField ff = createCensusMethodFormField(survey, record, species, taxonGroupAttr, loggedInUser, TAXON_GROUP_ATTRIBUTE_PREFIX, context);
                if (ff != null) {
                    taxonGroupFormFieldList.add(ff);
                }
            } else {
                taxonGroupFormFieldList.add(formFieldFactory.createRecordFormField(survey, record, taxonGroupAttr, TAXON_GROUP_ATTRIBUTE_PREFIX));
            }
        }
        // Add census method form fields
        for (Attribute cmAttr : censusMethodAttributeList) {
            if (AttributeType.isCensusMethodType(cmAttr.getType())) {
                FormField ff = createCensusMethodFormField(survey, record, cmAttr, loggedInUser, CENSUS_METHOD_ATTRIBUTE_PREFIX, context);
                if (ff != null) {
                    censusMethodFormFieldList.add(ff);
                }
            } else {
                censusMethodFormFieldList.add(formFieldFactory.createRecordFormField(survey, record, cmAttr, CENSUS_METHOD_ATTRIBUTE_PREFIX));
            }
        }

        Taxonomic taxonomic;
        if(censusMethod != null && censusMethod.getTaxonomic() != null) {
            taxonomic = censusMethod.getTaxonomic();
        }
        else {
            taxonomic = Taxonomic.OPTIONALLYTAXONOMIC;
        }
        
        RecordPropertyType[] recordProperties;
        if(Taxonomic.OPTIONALLYTAXONOMIC.equals(taxonomic) || Taxonomic.TAXONOMIC.equals(taxonomic)) {
            recordProperties = RecordPropertyType.values();
        } else {
            recordProperties = Record.NON_TAXONOMIC_RECORD_PROPERTY_NAMES.toArray(
                    new RecordPropertyType[Record.NON_TAXONOMIC_RECORD_PROPERTY_NAMES.size()]);
        }
        
        boolean showMap =false;
        // Add all property form fields
        for (RecordPropertyType type : recordProperties) {
            RecordProperty recordProperty = new RecordProperty(survey, type, metadataDAO);
            //showMap if location or point fields are hidden.
            if(!showMap && (type.equals(RecordPropertyType.LOCATION) || type.equals(RecordPropertyType.POINT))){
                showMap = !recordProperty.isHidden();
            }
            surveyFormFieldList.add(formFieldFactory.createRecordFormField(record, recordProperty, species, taxonomic));
        }
        
        Collections.sort(surveyFormFieldList);
        Collections.sort(taxonGroupFormFieldList);
        Collections.sort(censusMethodFormFieldList);
        
        Map<String, String> errorMap = (Map<String, String>)getRequestContext().getSessionAttribute(MV_ERROR_MAP);
        getRequestContext().removeSessionAttribute(MV_ERROR_MAP);
        String wktString = (String)getRequestContext().getSessionAttribute(MV_WKT);
        getRequestContext().removeSessionAttribute(MV_WKT);

        Set<Location> locations = locationService.locationsForSurvey(survey, getRequestContext().getUser());

        User updatedByUser = recordService.getUpdatedByUser(record);

        // Add the form fields to the form
        context.addFormFields("surveyFormFieldList", surveyFormFieldList);
        context.addFormFields("taxonGroupFormFieldList", taxonGroupFormFieldList);
        context.addFormFields("censusMethodFormFieldList", censusMethodFormFieldList);
        
        ModelAndView mv = new ModelAndView(TRACKER_VIEW_NAME);
        mv.addObject("censusMethod", censusMethod);
        mv.addObject("record", record);
        mv.addObject(BdrsWebConstants.PARAM_RECORD_ID, record.getId());
        mv.addObject("survey", survey);
        mv.addObject("locations", locations);
        mv.addObject("preview", request.getParameter("preview") != null);
        mv.addObject("taxonomic", taxonomic);
        mv.addObject(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT, context);
        mv.addObject("parentRecordId", parentRecordId);
        mv.addObject("updatedBy", updatedByUser);
        // default to the record tab
        mv.addObject("selectedTab", StringUtils.hasLength(selectedTab) ? selectedTab : SELECT_TAB_RECORD);
        
        // make an ordered list of records to show the lineage of this record
        // index 0 is the most senior record.
        List<Record> parentRecordList = new ArrayList<Record>();
        Record r = record;
        while (r.getParentRecord() != null) {
            parentRecordList.add(0, r.getParentRecord());
            r = r.getParentRecord();
        }
        mv.addObject(MV_PARENT_RECORD_LIST, parentRecordList);
        
        if (StringUtils.hasLength(wktString)) {
            mv.addObject(MV_WKT, wktString);
        } else {
            mv.addObject(MV_WKT, (record.getGeometry() != null) ? record.getGeometry().toText() : "");
        }
        
        mv.addObject(MV_ERROR_MAP, errorMap);
        mv.addObject(MV_VALUE_MAP, valueMap);
        mv.addObject("displayMap", showMap);
        
        return super.addRecord(mv, loggedInUser);
    }
    /**
     * Determines the IndicatorSpecies to use when displaying the form.
     * @param surveyId the id of the survey to be displayed.
     * @param taxonSearch the value of the taxonSearch http parameter, can be used to identify the species.
     * @param guid the guid of the species to use.
     * @param speciesId the id of the species to use.
     * @param record the Record to be displayed.
     * @return the most appropriate IndicatorSpecies based on the supplied data.
     */
    private IndicatorSpecies determineSpecies(int surveyId, String taxonSearch, String guid, Integer speciesId, Record record) {
        IndicatorSpecies species = null;

        // check for the speciesId first
        if (speciesId > 0) {
            species = taxaDAO.getIndicatorSpecies(speciesId);
        }
        // first attempt to assign to the species from the form fields....
        if (species == null && guid != null && !guid.isEmpty()) {
            species = taxaDAO.getIndicatorSpeciesByGuid(guid);
        }
        if (species == null && taxonSearch != null && !taxonSearch.isEmpty()) {
            List<IndicatorSpecies> speciesList = surveyDAO.getSpeciesForSurveySearch(surveyId, taxonSearch);
            if (speciesList.isEmpty()) {
                species = null;
            } else if (speciesList.size() == 1) {
                species = speciesList.get(0);
            } else {
                log.warn("Multiple species found for survey " + surveyId
                        + " and taxon search \"" + taxonSearch
                        + "\". Using the first.");
                species = speciesList.get(0);
            }
        }
        // if the species is still null, use the species from the record.
        species = species != null ? species : record.getSpecies();
        return species;
    }

    /**
     * The POST handler for saving tracker forms
     * 
     * @param request - the http request object
     * @param response - the http response object
     * @param surveyPk - the survey to create  a new record for (if not editing an existing record)
     * @param censusMethodId - the census method to create a new record for (if not editing an existing record)
     * @return - A RedirectView
     * @throws ParseException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @RolesAllowed( {  Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN })
    @RequestMapping(value = EDIT_URL, method = RequestMethod.POST)
    public ModelAndView saveRecord(MultipartHttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value=PARAM_SURVEY_ID, required=true) int surveyPk,
            // We are allowing a null census method ID to indicate a default form
            @RequestParam(value=PARAM_CENSUS_METHOD_ID, required=false, defaultValue="0") int censusMethodId,
            @RequestParam(value=PARAM_SELECTED_TAB, required=false) String selectedTab) throws ParseException, IOException {
        Map<String, String[]> parameterMap = this.getModifiableParameterMap(request);
        
        Survey survey = surveyDAO.getSurvey(surveyPk);
        if (survey == null) {
            return nullSurveyError();
        }
        
        RecordKeyLookup lookup = new TrackerFormRecordKeyLookup();
        
        TrackerFormToRecordEntryTransformer transformer = new TrackerFormToRecordEntryTransformer();
        TrackerFormAttributeDictionaryFactory adf = new TrackerFormAttributeDictionaryFactory();
        AttributeParser parser = new WebFormAttributeParser(taxaDAO);
        
        RecordDeserializer rds = new RecordDeserializer(lookup, adf, parser);
        List<RecordEntry> entries = transformer.httpRequestParamToRecordMap(parameterMap, request.getFileMap());
        List<RecordDeserializerResult> results = rds.deserialize(getRequestContext().getUser(), entries);
        
        // there should be exactly 1 result since we are only putting in 1 RecordEntry...
        if (results.size() != 1) {
            log.warn("Expecting only 1 deserialization result but got: " + results.size());
        }
        RecordDeserializerResult res = results.get(0);
        
        if (!res.isAuthorizedAccess()) {
            // Required since there will be an auto commit otherwise at the end of controller handling.
            requestRollback(request);
            
            throw new AccessDeniedException(RecordWebFormContext.MSG_CODE_EDIT_AUTHFAIL);
        }
        
        if (!res.getErrorMap().isEmpty()) {
            // an error has occured
            requestRollback(request);
            
            // create valueMap to repopulate the form...
            Map<String, String[]> params = request.getParameterMap();
            Map<String, String> valueMap = new HashMap<String, String>();
            for(Map.Entry<String, String[]> paramEntry : params.entrySet()) {
                if(paramEntry.getValue() != null && paramEntry.getValue().length > 0) {
                    if(paramEntry.getValue().length == 1) {
                            valueMap.put(paramEntry.getKey(), paramEntry.getValue()[0]);
                    } else {
                            // Not bothering with a csv writer here because the
                            // jsp template does a simple String.contains to check
                            // if the the multi select or multi combo should be picked.
                            StringBuilder b = new StringBuilder();
                            for(int q = 0; q<paramEntry.getValue().length; q++) {
                                    b.append(paramEntry.getValue()[q]);
                                    b.append(',');
                            }
                            valueMap.put(paramEntry.getKey(), b.toString());
                    }
                }
            }
            
            // collect some details for reporting
            IndicatorSpecies species;
            try {
                species = taxaDAO.getIndicatorSpecies(Integer.parseInt(request.getParameter(PARAM_SPECIES_ID)));
            } catch (NumberFormatException nfe) {
                species = null;
            }
            
            getRequestContext().setSessionAttribute(MV_ERROR_MAP, res.getErrorMap());
            getRequestContext().setSessionAttribute(MV_VALUE_MAP, valueMap);
            getRequestContext().setSessionAttribute(MV_WKT, request.getParameter(PARAM_WKT));
            
            String redirectURL = request.getRequestURI().replace(ContentService.getContextPath(request.getRequestURL().toString()), "");
            ModelAndView mv = new ModelAndView(new PortalRedirectView(redirectURL, true));
            mv.addObject(MV_ERROR_MAP, res.getErrorMap());

            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, surveyPk);
            mv.addObject(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, Integer.valueOf(censusMethodId));
            if(species != null) {
                mv.addObject("taxonSearch", species.getScientificName());
            }
            String recordId = request.getParameter(PARAM_RECORD_ID);
            if(recordId != null && !recordId.isEmpty()) {
                mv.addObject(PARAM_RECORD_ID, Integer.parseInt(recordId));
            }
            getRequestContext().addMessage("form.validation");
            // add the edit parameter on error so the form will remain editable
            mv.addObject(RecordWebFormContext.PARAM_EDIT, true);
            return mv;
        }
        
        ModelAndView mv;
        
        // Tracker form has a special case : switching tabs
        if (request.getParameter(PARAM_SUBMIT_AND_SWITCH_TO_SUB_RECORD_TAB) != null) {
            // A tab change has been requested
            mv = new ModelAndView(new PortalRedirectView(
               RenderController.SURVEY_RENDER_REDIRECT_URL, true));
               mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
               mv.addObject(BdrsWebConstants.PARAM_CENSUS_METHOD_ID, Integer.valueOf(censusMethodId));
               mv.addObject("selectedTab", SELECT_TAB_SUB_RECORD);
               // record has been saved successfully by this point so
               // it must have a valid ID.
               mv.addObject(BdrsWebConstants.PARAM_RECORD_ID, res.getRecord().getId());
        } else {
            mv = RecordWebFormContext.getSubmitRedirect(request, res.getRecord());

            if (request.getParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER) != null) {
                if (StringUtils.hasLength(request.getParameter(TrackerController.PARAM_RECORD_ID))) {
                    getRequestContext().addMessage(new Message(MSG_CODE_SAVE_EXISTING_SUCCESS_ADD_ANOTHER));
                } else {
                    getRequestContext().addMessage(new Message(MSG_CODE_SAVE_SUCCESS_ADD_ANOTHER));    
                }
            } else {
                if (StringUtils.hasLength(request.getParameter(TrackerController.PARAM_RECORD_ID))) {
                    switch (survey.getFormSubmitAction()) {
                    case MY_SIGHTINGS:
                        getRequestContext().addMessage(new Message(MSG_CODE_SAVE_EXISTING_SUCCESS_MY_SIGHTINGS, new Object[]{defaultTab()}));
                        break;
                    case STAY_ON_FORM:
                        getRequestContext().addMessage(new Message(MSG_CODE_SAVE_EXISTING_SUCCESS_STAY_ON_FORM));
                        break;
                    default:
                        throw new IllegalStateException("Case not handled for : " + survey.getFormSubmitAction());
                    }
                } else {
                    switch (survey.getFormSubmitAction()) {
                    case MY_SIGHTINGS:
                        getRequestContext().addMessage(new Message(MSG_CODE_SAVE_NEW_SUCCESS_MY_SIGHTINGS, new Object[]{defaultTab()}));
                        break;
                    case STAY_ON_FORM:
                        getRequestContext().addMessage(new Message(MSG_CODE_SAVE_NEW_SUCCESS_STAY_ON_FORM));
                        break;
                        default:
                            throw new IllegalStateException("Case not handled for : " + survey.getFormSubmitAction());
                    }
                }
            }
        }
        
        RecordWebFormContext.addRecordHighlightId(mv, res.getRecord());
        return mv;
    }
    
    /**
     * Ajax handler for returning the taxon attribute table, a dynamic set of fields
     * for displaying taxon group attributes when the species field on the form is
     * changed
     * 
     * @param request - the http request object
     * @param response - the http response object
     * @param surveyPk - the survey id to create form fields with
     * @param taxonPk - the taxon id for the species to retrieve the taxon group from
     * @param recordPk - the record id to create form fields with
     * @return ModelAndView used to render the table
     */
    @RolesAllowed( {  Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN })
    @RequestMapping(value = "/bdrs/user/ajaxTrackerTaxonAttributeTable.htm", method = RequestMethod.GET)
    public ModelAndView ajaxTaxonAttributeTable(HttpServletRequest request,
                                                HttpServletResponse response,
                                                @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) int surveyPk,
                                                @RequestParam(value="taxonId", required=true) int taxonPk,
                                                @RequestParam(value=PARAM_RECORD_ID, required=false, defaultValue="0") int recordPk) {
 
        Survey survey = surveyDAO.getSurvey(surveyPk);
        IndicatorSpecies taxon = taxaDAO.getIndicatorSpecies(taxonPk);
        Record record = recordPk > 0 ? recordDAO.getRecord(recordPk) : new Record();
        
        User loggedInUser = getRequestContext().getUser();
        RecordWebFormContext context = new RecordWebFormContext(request, record, loggedInUser, survey, geoMapService);
        
        List<FormField> formFieldList = new ArrayList<FormField>();
        List<Attribute> taxonGroupAttributeList = new ArrayList<Attribute>();
        for(Attribute taxonGroupAttribute : taxon.getTaxonGroup().getAttributes()) {
            if(!taxonGroupAttribute.isTag()) {
                taxonGroupAttributeList.add(taxonGroupAttribute);
            }
        }
        // For those attribute that have a record attribute, create
        // the form field with that value pre-populated
        Attribute attr;
        FormField formField = null;
        for(AttributeValue recAttr : record.getAttributes()) {
            attr = recAttr.getAttribute();
            if(taxonGroupAttributeList.remove(attr)) {
                // its a taxon group attribute
                if (AttributeType.isCensusMethodType(attr.getType())) {
                    formField = createCensusMethodFormField(survey, record, attr, loggedInUser, TAXON_GROUP_ATTRIBUTE_PREFIX, context);
                } else {
                    formField = formFieldFactory.createRecordFormField(survey, record, attr, recAttr, TAXON_GROUP_ATTRIBUTE_PREFIX);
                }
                if (formField != null) {
                    formFieldList.add(formField);
                }
            }
        }

        // Add the remaining taxon group attributes.
        for (Attribute taxonGroupAttr : taxonGroupAttributeList) {
            if(!taxonGroupAttr.isTag()) {
                if (AttributeType.isCensusMethodType(taxonGroupAttr.getType())) {
                    formField = createCensusMethodFormField(survey, record, taxonGroupAttr, loggedInUser, TAXON_GROUP_ATTRIBUTE_PREFIX, context);
                } else {
                    formField = formFieldFactory.createRecordFormField(survey, record, taxonGroupAttr, TAXON_GROUP_ATTRIBUTE_PREFIX);
                }
                if (formField != null) {
                    formFieldList.add(formField);
                }
            }
        }

        Collections.sort(formFieldList);
        
        ModelAndView mv = new ModelAndView("formFieldListRenderer");
        mv.addObject("formFieldList", formFieldList);
        mv.addObject(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT, context);
        // this isn't an entire web form so it doesn't need the complete web form context
        mv.addObject(RecordWebFormContext.MODEL_EDIT, context.isEditable());
        return mv;
    }
    
    /**
     * Error to display when the survey ID does not return a valid survey
     * @return ModelAndView
     */
    private ModelAndView nullSurveyError() {
        getRequestContext().addMessage(NO_SURVEY_ERROR_KEY, new Object[]{defaultTab()});
        return new ModelAndView(new PortalRedirectView(redirectionService.getMySightingsUrl(null), true));
    }
    
    /**
     * Provides a single row representing a group of census method attributes. 
     * This view is typically invoked by an AJAX request.
     * 
     * @param request the browser request
     * @param response the server response
     * @param surveyId the primary key of the survey where the record shall be added.
     * @param rowIndex the row index where 0 is the first row.
     * @return 
     */
    @RequestMapping(value = "/bdrs/user/contribute/attributeRecordRow.htm", method = RequestMethod.GET)
    public ModelAndView ajaxAddAttributeRecordRow(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=false) Integer surveyId,
                                    @RequestParam(value="rowIndex", defaultValue="0") int rowIndex,
                                    @RequestParam(value="attributeId", required=true) int attributeId,
                                    @RequestParam(value="speciesId", required=false) Integer speciesId) {
        
        return ajaxGetAttributeTable(request, response, surveyId, speciesId, rowIndex, "attributeRecordRow", attributeId);
    }
    
    /**
     * Handler for duplicating a Record on a form.  This method populates the 
     * value map with the request parameters to reload on the form and calls 
     * addRecords with the valueMap added to the request context.
     * 
     * @param request the browser request
     * @param response the server response
     * @param recordId the id of the record to duplicate
     * @return the addRecord view populated with the values from the Record with id recordId
     */
    @RequestMapping(value = "/bdrs/user/contribute/duplicateRecord.htm", method = RequestMethod.POST)
    public ModelAndView duplicateRecord(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value=BdrsWebConstants.PARAM_RECORD_ID, required=true) Integer recordId) {
        
        Record record = recordDAO.getRecord(recordId);
        int surveyId = record.getSurvey().getId();
        Integer cmId = getIntFromParam(request.getParameter(PARAM_CENSUS_METHOD_ID));
        Integer speciesId = getIntFromParam(request.getParameter("speciesId"));
        Integer parentRecId = getIntFromParam(request.getParameter(PARAM_PARENT_RECORD_ID));
        
        Set<String> recIdParams = new HashSet<String>();
        
        // populate the valueMap from the request parameters which will fill out the form as if an error had occurred
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> valueMap = new HashMap<String, String>();
        for(Map.Entry<String, String[]> paramEntry : params.entrySet()) {
            String paramKey = paramEntry.getKey();
            String[] paramVal = paramEntry.getValue();
            if(paramVal != null && paramVal.length > 0) {
                if (paramKey.matches(".*attribute_\\d+_record.*")) {
                    // this is a data matrix attribute
                    // need to strip record ids from parameter name
                    paramKey = paramKey.replaceAll("record_\\d+_", "record_");
                }
                
                // add all recordId parameters to the list of ones to convert to value 0
                if (paramKey.endsWith("recordId")) {
                    recIdParams.add(paramKey);
                }
                
                if (paramKey.endsWith("_rowPrefix")) {
                    // remove the record id from the row prefix
                    for (int i = 0; i < paramVal.length; i++) {
                        paramVal[i] = paramVal[i].replaceAll("attribute_\\d+_record_\\d+_", "");
                    }
                }
                
                if (paramVal.length == 1) {
                    valueMap.put(paramKey, paramVal[0]);
                } else {
                    // Not bothering with a csv writer here because the
                    // jsp template does a simple String.contains to check
                    // if the the multi select or multi combo should be picked.
                    StringBuilder b = new StringBuilder();
                    for(int q = 0; q<paramVal.length; q++) {
                        b.append(paramVal[q]);
                        b.append(',');
                    }
                    valueMap.put(paramKey, b.toString());
                }
            }
        }
        
        for (String string : recIdParams) {
            valueMap.put(string, "0");
        }
        
        // remove the record id from the valueMap
        valueMap.remove(PARAM_RECORD_ID);
        
        getRequestContext().setSessionAttribute(MV_VALUE_MAP, valueMap);
        
        return addRecord(request, response, surveyId, 
                         request.getParameter("taxonSearch"),
                         0,
                         request.getParameter("guid"),
                         cmId == null ? 0 : cmId,
                         speciesId == null ? 0 : speciesId,
                         parentRecId,
                         request.getParameter(PARAM_SELECTED_TAB));
    }

    /**
     * Helper method to safely get an Integer from a String or null if the 
     * String is not an Integer.
     * @param parameter the String to parse to an Integer
     * @return an Integer representation of the String or null if it cannot be parsed
     */
    private Integer getIntFromParam(String parameter) {
        Integer intVal = null;
        try {
            intVal = Integer.parseInt(parameter);
        } catch (NumberFormatException e) {
            // if the number cannot be parsed, it is a null parameter value
        }
        return intVal;
    }
}
