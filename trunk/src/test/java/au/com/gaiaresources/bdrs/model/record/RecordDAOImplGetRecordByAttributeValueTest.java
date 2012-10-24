package au.com.gaiaresources.bdrs.model.record;

import java.util.ArrayList;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;

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
