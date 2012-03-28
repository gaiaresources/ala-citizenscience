package au.com.gaiaresources.bdrs.service.survey.handler;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.service.survey.ImportHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.hibernate.classic.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * A dedicated {@ImportHandler} for {@link Metadata} that is aware of stored Survey Logo files.
 */
public class MetadataImportHandler extends SimpleImportHandler implements ImportHandlerListener {

    /**
     * The logging instance for this class.
     */
    private Logger log = Logger.getLogger(getClass());

    /**
     * Provides access to the filestore to save decoded files.
     */
    private FileService fileService;

    /**
     * Creates a new instance.
     *
     * @param locationService provides facilities to convert WKT strings to Geometry instances.
     * @param fileService     provides access to the filestore to save decoded files.
     */
    public MetadataImportHandler(LocationService locationService, FileService fileService) {
        super(locationService, Metadata.class);
        this.fileService = fileService;

        addListener(this);
    }

    @Override
    public void preSave(Session sesh, JSONObject importData, Map<Class, Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent, Object bean) {

    }

    @Override
    public void postSave(Session sesh, JSONObject importData, Map<Class, Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent, Object bean) {
        Metadata md = (Metadata) bean;
        if (!Metadata.SURVEY_LOGO.equals(md.getKey())) {
            return;
        }

        FileOutputStream fos = null;
        String encoded = null;
        if (!jsonPersistent.isNull(ImportHandler.FILE_CONTENT_KEY)) {
            encoded = jsonPersistent.optString(ImportHandler.FILE_CONTENT_KEY, null);
        }
        if (encoded == null) {
            log.warn(String.format("Unable to decode %s. The encoded data was null.", md.getKey()));
            return;
        }

        try {
            File file = File.createTempFile(md.getClass().getSimpleName(), String.valueOf(System.currentTimeMillis()));
            file.deleteOnExit();
            fos = new FileOutputStream(file);
            fos.write(Base64.decodeBase64(encoded));
            fos.flush();
            fos.close();

            fileService.createFile(md, file, md.getValue());

        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing to be done here.
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
