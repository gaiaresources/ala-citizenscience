/*global jQuery: false OpenLayers: false */

/*
 * Contains functions common to all Advanced Review pages.
 */
if(window.bdrs === undefined) {
    bdrs = {};
}

if(window.bdrs.advancedReview === undefined) {
    bdrs.advancedReview = {};
    bdrs.advancedReview.map = {};
}

// Some nice constants...
bdrs.advancedReview.SORT_ORDER_INPUT_SELECTOR = '#sortOrder';
bdrs.advancedReview.SORT_BY_INPUT_SELECTOR = '#sortBy';
bdrs.advancedReview.FACET_FORM_SELECTOR = "#facetForm";

bdrs.advancedReview.ASC = "ASC";
bdrs.advancedReview.DESC = "DESC";

bdrs.advancedReview.VIEW_STYLE_TABLE = "TABLE";
bdrs.advancedReview.VIEW_STYLE_DIV = "DIV";

// the sort by parameter is stored in the class variable. Use this
// regex to extract the value...
bdrs.advancedReview.SORT_BY_REGEX = "sortBy\\(([\\w\\.]+)\\)$";

/**
 * Applies click handlers to the tabs to switch between map and table views.
 */
bdrs.advancedReview.initTabHandlers = function() {

    jQuery("#listViewTab").click(function() {
        jQuery("input[name=viewType]").val("table");
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    });

    jQuery("#mapViewTab").click(function() {
        jQuery("input[name=viewType]").val("map");
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    });

    jQuery("#downloadViewTab").click(function() {
        jQuery("input[name=viewType]").val("download");
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    });

    jQuery("#imagesViewTab").click(function() {
        jQuery("input[name=viewType]").val("images");
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    });
};

/**
 * Initialises the table view by retrieving records from the server via AJAX.
 *
 * @param formSelector jQuery selector of the form containing the query parameters.
 * @param tableSelector jQuery selector of the table where the list of records will be inserted.
 * @param sortOrderSelector selector for the input providing the sorting order of the records.
 * @param sortBySelector selector for the input providing the sorting property of the records.
 * @param resultsPerPageSelector selector for the input to alter the number of records displayed per page.
 * @param viewStyle what view style to use. TABLE or DIV. defaults to table
 */
bdrs.advancedReview.initTableView = function(formSelector,
                                            tableSelector, sortOrderSelector,
                                            sortBySelector, resultsPerPageSelector, viewStyle, selectAllSelector) {

    jQuery(selectAllSelector).change(function(evt) {
        var select_all_elem = jQuery(evt.currentTarget);
        var select_all = select_all_elem.prop("checked");
        var checkboxes = select_all_elem.parents("table").find('.recordIdCheckbox:not(:disabled)');
        checkboxes.prop("checked", select_all);

    });

    // AJAX load the content for the table
    bdrs.advancedReview.loadTableContent(formSelector, tableSelector, viewStyle);

    // Change Handlers for the Sort Property and Order
    var changeHandler = function(evt) {
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    };

    jQuery(sortOrderSelector).change(changeHandler);
    jQuery(sortBySelector).change(changeHandler);
    jQuery(resultsPerPageSelector).change(changeHandler);

    var currentSortOrder = jQuery(sortOrderSelector).val();
    var currentSortBy = jQuery(sortBySelector).val();

    // Attach event handlers
    // can run this even in div view style without problems. The
    // jquery selector will return an empty list.
    bdrs.handleClassParamNodes("sortBy", function(node, sortByParam) {
        var jNode = jQuery(node);
        if (currentSortBy === sortByParam) {
            var arrowDiv = jQuery('<div class="right sortArrows"></div>');
            if (currentSortOrder === bdrs.advancedReview.ASC) {
                arrowDiv.append(jQuery('<span class="ui-upArrowAdjust ui-grid-ico-sort ui-icon-asc ui-icon ui-icon-triangle-1-n ui-sort-ltr"></span>'));
                arrowDiv.append(jQuery('<span class="ui-downArrowAdjust ui-grid-ico-sort ui-icon-desc ui-icon ui-icon-triangle-1-s ui-sort-ltr ui-state-disabled"></span>'));
                jNode.click(bdrs.advancedReview.getSortArrowClickedHandlerFcn(sortByParam, bdrs.advancedReview.DESC));
            } else {
                arrowDiv.append(jQuery('<span class="ui-upArrowAdjust ui-grid-ico-sort ui-icon-asc ui-icon ui-icon-triangle-1-n ui-sort-ltr ui-state-disabled"></span>'));
                arrowDiv.append(jQuery('<span class="ui-downArrowAdjust ui-grid-ico-sort ui-icon-desc ui-icon ui-icon-triangle-1-s ui-sort-ltr"></span>'));
                jNode.click(bdrs.advancedReview.getSortArrowClickedHandlerFcn(sortByParam, bdrs.advancedReview.ASC));
            }
            jNode.append(arrowDiv);
        } else {
            jNode.click(bdrs.advancedReview.getSortArrowClickedHandlerFcn(sortByParam, bdrs.advancedReview.ASC));
        }
        jNode.addClass("cursorPointer");
    });

};

/**
 * AJAX loads content to the table described by tableselector.
 */
bdrs.advancedReview.loadTableContent = function(formSelector, tableSelector, viewStyle) {

    var getRecordsHandlerFcn;

    if (viewStyle === bdrs.advancedReview.VIEW_STYLE_DIV) {
        getRecordsHandlerFcn = bdrs.advancedReview.getInitViewStyleDivFcn(tableSelector);
    } else {
        // default to table style
        getRecordsHandlerFcn = bdrs.advancedReview.getInitViewStyleTableFcn(tableSelector);
    }

    bdrs.advancedReview.loadRecords(formSelector, getRecordsHandlerFcn);
};


bdrs.advancedReview.loadRecords = function(formSelector, callback, url) {
    // AJAX load the content for the table
    if (!url) {
        url = bdrs.portalContextPath + bdrs.advancedReview.JSON_URL;
    }
    var queryParams = bdrs.serializeObject(formSelector, false);
    bdrs.ajaxPostWith(url, queryParams, callback);
}


/**
 * Get the function used to handle the result of 'get json records'
 * Used for the 'table' view style
 *
 * @param {Object} tableSelector - selector used to append the dom elements
 * created from the downloaded json records
 */
bdrs.advancedReview.getInitViewStyleTableFcn = function(tableSelector) {
    return function(recordArray) {

        var compiled_row_tmpl = jQuery.template(bdrs.advancedReview.TABLE_ROW_TMPL);
        var tbody = jQuery(tableSelector).find('tbody');

        for(var i=0; i<recordArray.length; i++) {
            var record = recordArray[i];
            // add the context path onto the js object...
            record.contextPath = bdrs.portalContextPath;
            // Start of sighting
            if (bdrs.authenticated) {
                record.authenticated = true;
                record.authenticatedUserId = bdrs.authenticatedUserId;
                record.isAdmin = bdrs.isAdmin;
                record.authenticatedRole = bdrs.authenticatedRole;
            }
            if (record.when) {
                record._when = record._when_formatted;
            } else {
                record.created_at = record._createdAt_formatted;
            }
            var row = jQuery.tmpl(compiled_row_tmpl, record);
            // style the odd rows
            if (i%2 != 0) {
                row.addClass("altRow");
            }

            tbody.append(row);

            // clicking on any cell except the last one should open
            var clickableCells = row.find('.openRecord');
            if(clickableCells.length > 0 && record !== undefined && record.survey !== undefined) {
                clickableCells.click({
                    recordId: record.id,
                    surveyId: record.survey.id
                }, function(ev) {
                    var data = ev.data;
                    var recordId = data.recordId;
                    var surveyId = data.surveyId;
                    window.open(bdrs.contextPath + '/bdrs/user/surveyRenderRedirect.htm?surveyId='+
                        surveyId+'&recordId='+recordId, '_self');
                });
            }

            if (bdrs.advancedReview.getCheckedState(record.id)) {
                row.find(".recordIdCheckbox").prop("checked", true);
            }
        }
    };
};

// Add the search within an area feature.
bdrs.advancedReview.addSpatialSearchFeature = function (map) {
    // check for previously drawn area taken from the facet
    var wktParser = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
    var previousArea = jQuery("input[id^='within_area']").val();
    var initialFeature = previousArea ? wktParser.read(previousArea) : null;
    // add the polygon drawing feature on the map
    var layer = bdrs.advancedReview.map.addPolygonFeature(map, bdrs.advancedReview.searchWithinAreaHandler, initialFeature);

    //create the clear map link
    // Handler
    var clearMapHandler = function (event) {
      // prevent the default anchor server call
      event.preventDefault();
      // clear the map of all drawn features
      layer.removeAllFeatures();
      //Clears the Within Area facet data and simulates a click to call server 
      var element = jQuery("input[id^='within_area']");
      element.val("");
      element.change();  // use change() instead of element.trigger("click") for IE8
    };
    var link = jQuery("<a id='clearMapLink' href='#'>Clear map selection</a>"); 
    var visibility = initialFeature ? "inline" : "none";

    link.bind("click", clearMapHandler);
    link.appendTo(jQuery("#resultsMessages"));
    // make it visible only if there is a feature
    link.css("display", visibility);
};

// Handler called after an area is drawn on the map.
//  - Update the within area facet form.
//  - Call server to get the number of matching records.
//  - Call a map refresh.
bdrs.advancedReview.searchWithinAreaHandler = function (area) {
    // Handler receiving the server response.
    var updateCountLabel = function (rcount) {
        // The top label
        jQuery("#count").html(rcount + " record" + (rcount === 0 ? "" : "s") + " returned.");
        // Update also the label of the facet to add the count  
        jQuery("label[for^='within_area']").text("Drawn Area (" + rcount + ")");
    };

    // set the label and the value of the Within Area facet
    //make sure it's visible
    jQuery("div[id^='facetOption_within']").css("display","block");
    jQuery("label[for^='within_area']").text("Drawn Area");
    jQuery("input[id^='within_area']").val(area).prop("checked", true);
    // request server for records count.
    jQuery.ajax({
        url: bdrs.portalContextPath + "/review/sightings/advancedReviewRecordCount.htm",
        type: "POST",
        data: bdrs.serializeObject("form", false),
        success: updateCountLabel,
        traditional: true
    });
    // refresh the map
    bdrs.advancedReview.map.refreshSightings();
    // be sure that the clear map selection link is visible
    jQuery("a#clearMapLink").css("display", "inline");
};


// Refresh the Sigthings layer of the map (the one containing the KML points) by resending request to server with the 
// params updated from the form.
bdrs.advancedReview.map.refreshSightings = function () {
    var slayer = bdrs.map.baseMap.getLayersByName("Sightings")[0];
    if (slayer && slayer.protocol) {
        slayer.destroyFeatures();
        // Hack!! When no records are returned by the server, the map is not 'cleaned' but cached records are shown.
        // The cached records are stored in the Cluster Strategy
        // Best to call the clearCache function of every strategy
        for (var i = 0; i < slayer.strategies.length; i++) {
            if (typeof slayer.strategies[i].clearCache === 'function') {
                slayer.strategies[i].clearCache();
            }    
        }

        // update the request post with new parameters taken from the form
        var params = bdrs.serializeObject("form", true); //param encoding should be true for KML and false for JSON (???)        
        var protocol = new OpenLayers.Protocol.HTTP({
                url: slayer.protocol.url,
                format: slayer.protocol.format,                
                readWithPOST: slayer.protocol.readWithPOST,
                params: params
        });
        slayer.protocol = protocol;
        slayer.refresh();
    }
};

// Add the editing tool bar to the given map.
// Only a single feature can be drawn.
// Callbacks:
//   - searchAreaHandler: called when a feature is added or moved with the area serialized as string
// Return the layer containing the features.
bdrs.advancedReview.map.addPolygonFeature = function (map, searchAreaHandler, initialFeature) {
    var editControls = []; // contains the 3 feature controls  (draw, drag, modify)
                           // put them in an array to help the top handler to deactivate them.
    //
    // handler: a single handler called when a polygon is added, moved or modified.
    //    
    var featureChangedHandler = function (feature) {
        if (!feature || !feature.layer) {
            return;
        }
        // Remove other features (polygons) previously drawn
        var layer = feature.layer;
        var featuresToRemove = [];
        var i = 0;
        for (i = 0; i < layer.features.length; i++) {
            if (feature !== layer.features[i]) {
                featuresToRemove.push(layer.features[i]);
            }
        }
        layer.removeFeatures(featuresToRemove);
        //Trick: We need to deactivate all the editing controls in order to have access
        // to the sightings popup feature.
        for (i = 0; i < editControls.length; i++) {
            editControls[i].deactivate();
        }
        // zoom the map to show the selected area
        bdrs.map.centerMapToLayerExtent(map, layer);
        //WKT serialize the feature and pass it to the search handler passed in arg.
        if (typeof searchAreaHandler === 'function') {
            var wktParser = new OpenLayers.Format.WKT(bdrs.map.wkt_options);
            searchAreaHandler(wktParser.write(feature));
        }
    };

    //
    // Map initialization
    //
    var styleMap = {
        'strokeWidth': 2,
        'strokeOpacity': 1,
        'strokeColor': '#669900',
        'fillColor': '#669900',
        'fillOpacity': 0.2,
        'pointRadius': 4
    };

    // the layer to host the drawing of features
    var vlayer = new OpenLayers.Layer.Vector("Areas", { "styleMap": new OpenLayers.StyleMap(styleMap)});
    // draw initial feature passed in arg.
    // It is important to add the feature before registering the 'featureadded' handler
    // to avoid the triggering of a search request, not needed at this point.
    if (initialFeature){
        vlayer.addFeatures([initialFeature]);
        bdrs.map.centerMapToLayerExtent(map, vlayer);
    }

    vlayer.events.register('featureadded', null, function (event) {
        featureChangedHandler(event.feature);
    });
    map.addLayer(vlayer);


    // Add a drag feature control to move features around.
    var dragFeature = new OpenLayers.Control.DragFeature(vlayer, {
        onComplete: featureChangedHandler
    });
    map.addControl(dragFeature);

    // The 'classic' button controls
    // The editing tool bar holding the 'classic' buttons
    var toolbar = new OpenLayers.Control.Panel({
            displayClass: 'olControlEditingToolbar'
        });
    // The 'hand' control is there to activate/deactivate the drag feature 
    var navControl = new OpenLayers.Control({
        displayClass: 'olControlNavigation',
        autoActivate: false,
        defaultControl: false,
        title: "Select/Drag area"
    });
    navControl.events.register("activate", null, function () { dragFeature.activate(); });
    navControl.events.register("deactivate", null, function () { dragFeature.deactivate(); });

    //draw polygon
    var drawPolygonControl = new OpenLayers.Control.DrawFeature(vlayer, OpenLayers.Handler.Polygon, {
        displayClass: 'olControlDrawFeaturePolygon',
        title: "Draw a polygon to search over an area"
    });

    // // modify polygon
    // var modifyFeatureControl = new OpenLayers.Control.ModifyFeature(vlayer, {
    //     vertexRenderIntent: 'temporary',
    //     displayClass: 'olControlModifyFeature',
    //     title: "Modify an area"
    // });
    // vlayer.events.register("featuremodified", null, function (event) {
    //     featureChangedHandler(event.feature);
    // });


    // put them in an array, it will help the top handler to deactivate them all.
    // var is declared at the top.
    editControls = [navControl, drawPolygonControl];
    toolbar.addControls(editControls);
    map.addControl(toolbar);
    return vlayer;
};

/**
 * Page selected handler
 *
 * @param {Object} pageNumber
 */
bdrs.advancedReview.pageSelected = function(pageNumber) {
    jQuery("#pageNumber").val(pageNumber);
    jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
};

bdrs.advancedReview.doBulkAction = function(noneSelectedMessage, confirmMessage, path, params) {
    var idArray = new Array();
    // select all of the checked checkboxes
    jQuery('.recordIdCheckbox:checked').each(function(index, element) {
        // need to rebox the dom element
        idArray.push(jQuery(element).val());
    });

    if (idArray.length == 0) {
        alert(noneSelectedMessage);
        return;
    }

    if (confirm(confirmMessage)) {

        var url = bdrs.portalContextPath + path;
        var param = {
            recordId: idArray,
            // return to the current page with the current facet settings.
            redirecturl: window.location.href
        };

        if (params) {
            jQuery().extend(param, params);
        }

        bdrs.postWith(url, param);
    }
};

/**
 * Initialises the map view by requesting a KML from the server and populating
 * the record count.
 *
 * @param formSelector jQuery selector of the form containing the query parameters.
 * @param mapId the id of the element where the map will be inserted.
 * @param mapOptions options to be used for map initialisation.
 */
bdrs.advancedReview.initMapView = function(formSelector, mapId, mapOptions, idSelector, initMapLayersFcn) {

    bdrs.map.initBaseMap(mapId, mapOptions);

	if (jQuery.isFunction(initMapLayersFcn)) {
		initMapLayersFcn(bdrs.map.baseMap);
	}

    bdrs.map.centerMap(bdrs.map.baseMap);
    // bdrs.map.baseMap.events.register('addlayer', null, bdrs.map.addFeaturePopUpHandler);
    // bdrs.map.baseMap.events.register('removeLayer', null, bdrs.map.removeFeaturePoupUpHandler);

    var queryParams = bdrs.serializeObject(formSelector, true);

    var kmlURL = bdrs.portalContextPath + bdrs.advancedReview.KML_URL;
    var selectedId = jQuery(idSelector).val();
    var style = bdrs.map.createOpenlayersStyleMap(selectedId.toString());

    var layerOptions = {
        visible: true,
        includeClusterStrategy: true,
        styleMap: style
    };
    var layer = bdrs.map.addKmlLayerWithPost(bdrs.map.baseMap, "Sightings", kmlURL, layerOptions, selectedId, queryParams);
    bdrs.map.addFeatureClickPopup(layer);
    layer.events.register('loadend', layer, function(event) {
        bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, layer);
        bdrs.map.recordOriginalCenterZoom(bdrs.map.baseMap);
    });
};

/**
 * Initialises the facets by attaching change handlers to the facet options.
 *
 * @param formSelector jQuery selector for the form that will be submitted when the facet option is changed.
 * @param facetSelector jQuery selector for the facet container.
 */
bdrs.advancedReview.initFacets = function(formSelector, facetSelector) {
    var facet = jQuery(facetSelector);
    var form = jQuery(formSelector);
    facet.find('.facetOptions input[type=checkbox]').change(function() {
        form.submit();
    });

    facet.find(".select_all input[type=checkbox]").change(function(evt) {
        var select_all_elem = jQuery(evt.currentTarget);
        var select_all = select_all_elem.prop("checked");
        select_all_elem.parents(facetSelector).find('.facetOptions input[type="checkbox"]').prop("checked", select_all);
        form.submit();
    });

    jQuery('.showMore > a').click(function() {
        var parent = jQuery(this).parent().parent();
        parent.children('div').show();
        jQuery(this).parent().hide();
        parent.find('.showLess').show();
        parent.find('input[name$=_e]').removeAttr('disabled');

    });
    jQuery('.showLess > a').click(function() {
        var parent = jQuery(this).parent().parent();
        parent.children('div:.overflow').hide();
        jQuery(this).parent().hide();
        parent.find('.showMore').show();
        parent.find('input[name$=_e]').attr('disabled', 'disabled');
    });
    jQuery('input[name$=_e]:not([disabled])').parent().find('.showMore > a').click();
};

/**
 * Click handler for reporting links. This function will perform a GET request to the advanced review
 * report request handler method with all inputs in the specified form as well as the specified report id.
 * @param {Object} formSelector - selector for the facet form.
 * @param {Object} reportId - the primary key of the report to run.
 */
bdrs.advancedReview.renderReport = function(formSelector, reportId) {
    var queryParams = bdrs.serializeObject(formSelector, false);
    queryParams["reportId"] = reportId;
    var url = bdrs.portalContextPath + "/review/sightings/advancedReviewReport.htm";
    bdrs.postWith(url, queryParams);
};

/**
 * Initialises the record download tab
 *
 * @param {Object} formSelector - selector for the facet form
 * @param {Object} downloadSelector - selector for the download button
 */
bdrs.advancedReview.initRecordDownload = function(formSelector, downloadSelector) {
    jQuery(downloadSelector).click(function(event) {
        var queryParams = bdrs.serializeObject(formSelector, false);
        var downloadURL = portalContextPath + bdrs.advancedReview.DOWNLOAD_URL;
        bdrs.postWith(url, queryParams);
    });
};

/**
 * Gets the handlers for the up/down arrows used in the table style view for the list tab.
 *
 * @param {Object} sortBy - the 'sort by' to use when firing click events
 * @param {Object} sortOrder - the 'sort order' to use when firing click events
 */
bdrs.advancedReview.getSortArrowClickedHandlerFcn = function(sortBy, sortOrder) {
    return function() {
        jQuery(bdrs.advancedReview.SORT_ORDER_INPUT_SELECTOR).val(sortOrder);
        jQuery(bdrs.advancedReview.SORT_BY_INPUT_SELECTOR).val(sortBy);
        jQuery(bdrs.advancedReview.FACET_FORM_SELECTOR).submit();
    };
};

/**
 * Copy of bdrs.mySightings.downloadSightingsWidgetInit
 * Necessary because this form must do a POST request for the download
 * because data can be too large for GET.
 */
bdrs.advancedReview.downloadSightingsWidgetInit = function(formSelector, fileFormatSelectionChangeCallback) {
    // Disable file downloading if no format is selected, and update the permalink
    jQuery(bdrs.review.downloadSightingsWidget.DOWNLOAD_FILE_FORMAT_SELECTOR).change(function(event) {
        if (fileFormatSelectionChangeCallback) {
            fileFormatSelectionChangeCallback();
        }
        var checked = jQuery(bdrs.review.downloadSightingsWidget.DOWNLOAD_FILE_FORMAT_SELECTOR).filter(":checked");
        jQuery(bdrs.review.downloadSightingsWidget.FILE_DOWNLOAD_BUTTON_SELECTOR).prop("disabled", checked.length === 0);
    }).trigger("change");

    // Click handler for the file download tab
    jQuery(bdrs.review.downloadSightingsWidget.FILE_DOWNLOAD_BUTTON_SELECTOR).click(function(event) {
        var url = [
            bdrs.portalContextPath,
            bdrs.advancedReview.DOWNLOAD_URL
        ].join('');
        var queryParams = bdrs.serializeObject(formSelector, false);

        bdrs.postWith(url, queryParams);
    });
};
