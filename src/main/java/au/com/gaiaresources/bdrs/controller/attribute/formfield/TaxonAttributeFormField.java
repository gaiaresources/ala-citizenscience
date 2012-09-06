package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.Collection;
import java.util.Collections;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;

/**
 * The <code>TaxonAttributeFormField</code> is a representation of a
 * configurable field on the taxon editing form that stores its value in an
 * {@link AttributeValue}.
 */
public class TaxonAttributeFormField extends AbstractFormField implements TypedAttributeValueFormField {
    private AttributeValue taxonAttribute;
    private Attribute attribute;

    /**
     * Creates a new <code>TaxonAttributeFormField</code> for the specified
     * survey attribute.
     * 
     * @param attribute
     *            the attribute represented by this field.
     * @param taxonAttribute
     *            the current value of this field or null
     * @param prefix
     *            the prefix to be prepended to input names.
     */
    TaxonAttributeFormField(Attribute attribute, 
                            AttributeValue taxonAttribute, String prefix) {

        super(prefix);

        this.attribute = attribute;
        this.taxonAttribute = taxonAttribute;
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

    public AttributeValue getTaxonAttribute() {
        return taxonAttribute;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    @Override
    public TypedAttributeValue getAttributeValue() {
        return this.taxonAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FormField other) {
        return Integer.valueOf(this.getWeight()).compareTo(other.getWeight());
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractFormField#isModerationFormField()
     */
    @Override
    public boolean isModerationFormField() {
        return AttributeScope.isModerationScope(attribute.getScope());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true; 
        } else if (obj instanceof TaxonAttributeFormField) {
            TaxonAttributeFormField that = (TaxonAttributeFormField) obj;
            return (this.getTaxonAttribute().equals(that.getTaxonAttribute()) && 
                    this.getAttribute().equals(that.getAttribute()) && 
                    super.equals(obj));
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        int prime = 31;
        return (getTaxonAttribute() != null ? getTaxonAttribute().hashCode() : 1)*prime + 
                getAttribute().hashCode()*prime + super.hashCode()*prime;
    }

    /**
     * Returns true if this TaxonAttributeFormField should be visible in the supplied DisplayContext.
     * @param context the context to check the visibility in.
     * @return true if this TaxonAttributeFormField should be visible.
     */
    @Override
    public boolean isVisible(DisplayContext context) {
        return attribute.isVisible(context);
    }

    @Override
    public Collection<IndicatorSpecies> getAllowableSpecies() {
        // all species are allowed for this form field type.
        return Collections.<IndicatorSpecies> emptyList();
    }

    @Override
    public IndicatorSpecies getSpecies() {
        return taxonAttribute != null ? taxonAttribute.getSpecies() : null;
    }

    @Override
    public boolean isRequired() {
        return this.attribute.isRequired();
    }

    @Override
    public Survey getSurvey() {
        // taxon attributes will never have a survey.
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField#getName()
     */
    @Override
    public String getName() {
        return attribute != null ? attribute.getName() : null;
    }
}
