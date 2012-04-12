<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<h1>Site Review</h1>

<tiles:insertDefinition name="advancedReview">
    <tiles:putAttribute name="viewType" value="location"/>
    <tiles:putAttribute name="resultCount" value="${ recordCount }"/>
</tiles:insertDefinition>

<c:if test="${ not downloadViewSelected }">
	<div class="buttonpanel textright">
	    <input id="viewRecords" name="submit" type="submit" value="View Records" class="form_action" onclick="showRecordReviewWithFormOptions();"/>
	</div>
</c:if>
<script type="text/javascript">
	var tableColumns = [{'sortName' : 'location.name',
					     'tdClass' : '',
					     'divClass' :  'left alaSightingsTableHeader',
					     'tooltip' : 'The location name',
					     'title' : 'Name'},
					    {'sortName' : 'location.description',
					     'tdClass' : '',
					     'divClass' :  'left alaSightingsTableHeader',
					     'tooltip' : 'The location description',
					     'title' : 'Description'},
					    {'sortName' : 'location.location',
					     'displayClass' : 'locationColumn ',
					     'divClass' :  'left alaSightingsTableHeader',
					     'tooltip' : 'The latitude and longitude of the location.',
					     'title' : 'Lat/Lon'},
					    {'sortName' : 'location.user',
					     'displayClass' : 'userColumn',
					     'divClass' :  'left alaSightingsTableHeader',
					     'tooltip' : 'The user that owns the location.',
					     'title' : 'User'}];

    var locations = new Array();
	   
    jQuery(function() {
      <c:if test="${ downloadViewSelected }">
            bdrs.review.downloadSightingsWidget.init("#facetForm", "/review/sightings/advancedReviewDownloadLocations.htm");
       </c:if>
       console.log(${locations});
       <c:forEach var="loc" items="${locations}">
           locations.push(${loc});
       </c:forEach>
       
       // add a change listener for the checkboxes to keep track of selected locations
       jQuery("#alaSightingsTable").delegate(".recordIdCheckbox", 'change', function() {
    	   var checkBox = jQuery(this);
           var checked = checkBox.prop("checked");
           if (checked) {
        	   // add to the locations array
        	   locations.push(checkBox.val());
           } else {
        	   var index = locations.indexOf(checkBox.val());
        	   // remove from locations array
        	   if (index != -1) {
        		   locations.splice(index, 1);
        	   }
           }
           jQuery("#locations").val(locations);
       });
       
       // add a change handler to manage locations with the select all checkbox
       jQuery("#bulkSelectCheckbox").change(function(evt) {
           var select_all_elem = jQuery(evt.currentTarget);
           var checked = select_all_elem.prop("checked");
           if (checked) {
        	   // add all to the locations array
        	   var checkboxes = select_all_elem.parents("table").find('.recordIdCheckbox:not(:disabled)');
			   checkboxes.each(function() {
        		   locations.push(jQuery(this).val());
        	   });
           } else {
        	   locations.length = 0;
           }
           jQuery("#locations").val(locations);
       });
   });
    
    var stylemap = {
            'strokeWidth': 2,
            'strokeOpacity': 1,
            'strokeColor': '#669900',
            'fillColor': '#669900',
            'fillOpacity': 0.2,
            'pointRadius': 4
        };
    var wktWriter = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
    var inversewkt = new OpenLayers.Format.WKT({
            'internalProjection': bdrs.map.WGS84_PROJECTION,
            'externalProjection': bdrs.map.GOOGLE_PROJECTION
        });
    var initMap = function() {
    	var layer = bdrs.map.addVectorLayer(bdrs.map.baseMap, "Location Selector", stylemap);
    	// Will remove all but the most recently added feature
        var featureAddedHandler = function(feature) {
            // protect from any shenanigans
            if (!feature) {
                return;
            }
            var layer = feature.layer;
            if (!layer) {
                return;
            }
            var featuresToRemove = new Array();
            for (var i = 0; i < layer.features.length; ++i) {
                if (feature != layer.features[i]) {
                    featuresToRemove.push(layer.features[i]);
                }
            }
            layer.removeFeatures(featuresToRemove);
            
            // now get all of the locations contained within that feature's geometry
            // add the geometry as a parameter to the form
            var transGeom = OpenLayers.Geometry.fromWKT(wktWriter.write(feature)).transform(bdrs.map.GOOGLE_PROJECTION, bdrs.map.WGS84_PROJECTION);
            jQuery("#locationArea").val(transGeom);
            
            // clear the locations array
            locations.length = 0;
            
            // refresh the map layer to only show locations within the added feature
            jQuery.get(bdrs.contextPath+bdrs.advancedReview.JSON_URL+jQuery("form").serialize(), {}, function(data) {
                var featureArray = [];

                // use the selection geometry to determine the map zoom
                var geobounds;
                for (var i = 0; i < data.length; i++) {
	                var feature = wktWriter.read(OpenLayers.Geometry.fromWKT(data[i].location).transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION));
	                
	                featureArray.push(feature);
					if (geobounds) {
						geobounds.extend(feature.geometry.getBounds());
					} else {
		                geobounds = feature.geometry.getBounds();
					}
					locations.push(data[i].id);
                }
                var layer = bdrs.map.baseMap.getLayersByName("Sightings")[0];
                // remove the existing features from the layer and add the new ones
                layer.removeAllFeatures();
                layer.addFeatures(featureArray);

                // zoom the map to show the selected area
                var selectLayer = bdrs.map.baseMap.getLayersByName("Location Selector")[0];
                bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, selectLayer);
                // update the locations form input
                jQuery("#locations").val(locations);
            });
        };
        layer.events.register('featureadded', null, function(event){
            featureAddedHandler(event.feature);
        });
    	var drawControl = new OpenLayers.Control.DrawFeature(layer, OpenLayers.Handler.Polygon);
        bdrs.map.baseMap.addLayers([layer]);
        bdrs.map.baseMap.addControl(drawControl);
        drawControl.activate();
    };
    
    jQuery(window).load(function() {
        // add a layer for drawing polygons to select location areas to the map
        initMap();
    });
    
   var showRecordReviewWithFormOptions = function() {
	   var url = bdrs.contextPath + "/review/sightings/advancedReview.htm?locations="+locations;
	   window.location = url;
   };
</script>
