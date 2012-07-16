package au.com.gaiaresources.bdrs.service.survey.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.hibernate.Session;
import org.springframework.beans.BeanUtils;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Handler for importing {@link User Users}. Will not import accounts that already exist.
 * 
 * @author stephanie
 *
 */
public class RecordImportHandler extends SimpleImportHandler {

    public RecordImportHandler(SpatialUtilFactory spatialUtilFactory, Class<?> klazz) {
        super(spatialUtilFactory, klazz);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.handler.AbstractImportHandler#importData(org.hibernate.Session, au.com.gaiaresources.bdrs.json.JSONObject, java.util.Map, au.com.gaiaresources.bdrs.json.JSONObject)
     */
    @Override
    public Object importData(Session sesh, JSONObject importData, Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {

        // Remove the representation from the registry of instances to be imported
        removeJSONPersistent(importData, jsonPersistent);
        Object bean = createBean(sesh, importData, persistentLookup, jsonPersistent);

        Record newRecord = (Record) bean;
        // user is required, retrieve user from the request context here
        newRecord.setUser(RequestContextHolder.getContext().getUser());
        // Notify all listeners that we are about to save the instance.
        firePreSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newRecord);

        // Save the instance and add it to the registry of saved data.
        sesh.save(newRecord);
        addToPersistentLookup(persistentLookup, jsonPersistent, newRecord);

        // Notify all listeners that the instance has been saved.
        firePostSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newRecord);
        
        return newRecord;
    }
    
    @Override
    protected Object createNewInstance() {
        Object bean = BeanUtils.instantiate(klazz);
        Record newRecord = (Record) bean;
        // user is required, retrieve user from the request context here
        newRecord.setUser(RequestContextHolder.getContext().getUser());
        return newRecord;
    }
}
