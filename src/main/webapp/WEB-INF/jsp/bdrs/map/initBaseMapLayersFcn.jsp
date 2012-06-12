<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:useAttribute name="webMap" classname="au.com.gaiaresources.bdrs.controller.map.WebMap" ignore="true" />

<c:if test="${ not empty webMap }">

<%-- 
Statically defines a function that is used in bdrs.js during map creation.
If the function does not exist a default map creation runs
--%>

bdrs.map.customMapLayers = function() {
    var layers =  [];
    var baseLayers = ${webMap.mapBaseLayers};
    for (var i = 0; i < baseLayers.length; i++) {
        var layer = baseLayers[i];
        var thisLayer;
        
        if(layer.layerSource === 'G_PHYSICAL_MAP' && window.G_PHYSICAL_MAP !== undefined && window.G_PHYSICAL_MAP !== null) {
            thisLayer = new OpenLayers.Layer.Google('Google Physical', {
                type: G_PHYSICAL_MAP,
                sphericalMercator: true,
                MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
            });
        } else if(layer.layerSource === 'G_NORMAL_MAP' && window.G_NORMAL_MAP !== undefined && window.G_NORMAL_MAP !== null) {
            thisLayer = new OpenLayers.Layer.Google('Google Streets', // the default
            {
                type: G_NORMAL_MAP,
                numZoomLevels: 20,
                sphericalMercator: true,
                MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
            });
        } else if(layer.layerSource === 'G_HYBRID_MAP' && window.G_HYBRID_MAP !== undefined && window.G_HYBRID_MAP !== null) {
            thisLayer = new OpenLayers.Layer.Google('Google Hybrid', {
                type: G_HYBRID_MAP,
                numZoomLevels: 20,
                sphericalMercator: true,
                MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
            });
        } else if(layer.layerSource === 'G_SATELLITE_MAP' && window.G_SATELLITE_MAP !== undefined && window.G_SATELLITE_MAP !== null) {
            thisLayer = new OpenLayers.Layer.Google('Google Satellite', {
                type: G_SATELLITE_MAP,
                numZoomLevels: 22,
                sphericalMercator: true,
                MIN_ZOOM_LEVEL: bdrs.map.MIN_GOOGLE_ZOOM_LEVEL
            });
        } else if (layer.layerSource === 'OSM') {
            thisLayer = new OpenLayers.Layer.OSM();
        } else if (layer.layerSource === 'OPEN_LAYERS_WMS') {
            thisLayer = new OpenLayers.Layer.WMS( "OpenLayers WMS", 
                    "http://vmap0.tiles.osgeo.org/wms/vmap0?", {layers: 'basic'});
        }

        if (thisLayer) {
            layers.push(thisLayer);
        }
        
        if (layer['default']) {
            bdrs.map.baseLayer = thisLayer;
        }
    }
    
    if(layers.length === 0) {
        var nobase = new OpenLayers.Layer("No Basemap",{isBaseLayer: true, 'displayInLayerSwitcher': true});
        layers.push(nobase);
    }

    return layers;
};
</c:if>