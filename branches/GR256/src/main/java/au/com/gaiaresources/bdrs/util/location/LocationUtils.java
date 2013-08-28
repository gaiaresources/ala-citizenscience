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
     * @param survey the survey for which the location is requested
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
