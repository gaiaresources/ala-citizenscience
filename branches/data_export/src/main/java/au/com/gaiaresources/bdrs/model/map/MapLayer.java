package au.com.gaiaresources.bdrs.model.map;

import au.com.gaiaresources.bdrs.model.survey.Survey;

/**
 * Interface that describes a layer that can be shown on a map for a {@link Survey}.
 * 
 * @author stephanie
 */
public interface MapLayer extends Comparable<MapLayer> {
    /**
     * @return The {@link GeoMap} on which the layer will show.
     */
    public GeoMap getMap();
    /**
     * Set the {@link GeoMap} on which the layer will show.
     * @param survey the {@link GeoMap} on which the layer will show
     */
    public void setMap(GeoMap geoMap);
    /**
     * The source for the map layer. This will describe to the renderer how to create the layer.
     * @return the {@link MapLayerSource} describing the origin of the map layer
     */
    public MapLayerSource getLayerSource();
    /**
     * Sets the source for the map layer. This will describe to the renderer how to create the layer.
     * @param source the {@link MapLayerSource} describing the origin of the map layer
     */
    public void setLayerSource(MapLayerSource source);
    /**
     * @return boolean indicating if this layer should show on the map.
     */
    public boolean getShowOnMap();
    /**
     * Set the boolean indicating if this layer should show on the map.
     * @param showOnMap
     */
    public void setShowOnMap(boolean showOnMap);
}
