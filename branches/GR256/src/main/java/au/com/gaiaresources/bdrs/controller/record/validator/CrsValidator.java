package au.com.gaiaresources.bdrs.controller.record.validator;

import java.util.Map;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

/**
 * Validates that the passed CRS string matches with one of our
 * BDRS defined coordinate reference systems.
 *
 */
public class CrsValidator extends AbstractValidator {
	
	public static final String MESSAGE_KEY_INVALID_CRS = "CrsValidator.invalidCrs";

	public CrsValidator(PropertyService propertyService, boolean required,
			boolean blank) {
		super(propertyService, required, blank);
	}

	@Override
	public boolean validate(Map<String, String[]> parameterMap, String key,
			Attribute attribute, Map<String, String> errorMap) {
		boolean isValid = super.validate(parameterMap, key, attribute, errorMap);
		if (isValid) {
			try {
				String crsString = this.getSingleParameter(parameterMap, key);
				Integer srid = Integer.valueOf(crsString);
        		if (BdrsCoordReferenceSystem.getBySRID(srid) == null) {
        			putInvalidCrsError(errorMap, key);
        		}
			} catch (NumberFormatException nfe) {
				putInvalidCrsError(errorMap, key);
        	}
		}
		return isValid && !errorMap.containsKey(key);
	}
	
	private void putInvalidCrsError(Map<String, String> errorMap, String key) {
		errorMap.put(key, propertyService.getMessage(MESSAGE_KEY_INVALID_CRS));
	}
}
