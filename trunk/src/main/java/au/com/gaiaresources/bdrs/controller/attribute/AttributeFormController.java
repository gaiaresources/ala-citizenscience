package au.com.gaiaresources.bdrs.controller.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormFieldFactory;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.record.RecordWebFormContext;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;

/**
 * Base controller for forms that can edit attributes.
 * Provides common census method attribute building code.
 * 
 * @author stephanie
 *
 */
public abstract class AttributeFormController extends AbstractController {
    public static final String PREFIX_TEMPLATE = "%d_";

    public static final int STARTING_SIGHTING_INDEX = 0;
    /**
     * The list of record form field collection objects that may be populated with
     * attribute value data,.
     */
    public static final String MODEL_RECORD_ROW_LIST = "recordFieldCollectionList";
    
    /**
     * The form fields used to create the header of the sightings table
     */
    public static final String MODEL_SIGHTING_ROW_LIST = "sightingRowFormFieldList";
    
    /** Required to create census method attribute form fields */
    @Autowired
    protected AttributeDAO attributeDAO;

    /** Required to create census method attribute form fields */
    @Autowired
    protected CensusMethodDAO cmDAO;

    @Autowired
    protected SurveyDAO surveyDAO;
    @Autowired
    protected TaxaDAO taxaDAO;
    
    @Autowired
    protected MetadataDAO metadataDAO;
    
    @Autowired
    protected GeoMapService geoMapService;
    
    /** Required to create census method attribute form fields */
    protected FormFieldFactory formFieldFactory = new FormFieldFactory();
    
    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param attr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @return a form field that represents the census method attribute
     */
    protected FormField createCensusMethodFormField(Survey survey, Record record,
            Attribute attr, User loggedInUser, String prefix,
            RecordWebFormContext context) {
        return createCensusMethodFormField(survey, record, attr, loggedInUser, prefix, context, new HashSet<Integer>());
    }

    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param species the species that contains the attribute values
     * @param attr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @return a form field that represents the census method attribute
     */
    protected FormField createCensusMethodFormField(Survey survey, Record record,
            IndicatorSpecies species, Attribute attr,
            User loggedInUser, String prefix,
            RecordWebFormContext context) {
        return createCensusMethodFormField(survey, record, species, attr, loggedInUser, prefix, context, new HashSet<Integer>());
    }

    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param location the location that contains the attribute values
     * @param attr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @return a form field that represents the census method attribute
     */
    protected FormField createCensusMethodFormField(Survey survey, Record record, Location location, 
            Attribute attr, User loggedInUser, String prefix, RecordWebFormContext context) {
        return createCensusMethodFormField(survey, record, location, attr, loggedInUser, prefix, context, new HashSet<Integer>());
    }
    
    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param cmAttr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @param existingIds a set of census method ids that have already been used in
     *                    the stack (prevents infinite recursion over census methods
     *                    that reference themselves or two that reference each other)
     * @return a form field that represents the census method attribute
     */
    private FormField createCensusMethodFormField(Survey survey, Record record,
            Attribute cmAttr, User loggedInUser, String prefix, RecordWebFormContext context, Set<Integer> existingIds) {
        
        AttributeValue attrVal = record != null ? AttributeValueUtil.getAttributeValue(cmAttr, record) : null;
        CensusMethod cm = cmAttr.getCensusMethod();
        if (cm == null || !existingIds.add(cm.getId())) {
            return null;
        }
        
        Set<Record> childRecords = attrVal != null ? attrVal.getRecords() : null;
        FormField ff = formFieldFactory.createCensusMethodAttributeFormField(survey, record, cmAttr, attrVal, prefix);
        
        if (createSubFormFields(childRecords, cm, prefix, cmAttr, loggedInUser, record, survey, null, context, existingIds).isEmpty()) {
            return null;
        }
        return ff;
    }

    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param species the species that contains the attribute values
     * @param cmAttr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @param existingIds a set of census method ids that have already been used in
     *                    the stack (prevents infinite recursion over census methods
     *                    that reference themselves or two that reference each other)
     * @return a form field that represents the census method attribute
     */
    private FormField createCensusMethodFormField(Survey survey, Record record, IndicatorSpecies species, 
            Attribute cmAttr, User loggedInUser, String prefix, RecordWebFormContext context, Set<Integer> existingIds) {
        AttributeValue attrVal = species != null ? AttributeValueUtil.getAttributeValue(cmAttr, species) : null;
        CensusMethod cm = cmAttr.getCensusMethod();
        if (cm == null || !existingIds.add(cm.getId())) {
            return null;
        }
        
        Set<Record> childRecords = attrVal != null ? attrVal.getRecords() : null;
        FormField ff = formFieldFactory.createCensusMethodAttributeFormField(survey, record, cmAttr, attrVal, prefix);
        
        if (createSubFormFields(childRecords, cm, prefix, cmAttr, loggedInUser, record, survey, species, context, existingIds).isEmpty()) {
            return null;
        }
        return ff;
    }
    
    /**
     * Creates a form field for census method attribute types.
     * @param survey the survey that contains the attributes
     * @param record the record that contains the attribute values
     * @param location the location that contains the attribute values
     * @param cmAttr the attribute to create a field for
     * @param loggedInUser the user requesting the form
     * @param prefix the prefix to append to the form fields
     * @param context the context of the form
     * @param existingIds a set of census method ids that have already been used in
     *                    the stack (prevents infinite recursion over census methods
     *                    that reference themselves or two that reference each other)
     * @return a form field that represents the census method attribute
     */
    private FormField createCensusMethodFormField(Survey survey, Record record, Location location, 
            Attribute cmAttr, User loggedInUser, String prefix, RecordWebFormContext context, Set<Integer> existingIds) {
        AttributeValue attrVal = location != null ? AttributeValueUtil.getAttributeValue(cmAttr, location) : null;
        CensusMethod cm = cmAttr.getCensusMethod();
        if (cm == null || !existingIds.add(cm.getId())) {
            return null;
        }
        
        Set<Record> childRecords = attrVal != null ? attrVal.getRecords() : null;
        FormField ff = formFieldFactory.createCensusMethodAttributeFormField(survey, record, cmAttr, attrVal, prefix);
        // create the blank ones for the headings
        if (createSubFormFields(childRecords, cm, prefix, cmAttr, loggedInUser, record, survey, null, context, existingIds).isEmpty()) {
            return null;
        }
        return ff;
    }
    
    /**
     * Creates a List of form fields contained by a parent census method form field
     * @param childRecords a List of records that contain the attribute values for the sub fields
     * @param cm the census method of the parent attribute
     * @param prefix a prefix to apply to all sub fields
     * @param cmAttr the parent attribute
     * @param loggedInUser the current user of the system
     * @param record the record that contains the attribute and the parent of the records in the list
     * @param survey the survey that contains the attributes
     * @param species the species that contains the attributes
     * @param context the context of the form
     * @param existingIds a set of census method ids that have already been used in
     *                    the stack (prevents infinite recursion over census methods
     *                    that reference themselves or two that reference each other)
     * @return a List of form field that represents the census method attribute
     */
    private List<FormField> createSubFormFields(Set<Record> childRecords, 
                                                CensusMethod cm, String prefix, 
                                                Attribute cmAttr, User loggedInUser, 
                                                Record record, Survey survey, IndicatorSpecies species, 
                                                RecordWebFormContext context, Set<Integer> existingIds) {
        List<FormField> subFFs = new ArrayList<FormField>();
        List<RecordFormFieldCollection> recordFormFieldList = new ArrayList<RecordFormFieldCollection>();
        int rowPrefix = 0;
        // create populated form fields from any previously saved records
        if (childRecords != null && !childRecords.isEmpty()) {
            for (Record childRec : childRecords) {
                String tmpPrefix = prefix + (AttributeType.CENSUS_METHOD_COL.equals(cmAttr.getType()) ? getRowIndexPrefix(rowPrefix++) : "") + 
                        "attribute_"+cmAttr.getId()+
                        String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, 
                                      (childRec.getId() != null ? childRec.getId()+"_" : ""));
                createFieldsForCensusMethod(cm, cmAttr, prefix+"attribute_"+cmAttr.getId(), tmpPrefix, childRec, survey, species, 
                                            loggedInUser, recordFormFieldList, subFFs, context, existingIds);

                RecordFormFieldCollection rffc = new RecordFormFieldCollection(
                    tmpPrefix, childRec, false, Collections.<RecordProperty> emptyList(), cm.getAttributes(), existingIds);
                recordFormFieldList.add(rffc);
            }
        } else if (cm != null) {
            // create blank form fields if no records have been saved yet
            String tmpPrefix = prefix + "attribute_"+cmAttr.getId();
            String recString = String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, "");
            createFieldsForCensusMethod(cm, cmAttr, tmpPrefix, tmpPrefix+recString, null, survey, species, 
                                        loggedInUser, recordFormFieldList, subFFs, context, existingIds);
        }
        if (!recordFormFieldList.isEmpty()) {
            String tmpPrefix = prefix + "attribute_"+cmAttr.getId();
            context.getNamedCollections().put(tmpPrefix, recordFormFieldList);
        }
        return subFFs;
    }

    private void createFieldsForCensusMethod(CensusMethod cm, Attribute cmAttr, String collectionName,
            String tmpPrefix, Record childRec, Survey survey, IndicatorSpecies species, User loggedInUser, 
            List<RecordFormFieldCollection> recordFormFieldList, List<FormField> subFFs, RecordWebFormContext context, Set<Integer> existingIds) {
        List<Attribute> atts = cm.getAttributes();
        List<Attribute> cmAtts = new ArrayList<Attribute>();
        FormField ff = null;
        for (Attribute attribute : atts) {
            AttributeValue aVal = AttributeValueUtil.getAttributeValue(attribute, childRec);
            if (AttributeType.isCensusMethodType(attribute.getType())) {
                ff = createCensusMethodFormField(survey, childRec, attribute, loggedInUser, tmpPrefix, context, new HashSet<Integer>(existingIds));
            } else {
                // TODO: comment about location here and join with below code
                if (survey != null) {
                    ff = formFieldFactory.createRecordFormField(survey, childRec, attribute, aVal, tmpPrefix);
                } else if (species != null) {
                    ff = formFieldFactory.createTaxonFormField(attribute, aVal, tmpPrefix);
                } else {
                    ff = null;
                }
            }
            if (ff != null) {
                subFFs.add(ff);
                cmAtts.add(attribute);
            }
        }
        
        Collections.sort(subFFs);
        
        if (!context.getNamedFormFields().containsKey(collectionName) && !subFFs.isEmpty()) {
            context.getNamedFormFields().put(collectionName, new ArrayList<FormField>(subFFs));
        }
    }

    /**
     * Returns a table row representation for census method attributes
     * @param request the request for the view
     * @param response the response
     * @param surveyId the id of the survey that the attribute row will appear on
     * @param speciesId the id of the species that the attribute row will appear on
     * @param rowIndex the index of the row on the page
     * @param rowView the name of the view for the row
     * @param attributeId the id of the census method attribute for which the row should be built
     * @return a table row in a census method attribute table
     */
    protected ModelAndView ajaxGetAttributeTable(HttpServletRequest request,
            HttpServletResponse response, Integer surveyId, Integer speciesId, 
            int rowIndex, String rowView, int attributeId) {
        Survey survey = null;
        Record record = new Record();
        RecordWebFormContext context = null;
        IndicatorSpecies species = null;
        
        if (surveyId != null && surveyId != 0) {
            survey = surveyDAO.getSurvey(surveyId);
            context = new RecordWebFormContext(request, record, getRequestContext().getUser(), survey, geoMapService);
        } else if (speciesId != null && speciesId != 0) {
            species = taxaDAO.getIndicatorSpecies(speciesId);
            context = new RecordWebFormContext(request, record, getRequestContext().getUser(), null, geoMapService);
        } else {
            throw new IllegalArgumentException("One of either speciesId or surveyId must be specified.");
        }
        
        // Add survey scope attribute form fields
        List<FormField> formFieldList = new ArrayList<FormField>();
        Attribute attribute = attributeDAO.get(attributeId);
        String prefix = getRowIndexPrefix(rowIndex);
        String tmpPrefix = getAttributePrefix(request) + prefix;
        if (AttributeType.isCensusMethodType(attribute.getType())) {
            Set<Integer> existingIds = createExistingIdsList(request);
            formFieldList.addAll(createSubFormFields(null, attribute.getCensusMethod(), tmpPrefix, attribute, 
                                 getRequestContext().getUser(), record, survey, species, context, existingIds));
        }
        Collections.sort(formFieldList);
        
        context.addFormFields("formFieldList", formFieldList);

        ModelAndView mv = new ModelAndView(rowView);
        mv.addObject("record", record);
        mv.addObject("survey", survey);
        mv.addObject(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT, context);
        mv.addObject("rowIndex", prefix);
        mv.addObject("attributeId", attributeId);
        mv.addObject("formPrefix", getAttributePrefix(request));
        // by definition editing must be enabled for items to be added to the
        // sightings table.
        mv.addObject(RecordWebFormContext.MODEL_EDIT, true);
        return mv;
    }

    /**
     * Creates a set of census method ids that exist in the stack as given by 
     * the request parameter "cmIds"
     * @param request the request
     * @return A set of census method ids that exist in the stack
     */
    private Set<Integer> createExistingIdsList(HttpServletRequest request) {
        Set<Integer> existingIds = new HashSet<Integer>();
        String[] cmIds = request.getParameterValues("cmIds");
        if (cmIds != null) {
            for (String string : cmIds) {
                existingIds.add(Integer.valueOf(string));
            }
        }
        
        return existingIds;
    }

    /**
     * Gets the prefix for the attribute from the parameter mapping or returns 
     * an empty string if it does not exist in the mapping.
     * @param request the request to get the parameter from
     * @return The prefix parameter from the request
     */
    private String getAttributePrefix(HttpServletRequest request) {
        String prefix = request.getParameter("prefix");
        if (prefix != null) {
            return prefix;
        }
        return "";
    }

    /**
     * Returns the formatted row index
     * @param rowIndex the index of the row
     * @return the row index formatted by PREFIX_TEMPLATE
     */
    protected static String getRowIndexPrefix(int rowIndex) {
        return String.format(PREFIX_TEMPLATE, rowIndex);
    }
}
