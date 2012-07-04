/**
 * 
 */
package au.com.gaiaresources.bdrs.util.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeUtil;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;

/**
 * @author stephanie
 *
 */
public class LocationUtils {
    private static Logger log = Logger.getLogger(LocationUtils.class);
    /**
     * Filter attributes out of the flattened map to avoid changing the actual object
     * @param surveyId the survey for which the location is requested
     * @param location the location
     * @param locationObj flattened location object
     */
    public static void filterAttributesBySurvey(Survey survey, Location location, Map<String, Object> locationObj) {
        // create an Attribute-indexed map of the location attributes
        // so we can get the attribute value by attribute later
        Map<Attribute, AttributeValue> typedAttrMap = new HashMap<Attribute, AttributeValue>();
        for (AttributeValue attr : location.getAttributes()) {
            typedAttrMap.put(attr.getAttribute(), attr);
        }
        
        // get all of the attributes for the survey and filter - only keep 
        // attribute values for this location that match the survey attributes
        Set<Integer> attrIdsToKeep = new HashSet<Integer>();
        // also keep track of any location-scoped HTML attributes on the survey
        // as they will need to be added to the form too
        List<Attribute> attributesToAdd = new ArrayList<Attribute>();
        for(Attribute attribute : survey.getAttributes()) {
            if(AttributeScope.LOCATION.equals(attribute.getScope())) {
                AttributeValue value = typedAttrMap.get(attribute);
                // if the attribute has a value, it is a keeper
                if (value != null && value.getValue() != null) {
                    attrIdsToKeep.add(attribute.getId());
                } else {
                    // if it is an HTML attribute, it will need to be added later
                    if (AttributeUtil.isHTMLType(attribute)) {
                        attributesToAdd.add(attribute);
                    }
                }
            }
        }
        
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) locationObj.get("orderedAttributes");
        List<Object> removeAttrs = new ArrayList<Object>();
        Map<Integer, Attribute> indexedAttributes = new TreeMap<Integer, Attribute>();
        int index = 0;
        for (Map<String, Object> attributeMap : attributes) {
            Map<String, Object> attributeValues = (Map<String, Object>) attributeMap.get("attribute");
            Integer id = (Integer) attributeValues.get("id");
            if (!attrIdsToKeep.contains(id)) {
                // remove attributes not found in the survey
                removeAttrs.add(attributeMap);
            }
            
            // check the weight of the attribute and insert or append the values from attributesToAdd accordingly
            Integer weight = (Integer) attributeValues.get("weight");
            // check the attributesToAdd for any of this weight or less
            for (Attribute att : attributesToAdd) {
                if (att.getWeight() < weight) {
                    indexedAttributes.put(index++, att);
                }
            }
            // remove any added attributes from the attributesToAdd list
            attributesToAdd.removeAll(indexedAttributes.values());
            
            // increment the index for the element
            index++;
        }
        
        // add any remaining attributes from attributesToAdd
        for (Attribute att : attributesToAdd) {
            indexedAttributes.put(index++, att);
        }
        
        // add the indexed attributes to the list
        for (Entry<Integer, Attribute> entry : indexedAttributes.entrySet()) {
            // create an empty attribute value to hold the attribute
            AttributeValue value = new AttributeValue();
            value.setStringValue("");
            value.setAttribute(entry.getValue());
            attributes.add(entry.getKey(), value.flatten(2));
        }
        
        attributes.removeAll(removeAttrs);
    }
    
    /**
     * Filter attributes out of the flattened map to avoid changing the actual object
     * @param surveyId the survey for which the location is requested
     * @param location the location
     * @param locationObj flattened location object
     */
    public static void filterAttributesBySurveySimple(Survey survey, Location location, Map<String, Object> locationObj) {
        // create an Attribute-indexed map of the location attributes
        // so we can get the attribute value by attribute later
        Map<Attribute, AttributeValue> typedAttrMap = new HashMap<Attribute, AttributeValue>();
        for (AttributeValue attr : location.getAttributes()) {
            typedAttrMap.put(attr.getAttribute(), attr);
        }
        
        // get all of the attributes for the survey and filter - only keep 
        // attribute values for this location that match the survey attributes
        Set<Integer> attrIdsToKeep = new HashSet<Integer>();
        // also keep track of any location-scoped HTML attributes on the survey
        // as they will need to be added to the form too
        for(Attribute attribute : survey.getAttributes()) {
            AttributeValue value = typedAttrMap.get(attribute);
            if (value != null) {
                if(AttributeScope.LOCATION.equals(attribute.getScope())) {
                    attrIdsToKeep.add(value.getId());
                }
            }
        }
        
        List<Integer> orderedAttributes = (List<Integer>) locationObj.get("orderedAttributes");
        List<Integer> attributes = (List<Integer>) locationObj.get("attributes");
        Set<Integer> attrSet = new LinkedHashSet<Integer>(orderedAttributes);
        attrSet.retainAll(attrIdsToKeep);
        attributes.clear();
        attributes.addAll(attrSet);
        orderedAttributes.clear();
        orderedAttributes.addAll(attrSet);
    }
}
