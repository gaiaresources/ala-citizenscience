package au.com.gaiaresources.bdrs.controller.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeUtil;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

import com.vividsolutions.jts.geom.Geometry;

@Controller
public class LocationWebService extends AbstractController {
    @Autowired
    private au.com.gaiaresources.bdrs.model.location.LocationService locationService;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    
    public static final String JSON_KEY_ISVALID = "isValid";
    public static final String JSON_KEY_MESSAGE = "message";
    
    public static final String PARAM_WKT = "wkt";
    
    public static final String IS_WKT_VALID_URL = "/webservice/location/isValidWkt.htm";

    private Logger log = Logger.getLogger(getClass());
    
    /**
     * Service to get a {@link Location} object by id. Returns the object in JSONObject
     * form. The surveyId parameter should be specified in order to prevent 
     * {@link AttributeValue AttributeValues} from other surveys from being returned
     * as part of the object.
     * @param request
     * @param response
     * @param pk the id of the {@link Location}
     * @param surveyId the id of the {@link Survey} (used to filter attributes)
     * @throws IOException
     */
    @RequestMapping(value="/webservice/location/getLocationById.htm", method=RequestMethod.GET)
    public void getLocationById(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value="id", required=true) int pk,
                                @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=false, defaultValue="-1") int surveyId)
        throws IOException {

        Location location = locationDAO.getLocation(pk);
        
        Map<String,Object> locationObj = location.flatten(2);
        if (surveyId != -1) {
            filterAttributesBySurvey(surveyId, location, locationObj);
        }
        
        response.setContentType("application/json");
        response.getWriter().write(JSONObject.fromMapToString(locationObj));
    }

    @RequestMapping(value="/webservice/location/getLocationsById.htm", method=RequestMethod.GET)
    public void getLocationsById(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value="ids", required=true) String ids)
        throws IOException {

        JSONArray arr = JSONArray.fromString(ids);
        List<Integer> idList = new ArrayList<Integer>();
        for (int i = 0; i < arr.size(); i++) {
            idList.add(Integer.parseInt(arr.get(i).toString()));
        }
        List<Location> locationList = locationDAO.getLocations(idList);
        Geometry g = null;
        for (Location loc : locationList) {
            Geometry locGeo = loc.getLocation().getEnvelope();
            
            if (g == null) {
                g = locGeo;
            } else {
                g = g.getEnvelope().union(locGeo);
            }
        }
        JSONObject ob = new JSONObject();
        ob.put("geometry", g.toText());
        response.setContentType("application/json");
        response.getWriter().write(ob.toString());
    }

    @RequestMapping(value="/webservice/location/bookmarkUserLocation.htm", method=RequestMethod.GET)
    public void bookmarkUserLocation(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value="ident", required=true) String ident,
                                @RequestParam(value="locationName", required=true) String locationName,
                                @RequestParam(value="latitude", required=true) double latitude,
                                @RequestParam(value="longitude", required=true) double longitude,
                                @RequestParam(value="isDefault", required=true) boolean isDefault)
        throws IOException {

        if(locationName.isEmpty()) {
            locationName = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        }
        
        User user = userDAO.getUserByRegistrationKey(ident);
        if(user == null) {
            // Perhaps a wrong ident or a wrong portal
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }
        
        Location loc = new Location();
        loc.setName(locationName);
        loc.setUser(user);
        loc.setLocation(locationService.createPoint(latitude, longitude));
        loc = locationDAO.save(loc);
        
        if(isDefault) {
            Metadata defaultLocIdMd = user.getMetadataObj(Metadata.DEFAULT_LOCATION_ID);
            if(defaultLocIdMd == null) {
                defaultLocIdMd = new Metadata();
                defaultLocIdMd.setKey(Metadata.DEFAULT_LOCATION_ID);
            }
            defaultLocIdMd.setValue(loc.getId().toString());
            metadataDAO.save(defaultLocIdMd);
            
            user.getMetadata().add(defaultLocIdMd);
            userDAO.updateUser(user);
        }
        
        response.setContentType("application/json");
        response.getWriter().write(JSONObject.fromMapToString(loc.flatten()));
    }
    
    @RequestMapping(value=IS_WKT_VALID_URL, method=RequestMethod.GET)
    public void wktValidation(HttpServletRequest request,
                                HttpServletResponse response, 
                                @RequestParam(value=PARAM_WKT, required=false) String wkt) throws IOException {
        
        JSONObject result = new JSONObject();
        result.put(JSON_KEY_ISVALID, true);
        result.put(JSON_KEY_MESSAGE, "");
        
        if (!StringUtils.hasLength(wkt)) {
            result.put(JSON_KEY_ISVALID, false);
            result.put(JSON_KEY_MESSAGE, "");   // no message
        } else {
            try {
                Geometry geom = locationService.createGeometryFromWKT(wkt);
                if (geom == null) {
                    result.put(JSON_KEY_ISVALID, false);
                    result.put(JSON_KEY_MESSAGE, "Geometry is null");
                } else {
                    if (!geom.isValid()) {
                        result.put(JSON_KEY_ISVALID, false);
                        result.put(JSON_KEY_MESSAGE, "Geometry is invalid. Note that self intersecting polygons are not allowed.");
                    }
                }
            } catch (IllegalArgumentException iae) {
                result.put(JSON_KEY_ISVALID, false);
                result.put(JSON_KEY_MESSAGE, iae.getMessage());
            }  
        }
        writeJson(request, response, result.toString());
    }
    
    /**
     * Filter attributes out of the flattened map to avoid changing the actual object
     * @param surveyId the survey for which the location is requested
     * @param location the location
     * @param locationObj flattened location object
     */
    private void filterAttributesBySurvey(int surveyId, Location location, Map<String, Object> locationObj) {
        Survey survey = surveyDAO.get(surveyId);
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
}
