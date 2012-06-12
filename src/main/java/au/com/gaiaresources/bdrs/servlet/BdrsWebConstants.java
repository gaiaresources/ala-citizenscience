package au.com.gaiaresources.bdrs.servlet;

/**
 * Constants for use with HTTP requests and sessions.
 * 
 * @author stephanie
 */
public class BdrsWebConstants {
    /**
     * Constant for storing a request URL in the session attributes for redirection on login.
     */
    public static final String SAVED_REQUEST_KEY = "bdrs-security-redirect";
    /**
     * Constant for web form parameter "recordId"
     */
    public static final String PARAM_RECORD_ID = "recordId";
    /**
     * Constant for web form parameter "surveyId"
     */
    public static final String PARAM_SURVEY_ID = "surveyId";
    /**
     * Constant for web form parameter "censusMethodId"
     */
    public static final String PARAM_CENSUS_METHOD_ID = "censusMethodId";
    /**
     * Constant for web form parameter "locationId"
     */
    public static final String PARAM_LOCATION_ID = "locationId";

    /**
     * Constant for the web form parameter "commentId".
     */
    public static final String PARAM_COMMENT_ID = "commentId";
    /**
     * Constant for the session attribute name that stores property/attribute specific errors
     */
    public static final String MV_ERROR_MAP = "errorMap";
    /**
     * Constant for the session attribute name that stores property/attribute values on error.
     */
    public static final String MV_VALUE_MAP = "valueMap";
    /**
     * Constant for the session attribute name that stores the wkt string of the location on error.
     */
    public static final String MV_WKT = "wkt";
    /**
     * Constant for adding a WebMap object to the ModelAndView
     */
    public static final String MV_WEB_MAP = "webMap";
}
