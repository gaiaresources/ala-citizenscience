package au.com.gaiaresources.bdrs.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates LocationUtil objects.
 */
public class SpatialUtilFactory {
	
	private Map<Integer, SpatialUtil> cache = new HashMap<Integer, SpatialUtil>();
	
	/**
	 * Get a LocationUtil object for the given srid.
	 * @param srid SRID of the LocationUtil context.
	 * @return LocationUtil
	 */
	public SpatialUtil getLocationUtil(int srid) {
		if (cache.get(srid) == null) {
			cache.put(srid, new SpatialUtil(srid));
		}
		return cache.get(srid);
	}
	
	/**
	 * Get a LocationUtil object for the default SRID (4326).
	 * @return LocationUtil
	 */
	public SpatialUtil getLocationUtil() {
		return getLocationUtil(4326);
	}
}
