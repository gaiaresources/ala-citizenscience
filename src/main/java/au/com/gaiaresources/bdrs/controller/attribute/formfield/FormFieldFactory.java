package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;

/**
 * The <code>RecordFormFieldFactory</code> is the one-stop-shop for the creation
 * of all <code>RecordFormFields</code>.
 */
public class FormFieldFactory {
    
    private Logger log = Logger.getLogger(getClass());

    /**
     * Creates a new {@link FormField} for the specified survey attribute.
     * 
     * @param survey the survey containing the record
     * @param record the record to be updated
     * @param attribute the attribute represented by this field. 
     * @param recordAttribute the current value of this field or null
     * @param prefix the prefix to be prepended to input names.
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Survey survey, Record record,
            Attribute attribute, TypedAttributeValue recordAttribute, String prefix, String category) {
        return new RecordAttributeFormField(survey, record, attribute,
                recordAttribute, prefix, category);
    }

    /**
     * Creates a new {@link FormField} for the specified survey attribute.
     * 
     * @param survey the survey containing the record
     * @param record the record to be updated
     * @param attribute the attribute represented by this field. 
     * @param recordAttribute the current value of this field or null
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Survey survey,
            Record record, Attribute attribute, TypedAttributeValue recordAttribute) {
        return new RecordAttributeFormField(survey, record, attribute,
                recordAttribute, AttributeParser.DEFAULT_PREFIX);
    }

    /**
     * Creates a new {@link FormField} for the specified survey attribute.
     * 
     * @param survey the survey containing the record
     * @param record the record to be updated
     * @param attribute the attribute represented by this field. 
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Survey survey,
            Record record, Attribute attribute) {
        return new RecordAttributeFormField(survey, record, attribute, null,
                AttributeParser.DEFAULT_PREFIX);
    }
    
    /**
     * Creates a new {@link FormField} for the specified survey attribute.
     * 
     * @param survey the survey containing the record
     * @param record the record to be updated
     * @param attribute the attribute represented by this field.
     * @param prefix the prefix to be prepended to input names. 
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Survey survey, Record record,
            Attribute attribute, String prefix, String category) {
        return new RecordAttributeFormField(survey, record, attribute, null, prefix, category);
    }

    /**
     * Creates a new {@link FormField} for a record property.
     * 
     * @param record the record to be updated
     * @param recordProperty the <code>RecordProperty</code> that is stored in the <code>RecordFormField</code>
     * @param species the indicator species to be represented by this field, or
     * null otherwise.
     * @param taxonomic determines if the species field is mandatory.
     * @param prefix the prefix to be prepended to input names.
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Record record, RecordProperty recordProperty, IndicatorSpecies species,
            Taxonomic taxonomic, String prefix) {
        return new RecordPropertyFormField(record, recordProperty, species, taxonomic, prefix);
    }

    /**
     * Creates a new {@link FormField} for a record property.
     * 
     * @param record the record to be updated
	 * @param recordProperty the <code>RecordProperty</code> that is stored in the <code>RecordFormField</code>
     * @param species the indicator species to be represented by this field, or
     * null otherwise.
     * @param taxonomic determines if the species field is mandatory.
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(
            Record record, RecordProperty recordProperty, IndicatorSpecies species, Taxonomic taxonomic) {
        return new RecordPropertyFormField(record, recordProperty,
                species, taxonomic, AttributeParser.DEFAULT_PREFIX);
    }

    /**
     * Creates a new {@link FormField} for a record property.
     * 
     * @param record the record to be updated
	 * @param recordProperty the <code>RecordProperty</code> that is stored in the <code>RecordFormField</code>
     * @return a <code>RecordFormField</code>
     */
    public FormField createRecordFormField(Record record,
            RecordProperty recordProperty) {
        return new RecordPropertyFormField(record, recordProperty, null, null,
                AttributeParser.DEFAULT_PREFIX);
    }
    
    /**
     * Creates a new form field for the taxon.
     * @param attribute the attribute (tag) from the taxon group.
     * @param value the current value for this attribute
     * @return a <code>TaxonAttributeFormField</code>
     */
    public FormField createTaxonFormField(Attribute attribute, AttributeValue value) {
    	return new TaxonAttributeFormField(attribute, value, AttributeParser.DEFAULT_PREFIX);
    }
    
    /**
     * Create a new form field for the taxon.
     * @param attribute the attribute (tag) from the taxon group.
     * @return the <code>TaxonAttributeFormField</code>
     */
    public FormField createTaxonFormField(Attribute attribute) {
        return new TaxonAttributeFormField(attribute, null, AttributeParser.DEFAULT_PREFIX);
    }
    
    /**
     * Create a new form field for the taxon.
     * @param attribute the attribute (tag) from the taxon group.
     * @param value the current value for this attribute
     * @param prefix the prefix for the form field
     * @return the <code>TaxonAttributeFormField</code>
     */
    public FormField createTaxonFormField(Attribute attribute, AttributeValue value, String prefix) {
        return new TaxonAttributeFormField(attribute, value, prefix);
    }
    
    /**
     * Creates a new form field for the location.
     * @param attribute the attribute (tag) from the survey attribute.
     * @param value the current value for this attribute
     * @param survey the survey that is being rendered.
     * @return a <code>LocationAttributeFormField</code>
     */
    public FormField createLocationFormField(Attribute attribute, TypedAttributeValue value, Survey survey) {
        return new LocationAttributeFormField(attribute, value, LocationAttributeFormField.LOCATION_PREFIX, survey);
    }
    
    /**
     * Create a new form field for the location.
     * @param attribute the attribute (tag) from the survey attribute.
     * @param survey the survey that is being rendered.
     * @return the <code>LocationAttributeFormField</code>
     */
    public FormField createLocationFormField(Attribute attribute, Survey survey) {
    	return createLocationFormField(attribute, null, survey);
    }
    
    /**
     * Creates a new census method attribute form field.  These are attributes 
     * that are displayed as a table of census method attributes
     * @param survey the {@link Survey} that owns the attribute
     * @param record the {@link Record} for which the attribute value will be saved
     * @param attribute the {@link Attribute} being represented
     * @param recordAttribute the {@link AttributeValue} of the {@link Attribute
     * @param prefix a String to prepend to the name of the field
     * @return a {@link FormField} object which represents the attribute on the page
     */
    public FormField createCensusMethodAttributeFormField(Survey survey, Record record,
            Attribute attribute, TypedAttributeValue recordAttribute, String prefix, String category) {
        return new CensusMethodAttributeFormField(survey, record, attribute, recordAttribute, prefix, category);
    }
}
