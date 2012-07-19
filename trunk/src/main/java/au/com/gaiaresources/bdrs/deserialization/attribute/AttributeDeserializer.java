package au.com.gaiaresources.bdrs.deserialization.attribute;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.controller.record.RecordFormValidator;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.attribute.Attributable;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeUtil;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Parses and saves {@link AttributeValue}s.
 * 
 * @author stephanie
 */
public class AttributeDeserializer {

    private Logger log = Logger.getLogger(getClass());
    
    private AttributeParser attributeParser;
    private Set<Integer> newRecords = new HashSet<Integer>();
    
    private RecordDAO recordDAO = AppContext.getBean(RecordDAO.class);
    private AttributeDAO attributeDAO = AppContext.getBean(AttributeDAO.class);
    private FileService fileService = AppContext.getBean(FileService.class);
    
    private static final Integer NULL_PARENT_ID = -1; 
    
    /**
     * Create an instance of AttributeDeserializer
     * @param parser an {@link AttributeParser} that parses the {@link AttributeValue}s for saving
     */
    public AttributeDeserializer(AttributeParser parser) {
        this.attributeParser = parser;
    }
    
    /**
     * Deserialize a {@link List} of {@link Attribute}s from the request mapping.
     * @param attributes a {@link List} of {@link Attribute}s to deserialize
     * @param attrValuesToDelete a {@link List} of {@link TypedAttributeValue}s to be deleted
     * @param recAtts a {@link Set} of {@link TypedAttributeValue}s to save
     * @param entryPrefix a {@link String} to prepend to each {@link Attribute} parameter
     * @param attrNameMap an {@link Attribute} to parameter name mapping
     * @param attrFilenameMap an {@link Attribute} to parameter name mapping for file attributes
     * @param attributable the {@link Attributable} object that owns the {@link AttributeValue}s
     * @param dataMap the parameter map from the request
     * @param fileMap the file map from the request
     * @param currentUser the user making the request
     * @param moderationOnly boolean flag indicating if the form is for 
     *                       moderation only (this affects which parameters can be saved)
     * @param scope a {@link Set} of {@link AttributeScope}s to save
     * @return true if at least one attribute was saved, false otherwise
     * @throws ParseException
     * @throws IOException
     */
    public boolean deserializeAttributes(List<Attribute> attributes, 
            List<TypedAttributeValue> attrValuesToDelete, Set recAtts, String entryPrefix, 
            Map<Attribute, Object> attrNameMap, Map<Attribute, Object> attrFilenameMap, 
            Attributable<? extends TypedAttributeValue> attributable, Map<String, 
            String[]> dataMap, Map<String, MultipartFile> fileMap, 
            User currentUser, boolean moderationOnly, Set<AttributeScope> scope, boolean save) throws ParseException, IOException {
        return deserializeAttributes(attributes, attrValuesToDelete, recAtts, entryPrefix, attrNameMap, attrFilenameMap, attributable, dataMap, fileMap, currentUser, moderationOnly, scope, false, save);
    }
    
    /**
     * Deserialize a {@link List} of {@link Attribute}s from the request mapping.
     * @param attributes a {@link List} of {@link Attribute}s to deserialize
     * @param attrValuesToDelete a {@link List} of {@link TypedAttributeValue}s to be deleted
     * @param recAtts a {@link Set} of {@link TypedAttributeValue}s to save
     * @param entryPrefix a {@link String} to prepend to each {@link Attribute} parameter
     * @param attrNameMap an {@link Attribute} to parameter name mapping
     * @param attrFilenameMap an {@link Attribute} to parameter name mapping for file attributes
     * @param attributable the {@link Attributable} object that owns the {@link AttributeValue}s
     * @param dataMap the parameter map from the request
     * @param fileMap the file map from the request
     * @param currentUser the user making the request
     * @param moderationOnly boolean flag indicating if the form is for 
     *                       moderation only (this affects which parameters can be saved)
     * @param scope a {@link Set} of {@link AttributeScope}s to save
     * @param doTagAttributes boolean flag indicating if we want to save tag attributes or not
     * @return true if at least one attribute was saved, false otherwise
     * @throws ParseException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public boolean deserializeAttributes(List<Attribute> attributes, 
            List<TypedAttributeValue> attrValuesToDelete, Set recAtts, String entryPrefix, 
            Map<Attribute, Object> attrNameMap, Map<Attribute, Object> attrFilenameMap, 
            Attributable<? extends TypedAttributeValue> attributable, Map<String, 
            String[]> dataMap, Map<String, MultipartFile> fileMap, 
            User currentUser, boolean moderationOnly, Set<AttributeScope> scope, boolean doTagAttributes, boolean save) throws ParseException, IOException {
    
        boolean savedOne = false;
        for (Attribute attribute : attributes) {
            if (scope.contains(attribute.getScope()) && attribute.isTag() == doTagAttributes) {
                String prefix = AttributeScope.isRecordScope(attribute.getScope()) ? entryPrefix : "";
                Object attrNameObj = attrNameMap.get(attribute);
                if (attrNameObj instanceof List) {
                    int nameIndex = 0;
                    String filename = "";
                    List<String> fileNameList = null;
                    if (attrFilenameMap.get(attribute) instanceof List) {
                        fileNameList = (List<String>)attrFilenameMap.get(attribute);
                    } else {
                        filename = (String)attrFilenameMap.get(attribute);
                    }
                    for (String name : (List<String>)attrNameObj) {
                        // only save this attribute for this record if it matches
                        // the prefix + attribute_id
                        String paramName = WebFormAttributeParser.getParamKey(entryPrefix, attribute);
                        if (name.equals(paramName)) {
                            if (fileNameList != null && fileNameList.size() > nameIndex) {
                                filename = fileNameList.get(nameIndex++);
                            }
                            savedOne |= saveAttribute(prefix, name, 
                                      filename, attribute,
                                      attributable, dataMap, fileMap, currentUser, recAtts, 
                                      attrValuesToDelete, moderationOnly, attrNameMap, attrFilenameMap, entryPrefix, scope, save);
                        }
                    }
                } else {
                    savedOne |= saveAttribute(prefix, getAttrNameKey(attrNameMap, attribute), 
                                  getAttrNameKey(attrFilenameMap, attribute), attribute,
                                  attributable, dataMap, fileMap, currentUser, recAtts, 
                                  attrValuesToDelete, moderationOnly, attrNameMap, attrFilenameMap, entryPrefix, scope, save);
                }
            }
        }
        
        return savedOne;
    }

    /**
     * Parses and saves an attribute value.
     * @param prefix a {@link String} to prepend to each {@link Attribute} parameter
     * @param attrName the request parameter name for the {@link Attribute} value
     * @param attrFileName the request parameter name for the {@link Attribute} file
     * @param attribute the {@link Attribute} to save
     * @param attributable the {@link Attributable} object that owns the {@link AttributeValue}s
     * @param dataMap the parameter map from the request
     * @param fileMap the file map from the request
     * @param currentUser the user making the request
     * @param recAtts a {@link Set} of {@link TypedAttributeValue}s to save
     * @param attrValuesToDelete a {@link List} of {@link TypedAttributeValue}s to be deleted
     * @param moderationOnly boolean flag indicating if the form is for 
     *                       moderation only (this affects which parameters can be saved)
     * @param attrNameMap an {@link Attribute} to parameter name mapping
     * @param attrFilenameMap an {@link Attribute} to parameter name mapping for file attributes
     * @param entryPrefix a {@link String} to prepend to each sub parameter of the {@link Attribute}
     *                    (for census method attribute types)
     * @param scope a {@link Set} of {@link AttributeScope}s to save
     * @return true if the attribute was saved, false otherwise
     * @throws ParseException
     * @throws IOException
     */
    private boolean saveAttribute(String prefix, String attrName, String attrFileName, 
            Attribute attribute, Attributable<? extends TypedAttributeValue> attributable, Map<String, String[]> dataMap, 
            Map<String, MultipartFile> fileMap, User currentUser, Set recAtts, 
            List<TypedAttributeValue> attrValuesToDelete, boolean moderationOnly, 
            Map<Attribute, Object> attrNameMap, Map<Attribute, Object> attrFilenameMap, 
            String entryPrefix, Set<AttributeScope> scope, boolean save) throws ParseException, IOException {
        boolean savedOne = false;
        TypedAttributeValue recAttr = attributeParser.parse(prefix+attrName, prefix+attrFileName, 
                                        attribute, attributable, dataMap, fileMap);
        // have to save the value here since we are recursing with the attributeParser
        // and the value may change as child attributes are saved
        boolean isAddOrUpdate = attributeParser.isAddOrUpdateAttribute();
        if (AttributeType.isCensusMethodType(attribute.getType())) {
            // create a sub record to record the census method attributes
            // add the census method attributes to the dictionary too
            // get the census method from the attribute value
            CensusMethod cm = attribute.getCensusMethod();
            Set<Record> childRecords = recAttr.getRecords();
            if (childRecords == null) {
                childRecords = new LinkedHashSet<Record>();
            }
            Record thisRecord = null;
            if (attributable instanceof Record) {
                thisRecord = (Record) attributable;
            }
            
            Map<String, Integer> recIds = getRecIdsFromMap(attrNameMap, attribute, dataMap, thisRecord);
            // if the census method is null, we cannot save any attribute values
            // if the attribute value is not AttributeValue type, we cannot save it
            if (cm != null) {
                int rowId = 0;
                String rowPrefix = "";
                // rowId is mapped by the parent record
                Map<Integer, Integer> rowIds = new HashMap<Integer, Integer>();
                for (Entry<String, Integer> rowRec : recIds.entrySet()) {
                    Integer integer = rowRec.getValue();
                    rowPrefix = rowRec.getKey();
                    if (!rowPrefix.startsWith(entryPrefix)) {
                        // ignore any entry that is from another row
                        continue;
                    }
                    Record rec = null;
                    if (integer == 0) {
                        // create a new record
                        rec = new Record();
                        // save the parent record here if it has not yet been saved
                        if (thisRecord != null && thisRecord.getId() == null) {
                            if (save) {
                                thisRecord = recordDAO.saveRecord(thisRecord);
                            }
                            newRecords.add(thisRecord.getId());
                        }
                        rec.setParentRecord(thisRecord);
                        rec.setAttributeValue((AttributeValue)recAttr);
                        rec.setUser(currentUser);
                    } else {
                        rec = recordDAO.getRecord(integer);
                        if (rec == null) {
                            log.error("Could not retrieve record with id "+integer);
                        }
                    }
                    
                    // get the row id for the parent record
                    if (rec.getParentRecord() != null && rowIds.containsKey(rec.getParentRecord().getId())) {
                        rowId = rowIds.get(rec.getParentRecord().getId());
                    } else if (rec.getParentRecord() == null && rowIds.containsKey(NULL_PARENT_ID)) {
                        rowId = rowIds.get(NULL_PARENT_ID);
                    } else {
                        rowId = 0;
                    }
                    
                    prefix = rowPrefix + String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, 
                                                       (integer == 0 ? "" : integer+"_"));

                    boolean saved = deserializeAttributes(cm.getAttributes(), attrValuesToDelete, 
                                          rec.getAttributes(), prefix, 
                                          (Map<Attribute,Object>)attrNameMap.get(attribute), 
                                          (Map<Attribute,Object>)attrFilenameMap.get(attribute), 
                                          rec, dataMap, 
                                          fileMap, currentUser, moderationOnly, scope, save);
                    
                    if (saved) {
                        savedOne = true;
                        // only save the record if at least one attribute is saved
                         if (AttributeUtil.isModifiableByScopeAndUser(attribute, currentUser)) {
                            if (isAddOrUpdate) {
                                if (save) {
                                    recAttr = attributeDAO.save(recAttr);
                                    
                                    rec = recordDAO.save(rec);
                                }
                                if (integer == 0) {
                                    newRecords.add(rec.getId());
                                }
                                childRecords.add(rec);
                                if (attributeParser.getAttrFile() != null && save) {
                                    fileService.createFile(recAttr, attributeParser.getAttrFile());
                                }

                                recAttr.setRecords(childRecords);
                                recAtts.add(recAttr);
                            } else if (!moderationOnly) {
                                // don't delete any attributes on moderation only
                                recAtts.remove(recAttr);
                                attrValuesToDelete.add(recAttr);
                            }
                        }
                        if (AttributeType.CENSUS_METHOD_COL.equals(attribute.getType())) {
                            rowId++;
                            // map the parent id to the row id so we can map records
                            // to their position inside a table within the parent record
                            rowIds.put(rec.getParentRecord() != null ? rec.getParentRecord().getId() : NULL_PARENT_ID, rowId);
                        }
                    }
                }
            }
        } else if (AttributeUtil.isModifiableByScopeAndUser(attribute, currentUser)) {
            if (isAddOrUpdate) {
                if (save) {
                    recAttr = attributeDAO.save(recAttr);
                    
                    if (attributeParser.getAttrFile() != null) {
                        fileService.createFile(recAttr, attributeParser.getAttrFile());
                    }
                }
                recAtts.add(recAttr);
                savedOne = true;
            } else if (!moderationOnly) {
                // don't delete any attributes on moderation only
                recAtts.remove(recAttr);
                attrValuesToDelete.add(recAttr);
            }
        }
        
        return savedOne;
    }

    /**
     * Gets a list of record ids that should be saved for the attribute from the attribute - name mapping.
     * @param attrNameMap the {@link Attribute} name mapping
     * @param attribute the {@link Attribute}
     * @param dataMap the request parameter map
     * @param parentRecord the parent {@link Record} of the {@link Attribute} or null if it does not have a parent record
     * @return a {@link List} of {@link Record} ids to save
     */
    private Map<String, Integer> getRecIdsFromMap(Map<Attribute, Object> attrNameMap,
            Attribute attribute, Map<String, String[]> dataMap, Record parentRecord) {
        Set<String> rowIndex = new HashSet<String>();           
        return getRecIdsFromMap(attrNameMap, attribute, null, dataMap, rowIndex, parentRecord);
    }
    
    /**
     *  Gets a list of record ids that should be saved for the attribute from the attribute - name mapping.
     * @param attrNameMap the {@link Attribute} name mapping
     * @param attribute the {@link Attribute}
     * @param parentAttribute the parent {@link Attribute} of this {@link Attribute} or null if it does not have a parent attribute
     * @param dataMap the request parameter map
     * @param rowIndex a {@link Set} of row prefixes to keep track of saved ones
     * @param parentRecord the parent {@link Record} of the {@link Attribute} or null if it does not have a parent record
     * @return a {@link List} of {@link Record} ids to save
     */
    private Map<String, Integer> getRecIdsFromMap(Map<Attribute, Object> attrNameMap,
                                           Attribute attribute, Attribute parentAttribute, 
                                           Map<String, String[]> dataMap, Set<String> rowIndex, Record parentRecord) {
        // keep the mapping ordered with a LinkedHashMap
        Map<String, Integer> ids = new LinkedHashMap<String, Integer>();
        if (attrNameMap.containsKey(attribute)) {
            // first get the prefix for the key we are looking for
            String nameKey = String.format(AttributeParser.ATTRIBUTE_NAME_TEMPLATE, "", 
                                           (parentAttribute != null ? parentAttribute.getId() : attribute.getId())) + 
                             String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, "");
            Object nameMapValue = attrNameMap.get(attribute);
            if (nameMapValue instanceof Map) {
                Map<Attribute,Object> nameMap = (Map<Attribute, Object>) attrNameMap.get(attribute);
                for (Object value : nameMap.values()) {
                    getRecIdFromValue(value, nameKey, rowIndex, ids, attribute, parentAttribute, dataMap, parentRecord);
                }
            } else {
                getRecIdFromValue(nameMapValue, nameKey, rowIndex, ids, attribute, parentAttribute, dataMap, parentRecord);
            }
        }
        
        return ids;
    }

    /**
     * Gets a {@link Record} id from an {@link Attribute} name mapping entry
     * @param value the value to retrieve the {@link Record} id from, this could be a List, Map, or String
     * @param nameKey the name of the {@link Attribute} parameter
     * @param rowIndex a {@link Set} of row prefixes to keep track of saved ones
     * @param ids a {@link List} of {@link Record} ids to add to
     * @param attribute the {@link Attribute}
     * @param parentAttribute the parent {@link Attribute} of this {@link Attribute} or null if it does not have a parent attribute
     * @param dataMap the request parameter map
     * @param parentRecord the parent {@link Record} of the {@link Attribute} or null if it does not have a parent record
     */
    private void getRecIdFromValue(Object value, String nameKey, Set<String> rowIndex, 
            Map<String, Integer> ids, Attribute attribute, Attribute parentAttribute, 
            Map<String, String[]> dataMap, Record parentRecord) {
        // check to make sure the name also contains the parent if there is one
        String parentNameKey = "";
        // don't include a check for the topmost parent id because it will not be part of the parameter name
        // also don't include any records that have been created during this save
        String[] recordIdParams = dataMap.get(BdrsWebConstants.PARAM_RECORD_ID);
        if (parentRecord != null && parentRecord.getId() != null && 
                recordIdParams != null && recordIdParams.length > 0 && 
                !String.valueOf(parentRecord.getId()).equals(recordIdParams[0]) &&
                !newRecords.contains(parentRecord.getId())) {
            parentNameKey = "record_"+parentRecord.getId()+"_";
        }
        
        // ignore values that are not lists or strings
        if (value instanceof List) {
            List<String> paramNames = (List<String>) value;
            // keep track of the row prefixes to ensure we only add each record id once
            // and that we add a new record (0) for each row
            for (String paramName : paramNames) {
                getRecIdFromParam(paramName, nameKey, parentNameKey, rowIndex, ids, dataMap);
            }
        } else if (value instanceof String) {
            String paramName = (String) value;
            getRecIdFromParam(paramName, nameKey, parentNameKey, rowIndex, ids, dataMap);
        } else if (value instanceof Map) {
            Map<Attribute,Object> childMap = (Map<Attribute, Object>) value;
            for (Attribute att : childMap.keySet()) {
                Map<String, Integer> idsFromChild = getRecIdsFromMap(childMap, att, 
                                 attribute, dataMap, rowIndex, parentRecord);
                ids.putAll(idsFromChild);
            }
        }
    }
    
    private void getRecIdFromParam(String paramName, String nameKey,
            String parentNameKey, Set<String> rowIndex, Map<String, Integer> ids, Map<String, String[]> dataMap) {
        // only get a record id if the parameter contains the attribute we are looking for
        // and the parent attribute as well
        if (paramName.contains(nameKey) && paramName.contains(parentNameKey)) {
            // get the parameter for the parent attribute (for finding the record ids and row prefixes)
            String rowPrefixParamPrefix = paramName.substring(0, paramName.indexOf(nameKey)+nameKey.length());
            // remove the last _record from the name
            int lastRecordIndex = rowPrefixParamPrefix.lastIndexOf("_record");
            rowPrefixParamPrefix = rowPrefixParamPrefix.substring(0, lastRecordIndex);
            // get the rowPrefix and recordId parameters that correspond to this attribute
            String[] rowRecIdParam = dataMap.get(rowPrefixParamPrefix + "_recordId");
            int id = 0;
            String rowNum = rowPrefixParamPrefix;
            try {
                if (rowRecIdParam != null && rowRecIdParam.length > 0) {
                    id = Integer.valueOf(rowRecIdParam[0]);
                } else {
                    int startId = paramName.indexOf(nameKey)+nameKey.length();
                    //rowNum = paramName.substring(0, paramName.indexOf(nameKey));
                    id = Integer.valueOf(paramName.substring(startId, paramName.indexOf("_", startId)));
                }
            } catch (NumberFormatException e) {
                // this means there was no record id associated with the value,
                // add a 0 to the list, indicating that we should add a new record
            }
            if (!rowIndex.contains(rowNum)) {
                ids.put(rowNum, id);
                rowIndex.add(rowNum);
            }
        }
    }

    /**
     * Gets the key for the given attribute from the mapping
     * @param attrNameMap the Attribute name mapping
     * @param attr the attribute to get the key of
     * @return a String key for the given attribute
     */
    private String getAttrNameKey(Map<Attribute, Object> attrNameMap, Attribute attr) {
        if (attrNameMap != null) {
            Object o = attrNameMap.get(attr);
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof List && !((List)o).isEmpty()) {
                return ((List<String>)o).get(0);
            } else if (o instanceof Map && !((Map)o).isEmpty()) {
                Map<Attribute,Object> mapping = (Map<Attribute, Object>) o;
                // get the first value in the mapping
                for (Entry<Attribute, Object> entry : mapping.entrySet()) {
                    return getAttrNameKey(mapping, entry.getKey());
                }
            } else {
                log.error("mapping for attribute "+attr.getId()+" does not exist or is "+o);
            }
        }
        return null;
    }

    /**
     * Validates an attribute value
     * @param validator the validator to use for the validation
     * @param prefix a prefix to prepend to the attribute name
     * @param attrNameMap the attribute - name mapping for looking up parameter names
     * @param attrFilenameMap the attribute - filename mapping for looking up parameter names
     * @param attr the attribute
     * @param params the request parameter map
     * @param fileMap the request file map
     * @return true if the value for the attribute if valid, false otherwise
     */
    public boolean validate(RecordFormValidator validator, String prefix,
            Map<Attribute, Object> attrNameMap,
            Map<Attribute, Object> attrFilenameMap, Attribute attr,
            Map<String, String[]> params, Map<String, MultipartFile> fileMap) {
        return attributeParser.validate(validator,prefix+getAttrNameKey(attrNameMap, attr), 
                                        getAttrNameKey(attrFilenameMap, attr), attr, params, fileMap);
    }
}
