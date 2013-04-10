package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import org.apache.log4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SingleSiteFormToRecordEntryTransformer handles the case where survey scoped row matrix attributes need to be
 * edited.  In this case, the parameters reference the child recordId ids of the primary recordId being edited directly
 * so this class manipulates the parameters to include the correct recordId ids for other records recorded on the same
 * form.
 */
public class SingleSiteFormToRecordEntryTransformer {

    private static Logger log = Logger.getLogger(SingleSiteFormToRecordEntryTransformer.class);

    /** Parameter suffix treated as a special case for matching / replacement */
    private static final String ROW_PREFIX = "rowPrefix";
    /** Parameter suffix treated as a special case for matching / replacement */
    private static final String RECORD_ID_PARAM = "recordId";



    /**
     * Represents the path to the Record that contains the attribute values for a single row of a census method
     * attribute.
     */
    static class RecordPathElement {

        public Integer recordId;
        public Integer row;
        public int attributeId;
        public boolean containsRecord;

        public RecordPathElement(int attributeId, Integer row, boolean containsRecord, Integer recordId) {
            this.attributeId = attributeId;
            this.row = row;
            this.containsRecord = containsRecord;
            this.recordId = recordId;
        }

        public boolean hasRow() {
            return row != null;
        }

        public String toParameterString() {

            return toParameterString(false);
        }

        public String toParameterString(boolean omitRecordString) {
            StringBuilder result = new StringBuilder();
            if (hasRow()) {
                result.append(row).append("_");
            }
            result.append("attribute_").append(attributeId);

            if (!omitRecordString && containsRecord) {
                result.append("_record");
                if (recordId != null) {
                    result.append("_").append(recordId);
                }
            }

            return result.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RecordPathElement)) return false;

            RecordPathElement that = (RecordPathElement) o;

            if (attributeId != that.attributeId) return false;
            if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;
            if (row != null ? !row.equals(that.row) : that.row != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = recordId != null ? recordId.hashCode() : 0;
            result = 31 * result + (row != null ? row.hashCode() : 0);
            result = 31 * result + attributeId;
            return result;
        }

        @Override
        public String toString() {
            return "RecordPathElement{" +
                    "recordId=" + recordId +
                    ", row=" + row +
                    ", attributeId=" + attributeId +
                    ", containsRecord=" + containsRecord +
                    '}';
        }
    }


    /**
     * Identifies a single Record storing data for a row of a census method attribute.
     * The path begins from the top level record.
     */
    static class RecordPath extends ArrayList<RecordPathElement> {

        /**
         * Used for census method column attributes which can have multiple records associated with the attribute.
         * The row information is provided in the in the parameter value.
         * @param attributeId the attribute id.
         * @param row the row that identifies the record that contains the values for this attribute.
         */
        public RecordPathElement addPathElement(Integer attributeId, Integer row, boolean containsRecord, Integer recordId) {
            RecordPathElement pathElement = new RecordPathElement(attributeId, row, containsRecord, recordId);
            add(pathElement);
            return pathElement;
        }

        public RecordPathElement addPathElement(String attributeIdStr, String rowStr, boolean containsRecord, String recordIdStr) {
            int attributeId = Integer.parseInt(attributeIdStr);
            Integer row = rowStr != null ? Integer.parseInt(rowStr) : null;
            Integer recordId = recordIdStr != null ? Integer.parseInt(recordIdStr) : null;
            RecordPathElement pathElement = new RecordPathElement(attributeId, row, containsRecord, recordId);
            add(pathElement);
            return pathElement;
        }

        public Integer getDeepestRecord() {
            if (size() > 0) {
                Integer recordId = get(size()-1).recordId;
                if (recordId != null) {
                    return recordId;
                }
            }
            return null;
        }
    }

    /**
     * The ParameterParser is responsible for parsing HTTP parameters into a RecordPath.
     */
    static class ParameterParser {
        /** Pattern to match census method attributes and their associated records */
        private static Pattern paramPattern = Pattern.compile("(?:(\\d+)_)?attribute_(\\d+)_(record_(?:(\\d+)_)?)?");
        private static final int ROW_GROUP = 1;
        private static final int ATTRIBUTE_ID_GROUP = 2;
        private static final int RECORD_GROUP = 3;
        private static final int RECORD_ID_GROUP = 4;

        /** Used as context for the parsing operation */
        private Record context;

        /**
         * The parser needs the Record as a parsing context because otherwise a row number can be matched as a
         * record in the case of a new record or row.
         * e.g. For the parameter: attribute_3_record_1_attribute_2 the "1" is the row number for attribute 2
         * however it will be matched as record id 1 for attribute 3.  Having the Record allows us to detect
         * this (the parsed record id won't match the context, which will not have a saved record stored against
         * the value of attribute 3.
         * @param record used to provide necessary context for the parsing operation.
         */
        public ParameterParser(Record record) {
            context = record;
        }

        public RecordPath parse(String parameter) {

            RecordMatcher matcher = new RecordMatcher(context);
            RecordPath path = new RecordPath();
            Record record = context;
            Matcher m = paramPattern.matcher(parameter);
            int offset = 0;
            while (m.find(offset)) {
                offset = m.end();
                // Each match contains three groups, the row, the attribute and the record.  For any particular match,
                // the row and record may be null
                String row = m.group(ROW_GROUP);
                String attributeId = m.group(ATTRIBUTE_ID_GROUP);
                String recordId = m.group(RECORD_ID_GROUP);

                RecordPathElement pathElement = path.addPathElement(attributeId, row, m.group(RECORD_GROUP) != null, recordId);
                if (record != null) {
                    record = matcher.recordForPathElement(record, pathElement);
                    if (recordId != null && record == null) {
                        // The row number from the following attribute has been incorrectly parsed as a record id.
                        pathElement.recordId = null;
                        offset = m.start(RECORD_ID_GROUP);
                    }
                }

            }
            return path;
        }

    }

    /**
     * The RecordMatcher is responsible for performing operations on Records using a RecordPath as input.
     * It can return a Record identified by a Path or transform a RecordPath created for one Record into
     * the equivalent path for a second Record that was created using the same survey form.
     */
    static class RecordMatcher {

        private Record record;
        public RecordMatcher(Record record) {
            this.record = record;
        }

        /**
         * Returns the Record identified by the supplied PathElement.
         * @param parent the parent Record, (obtained by a previous call to this method).
         * @param pathElement the path to the child record of the supplied parent.
         * @return the Record identified by the supplied path or null if the path doesn't match (which may be the
         * case if the edit is adding new rows to a census method attribute value).
         */
        public Record recordForPathElement(Record parent, RecordPathElement pathElement) {
            Record record = null;
            int attributeId = pathElement.attributeId;

            AttributeValue value = valueOf(attributeId, parent);
            int row = pathElement.hasRow() ? pathElement.row : 0;

            if (value != null) {

                List<Record> records = value.getOrderedRecords();
                if (records.size() > row) {
                    record = value.getOrderedRecords().get(row);
                }
            }

            return record;
        }

        /**
         * Returns true if the Record managed by this RecordMatcher is the one being edited (as evidenced by the
         * values of the supplied HTTP parameters)
         * @param parameters the HTTP parameters submitted by the user.
         * @return true if this RecordMatcher represents the Record being edited.
         */
        public boolean matches(Map<String, String[]> parameters) {

            try {
                for (String key : parameters.keySet()) {
                    RecordPath path = new ParameterParser(this.record).parse(key);
                    if (path.size() > 0 && path.get(0).recordId != null) {
                        return (transformPath(path).equals(path));
                    }
                }
            }
            catch (Exception e) {
                log.error("Error performing match: ", e);
            }
            return false;
        }


        public RecordPath transformPath(RecordPath path) {

            RecordPath newPath = new RecordPath();

            Record record = this.record;
            for (RecordPathElement pathElement : path) {

                record = recordForPathElement(record, pathElement);
                Integer recordId = record != null ? record.getId() : null;
                newPath.addPathElement(pathElement.attributeId, pathElement.row, pathElement.containsRecord, recordId);
            }
            return newPath;
        }

        private AttributeValue valueOf(int attributeId, Record record) {
            // A null record is valid in the case of a new record being added during an edit operation.
            if (record != null) {

                for (AttributeValue value : record.getAttributes()) {
                    if (Integer.valueOf(attributeId).equals(value.getAttribute().getId())) {
                        return value;
                    }
                }
            }
            return null;
        }
    }

    private RecordDAO recordDAO;

    /**
     * Key: recordPrefix , value: mappings of attribute/row/recordId for all census method attributes in the record.
     */
    private Map<String, RecordMatcher> recordMappings;

    public SingleSiteFormToRecordEntryTransformer(RecordDAO recordDAO) {
        this.recordDAO = recordDAO;
        recordMappings = new HashMap<String, RecordMatcher>();
    }

    /**
     * Returns a list of mappings with key: rowPrefix, value: http parameters to use when updating the
     * record identified by the row prefix.
     * This class will modify the http parameters as necessary to support editing census method attributes
     * on single site multi/all species forms.
     * @param paramMap the http parameters passed.
     * @param fileMap any file uploads created as a part of processing a mulitpart http request.
     * @param rowIds the prefixes that identify each record in a single site multi/all taxa survey.
     * @return the parameters to use when processing each record.
     */
    public List<RecordEntry> httpRequestParamToRecordMap(Map<String, String[]> paramMap, Map<String, MultipartFile> fileMap, String[] rowIds) {
        List<RecordEntry> result = new ArrayList<RecordEntry>();

        boolean substitutionRequired = isSubstitutionRequired(paramMap, rowIds);

        for (String recordPrefix : rowIds) {
            Map<String, String[]> recordParams = paramMap;
            Map<String, MultipartFile> recordFiles = fileMap;

            // Don't do the substitution for the master mapping (or if substitution is not required)
            if (substitutionRequired && recordMappings.containsKey(recordPrefix)) {

                RecordMatcher matcher = recordMappings.get(recordPrefix);
                recordParams = substituteParameters(paramMap, matcher);

                recordFiles = substituteFiles(fileMap, matcher);
            }
            RecordEntry entry = new RecordEntry(recordParams, recordFiles, recordPrefix);
            result.add(entry);
        }

        return result;
    }

    private boolean isSubstitutionRequired(Map<String, String[]> paramMap, String[] rowIds) {
        boolean recordFound = false;
        for (String recordPrefix : rowIds) {
            Integer recordId = getRecordId(recordPrefix, paramMap);
            if (recordId != null) {
                Record record = recordDAO.getRecord(recordId);
                RecordMatcher mapping = new RecordMatcher(record);

                // No need to store a mapping for the record that already has the correct parameters supplied.
                if (!mapping.matches(paramMap)) {

                    recordMappings.put(recordPrefix, mapping);

                }
                recordFound = true;
            }
            else {
                RecordMatcher mapping = new RecordMatcher(null);
                recordMappings.put(recordPrefix, mapping);
            }
        }

        return recordFound && !recordMappings.isEmpty();
    }

    private Integer getRecordId(String rowId, Map<String, String[]> params) {
        String key = rowId + RECORD_ID_PARAM;
        String[] record = params.get(key);
        if (record != null && record.length > 0) {
            if (record[0] != null && record[0].length() > 0) {
                return Integer.parseInt(record[0]);
            }
        }
        return null;
    }

    /**
     * Takes a the set of user supplied HTTP parameters and returns a modified set of parameters suitable for
     * editing the census method attribute values associated with the Record managed by the supplied RecordMatcher.
     * @param params the original HTTP parameters
     * @param mapping capable of mapping the original parameters to ones suitable for the contained Record.
     * @return a new set of HTTP parameters to use.
     */
    private Map<String, String[]> substituteParameters(Map<String, String[]> params, RecordMatcher mapping) {

        Map<String, String[]> newParams = new HashMap<String, String[]>();

        ParameterParser parser = new ParameterParser(mapping.record);
        for (Map.Entry<String, String[]> entry : params.entrySet()) {

            String key = entry.getKey();
            RecordPath path = parser.parse(key);
            RecordPath newPath = mapping.transformPath(path);
            String newKey = doSubstitution(path, newPath, key);

            String[] value = entry.getValue();
            String[] newValue = value;

            if (key.endsWith(ROW_PREFIX) && value != null) {
                newValue = new String[value.length];
                for (int i=0;i<value.length; i++) {
                    RecordPath valuePath = parser.parse(value[i]);
                    if (mapping.record == null && valuePath.size() > 0) {
                        newValue[i] = valuePath.get(0).row + "_";
                    }
                    else {
                        RecordPath newValuePath = mapping.transformPath(valuePath);

                        // We need to do a similar substitution on the value.
                        newValue[i]= doSubstitution(valuePath, newValuePath, value[i]);
                    }
                }
            }
            // The reason for the check for "attribute" is we don't want to modify the value of
            // rowIndex_recordId=x parameters (e.g. 0_recordId=1, 1_recordId=5) as these specify the values of
            // top level records, not census method attribute records.
            else if (key.endsWith(RECORD_ID_PARAM) && key.contains("attribute")) {

                Integer record = newPath.getDeepestRecord();
                newValue = new String[] {record != null ? record.toString() : "0"};
            }

            newParams.put(newKey, newValue);

        }
        return newParams;
    }

    /**
     * Takes a the set of user supplied HTTP parameters and returns a modified set of parameters suitable for
     * editing any files uploaded during an edit operation of a multi species form.
     * @param fileMap the set original HTTP parameters containing an uploaded file.
     * @param mapping capable of mapping the original parameters to ones suitable for the contained Record.
     * @return a new set of HTTP parameters to use.
     */
    private Map<String, MultipartFile> substituteFiles(Map<String, MultipartFile> fileMap, RecordMatcher mapping) {
        Map<String, MultipartFile> newFiles = new HashMap<String, MultipartFile>();

        ParameterParser parser = new ParameterParser(mapping.record);
        for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {

            String key = entry.getKey();
            RecordPath path = parser.parse(key);
            RecordPath newPath = mapping.transformPath(path);
            String newKey = doSubstitution(path, newPath, key);
            newFiles.put(newKey, entry.getValue());
        }

        return newFiles;
    }

    private String doSubstitution(RecordPath path, RecordPath newPath, String key) {

        StringBuffer newKey = new StringBuffer(key);
        int offset = 0;
        for (int i=0; i<path.size(); i++) {
            String originalPath = path.get(i).toParameterString();
            RecordPathElement newPathElement = newPath.get(i);

            int pathPos = newKey.indexOf(originalPath, offset);
            if (pathPos >= 0) {
                String newPathSection;
                // This condition is to test for and omit the trailing _record_recordId for new records
                // to enable conversion of e.g 0_attribute_record_3_attribute_4_record_4_recordId to
                // 0_attribute_record_3_attribute_4_recordId
                if (i == (path.size()-1) && key.endsWith(RECORD_ID_PARAM) && newPathElement.recordId == null) {
                    newPathSection = newPathElement.toParameterString(true);
                }
                else {
                    newPathSection = newPathElement.toParameterString();
                }
                newKey.replace(pathPos, pathPos+originalPath.length(), newPathSection);
                offset += newPathSection.length();
            }

        }
        return newKey.toString();
    }

}
