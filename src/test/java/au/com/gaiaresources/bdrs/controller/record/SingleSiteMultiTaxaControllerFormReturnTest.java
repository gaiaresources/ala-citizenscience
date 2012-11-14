package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.TypedAttributeValueFormField;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedCountRecordFilter;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.security.Role;

public class SingleSiteMultiTaxaControllerFormReturnTest extends
        AbstractGridControllerTest {
    
    @Test
    public void openFormWithExistingRecordsTest() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Survey survey = this.singleSiteMultiTaxaSurvey;
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        Record refRecord2 = surveyRecList.get(1);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_RECORD_ID, refRecord1.getId().toString());
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, refRecord1.getSurvey().getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        Assert.assertEquals("Only expect 2 items", 2, rffcList.size());

        // these ref items are created in AbstractGridControllerTest
        
        RecordFormFieldCollection rf1 = getByRecordId(refRecord1.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 1 in the list", rf1);
        RecordFormFieldCollection rf2 = getByRecordId(refRecord2.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 2 in the list", rf2);
        
        Assert.assertTrue("item 1 should be highlighted", rf1.isHighlight());
        Assert.assertFalse("item 2 should not be highlighted", rf2.isHighlight());

        // check record scoped values ...
        List<AttributeScope> RECORD_SCOPES = new ArrayList<AttributeScope>();
        RECORD_SCOPES.add(AttributeScope.RECORD);
        RECORD_SCOPES.add(AttributeScope.RECORD_MODERATION);
        this.assertFormFieldList(rf1.getFormFields(), refRecord1, true, RECORD_SCOPES);
        this.assertFormFieldList(rf2.getFormFields(), refRecord2, true, RECORD_SCOPES);
        
        List<FormField> formFieldList = formContext.getNamedFormFields().get(SingleSiteController.MODEL_SURVEY_FORM_FIELD_LIST);
        
        // check survey scoped attribute values and record properties
        List<AttributeScope> SURVEY_SCOPES = new ArrayList<AttributeScope>();
        SURVEY_SCOPES.add(AttributeScope.SURVEY);
        SURVEY_SCOPES.add(AttributeScope.SURVEY_MODERATION);
        assertFormFieldList(formFieldList, refRecord1, true, SURVEY_SCOPES);
        
        // check the list used to generate the sightings table header (Record scope)
        List<FormField> tableHeaderList = formContext.getNamedFormFields().get(SingleSiteController.MODEL_SIGHTING_ROW_LIST);
        Assert.assertNotNull("table header form field list should exist in model", tableHeaderList);
        
        assertFormFieldList(tableHeaderList, refRecord1, false, RECORD_SCOPES);
    }
    
    @Test
    public void openFormWithNewRecordScopedAttributesAddedSinceRecCreation() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Survey survey = this.singleSiteMultiTaxaSurvey;
        
        Attribute newRecordScopedAttr = this.createAttribute("rec scoped attr", AttributeType.INTEGER, false, AttributeScope.RECORD, false);
        List<Attribute> newSurveyAttrList = new ArrayList<Attribute>();
        newSurveyAttrList.addAll(survey.getAttributes());
        newSurveyAttrList.add(newRecordScopedAttr);
        survey.setAttributes(newSurveyAttrList);
        
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        Record refRecord2 = surveyRecList.get(1);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_RECORD_ID, refRecord1.getId().toString());
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, refRecord1.getSurvey().getId().toString());
        
        ModelAndView mv = this.handle(request, response);

        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        Assert.assertEquals("Only expect 2 items", 2, rffcList.size());

        // these ref items are created in AbstractGridControllerTest
        
        RecordFormFieldCollection rf1 = getByRecordId(refRecord1.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 1 in the list", rf1);
        RecordFormFieldCollection rf2 = getByRecordId(refRecord2.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 2 in the list", rf2);
        
        Assert.assertTrue("item 1 should be highlighted", rf1.isHighlight());
        Assert.assertFalse("item 2 should not be highlighted", rf2.isHighlight());

        // check record scoped values ...
        List<AttributeScope> RECORD_SCOPES = new ArrayList<AttributeScope>();
        RECORD_SCOPES.add(AttributeScope.RECORD);
        RECORD_SCOPES.add(AttributeScope.RECORD_MODERATION);
        this.assertFormFieldList(rf1.getFormFields(), refRecord1, true, RECORD_SCOPES);
        this.assertFormFieldList(rf2.getFormFields(), refRecord2, true, RECORD_SCOPES);
        
        List<FormField> formFieldList = formContext.getNamedFormFields().get(SingleSiteController.MODEL_SURVEY_FORM_FIELD_LIST);
        
        // check survey scoped attribute values and record properties
        List<AttributeScope> SURVEY_SCOPES = new ArrayList<AttributeScope>();
        SURVEY_SCOPES.add(AttributeScope.SURVEY);
        SURVEY_SCOPES.add(AttributeScope.SURVEY_MODERATION);
        assertFormFieldList(formFieldList, refRecord1, true, SURVEY_SCOPES);
        
        // check the list used to generate the sightings table header (Record scope)
        List<FormField> tableHeaderList = formContext.getNamedFormFields().get(SingleSiteController.MODEL_SIGHTING_ROW_LIST);
        Assert.assertNotNull("table header form field list should exist in model", tableHeaderList);
        
        assertFormFieldList(tableHeaderList, refRecord1, false, RECORD_SCOPES);
    }
    
    @Test
    public void openFormWithNoRecord() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Survey survey = this.singleSiteMultiTaxaSurvey;
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, refRecord1.getSurvey().getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        Assert.assertEquals("expect an empty list", 0, rffcList.size());
    }
    
    @Test
    public void editExistingSingleSiteMultiTaxaFormSetLocation() throws Exception {
        editExistingSingleSiteMultiTaxaForm(false, true);
    }
    
    @Test
    public void editExistingSingleSiteMultiTaxaFormRemoveLocation() throws Exception {
        editExistingSingleSiteMultiTaxaForm(true, false);
    }
    
    @Test
    public void editExistingSingleSiteMultiTaxaFormMaintainLocation() throws Exception {
        editExistingSingleSiteMultiTaxaForm(true, true);
    }
    
    @Test
    public void editExistingSingleSiteMultiTaxaFormDontUseLocation() throws Exception {
        editExistingSingleSiteMultiTaxaForm(false, false);
    }
    
    private void editExistingSingleSiteMultiTaxaForm(boolean startWithLocation, boolean setLocation) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Survey survey = this.singleSiteMultiTaxaSurvey;
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        Record refRecord2 = surveyRecList.get(1);
        
        if (startWithLocation) {
            refRecord1.setLocation(locationList.get(0));
            refRecord2.setLocation(locationList.get(0));
        }
        
        AdvancedCountRecordFilter filter = new AdvancedCountRecordFilter();
        filter.setSurveyPk(refRecord1.getSurvey().getId());
        int surveyRecordCount = recordDAO.countRecords(filter);
        
        int avCount = refRecord1.getAttributes().size();
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("POST");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, refRecord1.getSurvey().getId().toString());
        
        FilterManager.disablePartialRecordCountFilter(getSession());
        int seed = 2;
        Map<Attribute, Object> newSurveyScopedAttributeValues = new HashMap<Attribute, Object>();
        Map<RecordPropertyType, String> newSurveyScopedRecPropValues = new HashMap<RecordPropertyType, String>();
        this.addSurveyScopedItemsToPostMap(refRecord1, request, seed++, newSurveyScopedAttributeValues, newSurveyScopedRecPropValues, setLocation);
        
        int index = 0;
        Map<Attribute, Object> newRecordScopedAttributeValues = new HashMap<Attribute, Object>();
        Map<RecordPropertyType, String> newRecordScopedRecPropValues = new HashMap<RecordPropertyType, String>();
        List<AttributeScope> RECORD_SCOPES = new ArrayList<AttributeScope>();
        RECORD_SCOPES.add(AttributeScope.RECORD);
        RECORD_SCOPES.add(AttributeScope.RECORD_MODERATION);
        this.addRecordScopedItemsToPostMap(refRecord1, request, index++, seed++, newRecordScopedAttributeValues, newRecordScopedRecPropValues, setLocation, RECORD_SCOPES);
        
        Map<Attribute, Object> newRecordScopedAttributeValues2 = new HashMap<Attribute, Object>();
        Map<RecordPropertyType, String> newRecordScopedRecPropValues2 = new HashMap<RecordPropertyType, String>();
        this.addRecordScopedItemsToPostMap(refRecord2, request, index++, seed++, newRecordScopedAttributeValues2, newRecordScopedRecPropValues2, setLocation, RECORD_SCOPES);
        
        request.addParameter(SingleSiteMultiTaxaController.PARAM_SIGHTING_INDEX, Integer.toString(index));
        FilterManager.setPartialRecordCountFilter(getSession());
        
        ModelAndView mv = this.handle(request, response);
        
        Assert.assertNotNull("model and view should not be null", mv);
        
        int newSurveyRecordCount = recordDAO.countRecords(filter);
        Assert.assertEquals("record count should not have changed", surveyRecordCount, newSurveyRecordCount);
        
        Assert.assertEquals("attribute value count should not have changed", avCount, refRecord1.getAttributes().size());
        
        // check survey scoped attribute values - note both records expect the same value
        assertRecordAttributeValue(refRecord1, newSurveyScopedAttributeValues);
        assertRecordAttributeValue(refRecord2, newSurveyScopedAttributeValues);
        
        // check survey scoped record properties - note both records expect the same value
        this.assertRecordPropertyValue(refRecord1, newSurveyScopedRecPropValues);
        this.assertRecordPropertyValue(refRecord2, newSurveyScopedRecPropValues);

        // check reference item 1 - note records have different expected values
        assertRecordAttributeValue(refRecord1, newRecordScopedAttributeValues);
        assertRecordPropertyValue(refRecord1, newRecordScopedRecPropValues);
        
        // check reference item 2 - note records have different expected values
        assertRecordAttributeValue(refRecord2, newRecordScopedAttributeValues2);
        assertRecordPropertyValue(refRecord2, newRecordScopedRecPropValues2);
    }
    
    // more so just to make sure there are no null pointer exceptions
    @Test
    public void testNullSurveyFormReturn() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Metadata md = nullSurvey.setFormRendererType(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
        metaDAO.save(md);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, nullRecord.getSurvey().getId().toString());
        request.setParameter(SingleSiteMultiTaxaController.PARAM_RECORD_ID, nullRecord.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        Assert.assertEquals("expect our one and only record in the return list", 1, rffcList.size());
    }
    
    @Test
    public void testSurveyScopedFormReturn() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        Survey survey = this.surveyScopedSurvey;
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        Record refRecord2 = surveyRecList.get(1);
        
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
        metaDAO.save(md);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(SingleSiteMultiTaxaController.PARAM_RECORD_ID, refRecord1.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        Assert.assertEquals("expect 2 items that match the record capture criteria", 2, rffcList.size());
        
        RecordFormFieldCollection rf1 = getByRecordId(refRecord1.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 1 in the list", rf1);
        RecordFormFieldCollection rf2 = getByRecordId(refRecord2.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 2 in the list", rf2);
        
        Assert.assertTrue("item 1 should be highlighted", rf1.isHighlight());
        Assert.assertFalse("item 2 should not be highlighted", rf2.isHighlight());
    }
    
    @Test
    public void testRecordScopedFormReturn() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        Survey survey = this.recordScopedSurvey;
        List<Record> surveyRecList = this.getInitialRecordList(survey);
        
        Record refRecord1 = surveyRecList.get(0);
        Record refRecord2 = surveyRecList.get(1);
        
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
        metaDAO.save(md);
        
        request.setRequestURI(SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
        request.setMethod("GET");
        request.setParameter(SingleSiteMultiTaxaController.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter(SingleSiteMultiTaxaController.PARAM_RECORD_ID, refRecord1.getId().toString());
        
        ModelAndView mv = this.handle(request, response);
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

        List<RecordFormFieldCollection> rffcList = formContext.getNamedCollections().get(SingleSiteController.MODEL_RECORD_ROW_LIST);
        Assert.assertNotNull("record form field collection list should exist in model", rffcList);
        
        // since there are no survey scoped attributes - we will actually match all the records in the survey.
        Assert.assertEquals("expect all records in survey to be included by the record capture criteria", surveyRecList.size(), rffcList.size());
        
        RecordFormFieldCollection rf1 = getByRecordId(refRecord1.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 1 in the list", rf1);
        RecordFormFieldCollection rf2 = getByRecordId(refRecord2.getId(), rffcList);
        Assert.assertNotNull("we expect to find ref item 2 in the list", rf2);
        
        Assert.assertTrue("item 1 should be highlighted", rf1.isHighlight());
        Assert.assertFalse("item 2 should not be highlighted", rf2.isHighlight());
    }
    
    /**
     * 
     * @param formFields
     * @param rec
     * @param expectFilledValues - whether we expect attribute values to be non null
     */
    private void assertFormFieldList(List<FormField> formFields, Record rec, boolean expectFilledValues, List<AttributeScope> scopeList) {
        
        List<RecordProperty> scopedRecordProperties = new ArrayList<RecordProperty>();
        List<Attribute> expectedAttributes = new ArrayList<Attribute>();
        for (AttributeScope scope  : scopeList) {
            scopedRecordProperties.addAll(this.getRecordProperty(rec.getSurvey(), scope));
            expectedAttributes.addAll(this.getAttributeByScope(rec.getSurvey(), scope));
        }
        
        
        int attrCount = 0;
        int propCount = 0;
        
        for (FormField ff : formFields) {
            if (ff.isAttributeFormField()) {
                Assert.assertTrue("assert before upcast", ff instanceof TypedAttributeValueFormField);
                TypedAttributeValueFormField tavff = (TypedAttributeValueFormField)ff;
                AttributeValue origAv = this.getAttributeValueByAttributeId(rec.getAttributes(), tavff.getAttribute().getId());
                TypedAttributeValue targetAv = tavff.getAttributeValue();
                
                Assert.assertTrue("check scope...", scopeList.contains(tavff.getAttribute().getScope()));
                
                if (expectFilledValues) {
                    // NOTE THIS DOESNT CHECK THE CONTENTS OF THE ATTRIBUTE VALUE ! we do however check that the object references
                    // are equal
                    
                    // if the attribute does not exist in the record's attribute list then we can't
                    // expect the attribute value to exist...
                    if (this.getAttributeValueByAttributeId(rec.getAttributes(), tavff.getAttribute().getId()) != null) {
                        Assert.assertNotNull("the attribute value must exist", origAv);
                        Assert.assertNotNull("the target TypedAttributeValue must exist", targetAv);    
                        Assert.assertEquals("attribute value refs should equal", origAv, targetAv);    
                    }
                } else {
                    // census methods can have an attribute value but yet not be filled in
                    if (!AttributeType.isCensusMethodType(tavff.getAttribute().getType())) {
                        Assert.assertNull("the target TypedAttributeValue should be null", targetAv);
                    }
                }
                
                // mark of the item as existing...
                expectedAttributes.remove(tavff.getAttribute());
                
                ++attrCount;
            } else if (ff.isDisplayFormField() && !ff.isPropertyFormField()) {
                // do nothing
            } else if (ff.isPropertyFormField()) {
                Assert.assertTrue("assert before upcast", ff instanceof RecordPropertyFormField);
                RecordPropertyFormField rpff = (RecordPropertyFormField)ff;
                Assert.assertTrue("check scope...", scopeList.contains(rpff.getScope()));
                
                ++propCount;
            }
        }
        
        // as we have been removing items from this list to ensure the attributes exist in the form
        // field list, if all attributes have been detected, the list should now be empty.
        Assert.assertEquals("list should be empty", 0, expectedAttributes.size());

        Assert.assertEquals(scopedRecordProperties.size(), propCount);
    }
    
    private RecordFormFieldCollection getByRecordId(Integer id, List<RecordFormFieldCollection> rffcList) {
        for (RecordFormFieldCollection rffc : rffcList) {
            if (id.equals(rffc.getRecordId())) {
                return rffc;
            }
        }
        return null;
    }
    
    private void addSurveyScopedItemsToPostMap(Record rec, MockHttpServletRequest req, int seed,
            Map<Attribute, Object> newSurveyScopedAttributeValues, Map<RecordPropertyType, String> recPropMap, boolean useLocation) {
        List<AttributeScope> scopeList = new ArrayList<AttributeScope>();
        scopeList.add(AttributeScope.SURVEY);
        scopeList.add(AttributeScope.SURVEY_MODERATION);
        addItemsToPostMap(rec, req, scopeList, "", seed, newSurveyScopedAttributeValues, recPropMap, useLocation);
    }
    
    private void addRecordScopedItemsToPostMap(Record rec, MockHttpServletRequest req, int index, int seed,
                                                                 Map<Attribute, Object> avMap, 
                                                                 Map<RecordPropertyType, String> recPropMap, 
                                                                 boolean useLocation, List<AttributeScope> scopeList) {
        String prefix = String.format("%d_", index);
        // add the record id....
        req.addParameter(prefix + SingleSiteController.PARAM_RECORD_ID, rec.getId().toString());
        req.addParameter(SingleSiteController.PARAM_ROW_PREFIX, prefix);
        addItemsToPostMap(rec, req, scopeList, prefix, seed, avMap, recPropMap, useLocation);
    }
    
    private void addItemsToPostMap(Record rec, MockHttpServletRequest req, List<AttributeScope> scopeList, String prefix, int seed,
                              Map<Attribute, Object> avMap, Map<RecordPropertyType, String> recPropMap, boolean useLocation) {
        
        List<AttributeValue> scopedAttributeValues = new ArrayList<AttributeValue>();
        List<RecordProperty> scopedRecordProperties = new ArrayList<RecordProperty>();
        for (AttributeScope scope : scopeList) {
            scopedAttributeValues.addAll(this.getAttributeValuesByScope(rec, scope));
            scopedRecordProperties.addAll(this.getRecordProperty(rec.getSurvey(), scope));
        }
        Map<String,String> params = new HashMap<String,String>();
        for (AttributeValue av : scopedAttributeValues) {
            // increment the seed for each iteration
            Object newValue = genRandomAttributeValue(av, seed++, true, true, avMap, prefix, params);
            avMap.put(av.getAttribute(), newValue);
        }
        req.addParameters(params);
        for (RecordProperty rp : scopedRecordProperties) {
            
            // don't add certain items to the post map depending on input args...
            if (useLocation && rp.getRecordPropertyType().equals(RecordPropertyType.POINT)) {
                // point coordinates should be the center x,y of the location
                
                //continue;
            } else if (!useLocation && rp.getRecordPropertyType().equals(RecordPropertyType.LOCATION)) {
                // signal that the location field on record should be null
                recPropMap.put(RecordPropertyType.LOCATION, null);
                continue;
            }
            
            String newValue = this.genRandomRecordPropertyValue(rec, rec.getSurvey(), rp.getRecordPropertyType(), seed++, false);
            recPropMap.put(rp.getRecordPropertyType(), newValue);
            addRecordPropertiesToPostMap(rec, rp, newValue, req, prefix);
        }
    }
    
    private void addRecordPropertiesToPostMap(Record rec, RecordProperty prop, String value, MockHttpServletRequest req, String prefix) {

        if (value == null) {
            return;
        }
        
        switch (prop.getRecordPropertyType()) {
        case NUMBER:
            req.addParameter(prefix+SingleSiteController.PARAM_NUMBER, value);
            break;
        case SPECIES:
            req.addParameter(prefix+SingleSiteController.PARAM_SPECIES, value);
            break;
        case LOCATION:
            req.addParameter(prefix+SingleSiteController.PARAM_LOCATION, value);
            break;
        case POINT:
        {
            String[] latLonSplit = value.split(",");
            req.addParameter(prefix+SingleSiteController.PARAM_LATITUDE, latLonSplit[0]);
            req.addParameter(prefix+SingleSiteController.PARAM_LONGITUDE, latLonSplit[1]);
            break;
        }
        case ACCURACY:
            req.addParameter(prefix+SingleSiteController.PARAM_ACCURACY,  value);
            break;
        case WHEN:
            req.addParameter(prefix+SingleSiteController.PARAM_DATE, value);
            break;
        case TIME:
        {
            String[] timeSplit = value.split(":");
            req.addParameter(prefix+SingleSiteController.PARAM_TIME_HOUR, timeSplit[0]);
            req.addParameter(prefix+SingleSiteController.PARAM_TIME_MINUTE, timeSplit[1]);
            req.addParameter(prefix+"time", value);
            break;
        }
        case NOTES:
            req.addParameter(prefix+SingleSiteController.PARAM_NOTES, value);
            break;
        case CREATED:
        case UPDATED:
            // These are read only properties.
            break;

            default:
                throw new IllegalStateException("property type not handled : " + prop);
        }
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
