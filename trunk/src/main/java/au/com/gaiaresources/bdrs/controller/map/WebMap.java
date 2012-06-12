package au.com.gaiaresources.bdrs.controller.map;

import java.util.Collections;
import java.util.List;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMap;

/**
 * Wrapper for the GeoMap object when creating a web map.
 */
public class WebMap {

	private JSONArray mapBaseLayers = new JSONArray();
    private JSONArray geoMapLayers = new JSONArray();
    
    private GeoMap map;
    
	public WebMap(GeoMap map) {
		
		this.map = map;
		
		List<BaseMapLayer> sortedLayers = map.getBaseMapLayers();
        Collections.sort(sortedLayers);
        for (BaseMapLayer layer : sortedLayers) {
            mapBaseLayers.add(layer.flatten());
        }
        
        List<AssignedGeoMapLayer> sortedGeoLayers = map.getAssignedLayers();
        Collections.sort(sortedGeoLayers);
        for (AssignedGeoMapLayer layer : sortedGeoLayers) {
            geoMapLayers.add(layer.flatten(2));
        }
	}
	
    /**
     * Gets the base layers that should be shown on the map.
     * @return the mapBaseLayers
     */
    public String getMapBaseLayers() {
        return mapBaseLayers.toString();
    }
    
    /**
     * Gets the geo map layers that should be shown on the map.
     * @return the geoMapLayers
     */
    public String getGeoMapLayers() {
        return geoMapLayers.toString();
    }
    
    /**
     * Get the zoom level for the map.
     * @return zoom level.
     */
    public Integer getZoom() {
    	return map.getZoom();
    }
    
    /**
     * Get the center point for the map as a wkt string.
     * @return center point for map as a wkt string.
     */
    public String getCenter() {
    	return map.getCenter() != null ? map.getCenter().toText() : "";
    }
    
    /**
     * Get the underlying GeoMap object.
     * @return the underlying GeoMap object.
     */
    public GeoMap getMap() {
    	return map;
    }
}
