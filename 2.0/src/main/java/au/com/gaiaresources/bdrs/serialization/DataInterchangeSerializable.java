package au.com.gaiaresources.bdrs.serialization;

import java.util.Map;
import java.util.Set;

public interface DataInterchangeSerializable {

    public Map<String, Object> flatten();
    public Map<String, Object> flatten(int depth);
    
    /**
     * Flattens out an object to a map.
     * @param compact whether to include fields annotated with @CompactAttribute
     * @param mobileFields whether to rename fields according to @MobileField
     * @return a flattened map of the attributes to their values.
     */
    public Map<String, Object> flatten(boolean compact, boolean mobileFields);
    
    /**
     * Flattens out an object to a map.
     * @param compact whether to include fields annotated with @CompactAttribute
     * @param mobileFields whether to rename fields according to @MobileField
     * @param depth the depth to flatten included collections to.
     * @return a flattened map of the attributes to their values.
     */
    public Map<String, Object> flatten(int depth, boolean compact, boolean mobileFields);
    
    /**
     * Flattens out an object to a map.
     * @param compact whether to include fields annotated with @CompactAttribute
     * @param mobileFields whether to rename fields according to @MobileField
     * @param depth the depth to flatten included collections to.
     * @param sensitiveFields whether to include fields marked as @Sensitive
     * @return a flattened map of the attributes to their values.
     */
    public Map<String, Object> flatten(int depth, boolean compact,
            boolean mobileFields, boolean sensitiveFields);
    
    /**
     * Flattens out an object to a map.
     * @param compact whether to include fields annotated with @CompactAttribute
     * @param mobileFields whether to rename fields according to @MobileField
     * @param sensitiveFields whether to include fields marked as @Sensitive
     * @return a flattened map of the attributes to their values.
     */
    public Map<String, Object> flatten(boolean compact, boolean mobileFields,
            boolean sensitiveFields);

    /**
     * Flattens out an object to a map.
     * @param depth the depth to flatten included collections and associations to.
     * @param compact whether to include fields annotated with @CompactAttribute
     * @param mobileFields whether to rename fields according to @MobileField
     * @param sensitiveFields whether to include fields marked as @Sensitive
     * @param propertiesMap if supplied, for each class in the map, only properties with names matching the values in
     *                      the associated Set will be included in the returned Map.  If null, all values will be included.
     * @return a flattened map of the attributes to their values.
     */
    public Map<String, Object> flatten(int depth, boolean compact, boolean mobileFields,
                                       boolean sensitiveFields, Map<Class<?>, Set<String>> propertiesMap);


}
