package au.com.gaiaresources.bdrs.python;

import au.com.gaiaresources.bdrs.json.JSONObject;
import edu.emory.mathcs.backport.java.util.Arrays;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Encapsulates the response that the Python report wishes to send back to the
 * requesting browser.
 */
public class PyResponse {

    /**
     * The content type string of a HTML document.
     */
    public static final String HTML_CONTENT_TYPE = "text/html";

    private String headerName = null;
    private String headerValue = null;
    private String contentType = HTML_CONTENT_TYPE;
    private byte[] content = "".getBytes(Charset.defaultCharset());
    private boolean isError = false;
    private boolean isStandalone = false;
    private String redirectURL = null;
    private Map<String, ?> redirectParams = null;
    private String errorMsg = "";

    /**
     * @return the headerName
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * @param headerName the headerName to set
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * @return the headerValue
     */
    public String getHeaderValue() {
        return headerValue;
    }

    /**
     * @param headerValue the headerValue to set
     */
    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param content sets the content of this response.
     */
    public void setContent(String content) {
        this.content = content.getBytes(Charset.defaultCharset());
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the content
     */
    public byte[] getContent() {
        return Arrays.copyOf(this.content, this.content.length);
    }

    /**
     * @return the text based content of this response.
     */
    public String getContentAsString() {
        return new String(this.getContent(), Charset.defaultCharset());
    }

    /**
     * @param content the content to set
     */
    public void setContent(byte[] content) {
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * @return the isStandalone
     */
    public boolean isStandalone() {
        return isStandalone;
    }

    /**
     * @param isStandalone the isStandalone to set
     */
    public void setStandalone(boolean isStandalone) {
        this.isStandalone = isStandalone;
    }

    /**
     * @return the isError
     */
    public boolean isError() {
        return isError;
    }

    /**
     * @param isError the isError to set
     */
    public void setError(boolean isError) {
        this.isError = isError;
    }

    /**
     * Sets the redirect parameters for the response.
     *
     * @param url        the target redirect url
     * @param jsonParams query parameters to be passed to the redirect url
     */
    public void setRedirect(String url, String jsonParams) {
        redirectURL = url == null ? null : url;
        redirectParams = jsonParams == null ? null : JSONObject.fromStringToJSONObject(jsonParams);
    }

    /**
     * @return true if redirect values have been setup, false otherwise.
     */
    public boolean isRedirect() {
        return redirectURL != null;
    }

    /**
     * @return the redirect URL is set, null otherwise.
     */
    public String getRedirectURL() {
        return redirectURL;
    }

    /**
     * @return redirect query parameters if set, null otherwise.
     */
    public Map<String, ?> getRedirectParams() {
        return redirectParams;
    }

    /**
     * Returns the stringified throwable object
     *
     * @return stringified throwable object
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Sets the stringified throwable object (normally called from python)
     *
     * @param errorMsg stringified throwable object
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
