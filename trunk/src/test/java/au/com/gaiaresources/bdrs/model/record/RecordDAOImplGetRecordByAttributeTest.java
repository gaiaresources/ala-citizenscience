package au.com.gaiaresources.bdrs.model.record;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;

public class RecordDAOImplGetRecordByAttributeTest extends
        AbstractGridControllerTest {

    private Attribute attrUnderTest;
    
    @Before
    public void setup() {        
        for (Attribute a : survey1.getAttributes()) {
            if (a.getType() == AttributeType.STRING) {
                attrUnderTest = a;
            }
        }
    }
    
    @Test
    public void testQuery() {
        Attribute attr = attrUnderTest;
        Assert.assertNotNull("must have a attr", attr);
        
        List<Record> result = recDAO.getRecordByAttribute(null, currentUser.getId(), survey1.getId(), attr.getId());
        
        List<Record> expectedResult = getExpectedResult(survey1, attr, currentUser);
        
        Assert.assertFalse("must contain some records", result.isEmpty());
        
        Assert.assertEquals("wrong size", expectedResult.size(), result.size());
        
        for (Record r : result) {
            boolean recPassed = false;
            for (AttributeValue av : r.getAttributes()) {
                if (av.getAttribute().equals(attr)) {
                    recPassed = true;
                }
            }
            Assert.assertTrue("rec did not contain attr val", recPassed);
        }
    }
    
    @Test
    public void testQueryWrongUser() {
        Attribute attr = attrUnderTest;
        Assert.assertNotNull("must have a attr", attr);
        
        List<Record> result = recDAO.getRecordByAttribute(null, poweruser.getId(), survey1.getId(), attr.getId());
        Assert.assertTrue("expect empty result", result.isEmpty());
    }
    
    @Test
    public void testQueryWrongSurvey() {
        Attribute attr = attrUnderTest;
        Assert.assertNotNull("must have a attr", attr);
        
        List<Record> result = recDAO.getRecordByAttribute(null, poweruser.getId(), survey2.getId(), attr.getId());
        Assert.assertTrue("expect empty result", result.isEmpty());
    }
    
    List<Record> getExpectedResult(Survey survey, Attribute attr, User user) {
        List<Record> result = new ArrayList<Record>();
        List<Integer> userIdList = new ArrayList<Integer>(1);
        userIdList.add(user.getId());
        List<Record> records = recDAO.search(null, survey.getId(), userIdList).getList();
        for (Record r : records) {
            for (AttributeValue av : r.getAttributes()) {
                if (av.getAttribute().equals(attr)) {
                    result.add(r);
                }
            }
        }
        return result;
    }
}
