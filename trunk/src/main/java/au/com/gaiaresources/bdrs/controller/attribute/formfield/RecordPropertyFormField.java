package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.Collection;
import java.util.Collections;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;

/**
 * The <code>RecordPropertyFormField</code> is a representation of a
 * record property on the record form.
 */
public class RecordPropertyFormField extends AbstractRecordFormField implements TypedAttributeValueFormField {

    private IndicatorSpecies species;
    private Taxonomic taxonomic;
    private RecordProperty recordProperty;

    /**
     * Creates a new <code>RecordPropertyFormField</code> for a record property.
     * 
     * @param record
     *            the record to be updated
     * @param recordProperty
     *            the <code>RecordProperty</code> represented by this field
     * @param species
     *            the indicator species to be represented by this field, or null
     *            otherwise.
     * @param prefix
     *            the prefix to be prepended to input names.
     */
    public RecordPropertyFormField(Record record, RecordProperty recordProperty,
            IndicatorSpecies species, Taxonomic taxonomic, String prefix) {
        super(recordProperty.getSurvey(), record, prefix);
        this.recordProperty = recordProperty;
        this.taxonomic = taxonomic;
        this.species = species;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight() {
        return recordProperty.getWeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyFormField() {
        return true;
    }

    @Override
    public IndicatorSpecies getSpecies() {
        return species;
    }
    
    public Taxonomic getTaxonomic() {
		return taxonomic;
	}

	/**
	 * Returns the fields <code>RecordPropertyType</code>
	 * @return the fields <code>RecordPropertyType</code>
	 */
	public String getPropertyName() {
        return this.recordProperty.getRecordPropertyType().getName();
    }

    /**
     * Returns the fields description on the form or null when no description is available.
     * @return String description of the field on the form.
     */
    public String getDescription() {
    	return this.recordProperty.getDescription();
    }
    
    /**
     * Gets the required value from the <code>RecordProperty</code>
     * @return  Either true or false
     */
    @Override
    public boolean isRequired() {
    	return this.recordProperty.isRequired();
    }
    
    /**
     * Gets the scope from the <code>RecordProperty</code>
     * @return  a String containing the scope
     */
    public AttributeScope getScope() {
    	return this.recordProperty.getScope();
    }
    
    /**
     * Gets the hidden value from the <code>RecordProperty</code>
     * @return  Either true or false
     */
    public boolean isHidden() {
    	return this.recordProperty.isHidden();
    }
    
    /**
    * @return the recordProperty
    */
    public RecordProperty getRecordProperty() {
        return recordProperty;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractFormField#isModerationFormField()
     */
    @Override
    public boolean isModerationFormField() {
        return AttributeScope.isModerationScope(recordProperty.getScope()) || recordProperty.getRecordPropertyType().equals(RecordPropertyType.SPECIES);
    }

    /**
     * The RecordProperties of type RecordProperty.UPDATED/CREATED are display only.
     * @return true if this form field is display only.
     */
    @Override
    public boolean isDisplayFormField() {
        return this.recordProperty.getRecordPropertyType().isReadOnly();
    }

    /**
     * Returns true if this RecordAttributeFormField should be visible in the supplied DisplayContext.
     * @param context the context to check the visibility in.
     * @return true if this RecordAttributeFormField should be visible.
     */
    @Override
    public boolean isVisible(DisplayContext context) {
        return !isHidden() && this.recordProperty.getVisibility().isVisible(context);
    }

	@Override
	public String getName() {
		return this.recordProperty.getRecordPropertyType().getName();
	}

	@Override
	public Attribute getAttribute() {
		// record properties never have an attribute.
		return null;
	}

	@Override
	public TypedAttributeValue getAttributeValue() {
		// record properties never have an attribute value.
		return null;
	}

	@Override
	public Collection<IndicatorSpecies> getAllowableSpecies() {
		return survey != null ? survey.getSpecies() : Collections.EMPTY_LIST;
	}
	
	/**
	 * Get the coordinate reference system to use for this field.
	 * Only applicable if the field is of type POINT.
	 * @return BdrsCoordReferenceSystem 
	 */
	public BdrsCoordReferenceSystem getCrs() {
		return this.survey != null ? this.survey.getMap().getCrs() : null;
	}
}
