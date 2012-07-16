<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<h1>Location Review</h1>

<tiles:insertDefinition name="advancedReview">
    <tiles:putAttribute name="resultsType" value="location"/>
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
                         'tooltip' : 'The coordinates of the location.',
                         'title' : 'Coordinates'},
                        {'sortName' : 'location.user',
                         'displayClass' : 'userColumn',
                         'divClass' :  'left alaSightingsTableHeader',
                         'tooltip' : 'The user that owns the location.',
                         'title' : 'User'}];

    jQuery(function() {
        // initialize the locations variable with the parameter value
        var locString = "[${locations}]";
        bdrs.advancedReview.locations = jQuery.parseJSON(locString);
        
      <c:if test="${ downloadViewSelected }">
      bdrs.advancedReview.downloadSightingsWidgetInit("#facetForm");
       </c:if>
       
        // add a change listener for the checkboxes to keep track of selected locations
       jQuery("#alaSightingsTable").delegate(".recordIdCheckbox", 'change', function() {
           var checkBox = jQuery(this);
           var checked = checkBox.prop("checked");
           // check for the int value (if it has been added on page load it will be an int)
           var index = jQuery.inArray(parseInt(checkBox.val()), bdrs.advancedReview.locations);
           if (checked) {
               // add to the locations array if it is not already in the array
               if (index === -1) {
                   bdrs.advancedReview.locations.push(parseInt(checkBox.val()));
               }
           } else {
               // remove from locations array
               if (index !== -1) {
                   bdrs.advancedReview.locations.splice(index, 1);
               }
           }
           jQuery("#locations").val(bdrs.advancedReview.locations);
       });
       
       // add a change handler to manage locations with the select all checkbox
       jQuery("#bulkSelectCheckbox").change(function(evt) {
           var select_all_elem = jQuery(evt.currentTarget);
           var checked = select_all_elem.prop("checked");
           if (checked) {
               // add all to the locations array
               var checkboxes = select_all_elem.parents("table").find('.recordIdCheckbox:not(:disabled)');
               checkboxes.each(function() {
                   bdrs.advancedReview.locations.push(jQuery(this).val());
               });
           } else {
               bdrs.advancedReview.locations.length = 0;
           }
           jQuery("#locations").val(bdrs.advancedReview.locations);
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
            // add a link to clear the map selection
            jQuery("#resultsMessages").html("<a href=\"javascript:clearMapSelection();\">Clear map selection</a>");
            refreshLocationLayer();
        };
        
        layer.events.register('featureadded', null, function(event){
            featureAddedHandler(event.feature);
        });
        
        var featureMovedHandler = function(feature, pixel){
            jQuery("#locationArea").val(wktWriter.write(feature));
        };
        
        var featureMoveCompleteHandler = function(feature, pixel){
            featureMovedHandler(feature, pixel);
            // reload the locations
            featureAddedHandler(feature);
        };
        
        var featureModifiedHandler = function(feature, pixel) {
            featureMoveCompleteHandler(feature, pixel);
        }
        
         // Add a drag feature control to move features around.
        var dragFeature = new OpenLayers.Control.DragFeature(layer, {
            onDrag: featureMovedHandler,
            onComplete: featureMoveCompleteHandler,
            onStart: featureMovedHandler
        });
         
        var options = {
                // default editing options
                modifyFeature: false,
                drawPoint: false,
                drawLine: false,
                drawPolygon: true,
                initialDrawTool: bdrs.map.control.DRAG_FEATURE
            };
        
        bdrs.map.baseMap.addControl(dragFeature);
        dragFeature.activate();
            
        bdrs.map.baseMap.addLayers([layer]);
        bdrs.map.addEditingToolbar(bdrs.map.baseMap, layer, dragFeature, new function(){}, options);
            
        // add the locationArea feature
        var locationAreaWkt = jQuery("#locationArea").val();
        if (locationAreaWkt) {
            var feature = wktWriter.read(OpenLayers.Geometry.fromWKT(locationAreaWkt).transform(bdrs.map.WGS84_PROJECTION, bdrs.map.GOOGLE_PROJECTION));
            layer.addFeatures([feature]);
        }
    };
    
    jQuery(window).load(function() {
        // add a layer for drawing polygons to select location areas to the map
        // if the map has been initialized (i.e. we are in map view)
        if (bdrs.map.baseMap) {
            initMap();
            var layer = bdrs.map.baseMap.getLayersByName("Sightings")[0];
            
            // add a load end listener for the sightings layer to highlight the selected locations
            layer.events.register('loadend', layer, function(event) {
                highlightFeatures(layer);
            });
        }
        <c:if test="${ tableViewSelected }">
            <c:if test="${ locationArea != null and locationArea != '' }">
               // add a message about the result limitation and a link to clear the map selection
               jQuery("#resultsMessages").html("Results limited by map selection. "+
                       "<a href=\"javascript:clearMapSelection();\">Clear</a>");
            </c:if>
        </c:if>
    });
    
    var highlightFeatures = function(layer) {
        var selectControl = new OpenLayers.Control.SelectFeature(layer);
        selectControl.unselectAll();
        // highlight selected features
        for (var i = 0; i < bdrs.advancedReview.locations.length; i++) {
            var feature = layer.getFeatureByFid(bdrs.advancedReview.locations[i]);
            if (feature) {
                selectControl.select(feature);
            }
        }
    };
    
    // action to perform on the View Records button click
    var showRecordReviewWithFormOptions = function() {
       var url = bdrs.portalContextPath + "/review/sightings/advancedReview.htm";
       var query_params;
       if (bdrs.advancedReview.locations.length > 0) {
           query_params = {"locations": [jQuery("#locations").val()]};
       } else {
           // add the facet selections and location area if there are no selected locations
           query_params = bdrs.serializeObject("#facetForm", false);
           query_params["locationArea"] = jQuery("#locationArea").val();
       }

       query_params["sourcePage"] = "locations";
       bdrs.postWith(url, query_params);
    };
    
    /**
     * Refreshes the location selection layer on the map.
     */
    var refreshLocationLayer = function() {
       // refresh the map layer to only show locations within the added feature
       // don't filter this by locations
       jQuery("#locations").val('');
       // clear array
       bdrs.advancedReview.locations = [];
       
       bdrs.ajaxPostWith(bdrs.portalContextPath+bdrs.advancedReview.JSON_URL, bdrs.serializeObject("form", false), function(data) {
           var featureArray = [];
           var selFeatureArr = [];
           // use the selection geometry to determine the map zoom
           var layer = bdrs.map.baseMap.getLayersByName("Sightings")[0];
           
           for (var i = 0; i < data.length; i++) {
               bdrs.advancedReview.locations.push(data[i].id);
           }
           // remove the existing features from the layer and add the new ones
           
           // zoom the map to show the selected area
           var selectLayer = bdrs.map.baseMap.getLayersByName("Location Selector")[0];
           bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, selectLayer);

           var count = data.length;
           jQuery("#count").html(count + " location" + (count == 1 ? "" : "s") + " returned");
           // update the locations from input
           jQuery("#locations").val(bdrs.advancedReview.locations);
           highlightFeatures(layer);
       });
   };
   
   var clearMapSelection = function() {
       jQuery("#locationArea").val('');
       jQuery("#locations").val('');
       bdrs.advancedReview.locations = [];
       // clear any messages about results limitation and links to clear maps
       jQuery("#resultsMessages").html('');
       if (bdrs.map.baseMap) {
           var selectLayer = bdrs.map.baseMap.getLayersByName("Location Selector")[0];
           selectLayer.removeAllFeatures();
           // use the selection geometry to determine the map zoom
           var layer = bdrs.map.baseMap.getLayersByName("Sightings")[0];
           highlightFeatures(layer);
           bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, layer);
       } else {
           // clear the table, then reload the data
           jQuery("#alaSightingsTable").find('tbody').empty();
           bdrs.advancedReview.loadTableContent('#facetForm', '#alaSightingsTable', "${viewStyle != null ? viewStyle : 'null'}");
           refreshCount();
           jQuery("#locations").val(bdrs.advancedReview.locations);
       }
   };
   
   var refreshCount = function() {
       var queryParams = bdrs.serializeObject("#facetForm", false);
       bdrs.ajaxPostWith(bdrs.portalContextPath + bdrs.advancedReview.COUNT_URL, queryParams,
           function(data) {
               var count = parseInt(data);
               jQuery("#count").html(count + " location" + (count == 1 ? "" : "s") + " returned");
           }
       );
   };
</script>
