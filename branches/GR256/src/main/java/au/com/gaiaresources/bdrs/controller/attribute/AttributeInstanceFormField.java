package au.com.gaiaresources.bdrs.controller.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeVisibility;
import au.com.gaiaresources.bdrs.util.StringUtils;

/**
 * The <code>AttributeInstanceFormField</code> represents an
 * <code>Attribute</code> on a <code>Survey</code>.
 */
public class AttributeInstanceFormField extends AbstractAttributeFormField {

    private Attribute attribute = null;
    private AttributeDAO attributeDAO;
    private String weightName;

    private Collection<AttributeOption> optionsToDelete = null;
    
    private static final String EMPTY_ATTRIBUTE_NAME = "";

    /**
     * Creates and populates a new <code>Attribute</code>.
     * 
     * @param attributeDAO
     *            the database object to use when saving the attribute.
     * @param index
     *            the index of this field. The first added field is 1, the
     *            second is 2 and so on.
     * @param parameterMap
     *            the map of POST parameters that the form field will utilise to
     *            populate the <code>Attribute</code> that is created.
     */
    AttributeInstanceFormField(AttributeDAO attributeDAO, CensusMethodDAO cmDAO, int index,
            Map<String, String[]> parameterMap) throws NullPointerException, NumberFormatException {
        this(index);
        this.attributeDAO = attributeDAO;

        this.attribute = new Attribute();
        // We now can support empty names for horizontal rule attribute types.
        String nameParam = getParameter(index, parameterMap, "add_name_%d");
        this.attribute.setName(nameParam != null ? nameParam : EMPTY_ATTRIBUTE_NAME);
        this.attribute.setDescription(getParameter(index, parameterMap, "add_description_%d"));
        this.attribute.setTypeCode(getParameter(index, parameterMap, "add_typeCode_%d"));
        this.attribute.setRequired(getParameter(index, parameterMap, "add_required_%d") != null);
        this.attribute.setTag(Boolean.parseBoolean(getParameter(index, parameterMap, "add_tag_%d")));
        String attrScopeStr = getParameter(index, parameterMap, "add_scope_%d");
        if(attrScopeStr != null) {
            this.attribute.setScope(AttributeScope.valueOf(attrScopeStr));
        }
        this.attribute.setWeight(Integer.parseInt(getParameter(parameterMap, this.weightName)));
        this.attribute.setVisibility(AttributeVisibility.valueOf(
                getParameter(index, parameterMap, "add_visibility_%d", AttributeVisibility.ALWAYS.toString())));

        // if the attribute is a Census method type, save the census method
        if (AttributeType.isCensusMethodType(this.attribute.getType())) {
            String cmParam = getParameter(index, parameterMap, "add_attribute_census_method_%s");
            if (cmParam == null) {
                throw new NullPointerException("Parameter "+String.format("add_attribute_census_method_%s", index)+" cannot be null.");
            }
            int cmId = 0;
            try {
                cmId = Integer.valueOf(cmParam);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Parameter "+String.format("add_attribute_census_method_%s", index)+" must be a number.");
            }
            CensusMethod cm = cmDAO.get(cmId);
            if (cm == null) {
                throw new NullPointerException("Census method cannot be null.");
            }
            this.attribute.setCensusMethod(cm);
        }
        
        // Options
        AttributeOption opt;
        List<AttributeOption> optList = new ArrayList<AttributeOption>();
        if (getParameter(index, parameterMap, "add_option_%d") != null) {
            for (String optValue : getParameter(index, parameterMap, "add_option_%d").split(",")) {
                optValue = optValue.trim();
                if (!optValue.isEmpty()) {
                    opt = new AttributeOption();
                    opt.setValue(optValue);
                    optList.add(opt);
                }
            }
        }
        attribute.setOptions(optList);
    }


    /**
     * Updates the specified <code>Attribute</code> using the POST parameters
     * provided.
     * 
     * @param attributeDAO
     *            the database object to use when saving the attribute.
     * @param attribute
     *            the <code>Attribute</code> that shall be updated.
     * @param parameterMap
     *            the map of POST parameters that the form field will utilise to
     *            populate the <code>Attribute</code> that is created.
     */
    AttributeInstanceFormField(AttributeDAO attributeDAO, CensusMethodDAO cmDAO, Attribute attribute,
            Map<String, String[]> parameterMap) throws NullPointerException, NumberFormatException {
        this(attributeDAO, attribute);

        int attributePk = attribute.getId();
        // We now can support empty names for horizontal rule attribute types.
        String nameParam = getParameter(attributePk, parameterMap, "name_%d");
        this.attribute.setName(nameParam != null ? nameParam : EMPTY_ATTRIBUTE_NAME);
        this.attribute.setDescription(getParameter(attributePk, parameterMap, "description_%d"));
        this.attribute.setTypeCode(getParameter(attributePk, parameterMap, "typeCode_%d"));
        this.attribute.setRequired(getParameter(attributePk, parameterMap, "required_%d") != null);
        this.attribute.setTag(Boolean.parseBoolean(getParameter(attributePk, parameterMap, "tag_%d")));
        String attrScopeStr = getParameter(attributePk, parameterMap, "scope_%d");
        if(attrScopeStr != null) {
            this.attribute.setScope(AttributeScope.valueOf(attrScopeStr));
        }
        this.attribute.setWeight(Integer.parseInt(getParameter(parameterMap, this.weightName)));
        this.attribute.setVisibility(AttributeVisibility.valueOf(
                getParameter(attributePk, parameterMap, "visibility_%d", AttributeVisibility.ALWAYS.toString())));
        
        // if the attribute is a Census method type, save the census method
        if (AttributeType.isCensusMethodType(this.attribute.getType())) {
            String cmParam = getParameter(attributePk, parameterMap, "attribute_census_method_%s");
            if (cmParam == null) {
                throw new NullPointerException("Parameter "+String.format("attribute_census_method_%s", attributePk)+" cannot be null.");
            }
            int cmId = 0;
            try {
                cmId = Integer.valueOf(cmParam);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Parameter "+String.format("attribute_census_method_%s", attributePk)+" must be a number.");
            }
            CensusMethod cm = cmDAO.get(cmId);
            if (cm == null) {
                throw new NullPointerException("Census method cannot be null.");
            }
            this.attribute.setCensusMethod(cm);
        }
        else {
            this.attribute.setCensusMethod(null);
        }
        
        // Options
        List<AttributeOption> optList = new ArrayList<AttributeOption>();
        if (getParameter(parameterMap, "option_" + attributePk) != null) {
            Map<String, AttributeOption> optMap = new HashMap<String, AttributeOption>();
            for (AttributeOption opt : this.attribute.getOptions()) {
                optMap.put(opt.getValue(), opt);
            }
            AttributeOption opt;
            for (String optValue : getParameter(parameterMap, "option_"
                    + attributePk).split(",")) {
                optValue = optValue.trim();
                if (!optValue.isEmpty()) {
                    if (optMap.containsKey(optValue)) {
                        // Preexisting option
                        opt = optMap.remove(optValue);
                    } else {
                        // New option
                        opt = new AttributeOption();
                        opt.setValue(optValue);
                        //taxaDAO.save(opt);
                    }
                    optList.add(opt);
                }
            }
            optionsToDelete = optMap.values();
        }
        this.attribute.setOptions(optList);
    }

    /**
     * Creates a new form field for a previously saved <code>Attribute</code>.
     * 
     * @param attributeDAO
     *            the database object to use when saving the attribute.
     * @param attribute
     *            the attribute where the form field value is stored.
     */
    AttributeInstanceFormField(AttributeDAO attributeDAO, Attribute attribute) {
        this.attributeDAO = attributeDAO;
        this.attribute = attribute;

        this.weightName = "weight_" + attribute.getId();
    }

    /**
     * Creates a new, blank form field that is used for adding new
     * <code>Attributes</code>.
     * 
     * Note: You cannot invoke {@link AttributeFormField#save()} on fields
     * returned by this invocation because not all parameters required to
     * construct a valid Attribute (such as description and type) are available.
     * To create a new Attribute see
     * {@link #createAttributeFormField(AttributeDAO, int, Map)}
     * 
     * @param index
     *            the index of this field. The first added field is 1, the
     *            second is 2 and so on.
     */
    AttributeInstanceFormField(int index) {
        this.weightName = "add_weight_" + index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight() {
        return attribute == null ? 0 : attribute.getWeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWeight(int weight) {
        attribute.setWeight(weight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistentImpl save() {

        // Save the attribute options.
        for (int i = 0; i < attribute.getOptions().size(); i++) {
            attribute.getOptions().set(i, attributeDAO.save(attribute.getOptions().get(i)));
        }
        // Save the attribute
        Attribute attr = attributeDAO.save(this.attribute);

        // Delete any dereferenced options.
        if (optionsToDelete != null) {
            for (AttributeOption option : optionsToDelete) {
                attributeDAO.delete(option);
            }
            optionsToDelete = null;
        }

        return attr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAttributeField() {
        return true;
    }

    /**
     * Returns the <code>Attribute</code> represented by this form field.
     * 
     * @return the <code>Attribute</code> represented by this form field.
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * Sets the <code>Attribute</code> represented by this form field.
     * 
     * @param attribute
     *            the <code>Attribute</code> represented by this form field.
     */
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWeightName() {
        return this.weightName;
    }

    /**
     * Helper method to return the value of a parameter.
     * @param index identifies the parameter from others of the same type.
     * @param parameterMap the available parameters
     * @param parameterName the name of the parameter to get - this name is expected to contain the position of the
     *                      index in the form %d.
     * @return the value of the parameter identified by name and index, or null if no such parameter exists.
     */
    private String getParameter(int index, Map<String, String[]> parameterMap, String parameterName) {
        return getParameter(parameterMap, String.format(parameterName, index));
    }

    /**
     * Helper method to return the value of a parameter, or the supplied default if the parameter was not supplied.
     * @param index identifies the parameter from others of the same type.
     * @param parameterMap the available parameters
     * @param parameterName the name of the parameter to get - this name is expected to contain the position of the
     *                      index in the form %d.
     * @param defaultValue the value to return if the parameter is null or an empty string.
     * @return the value of the parameter identified by name and index, or null if no such parameter exists.
     */
    private String getParameter(int index, Map<String, String[]> parameterMap, String parameterName, String defaultValue) {
        String param = getParameter(index, parameterMap, parameterName);
        if (StringUtils.nullOrEmpty(param)) {
            param = defaultValue;
        }
        return param;
    }

}
