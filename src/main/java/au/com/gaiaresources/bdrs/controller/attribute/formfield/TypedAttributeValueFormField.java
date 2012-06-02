package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.Collection;

import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;

public interface TypedAttributeValueFormField {
	
    /**
     * get the AttributeValue for this form field
     * @return the attribute value for this form field.
     */
	public TypedAttributeValue getAttributeValue();
	
	/**
	 * gets the attribute for this form field.
	 * may return null.
	 * 
	 * @return the attribute for this form field.
	 */
	public Attribute getAttribute();
	
	/**
	 * get the prefix for the name of this form field. This prefix is normally
	 * used when generating the input name
	 * @return the prefix for this form field.
	 */
	public String getPrefix();
	
	/**
	 * Gets a list of allowable species to be set. Only used when the attribute type
	 * is 'SPECIES'
	 * @return collection of allowable species.
	 */
	public Collection<IndicatorSpecies> getAllowableSpecies();
	
	/**
	 * Gets the species stored in the TypedAttributeValue.
	 * @return species stored in internal attribute value.
	 */
	public IndicatorSpecies getSpecies();
	
    /**
     * Returns true if the field is a required one.
     * @return true if field is required
     */
    public boolean isRequired();
    
    /**
     * Gets the survey for this attribute value if it exists.
     * Possible to return null.
     * 
     * @return survey for this attribute value.
     */
    public Survey getSurvey();
}
