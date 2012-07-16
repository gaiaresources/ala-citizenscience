package au.com.gaiaresources.bdrs.controller.record.validator;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

public class CoordXValidator extends AbstractCoordValidator {

	private BdrsCoordReferenceSystem crs;
	
	public CoordXValidator(PropertyService propertyService, boolean required,
			boolean blank, BdrsCoordReferenceSystem crs) {
		super(propertyService, required, blank);
		this.crs = crs;
	}

	@Override
	protected Double getMin() {
		return crs.getMinX();
	}

	@Override
	protected Double getMax() {
		return crs.getMaxX();
	}
}
