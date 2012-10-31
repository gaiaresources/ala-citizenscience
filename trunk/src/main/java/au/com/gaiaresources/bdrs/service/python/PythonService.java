package au.com.gaiaresources.bdrs.service.python;

import au.com.gaiaresources.bdrs.controller.file.DownloadFileController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.python.AbstractPythonRenderable;
import au.com.gaiaresources.bdrs.python.PyBDRS;
import au.com.gaiaresources.bdrs.python.PyResponse;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import jep.Jep;
import jep.JepException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Provides common facilities for Python services that render web pages.
 */
public class PythonService {

    /**
     * Tile definition name for rendering reports.
     */
    public static final String RENDER_VIEW = "pythonRender";

    @Autowired
    protected LocalSessionFactoryBean sessionFactoryBean;

    private List<String> providedPythonContentDirList = null;

    private Logger log = Logger.getLogger(this.getClass());

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

    /**
     * @return a list of absolute paths to be added to the python path before starting the python interpreter.
     * @throws URISyntaxException
     */
    public List<String> getProvidedPythonContentDirs() throws URISyntaxException {

        if(this.providedPythonContentDirList == null) {
            File pythonResources = new File(PyBDRS.class.getResource("pybdrs").toURI()).getParentFile();

            ArrayList<String> temp = new ArrayList<String>();
            temp.add(pythonResources.getAbsolutePath());
            temp.add(new File(pythonResources, "django").getAbsolutePath());

            this.providedPythonContentDirList = Collections.unmodifiableList(temp);
        }
        return providedPythonContentDirList;
    }

    /**
     * Overrides the list of absolute python paths returned by {@link #getProvidedPythonContentDirs()}.
     * This method should be used if the locations of python resources are not in their usual places such as when
     * running unit tests.
     *
     * @param providedPythonContentDir the new list of absolute paths to be added to the python path or null if
     *                                 the default paths should be restored.
     */
    public void setProvidedPythonContentDirs(List<String> providedPythonContentDir) {
        this.providedPythonContentDirList = providedPythonContentDir;
    }

    /**
     * Configures and loads django into the provided python interpreter.
     *
     * @param jep the python interpreter where django will be loaded
     * @param bdrs the bdrs python bridge that is used to set up the location where django templates will be stored.
     */
    public void loadDjango(Jep jep, PyBDRS bdrs) throws JepException {
        int portalId = RequestContextHolder.getContext().getPortal().getId();

        DataSource dataSource = sessionFactoryBean.getDataSource();
        PropertyDescriptor usernamePD = BeanUtils.getPropertyDescriptor(dataSource.getClass(), "username");
        PropertyDescriptor passwordPD = BeanUtils.getPropertyDescriptor(dataSource.getClass(), "password");
        PropertyDescriptor urlPD = BeanUtils.getPropertyDescriptor(dataSource.getClass(), "url");

        try {
            String username = usernamePD.getReadMethod().invoke(dataSource).toString();
            String password = passwordPD.getReadMethod().invoke(dataSource).toString();
            String url = urlPD.getReadMethod().invoke(dataSource).toString();

            // Remove the start that reads "jdbc:" from jdbc:postgresql://<db_host>:<db_port>/<db_name>
            URI uri = URI.create(url.substring(5));

            String host = uri.getHost();
            int port = uri.getPort();
            // Remove the starting '/'. It will otherwise read "/<db_name>"
            String dbName = uri.getPath().substring(1);

            String djangoDBSettings = String.format(
                    "{'NAME':'%s','USER':'%s','PASSWORD':'%s','HOST': '%s','PORT': '%d',}",
                    dbName, username, password, host, port);

            // Set the environment variable for Django settings and import the Django models.
            // Note: We are only assigning the variable 'm' to silence the console output.
            jep.eval("import os");
            jep.eval("m = os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'bdrs.settings')");
            jep.eval("del m");

            jep.eval("from django.conf import settings");
            jep.eval(String.format("settings.DATABASES.get('default',{}).update(%s)", djangoDBSettings));
            jep.eval(String.format("settings.TEMPLATE_DIRS += ('%s',)", bdrs.toAbsolutePath("")));
            jep.eval(String.format("settings.PORTAL_ID = %d", portalId));
            jep.eval("del settings");

            jep.eval("from core import models");
        } catch (IllegalAccessException e) {
            // This should never occur
            throw new IllegalArgumentException("Failed to extract database credentials.");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to extract database credentials.");
        }
    }
}
