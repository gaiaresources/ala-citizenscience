package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;

/**
 * Basic implementation of the {@link FormField} providing accessors and
 * mutators for the <code>Survey</code>, <code>Record</code> and Prefix.
 */
public abstract class AbstractRecordFormField extends AbstractFormField {
    
    protected Survey survey;
    private Record record;
    
    /**
     * Creates a new <code>AbstractRecordFormField</code>.
     * 
     * @param survey the survey containing the record
     * @param record the record to be updated
     * @param prefix the prefix to be prepended to input names
     */
    public AbstractRecordFormField(Survey survey, Record record, String prefix, String category) {
    	super(prefix, category);
        this.survey = survey;
        this.record = record;
    }

    /**
     * {@inheritDoc}
     */ 
    @Override
    public int compareTo(FormField other) {
        if (other == null) {
            return 1;
        }
        return Integer.valueOf(this.getWeight()).compareTo(other.getWeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getClass().getName() + ": weight="+ getWeight();
    }

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
    
    /**
     * Returns true if the field is a required one.
     * @return
     */
    public abstract boolean isRequired();
    
    /**
     * Get the name for this record field.
     * @return the name.
     */
    public abstract String getName();
    
    /**
     * Get the description for this record field.
     * @return the description.
     */
    public abstract String getDescription();
    
    /**
     * Get the attribute associated with this form field.
     * @return the attribute if one exists. null otherwise.
     */
    public abstract Attribute getAttribute();
    
    /**
     * Gets the indicator species.
     * @return the indicator species.
     */
    public abstract IndicatorSpecies getSpecies();
}
