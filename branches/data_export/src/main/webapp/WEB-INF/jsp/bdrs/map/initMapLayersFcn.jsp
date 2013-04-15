<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:useAttribute name="webMap" classname="au.com.gaiaresources.bdrs.controller.map.WebMap" ignore="true" />

<%--
Creates a javascript function that assigned layers to an OpenLayers map.
Returns an array of the created layers.

Example of use:

var initMapLayers = <tiles:insertDefinition name="initMapLayersFcn">
	<tiles:putAttribute name="webMap" value="${webMap}" />
	</tiles:insertDefinition>;
	
initMapLayers(mapObject);
--%>

<c:choose>
	<c:when test="${not empty webMap}">
		function(map) {
			var layerArray = new Array();
		    <c:forEach items="${webMap.map.assignedLayers}" var="assignedLayer">
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
		                size: ${assignedLayer.layer.symbolSize}
		            };
		            // intentionally don't add this one as mapserver layers use transparent tiles not kml features
		            bdrs.map.addMapServerLayer(map, "${assignedLayer.layer.name}", bdrs.map.getBdrsMapServerUrl(), layerOptions);
		        </c:when>
		        <c:when test="${assignedLayer.layer.layerSource == \"SURVEY_KML\"}">
		            var layerOptions = {
		                visible: ${assignedLayer.visible},
		                // cluster strategy doesn't work properly for polygons
		                includeClusterStrategy: true
		            };
		            layer = bdrs.map.addKmlLayer(map, "${assignedLayer.layer.name}", "${portalContextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
		        </c:when>
		        <c:when test="${assignedLayer.layer.layerSource == \"KML\"}">
		            var layerOptions = {
		                visible: ${assignedLayer.visible},
		                // cluster strategy doesn't work properly for polygons
		                includeClusterStrategy: false
		            };
		            layer = bdrs.map.addKmlLayer(map, "${assignedLayer.layer.name}", "${portalContextPath}/bdrs/map/getLayer.htm?layerPk=${assignedLayer.layer.id}", layerOptions);
		        </c:when>
				<c:when test="${assignedLayer.layer.layerSource == \"WMS_SERVER\"}">
					var layerOptions = {
		                visible: ${assignedLayer.visible}
		            };
					layer = bdrs.map.addWmsLayer(map, "${assignedLayer.layer.name}", "${assignedLayer.layer.serverUrl}", layerOptions);
				</c:when>
		        </c:choose>
		        if (layer) {
		            layerArray.push(layer);
		        }
		    }
		    </c:forEach>
			return layerArray;
		}
	</c:when>
	<c:otherwise>
		function() { return []; }
	</c:otherwise>
</c:choose>

