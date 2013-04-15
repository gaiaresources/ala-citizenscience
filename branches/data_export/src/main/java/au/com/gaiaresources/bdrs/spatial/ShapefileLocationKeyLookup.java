package au.com.gaiaresources.bdrs.spatial;

/**
 * Provides a lookup for location shapefile key names.
 * 
 * @author stephanie
 *
 */
public class ShapefileLocationKeyLookup extends ShapefileRecordKeyLookup {

    /**
     * Returns ShapefileFields.PARAM_LOCATION ("loc_id") instead of record id.
     * This is the lookup for location types, the method should really be "getIdKey()"
     */
    @Override
    public String getRecordIdKey() {
        return ShapefileFields.PARAM_LOCATION;
    }
}
