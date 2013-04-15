package au.com.gaiaresources.bdrs.service.bulkdata;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class used to flatten out a Record containing nested census method attributes into a form suitable for
 * presentation in a spreadsheet.
 */
public class RecordCensusMethodHelper {

    /**
     * Given a Record with Census Method Attributes this method returns a list of list of Records such that
     * each distinct combination of Records (each containing data from a row in a census method attribute) is represented.
     * @param record the Record of interest.
     * @return
     */
    public List<List<Record>> cartesianProduct(Record record) {

        // First up get the CensusMethodAttributes.
        List<AttributeValue> censusMethodAttributes = getCensusMethodAttributes(record);
        List<List<Record>> results = new ArrayList<List<Record>>();

        getProduct(results, censusMethodAttributes, new ArrayList<Record>());

        return results;
    }

    private void getProduct(List<List<Record>> results, List<AttributeValue> values, List<Record> currentResult) {

        if (!values.isEmpty()) {
            List<List<List<Record>>> recordValues = new ArrayList<List<List<Record>>>();
            for (AttributeValue value: values) {
                recordValues.add(getProductDepthWise(value));
            }
            getProduct(results, recordValues, 0, currentResult);
        }
    }

    private void getProduct(List<List<Record>> results, List<List<List<Record>>> allRecords, int listIndex, List<Record> currentResult) {

        List<List<Record>> recordList = allRecords.get(listIndex);
        for (List<Record> records : recordList) {
            currentResult.addAll(records);
            if (listIndex < allRecords.size() - 1) {
                getProduct(results, allRecords, listIndex +1, currentResult);
            }
            else {
                results.add(currentResult);
                currentResult = new ArrayList<Record>(currentResult.subList(0, currentResult.size()-records.size()));
            }
        }
    }

    private List<List<Record>> getProductDepthWise(AttributeValue value) {
        List<List<Record>> results = new ArrayList<List<Record>>();

        for (Record record : value.getOrderedRecords()) {
            List<Record> current = new ArrayList<Record>();
            getProduct(results, record, current);
        }
        return results;
    }

    private void getProduct(List<List<Record>> results, Record record, List<Record> currentResult) {

        currentResult.add(record);
        List<AttributeValue> values = getCensusMethodAttributes(record);
        if (values.size() == 0) {
            results.add(currentResult);
        }
        else {
            getProduct(results, values, currentResult);
        }

    }

    private List<AttributeValue> getCensusMethodAttributes(Record record) {
        List<AttributeValue> censusMethodAttributes = new ArrayList<AttributeValue>();
        for (AttributeValue value : record.getAttributes()) {
            if (AttributeType.isCensusMethodType(value.getAttribute().getType())) {
                if (value.getRecords() != null && value.getRecords().size() > 0) {
                    censusMethodAttributes.add(value);
                }
            }
        }
        return censusMethodAttributes;
    }
}
