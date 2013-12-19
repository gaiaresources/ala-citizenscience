<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:useAttribute name="webMap" classname="au.com.gaiaresources.bdrs.controller.map.WebMap" ignore="true" />

<c:if test="${ not empty webMap }">

<%--
Statically defines a function that is used in bdrs.js during map creation.
If the function does not exist a default map creation runs
--%>

bdrs.map.customMapLayers = function() {
    var result = [];
    var baseLayers = ${webMap.mapBaseLayers};
    for (var i = 0; i < baseLayers.length; i++) {
        var baseLayer = baseLayers[i];
        var mapLayer = bdrs.map.createMapLayer(baseLayer.layerSource);
        if (mapLayer !== null) {
            result.push(mapLayer);
            if (baseLayer['default']) {
                bdrs.map.baseLayer = mapLayer;
            }
        }
    }
    if (result.length === 0) {
        result.push(bdrs.map.createMapLayer("NO_MAP"));
    }
    return result;
};
</c:if>
