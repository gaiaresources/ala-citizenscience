package au.com.gaiaresources.bdrs.controller.customform;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.net.URISyntaxException;

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
     * @param dir the report or custom form directory.
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

}