package au.com.gaiaresources.bdrs.serialization;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedRecordFilter;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.util.DateUtils;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Writes records to CSV format
 * This class is not thread safe.
 * Created by aaron on 22/04/2014.
 */
public class RecordCsvWriter {

    /**
     * These types are ignored by RecordCsvWriter
     */
    public static final List<AttributeType> IGNORE_ATTR_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    AttributeType.HTML,
                    AttributeType.HTML_NO_VALIDATION,
                    AttributeType.HTML_RAW,
                    AttributeType.HTML_COMMENT,
                    AttributeType.HTML_HORIZONTAL_RULE,
                    AttributeType.CENSUS_METHOD_ROW,
                    AttributeType.CENSUS_METHOD_COL));

    public static final String HEADER_RECORD_ID = "record_id";
    public static final String HEADER_OWNER_FIRST = "owner_first_name";
    public static final String HEADER_OWNER_LAST = "owner_last_name";

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";

    private RecordDAO recordDAO;
    private RedirectionService redirectionService;
    private Survey survey;
    private CensusMethod censusMethod;

    private Logger log = Logger.getLogger(getClass());

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
    private SimpleDateFormat isoFormat = new SimpleDateFormat(DateUtils.ISO_DATE_FORMAT);

    private List<RecordProperty> recPropToWrite = new ArrayList<RecordProperty>();
    private List<String> headers = new ArrayList<String>();

    private SpatialUtil spatialUtil;
    private BdrsCoordReferenceSystem bdrsCoordReferenceSystem;

    private LinkedHashMap<Attribute, String> attrHeaderMap = new LinkedHashMap<Attribute, String>();

    /**
     * Construct a new writer
     *
     * @param recordDAO RecordDAO
     * @param mdDAO     MetadataDAO
     * @param redirectionService RedirectionService
     */
    public RecordCsvWriter(RecordDAO recordDAO,
                           MetadataDAO mdDAO,
                           RedirectionService redirectionService,
                           Survey survey,
                           CensusMethod censusMethod) {

        if (recordDAO == null) {
            throw new IllegalArgumentException("RecordDAO cannot be null");
        }
        if (mdDAO == null) {
            throw new IllegalArgumentException("MetadataDAO cannot be null");
        }
        if (redirectionService == null) {
            throw new IllegalArgumentException("RedirectionService cannot be null");
        }
        if (survey == null) {
            throw new IllegalArgumentException("Survey cannot be null");
        }
        this.recordDAO = recordDAO;
        this.redirectionService = redirectionService;
        this.survey = survey;
        this.censusMethod = censusMethod;

        headers = new ArrayList<String>();

        // Compulsory fields:

        // RECORD FIELDS
        addHeaderItem(null, HEADER_RECORD_ID);
        addHeaderItem(null, HEADER_OWNER_FIRST);
        addHeaderItem(null, HEADER_OWNER_LAST);

        recPropToWrite = new ArrayList<RecordProperty>();

        bdrsCoordReferenceSystem = survey.getCrs();

        if (bdrsCoordReferenceSystem.getSrid() != BdrsCoordReferenceSystem.NO_SPECIFIED_ZONE) {
            spatialUtil = new SpatialUtilFactory().
                    getLocationUtil(bdrsCoordReferenceSystem.getSrid());
        } else {
            spatialUtil = new SpatialUtilFactory().
                    getLocationUtil(BdrsCoordReferenceSystem.DEFAULT_SRID);
        }

        // write first row...
        RecordPropertyType[] recordProperties = RecordPropertyType.values();

        // RECORD PROPERTIES
        for (RecordPropertyType recordPropertyType : recordProperties) {
            RecordProperty rp = new RecordProperty(survey, recordPropertyType, mdDAO);
            if (!rp.isHidden()) {
                recPropToWrite.add(rp);
                if (rp.getRecordPropertyType() == RecordPropertyType.POINT) {
                    // POINT is a special case - 2 entries required.
                    if (bdrsCoordReferenceSystem.isXfirst()) {
                        addHeaderItem(null, bdrsCoordReferenceSystem.getXname());
                        addHeaderItem(null, bdrsCoordReferenceSystem.getYname());
                    } else {
                        addHeaderItem(null, bdrsCoordReferenceSystem.getYname());
                        addHeaderItem(null, bdrsCoordReferenceSystem.getXname());
                    }
                } else {
                    addHeaderItem(null, rp.getName());
                }
            }
        }

        // SURVEY ATTRIBUTES
        for (Attribute surveyAttr : survey.getAttributes()) {
            addAttributeHeader(surveyAttr);
        }

        // CENSUS METHOD ATTRIBUTES
        if (censusMethod != null) {
            for (Attribute cmAttr : censusMethod.getAttributes()) {
                addAttributeHeader(cmAttr);
            }
        }
    }

    /**
     * Add Attribute / name pairing. Handles name collisions.
     *
     * @param attr Attribute associated with the name. This can be null
     * @param name Initial name to use. This may be changed if we detect name collisions
     */
    private void addHeaderItem(Attribute attr, String name) {
        if (hasHeaderItem(name)) {
            if (attr == null) {
                // non attributes are user first name,
                // user last name, record id and any record property
                throw new IllegalStateException("We have 2 non attributes " +
                        "that share the same name. This shouldn't happen as " +
                        "users cannot make names for these fields. Name : " + name);
            }
            // Name collision. append on to name until we have something unique.
            Integer nameCounter = 0;
            while (hasHeaderItem(name)) {
                name = String.format("%s_%d", name, nameCounter);
            }
        }

        headers.add(name);
        if (attr != null) {
            if (attrHeaderMap.containsKey(attr)) {
                throw new IllegalStateException("We have tried to add a header for the same attribute twice : " + name);
            }
            attrHeaderMap.put(attr, name);
        }
    }

    /**
     * Helper method to check whether a header name has already been
     * used
     * @param name Check this header name
     * @return True if header name has been used otherwise false
     */
    private boolean hasHeaderItem(String name) {
        return headers.contains(name);
    }

    /**
     * Write CSV
     * @param writer It is the caller's responsibility to close this writer
     *
     */
    public void writeRecords(Writer writer) {

        RecordFilter filter = new AdvancedRecordFilter();
        filter.setRecordVisibility(RecordVisibility.PUBLIC);
        filter.setSurveyPk(survey.getId());
        filter.setCensusMethod(censusMethod);
        filter.setHeld(false);
        ScrollableRecords scrollableRecords = recordDAO.getScrollableRecords(filter);

        CSVWriter csvWriter = null;
        try {
            csvWriter = new CSVWriter(writer);

            // write the headers...
            csvWriter.writeNext(headers.toArray(new String[1]));

            // iterate over our records and write our values...
            while (scrollableRecords.hasMoreElements()) {
                Record r = scrollableRecords.nextElement();
                if (r.getCensusMethod() == censusMethod) {
                    List<String> values = new ArrayList<String>(headers.size());

                    // Add compulsory fields in the correct order...
                    values.add(r.getId().toString());
                    if (r.getUser() != null) {
                        String firstName = r.getUser().getFirstName();
                        if (firstName != null) {
                            values.add(firstName);
                        } else {
                            values.add("");
                        }
                        String lastName = r.getUser().getLastName();
                        if (lastName != null) {
                            values.add(lastName);
                        } else {
                            values.add("");
                        }
                    }

                    for (RecordProperty rp : recPropToWrite) {
                        addRecordPropertyValue(rp, r, values);
                    }

                    for (Map.Entry<Attribute, String> entry : attrHeaderMap.entrySet()) {
                        Attribute attrToWrite = entry.getKey();
                        AttributeValue recAttrValue = r.valueOfAttribute(attrToWrite);

                        if (recAttrValue != null) {
                            addAttributeValue(recAttrValue, values);
                        } else {
                            // Attribute value doesn't exist for this record.
                            // Write an empty entry.
                            values.add("");
                        }
                    }

                    csvWriter.writeNext(values.toArray(new String[1]));
                    csvWriter.flush();
                }
            }
        } catch (IOException e) {
            log.error("failed to write csv", e);
        } finally {
            if (csvWriter != null) {
                try {
                    csvWriter.close();
                } catch (IOException e) {
                    log.error("Failed to close csv writer", e);
                }
            }
        }
    }

    /**
     * Returns a mapping of attribute to header name used
     * in the resulting CSV output from writeRecords()
     * @return Map
     */
    public Map<Attribute, String> getHeaderMap() {
        return attrHeaderMap;
    }

    /**
     * Add a record property to the values List argument.
     *
     * @param recordProperty RecordProperty to add.
     * @param record         Record that we will extract the RecordProperty value from.
     * @param values         Current list of values. Will append new record property value to this list.
     */
    private void addRecordPropertyValue(RecordProperty recordProperty,
                                        Record record,
                                        List<String> values) {

        switch (recordProperty.getRecordPropertyType()) {
            case SPECIES: {
                IndicatorSpecies species = record.getSpecies();
                if (species != null
                        && species.getScientificName() != null) {
                    values.add(species.getScientificName());
                } else {
                    values.add("");
                }
            }
            break;
            case NUMBER:
                if (record.getNumber() != null) {
                    values.add(record.getNumber().toString());
                } else {
                    values.add("");
                }
                break;
            case LOCATION: {
                Location loc = record.getLocation();
                if (loc != null && loc.getName() != null) {
                    values.add(loc.getName());
                } else {
                    values.add("");
                }
            }

            break;
            case POINT: {
                Geometry geometry = record.getGeometry();
                if (geometry != null) {
                    // Reassign our geometry. Make sure we have the correct CRS
                    geometry = spatialUtil.transform(geometry);

                    if (bdrsCoordReferenceSystem.isXfirst()) {
                        values.add(Double.toString(geometry.getCentroid().getX()));
                        values.add(Double.toString(geometry.getCentroid().getY()));
                    } else {
                        values.add(Double.toString(geometry.getCentroid().getY()));
                        values.add(Double.toString(geometry.getCentroid().getX()));
                    }
                } else {
                    values.add("");
                    values.add("");
                }
                break;
            }
            case GPS_ALTITUDE: {
                Double altitude = record.getGpsAltitude();
                if (altitude != null) {
                    values.add(altitude.toString());
                } else {
                    values.add("");
                }
            }
            break;
            case ACCURACY: {
                Double accuracy = record.getAccuracyInMeters();
                if (accuracy != null) {
                    values.add(accuracy.toString());
                } else {
                    values.add("");
                }
            }
            break;
            case WHEN: {
                Date when = record.getWhen();
                if (when != null) {
                    values.add(dateFormat.format(when));
                } else {
                    values.add("");
                }
            }
            break;
            case TIME: {
                Date when = record.getWhen();
                if (when != null) {
                    values.add(timeFormat.format(when));
                } else {
                    values.add("");
                }
            }
            break;
            case NOTES: {
                String notes = record.getNotes();
                if (notes != null) {
                    values.add(notes);
                } else {
                    values.add("");
                }
            }
            break;
            case CREATED: {
                Date createdAt = record.getCreatedAt();
                if (createdAt != null) {
                    values.add(isoFormat.format(createdAt));
                } else {
                    values.add("");
                }
            }
            break;
            case UPDATED: {
                Date updatedAt = record.getUpdatedAt();
                if (updatedAt != null) {
                    values.add(isoFormat.format(updatedAt));
                } else {
                    values.add("");
                }
            }
            break;
            default:
                // ignore and log warning
                log.warn("Ignoring unhandled record property type : " + recordProperty.getRecordPropertyType());
        }
    }

    /**
     * Run during object setup. Add an attribute that will later be written to CSV
     * @param attr Attribute to add
     */
    private void addAttributeHeader(Attribute attr) {

        AttributeType attrType = attr.getType();

        switch (attrType) {
            case INTEGER:
            case INTEGER_WITH_RANGE:
            case DECIMAL:

            case BARCODE:
            case REGEX:

            case DATE:
            case TIME:

            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:

            case STRING_WITH_VALID_VALUES:

            case SINGLE_CHECKBOX:
            case MULTI_CHECKBOX:
            case MULTI_SELECT:

            case IMAGE:
            case AUDIO:
            case FILE:
            case VIDEO:

            case SPECIES:
                addHeaderItem(attr, attr.getName());
                break;

            default:
                if (!IGNORE_ATTR_TYPES.contains(attrType)) {
                    // ignore and log warning
                    log.warn("Ignoring unhandled attribute type : " + attrType);
                }
        }
    }

    /**
     * Called while iterating over records and their attribute values.
     * Writes an attribute value to the outgoing
     *
     * @param av retrieve CSV value to write from this attribute value
     * @param valueList append CSV value to write on to this list
     */
    private void addAttributeValue(AttributeValue av, List<String> valueList) {

        Attribute attr = av.getAttribute();
        AttributeType attrType = attr.getType();

        switch (attrType) {
            case INTEGER:
            case INTEGER_WITH_RANGE:
            case DECIMAL:
            case BARCODE:
            case REGEX:
            case DATE:
            case TIME:
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            case SINGLE_CHECKBOX:
            case MULTI_CHECKBOX:
            case STRING_WITH_VALID_VALUES:
            case MULTI_SELECT: {
                String strValue = av.getValue();
                if (strValue != null) {
                    valueList.add(strValue);
                } else {
                    valueList.add("");
                }
            }
                break;

            case IMAGE:
            case AUDIO:
            case FILE:
            case VIDEO: {
                String strValue = av.getStringValue();
                if (strValue != null) {
                    // add the full file download url
                    valueList.add(redirectionService.getFileDownloadUrl(av, true));
                } else {
                    valueList.add("");
                }
            }
                break;

            case SPECIES: {
                IndicatorSpecies sp = av.getSpecies();
                if (sp != null) {
                    valueList.add(sp.getScientificName());
                } else {
                    valueList.add("");
                }
            }
            break;

            default:
                if (!IGNORE_ATTR_TYPES.contains(attrType)) {
                    // ignore and log warning
                    log.warn("Ignoring unhandled attribute type : " + attrType);
                }
        }
    }
}
