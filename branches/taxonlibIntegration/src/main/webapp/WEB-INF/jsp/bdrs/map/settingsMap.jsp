<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<jsp:useBean id="context" scope="request" type="au.com.gaiaresources.bdrs.servlet.RequestContext"></jsp:useBean>

<tiles:useAttribute name="mapCenter" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="mapZoom" ignore="true"/>

<div class="clear"></div>

<div class="map_wrapper" id="map_wrapper">
    <div id="view_base_map" class="defaultmap review_map"></div>
</div>

<div class="clear"></div>

<script type="text/javascript">

    jQuery(window).load(function() {
        var mapOpts = {enlargeMapLink: false, zoomLock: false};
        var geom = OpenLayers.Geometry.fromWKT('${mapCenter}');
        if (geom) {
            var point = geom.getCentroid();
            bdrs.map.defaultCenterLat = point.y;
            bdrs.map.defaultCenterLong = point.x;
        }
        if ('' !== '${mapZoom}') {
            bdrs.map.defaultCenterZoom = '${mapZoom}';
        }
        console.log(mapOpts);
        bdrs.map.initBaseMap('view_base_map', mapOpts);
        
        var layerArray = new Array();
        <c:forEach items="${assignedLayers}" var="assignedLayer">
        {
            var layer;
            <c:choose>
            <c:when test="${assignedLayer.layer.layerSource == \"SHAPEFILE\" || assignedLayer.layer.layerSource == \"SURVEY_MAPSERVER\"}">
                var layerOptions = {
                    bdrsLayerId: ${assignedLayer.layer.id},
                    visible: ${assignedLayer.visible},
                    opacity: bdrs.map.DEFAULT_OPACITY,
                    fillColor: "${assignedLayer.layer.fillColor}",
                    strokeColor: "${assignedLayer.layer.strokeColor}",
                    strokeWidth: ${assignedLayer.layer.strokeWidth},
                    size: ${assignedLayer.layer.symbolSize},
                    upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
                    lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
                };
                // intentionally don't add this one as mapserver layers use transparent tiles not kml features
                bdrs.map.addMapServerLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", bdrs.map.getBdrsMapServerUrl(), layerOptions);
            </c:when>
            <c:when test="${assignedLayer.layer.layerSource == \"SURVEY_KML\"}">
                var layerOptions = {
                    visible: ${assignedLayer.visible},
                    // cluster strategy doesn't work properly for polygons
                    includeClusterStrategy: true,
                    upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
                    lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
                };
                layer = bdrs.map.addKmlLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", "${pageContext.request.contextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
            </c:when>
            <c:when test="${assignedLayer.layer.layerSource == \"KML\"}">
                var layerOptions = {
                    visible: ${assignedLayer.visible},
                    // cluster strategy doesn't work properly for polygons
                    includeClusterStrategy: false,
                    upperZoomLimit: ${assignedLayer.upperZoomLimit != null ? assignedLayer.upperZoomLimit : 'null'},
                    lowerZoomLimit: ${assignedLayer.lowerZoomLimit != null ? assignedLayer.lowerZoomLimit: 'null'}
                };
                layer = bdrs.map.addKmlLayer(bdrs.map.baseMap, "${assignedLayer.layer.name}", "${pageContext.request.contextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
            </c:when>
            </c:choose>
            if (layer) {
                layerArray.push(layer);
            }
        }
        </c:forEach>

        // Add select for KML stuff
        bdrs.map.addSelectHandler(bdrs.map.baseMap, layerArray);

        // center map to given zoom and center
        // default center map
        bdrs.map.centerMap(bdrs.map.baseMap);
        
        // In order to force correct map centering in IE7
        jQuery("#view_base_map").removeClass("defaultmap");
        jQuery("#view_base_map").addClass("defaultmap");
    });

</script>