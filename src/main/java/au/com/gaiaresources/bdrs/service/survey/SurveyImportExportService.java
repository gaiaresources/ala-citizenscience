package au.com.gaiaresources.bdrs.service.survey;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.service.AbstractImportExportService;
import au.com.gaiaresources.bdrs.util.location.LocationUtils;

/**
 * This service handles the encoding and decoding of surveys to allow surveys to be exported from one BDRS and
 * imported into another. Currently, this service does not support the export of related Portal, Users, IndicatorSpecies
 * or Records.
 */
@Service
public class SurveyImportExportService extends AbstractImportExportService<Survey> {
    /* 
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportExportService#initService()
     */
    @Override
    @PostConstruct
    public void initService() {
        super.initService();
    }
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportExportService#exportSurvey(au.com.gaiaresources.bdrs.model.survey.Survey)
     */
    @Override
    public JSONObject exportObject(Survey survey) {
        // keep track of the exported census method list for census method attributes
        Set<Integer> exportedCensusMethodsInAttributes = new HashSet<Integer>();
        Set<Integer> exportedCensusMethods = new HashSet<Integer>();
        
        // { klazz : { id : JSONObject }}
        JSONObject exportData = new JSONObject();

        addToExport(exportData, survey);
        addMetadataToExport(exportData, survey.getMetadata());
        addAttributesToExport(exportData, survey.getAttributes(), exportedCensusMethods, exportedCensusMethodsInAttributes);
        addCensusMethodsToExport(exportData, survey.getCensusMethods(), new HashSet<Integer>());
        addLocationsToExport(exportData, survey.getLocations(), survey);

        return exportData;
    }

    /**
     * Encodes each of the specified Census Methods adding them to the provided JSON representation.
     *
     * @param exportData    the JSON representation of the object graph.
     * @param censusMethods the census methods to be encoded.
     * @param exportedCensusMethods 
     */
    private void addCensusMethodsToExport(JSONObject exportData, List<CensusMethod> censusMethods, Set<Integer> exportedCensusMethods) {
        for (CensusMethod method : censusMethods) {
            addCensusMethodToExport(exportData, method, exportedCensusMethods, new HashSet<Integer>());
        }
    }

    /**
     * Encodes each of the specified Census Methods adding them to the provided JSON representation.
     *
     * @param exportData    the JSON representation of the object graph.
     * @param exportedCensusMethods 
     * @param exportedCensusMethodsInAttributes 
     * @param censusMethods the census methods to be encoded.
     */
    private void addCensusMethodToExport(JSONObject exportData, CensusMethod method, Set<Integer> exportedCensusMethods, Set<Integer> exportedCensusMethodsInAttributes) {
        if (method != null && exportedCensusMethods.add(method.getId())) {
            addToExport(exportData, method);
            addMetadataToExport(exportData, method.getMetadata());
            addAttributesToExport(exportData, method.getAttributes(), new HashSet<Integer>(exportedCensusMethods), 
                                  exportedCensusMethodsInAttributes);
            addCensusMethodsToExport(exportData, method.getCensusMethods(), exportedCensusMethods);
        }
    }
    
    /**
     * Encodes each of the specified Locations adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param locations  the locations to be encoded.
     */
    private void addLocationsToExport(JSONObject exportData, Collection<Location> locations, Survey survey) {
        for (Location loc : locations) {
            // flatten the location and prune the attributes to match only the current survey
            Map<String, Object> flatLoc = loc.flatten();
            LocationUtils.filterAttributesBySurveySimple(survey, loc, flatLoc);
            this.addToExport(exportData, loc, JSONObject.fromMapToJSONObject(flatLoc));
            addMetadataToExport(exportData, loc.getMetadata());
            addAttributeValuesToExport(exportData, loc.getAttributes(survey));
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
            if (AttributeType.IMAGE.equals(type) || AttributeType.FILE.equals(type) || AttributeType.AUDIO.equals(type)) {
                flat.put(ImportHandler.FILE_CONTENT_KEY, encodeBase64(attrVal, attrVal.getStringValue()));
            } else if (AttributeType.isCensusMethodType(type)) {
                // flatten the records for the attribute value
                addRecordsToExport(exportData, attrVal.getRecords());
            }
            addToExport(exportData, attrVal, JSONObject.fromMapToJSONObject(flat));
        }
    }
    private void addRecordsToExport(JSONObject exportData, Set<Record> records) {
        // records can be null if no attribute values have been saved for 
        // census method attributes
        if (records != null) {
            for (Record rec : records) {
                addToExport(exportData, rec);
                addMetadataToExport(exportData, rec.getMetadata());
                addAttributeValuesToExport(exportData, rec.getAttributes());
            }
        }
    }

    /**
     * Encodes each of the specified Attributes adding them to the provided JSON representation.
     *
     * @param exportData the JSON representation of the object graph.
     * @param attributes the attributes to be encoded.
     * @param exportedCensusMethods 
     * @param exportedCensusMethodsInAttributes 
     */
    private void addAttributesToExport(JSONObject exportData, Collection<Attribute> attributes, 
            Set<Integer> exportedCensusMethods, Set<Integer> exportedCensusMethodsInAttributes) {
        for (Attribute attr : attributes) {
            addToExport(exportData, attr);
            if (AttributeType.isCensusMethodType(attr.getType())) {
                // export the census method and associated attributes if it has not been exported already
                if (exportedCensusMethodsInAttributes.add(attr.getCensusMethod().getId())) {
                    // create a copy of the set to keep the list safe for each attribute stack
                    addCensusMethodToExport(exportData, attr.getCensusMethod(), 
                                            exportedCensusMethods, 
                                            new HashSet<Integer>(exportedCensusMethodsInAttributes));
                }
            }
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
}
