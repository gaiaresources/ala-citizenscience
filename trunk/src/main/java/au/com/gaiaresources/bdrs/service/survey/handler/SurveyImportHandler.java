package au.com.gaiaresources.bdrs.service.survey.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class SurveyImportHandler extends SimpleImportHandler {

	private GeoMapService geoMapService;
	private Logger log = Logger.getLogger(getClass());

    public SurveyImportHandler(SpatialUtilFactory spatialUtilFactory, Class<?> klazz, GeoMapService geoMapService) {
        super(spatialUtilFactory, klazz);
        this.geoMapService = geoMapService;
    }

    @Override
    public Object importData(Session sesh, JSONObject importData, Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {

        // Remove the representation from the registry of instances to be imported
        removeJSONPersistent(importData, jsonPersistent);
        Object bean = createBean(sesh, importData, persistentLookup, jsonPersistent);

        Survey newSurvey = (Survey) bean;
        // user is required, retrieve user from the request context here
        // Notify all listeners that we are about to save the instance.
        firePreSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newSurvey);

        // Save the instance and add it to the registry of saved data.
        sesh.save(newSurvey);
        
        GeoMap map = geoMapService.getForSurvey(sesh, newSurvey);
        map.setCrs(BdrsCoordReferenceSystem.valueOf(jsonPersistent.getString("crs")));
        
        addToPersistentLookup(persistentLookup, jsonPersistent, newSurvey);

        // Notify all listeners that the instance has been saved.
        firePostSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newSurvey);
        
        return newSurvey;
    }
}
