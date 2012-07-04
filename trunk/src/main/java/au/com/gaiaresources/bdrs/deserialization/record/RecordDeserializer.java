package au.com.gaiaresources.bdrs.deserialization.record;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.controller.record.RecordFormValidator;
import au.com.gaiaresources.bdrs.controller.record.TrackerController;
import au.com.gaiaresources.bdrs.controller.record.ValidationType;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.deserialization.attribute.AttributeDeserializer;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeUtil;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.util.DateFormatter;
import au.com.gaiaresources.bdrs.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

import edu.emory.mathcs.backport.java.util.Arrays;

public class RecordDeserializer {

    private RecordDAO recordDAO = AppContext.getBean(RecordDAO.class);
    private AttributeDAO attributeDAO = AppContext.getBean(AttributeDAO.class);
    private SurveyDAO surveyDAO = AppContext.getBean(SurveyDAO.class);
    private TaxaDAO taxaDAO = AppContext.getBean(TaxaDAO.class);
    private LocationDAO locationDAO = AppContext.getBean(LocationDAO.class);
    private CensusMethodDAO cmDAO = AppContext.getBean(CensusMethodDAO.class);
    private LocationService locationService = AppContext.getBean(LocationService.class);
    private PropertyService propertyService = AppContext.getBean(PropertyService.class);
    private MetadataDAO metadataDAO = AppContext.getBean(MetadataDAO.class);
    private SessionFactory sessionFactory = AppContext.getBean(SessionFactory.class);
    
    RecordKeyLookup klu;
    AttributeParser attributeParser;
    AttributeDictionaryFactory attrDictFact;
    
    Logger log = Logger.getLogger(getClass());
    
    public static final String TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE_KEY = "Tracker.TaxonAndNumberRequiredTogether";
    public static final String TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE = "Species and number must both be blank, or both filled in.";
    
    public static final String TAXON_NOT_IN_SURVEY_KEY = "Tracker.TaxonNotValidForSurvey";
    public static final String TAXON_NOT_IN_SURVEY_KEY_DEFAULT_MESSAGE = "This species is not valid for the survey.";
    
    public static final String GEOM_INVALID_KEY = "Tracker.GeometryInvalid";
    public static final String GEOM_INVALID_DEFAULT_MESSAGE = "The geometry is invalid. Self intersecting polygons are not allowed";
    
    /**
     * Create a new record deserializer.
     * 
     * RecordDeserializer is intended to provide agnostic parameter map parsing via the implementations of the interfaces passed into
     * the ctor.
     * 
     * @param recKeyLookup
     * @param attributeDictionaryFactory
     * @param parser
     */
    public RecordDeserializer(RecordKeyLookup recKeyLookup, AttributeDictionaryFactory attributeDictionaryFactory, AttributeParser parser) {
        if (recKeyLookup == null) {
            throw new IllegalArgumentException("arg recKeyLookup cannot be null");
        }
        if (attributeDictionaryFactory == null) {
            throw new IllegalArgumentException("arg attrDictFact cannot be null");
        }
        if (parser == null) {
            throw new IllegalArgumentException("arg parser cannot be null");
        }
        this.attrDictFact = attributeDictionaryFactory;
        klu = recKeyLookup;
        attributeParser = parser;
    }

    /**
     * Does a few things:
     * 1. Parse recordEntry param map for record values
     * 2. Validate and create error maps if appropriate
     * 3. Persist records
     * 
     * @param currentUser - the user that the records will be attributed to
     * @param entries - RecordEntry objects - similar to a form backing object but more generic
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public List<RecordDeserializerResult> deserialize(User currentUser, List<RecordEntry> entries) throws ParseException, IOException {
        return deserialize(currentUser, entries, true, true);
    }
    
    /**
     * Does a few things:
     * 1. Parse recordEntry param map for record values
     * 2. Validate and create error maps if appropriate
     * 3. Persist records if appropriate
     * 
     * This override allows for the deserialization of records without saving or validating
     * allowing this method to be reused for error form population
     * 
     * @param currentUser - the user that the records will be attributed to
     * @param entries - RecordEntry objects - similar to a form backing object but more generic
     * @param validate - boolean flag indicating whether or not to run the validation
     * @param save - boolean flag indicating whether or not to save the records
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public List<RecordDeserializerResult> deserialize(User currentUser, List<RecordEntry> entries, boolean validate, boolean save) throws ParseException, IOException {
        List<RecordDeserializerResult> results = new LinkedList<RecordDeserializerResult>();

        // disable the partial record filter to allow records for attribute values to be retrieved
        FilterManager.disablePartialRecordCountFilter(sessionFactory.getCurrentSession());
        try {
            for (RecordEntry entry : entries) {
                
                RecordDeserializerResult rsResult = new RecordDeserializerResult(entry);
                results.add(rsResult);
                
                Map<String, String[]> dataMap = entry.getDataMap();
                
                Integer surveyPk = Integer.parseInt(entry.getValue(klu.getSurveyIdKey()));
                
                String censusMethodIdString = entry.getValue(klu.getCensusMethodIdKey());
                
                censusMethodIdString = censusMethodIdString == null ? "0" : censusMethodIdString; 
                Integer censusMethodId = Integer.parseInt(censusMethodIdString);
                
                Survey survey = surveyDAO.getSurvey(surveyPk);
                CensusMethod censusMethod = cmDAO.get(censusMethodId);
                
                // Get the record here so we can first check authorization without doing all of the 
                // other checks...
                Record record;
                // use the prefix in case it is a single site form
                // all other forms will have a default prefix of ""
                String recordId = entry.getValue(entry.prefix + klu.getRecordIdKey());
                if (recordId != null && !recordId.isEmpty()) {
                    int recordIdInt;
                    try {
                        recordIdInt = Integer.parseInt(recordId);
                    } catch (NumberFormatException nfe) {
                        setErrorMessage(rsResult, "Invalid record ID", "Record id: " + recordId + " is not a valid integer. If you want to create a new record indicate '0' as the record ID");
                        continue;
                    }
                    
                    // Unfortunately in shapefiles when you leave an integer field blank it becomes 
                    // a '0'. If we detect such a record_id we will create a new record.
                    if (recordIdInt == 0) {
                        record = createNewRecord(entry, klu);
                    } else {
                        record = recordDAO.getRecord(recordIdInt);
                        
                        // we are attempting to edit an existing record but the record
                        // query has returned null. Error!
                        if (record == null) {
                            setErrorMessage(rsResult, "Record retrieval failure", "Record id: " + recordId + " is not an existing record.");
                            // don't do any further processing for this record
                            continue;
                        }
                    }
                } else {
                    record = createNewRecord(entry, klu);
                }
    
                // set the survey for the record here because it is used for 
                // some of the checking mechanisms before the final save
                record.setSurvey(survey);
                
                // set the record here so we have a copy to use even in the event of an error
                rsResult.setRecord(record);
                
                // check authorization!
                if (!record.canWrite(currentUser)) {
                    // failed, do reporting.
                    rsResult.setAuthorizedAccess(false);
                    setErrorMessage(rsResult, "Authorization failure", "You do not have authorization to edit this record");
                    // don't do any further processing for this record
                    continue;
                }
            
                Taxonomic taxonomic;
                if(censusMethod != null && censusMethod.getTaxonomic() != null) {
                        taxonomic = censusMethod.getTaxonomic();
                }
                else {
                        taxonomic = Taxonomic.OPTIONALLYTAXONOMIC;
                }
                
                // If taxon is denoted by numeric ID, then use it!
                IndicatorSpecies species;
               
                try {
                    // use the prefix as it is necessary for single site forms
                    species = taxaDAO.getIndicatorSpecies(Integer.parseInt(entry.getValue(entry.prefix+klu.getSpeciesIdKey())));
                } catch (NumberFormatException nfe) {
                    species = null;
                }
                String speciesSearch = entry.getValue(entry.prefix+klu.getSpeciesNameKey());
                if (species == null && StringUtils.notEmpty(speciesSearch)) {
                	species = this.getSpeciesFromName(speciesSearch);
                }
            
                // if we are doing moderation only, it is not necessary to validate all of the fields, only the moderation ones
                boolean moderationOnly = record.getId() != null && !currentUser.equals(record.getUser()) &&
                                         !currentUser.isAdmin() && currentUser.isModerator() && 
                                         record.isAtLeastOneModerationAttribute();
    
                boolean isTaxonomicRecord = Taxonomic.TAXONOMIC.equals(taxonomic) || Taxonomic.OPTIONALLYTAXONOMIC.equals(taxonomic);
                // use the prefix as it is necessary for single site forms
            
                String numberString = entry.getValue(entry.prefix+klu.getIndividualCountKey());
                
                RecordProperty recordProperty;
                RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, survey);
                boolean isValid = false;
                Map<String, String[]> params = dataMap;
                Map<String, String[]> dateRangeParams = new HashMap<String, String[]>(params);
                dateRangeParams.put("dateRange", 
                           new String[] {
                                survey.getStartDate() == null ? "" : DateFormatter.format(survey.getStartDate(), DateFormatter.DAY_MONTH_YEAR),
                                survey.getEndDate() == null ? "" : DateFormatter.format(survey.getEndDate(), DateFormatter.DAY_MONTH_YEAR) } );
                TaxonGroup taxonGroup = null;
                // don't validate when we are only moderating or if we have explicitly said not to
                if (validate) {
                    if (!moderationOnly) {
                        // Validate darwin core fields
                        recordProperty = new RecordProperty(survey, RecordPropertyType.NOTES, metadataDAO);
                        if ( recordProperty.isRequired()) {
                            validator.validate(params, ValidationType.REQUIRED_BLANKABLE_STRING, klu.getNotesKey(), null, recordProperty);
                        } else {
                            validator.validate(params, ValidationType.STRING, klu.getNotesKey(), null, recordProperty);
                        }
                        
                        recordProperty = new RecordProperty(survey, RecordPropertyType.WHEN, metadataDAO);
                        if(recordProperty.isRequired()) {
                            validator.validate(params, ValidationType.REQUIRED_HISTORICAL_DATE, klu.getDateKey(), null, recordProperty);
                            validator.validate(dateRangeParams, ValidationType.REQUIRED_DATE_WITHIN_RANGE, klu.getDateKey(), null, recordProperty);
                        } else {
                            validator.validate(params, ValidationType.BLANKABLE_HISTORICAL_DATE, klu.getDateKey(), null, recordProperty);
                            validator.validate(dateRangeParams, ValidationType.DATE_WITHIN_RANGE, klu.getDateKey(), null, recordProperty);
                        }
                
                        recordProperty = new RecordProperty(survey, RecordPropertyType.ACCURACY, metadataDAO);
                        if (recordProperty.isRequired()) {
                            validator.validate(params, ValidationType.REQUIRED_DOUBLE, klu.getAccuracyKey(), null, recordProperty);
                        } else {
                            validator.validate(params, ValidationType.DOUBLE, klu.getAccuracyKey(), null, recordProperty);
                        }
                        
                        validator.validate(params, ValidationType.INTEGER, klu.getRecordIdKey(), null);
                        
                        recordProperty = new RecordProperty(survey, RecordPropertyType.TIME, metadataDAO);
                        if (recordProperty.isRequired()) {
                            validator.validate(params, ValidationType.REQUIRED_TIME, klu.getTimeKey(), null, recordProperty);
                        } else {
                            validator.validate(params, ValidationType.TIME, klu.getTimeKey(), null, recordProperty);
                        }
                                
                        if (entry.getGeometry() == null) {
                            recordProperty = new RecordProperty(survey, RecordPropertyType.POINT, metadataDAO);
                            if ( recordProperty.isRequired()) {
                                validator.validate(params, ValidationType.REQUIRED_DEG_LATITUDE, klu.getLatitudeKey(), null, recordProperty);
                                validator.validate(params, ValidationType.REQUIRED_DEG_LONGITUDE, klu.getLongitudeKey(), null, recordProperty);    
                            } else {
                                validator.validate(params, ValidationType.DEG_LATITUDE, klu.getLatitudeKey(), null, recordProperty);
                                validator.validate(params, ValidationType.DEG_LONGITUDE, klu.getLongitudeKey(), null, recordProperty); 
                            }
                        } else {
                            // make sure the geometry is valid !
                            boolean geomValid = entry.getGeometry().isValid();
                            if (!geomValid) {
                                Map<String, String> errorMap = validator.getErrorMap();
                                String errMsg = propertyService.getMessage(
                                                GEOM_INVALID_KEY, 
                                                GEOM_INVALID_DEFAULT_MESSAGE);
                                errorMap.put(klu.getLatitudeKey(), errMsg);
                                errorMap.put(klu.getLongitudeKey(), errMsg);
                            } else {
                                // attempt to do geometry conversion as we only support multiline, multipolygon and singlepoint
                                try {
                                    Geometry geom = locationService.convertToMultiGeom(entry.getGeometry());
                                    entry.setGeometry(geom);
                                } catch (IllegalArgumentException iae) {
                                    Map<String, String> errorMap = validator.getErrorMap();
                                    errorMap.put(klu.getLatitudeKey(), iae.getMessage());
                                    errorMap.put(klu.getLongitudeKey(), iae.getMessage());
                                }
                            }
                            isValid = isValid && geomValid;
                        }
        
                    }
                    else {
                        // Because the occurences/number field is not editable in moderation mode (if the moderator is
                        // not an admin), the number property won't be submitted with the form.  Hence we are faking it
                        // as species and occurances are validated as a block).
                        numberString = Integer.toString(record.getNumber());
                        params.put(klu.getIndividualCountKey(), new String[] {numberString});
                    }
                }
                taxonGroup = species != null ? species.getTaxonGroup() : null;
                Map<Attribute, Object> attrNameMap = attrDictFact.createNameKeyDictionary(record, survey, null, taxonGroup, censusMethod, entry.getDataMap());
                Map<Attribute, Object> attrFilenameMap = attrDictFact.createFileKeyDictionary(record, survey, null, taxonGroup, censusMethod, entry.getDataMap());
                AttributeDeserializer attrDeserializer = new AttributeDeserializer(attrDictFact, attributeParser);

                if (validate) {
                    isValid = validateSpeciesInformation(survey, taxonomic, species, isTaxonomicRecord, speciesSearch, numberString, validator, isValid, params);
                    // Here is the point we require our name dictionary as we are about to start validating attributes...
                    for(Attribute attr : survey.getAttributes()) {
                        if(attrDictFact.getDictionaryAttributeScope().contains(attr.getScope())) {
                            // validate all attributes when not moderating, 
                            // but only moderation attributes when moderation only
                            if (AttributeUtil.isModifiableByScopeAndUser(attr, currentUser) && (!moderationOnly || 
                                    (moderationOnly && AttributeScope.isModerationScope(attr.getScope())))) {
                                // use the entry prefix for record scoped attributes
                                // the record entry prefix will be the id for the row in the sightings table
                                // for single site record entry forms
                                String prefix = AttributeScope.isRecordScope(attr.getScope()) ? entry.prefix : "";
                                isValid = isValid & attrDeserializer.validate(validator, prefix, attrNameMap, attrFilenameMap, attr, params, entry.getFileMap());
                            }
                        }
                    }
                    if(species != null) {
                        for(Attribute attr : species.getTaxonGroup().getAttributes()) {
                            if(!attr.isTag()) {
                                isValid = isValid & attrDeserializer.validate(validator, "", attrNameMap, attrFilenameMap, attr, params, entry.getFileMap());
                            }
                        }
                    }
                    if (censusMethod != null) {
                        for (Attribute attr : censusMethod.getAttributes()) {
                            isValid = isValid & attrDeserializer.validate(validator, "", attrNameMap, attrFilenameMap, attr, params, entry.getFileMap());
                        }
                    }
                    
                    if(validator.getErrorMap().size() > 0) {
                        rsResult.setErrorMap(validator.getErrorMap());
                        // and early end of loop
                        continue;
                    }
                }
                // At this point we know that, 
                // the species primary key does not match an indicator species possibly
                // due to it not being set because javascript being disabled, however
                // the taxon validator has been run so the scientific name has been
                // entered so we can search on the name.
                
                Integer number = null;
                if (isTaxonomicRecord) {
                    if(species != null && !StringUtils.nullOrEmpty(numberString)) {
                        try {
                            number = Integer.valueOf(numberString); 
                        } catch (NumberFormatException nfe) {
                            log.warn("Record deserializer could not parse number string to an int : " + numberString);
                        }
                    }
                } else {
                    species = null;
                    number = null;
                }
                
                User user = currentUser;
    
                // if we are just moderating the record, only want to set the moderation attributes
                record.setSpecies(species);
                if (!moderationOnly) {
                    // Preserve the original owner of the record if this is a record edit.
                    if (record.getUser() == null) {
                        record.setUser(user);
                    }
                    record.setNumber(number);
                    recordProperty = new RecordProperty(survey, RecordPropertyType.NOTES, metadataDAO);
                    if (!recordProperty.isHidden()) {
                        record.setNotes(entry.getValue(klu.getNotesKey()));
                    }
                    record.setFirstAppearance(false);
                    record.setLastAppearance(false);
                    // is possible to set this to null
                    record.setCensusMethod(censusMethod);
                    
                    // attempt to parse the record visibility key. if not cannot be parsed i.e. invalid or not filled in,
                    // use the default record visibility for the survey.
                    record.setRecordVisibility(RecordVisibility.parse(entry.getValue(klu.getRecordVisibilityKey()), survey.getDefaultRecordVisibility()));
            
                    // Dates
                    String timeString = attributeParser.getTimeValue(klu.getTimeKey(), klu.getTimeHourKey(), klu.getTimeMinuteKey(), entry.getDataMap());
                    String[] timeStringSplit = timeString != null ? timeString.split(":") : null;
                    
                    // default record time is midnight.
                    Integer hour = null;
                    Integer minute = null;
                    
                    if (timeStringSplit != null && timeStringSplit.length == 2) {
                        try {
                            hour = Integer.parseInt(timeStringSplit[0]);
                        } catch(NumberFormatException nfe) {
                            
                        }
                        try {
                            minute = Integer.parseInt(timeStringSplit[1]);
                        } catch (NumberFormatException nfe) {
                            
                        }
                    }
            
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
                    // Dates
                    Date date = null;
                    
                    String dateString = entry.getValue(klu.getDateKey());
                    if (StringUtils.nullOrEmpty(dateString)){
                        date = null;
                    } else {
                        date = dateFormat.parse(dateString);
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    if(date != null) {
                        cal.setTime(date);
                        cal.clear(Calendar.HOUR_OF_DAY);
                        cal.clear(Calendar.MINUTE);
                        cal.clear(Calendar.SECOND);
                        cal.clear(Calendar.MILLISECOND);
                    }
                    if (minute != null) {
                        cal.set(Calendar.MINUTE, minute);
                    }
                    if (hour != null) {
                         cal.set(Calendar.HOUR_OF_DAY, hour);
                    }
                    
                    // if any of the time fields are non null, set the
                    // date/time fields of the record.
                    if(date != null || hour != null || minute != null) {
                        record.setWhen(cal.getTime());
                        record.setTime(cal.getTimeInMillis());
                        record.setLastDate(cal.getTime());
                        record.setLastTime(cal.getTimeInMillis());
                    }
                    if (entry.getGeometry() == null) {
                        // Position
                        // Geometry is nullable now
                        if(!(StringUtils.nullOrEmpty(entry.getValue(klu.getLatitudeKey())) && StringUtils.nullOrEmpty(entry.getValue(klu.getLongitudeKey())))){
                            double latitude = Double.parseDouble(entry.getValue(klu.getLatitudeKey()));
                            double longitude = Double.parseDouble(entry.getValue(klu.getLongitudeKey()));
                            record.setPoint(locationService.createPoint(latitude, longitude));
                        }
                            
                    } else {
                        // we always store our geometries in SRID 4326 aka WGS84
                        Geometry theGeom = entry.getGeometry();
                        
                        theGeom.setSRID(4326);
                        record.setGeometry(theGeom);
                    }
                    Location loc = null;
                    // First try to get the location by primary key.
                    if(entry.getValue(klu.getLocationKey()) != null) {
                        int locationId = Integer.parseInt(entry.getValue(klu.getLocationKey()));
                        // At this point locationId may be -1 and therefore loc will be null.
                        loc = locationDAO.getLocation(locationId);
                    }
                    
                    // If the location lookup fails, try to see if we should create
                    // a new location.
                    if(loc == null) {
                        String locationName = entry.getValue(klu.getLocationNameKey());
                        if(locationName != null && !locationName.isEmpty()) {
                            loc = new Location();
                            loc.setName(locationName);
                            
                            double latitude = record.getGeometry().getCentroid().getY();
                            double longitude = record.getGeometry().getCentroid().getX();
                            
                            loc.setLocation(locationService.createPoint(latitude, longitude));
                            loc = locationDAO.save(loc);
                        }
                    }
                    // This loc here may still be null but that is ok.
                    record.setLocation(loc);
                    
                    String accuracyStr = entry.getValue(klu.getAccuracyKey());
                    record.setAccuracyInMeters(StringUtils.notEmpty(accuracyStr) ? Double.parseDouble(accuracyStr) : null);
                }
                // Attach the record Attributes
                List<TypedAttributeValue> attrValuesToDelete = new ArrayList<TypedAttributeValue>();
                Set recAtts = record.getAttributes();
                Set<AttributeScope> scope = new HashSet<AttributeScope>(AttributeScope.values().length+1);
                scope.addAll(Arrays.asList(AttributeScope.values()));
                scope.remove(AttributeScope.LOCATION);
                scope.add(null);
                // Survey Attributes
                attrDeserializer.deserializeAttributes(survey.getAttributes(), attrValuesToDelete, 
                                      recAtts, entry.prefix, attrNameMap, attrFilenameMap, 
                                      record, entry.getDataMap(), entry.getFileMap(), currentUser, moderationOnly, scope, save);
                
                if (isTaxonomicRecord && species != null) {
                    // Taxon Group Attributes
                    attrDeserializer.deserializeAttributes(species.getTaxonGroup().getAttributes(), attrValuesToDelete, 
                                          recAtts, TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX, attrNameMap, attrFilenameMap, 
                                          record, entry.getDataMap(), entry.getFileMap(), currentUser, moderationOnly, scope, save);
                }
                
                // Census Method Attributes
                if (censusMethod != null) {
                    attrDeserializer.deserializeAttributes(censusMethod.getAttributes(), attrValuesToDelete, 
                                          recAtts, TrackerController.CENSUS_METHOD_ATTRIBUTE_PREFIX, attrNameMap, attrFilenameMap, 
                                          record, entry.getDataMap(), entry.getFileMap(), currentUser, moderationOnly, scope, save);
                }
                
                Record testRecord;
                if (save) {
                    testRecord = recordDAO.saveRecord(record);
                } else {
                    testRecord = record;
                }
                if (testRecord != null) {
                    Geometry g = testRecord.getGeometry();
                }
                rsResult.setRecord(record);
                
                if (save) {
                    for(TypedAttributeValue attrVal : attrValuesToDelete) {
                        attributeDAO.save(attrVal);
                        attributeDAO.delete(attrVal);
                    }
                }
            }
        } finally {
            // enable the partial record filter to prevent records for attribute values to be retrieved
            FilterManager.setPartialRecordCountFilter(sessionFactory.getCurrentSession());
        }
        return results;
    }

    /**
     * Validates species and number seen as a group.
     * @param survey the survey the record being validated belongs to.
     * @param taxonomic whether the survey is taxonomic or not.
     * @param species the species to validate.
     * @param taxonomicRecord true if the record is taxonomic
     * @param speciesSearch string for searching for the species if the id wasn't supplied.
     * @param numberString string containing the number seen
     * @param validator the validator to use.
     * @param valid whether validations have passed up to this point.
     * @param params the HTTP params being processed.
     * @return true if the species information is valid.
     */
    private boolean validateSpeciesInformation(Survey survey, Taxonomic taxonomic, IndicatorSpecies species, boolean taxonomicRecord, String speciesSearch, String numberString, RecordFormValidator validator, boolean valid, Map<String, String[]> params) {
        RecordProperty recordProperty;
        recordProperty = new RecordProperty(survey, RecordPropertyType.NUMBER, metadataDAO);
        RecordProperty speciesRecordProperty = new RecordProperty(survey, RecordPropertyType.SPECIES, metadataDAO);

        if(taxonomicRecord) {
                ValidationType numberValidationType;
                ValidationType speciesValidationType;
            if(Taxonomic.TAXONOMIC.equals(taxonomic)) {
                if (speciesRecordProperty.isRequired())  {
                    speciesValidationType = ValidationType.REQUIRED_TAXON;
                } else {
                    speciesValidationType = ValidationType.TAXON;
                }
                if (recordProperty.isRequired()) {
                    numberValidationType = ValidationType.REQUIRED_POSITIVE_LESSTHAN;
                } else {
                    numberValidationType = ValidationType.POSITIVE_LESSTHAN;
                }
            } else {
                    numberValidationType = ValidationType.POSITIVE_LESSTHAN;
                    speciesValidationType = ValidationType.TAXON;
            }

            validator.validate(params, numberValidationType, klu.getIndividualCountKey(), null, recordProperty);

            // No need to check if the species primary key has already resolved a species
            IndicatorSpecies speciesForSurveyCheck = species;

            if(species == null) {
                boolean speciesValid = validator.validate(params, speciesValidationType, klu.getSpeciesNameKey(), null, speciesRecordProperty);
                if (speciesValid) {
                    speciesForSurveyCheck = getSpeciesFromName(speciesSearch);
                }
                valid = valid & speciesValid;
            }
            if(!(speciesRecordProperty.isHidden() && recordProperty.isHidden())){
                // If the record is optionally taxonomic and there is a species with
                // no number or a number with no species, then there is an error
                if(Taxonomic.OPTIONALLYTAXONOMIC.equals(taxonomic)) {
                    // What's with this logic? surely it can be done cleaner!
                    if(             (species == null) && speciesRecordProperty.isRequired() && recordProperty.isRequired() &&
                                    (!((StringUtils.nullOrEmpty(speciesSearch) && StringUtils.nullOrEmpty(numberString)) ||
                                    (!StringUtils.nullOrEmpty(speciesSearch) && !StringUtils.nullOrEmpty(numberString))))) {
                            valid = false;
                            Map<String, String> errorMap = validator.getErrorMap();
                            String errMsg = propertyService.getMessage(
                                            TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE_KEY,
                                            TAXON_AND_NUMBER_REQUIRED_TOGETHER_MESSAGE);
                            errorMap.put(klu.getIndividualCountKey(), errMsg);
                            errorMap.put(klu.getSpeciesNameKey(), errMsg);
                    }
                }
            }
            if (speciesForSurveyCheck != null && survey.getSpecies() != null && survey.getSpecies().size() > 0) {

                // a species set size > 0 indicates the survey has a limited number of species
                // to accept...
                if (!survey.getSpecies().contains(speciesForSurveyCheck)) {

                    Map<String, String> errorMap = validator.getErrorMap();
                    String errMsg = propertyService.getMessage(
                                    TAXON_NOT_IN_SURVEY_KEY,
                                    TAXON_NOT_IN_SURVEY_KEY_DEFAULT_MESSAGE);
                    errorMap.put(klu.getSpeciesNameKey(), errMsg);
                }
            }
        }
        return valid;
    }

    /**
     * Just avoids some repetition...
     * @param name
     * @return
     */
    private IndicatorSpecies getSpeciesFromName(String name) {
        if (StringUtils.nullOrEmpty(name)) {
            return null;
        }
        List<IndicatorSpecies> taxaList = taxaDAO.getIndicatorSpeciesByNameSearchExact(name);
        if (taxaList.size() == 0) {
            return null;
        }
        if (taxaList.size() > 1) {
            log.warn(taxaList.size() + " entries found for name: " + name + ". Returning the first result: " + taxaList.get(0).getScientificName());
        }
        return taxaList.get(0);
    }
    
    /**
     * Used for appending a single message to the record deserializer result in the case of early failure.
     * 
     * @param rdr
     * @param errorKey
     * @param errorMessage
     */
    private void setErrorMessage(RecordDeserializerResult rdr, String errorKey, String errorMessage) {
        Map<String, String> errMap = new HashMap<String, String>(1);
        errMap.put(errorKey, errorMessage);
        rdr.setErrorMap(errMap);
    }
    
    /**
     * Create a new record and assign the parent record if required.
     * 
     * The parent record id can only be set at record creation.
     * 
     * @param entry RecordEntry object
     * @param klu RecordKeyLookup interface
     * @return the newly created Record
     */
    private Record createNewRecord(RecordEntry entry, RecordKeyLookup klu) {
        Record rec = new Record();
        Integer parentRecordId = 0;
        String parentRecordIdString = entry.getValue(klu.getParentRecordIdKey());
        if (StringUtils.notEmpty(parentRecordIdString)) {
            try {
                parentRecordId = Integer.valueOf(parentRecordIdString);    
            } catch (NumberFormatException nfe) {
                log.error("Failed to parse parent record id string : " + parentRecordIdString);
            }   
        }
        rec.setParentRecord(recordDAO.getRecord(parentRecordId));
        return rec;
    }
}
