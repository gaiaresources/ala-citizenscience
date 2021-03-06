package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.Collection;
import java.util.Collections;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

import org.apache.log4j.Logger;

/**
 * The <code>LocationAttributeFormField</code> is a representation of a
 * configurable field on the location editing form that stores its value in a
 * {@link AttributeValue}.
 */
public class LocationAttributeFormField extends AbstractFormField implements TypedAttributeValueFormField {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    private TypedAttributeValue locationAttribute;
    private Attribute attribute;
    private Survey survey;

    public static final String LOCATION_PREFIX = "";
    
    /**
     * Creates a new <code>locationAttributeFormField</code> for the specified
     * survey attribute.
     * 
     * @param attribute
     *            the attribute represented by this field.
     * @param locationAttribute
     *            the current value of this field or null
     * @param prefix
     *            the prefix to be prepended to input names.
     * @param species
     *            collection of allowable species for species attribute autocomplete.
     */
    LocationAttributeFormField(Attribute attribute, 
            TypedAttributeValue locationAttribute, String prefix, Survey survey) {

        super(prefix, BdrsWebConstants.LOCATION_ATTR_CATEGORY);

        this.survey = survey;
        this.attribute = attribute;
        this.locationAttribute = locationAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight() {
        return this.attribute.getWeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAttributeFormField() {
        return true;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    @Override
    public TypedAttributeValue getAttributeValue() {
        return this.locationAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FormField other) {
        return Integer.valueOf(this.getWeight()).compareTo(other.getWeight());
    }

    
    @Override
    public boolean isDisplayFormField() {
        return attribute != null && (AttributeType.isHTMLType(attribute.getType()) || 
                AttributeType.isCensusMethodType(attribute.getType()));
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractFormField#isModerationFormField()
     */
    @Override
    public boolean isModerationFormField() {
        return AttributeScope.isModerationScope(attribute.getScope());
    }

    /**
     * Returns true if this LocationAttributeFormField should be visible.
     * @param context the context to check the visibility in.
     * @return true if this LocationAttributeFormField should be visible.
     */
    @Override
    public boolean isVisible(DisplayContext context) {
        return attribute.isVisible(context);
    }

    @Override
    public Collection<IndicatorSpecies> getAllowableSpecies() {
        return survey != null ? survey.getSpecies() : Collections.<IndicatorSpecies>emptyList();
    }

    @Override
    public IndicatorSpecies getSpecies() {
        return this.locationAttribute != null ? locationAttribute.getSpecies() : null;
    }

    @Override
    public boolean isRequired() {
        return attribute.isRequired();
    }

    @Override
    public Survey getSurvey() {
        return this.survey;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField#getName()
     */
    @Override
    public String getName() {
        return attribute != null? attribute.getName() : null;
    }
}
