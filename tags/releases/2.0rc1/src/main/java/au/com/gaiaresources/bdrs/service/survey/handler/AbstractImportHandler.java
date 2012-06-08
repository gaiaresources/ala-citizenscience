package au.com.gaiaresources.bdrs.service.survey.handler;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.service.survey.ImportHandler;
import au.com.gaiaresources.bdrs.service.survey.ImportHandlerRegistry;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.Session;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.*;

/**
 * Provides basic import capability for objects that do not require special handling.
 */
public abstract class AbstractImportHandler implements ImportHandler {

    /**
     * The logging instance for this class.
     */
    private Logger log = Logger.getLogger(getClass());

    /**
     * A registry of importers for various data types.
     */
    private ImportHandlerRegistry registry;

    /**
     * Provides facilities to convert WKT strings to Geometry instances.
     */
    private LocationService locationService;

    /**
     * A registry for listeners to be fired at various stages of the import process.
     */
    private ArrayList<ImportHandlerListener> listenerList = new ArrayList<ImportHandlerListener>();

    /**
     * Messages to send back from import.
     */
    protected Map<String, Object[]> messages = new LinkedHashMap<String, Object[]>();
    
    /**
     * Creates a new instance of the object imported by the implementation of this handler.
     *
     * @return a newly created instance of the object imported by this handler.
     */
    protected abstract Object createNewInstance();

    /**
     * Creates a new instance.
     *
     * @param locationService provides facilities to convert WKT strings to Geometry instances.
     */
    public AbstractImportHandler(LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public void addListener(ImportHandlerListener listener) {
        if (listener != null) {
            listenerList.add(listener);
        }
    }

    @Override
    public void removeListener(ImportHandlerListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Fires a post save event on all registered listeners.
     *
     * @param sesh             the session that has been used to save the bean.
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved beans.
     * @param jsonPersistent   the encoded representation of the bean that has been saved.
     * @param bean             the bean that was saved.
     */
    protected void firePostSaveEvent(Session sesh, JSONObject importData,
                                     Map<Class, Map<Integer, PersistentImpl>> persistentLookup,
                                     JSONObject jsonPersistent, Object bean) {
        for (ImportHandlerListener listener : listenerList) {
            listener.postSave(sesh, importData, persistentLookup, jsonPersistent, bean);
        }
    }

    /**
     * Fires a pre save event on all registered listeners.
     *
     * @param sesh             the session that shall be used to save the bean.
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved beans.
     * @param jsonPersistent   the encoded representation of the bean that will be saved.
     * @param bean             the bean that will be saved.
     */
    protected void firePreSaveEvent(Session sesh, JSONObject importData,
                                    Map<Class, Map<Integer, PersistentImpl>> persistentLookup,
                                    JSONObject jsonPersistent, Object bean) {
        for (ImportHandlerListener listener : listenerList) {
            listener.preSave(sesh, importData, persistentLookup, jsonPersistent, bean);
        }
    }

    @Override
    public void register(ImportHandlerRegistry registry) {
        this.registry = registry;
        registry.put(getRegistryKey(), this);
    }

    /**
     * Return the registry of import handlers that will be used by this class when importing object dependencies.
     *
     * @return the registry of import handlers that will be used by this class when importing object dependencies.
     */
    protected ImportHandlerRegistry getRegistry() {
        return this.registry;
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes the primary key of the object.
     *
     * @param pd the description of the property in question.
     * @return true if the property is a primary key, false otherwise.
     */
    protected boolean isPrimaryKey(PropertyDescriptor pd) {
        return hasAnnotation(pd, Id.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a simple column (e.g String, int etc).
     *
     * @param pd the description of the property in question.
     * @return true if the property is a simple data type, false otherwise.
     */
    protected boolean isColumn(PropertyDescriptor pd) {
        // the check for Transient annotation is for boolean property descriptors
        // pd.getReadMethod() only returns get methods, not is methods
        return hasAnnotation(pd, Column.class) || hasAnnotation(pd, Transient.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a many to many relationship.
     *
     * @param pd the description of the property in question.
     * @return true if the property is a many to many relation, false otherwise.
     */
    protected boolean isManyToMany(PropertyDescriptor pd) {
        return hasAnnotation(pd, ManyToMany.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a one to many relationship.
     *
     * @param pd the description of the property in question.
     * @return true if the property is a one to many relationship.
     */
    protected boolean isOneToMany(PropertyDescriptor pd) {
        return hasAnnotation(pd, OneToMany.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a join column (foreign key).
     *
     * @param pd the description of the property in question.
     * @return true if the property is a foreign key relationship.
     */
    protected boolean isJoinColumn(PropertyDescriptor pd) {
        return hasAnnotation(pd, JoinColumn.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a collection of elements.
     *
     * @param pd the description of the property in question.
     * @return true if the property describes a collection of elements.
     */
    protected boolean isCollectionOfElements(PropertyDescriptor pd) {
        return hasAnnotation(pd, CollectionOfElements.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> describes a many to one relationship.
     *
     * @param pd the description of the property in question.
     * @return true if the property describes a many to one relationship.
     */
    protected boolean isManyToOne(PropertyDescriptor pd) {
        return hasAnnotation(pd, ManyToOne.class);
    }

    /**
     * True if the <code>PropertyDescriptor</code> has the specified annotation applied.
     *
     * @param pd the description of the property in question.
     * @return true if the property has the specified annotation, false otherwise.
     */
    protected boolean hasAnnotation(PropertyDescriptor pd, Class<? extends Annotation> annotationClass) {
        return pd.getReadMethod().getAnnotation(annotationClass) != null;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportHandler#importData(org.hibernate.Session, au.com.gaiaresources.bdrs.json.JSONObject, java.util.Map, au.com.gaiaresources.bdrs.json.JSONObject)
     */
    @Override
    public Object importData(Session sesh, JSONObject importData, Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {

        // Remove the representation from the registry of instances to be imported
        removeJSONPersistent(importData, jsonPersistent);
        Object bean = createBean(sesh, importData, persistentLookup, jsonPersistent);

        // Notify all listeners that we are about to save the instance.
        firePreSaveEvent(sesh, importData, persistentLookup, jsonPersistent, bean);

        // Save the instance and add it to the registry of saved data.
        sesh.save(bean);
        addToPersistentLookup(persistentLookup, jsonPersistent, (PersistentImpl) bean);

        // Notify all listeners that the instance has been saved.
        firePostSaveEvent(sesh, importData, persistentLookup, jsonPersistent, bean);

        return bean;
    }

    /**
     * Creates the object specified using the provided session.
     *
     * @param sesh             the session to use for importing data.
     * @param importData       the data to be imported.
     * @param persistentLookup a lookup of classes that have been parsed and saved as part of the import sequence.
     *                         The key of the map is the class that was parsed, and the value of the map is a mapping
     *                         between the original ID of the parsed object and the newly saved instance.
     * @param jsonPersistent
     * @return
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected Object createBean(Session sesh, JSONObject importData, 
            Map<Class, Map<Integer, PersistentImpl>> persistentLookup, 
            JSONObject jsonPersistent) 
                    throws InvocationTargetException, NoSuchMethodException, 
                           InstantiationException, IllegalAccessException {
        Object bean = createNewInstance();

        // Import each of the keys in the representation
        for (Object keyObj : jsonPersistent.keySet()) {
            String key = keyObj.toString();
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(bean.getClass(), key);

            // Check if there is a property descriptor. This avoids any transient methods.
            if (pd != null) {
                Method writeMethod = pd.getWriteMethod();
                // Don't import the primary key or bad things will happen
                if (writeMethod != null && !isPrimaryKey(pd)) {
                    if (isColumn(pd)) {
                        // Import the value and invoke the mutator
                        // do this check last because @JoinColumn takes precedence over @Column
                        Object value = getByDataType(sesh, importData, persistentLookup, jsonPersistent, pd);
                        writeMethod.invoke(bean, value);
                    } else if (isManyToMany(pd) || isOneToMany(pd) || (isJoinColumn(pd) && isCollectionOfElements(pd))) {
                        // Import the collection of elements if the collection is not empty
                        JSONArray relatedArray = jsonPersistent.getJSONArray(key);
                        if (!relatedArray.isEmpty()) {
                            Collection collection = createEmptyCollection(pd, relatedArray.size());
                            for (int i = 0; i < relatedArray.size(); i++) {
                                if (relatedArray.isNull(i)) {
                                    collection.add(null);
                                } else {
                                    int jsonPersistentPK = relatedArray.getInt(i);

                                    // This is an unchecked cast however the code only handles simple parameterized collections
                                    ParameterizedType parameterizedType = (ParameterizedType) pd.getReadMethod().getGenericReturnType();
                                    Class type = (Class) parameterizedType.getActualTypeArguments()[0];
                                    Object obj = importRelated(sesh, importData, persistentLookup, type, jsonPersistentPK);
                                    if (obj != null) {
                                        collection.add(obj);
                                    }
                                }
                            }
                            // Invoke the mutator
                            pd.getWriteMethod().invoke(bean, collection);
                        }
                    } else if (isManyToOne(pd) && !jsonPersistent.isNull(key)) {
                        // Import the related instance
                        int relatedPK = jsonPersistent.getInt(key);
                        Class<?> type = pd.getReadMethod().getReturnType();
                        Object obj = importRelated(sesh, importData, persistentLookup, type, relatedPK);
                        pd.getWriteMethod().invoke(bean, obj);
                    }
                }
            }
        }
        return bean;
    }

    /**
     * Adds the specified instance to the registry of objects that have been imported.
     *
     * @param persistentLookup the registry of objects that have been imported.
     * @param jsonPersistent   the original representation of the imported instance.
     * @param bean             the newly created object.
     */
    protected void addToPersistentLookup(Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent, PersistentImpl bean) {
        int id = getJSONPersistentId(jsonPersistent);
        Map<Integer, PersistentImpl> typeLookup = persistentLookup.get(bean.getClass());
        if (typeLookup == null) {
            typeLookup = new HashMap<Integer, PersistentImpl>();
            persistentLookup.put(bean.getClass(), typeLookup);
        }
        typeLookup.put(Integer.valueOf(id), bean);
    }

    /**
     * Creates a new empty collection represented by the specified property.
     *
     * @param pd   the description of the collection property.
     * @param size the initial size of the collection to be created.
     * @return a new empty collection described by the specified property.
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    protected Collection<?> createEmptyCollection(PropertyDescriptor pd, int size)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?> type = pd.getReadMethod().getReturnType();
        if (!Collection.class.isAssignableFrom(type)) {
            log.warn("Unhandled datatype encountered: " + type.getCanonicalName());
            return null;
        }

        Class<?> collectionImplKlazz = null;
        if (Set.class.isAssignableFrom(type)) {
            collectionImplKlazz = HashSet.class;
        } else if (List.class.isAssignableFrom(type)) {
            collectionImplKlazz = ArrayList.class;
        }

        if (collectionImplKlazz != null) {
            return (Collection<?>) ConstructorUtils.invokeConstructor(collectionImplKlazz, new Object[]{size});
        } else {
            log.warn("Unhandled collection type encountered: " + type.getCanonicalName());
            return null;
        }
    }

    /**
     * Initialises a value from the representation using the datatype described by the property.
     *
     * @param sesh             the session to be used to import any dependant objects
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved beans.
     * @param jsonPersistent   the encoded representation of the object being imported.
     * @param pd               the property that we are currently trying to import.
     * @return a value from the representation using the datatype described by the property.
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    private Object getByDataType(Session sesh, JSONObject importData,
                                 Map<Class, Map<Integer, PersistentImpl>> persistentLookup,
                                 JSONObject jsonPersistent, PropertyDescriptor pd)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String key = pd.getName();
        if (jsonPersistent.isNull(key)) {
            return null;
        }

        Class<?> type = pd.getReadMethod().getReturnType();
        if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return jsonPersistent.getBoolean(key);
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            return jsonPersistent.getInt(key);
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            return jsonPersistent.getLong(key);
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            return jsonPersistent.getDouble(key);
        } else if (String.class.equals(type)) {
            return jsonPersistent.getString(key);
        } else if (Date.class.equals(type)) {
            return new Date(jsonPersistent.getLong(key));
        } else if (Enum.class.isAssignableFrom(type)) {
            String enumStr = jsonPersistent.getString(key);
            if (!jsonPersistent.isNull(key) && enumStr != null && !enumStr.isEmpty()) {
                return Enum.valueOf((Class<? extends Enum>) type, enumStr);
            } else {
                return null;
            }
        } else if (PersistentImpl.class.isAssignableFrom(type)) {
            int otherId = getJSONPersistentId(jsonPersistent);
            return importRelated(sesh, importData, persistentLookup, type, otherId);
        } else if (BigDecimal.class.isAssignableFrom(type)) {
            return new BigDecimal(jsonPersistent.getString(key));
        } else if (Geometry.class.isAssignableFrom(type)) {
            return locationService.createGeometryFromWKT(jsonPersistent.getString(key));
        } else if (type.isArray()) {
            JSONArray array = jsonPersistent.getJSONArray(key);
            if (array.size() > 0) {
                // set the type of the array or a ClassCastException will occur
                type = array.get(0).getClass();
                Object[] newArray = (Object[]) Array.newInstance(type, array.size());
                return array.toArray(newArray);
            }
        }

        log.warn("Unhandled data type encountered: " + type.getCanonicalName());
        return null;
    }

    /**
     * Returns an instance of the dependant object either by retrieving it from the registry of instances
     * that have already been saved, or by importing its representation.
     *
     * @param sesh             the session that has been used to save the dependant instance.
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved instance.
     * @param type             the datatype of the instance to be returned
     * @param otherId          the primary key (used in the original representation) of the instance to be returned.
     * @return an instance of the dependant object.
     * @throws InvocationTargetException thrown if there has been an error introspecting the object to be created.
     * @throws NoSuchMethodException     thrown if there has been an error introspecting the object to be created.
     * @throws IllegalAccessException    thrown if there has been an error introspecting the object to be created.
     * @throws InstantiationException    thrown if there has been an error introspecting the object to be created.
     */
    private Object importRelated(Session sesh, JSONObject importData, Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, Class<?> type, int otherId)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        PersistentImpl p = getPersistedInstance(persistentLookup, type, otherId);
        if (p == null) {
            JSONObject jp = getJSONPersistent(importData, type, otherId);
            if (jp == null) {
                log.warn(String.format("Cannot find persistent or json representations of %s:%d", type, otherId));
                return null;
            } else {
                return getRegistry().get(type.getSimpleName()).importData(sesh, importData, persistentLookup, jp);
            }
        } else {
            return p;
        }
    }

    /**
     * Returns a previously imported (and saved) instance with the specified type and id, or null if one cannot be found.
     *
     * @param persistentLookup the registry of saved instances.
     * @param type             the datatype of the instance to be retrieved.
     * @param otherId          the primary key (used in the original representation) of the instance to be returned.
     * @return a previously imported instance with the specified type and id, or null if one cannot be found.
     */
    private PersistentImpl getPersistedInstance(Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, Class<?> type, int otherId) {
        Map<Integer, PersistentImpl> typeLookup = persistentLookup.get(type);
        if (typeLookup != null) {
            return typeLookup.get(otherId);
        }

        return null;
    }

    /**
     * Retrieves the representation of the as yet unimported object or null if one does not exist.
     *
     * @param importData the registry of encoded data that has not yet been saved.
     * @param type       the datatype of the representation to be retrieved.
     * @param otherId    the primary key (used in the original representation) of the instance to be returned.
     * @return the representation of the as yet unimported object or null if one does not exist.
     */
    private JSONObject getJSONPersistent(JSONObject importData, Class<?> type, int otherId) {
        String klazzName = type.getSimpleName();
        JSONObject idToJsonPersistentLookup = importData.optJSONObject(klazzName);

        if (idToJsonPersistentLookup != null) {
            return idToJsonPersistentLookup.getJSONObject(String.valueOf(otherId));
        }

        return null;
    }

    /**
     * Removes the specified representation from the registry of encoded data that has not yet been saved.
     *
     * @param importData     the registry of encoded data that has not yet been saved.
     * @param jsonPersistent the encoded representation to be removed.
     * @return true if the representation was removed successfully, false otherwise.
     */
    protected boolean removeJSONPersistent(JSONObject importData, JSONObject jsonPersistent) {
        String klazzName = jsonPersistent.getString(PersistentImpl.FLATTEN_KEY_CLASS);
        JSONObject idToJsonPersistentLookup = importData.getJSONObject(klazzName);
        if (idToJsonPersistentLookup == null) {
            log.warn("Unable to remove JSON persistent because there is no lookup for the class type: " + klazzName);
            return false;
        } else {
            Object j = idToJsonPersistentLookup.remove(String.valueOf(getJSONPersistentId(jsonPersistent)));
            if (idToJsonPersistentLookup.isEmpty()) {
                importData.remove(klazzName);
            }
            return j != null;
        }
    }

    /**
     * Returns the primary key of the representation provided.
     *
     * @param jsonPersistent the representation to be interrogated for a primary key.
     * @return the primary key of the representation.
     */
    private int getJSONPersistentId(JSONObject jsonPersistent) {
        return jsonPersistent.getInt("id");
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportHandler#getMessages()
     */
    @Override
    public Map<String, Object[]> getMessages() {
        return this.messages;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportHandler#clearMessages()
     */
    @Override
    public void clearMessages() {
        this.messages.clear();
    }
}
