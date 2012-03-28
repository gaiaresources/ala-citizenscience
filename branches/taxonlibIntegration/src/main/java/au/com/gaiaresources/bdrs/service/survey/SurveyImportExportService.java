package au.com.gaiaresources.bdrs.service.survey;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.hibernate.classic.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service handles the encoding and decoding of surveys to allow surveys to be exported from one BDRS and
 * imported into another. Currently, this service does not support the export of related Portal, Users, IndicatorSpecies
 * or Records.
 */
@Service
public class SurveyImportExportService {
    /**
     * The logging instance for this class.
     */
    private Logger log = Logger.getLogger(getClass());

    /**
     * Provides access to the BDRS file store.
     */
    @Autowired
    private FileService fileService;

    /**
     * Provides WKT to Geometry conversion facilities.
     */
    @Autowired
    private LocationService locationService;

    /**
     * A mapping of datatypes (such as Survey) to their associated parsers.
     */
    private ImportHandlerRegistry importHandlerRegistry;

    /**
     * Executed after dependency injection is done to perform any initialization.
     */
    @PostConstruct
    public void initService() {
        importHandlerRegistry = new ImportHandlerRegistry(locationService, fileService);
    }

    /**
     * Encodes the specified survey into a JSON representation.
     *
     * @param survey the survey to be encoded.
     * @return the encoded representation of this survey.
     */
    public JSONObject exportSurvey(Survey survey) {
        // { klazz : { id : JSONObject }}
        JSONObject exportData = new JSONObject();

        addToExport(exportData, survey);
        addMetadataToExport(exportData, survey.getMetadata());
        addAttributesToExport(exportData, survey.getAttributes());
        addCensusMethodsToExport(exportData, survey.getCensusMethods());
        addLocationsToExport(exportData, survey.getLocations());

        return exportData;
    }

    /**
     * Encodes each of the specified Census Methods adding them to the provided JSON representation.
     *
     * @param exportData    the JSON representation of the object graph.
     * @param censusMethods the census methods to be encoded.
     */
    private void addCensusMethodsToExport(JSONObject exportData, List<CensusMethod> censusMethods) {
        for (CensusMethod method : censusMethods) {
            if (method != null) {
                addToExport(exportData, method);
                addMetadataToExport(exportData, method.getMetadata());
                addAttributesToExport(exportData, method.getAttributes());
                addCensusMethodsToExport(exportData, method.getCensusMethods());
            }
        }
    }

    /**
     * Encodes the specified instance adding it to the JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param persistent the instance to be encoded.
     */
    private void addToExport(JSONObject exportData, PersistentImpl persistent) {
        if (persistent == null) {
            return;
        }
        this.addToExport(exportData, persistent, JSONObject.fromMapToJSONObject(persistent.flatten()));
    }

    /**
     * Adds the specified encoded representation to the object graph.
     *
     * @param exportData     the JSON representation of the object graph.
     * @param persistent     the instance that is represented by the {#persistentJSON}
     * @param persistentJSON the encoded instance to be added to the object graph.
     */
    private void addToExport(JSONObject exportData, PersistentImpl persistent, JSONObject persistentJSON) {
        String simpleName = persistent.getClass().getSimpleName();
        JSONObject klazzMapping = exportData.optJSONObject(simpleName);
        if (klazzMapping == null) {
            klazzMapping = new JSONObject();
            exportData.put(simpleName, klazzMapping);
        }

        klazzMapping.put(persistent.getId(), persistentJSON);
    }

    /**
     * Encodes each of the specified Locations adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param locations  the locations to be encoded.
     */
    private void addLocationsToExport(JSONObject exportData, Collection<Location> locations) {
        for (Location loc : locations) {
            addToExport(exportData, loc);
            addMetadataToExport(exportData, loc.getMetadata());
            addAttributeValuesToExport(exportData, loc.getAttributes());
        }
    }

    /**
     * Encodes each of the specified AttributeValues adding them to the provided JSON representation.
     *
     * @param exportData      the JSON representation of the object graph.
     * @param attributeValues the attribute values to be encoded.
     */
    private void addAttributeValuesToExport(JSONObject exportData, Collection<AttributeValue> attributeValues) {
        for (AttributeValue attrVal : attributeValues) {
            Map<String, Object> flat = attrVal.flatten();
            AttributeType type = attrVal.getAttribute().getType();
            if (AttributeType.IMAGE.equals(type) || AttributeType.FILE.equals(type)) {
                flat.put(ImportHandler.FILE_CONTENT_KEY, encodeBase64(attrVal, attrVal.getStringValue()));
            }
            addToExport(exportData, attrVal, JSONObject.fromMapToJSONObject(flat));
        }
    }

    /**
     * Base 64 encodes the file for the specified persistent instance.
     *
     * @param persistent the persistence instance that owns the file to be encoded.
     * @param filename   the name of the file to be encoded.
     * @return the base 64 representation of the file for the specified persistent instance.
     */
    private String encodeBase64(PersistentImpl persistent, String filename) {
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
    private void addMetadataToExport(JSONObject exportData, Collection<Metadata> collection) {
        for (Metadata md : collection) {
            Map<String, Object> flat = md.flatten();
            if (Metadata.SURVEY_LOGO.equals(md.getKey())) {
                flat.put(ImportHandler.FILE_CONTENT_KEY, encodeBase64(md, md.getValue()));
            }
            addToExport(exportData, md, JSONObject.fromMapToJSONObject(flat));
        }
    }

    /**
     * Encodes each of the specified Attributes adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param attributes the attributes to be encoded.
     */
    private void addAttributesToExport(JSONObject exportData, Collection<Attribute> attributes) {
        for (Attribute attr : attributes) {
            addToExport(exportData, attr);
            addAttributeOptionsToExport(exportData, attr.getOptions());
        }
    }

    /**
     * Encodes each of the specified AttributeOptions adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param options    the attribute options to be encoded.
     */
    private void addAttributeOptionsToExport(JSONObject exportData, List<AttributeOption> options) {
        for (AttributeOption opt : options) {
            addToExport(exportData, opt);
        }
    }

    /**
     * Imports the specified object graph using the provided session.
     *
     * @param sesh       the session to use when saving created instance.
     * @param importData the object graph to be imported.
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    public void importSurvey(Session sesh, JSONObject importData)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Map<Class, Map<Integer, PersistentImpl>> persistentLookup =
                new HashMap<Class, Map<Integer, PersistentImpl>>(importHandlerRegistry.size());

        while (!importData.isEmpty()) {
            String klazzName = importData.keySet().iterator().next().toString();
            JSONObject idToJsonPersistentLookup = importData.getJSONObject(klazzName);

            Object id = idToJsonPersistentLookup.keySet().iterator().next();
            JSONObject jsonPersistent = idToJsonPersistentLookup.getJSONObject(id.toString());

            importHandlerRegistry.importData(sesh, importData, persistentLookup, jsonPersistent);
        }
    }

}
