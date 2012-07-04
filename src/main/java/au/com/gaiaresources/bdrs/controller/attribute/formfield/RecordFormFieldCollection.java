package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;

/**
 * Intended for use with single site forms. Groups a collection of form fields with a record id.
 * 
 * @author aaron
 *
 */
public class RecordFormFieldCollection {
    
    private List<FormField> formFields;
    private String prefix;
    private Integer recordId;
    private boolean highlight;
    
    /**
     * 
     * @param prefix - the prefix to use for the form fields.
     * @param record - the record the form field collection belongs to
     * @param highlight - should the webform render this collection of form fields as highlighted
     * @param recPropList - the RecordProperty fields to render...
     * @param attrList - the Attributes to render
     */
    public RecordFormFieldCollection(String prefix, Record record, boolean highlight, Collection<RecordProperty> recPropList,
            Collection<Attribute> attrList) {
        this(prefix, record, highlight, recPropList, attrList, Collections. <Integer> emptySet());
    }
    
    public RecordFormFieldCollection(String prefix, Record record, boolean highlight, Collection<RecordProperty> recPropList,
            Collection<Attribute> attrList, Set<Integer> existingCmIds) {
        this.prefix = prefix;
        this.highlight = highlight;
        this.recordId = record.getId();
        // check if the record is persisted, if it is not, use 0 for the 
        // record id to prevent null pointer exceptions
        if (this.recordId == null) {
            this.recordId = 0;
        }
        
        Set<AttributeValue> avSet = record.getAttributes();
        
        FormFieldFactory formFieldFactory = new FormFieldFactory();
        
        Survey survey = record.getSurvey();
        
        formFields = new ArrayList<FormField>();
                
        // add form fields. if attribute value exists for a given attribute, populate the form field, else 
        // the form field is empty.
        for (Attribute a : attrList) {
            AttributeValue av = AttributeValueUtil.getByAttribute(avSet, a);
            if (AttributeType.isCensusMethodType(a.getType())) {
                if (a.getCensusMethod() != null) {
                    if (!existingCmIds.contains(a.getCensusMethod().getId())) {
                        FormField ff = formFieldFactory.createCensusMethodAttributeFormField(survey, record, a, av, prefix);
                        if (ff != null) {
                            formFields.add(ff);
                        }
                    }
                }
            } else {
                formFields.add(formFieldFactory.createRecordFormField(survey, record, a, av, prefix));
            }
        }
        
        for (RecordProperty rp : recPropList) {
            // we always use the optionally taxonomic for the 'taxonomic' argument here as single site forms
            // never have census methods and hence, should default to OPT TAXA.
            formFields.add(formFieldFactory.createRecordFormField(record, rp, record.getSpecies(), Taxonomic.OPTIONALLYTAXONOMIC, prefix));
        }
        
        Collections.sort(formFields);
    }
    
    /**
     * Returns a list of form fields which may be populated, all relating to a single record
     * @return List<FormField> - the list of form fields to display
     */
    public List<FormField> getFormFields() {
        return this.formFields;
    }
    
    /**
     * Convenient way of finding out what record a group of web form inputs are related to.
     * @return Integer - the associated record id
     */
    public Integer getRecordId() {
        return this.recordId;
    }
    
    /**
     * Returns a string prefix which is used to uniquely identify a set of inputs in the web form
     * @return String - the form field prefix
     */
    public String getPrefix() {
        return this.prefix;
    }
    
    /**
     * Whether or not to highlight this record form field collection
     * @return boolean - is highlighted
     */
    public boolean isHighlight() {
        return this.highlight;
    }
}
