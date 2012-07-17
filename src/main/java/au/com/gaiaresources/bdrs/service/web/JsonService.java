package au.com.gaiaresources.bdrs.service.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.map.GeoMapFeature;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

@Service
public class JsonService {
    
    public static final String JSON_KEY_ITEMS = "items";
    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_ATTRIBUTES = "attributes";
    public static final String JSON_KEY_ID = "id";
    public static final String JSON_KEY_ATTR_TYPE = "type";
    public static final String JSON_KEY_ATTR_NAME = "name";
    public static final String JSON_KEY_ATTR_VALUE = "value";
    public static final String JSON_KEY_IS_SCINAME = "sciName";
    
    public static final String JSON_ITEM_TYPE_RECORD = "record";
    public static final String JSON_ITEM_TYPE_MAP_FEATURE = "geoMapFeature";
    
    public static final String JSON_KEY_SRID = "srid";
    public static final String JSON_KEY_X_NAME = "xname";
    public static final String JSON_KEY_Y_NAME = "yname";
    public static final String JSON_KEY_CRS_DISPLAY_NAME = "name";
    
    public static final String RECORD_KEY_CENSUS_METHOD = "census_method";
    public static final String RECORD_KEY_NUMBER = "number";
    public static final String RECORD_KEY_NOTES = "notes";
    public static final String RECORD_KEY_SPECIES = "species";
    public static final String RECORD_KEY_COMMON_NAME = "common_name";
    public static final String RECORD_KEY_HABITAT = "habitat";
    public static final String RECORD_KEY_WHEN = "when";
    public static final String RECORD_KEY_BEHAVIOUR = "behaviour";
    public static final String RECORD_KEY_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    public static final String RECORD_KEY_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    public static final String RECORD_KEY_VISIBILITY = "recordVisibility";
    public static final String RECORD_KEY_X_COORD = "x";
    public static final String RECORD_KEY_Y_COORD = "y";
    public static final String RECORD_KEY_CRS = "coord_ref_system";
    
    // first + last name of the recording user
    public static final String RECORD_KEY_USER = "owner";
    public static final String RECORD_KEY_USER_ID = "ownerId";
    
    public static final String DATE_FORMAT = "dd-MMM-yyyy";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    
    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private PreferenceDAO prefDAO;
    
    /**
     * 
     * @param record - the record to convert to json
     * @param hideDetails - whether or not we should hide the details of the record. In general, on a public map we will hide the details
     * @return
     */
    public JSONObject toJson(AccessControlledRecordAdapter record, String contextPath, SpatialUtilFactory spatialUtilFactory) {
        if (contextPath == null) {
            throw new IllegalArgumentException("String, contextPath, cannot be null");
        }
        if (record == null) {
            throw new IllegalArgumentException("AccessControlledRecordAdapter, record, cannot be null");
        }
        
        Map<String, Object> attrMap = new HashMap<String, Object>(16);
        
        addToAttributeMap(attrMap, RECORD_KEY_USER, record.getUser().getFirstName() + " " + record.getUser().getLastName());
        addToAttributeMap(attrMap, RECORD_KEY_USER_ID, record.getUser().getId());
        
        if (record.getCensusMethod() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_CENSUS_METHOD, record.getCensusMethod().getName());
        } else {
            addToAttributeMap(attrMap, RECORD_KEY_CENSUS_METHOD, "Standard Taxonomic");
        }
        
        if (record.getSpecies() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_SPECIES, record.getSpecies().getScientificName());
            addToAttributeMap(attrMap, RECORD_KEY_COMMON_NAME, record.getSpecies().getCommonName());
            addToAttributeMap(attrMap, RECORD_KEY_NUMBER, record.getNumber());
        }
        
        if (record.getNotes() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_NOTES, record.getNotes());
        }
        
        addToAttributeMap(attrMap, RECORD_KEY_HABITAT, record.getHabitat());
        addToAttributeMap(attrMap, JSON_KEY_ATTRIBUTES, getOrderedAttributes(record.getOrderedAttributes(), contextPath));
        addToAttributeMap(attrMap, RECORD_KEY_BEHAVIOUR, record.getBehaviour());   
        
        if(record.getWhen() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_WHEN, record.getWhen().getTime());

            String k = String.format(PersistentImpl.FLATTENED_FORMATTED_DATE_TMPL, RECORD_KEY_WHEN);
            SimpleDateFormat formatter = new SimpleDateFormat(PersistentImpl.DATE_FORMAT_PATTERN);
            String v = formatter.format(record.getWhen());
            addToAttributeMap(attrMap, k, v);
        }
        
        // legacy
        addToAttributeMap(attrMap, RECORD_KEY_RECORD_ID, record.getId());
        addToAttributeMap(attrMap, RECORD_KEY_SURVEY_ID, record.getSurvey().getId());
        
        // This is important, always include this stuff
        addToAttributeMap(attrMap, JSON_KEY_ID, record.getId());
        addToAttributeMap(attrMap, JSON_KEY_TYPE, JSON_ITEM_TYPE_RECORD);
        addToAttributeMap(attrMap, RECORD_KEY_VISIBILITY, record.getRecordVisibility());
        
        
        if (record.getGeometry() != null) {
        	SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil(record.getGeometry().getSRID());
        	BdrsCoordReferenceSystem crs = BdrsCoordReferenceSystem.getBySRID(record.getGeometry().getSRID());
        	addToAttributeMap(attrMap, RECORD_KEY_CRS, toJson(crs));
        	addToAttributeMap(attrMap, RECORD_KEY_X_COORD, spatialUtil.truncate(record.getLongitude()));
            addToAttributeMap(attrMap, RECORD_KEY_Y_COORD, spatialUtil.truncate(record.getLatitude()));
        }
        
        return JSONObject.fromMapToJSONObject(attrMap);
    }
    
    public JSONObject toJson(BdrsCoordReferenceSystem crs) {
    	JSONObject obj = new JSONObject();
    	obj.accumulate(JSON_KEY_SRID, crs.getSrid());
    	obj.accumulate(JSON_KEY_X_NAME, crs.getXname());
    	obj.accumulate(JSON_KEY_Y_NAME, crs.getYname());
    	obj.accumulate(JSON_KEY_CRS_DISPLAY_NAME, crs.getDisplayName());
    	return obj;
    }
    
    public JSONObject toJson(GeoMapFeature feature) {
        Map<String, Object> attrMap = new HashMap<String, Object>(3);
        attrMap.put(JSON_KEY_ID, feature.getId());
        attrMap.put(JSON_KEY_TYPE, JSON_ITEM_TYPE_MAP_FEATURE);
        // it's ok to use an empty context path here since GeoMapFeatures cannot have file attributes
        // which is the only type that requires the contextPath to create the download link
        attrMap.put(JSON_KEY_ATTRIBUTES, getOrderedAttributes(feature.getOrderedAttributes(), ""));
        return JSONObject.fromMapToJSONObject(attrMap);
    }
    
    private void addToAttributeMap(Map<String, Object> attrMap, String key, Object value) {
        if (attrMap.containsKey(key)) {
            log.warn("overwriting attribute map key : " + key);
        }
        if (value != null) {
            attrMap.put(key, value);
        }
    }
    
    private JSONArray getOrderedAttributes(List<AttributeValue> attributeValues, String contextPath) {
        JSONArray array = new JSONArray();
        for (AttributeValue av : attributeValues) {
            array.add(toJson(av, contextPath));
        }
        return array;
    }
    
    private JSONObject toJson(AttributeValue av, String contextPath) {
        Preference sciNamePref = prefDAO.getPreferenceByKey(Preference.SHOW_SCIENTIFIC_NAME_KEY);
        // default to true.
        Boolean showSciName = sciNamePref != null ? Boolean.valueOf(sciNamePref.getValue()) : true;
        
        Attribute attr = av.getAttribute();
        JSONObject obj = new JSONObject();
        String key = StringUtils.hasLength(attr.getDescription()) ? attr.getDescription() : attr.getName();
        obj.accumulate(JSON_KEY_ATTR_TYPE, attr.getTypeCode());
        obj.accumulate(JSON_KEY_ATTR_NAME, key);
        switch (attr.getType()) {
            case INTEGER:
            case INTEGER_WITH_RANGE:
            case DECIMAL:
                obj.accumulate(JSON_KEY_ATTR_VALUE, av.getNumericValue());
                break;
            case DATE:
                Date d = av.getDateValue();
                String format = d == null ? null : dateFormat.format(av.getDateValue()); 
                obj.accumulate(JSON_KEY_ATTR_VALUE, format);
                break;
            case HTML:
            case HTML_NO_VALIDATION:
            case HTML_COMMENT:
            case HTML_HORIZONTAL_RULE:
                // ignore html attributes because they do not have attribute values
                break;
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            case STRING_WITH_VALID_VALUES:
                obj.accumulate(JSON_KEY_ATTR_VALUE, av.getStringValue());
                break;
            // allow download of files and image attribute types
            case IMAGE:
            case AUDIO:
            case FILE:
                obj.accumulate(JSON_KEY_ATTR_VALUE, getAttributeValueFileDownloadLink(av, contextPath));
                break;
            case SPECIES:
            {
                IndicatorSpecies species = av.getSpecies();
                obj.accumulate(JSON_KEY_IS_SCINAME, showSciName);
                if (species != null) {
                    obj.accumulate(JSON_KEY_ATTR_VALUE, showSciName.equals(Boolean.TRUE) ? 
                            species.getScientificName() : species.getCommonName());    
                }
            }
                break;
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                Set<Record> records = av.getRecords();
                if (records != null) {
                    JSONObject recObj = new JSONObject();
                    for (Record record : records) {
                        JSONObject attObj = new JSONObject();
                        for (AttributeValue recordValue : record.getAttributes()) {
                            attObj.accumulate(JSON_KEY_ATTR_VALUE, toJson(recordValue, contextPath));
                        }
                        recObj.accumulate(JSON_ITEM_TYPE_RECORD, attObj);
                    }
                    obj.accumulate(JSON_KEY_ATTR_VALUE, recObj);
                }
                break;
            default:
                // ignore
        }
        return obj;
    }
    
    private String getAttributeValueFileDownloadLink(AttributeValue av, String contextPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"");
        sb.append(contextPath);
        sb.append("/files/download.htm?className=au.com.gaiaresources.bdrs.model.taxa.AttributeValue&id=");
        sb.append(av.getId().toString());
        sb.append("&fileName=");
        sb.append(av.getStringValue());
        sb.append("\">Download file</a>");
        return sb.toString();
    }

    /**
     * Returns a JSON representation of a location.  (Used for writing kml records)
     * @param location the location to jsonify
     * @param contextPath the contextPath of the application
     * @return A JSONObject representing the location.
     */
    public JSONObject toJson(Location location, String contextPath, SpatialUtilFactory spatialUtilFactory) {
        Map<String, Object> attrMap = new HashMap<String, Object>(16);
        addToAttributeMap(attrMap, "name", location.getName());
        addToAttributeMap(attrMap, "description", location.getDescription());
        
        User owner = location.getUser();
        if (owner != null) {
            addToAttributeMap(attrMap, RECORD_KEY_USER, owner.getFirstName() + " " + owner.getLastName());
            addToAttributeMap(attrMap, RECORD_KEY_USER_ID, owner.getId());
        } else {
            // use the creator if the owner is null? createdBy only returns id, must retrieve entire user
        }

        if(location.getCreatedAt() != null) {
            addToAttributeMap(attrMap, RECORD_KEY_WHEN, location.getCreatedAt().getTime());
        }
        
        if (location.getLocation() != null) {
        	int srid = location.getLocation().getSRID();
	    	SpatialUtil spatialUtil = spatialUtilFactory.getLocationUtil(srid);
	    	BdrsCoordReferenceSystem crs = BdrsCoordReferenceSystem.getBySRID(srid);
	    	addToAttributeMap(attrMap, RECORD_KEY_CRS, toJson(crs));
	    	addToAttributeMap(attrMap, RECORD_KEY_X_COORD, spatialUtil.truncate(location.getLongitude()));
	        addToAttributeMap(attrMap, RECORD_KEY_Y_COORD, spatialUtil.truncate(location.getLatitude()));
        }
        
        attrMap.put(JSON_KEY_ATTRIBUTES, getOrderedAttributes(location.getOrderedAttributes(), contextPath));
        
        return JSONObject.fromMapToJSONObject(attrMap);
    }
}
