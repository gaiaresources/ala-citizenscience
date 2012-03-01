package au.com.gaiaresources.bdrs.controller.record;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.gaiaresources.bdrs.model.record.RecordService;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormFieldFactory;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.RecordDeserializer;
import au.com.gaiaresources.bdrs.deserialization.record.RecordDeserializerResult;
import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;
import au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationNameComparator;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedRecordFilter;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.content.ContentService;
import au.com.gaiaresources.bdrs.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The <code>SingleSiteMultiTaxa</code> controller is a record add form renderer
 * that allows multiple sightings of differing taxa to be created for a single
 * location.
 * 
 * @author benk
 */

public abstract class SingleSiteController extends AbstractController {

    private static final RecordPropertyType[] TAXA_RECORD_PROPERTY_NAMES = new RecordPropertyType[] {
            RecordPropertyType.SPECIES, RecordPropertyType.NUMBER };

    public static final String PREFIX_TEMPLATE = "%d_";
    public static final String PARAM_ROW_PREFIX = "rowPrefix";

    public static final String PARAM_RECORD_ID = "recordId";
    public static final String PARAM_SURVEY_ID = "surveyId";
    public static final String PARAM_CENSUS_METHOD_ID = "censusMethodId";
    public static final String PARAM_ACCURACY = "accuracyInMeters";
    
    public static final String PARAM_SPECIES = "species";
    public static final String PARAM_NUMBER = "number";
    
    public static final String PARAM_LATITUDE = "latitude";
    public static final String PARAM_LONGITUDE = "longitude";
    public static final String PARAM_DATE = "date";
    public static final String PARAM_TIME_HOUR = "time_hour";
    public static final String PARAM_TIME_MINUTE = "time_minute";
    public static final String PARAM_NOTES = "notes";
    public static final String PARAM_LOCATION = "location";
    
    public static final String PARAM_SIGHTING_INDEX = "sightingIndex";
    
    private static final int STARTING_SIGHTING_INDEX = 0;
    
    public static final String ROW_VIEW = "singleSiteMultiTaxaRow";
    
    public static final String MSG_CODE_SUCCESS_MY_SIGHTINGS = "bdrs.record.singlesitemultitaxa.save.success.mySightings";
    public static final String MSG_CODE_SUCCESS_STAY_ON_FORM = "bdrs.record.singlesitemultitaxa.save.success.stayOnForm";
    public static final String MSG_CODE_SUCCESS_ADD_ANOTHER = "bdrs.record.singlesitemultitaxa.save.success.addAnother";
    
    /**
     * The survey scoped form fields that may be populated with attribute
     * value data.
     */
    public static final String MODEL_SURVEY_FORM_FIELD_LIST = "formFieldList";
    
    /**
     * The list of record form field collection objects that may be populated with
     * attribute value data,.
     */
    public static final String MODEL_RECORD_ROW_LIST = "recordFieldCollectionList";
    
    /**
     * The form fields used to create the header of the sightings table
     */
    public static final String MODEL_SIGHTING_ROW_LIST = "sightingRowFormFieldList";
    
    /**
     * The record object used to populate the form fields.
     */
    public static final String MODEL_RECORD = "record";

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private CensusMethodDAO cmDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private RecordService recordService;

    @Autowired
    private LocationService locationService;
    private FormFieldFactory formFieldFactory = new FormFieldFactory();

    private Logger log = Logger.getLogger(getClass());

    /**
     * Saves multiple records for a single site. Site information that is used
     * for all records are specified via parameters. Record specific data such
     * as the sighted taxon will be retrieved from the request parameters.
     * Record specific parameters are prefixed by the row index
     * 
     * @param request
     *            the browser request
     * @param response
     *            the server response
     * @param surveyId
     *            the primary key of the survey where the record shall be added
     * @param latitude
     *            the latitude of the sighting
     * @param longitude
     *            the longitude of the sighting
     * @param date
     *            the calendar date of the sighting
     * @param time
     *            the time when the sighting occurred
     * @param notes
     *            additional notes to be attached to all records
     * @param sightingIndex
     *            the number of records to be saved.
     * @return
     * @throws ParseException
     *             throws if the date cannot be parsed
     * @throws IOException
     *             thrown if uploaded files cannot be saved
     */
    protected ModelAndView saveRecordHelper(
            MultipartHttpServletRequest request, HttpServletResponse response,
            int surveyId, Double latitude, Double longitude, Date date,
            String time_hour, String time_minute, String notes, String[] rowIds) throws ParseException, IOException {

        Map<String, String[]> paramMap = this.getModifiableParameterMap(request);

        Survey survey = surveyDAO.getSurvey(surveyId);
        User user = getRequestContext().getUser();
        
        RecordKeyLookup lookup = new TrackerFormRecordKeyLookup();
        SingleSiteFormToRecordEntryTransformer transformer = new SingleSiteFormToRecordEntryTransformer(locationService);
        SingleSiteFormAttributeDictionaryFactory adf = new SingleSiteFormAttributeDictionaryFactory(rowIds);
        AttributeParser parser = new WebFormAttributeParser();

        RecordDeserializer rds = new RecordDeserializer(lookup, adf, parser);
        List<RecordEntry> entries = transformer.httpRequestParamToRecordMap(paramMap, request.getFileMap(), rowIds);
        List<RecordDeserializerResult> results = rds.deserialize(getRequestContext().getUser(), entries);
        
        // there should be at least one result
        if (results.size() != rowIds.length) {
            log.warn("Expecting "+rowIds.length+" deserialization results but got: " + results.size());
            return getErrorRedirect(survey, user, null, request, results);
        }
        for (RecordDeserializerResult res : results) {
            if (!res.isAuthorizedAccess()) {
                // Required since there will be an auto commit otherwise at the end of controller handling.
                requestRollback(request);
                throw new AccessDeniedException(RecordWebFormContext.MSG_CODE_EDIT_AUTHFAIL);
            }
            
            if (!res.getErrorMap().isEmpty()) {
                return getErrorRedirect(survey, user, res.getRecord(), request, results);
            }
        }
        
        // if we get to this point, clear the last errors out of the session attributes
        // because this is successful and the errors/values might still be present in the maps
        getRequestContext().removeSessionAttribute(TrackerController.MV_ERROR_MAP);
        getRequestContext().removeSessionAttribute("valueMap");
        getRequestContext().removeSessionAttribute(TrackerController.MV_WKT);
        
        ModelAndView mv = RecordWebFormContext.getSubmitRedirect(request, results.get(0).getRecord());

        if (request.getParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER) != null) {
            mv.addObject("surveyId", survey.getId());
            getRequestContext().addMessage(MSG_CODE_SUCCESS_ADD_ANOTHER, new Object[] { results.size() });
        } else {
            switch (survey.getFormSubmitAction()) {
            case MY_SIGHTINGS:
                getRequestContext().addMessage(MSG_CODE_SUCCESS_MY_SIGHTINGS, new Object[] { results.size() });
                break;
            case STAY_ON_FORM:
                getRequestContext().addMessage(MSG_CODE_SUCCESS_STAY_ON_FORM, new Object[] { results.size() });
                break;
            default:
                throw new IllegalStateException("Submit form action not handled : " + survey.getFormSubmitAction());
            }
        }
        return mv;
    }

    private ModelAndView getErrorRedirect(Survey survey, User accessor, Record record, MultipartHttpServletRequest request, List<RecordDeserializerResult> results) {
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
        // strip the context path out of the URL
        String redirectURL = request.getRequestURI().replace(ContentService.getContextPath(request.getRequestURL().toString()), "");
        ModelAndView mv = new ModelAndView(new RedirectView(redirectURL, true));
        Map<String, String> errorMap = new HashMap<String, String>();
        for (RecordDeserializerResult result : results) {
            errorMap.putAll(result.getErrorMap());
        }
        
        getRequestContext().setSessionAttribute(TrackerController.MV_ERROR_MAP, errorMap);
        getRequestContext().setSessionAttribute("valueMap", valueMap);
        getRequestContext().setSessionAttribute(TrackerController.MV_WKT, request.getParameter(TrackerController.PARAM_WKT));
        
        mv.addObject("surveyId", request.getParameter("surveyId"));
        mv.addObject("censusMethodId", request.getParameter("censusMethodId"));
        
        String recordId = request.getParameter(PARAM_RECORD_ID);
        if(recordId != null && !recordId.isEmpty()) {
            mv.addObject(PARAM_RECORD_ID, Integer.parseInt(recordId));
        }
        getRequestContext().addMessage("form.validation");
        return mv;
    }

    /**
     * Determines if we can save a record based on the value of the count field.
     * @param number the count field for the record
     * @return true if the record can be saved, false otherwise
     */
    protected boolean canSaveRecord(Integer number) {
        return true;
    }

    @InitBinder
    public void initBinder(HttpServletRequest request,
            ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }

    protected ModelAndView ajaxGetSightingsTable(HttpServletRequest request,
            HttpServletResponse response, int surveyId, int sightingIndex) {
        Survey survey = surveyDAO.getSurvey(surveyId);
        Record record = new Record();

        // Add survey scope attribute form fields
        String prefix = getSightingPrefix(sightingIndex);
        List<FormField> formFieldList = new ArrayList<FormField>();
        for (Attribute attribute : survey.getAttributes()) {
            // Only record scoped attributes should be in the sightings table.
            if (AttributeScope.isRecordScope(attribute.getScope()) 
                    && !attribute.isTag()) {
                formFieldList.add(formFieldFactory.createRecordFormField(survey, record, attribute, null, prefix));
            }
        }
        // Add all property form fields
        for (RecordPropertyType type : TAXA_RECORD_PROPERTY_NAMES) {
            RecordProperty recordProperty = new RecordProperty(survey, type,
                    metadataDAO);
            formFieldList.add(formFieldFactory.createRecordFormField(record, recordProperty, null, null, prefix));
        }
        Collections.sort(formFieldList);

        ModelAndView mv = new ModelAndView(ROW_VIEW);
        mv.addObject("record", record);
        mv.addObject("survey", survey);
        mv.addObject("formFieldList", formFieldList);
        mv.addObject("sightingIndex", prefix);
        // by definition editing must be enabled for items to be added to the
        // sightings table.
        mv.addObject(RecordWebFormContext.MODEL_EDIT, true);
        return mv;
    }

    /**
     * This is used by subclasses in the GET handler
     * 
     * @param request
     * @param response
     * @param surveyId
     * @param viewName
     * @param censusMethodId
     * @return
     */
    protected ModelAndView addRecord(HttpServletRequest request,
            HttpServletResponse response, int surveyId, String viewName,
            Integer censusMethodId) {
        Survey survey = surveyDAO.getSurvey(surveyId);
        Record record = null;
        CensusMethod censusMethod = null;
        if (request.getParameter(PARAM_RECORD_ID) != null
                && !request.getParameter(PARAM_RECORD_ID).isEmpty()) {
            try {
                record = recordDAO.getRecord(Integer.parseInt(request.getParameter(PARAM_RECORD_ID)));
                censusMethod = record.getCensusMethod();
            } catch (NumberFormatException nfe) {
                record = new Record();
                // Set record visibility to survey default. Setting via web form not supported.
                // Survey's default record visibility can be set in the 'admin -> projects' interface
                record.setRecordVisibility(survey.getDefaultRecordVisibility());
            }
        } else {
            record = new Record();
            censusMethod = cmDAO.get(censusMethodId);
            // Set record visibility to survey default. Setting via web form not supported.
            // Survey's default record visibility can be set in the 'admin -> projects' interface
            record.setRecordVisibility(survey.getDefaultRecordVisibility());
        }
        
        User accessor = getRequestContext().getUser();
        
        RecordWebFormContext context = new RecordWebFormContext(request, record, accessor, survey);
        
        // get the records for this form instance (if any)
        List<Record> recordsForFormInstance = getRecordsForFormInstance(record, accessor);
        
        // Add survey scope attribute form fields
        List<FormField> sightingRowFormFieldList = new ArrayList<FormField>();
        List<FormField> formFieldList = new ArrayList<FormField>();
        
        // save a list of the record scoped attributes for construction of form fields for each
        // record (aka sighting table row) later...
        Map<String, Attribute> recordScopedAttributeList = new TreeMap<String, Attribute>();
        int sightingIndex = STARTING_SIGHTING_INDEX;
        
        // the final list of populated form field collections that we will use to render the web form.
        List<RecordFormFieldCollection> recFormFieldCollectionList = new ArrayList<RecordFormFieldCollection>();
        
        // Add all property form fields.
        // save a list of the record scoped record properties for construction of form fields
        // for each record (aka sighting table row) later...
        Map<String, RecordProperty> recordScopedRecordPropertyList = new TreeMap<String, RecordProperty>();
        boolean showMap = createAttributeLists(survey, accessor, record, sightingRowFormFieldList, formFieldList, recordScopedAttributeList, recordScopedRecordPropertyList);

        Metadata predefinedLocationsMD = survey.getMetadataByKey(Metadata.PREDEFINED_LOCATIONS_ONLY);
        boolean predefinedLocationsOnly = predefinedLocationsMD != null
                && Boolean.parseBoolean(predefinedLocationsMD.getValue());

        Set<Location> locations = new TreeSet<Location>(
                new LocationNameComparator());
        locations.addAll(survey.getLocations());
        if (!predefinedLocationsOnly) {
            locations.addAll(locationDAO.getUserLocations(getRequestContext().getUser()));
        }
        ModelAndView mv = new ModelAndView(viewName);
        
        
        // modify the list
        // note we need to reassign as it is a new list instance...
        recordsForFormInstance = modifyRecordDisplayList(recordsForFormInstance, survey);
        
        for (Record rec : recordsForFormInstance) {
            boolean highlight = rec.equals(record);
            String prefix = getSightingPrefix(sightingIndex++);
            
            RecordFormFieldCollection rffc = new RecordFormFieldCollection(prefix, 
                                                                           rec, 
                                                                           highlight, 
                                                                           recordScopedRecordPropertyList.values(),
                                                                           recordScopedAttributeList.values());
            
            recFormFieldCollectionList.add(rffc);
        }

        Map<String, String> valueMap = (Map<String, String>) getRequestContext().getSessionAttribute("valueMap");
        if (valueMap != null) {
            // convert the valueMap into a set of records
            Map<String, Record> recordMap = new HashMap<String, Record>();
            // keep track of all the values we are creating records from
            // these values will become record form fields and the form values will
            // be retrieved from those objects instead of the form field to keep 
            // the rows intact
            Set<String> valsToRemove = new HashSet<String>();
            for (Entry<String,String> valueEntry : valueMap.entrySet()) {
                // only store indexed values here
                String key = valueEntry.getKey();
                int underIndex = key.indexOf("_");
                if (underIndex > 0) {
                    // get the prefix of the value (the sighting index)
                    String index = key.substring(0, underIndex);
                    if (recordMap.containsKey(index)) {
                        record = recordMap.get(index);
                    } else {
                        record = new Record();
                        recordMap.put(index, record);
                    }
                    // set the survey for the record for other lookups
                    record.setSurvey(survey);
                    
                    // set the attribute/property value for the record for this value
                    String attrName = key.substring(underIndex+1);
                    if (recordScopedAttributeList.containsKey(attrName)) {
                        AttributeValue value = new AttributeValue();
                        value.setAttribute(recordScopedAttributeList.get(attrName));
                        value.setStringValue(valueEntry.getValue());
                        try {
                            value.populateFromStringValue();
                        } catch (Exception e) {
                            log.warn("Unable to populate attribute value from string value.", e);
                        }
                        record.getAttributes().add(value);
                        // remove the attribute from the value map
                        valsToRemove.add(key);
                    } else if (recordScopedRecordPropertyList.containsKey(attrName)) {
                        RecordProperty prop = recordScopedRecordPropertyList.get(attrName);
                        if (prop.getRecordPropertyType().equals(RecordPropertyType.NUMBER)) {
                            record.setNumber(Integer.valueOf(valueEntry.getValue()));
                        } else if (prop.getRecordPropertyType().equals(RecordPropertyType.SPECIES)) {
                            // get the indicator species from the value
                            IndicatorSpecies species = taxaDAO.getIndicatorSpecies(Integer.valueOf(valueEntry.getValue()));
                            record.setSpecies(species);
                        }
                        // remove the attribute from the value map
                        valsToRemove.add(key);
                    } else if (attrName.equals("recordId")) {
                        if (!StringUtils.nullOrEmpty(valueEntry.getValue())) {
                            record.setId(Integer.valueOf(valueEntry.getValue()));
                        }
                        // remove the attribute from the value map
                        valsToRemove.add(key);
                    }
                }
            }
            
            for (String string : valsToRemove) {
                valueMap.remove(string);
            }
            
            for (Record record2 : recordMap.values()) {
                    String prefix = getSightingPrefix(sightingIndex++);
                    RecordFormFieldCollection rffc = new RecordFormFieldCollection(prefix, 
                                                                               record2, 
                                                                               false, 
                                                                               recordScopedRecordPropertyList.values(),
                                                                               recordScopedAttributeList.values());
                    recFormFieldCollectionList.add(rffc);
            }
        }

        User updatedByUser = recordService.getUpdatedByUser(record);
        mv.addObject("updatedBy", updatedByUser);
        // form field list is the survey scoped attributes.
        // contains the form field and the data (optional).
        // note: NON record scoped attributes only!
        mv.addObject(MODEL_SURVEY_FORM_FIELD_LIST, formFieldList);
        
        // sightings row form field list is the record scoped attributes
        // this is used to create the sightings table header row - no values!
        // note: record scoped attributes only!
        mv.addObject(MODEL_SIGHTING_ROW_LIST, sightingRowFormFieldList);
        
        // form field collections used to poplate the body of the sightings
        // table. i.e., data is in here!
        // note: record scoped attributes only!
        mv.addObject(MODEL_RECORD_ROW_LIST, recFormFieldCollectionList);
        
        mv.addObject(MODEL_RECORD, record);
        
        mv.addObject("survey", survey);
        mv.addObject("locations", locations);
        mv.addObject("preview", request.getParameter("preview") != null);
        mv.addObject("censusMethod", censusMethod);
        mv.addObject(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT, context);
        if (accessor != null) {
        	mv.addObject("ident", accessor.getRegistrationKey());
        }
        mv.addObject("displayMap", showMap);
        
        return mv;
    }

    /**
     * Populates the sightingRowFormFieldList with form fields to build the headers of the table,
     * the formFieldList with form fields for each row of the table, 
     * the recordScopedAttributeList with record scoped attributes to show in the table,
     * and the recordScopedRecordPropertyList with record properties to show in the table.
     * Returns whether or not to show the map on the form based on if location or point are present.
     * @param survey the survey for the form
     * @param accessor the user saving the form
     * @param record the record we are creating the lists from
     * @param sightingRowFormFieldList the list of form fields to show in a row on the table
     * @param formFieldList the list of form fields to show on the form
     * @param recordScopedAttributeList the record attributes to show in the table
     * @param recordScopedRecordPropertyList the record properties to show in the table
     * @return true if the map should show on the form and false otherwise
     */
    private boolean createAttributeLists(Survey survey, User accessor, Record record, 
            List<FormField> sightingRowFormFieldList,
            List<FormField> formFieldList,
            Map<String, Attribute> recordScopedAttributeList,
            Map<String, RecordProperty> recordScopedRecordPropertyList) {
        for (Attribute attribute : survey.getAttributes()) {
            if (!attribute.isTag()
                    && !AttributeScope.LOCATION.equals(attribute.getScope())) {
                AttributeValue attrVal = record == null ? null : AttributeValueUtil.getAttributeValue(attribute, record);
                if (AttributeScope.isSurveyScope(attribute.getScope())) {
                    formFieldList.add(formFieldFactory.createRecordFormField(survey, record, attribute, attrVal));
                } else if (AttributeScope.isRecordScope(attribute.getScope())) {
                    recordScopedAttributeList.put(String.format(AttributeParser.ATTRIBUTE_NAME_TEMPLATE, "", attribute.getId()), attribute);
                    sightingRowFormFieldList.add(formFieldFactory.createRecordFormField(survey, record, attribute));
                }
            }
        }
        
        boolean showMap = false;
        for (RecordPropertyType type : RecordPropertyType.values()) {
            
            RecordProperty recordProperty = new RecordProperty(survey, type,
                    metadataDAO);
            
            //showMap if location or point fields are hidden.
            if(!showMap && (type.equals(RecordPropertyType.LOCATION) || type.equals(RecordPropertyType.POINT))){
                showMap = !recordProperty.isHidden();
            }
            
            if (!recordProperty.isHidden()) {
                if (recordProperty.getScope().equals(AttributeScope.SURVEY) || 
                        AttributeScope.SURVEY_MODERATION.equals(recordProperty.getScope())) {
                    formFieldList.add(formFieldFactory.createRecordFormField(record, recordProperty));
                } else {
                    recordScopedRecordPropertyList.put(recordProperty.getName().toLowerCase(), recordProperty);
                    sightingRowFormFieldList.add(formFieldFactory.createRecordFormField(record, recordProperty));
                }
            }
        }

        Collections.sort(formFieldList);
        Collections.sort(sightingRowFormFieldList);
        
        return showMap;
    }

    private List<Record> getRecordsForFormInstance(Record rec, User accessor) {
        
        // early return if the record is a non persisted instance - i.e. this will return
        // an empty form.
        if (rec.getId() == null) {
            return Collections.emptyList();
        }
        
        AdvancedRecordFilter recFilter = new AdvancedRecordFilter();
        recFilter.setStartDate(rec.getWhen());
        recFilter.setEndDate(rec.getWhen());
        recFilter.setSurveyPk(rec.getSurvey().getId().intValue());
        recFilter.setUser(rec.getUser());
        recFilter.setAccessor(accessor);
        
        ScrollableRecords scrollableRecords = recordDAO.getScrollableRecords(recFilter);
        
        List<Record> result = new ArrayList<Record>();

        while (scrollableRecords.hasMoreElements()) {
            Record recordUnderTest = scrollableRecords.nextElement();
            
            // Make sure the geometry is the same. I'm not sure whether it's better to
            // do this in the database or not. I'll consider that an optimization
            // and will look into it if performance becomes an issue.
            Geometry geomA = rec.getGeometry();
            Geometry geomB = recordUnderTest.getGeometry();
            if (geomA != null && geomB == null) {
                // early continue, records cannot be from the same form instance
                continue;
            }
            if (geomA == null && geomB != null) {
                // early continue, records cannot be from the same form instance
                continue;
            }
            // finally, check for both non null and whether the geometries are the same
            if ((geomA != null && geomB != null) && !geomA.equalsExact(geomB)) {
                // early continue, records cannot be from the same form instance
                continue;
            }
            
            // make sure all survey scoped attributes are the same...
            
            // if even 1 survey scoped attribute value has been deemed to be different, the record will
            // not be accepted.
            boolean identicalSurveyScopedAttributeValues = true;
            
            for (AttributeValue av : rec.getAttributes()) {
                // we are only concerned about survey scoped attributes.
                // not sure if we need to consider location scoped attributes or not.
                // record attributes should definitely NOT be considered here.
                if (AttributeScope.SURVEY.equals(av.getAttribute().getScope())  || 
                        AttributeScope.SURVEY_MODERATION.equals(av.getAttribute().getScope())) {
                    AttributeValue avToTest = AttributeValueUtil.getAttributeValue(av.getAttribute(), recordUnderTest);
                    
                    if (avToTest == null) {
                        // early loop continue - item not added to final result
                        identicalSurveyScopedAttributeValues = false;
                        break;
                    }
                    if (!AttributeValueUtil.isAttributeValuesEqual(av, avToTest)) {
                        identicalSurveyScopedAttributeValues = false;
                        break;
                    }
                }
            }
            
            // record has been deemed to be part of the same single site form instance.
            // add to the result.
            if (identicalSurveyScopedAttributeValues) {
                result.add(recordUnderTest);
            }
        }
        
        return result;
    }
    
    
    private static String getSightingPrefix(int sightingIndex) {
        return String.format(PREFIX_TEMPLATE, sightingIndex);
    }
    
    /**
     * Overridable method that we can use to alter what items are displayed in the form instance.
     * 
     * Original intent was to allow SingleSiteAllTaxa form to have non persisted records that
     * contained species that weren't part of the recordsForFormInstance list.
     * 
     * @param recordsForFormInstance - the records determined by SingleSiteController that belong to the form instance
     * @param survey - The survey for the records
     * @return List<Record>, a new list instance with an updated record list
     */
    protected List<Record> modifyRecordDisplayList(List<Record> recordsForFormInstance, Survey survey) {
        return recordsForFormInstance;
    }
}