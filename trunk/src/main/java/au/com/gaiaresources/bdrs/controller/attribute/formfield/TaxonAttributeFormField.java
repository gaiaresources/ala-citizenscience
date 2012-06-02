package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.Collection;
import java.util.Collections;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpeciesAttribute;

/**
 * The <code>TaxonAttributeFormField</code> is a representation of a
 * configurable field on the taxon editing form that stores its value in a
 * {@link IndicatorSpeciesAttribute}.
 */
public class TaxonAttributeFormField extends AbstractFormField implements TypedAttributeValueFormField {

    private Logger log = Logger.getLogger(getClass());

    private IndicatorSpeciesAttribute taxonAttribute;
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
            IndicatorSpeciesAttribute taxonAttribute, String prefix) {

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

    public IndicatorSpeciesAttribute getTaxonAttribute() {
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
        return getTaxonAttribute().hashCode() + getAttribute().hashCode() + super.hashCode();
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
		return Collections.EMPTY_LIST;
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
}
