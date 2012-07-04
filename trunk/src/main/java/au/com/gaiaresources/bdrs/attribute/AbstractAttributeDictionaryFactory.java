package au.com.gaiaresources.bdrs.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.controller.record.TrackerController;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Abstract class to handle creation of attribute to parameter name mapping.
 * @author stephanie
 */
public abstract class AbstractAttributeDictionaryFactory implements
        AttributeDictionaryFactory {
    private Logger log = Logger.getLogger(getClass());
    protected static final Set<AttributeScope> SCOPE_RECORD_SURVEY;
    protected static final Set<AttributeScope> SCOPE_LOCATION;
    private static final String PREFIX_TEMPLATE = "%d_";
    
    static {
        Set<AttributeScope> tmp = new HashSet<AttributeScope>(5);
        tmp.add(null);
        tmp.add(AttributeScope.RECORD);
        tmp.add(AttributeScope.SURVEY);
        tmp.add(AttributeScope.RECORD_MODERATION);
        tmp.add(AttributeScope.SURVEY_MODERATION);
        SCOPE_RECORD_SURVEY = Collections.unmodifiableSet(tmp);
        
        Set<AttributeScope> tmp2 = new HashSet<AttributeScope>(1);
        tmp2.add(AttributeScope.LOCATION);
        SCOPE_LOCATION = Collections.unmodifiableSet(tmp2);
    }
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.AttributeDictionaryFactory#createFileKeyDictionary(au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, au.com.gaiaresources.bdrs.model.method.CensusMethod)
     */
    @Override
    public Map<Attribute, Object> createFileKeyDictionary(Record record, Survey survey, Location location,
            TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap) {
        return createDictionary(record, survey, location, taxonGroup, censusMethod, scope, dataMap, true);
    }

    /**
     * Constructs a prefix to apply to rows for census method column attributes.
     * This provides an index for each row in the table as %d_
     * @param attributeType the type of the attribute, used to determine if the prefix should be applied or not
     * @param rowIndex the index of the row in the table
     * @return a String that is either the prefix for the row as %d_ or an empty string
     */
    private String getRowPrefix(AttributeType attributeType, int rowIndex) {
        if (AttributeType.CENSUS_METHOD_COL.equals(attributeType)) {
            return String.format(PREFIX_TEMPLATE, rowIndex);
        } else {
            return "";
        }
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.AttributeDictionaryFactory#createFileKeyDictionary(java.util.List, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, java.util.List)
     */
    @Override
    public abstract Map<Attribute, Object> createFileKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod);

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.AttributeDictionaryFactory#createNameKeyDictionary(au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, au.com.gaiaresources.bdrs.model.method.CensusMethod)
     */
    @Override
    public Map<Attribute, Object> createNameKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap) {
        return createDictionary(record, survey, location, taxonGroup, censusMethod, scope, dataMap, false);
    }

    /**
     * Creates a dictionary of attributes to parameter names.  In the case of 
     * census method attribute types, a mapping of attributes to a mapping of 
     * attributes to parameter names.
     * @param record the record that owns the attributes being mapped, if one exists
     * @param survey the survey that contains the attributes
     * @param location the location that contains the attributes
     * @param taxonGroup the taxonGroup that contains the attributes
     *        Note that only one of survey, location, or taxonGroup is required
     * @param censusMethod the census method of the survey (for creating census method attributes mapping)
     * @param scope a set of attribute scopes to restrict attributes added to the map
     * @param dataMap the parameter map of the request
     * @param isFileDictionary true if you want to create a file name dictionary, false otherwise
     * @return A mapping of attributes to parameter names.  The parameter names 
     *         can be a single String name, a List of names, or a mapping of 
     *         child attributes to their corresponding parameter names.
     */
    protected Map<Attribute, Object> createDictionary(Record record, Survey survey,
            Location location, TaxonGroup taxonGroup,
            CensusMethod censusMethod, Set<AttributeScope> scope,
            Map<String, String[]> dataMap, boolean isFileDictionary) {
        if (survey == null) {
            throw new IllegalArgumentException("survey cannot be null");
        }

        Map<Attribute, Object> result = new HashMap<Attribute, Object>();
        Set<String> check = new HashSet<String>();
        
        Map<Attribute, AttributeValue> attrValMap = new HashMap<Attribute, AttributeValue>();
        if (record != null) {
            for (AttributeValue value : record.getAttributes()) {
                attrValMap.put(value.getAttribute(), value);
            }
        } else if (location != null) {
            for (AttributeValue value : location.getAttributes()) {
                attrValMap.put(value.getAttribute(), value);
            }
        }
        
        HashSet<Integer> existingCMs = new HashSet<Integer>();
        if (survey.getAttributes() != null) {
            addAttributesToMap(result, attrValMap, survey.getAttributes(), scope, check, "", "Survey", dataMap, existingCMs, isFileDictionary);
            existingCMs.clear();
        }
        
        if (taxonGroup != null && taxonGroup.getAttributes() != null) {
            addAttributesToMap(result, attrValMap, taxonGroup.getAttributes(), scope, check, TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX, "Taxon group", dataMap, existingCMs, isFileDictionary);
            existingCMs.clear();
        }
        
        if (censusMethod != null && censusMethod.getAttributes() != null) {
            addAttributesToMap(result, attrValMap, censusMethod.getAttributes(), scope, check, TrackerController.CENSUS_METHOD_ATTRIBUTE_PREFIX, "Census method", dataMap, existingCMs, isFileDictionary);
            existingCMs.clear();
        }
        return result;
    }

    /**
     * Adds a list of attributes to the attribute - name mapping.
     * @param result the mapping to add the attributes to
     * @param attrValueMap a mapping of attributes to their corresponding values
     * @param attributes a list of attributes to add to the mapping
     * @param scope a set of scopes to restrict the attributes added to the mapping
     * @param check a set of names that have already been added to ensure each name is only added once
     * @param prefix the prefix to append to each parameter name
     * @param attributeSource the source of the attribute (survey, taxon, or census method) for error reporting
     * @param dataMap the parameter mapping from the request
     * @param existingCMs a set of census method ids that have been added to the stack
     *                    (to prevent infinite recursion over census methods that 
     *                     reference each other or themselves)
     * @param isFileDictionary boolean flag indicating if we are creating a file name mapping or just a name mapping
     */
    protected void addAttributesToMap(Map<Attribute, Object> result, Map<Attribute, AttributeValue> attrValueMap, 
            List<Attribute> attributes, Set<AttributeScope> scope, Set<String> check, 
            String prefix, String attributeSource, Map<String, String[]> dataMap, Set<Integer> existingCMs, boolean isFileDictionary) {
        for (Attribute attribute : attributes) {
            if(scope.contains(attribute.getScope())) {
                addKeyToMap(prefix, result, attrValueMap, attribute, scope, check, attributeSource, dataMap, new HashSet<Integer>(existingCMs), isFileDictionary);
            } 
        }
    }

    /**
     * Adds an attribute - parameter name pair to the attribute - name mapping.
     * @param prefix the prefix to append to the parameter
     * @param result the mapping to add the pair to
     * @param attrValueMap a mapping of attributes to attribute values
     * @param attribute the attribute to add
     * @param scope a set of scopes to filter attributes by
     * @param check a set of names that have already been added to ensure each name is only added once
     * @param attributeSource the source of the attribute (survey, taxon, or census method) for error reporting
     * @param dataMap the parameter mapping from the request
     * @param existingCMs a set of census method ids that have been added to the stack
     *                    (to prevent infinite recursion over census methods that 
     *                     reference each other or themselves)
     * @param isFileDictionary boolean flag indicating if we are creating a file name mapping or just a name mapping
     */
    protected void addKeyToMap(String prefix, Map<Attribute, Object> result,
            Map<Attribute, AttributeValue> attrValueMap, Attribute attribute,
            Set<AttributeScope> scope, Set<String> check,
            String attributeSource, Map<String, String[]> dataMap, Set<Integer> existingCMs, boolean isFileDictionary) {
        if (AttributeType.isCensusMethodType(attribute.getType())) {
            // add the census method attributes to the dictionary too
            CensusMethod cm = attribute.getCensusMethod();
            
            if (cm != null && existingCMs.add(cm.getId())) {
                // get the existing value for the attribute, it should be a map
                // of all of the child record attributes to their parameter values 
                // or to a map of all the child's child record attributes to 
                // their parameter values and so on....
                Object subMap = result.get(attribute);
                Map<Attribute, Object> childMap;
                if (subMap == null) {
                    // create a new map if one does not yet exist
                    childMap = new HashMap<Attribute, Object>();
                    result.put(attribute, childMap);
                } else {
                    // if the value is other than a map, change it to a map and discard the value
                    if (subMap instanceof Map) {
                        childMap = (Map<Attribute,Object>)subMap;
                    } else {
                        childMap = new HashMap<Attribute, Object>();
                        result.put(attribute, childMap);
                    }
                }
                // get the records from the attribute value
                AttributeValue attrVal = attrValueMap.get(attribute);
                Set<Record> recs = attrVal != null ? attrVal.getRecords() : null;
                int rowIndex = 0;
                int maxRowCount = 1;
                // the paramkey for census method types is not file type, using false here
                String prefixParamKey = getParamKey(prefix, attribute, false);
                String[] rows = dataMap.get(prefixParamKey+"_rowPrefix");
                Map<String, Integer> prefixRecs = new HashMap<String, Integer>(rows != null ? rows.length : 1);
                Map<Integer, String> recPrefixes = new HashMap<Integer, String>(recs != null ? recs.size() : 1);
                // create the mapping from the parameters
                if (rows != null) {
                    // only CENSUS_METHOD_COL types have the _rowPrefix parameter
                    for (String rowPrefix : rows) {
                        // get the record id input parameter
                        if (StringUtils.hasLength(rowPrefix)) {
                            // if the row prefix is only a number, attach the original prefix to it as well
                            // otherwise, it represents the entire hierarchy of the attribute and can be left as is
                            if (rowPrefix.matches("\\d+_")) {
                                rowPrefix = getParamKey(prefix+rowPrefix, attribute, false);
                            }
                        } else {
                            // if the rowPrefix is an empty String, use the default attribute parameter key
                            rowPrefix = getParamKey(rowPrefix, attribute, false);
                        }
                        // have to check the rowPrefix for a trailing _ because the parameter 
                        // one will have it, but the attribute one will not
                        // get the recordId corresponding to the rowPrefix
                        String[] params = dataMap.get(rowPrefix+(!rowPrefix.endsWith("_") ? "_" : "")+"recordId");
                        Integer recId = 0;
                        if (params != null && params.length >= 1) {
                            recId = Integer.valueOf(params[0]);
                            prefixRecs.put(rowPrefix, recId);
                            if (recId != 0) {
                                recPrefixes.put(recId, rowPrefix);
                            }
                        }
                    }
                } else {
                    // get the recordId parameter
                    String[] params = dataMap.get(prefixParamKey+"_recordId");
                    Integer recId = 0;
                    if (params != null && params.length == 1) {
                        recId = Integer.valueOf(params[0]);
                        for (; rowIndex < maxRowCount; rowIndex++) {
                            String rowPrefix = getRowPrefix(attribute.getType(), rowIndex);
                            rowPrefix = getParamKey(rowPrefix, attribute, false);
                            prefixRecs.put(rowPrefix, recId);
                        }
                    } 
                    
                }
                String paramKey = "";
                if (recs != null && recs.size() > 0) {
                    for (Record rec : recs) {
                        String preRemove = recPrefixes.remove(rec.getId());
                        // remove record from prefixRecs mapping so it is not used again
                        prefixRecs.remove(preRemove);
                        // create a new attribute-value mapping
                        Map<Attribute, AttributeValue> recAttrValMap = new HashMap<Attribute, AttributeValue>();
                        for (AttributeValue value : rec.getAttributes()) {
                            recAttrValMap.put(value.getAttribute(), value);
                        }
                        if (preRemove != null) {
                            // if we have stored a prefix for the record, use that one
                            // as is if it starts with the given prefix otherwise
                            // prepend the prefix
                            if (preRemove.startsWith(prefix)) {
                                paramKey = preRemove;
                            } else {
                                paramKey = getParamKey(prefix+preRemove, attribute, false);
                            }
                        } else {
                            paramKey = getParamKey(prefix+getRowPrefix(attribute.getType(), rowIndex++), attribute, isFileDictionary)+String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, rec.getId()+"_");
                        }
                        addAttributesToMap(childMap, recAttrValMap, cm.getAttributes(), scope, check, 
                                           paramKey, 
                                           "Census method", dataMap, new HashSet<Integer>(existingCMs), isFileDictionary);
                    }
                } 
                // add the remaining rows
                for (Entry<String,Integer> entry : prefixRecs.entrySet()) {
                    String rowPrefix = entry.getKey();
                    Integer recId = entry.getValue();
                    if (rowPrefix.startsWith(prefix)) {
                        paramKey = rowPrefix;
                    } else {
                        paramKey = getParamKey(prefix+rowPrefix, attribute, false);
                    }
                    addAttributesToMap(childMap, new HashMap<Attribute, AttributeValue>(), 
                                       cm.getAttributes(), scope, check, 
                                       paramKey+String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, (recId != 0 ? recId+"_" : "")), 
                                       "Census method", dataMap, new HashSet<Integer>(existingCMs), isFileDictionary);
                }
            }
        } else {
            addKey(result, check, getParamKey(prefix, attribute, isFileDictionary), attribute, attributeSource);
        }
    }

    /**
     * Creates an attribute parameter key from the prefix and attribute id.
     * @param prefix the prefix to append to the parameter name
     * @param attribute the attribute to create the parameter key for
     * @param isFileDictionary true if the parameter name should be for retrieving a file, false otherwise
     * @return a String representing the name of the parameter for the attribute in the request map
     */
    private String getParamKey(String prefix, Attribute attribute, boolean isFileDictionary) {
        if (isFileDictionary) {
            return WebFormAttributeParser.getFileKey(prefix, attribute);
        } else {
            return WebFormAttributeParser.getParamKey(prefix, attribute);
        }
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.AttributeDictionaryFactory#createNameKeyDictionary(java.util.List, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, java.util.List)
     */
    @Override
    public abstract Map<Attribute, Object> createNameKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod);

    /**
     * Adds the attribute/key pair to the map.
     * @param result attribute to their key
     * @param existingKeys set of existing keys
     * @param key the key to add
     * @param attribute the attribute to add
     * @param attributeSource what object type owns the attribute
     */
    @SuppressWarnings("unchecked")
    protected void addKey(Map<Attribute, Object> result, Set<String> existingKeys, String key, Attribute attribute, String attributeSource) {
        if (!StringUtils.hasLength(key)) {
            // ignore if this attribute has no key
            return;
        }
        if (existingKeys.add(key)) {
            Object existingValue = result.get(attribute);
            if (existingValue != null) {
                if (existingValue instanceof List) {
                    ((List<String>)existingValue).add(key);
                } else if (existingValue instanceof String) {
                    // make a new list and add both Strings to it
                    List<String> newList = new ArrayList<String>(2);
                    newList.add((String)existingValue);
                    newList.add(key);
                    result.put(attribute, newList);
                }
            } else {
                result.put(attribute, key);
            }
        } else {
            //throw new IllegalArgumentException(attributeSource + " key: " + key + " already exists.");
            log.warn(attributeSource + " key: " + key + " already exists, adding to map for attribute "+attribute.getId());
        }
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory#getDictionaryAttributeScope()
     */
    @Override
    public abstract Set<AttributeScope> getDictionaryAttributeScope();
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory#createNameKeyDictionary(au.com.gaiaresources.bdrs.model.record.Record, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.model.location.Location, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, au.com.gaiaresources.bdrs.model.method.CensusMethod, java.util.Map)
     */
    @Override
    public Map<Attribute, Object> createNameKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Map<String, String[]> dataMap) {
        return createNameKeyDictionary(record, survey, location, taxonGroup, censusMethod, getDictionaryAttributeScope(), dataMap);
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory#createFileKeyDictionary(au.com.gaiaresources.bdrs.model.record.Record, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.model.location.Location, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, au.com.gaiaresources.bdrs.model.method.CensusMethod, java.util.Map)
     */
    @Override
    public Map<Attribute, Object> createFileKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Map<String, String[]> dataMap) {
        return createFileKeyDictionary(record, survey, location, taxonGroup, censusMethod, getDictionaryAttributeScope(), dataMap);
    }
}
