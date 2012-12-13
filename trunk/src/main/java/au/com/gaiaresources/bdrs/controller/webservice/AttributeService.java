package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Webservice for performing attribute operations.
 * 
 * @author anthony
 *
 */
@Controller
public class AttributeService extends AbstractController {
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private AttributeDAO attributeDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RecordDAO recordDAO;

    /**
     * Search through the list of record attributes for values that match the
     * given pattern. This is mainly used for autocomplete of fields.
     * 
     * @param ident - Ident key for the user
     * @param q - The search string fragment
     * @param attributePk - Primary key of the attribute to search record attributes
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/webservice/attribute/searchValues.htm", method = RequestMethod.GET)
    public void searchValues(@RequestParam(value="ident", defaultValue="") String ident,
                                @RequestParam(value="q", defaultValue="") String q,
                                @RequestParam(value="attribute", defaultValue="0") int attributePk,
                                HttpServletResponse response) throws IOException {

        if (!validateUser(response, ident)) {
            return;
        }

        List<String> values = attributeDAO.getAttributeValues(attributePk, q);
        JSONArray array = new JSONArray();
        for(String value : values) {
            array.add(value);
        }

        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    /**
     * Checks that either the user already has authenticated or has supplied a valid token.
     * Note that if the user is not valid a 401 response will be written.
     * @param response the response to be produced.
     * @param ident identifies the user making the request.  Not required if the request is part of an authenticated session.
     * @return true if the user is valid, false otherwise.  If false, the response will have been written.
     * @throws IOException if there is an error writing to the response.
     */
    private boolean validateUser(HttpServletResponse response, String ident) throws IOException {
        User user = getRequestContext().getUser();
        if (user == null) {
            if(!ident.isEmpty()) {
                user = userDAO.getUserByRegistrationKey(ident);
            }
        }
        if (user == null) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access this service");
        }
        return user != null;
    }

    /**
     * Returns a list of records that have an attribute recorded with the supplied name and value.
     * @param ident identifies the user making the request.  Not required if the request is part of an authenticated session.
     * @param surveyId the survey the attribute is a part of.
     * @param attributeName the name of the attribute of interest
     * @param attributeValue the value of the attribute of interest
     * @param response the response being produced.
     * @throws IOException if there is an error writing to the response.
     */
    @RequestMapping(value = "/webservice/attribute/recordsByAttributeValue.htm", method = RequestMethod.GET)
    public void findRecordsByAttributeValue(@RequestParam(value="ident", defaultValue="") String ident,
                                            @RequestParam(value="surveyId", defaultValue="0") int surveyId,
                                            @RequestParam(value="attributeName", defaultValue="") String attributeName,
                                            @RequestParam(value="attributeValue", defaultValue="") String attributeValue,
                                            HttpServletResponse response) throws IOException {

        if (!validateUser(response, ident)) {
            return;
        }
        JSONObject result = new JSONObject();

        try {
            List<Record> records = recordDAO.getRecordByAttributeValue(null, surveyId, attributeName, attributeValue);
            JSONArray jsonRecords = new JSONArray();
            for (Record record : records) {
                jsonRecords.add(record.flatten());
            }
            result.put("records", jsonRecords);

        }
        catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", HttpServletResponse.SC_BAD_REQUEST);
            result.put("message", e.getMessage());
        }

        response.setContentType("application/json");
        result.writeJSONString(response.getWriter());
    }

    /**
     * Given a record id, and attribute id, returns (as JSON) the attribute values entered for the census method identified by
     * the supplied attribute id.
     * @param ident identifies the user making the request.  Not required if the request is part of an authenticated session.
     * @param recordId identifies the record of interest.
     * @param attributeId must be the id of an attribute which has a census method type.
     * @param response the response to be produced.
     * @throws IOException if there is an error writing to the response.
     */
    @RequestMapping(value = "/webservice/attribute/getCensusMethodAttributes.htm", method = RequestMethod.GET)
    public void findCensusMethodAttributesByAttributeId(@RequestParam(value="ident", defaultValue="") String ident,
                                                        @RequestParam(value="recordId", defaultValue="0") int recordId,
                                                        @RequestParam(value="attributeId", defaultValue="0") int attributeId,
                                                        HttpServletResponse response) throws IOException {

        if (!validateUser(response, ident)) {
            return;
        }

        if (recordId == 0 || attributeId == 0) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "recordId and attributeId must be supplied");
            return;
        }
        JSONObject result = new JSONObject();

        Record record = recordDAO.getRecord(recordId);
        if (record == null) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "No record exists with id "+recordId);
            return;
        }
        JSONArray values = new JSONArray();

        for (AttributeValue val : record.getAttributes()) {
            if (attributeId == val.getAttribute().getId()) {
                if (!AttributeType.isCensusMethodType(val.getAttribute().getType())) {
                    writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Attribute "+attributeId+" is not a census method attribute");
                }
                addAttributeValues(values, val.getRecords());
            }
        }
        result.put("values", values);
        result.put("status", HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        result.writeJSONString(response.getWriter());
    }

    private void addAttributeValues(JSONArray values, Set<Record> records) {
        for (Record childRecord : records) {
            for (AttributeValue childVal : childRecord.getAttributes()) {
                if (AttributeType.isCensusMethodType(childVal.getAttribute().getType())) {
                    addAttributeValues(values, childVal.getRecords());
                }
                else {
                    values.add(childVal.flatten());
                }
            }
        }
    }


    @InitBinder
    public void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }

    private void writeErrorResponse(HttpServletResponse response, int responseCode, String message) throws IOException {
        JSONObject result = new JSONObject();

        response.setStatus(responseCode);
        result.put("status", responseCode);
        result.put("message", message);

        response.setContentType("application/json");
        result.writeJSONString(response.getWriter());
    }
}
