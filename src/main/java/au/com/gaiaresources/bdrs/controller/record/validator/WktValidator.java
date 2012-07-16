package au.com.gaiaresources.bdrs.controller.record.validator;

import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Validates wkt strings
 * 
 */
public class WktValidator extends AbstractValidator {
	
	public static final String MESSAGE_KEY_WKT_INVALID = "WktValidator.invalidWkt";
	
	private BdrsCoordReferenceSystem crs;

	public WktValidator(PropertyService propertyService, boolean required,
			boolean blank, BdrsCoordReferenceSystem crs) {
		super(propertyService, required, blank);
		if (crs == null) {
			throw new IllegalArgumentException("BdrsCoordReferenceSystem cannot be null");
		}
		this.crs = crs;
	}

	@Override
	public boolean validate(Map<String, String[]> parameterMap, String key,
			Attribute attribute, Map<String, String> errorMap) {
		boolean isValid = super.validate(parameterMap, key, attribute, errorMap);
		if (isValid) {
			SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil(crs.getSrid());
			Geometry geom = spatialUtil.createGeometryFromWKT(this.getSingleParameter(parameterMap, key));
			if (geom == null || !geom.isValid()) {
				errorMap.put(key, propertyService.getMessage(MESSAGE_KEY_WKT_INVALID));
			}
		}
		return isValid && !errorMap.containsKey(key);
	}

}
