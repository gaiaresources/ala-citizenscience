package au.com.gaiaresources.bdrs.controller.record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordAttributeFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.DateFormatter;

public class TrackerControllerDuplicateRecordTest extends AbstractGridControllerTest {
    
    /**
     * Tests that the tracker form is populated correctly when the duplicate
     * record URL is called.
     * @throws Exception
     */
    @Test
    public void testDuplicateRecord() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Record recToDupe = r1;
        Survey survey = recToDupe.getSurvey();
        
        // create parameter mapping
        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        addParameter(params, "survey_species_search", recToDupe.getSpecies().getScientificName());
        addParameter(params, "species", recToDupe.getSpecies().getId().toString());
        addParameter(params, "latitude", recToDupe.getLatitude().toString());
        addParameter(params, "longitude", recToDupe.getLongitude().toString());
        addParameter(params, "date", DateFormatter.getFormatter(DateFormatter.DAY_MONTH_YEAR).format(recToDupe.getWhen()));
        addParameter(params, "time", DateFormatter.getFormatter(DateFormatter.TIME).format(recToDupe.getTime()));
        addParameter(params, "number", String.valueOf(recToDupe.getNumber()));
        addParameter(params, "notes", recToDupe.getNotes());

        Map<Attribute, AttributeValue> expectedRecordAttrMap = new HashMap<Attribute, AttributeValue>();
        params.putAll(createAttributes(recToDupe, expectedRecordAttrMap));
        params.put("recordId", String.valueOf(recToDupe.getId()));
        
        request.setMethod("POST");
        request.setParameters(params);
        request.setRequestURI(request.getContextPath()+"/bdrs/user/contribute/duplicateRecord.htm");
        
        ModelAndView mv = handle(request, response);
        
        // get the value map
        Map<String, String> valueMap = (Map<String, String>)mv.getModelMap().get(TrackerController.MV_VALUE_MAP);
        
        // assert that we have landed on the tracker form
        ModelAndViewAssert.assertViewName(mv, "tracker");

        // assert that there is a record and survey object
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "record");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");

        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        Assert.assertNotNull(formContext.getNamedFormFields().get("surveyFormFieldList"));
        Assert.assertNotNull(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));

        List<FormField> allFormFields = new ArrayList<FormField>(formContext.getNamedFormFields().get("surveyFormFieldList"));
        allFormFields.addAll(formContext.getNamedFormFields().get("taxonGroupFormFieldList"));
        for (FormField formField : allFormFields) {
            if (formField.isAttributeFormField()) {
                RecordAttributeFormField attributeField = (RecordAttributeFormField) formField;
                // make sure the records are not the same object
                Assert.assertNotSame(recToDupe.getId(), attributeField.getRecord().getId());
                Assert.assertEquals(survey, attributeField.getSurvey());
                // only compare the string values of the attributes to make sure those are equal
                // the actual objects will be different
                if (expectedRecordAttrMap.get(attributeField.getAttribute()) != null && 
                        attributeField.getAttributeValue() != null) {
                    Assert.assertNotSame(expectedRecordAttrMap.get(attributeField.getAttribute()).getId(), 
                            attributeField.getAttributeValue().getId());
                    Assert.assertEquals(expectedRecordAttrMap.get(attributeField.getAttribute()).toString(), 
                            attributeField.getAttributeValue().toString());
                }
            } else if (formField.isPropertyFormField()) {
                RecordPropertyFormField propertyField = (RecordPropertyFormField) formField;
                // make sure the records are not the same object
                Assert.assertNotSame(recToDupe.getId(), propertyField.getRecord().getId());
                Assert.assertEquals(survey, propertyField.getSurvey());
                assertPropertyValue(propertyField.getRecordProperty().getRecordPropertyType(), recToDupe, valueMap);
            } else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertNotSame(recToDupe, mv.getModelMap().get("record"));
        Assert.assertEquals(survey, mv.getModelMap().get("survey"));
    }
    
    private void addParameter(Map<String, String> params, String key,
			String value) {
		if (value != null) {
			params.put(key, value);
		}
	}

	private void assertPropertyValue(RecordPropertyType recordPropertyType,
            Record recToDupe, Map<String, String> valueMap) {
        String paramName = recordPropertyType.toString().toLowerCase();
        String actualValue = null;
        String expectedValue = valueMap.get(paramName);
        switch (recordPropertyType) {
            case ACCURACY:
            	Double accuracy = recToDupe.getAccuracyInMeters();
            	if (accuracy != null) {
            		actualValue = String.valueOf(accuracy);
            	}
                break;
            case GPS_ALTITUDE:
                Double gpsAltitude = recToDupe.getGpsAltitude();
                if (gpsAltitude != null) {
                    actualValue = String.valueOf(gpsAltitude);
                }
                break;
            // skip created and updated since these values are not copied
            // and are set by the system
            case UPDATED:
            case CREATED:
                break;
            case LOCATION:
                // location isn't added to the parameter mapping so it should always be null
                break;
            case NOTES:
                actualValue = recToDupe.getNotes();
                break;
            case NUMBER:
            	Integer number = recToDupe.getNumber();
            	if (number != null) {
            		actualValue = String.valueOf(number);
            	}
                break;
            case POINT:
                actualValue = String.valueOf(recToDupe.getLatitude());
                expectedValue = valueMap.get("latitude");
                Assert.assertEquals(expectedValue, actualValue);
                actualValue = String.valueOf(recToDupe.getLongitude());
                expectedValue = valueMap.get("longitude");
                break;
            case SPECIES:
            	IndicatorSpecies species = recToDupe.getSpecies();
            	Integer speciesId = species != null ? species.getId() : null;
            	if (speciesId != null) {
            		actualValue = String.valueOf(speciesId);
            	}
                break;
            case TIME:
                actualValue = DateFormatter.getFormatter(DateFormatter.TIME).format(recToDupe.getTime());
                break;
            case WHEN:
                actualValue = DateFormatter.getFormatter(DateFormatter.DAY_MONTH_YEAR).format(recToDupe.getWhen());
                expectedValue = valueMap.get("date");
                break;
            default:
                break;
        }
        Assert.assertEquals(expectedValue, actualValue);
    }

    /**
     * Helper method to create attribute parameters for the request mapping.
     * @param recToDupe the record to duplicate the attributes of
     * @param expectedRecordAttrMap the mapping of expected values for testing
     * @return the parameter mapping for the attributes to add to the request
     */
    private Map<? extends String, ? extends String> createAttributes(Record recToDupe, Map<Attribute, AttributeValue> expectedRecordAttrMap) {
        Map<String, String> params = new HashMap<String,String>();
        for (AttributeValue value : recToDupe.getAttributes()) {
            setSpecificAttributeValue(value.getAttribute(), value.toString(), null, "", params);
            expectedRecordAttrMap.put(value.getAttribute(), value);
        }
        return params;
    }
}
