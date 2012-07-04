package au.com.gaiaresources.bdrs.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.user.User;

/**
 * Interface for creating an import/export service for PersistentImpl.
 * 
 * @author stephanie
 */
public interface ImportExportService<T extends PersistentImpl> {

    /**
     * Executed after dependency injection is done to perform any initialization.
     */
    public void initService();

    /**
     * Encodes the specified object into a JSON representation.
     *
     * @param exportObject the object to be encoded.
     * @return the encoded representation of this object.
     */
    public JSONObject exportObject(T exportObject);

    /**
     * Imports the specified object graph using the provided session.
     *
     * @param sesh       the session to use when saving created instance.
     * @param importData the object graph to be imported.
     * @return true if the object was successfully imported, false otherwise
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    public boolean importObject(Session sesh, JSONObject importData)
            throws InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException;

    /**
     * Encodes a list of objects into a JSON representation.
     * 
     * @param list
     */
    public JSONArray exportArray(List<T> list);

    /**
     * Imports an array of objects using the provided session.
     * 
     * @param sesh       the session to use when saving created instance.
     * @param importData the array graph to be imported.
     * @return the number of objects imported
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    public int importArray(Session sesh, JSONArray importData) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException;
}