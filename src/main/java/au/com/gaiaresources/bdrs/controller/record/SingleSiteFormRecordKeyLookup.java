package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * @author stephanie
 */
public class SingleSiteFormRecordKeyLookup implements RecordKeyLookup {

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getAccuracyKey()
     */
    @Override
    public String getAccuracyKey() {
        return SingleSiteController.PARAM_ACCURACY;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getAttributeNameTemplate()
     */
    @Override
    public String getAttributeNameTemplate() {
        return AttributeParser.ATTRIBUTE_NAME_TEMPLATE;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getCensusMethodAttributePrefix()
     */
    @Override
    public String getCensusMethodAttributePrefix() {
        return TrackerController.CENSUS_METHOD_ATTRIBUTE_PREFIX;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getCensusMethodIdKey()
     */
    @Override
    public String getCensusMethodIdKey() {
        return BdrsWebConstants.PARAM_CENSUS_METHOD_ID;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getDateKey()
     */
    @Override
    public String getDateKey() {
        return SingleSiteController.PARAM_DATE;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getIndividualCountKey()
     */
    @Override
    public String getIndividualCountKey() {
        return SingleSiteController.PARAM_NUMBER;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getLatitudeKey()
     */
    @Override
    public String getLatitudeKey() {
        return SingleSiteController.PARAM_LATITUDE;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getLocationKey()
     */
    @Override
    public String getLocationKey() {
        return SingleSiteController.PARAM_LOCATION;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getLocationNameKey()
     */
    @Override
    public String getLocationNameKey() {
        return TrackerController.PARAM_LOCATION_NAME;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getLongitudeKey()
     */
    @Override
    public String getLongitudeKey() {
        return SingleSiteController.PARAM_LONGITUDE;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getNotesKey()
     */
    @Override
    public String getNotesKey() {
        return SingleSiteController.PARAM_NOTES;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getParentRecordIdKey()
     */
    @Override
    public String getParentRecordIdKey() {
        // TODO Auto-generated method stub
        return TrackerController.PARAM_PARENT_RECORD_ID;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getRecordIdKey()
     */
    @Override
    public String getRecordIdKey() {
        return BdrsWebConstants.PARAM_RECORD_ID;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getRecordVisibilityKey()
     */
    @Override
    public String getRecordVisibilityKey() {
        return TrackerController.PARAM_RECORD_VISIBILITY;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getSpeciesIdKey()
     */
    @Override
    public String getSpeciesIdKey() {
        return SingleSiteController.PARAM_SPECIES;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getSpeciesNameKey()
     */
    @Override
    public String getSpeciesNameKey() {
        return TrackerController.PARAM_SPECIES_NAME;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getSurveyAttributePrefix()
     */
    @Override
    public String getSurveyAttributePrefix() {
        return "";
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getSurveyIdKey()
     */
    @Override
    public String getSurveyIdKey() {
        return BdrsWebConstants.PARAM_SURVEY_ID;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getTaxonAttributePrefix()
     */
    @Override
    public String getTaxonAttributePrefix() {
        return TrackerController.TAXON_GROUP_ATTRIBUTE_PREFIX;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getTimeHourKey()
     */
    @Override
    public String getTimeHourKey() {
        return SingleSiteController.PARAM_TIME_HOUR;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getTimeKey()
     */
    @Override
    public String getTimeKey() {
        return TrackerController.PARAM_TIME;
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.deserialization.record.RecordKeyLookup#getTimeMinuteKey()
     */
    @Override
    public String getTimeMinuteKey() {
        return SingleSiteController.PARAM_TIME_MINUTE;
    }

	@Override
	public String getZoneKey() {
		return BdrsWebConstants.PARAM_SRID;
	}

	@Override
	public String getWktKey() {
		return BdrsWebConstants.PARAM_WKT;
	}
}
