package au.com.gaiaresources.bdrs.service.survey;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.service.AbstractImportExportService;

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
}
