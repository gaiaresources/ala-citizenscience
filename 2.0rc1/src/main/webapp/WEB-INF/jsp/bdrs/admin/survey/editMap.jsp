<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h2>Edit Map Settings</h2>

    <p>
        Click and drag the map below to configure the default zoom level and center of the 
        maps for this project.
    </p>
<tiles:insertDefinition name="settingsMap">
    <tiles:putAttribute name="survey" value="${survey}"/>
    <tiles:putAttribute name="mapCenter" value="${survey.mapCenter}"/>
    <tiles:putAttribute name="mapZoom" value="${survey.mapZoom}"/>
</tiles:insertDefinition>

<form method="POST" action="${portalContextPath}/bdrs/admin/survey/editMap.htm" onsubmit="setCenterAndZoom();">
    <input type="hidden" name="surveyId" value="${survey.id}"/>
    <div>
        <h2>Base Layers</h2>
        <p>
        Use the base layer selector below the map to determine the default base 
        layer of the map as well as the other layer options that will show in 
        the layer selector.
        </p>
        <table id="base_layer_table" class="datatable attribute_input_table">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Show Layer?</th>
                    <th>Layer Type</th>
                    <th>Default?</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${ baseLayers }" var="baseLayer">
                    <tiles:insertDefinition name="baseLayerRow">
                        <tiles:putAttribute name="baseLayer" value="${ baseLayer }"/>
                    </tiles:insertDefinition>
                </c:forEach>    
            </tbody>
        </table>

     <h2>Overlay Layers</h2>
        
       <p>
        Use the overlay layer selector below to determine which <a>custom layers</a> 
        will show on the map as well as the other layer options that will show in 
        the layer selector.
       </p>
       <table id="bdrs_layer_table" class="datatable attribute_input_table">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Show Layer?</th>
                    <th>Layer Name</th>
                    <th>Layer Description</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${ bdrsLayers }" var="bdrsLayer">
                    <tiles:insertDefinition name="bdrsLayerRow">
                        <tiles:putAttribute name="bdrsLayer" value="${ bdrsLayer }"/>
                    </tiles:insertDefinition>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <input type="hidden" name="zoomLevel" id="zoomLevel"/>
    <input type="hidden" name="mapCenter" id="mapCenter"/>
    
    <div class="textright buttonpanel">
        <input type="submit" class="form_action" value="Save"/>
        <input type="submit" class="form_action" name="saveAndContinue" value="Save And Continue"/>
    </div>
</form>

<script>
    var checkAndDisableSelection = function(selector, eventTarget) {
        jQuery(selector).attr('checked', eventTarget.checked);
        if (eventTarget.checked) {
            jQuery(selector).attr('disabled', eventTarget.checked);
        } else {
            jQuery(selector).removeAttr('disabled');
        }
    };

    var setCenterAndZoom = function() {
        // set the hidden inputs to the current map settings
        var map = bdrs.map.baseMap;
        jQuery('#zoomLevel').val(map.getZoom());
        var wktFormatter = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
        var lonLat = map.getCenter();
        lonLat.transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.WGS84_PROJECTION);
        var lat = bdrs.map.roundLatOrLon(lonLat.lat);
        var lon = bdrs.map.roundLatOrLon(lonLat.lon);
        var point = new OpenLayers.Geometry.Point(lon, lat);
        // openlayers WKT.extract.point returns the inner portion of a WKT string
        // in order to consume it with WKT.read, it must be surrounded by POINT()
        // this is okay to add because we know it is always a point
        // in future openlayers versions, use extractGeometry, which correctly 
        // builds a complete WKT string for you
        jQuery('#mapCenter').val("POINT("+wktFormatter.extract.point(point)+")");
    };

    jQuery(function() {
        bdrs.dnd.attachTableDnD('#base_layer_table');
        bdrs.dnd.attachTableDnD('#bdrs_layer_table');
    });
    
    var changeDefaultLayer = function(layerName) {
    	console.log(layerName);
        // change the base layer of the map to the selected layer
        bdrs.map.baseLayer = bdrs.map.baseMap.getLayersByName(layerName);
        console.log(bdrs.map.baseLayer);
        if(bdrs.map.baseLayer) {
            //bdrs.map.baseMap.setBaseLayer(bdrs.map.baseLayer);
            bdrs.map.baseMap.setLayerIndex(bdrs.map.baseLayer, 0);
        }
    };
    

    bdrs.map.customMapLayers = function() {
        var layers =  [];
        var baseLayers = ${baseLayers};
        for (var i = 0; i < baseLayers.length; i++) {
            var layer = baseLayers[i];
            var thisLayer;
            console.log(layer.layerSource);
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
                thisLayer = new OpenLayers.Layer.OSM("Open Street Map");
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
</script>