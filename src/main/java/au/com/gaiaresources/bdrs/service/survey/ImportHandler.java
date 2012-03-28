package au.com.gaiaresources.bdrs.service.survey;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.service.survey.handler.ImportHandlerListener;
import org.hibernate.classic.Session;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Implemented by classes registered in the {@link ImportHandlerRegistry}, this interface provides the generic
 * invocation point to import data from a {@link JSONObject} to the system.
 */
public interface ImportHandler {

    /**
     * A special key that gets associated with a base 64 encoding of a file associated with a persistent instance.
     */
    public static final String FILE_CONTENT_KEY = "_fileContent";

    /**
     * Adds a listener that receives callbacks during various stages of the decoding phase.
     *
     * @param listener the listener that shall be invoked during various stages of the decoding phase.
     */
    public void addListener(ImportHandlerListener listener);

    /**
     * Removes a listener that receives callbacks during various stages of the decoding phase.
     *
     * @param listener the listener that shall be removed.
     */
    public void removeListener(ImportHandlerListener listener);

    /**
     * Imports the object specified using the provided session.
     *
     * @param sesh             the session to use for importing data.
     * @param importData       the data to be imported.
     * @param persistentLookup a lookup of classes that have been parsed and saved as part of the import sequence.
     *                         The key of the map is the class that was parsed, and the value of the map is a mapping
     *                         between the original ID of the parsed object and the newly saved instance.
     * @param jsonPersistent
     */
    public Object importData(Session sesh, JSONObject importData,
                             Map<Class, Map<Integer, PersistentImpl>> persistentLookup,
                             JSONObject jsonPersistent)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException;

    /**
     * Registers this handler with the {@link ImportHandlerRegistry}.
     *
     * @param registry the registry where this handler will be added.
     */
    public void register(ImportHandlerRegistry registry);

    /**
     * Returns the key for this handler that will be used in the registry.
     *
     * @return the registry key.
     */
    public String getRegistryKey();
}
