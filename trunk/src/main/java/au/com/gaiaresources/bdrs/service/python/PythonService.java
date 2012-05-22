package au.com.gaiaresources.bdrs.service.python;

import au.com.gaiaresources.bdrs.controller.file.DownloadFileController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.python.AbstractPythonRenderable;
import au.com.gaiaresources.bdrs.python.PyBDRS;
import au.com.gaiaresources.bdrs.python.PyResponse;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.commons.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides common facilities for Python services that render web pages.
 */
public class PythonService {

    /**
     * Tile definition name for rendering reports.
     */
    public static final String RENDER_VIEW = "pythonRender";

    private String providedPythonContentDir = null;

    /**
     * Provides static files such as media or javascript
     * without the need to create a Python interpreter. This handler will
     * rewrite the URL and delegate the servicing of the request to the
     * {@link au.com.gaiaresources.bdrs.controller.file.DownloadFileController}.
     *
     * @param renderable the persistent that 'owns' the files to be retrieved.
     * @param filePath   the relative path of the file to retrieve.
     */
    public ModelAndView downloadStaticFile(AbstractPythonRenderable renderable, String filePath) {
        if (renderable == null) {
            throw new NullPointerException();
        }

        File target = new File(renderable.getContentDir(), filePath);
        ModelAndView mv = new ModelAndView(new PortalRedirectView(DownloadFileController.FILE_DOWNLOAD_URL, true));
        mv.addObject(DownloadFileController.CLASS_NAME_QUERY_PARAM, renderable.getClass().getCanonicalName());
        mv.addObject(DownloadFileController.INSTANCE_ID_QUERY_PARAM, renderable.getId());
        mv.addObject(DownloadFileController.FILENAME_QUERY_PARAM, target.getPath());

        return mv;
    }

    /**
     * Sets the header of the server response if a header was provided.
     * Setting the header allows a page to generate a dynamically created file download.
     *
     * @param response   the server response.
     * @param pyResponse the response object that contains the header (if set).
     */
    protected void updateHeader(HttpServletResponse response, PyResponse pyResponse) {
        if (pyResponse.getHeaderName() != null && pyResponse.getHeaderValue() != null) {
            response.setHeader(pyResponse.getHeaderName(), pyResponse.getHeaderValue());
        }
    }


    /**
     * JSON encodes all query parameters. The JSON object will take the form
     * { string : [string, string, string, ...}
     *
     * @param request the browser request
     * @return a JSON encoded object of all the query parameters in the request. If the request is a multipart
     *         http request, then all uploaded data files will be base64 encoded
     *         and included in the <code>JSONObject</code>.
     * @throws java.io.IOException if there is an error reading data from the multipart request.
     */
    protected JSONObject toJSONParams(HttpServletRequest request) throws IOException {
        // The documentation says the map is of the specified type.
        @SuppressWarnings("unchecked")
        Map<String, String[]> rawMap = request.getParameterMap();

        Map<String, List<String>> paramMap =
                new HashMap<String, List<String>>(rawMap.size());
        for (Map.Entry<String, String[]> entry : rawMap.entrySet()) {
            paramMap.put(entry.getKey(), JSONArray.fromList(Arrays.asList(entry.getValue())));
        }

        JSONObject params = new JSONObject();
        params.accumulateAll(paramMap);

        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest req = (MultipartHttpServletRequest) request;
            // Base 64 encode all uploaded file data

            for (Map.Entry<String, MultipartFile> pair : req.getFileMap().entrySet()) {
                String data = Base64.encodeBase64String(pair.getValue().getBytes());
                params.accumulate(pair.getKey(), data);
            }
        }
        return params;
    }

    public String getProvidedPythonContentDir() throws URISyntaxException {
        if(this.providedPythonContentDir == null) {
            this.providedPythonContentDir = new File(PyBDRS.class.getResource("pybdrs").toURI()).getParentFile().getAbsolutePath();
        }
        return providedPythonContentDir;
    }

    public void setProvidedPythonContentDir(String providedPythonContentDir) {
        this.providedPythonContentDir = providedPythonContentDir;
    }
}
