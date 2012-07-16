package au.com.gaiaresources.bdrs.controller.record.validator;

import java.util.Map;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

public abstract class AbstractCoordValidator extends AbstractValidator {

	private boolean required;
	private boolean blank;
	private PropertyService ps;
	
	private Logger log = Logger.getLogger(getClass());
	
	public AbstractCoordValidator(PropertyService propertyService, boolean required,
			boolean blank) {
		super(propertyService, required, blank);
		
		this.required = required;
		this.blank = blank;
		this.ps = propertyService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validate(Map<String, String[]> parameterMap, String key,
			Attribute attribute, Map<String, String> errorMap) {
		boolean isValid = super.validate(parameterMap, key, attribute, errorMap);
		if (isValid) {
			Double min = getMin();
			Double max = getMax();
			Validator v;
			if (min != null && max != null) {
				v = new DoubleRangeValidator(ps, required, blank, min.doubleValue(), max.doubleValue());
			} else {
				v = new DoubleValidator(ps, required, blank);
			}
			return v.validate(parameterMap, key, attribute, errorMap);
		}
		return false;
	}
	
	/**
	 * Get the minimum valid value. Can return null (no limit).
	 * @return The minimum acceptable value.
	 */
	protected abstract Double getMin();
	/**
	 * Get the maximum valid value. Can return null (no limit).
	 * @return
	 */
	protected abstract Double getMax();
}
