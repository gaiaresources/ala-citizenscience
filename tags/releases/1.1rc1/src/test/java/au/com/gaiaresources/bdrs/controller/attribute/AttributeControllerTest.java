package au.com.gaiaresources.bdrs.controller.attribute;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.threshold.Operator;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;
import au.com.gaiaresources.bdrs.security.Role;

public class AttributeControllerTest extends AbstractGridControllerTest {

    @Autowired
    private ThresholdDAO thresholdDAO;
    
    @Test
    public void testAjaxAddAttribute() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        // Attempt 3 requests
        for (int i = 0; i < 3; i++) {
            for(Boolean showScope : new Boolean[]{ true, false }) {
                for(Boolean isTag : new Boolean[]{ true, false}) {
                    request.setMethod("GET");
                    request.setRequestURI("/bdrs/admin/attribute/ajaxAddAttribute.htm");
                    request.setParameter("index", String.valueOf(i));
                    request.setParameter("showScope", showScope.toString());
                    request.setParameter("isTag", isTag.toString());
        
                    ModelAndView mv = handle(request, response);
                    ModelAndViewAssert.assertViewName(mv, "attributeRow");
                    ModelAndViewAssert.assertModelAttributeAvailable(mv, "formField");
                    ModelAndViewAssert.assertModelAttributeAvailable(mv, "showScope");
                    ModelAndViewAssert.assertModelAttributeAvailable(mv, "index");
        
                    Assert.assertEquals(i, Integer.parseInt(mv.getModelMap().get("index").toString()));
                    AttributeFormField formField = (AttributeFormField) mv.getModelMap().get("formField");
                    Assert.assertTrue(formField.isAttributeField());
                    
                    Assert.assertTrue(showScope.equals(Boolean.parseBoolean(mv.getModelMap().get("showScope").toString())));
                    Assert.assertTrue(isTag.equals(Boolean.parseBoolean(mv.getModelMap().get("isTag").toString())));
        
                    AttributeInstanceFormField attrFormField = (AttributeInstanceFormField) formField;
                    Assert.assertNull(attrFormField.getAttribute());
                    Assert.assertEquals(String.format("add_weight_%d", i), attrFormField.getWeightName());
                    Assert.assertEquals(0, attrFormField.getWeight());
                }
            }
        }
    }
    
    /**
     * Test that controller returns true when a threshold exists for the attribute 
     * specifications given by the page and false otherwise.
     * @throws Exception 
     */
    @Test
    public void testAjaxCheckAttribute() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        // create a threshold to match on
        createThreshold(Record.class.getCanonicalName(), 1, 
                        "survey.attributes.scope",
                        Operator.CONTAINS, 
                        new String[]{
                                AttributeScope.RECORD_MODERATION.toString(),
                                AttributeScope.SURVEY_MODERATION.toString()
                        });
        
        // create an attribute that matches
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/attribute/ajaxCheckThresholdForAttribute.htm");
        request.setParameter("description", "Test Attribute");
        request.setParameter("name", "test_attr");
        request.setParameter("typeCode", AttributeType.TEXT.getCode());
        request.setParameter("scopeCode", AttributeScope.SURVEY_MODERATION.toString());
        request.setParameter("surveyId", survey1.getId().toString());

        handle(request, response);
        String resContent = response.getContentAsString();
        System.out.println(resContent);
        Assert.assertTrue(Boolean.valueOf(resContent));
        
        // create an attribute that doesn't match
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/attribute/ajaxCheckThresholdForAttribute.htm");
        request.setParameter("description", "Test Attribute");
        request.setParameter("name", "test_attr");
        request.setParameter("typeCode", AttributeType.TEXT.getCode());
        request.setParameter("scopeCode", AttributeScope.SURVEY.toString());
        request.setParameter("surveyId", survey1.getId().toString());

        handle(request, response);
        resContent = response.getContentAsString();
        System.out.println(resContent);
        Assert.assertFalse(Boolean.valueOf(resContent));
    }
}
