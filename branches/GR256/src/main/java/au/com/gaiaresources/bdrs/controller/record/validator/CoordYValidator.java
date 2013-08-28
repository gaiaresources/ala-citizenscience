package au.com.gaiaresources.bdrs.controller.record.validator;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

public class CoordYValidator extends AbstractCoordValidator {

	private BdrsCoordReferenceSystem crs;
	
	public CoordYValidator(PropertyService propertyService, boolean required,
			boolean blank, BdrsCoordReferenceSystem crs) {
		super(propertyService, required, blank);
		this.crs = crs;
	}

	@Override
	protected Double getMin() {
		return crs.getMinY();
	}

	@Override
	protected Double getMax() {
		return crs.getMaxY();
	}
}
