package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordDAOImplGetRecordByAttributeValueTest extends
        AbstractGridControllerTest {

    @Test
    public void testQuery() {
        Attribute attr = null;
        for (Attribute a : survey1.getAttributes()) {
            if (a.getType() == AttributeType.STRING) {
                attr = a;
            }
        }
        Assert.assertNotNull("must have a attr", attr);
        
        String val = null;
        for (AttributeValue av : r1.getAttributes()) {
            if (av.getAttribute().equals(attr)) {
                val = av.getStringValue();
            }
        }
        Assert.assertNotNull("value cannot be null", val);
        
        List<Record> result = recDAO.getRecordByAttributeValue(null, survey1.getId(), attr.getName(), val);
        
        List<Record> expectedResult = getExpectedResult(survey1, attr, val);
        
        Assert.assertFalse("must contain some records", result.isEmpty());
        
        Assert.assertEquals("wrong size", expectedResult.size(), result.size());
        
        for (Record r : result) {
            boolean recPassed = false;
            for (AttributeValue av : r.getAttributes()) {
                if (av.getAttribute().equals(attr)) {
                    Assert.assertEquals("wrong attr val", val, av.getStringValue());
                    recPassed = true;
                }
            }
            Assert.assertTrue("rec did not contain attr val", recPassed);
        }
    }


    @Test
    public void testQueryWithStartDateOnly() {
        Attribute attr = null;
        for (Attribute a : survey1.getAttributes()) {
            if (a.getType() == AttributeType.STRING) {
                attr = a;
            }
        }
        Assert.assertNotNull("must have a attr", attr);

        String val = null;
        for (AttributeValue av : r1.getAttributes()) {
            if (av.getAttribute().equals(attr)) {
                val = av.getStringValue();
            }
        }
        Assert.assertNotNull("value cannot be null", val);

        // In the parent class, 9 records are created such that they have dates 2010-09-21 through to 2010-09-29

        Date start = getDate(2010, 9, 24);

        List<Record> result = recDAO.findRecordsByAttributeValue(null, survey1.getId(), attr.getName(), val, start, null);

        List<Record> allWithNameAndValue = getExpectedResult(survey1, attr, val);
        List<Record> expected = new ArrayList<Record>();
        for (Record record : allWithNameAndValue) {
            if (record.getWhen().equals(start) || record.getWhen().after(start)) {
                expected.add(record);
            }
        }

        Assert.assertEquals(expected.size(), result.size());
        for (Record record : expected) {
            Assert.assertTrue(result.contains(record));
        }
    }

    @Test
    public void testQueryWithEndDateOnly() {
        Attribute attr = null;
        for (Attribute a : survey1.getAttributes()) {
            if (a.getType() == AttributeType.STRING) {
                attr = a;
            }
        }
        Assert.assertNotNull("must have a attr", attr);

        String val = null;
        for (AttributeValue av : r1.getAttributes()) {
            if (av.getAttribute().equals(attr)) {
                val = av.getStringValue();
            }
        }
        Assert.assertNotNull("value cannot be null", val);

        // In the parent class, 9 records are created such that they have dates 2010-09-21 through to 2010-09-29

        Date end = getDate(2010, 9, 24);

        List<Record> result = recDAO.findRecordsByAttributeValue(null, survey1.getId(), attr.getName(), val, null, end);

        List<Record> allWithNameAndValue = getExpectedResult(survey1, attr, val);
        List<Record> expected = new ArrayList<Record>();
        for (Record record : allWithNameAndValue) {
            if (record.getWhen().equals(end) || record.getWhen().before(end)) {
                expected.add(record);
            }
        }

        Assert.assertEquals(expected.size(), result.size());
        for (Record record : expected) {
            Assert.assertTrue(result.contains(record));
        }
    }

    @Test
    public void testQueryWithStartAndEndDate() {
        Attribute attr = null;
        for (Attribute a : survey1.getAttributes()) {
            if (a.getType() == AttributeType.STRING) {
                attr = a;
            }
        }
        Assert.assertNotNull("must have a attr", attr);

        String val = null;
        for (AttributeValue av : r1.getAttributes()) {
            if (av.getAttribute().equals(attr)) {
                val = av.getStringValue();
            }
        }
        Assert.assertNotNull("value cannot be null", val);

        // In the parent class, 9 records are created such that they have dates 2010-09-21 through to 2010-09-29

        Date start = getDate(2010, 9, 24);
        Date end = getDate(2010, 9, 24);

        List<Record> result = recDAO.findRecordsByAttributeValue(null, survey1.getId(), attr.getName(), val, start, end);

        List<Record> allWithNameAndValue = getExpectedResult(survey1, attr, val);
        List<Record> expected = new ArrayList<Record>();
        for (Record record : allWithNameAndValue) {
            if ((record.getWhen().equals(end) || record.getWhen().before(end)) &&
                    (record.getWhen().equals(start) || record.getWhen().after(start))) {
                expected.add(record);
            }
        }

        Assert.assertEquals(expected.size(), result.size());
        for (Record record : expected) {
            Assert.assertTrue(result.contains(record));
        }
    }

    List<Record> getExpectedResult(Survey survey, Attribute attr, String val) {
        List<Record> result = new ArrayList<Record>();
        List<Record> records = recDAO.search(null, survey.getId(), null).getList();
        for (Record r : records) {
            for (AttributeValue av : r.getAttributes()) {
                if (av.getAttribute().equals(attr)) {
                    if (av.getStringValue().equals(val)) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }
}
