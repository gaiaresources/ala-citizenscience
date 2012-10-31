package au.com.gaiaresources.bdrs.python;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.service.python.PythonService;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Performs test utility functions that assist report and custom form testing.
 */
public class PythonTestUtil {

    private PythonTestUtil() {
        // Do nothing. Cannot instantiate a utility class.
    }

    /**
     * Generates the URL to render the report or custom form.
     *
     * @param formRenderUrl the render url provided to Spring.
     * @param queryParam    the name of the url fragment where spring will insert the primary key.
     * @param persistent    the report or custom form to be rendered.
     * @return the URL to render the report or custom form.
     */
    public static String getRenderURL(String formRenderUrl, String queryParam, PersistentImpl persistent) {
        return formRenderUrl.replace(queryParam, String.valueOf(persistent.getId()));
    }

    /**
     * Creates an uploadable file from the specified directory.
     *
     * @param dir      the directory to be compressed.
     * @param fileName the target name of the compressed file (without extension)
     * @param postKey  the name of this item in the post dictionary
     * @return an uploadable file from the specified directory.
     * @throws URISyntaxException
     * @throws IOException
     */
    public static MockMultipartFile getTestFile(File dir, String fileName, String postKey) throws URISyntaxException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipUtils.compressToStream(dir.listFiles(), baos);

        return new MockMultipartFile(postKey,
                String.format("%s.zip", fileName),
                "application/zip",
                baos.toByteArray());
    }

    /**
     * Returns the configuration file for the specified report or custom form directory.
     *
     * @param dir            the report or custom form directory.
     * @param configFilename the name of the configuration file.
     * @return the configuration file for the specified report or custom form directory.
     * @throws IOException
     * @throws URISyntaxException
     */
    public static JSONObject getConfigFile(File dir, String configFilename) throws IOException, URISyntaxException {
        File config = new File(dir, configFilename);
        return JSONObject.fromStringToJSONObject(readFileAsString(config.getAbsolutePath()));
    }

    /**
     * Reads the specified file returning its contents as a String.
     *
     * @param filePath the file to be read.
     * @return the contents of the file.
     * @throws java.io.IOException
     */
    public static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ignored) {
                }
            }
        }
        return new String(buffer);
    }

    public static void setupReports(PythonService service) throws IOException, URISyntaxException {
        File providedPythonResources = File.createTempFile("bdrs_provided_python_", String.valueOf(System.currentTimeMillis()));
        providedPythonResources.delete();
        providedPythonResources.mkdir();
        providedPythonResources.deleteOnExit();

        ArrayList<String> providedPythonResourceDirs = new ArrayList<String>();

        // PyBDRS
        {
            File pybdrs = new File(providedPythonResources, "pybdrs");
            pybdrs.mkdir();

            String[] resources = getResourceListing(PyBDRS.class, "au/com/gaiaresources/bdrs/python/pybdrs/");
            for (String resource_path : resources) {
                writeResource(providedPythonResources, PyBDRS.class, String.format("pybdrs/%s", resource_path));
            }
            providedPythonResourceDirs.add(providedPythonResources.getAbsolutePath());
        }

        // Django
        {
            File django = new File(providedPythonResources, "django");
            django.mkdir();

            String[] resources = getResourceListing(PyBDRS.class, "au/com/gaiaresources/bdrs/python/django/");
            for (String resource_path : resources) {
                writeResource(providedPythonResources, PyBDRS.class, String.format("django/%s", resource_path));
            }
            providedPythonResourceDirs.add(django.getAbsolutePath());
        }

        service.setProvidedPythonContentDirs(providedPythonResourceDirs);
    }

    private static void writeResource(File parent, Class klazz, String resource_path) throws IOException {
        if (resource_path == null) {
            return;
        }

        if (resource_path.isEmpty()) {
            return;
        }
        InputStream in = klazz.getResourceAsStream(resource_path);
        if (in.available() > 0) {
            File file = new File(parent, resource_path);
            File subParent = file.getParentFile();
            if (!subParent.exists()) {
                subParent.mkdirs();
            }

            BufferedInputStream bis = new BufferedInputStream(in);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[4096];
            for (int read = bis.read(buffer); read > -1; read = bis.read(buffer)) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
            bos.close();
            bis.close();
        }
    }

    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path  Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     * @author Greg Briggs
     */
    private static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
            * In case of a jar file, we can't actually find a directory.
            * Have to assume the same jar as clazz.
            */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.substring(path.length());
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }
        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

}