package au.com.gaiaresources.bdrs.python.deserializer;

import au.com.gaiaresources.bdrs.deserialization.record.RecordDeserializerResult;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.json.JSONSerializer;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.python.model.PyDAOUtil;
import au.com.gaiaresources.bdrs.servlet.Interceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Captures the result of a record deserialisation process.
 */
public class PyRecordDeserializerResult {

    /**
     * The deserialization result for each uploaded record.
     */
    private List<RecordDeserializerResult> resultList;
    /**
     * Performs all database access for record instances.
     */
    private RecordDAO recordDAO;
    /**
     * The client request.
     */
    private HttpServletRequest request;

    /**
     * Creates a new instance.
     * @param request the client request.
     * @param recordDAO performs all database access for record instances.
     * @param resultList the deserialization result for each record.
     */
    public PyRecordDeserializerResult(HttpServletRequest request, RecordDAO recordDAO, List<RecordDeserializerResult> resultList) {
        this.request = request;
        this.resultList = resultList;
        this.recordDAO = recordDAO;
    }

    /**
     * @return A JSON encoded string representing a boolean true if an error has occured, or false otherwise.
     */
    public String hasError() {
        boolean hasError = false;
        for (RecordDeserializerResult res : this.resultList) {
            if(!hasError) {
                if (!res.isAuthorizedAccess()) {
                    // Required since there will be an auto commit otherwise at the end of controller handling.
                    hasError = true;
                }

                if (!hasError && !res.getErrorMap().isEmpty()) {
                    hasError = true;
                }
            }
        }
        return hasError ? "true" : "false";
    }

    /**
     * @return the JSON encoded deserialization result for all records.
     */
    public String getResults() {
        JSONArray array = new JSONArray();
        for (RecordDeserializerResult res : this.resultList) {
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("errorMap", res.getErrorMap());
            jsonResult.put("record", PyDAOUtil.toJSON(res.getRecord()));
            jsonResult.put("isAuthorizedAccess", res.isAuthorizedAccess());

            array.add(jsonResult);
        }
        return array.toString();
    }

    /**
     * Saves each of the deserialized records.
     */
    public void save() {
        for (RecordDeserializerResult res : this.resultList) {
            recordDAO.save(res.getRecord());
        }
    }

    /**
     * Sets a flag indicating that at the end of this request, the transaction should be rolled back.
     */
    public void rollback() {
        Interceptor.requestRollback(request);
    }
}
