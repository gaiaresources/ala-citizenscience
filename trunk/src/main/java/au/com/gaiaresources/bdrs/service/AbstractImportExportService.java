package au.com.gaiaresources.bdrs.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.FileDataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.survey.ImportHandler;
import au.com.gaiaresources.bdrs.service.survey.ImportHandlerRegistry;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Provides a basic import/export service for PersistentImpl.
 * 
 * @author stephanie
 *
 */
public abstract class AbstractImportExportService<T extends PersistentImpl> implements ImportExportService<T> {
    /**
     * The logging instance for this class.
     */
    protected Logger log = Logger.getLogger(getClass());

    /**
     * Provides access to the BDRS file store.
     */
    @Autowired
    protected FileService fileService;
    
    
    @Autowired
    protected GeoMapService geoMapService;

    /**
     * Provides WKT to Geometry conversion facilities.
     */
    protected SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();

    /**
     * A mapping of datatypes (such as Survey) to their associated parsers.
     */
    protected ImportHandlerRegistry importHandlerRegistry;

    private Map<String, Object[]> messages = new LinkedHashMap<String, Object[]>();
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportExportService#initService()
     */
    @Override
    public void initService() {
        importHandlerRegistry = new ImportHandlerRegistry(spatialUtilFactory, fileService, null, geoMapService);
    }
    
    /**
     * Encodes the specified instance adding it to the JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param persistent the instance to be encoded.
     */
    protected void addToExport(JSONObject exportData, PersistentImpl persistent) {
        if (persistent == null) {
            return;
        }
        JSONObject persistentJson = JSONObject.fromMapToJSONObject(persistent.flatten());
        this.addToExport(exportData, persistent, persistentJson);
    }

    /**
     * Adds the specified encoded representation to the object graph.
     *
     * @param exportData     the JSON representation of the object graph.
     * @param persistent     the instance that is represented by the {#persistentJSON}
     * @param persistentJSON the encoded instance to be added to the object graph.
     */
    protected void addToExport(JSONObject exportData, PersistentImpl persistent, JSONObject persistentJSON) {
        String simpleName = persistent.getClass().getSimpleName();
        JSONObject klazzMapping = exportData.optJSONObject(simpleName);
        if (klazzMapping == null) {
            klazzMapping = new JSONObject();
            exportData.put(simpleName, klazzMapping);
        }

        klazzMapping.put(persistent.getId(), persistentJSON);
    }

    /**
     * Base 64 encodes the file for the specified persistent instance.
     *
     * @param persistent the persistence instance that owns the file to be encoded.
     * @param filename   the name of the file to be encoded.
     * @return the base 64 representation of the file for the specified persistent instance.
     */
    protected String encodeBase64(PersistentImpl persistent, String filename) {
        BufferedInputStream in = null;
        try {
            FileDataSource fileDataSource = fileService.getFile(persistent, filename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            in = new BufferedInputStream(fileDataSource.getInputStream());
            byte[] buffer = new byte[4096];
            int read = in.read(buffer, 0, buffer.length);
            while (read > -1) {
                baos.write(buffer, 0, read);
                read = in.read(buffer, 0, buffer.length);
            }
            return Base64.encodeBase64String(baos.toByteArray());
        } catch (IllegalArgumentException iae) {
            // This may be thrown if the file cannot be found.
            log.error(iae.getMessage(), iae);
        } catch (IOException ioe) {
            // Do nothing there will simply be a broken image on the other side.
            log.error(ioe.getMessage(), ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing else to be done here.
                    log.error(e.getMessage(), e);
                }
            }
        }

        return null;
    }

    /**
     * Encodes each of the specified Metadata adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param collection the metadata to be encoded.
     */
    protected void addMetadataToExport(JSONObject exportData, Collection<Metadata> collection) {
        for (Metadata md : collection) {
            Map<String, Object> flat = md.flatten();
            if (Metadata.SURVEY_LOGO.equals(md.getKey())) {
                flat.put(ImportHandler.FILE_CONTENT_KEY, encodeBase64(md, md.getValue()));
            } else if (Metadata.SURVEY_CSS.equals(md.getKey())) {
                flat.put(ImportHandler.FILE_CONTENT_KEY, encodeBase64(md, md.getValue()));
            }
            addToExport(exportData, md, JSONObject.fromMapToJSONObject(flat));
        }
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportExportService#importSurvey(org.hibernate.classic.Session, au.com.gaiaresources.bdrs.json.JSONObject)
     */
    @Override
    public boolean importObject(Session sesh, JSONObject importData)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        // clear messages before beginning a new import
        clearMessages();
        return this.importObjectInternal(sesh, importData);
    }
    
    protected boolean importObjectInternal(Session sesh, JSONObject importData)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Map<Class, Map<Integer, PersistentImpl>> persistentLookup =
                new HashMap<Class, Map<Integer, PersistentImpl>>(importHandlerRegistry.size());

        boolean importedAll = true;
        while (!importData.isEmpty()) {
            String klazzName = importData.keySet().iterator().next().toString();
            JSONObject idToJsonPersistentLookup = importData.getJSONObject(klazzName);

            Object id = idToJsonPersistentLookup.keySet().iterator().next();
            JSONObject jsonPersistent = idToJsonPersistentLookup.getJSONObject(id.toString());

            Object importedObj = importHandlerRegistry.importData(sesh, importData, persistentLookup, jsonPersistent);
            if (importedObj == null) {
                // the object was not imported, add a message
                importedAll = false;
                messages.putAll(importHandlerRegistry.getMessages());
            }
        }
        
        return importedAll;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.ImportExportService#exportList(java.util.List)
     */
    @Override
    public JSONArray exportArray(List<T> list) {
        JSONArray array = new JSONArray();
        
        for (T object : list) {
            array.add(exportObject(object));
        }
        
        return array;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.ImportExportService#importArray(Session, JSONArray)
     */
    @Override
    public int importArray(Session sesh, JSONArray importData) 
            throws InvocationTargetException, NoSuchMethodException, 
                   IllegalAccessException, InstantiationException {
        // clear messages before starting a new import
        clearMessages();
        int count = 0;
        for (Object object : importData) {
            if (importObjectInternal(sesh, (JSONObject) object)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the messages from the import.
     * 
     * @return
     */
    public Map<String, Object[]> getMessages() {
        return messages;
    }
    
    /**
     * Clears any messages from a previous import.
     */
    public void clearMessages() {
        messages.clear();
        importHandlerRegistry.clearMessages();
    }
}
