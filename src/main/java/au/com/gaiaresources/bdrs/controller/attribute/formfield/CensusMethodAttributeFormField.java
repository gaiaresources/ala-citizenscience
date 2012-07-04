package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;

/**
 * The <code>CensusMethodAttributeFormField</code> is a representation of a
 * configurable field on the record form, taxon editing form, or location 
 * editing form that stores its value in an {@link AttributeValue}.
 * 
 * @author stephanie
 *
 */
public class CensusMethodAttributeFormField extends RecordAttributeFormField {
    
    /**
     * Create a new <code>CensusMethodAttributeFormField</code>
     * @param survey the {@link Survey} for which this attribute is being edited
     * @param record the {@link Record} for which this attribute is being edited
     * @param attribute the {@link Attribute} this field represents
     * @param recordAttribute the {@link AttributeValue} this field contains
     * @param prefix a prefix to append to the form field name
     */
    public CensusMethodAttributeFormField(Survey survey, Record record,
            Attribute attribute, TypedAttributeValue recordAttribute,
            String prefix) {
        super(survey, record, attribute, recordAttribute, prefix);
    }
    
    @Override
    public boolean isDisplayFormField() {
        // this can be considered "display" because it is displaying a 
        // table of attributes instead of a traditional single attribute
        return true;
    }
}
