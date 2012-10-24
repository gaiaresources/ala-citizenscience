package au.com.gaiaresources.bdrs.model.record;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;

public class RecordDAOImplGetBySurveySpeciesTest extends
        AbstractGridControllerTest {

    @Test
    public void testGetBySurveySpecies() {
        
        List<Record> result = recDAO.getRecordBySurveySpecies(survey1.getId(), dropBear.getId());
        Assert.assertTrue("expect item", result.contains(r1));
        Assert.assertTrue("expect item", result.contains(r8));
        Assert.assertFalse("dont expect item", result.contains(r2));
        Assert.assertFalse("dont expect item", result.contains(r3));
        Assert.assertFalse("dont expect item", result.contains(r4));
        Assert.assertFalse("dont expect item", result.contains(r5));
        Assert.assertFalse("dont expect item", result.contains(r6));
        Assert.assertFalse("dont expect item", result.contains(r7));
    }
}
