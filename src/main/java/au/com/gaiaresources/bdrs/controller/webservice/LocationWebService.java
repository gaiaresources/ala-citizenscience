package au.com.gaiaresources.bdrs.controller.webservice;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import au.com.gaiaresources.bdrs.model.taxa.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.location.LocationUtils;

import com.vividsolutions.jts.geom.Geometry;

@Controller
public class LocationWebService extends AbstractController {
    
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    
    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();
    
    public static final String JSON_KEY_ISVALID = "isValid";
    public static final String JSON_KEY_MESSAGE = "message";
    
    public static final String PARAM_WKT = "wkt";
    
    public static final String IS_WKT_VALID_URL = "/webservice/location/isValidWkt.htm";
    
    public static final String GET_LOCATION_BY_ID_URL = "/webservice/location/getLocationById.htm";
    
    public static final String PARAM_LOCATION_ID = "id";

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
    @RequestMapping(value=GET_LOCATION_BY_ID_URL, method=RequestMethod.GET)
    public void getLocationById(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value=PARAM_LOCATION_ID, required=true) int pk,
                                @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=false, defaultValue="-1") int surveyId,
                                @RequestParam(value="displayAttributes", required=false, defaultValue="false") boolean displayAttributes)
        throws IOException {

        Location location = locationDAO.getLocation(pk);
        // For simplicity make sure the output geometry is in WGS84. We could set it to the same
        // as the survey but then we would have to default back to WGS84 anyway. Added complexity for
        // little gain IMO
        Geometry geom = spatialUtil.transform(location.getLocation());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", location.getId());
        data.put("locations", geom.toString());
        data.put("name", location.getName());

        // Attach attribute values
        if(displayAttributes && surveyId > -1) {
            Survey survey = surveyDAO.getSurvey(surveyId);

            // Populate the known attribute values.
            Map<Attribute, AttributeValue> locAttrMap = new HashMap<Attribute, AttributeValue>();
            for(AttributeValue av : location.getAttributes(survey)) {
                if(av.getValue() != null) {
                    locAttrMap.put(av.getAttribute(), av);
                }
            }

            // Special handling for HTML type attributes.
            for(Attribute attr : survey.getAttributes()) {
                if( AttributeScope.LOCATION.equals(attr.getScope()) &&
                    AttributeUtil.isHTMLType(attr) &&
                    !locAttrMap.containsKey(attr)) {

                    AttributeValue tmp = new AttributeValue();
                    tmp.setAttribute(attr);
                    tmp.setStringValue("");
                    locAttrMap.put(attr, tmp);
                }
            }

            // Sort and serialize the attribute values
            List<AttributeValue> avList = new ArrayList<AttributeValue>(locAttrMap.values());
            Collections.sort(avList, new AttributeValueComparator());

            List<Map<String, Object>> flatAttrVals = new ArrayList<Map<String, Object>>(avList.size());
            for(AttributeValue av : avList) {
                flatAttrVals.add(av.flatten(1));
            }

            data.put("orderedAttributes", flatAttrVals);
        }

        super.writeJson(request, response, JSONObject.fromMapToString(data));
        getRequestContext().getHibernate().evict(location);
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
        loc.setLocation(spatialUtil.createPoint(latitude, longitude));
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
                Geometry geom = spatialUtil.createGeometryFromWKT(wkt);
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
}
