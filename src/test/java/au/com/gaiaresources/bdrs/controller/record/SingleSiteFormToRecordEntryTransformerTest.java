package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.deserialization.record.RecordEntry;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests the SingleSiteFormToRecordEntryTransformer
 */
public class SingleSiteFormToRecordEntryTransformerTest extends TestCase {

    private SingleSiteFormToRecordEntryTransformer recordEntryTransformer;
    private Mockery mockery = new Mockery();
    private RecordDAO mockRecordDAO = mockery.mock(RecordDAO.class);

    private Attribute censusMethodAttr;
    private Attribute nestedTextAttribute;
    private Attribute nestedCensusMethod;
    private Attribute doubleNestedTextAttribute;
    private Attribute rowCensusMethod;
    private Attribute nestedRowTextAttribute;
    private Attribute fileAttribute;

    private Record createTestRecord(int firstId) {
        Record parent = new Record();
        parent.setId(firstId++);

        censusMethodAttr = createAttribute(10, AttributeType.CENSUS_METHOD_COL);

        AttributeValue censusMethodValue = new AttributeValue();
        censusMethodValue.setAttribute(censusMethodAttr);

        Record child = new Record();
        child.setId(firstId++);
        Set<Record> children = new HashSet<Record>();
        children.add(child);
        censusMethodValue.setRecords(children);

        nestedTextAttribute = createAttribute(20, AttributeType.TEXT);

        AttributeValue attr2Value = new AttributeValue();
        attr2Value.setAttribute(nestedTextAttribute);
        attr2Value.setStringValue("Value 2");

        nestedCensusMethod = createAttribute(30, AttributeType.CENSUS_METHOD_COL);
        AttributeValue nestedCensusMethodValue = new AttributeValue();
        nestedCensusMethodValue.setAttribute(nestedCensusMethod);

        child.getAttributes().add(attr2Value);
        child.getAttributes().add(nestedCensusMethodValue);

        Record childOfChild = new Record();
        childOfChild.setId(firstId++);
        children = new HashSet<Record>();
        children.add(childOfChild);
        nestedCensusMethodValue.setRecords(children);

        doubleNestedTextAttribute = createAttribute(40, AttributeType.TEXT);

        AttributeValue doubleNestedTextAttributeValue = new AttributeValue();
        doubleNestedTextAttributeValue.setAttribute(doubleNestedTextAttribute);
        doubleNestedTextAttributeValue.setStringValue("Nested Test");
        childOfChild.getAttributes().add(doubleNestedTextAttributeValue);

        fileAttribute = createAttribute(70, AttributeType.IMAGE);
        AttributeValue fileAttributeValue = new AttributeValue();
        fileAttributeValue.setAttribute(fileAttribute);
        fileAttributeValue.setStringValue("filename");
        childOfChild.getAttributes().add(doubleNestedTextAttributeValue);

        parent.getAttributes().add(censusMethodValue);

        rowCensusMethod = createAttribute(50, AttributeType.CENSUS_METHOD_ROW);
        AttributeValue rowCensusMethodValue = new AttributeValue();
        rowCensusMethodValue.setAttribute(rowCensusMethod);

        parent.getAttributes().add(rowCensusMethodValue);

        Record childOfRow = new Record();
        childOfRow.setId(firstId++);
        children = new HashSet<Record>();
        children.add(childOfRow);
        rowCensusMethodValue.setRecords(children);

        nestedRowTextAttribute = createAttribute(60, AttributeType.TEXT);
        AttributeValue nestedRowVal = new AttributeValue();
        nestedRowVal.setAttribute(nestedRowTextAttribute);
        nestedRowVal.setStringValue("Row Text");
        childOfRow.getAttributes().add(nestedRowVal);

        return parent;
    }

    private Attribute createAttribute(int id, AttributeType type) {

        Attribute attr = new Attribute();
        attr.setId(id);
        attr.setScope(AttributeScope.SURVEY);
        attr.setTypeCode(type.getCode());

        return attr;
    }


    @Test
    public void testEditCensusMethodAttributes() {

        final Record species1 = createTestRecord(1);
        final Record species2 = createTestRecord(5);

        mockery.checking(new Expectations() {{
            oneOf(mockRecordDAO).getRecord(1);
            will(returnValue(species1));

            oneOf(mockRecordDAO).getRecord(5);
            will(returnValue(species2));

        }});

        Map<String, MultipartFile> fileMap = new LinkedHashMap<String, MultipartFile>();
        String[] rowIds = {"0_", "1_"};
        Map<String, String[]> paramMap = new LinkedHashMap<String, String[]>();

        addSpeciesRecordParams(paramMap, new String[]{Integer.toString(species1.getId()), Integer.toString(species2.getId())});
        int species1ChildRecordId = 2;
        addCensusMethodAttr("Test", species1ChildRecordId, paramMap, fileMap);
        int species1RowChildRecordId = 4;
        addRowCensusMethodAttr("Test row", species1RowChildRecordId, paramMap);

        recordEntryTransformer = new SingleSiteFormToRecordEntryTransformer(mockRecordDAO);
        List<RecordEntry> entries = recordEntryTransformer.httpRequestParamToRecordMap(paramMap, fileMap, rowIds);

        mockery.assertIsSatisfied();

        Assert.assertEquals(2, entries.size());

        // Make sure the primary record's record entry is unchanged.
        Assert.assertEquals(paramMap, entries.get(0).getDataMap());
        Assert.assertEquals(fileMap, entries.get(0).getFileMap());

        // Now test the second record's record entry has been modified appropriately.
        Map<String, String[]> species2Params = entries.get(1).getDataMap();
        Map<String, MultipartFile> species2Files = entries.get(1).getFileMap();

        Assert.assertEquals(paramMap.size(), species2Params.size());
        Assert.assertEquals(fileMap.size(), species2Files.size());

        Assert.assertEquals("0_attribute_10_record_6_", species2Params.get("attribute_10_rowPrefix")[0]);
        Assert.assertEquals("6", species2Params.get("0_attribute_10_recordId")[0]);

        Assert.assertEquals("6", species2Params.get("0_attribute_10_record_6_recordId")[0]);
        Assert.assertEquals("Test", species2Params.get("0_attribute_10_record_6_attribute_20")[0]);
        Assert.assertEquals("0", species2Params.get("0_attribute_10_record_6_rowIndex")[0]);


        // Now the nested census method
        Assert.assertEquals("7", species2Params.get("0_attribute_10_record_6_0_attribute_30_recordId")[0]);
        Assert.assertEquals("Test nested", species2Params.get("0_attribute_10_record_6_0_attribute_30_record_7_attribute_40")[0]);

        // Now the file in the nested census method
        Assert.assertEquals("file.txt", entries.get(1).getFileMap().get("0_attribute_10_record_6_0_attribute_30_record_7_attribute_file_70").getName());

        // Now the nested row census method
        Assert.assertEquals("8", species2Params.get("attribute_50_record_8_recordId")[0]);
        Assert.assertEquals("Test row", species2Params.get("attribute_50_record_8_attribute_60")[0]);

    }

    @Test
    public void testEditCensusMethodAttributesAddNewSpeciesRow() {

        final Record species1 = createTestRecord(1);

        mockery.checking(new Expectations() {{
            oneOf(mockRecordDAO).getRecord(1);
            will(returnValue(species1));

        }});

        Map<String, MultipartFile> fileMap = new LinkedHashMap<String, MultipartFile>();
        String[] rowIds = {"0_", "1_"};
        Map<String, String[]> paramMap = new LinkedHashMap<String, String[]>();

        addSpeciesRecordParams(paramMap, new String[]{Integer.toString(species1.getId()), ""});
        int species1ChildRecordId = 2;
        addCensusMethodAttr("Test", species1ChildRecordId, paramMap, fileMap);
        int species1RowChildRecordId = 4;
        addRowCensusMethodAttr("Test row", species1RowChildRecordId, paramMap);

        recordEntryTransformer = new SingleSiteFormToRecordEntryTransformer(mockRecordDAO);
        List<RecordEntry> entries = recordEntryTransformer.httpRequestParamToRecordMap(paramMap, fileMap, rowIds);

        mockery.assertIsSatisfied();

        Assert.assertEquals(2, entries.size());

        // Make sure the primary record's record entry is unchanged.
        Assert.assertEquals(paramMap, entries.get(0).getDataMap());
        Assert.assertEquals(fileMap, entries.get(0).getFileMap());

        // Now test the second record's record entry has been modified appropriately.
        Map<String, String[]> species2Params = entries.get(1).getDataMap();
        Map<String, MultipartFile> species2Files = entries.get(1).getFileMap();

        Assert.assertEquals(fileMap.size(), species2Files.size());

        Assert.assertEquals("0_", species2Params.get("attribute_10_rowPrefix")[0]);
        Assert.assertEquals("0", species2Params.get("0_attribute_10_recordId")[0]);

        Assert.assertEquals("Test", species2Params.get("0_attribute_10_record_attribute_20")[0]);
        Assert.assertEquals("0", species2Params.get("0_attribute_10_record_rowIndex")[0]);


        // Now the nested census method
        Assert.assertEquals("0", species2Params.get("0_attribute_10_record_0_attribute_30_recordId")[0]);
        Assert.assertEquals("Test nested", species2Params.get("0_attribute_10_record_0_attribute_30_record_attribute_40")[0]);

        // Now the row census method
        Assert.assertEquals("0", species2Params.get("attribute_50_recordId")[0]);
        Assert.assertEquals("Test row", species2Params.get("attribute_50_record_attribute_60")[0]);

    }

    @Test
    public void testEditCensusMethodAttributesAddCensusMethodRow() {

        final Record species1 = createTestRecord(1);
        final Record species2 = createTestRecord(5);

        mockery.checking(new Expectations() {{
            oneOf(mockRecordDAO).getRecord(1);
            will(returnValue(species1));

            oneOf(mockRecordDAO).getRecord(5);
            will(returnValue(species2));

        }});

        Map<String, MultipartFile> fileMap = new LinkedHashMap<String, MultipartFile>();
        String[] rowIds = {"0_", "1_"};
        Map<String, String[]> paramMap = new LinkedHashMap<String, String[]>();

        addSpeciesRecordParams(paramMap, new String[]{Integer.toString(species1.getId()), Integer.toString(species2.getId())});
        int species1ChildRecordId = 2;
        addCensusMethodAttr("Test", species1ChildRecordId, paramMap, fileMap);
        int species1RowChildRecordId = 4;
        addRowCensusMethodAttr("Test row", species1RowChildRecordId, paramMap);

        addNewCensusMethodRow("New Test", paramMap);

        recordEntryTransformer = new SingleSiteFormToRecordEntryTransformer(mockRecordDAO);
        List<RecordEntry> entries = recordEntryTransformer.httpRequestParamToRecordMap(paramMap, fileMap, rowIds);

        mockery.assertIsSatisfied();

        Assert.assertEquals(2, entries.size());

        // Make sure the primary record's record entry is unchanged.
        Assert.assertEquals(paramMap, entries.get(0).getDataMap());
        Assert.assertEquals(fileMap, entries.get(0).getFileMap());

        // Now test the second record's record entry has been modified appropriately.
        Map<String, String[]> species2Params = entries.get(1).getDataMap();
        Map<String, MultipartFile> species2Files = entries.get(1).getFileMap();

        Assert.assertEquals(fileMap.size(), species2Files.size());

        Assert.assertEquals("0_attribute_10_record_6_", species2Params.get("attribute_10_rowPrefix")[0]);
        Assert.assertEquals("1_", species2Params.get("attribute_10_rowPrefix")[1]);

        Assert.assertEquals("0", species2Params.get("1_attribute_10_recordId")[0]);

        Assert.assertEquals("New Test", species2Params.get("1_attribute_10_record_attribute_20")[0]);

        // Now the nested census method
        Assert.assertEquals("0_", species2Params.get("1_attribute_10_record_attribute_30_rowPrefix")[0]);
        Assert.assertEquals("0", species2Params.get("1_attribute_10_record_0_attribute_30_recordId")[0]);
        Assert.assertEquals("0", species2Params.get("1_attribute_10_record_0_attribute_30_rowIndex")[0]);

        Assert.assertEquals("New Test nested", species2Params.get("1_attribute_10_record_0_attribute_30_record_attribute_40")[0]);
    }

    private void addSpeciesRecordParams(Map<String, String[]> params, String[] recordIds) {
        for (int i = 0; i < recordIds.length; i++) {
            params.put(i + "_recordId", new String[]{recordIds[i]});
        }
    }

    private String addCensusMethodAttr(String value, int recordId, Map<String, String[]> params, Map<String, MultipartFile> files) {
        // Actually need all rows here.
        params.put("attribute_" + censusMethodAttr.getId() + "_rowPrefix", new String[]{"0_attribute_" + censusMethodAttr.getId() + "_record_" + recordId + "_"});

        String attrPrefix = "0_attribute_" + censusMethodAttr.getId() + "_record_" + recordId;
        params.put("0_attribute_" + censusMethodAttr.getId() + "_recordId", new String[]{Integer.toString(recordId)});
        params.put(attrPrefix + "_rowIndex", new String[]{"0"});
        params.put(attrPrefix + "_recordId", new String[]{Long.toString(recordId)});
        params.put(attrPrefix + "_attribute_" + nestedTextAttribute.getId(), new String[]{value});

        // Now the nested census method
        int childRecordId = recordId + 1;
        params.put(attrPrefix + "_attribute_" + nestedCensusMethod.getId() + "_rowPrefix", new String[]{attrPrefix + "_0_attribute_" + nestedCensusMethod.getId() + "_record_" + childRecordId + "_"});
        params.put(attrPrefix + "_0_attribute_" + nestedCensusMethod.getId() + "_recordId", new String[]{Integer.toString(recordId + 1)});

        params.put(attrPrefix + "_0_attribute_" + nestedCensusMethod.getId() + "_record_" + childRecordId + "_rowIndex", new String[]{"0"});

        // Now the nested census method attributes
        params.put(attrPrefix + "_0_attribute_" + nestedCensusMethod.getId() + "_record_" + childRecordId + "_attribute_" + doubleNestedTextAttribute.getId(), new String[]{value + " nested"});
        files.put(attrPrefix + "_0_attribute_" + nestedCensusMethod.getId() + "_record_" + childRecordId + "_attribute_file_"+fileAttribute.getId(), new MockMultipartFile("file.txt", new byte[0]));
        return attrPrefix;
    }

    private String addRowCensusMethodAttr(String value, int recordId, Map<String, String[]> params) {
        String rowAttrPrefix = "attribute_" + rowCensusMethod.getId() + "_record_" + recordId;
        params.put("attribute_" + rowCensusMethod.getId() + "_recordId", new String[]{Long.toString(recordId)});
        params.put(rowAttrPrefix + "_recordId", new String[]{Long.toString(recordId)});
        params.put(rowAttrPrefix + "_attribute_" + nestedRowTextAttribute.getId(), new String[]{value});
        return rowAttrPrefix;
    }

    private void addNewCensusMethodRow(String value, Map<String, String[]> params) {
        // We know there is an existing entry for this param.
        String[] existingCmRow = params.get("attribute_"+censusMethodAttr.getId()+"_rowPrefix");
        params.put("attribute_"+censusMethodAttr.getId()+"_rowPrefix", new String[] {existingCmRow[0], "1_"});
        String prefix = "1_attribute_"+censusMethodAttr.getId()+"_record_";
        params.put("1_attribute_"+censusMethodAttr.getId()+"_recordId", new String[] {"0"});
        params.put("1_attribute_"+censusMethodAttr.getId()+"_rowIndex", new String[] {"0"});
        params.put(prefix+"attribute_"+nestedTextAttribute.getId(), new String[] {value});

        // Now the nested census method attribute.
        params.put(prefix+"attribute_"+nestedCensusMethod.getId()+"_rowPrefix", new String[]{"0_"});
        params.put(prefix+"0_attribute_"+nestedCensusMethod.getId()+"_recordId", new String[] {"0"});
        params.put(prefix+"0_attribute_"+nestedCensusMethod.getId()+"_rowIndex", new String[] {"0"});
        params.put(prefix+"0_attribute_"+nestedCensusMethod.getId()+"_record_attribute_"+doubleNestedTextAttribute.getId(), new String[] {value+" nested"});

    }
}
