package au.com.gaiaresources.bdrs.service.bulkdata;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests the RecordCensusMethodHelper class.
 */
public class RecordCensusMethodHelperTest extends TestCase {


    private Attribute censusMethodAttr;
    private Attribute nestedCensusMethod;
    private Attribute rowCensusMethod;
    private Record parent;

    public void setUp() {
        parent = new Record();
        parent.setId(1);
        censusMethodAttr = createAttribute(10, AttributeType.CENSUS_METHOD_COL);
        nestedCensusMethod = createAttribute(20, AttributeType.CENSUS_METHOD_COL);
        rowCensusMethod = createAttribute(30, AttributeType.CENSUS_METHOD_ROW);
    }

    private AttributeValue addValue(Attribute attribute, Record parent) {
        AttributeValue value = new AttributeValue();
        value.setAttribute(attribute);
        parent.getAttributes().add(value);
        return value;
    }

    private Record addChild(AttributeValue value, int id) {
        Record child = new Record();
        child.setId(id);
        Set<Record> children = value.getRecords();
        if (children == null) {
            children = new HashSet<Record>();
            value.setRecords(children);
        }
        children.add(child);
        return child;
    }

    private Attribute createAttribute(int id, AttributeType type) {

        Attribute attr = new Attribute();
        attr.setId(id);
        attr.setScope(AttributeScope.SURVEY);
        attr.setTypeCode(type.getCode());

        return attr;
    }

    @Test
    public void testRecordWithNoCensusMethodAttributes() {
        RecordCensusMethodHelper helper = new RecordCensusMethodHelper();
        List<List<Record>> records = helper.cartesianProduct(parent);

        Assert.assertEquals(0, records.size());
    }

    @Test
    public void testCartesianProduct() {

        int recordId = parent.getId()+1;
        AttributeValue censusMethodValue = addValue(censusMethodAttr, parent);
        Record child = addChild(censusMethodValue, recordId++);

        AttributeValue nestedCensusMethodValue = addValue(nestedCensusMethod, child);
        addChild(nestedCensusMethodValue, recordId++);

        AttributeValue rowCensusMethodValue = addValue(rowCensusMethod, parent);
        addChild(rowCensusMethodValue, recordId++);

        RecordCensusMethodHelper helper = new RecordCensusMethodHelper();

        List<List<Record>> records = helper.cartesianProduct(parent);

        printResult(records);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(3, records.get(0).size());

        int[] expectedResults = {2,3,4};
        checkLists(records, expectedResults);


    }

    @Test
    public void testAnotherCartesianProduct() {

        int recordId = parent.getId()+1;
        AttributeValue censusMethodValue = addValue(censusMethodAttr, parent);
        Record child = addChild(censusMethodValue, recordId++);
        addChild(censusMethodValue, recordId++);

        AttributeValue nestedCensusMethodValue = addValue(nestedCensusMethod, child);
        addChild(nestedCensusMethodValue, recordId++);

        AttributeValue rowCensusMethodValue = addValue(rowCensusMethod, parent);
        addChild(rowCensusMethodValue, recordId++);

        RecordCensusMethodHelper helper = new RecordCensusMethodHelper();

        List<List<Record>> records = helper.cartesianProduct(parent);

        printResult(records);

        Assert.assertEquals(2, records.size());
        checkLists(records, new int[] {2,4,5});
        checkLists(records, new int[] {3,5});

    }

    @Test
    public void testYetAnotherCartesianProduct() {

        int recordId = parent.getId()+1;
        AttributeValue censusMethodValue = addValue(censusMethodAttr, parent);
        Record child = addChild(censusMethodValue, recordId++);
        addChild(censusMethodValue, recordId++);

        AttributeValue nestedCensusMethodValue = addValue(nestedCensusMethod, child);
        addChild(nestedCensusMethodValue, recordId++);
        addChild(nestedCensusMethodValue, recordId++);

        AttributeValue rowCensusMethodValue = addValue(rowCensusMethod, parent);
        addChild(rowCensusMethodValue, recordId++);


        RecordCensusMethodHelper helper = new RecordCensusMethodHelper();

        List<List<Record>> records = helper.cartesianProduct(parent);

        printResult(records);

        Assert.assertEquals(3, records.size());
        checkLists(records, new int[] {2,4,6});
        checkLists(records, new int[] {2,5,6});
        checkLists(records, new int[] {3,6});

    }

    /**
     * Because we are working with sets we can't guarantee order - hence we just check there is one list
     * that matches our expected values.
     */
    private void checkLists(List<List<Record>> allRecords, int[] recordIds) {
        boolean found = false;

        for (List<Record> records : allRecords) {
            found = checkList(records, recordIds);
            if (found) {
                break;
            }
        }
        StringBuilder expected = new StringBuilder("[");
        for (int i=0; i<recordIds.length; i++) {
            expected.append(recordIds[i]);
            if (i < recordIds.length-1) {
                expected.append(",");
            }
        }
        expected.append("]");
        Assert.assertTrue("List contains a set of records matching: "+ expected, found);

    }

    private boolean checkList(List<Record> records, int[] recordIds) {

        for (int i=0; i<recordIds.length; i++) {
            boolean found = false;
            for (Record record : records) {
                if (record.getId().intValue() == recordIds[i]) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void printResult(List<List<Record>> results) {
        System.out.println("==============================");
        for (int i=0; i<results.size(); i++) {
            List<Record> records2 = results.get(i);
            System.out.print("[");
            for (int j=0; j<records2.size(); j++) {
                System.out.print(records2.get(j).getId());
                if (j<records2.size()-1) {
                    System.out.print(", ");
                }
                else {
                    System.out.print("]");
                }

            }
            System.out.println();
        }

    }


}
