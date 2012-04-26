/*global jQuery: false OpenLayers: false */

/*
 * Contains functions common to all Advanced Review pages.
 */
if(window.bdrs === undefined) {
    bdrs = {};
}

if(window.bdrs.advancedReview === undefined) {
	bdrs.advancedReview = {};
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
    var url = bdrs.contextPath + bdrs.advancedReview.JSON_URL;
    var queryParams = jQuery(formSelector).serialize();

    var getRecordsHandlerFcn;
    
    if (viewStyle === bdrs.advancedReview.VIEW_STYLE_DIV) {
        getRecordsHandlerFcn = bdrs.advancedReview.getInitViewStyleDivFcn(tableSelector);
    } else {
        // default to table style
        getRecordsHandlerFcn = bdrs.advancedReview.getInitViewStyleTableFcn(tableSelector);
    }
    
    jQuery.getJSON(url + queryParams, {}, getRecordsHandlerFcn);
    
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
            record.contextPath = bdrs.contextPath;
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
            
            if (bdrs.advancedReview.getCheckedState(record.id)) {
            	row.find(".recordIdCheckbox").prop("checked", true);
            }
        }
    };
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

        var url = bdrs.contextPath + path;
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
bdrs.advancedReview.initMapView = function(formSelector, mapId, mapOptions, idSelector) {

    bdrs.map.initBaseMap(mapId, mapOptions);
    bdrs.map.centerMap(bdrs.map.baseMap);
    bdrs.map.baseMap.events.register('addlayer', null, bdrs.map.addFeaturePopUpHandler);
    bdrs.map.baseMap.events.register('removeLayer', null, bdrs.map.removeFeaturePoupUpHandler);
    
    var queryParams = jQuery(formSelector).serialize();
    var kmlURL = bdrs.contextPath + bdrs.advancedReview.KML_URL + queryParams;
    var selectedId = jQuery(idSelector).val();
    var style = bdrs.map.createOpenlayersStyleMap(selectedId.toString());
    
    var layerOptions = {
        visible: true,
        includeClusterStrategy: true,
        styleMap: style
    };

    var layer = bdrs.map.addKmlLayer(bdrs.map.baseMap, "Sightings", kmlURL, layerOptions, selectedId);
    layer.events.register('loadend', layer, function(event) {
        bdrs.map.centerMapToLayerExtent(bdrs.map.baseMap, layer);
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
        parent.find('input[name$=_expanded]').removeAttr('disabled');

    });
    jQuery('.showLess > a').click(function() {
        var parent = jQuery(this).parent().parent();
        parent.children('div:.overflow').hide();
        jQuery(this).parent().hide();
        parent.find('.showMore').show();
        parent.find('input[name$=_expanded]').attr('disabled', 'disabled');
    });
    jQuery('input[name$=_expanded]:not([disabled])').parent().find('.showMore > a').click();
};

/**
 * Click handler for reporting links. This function will perform a GET request to the advanced review
 * report request handler method with all inputs in the specified form as well as the specified report id.
 * @param {Object} formSelector - selector for the facet form.
 * @param {Object} reportId - the primary key of the report to run.
 */
bdrs.advancedReview.renderReport = function(formSelector, reportId) {
    var query_params = jQuery(formSelector).serialize();
    query_params += "&reportId="+reportId;
    var url = bdrs.contextPath + "/review/sightings/advancedReviewReport.htm?"+query_params;
    document.location = url;
};

/**
 * Initialises the record download tab
 * 
 * @param {Object} formSelector - selector for the facet form
 * @param {Object} downloadSelector - selector for the download button
 */
bdrs.advancedReview.initRecordDownload = function(formSelector, downloadSelector) {
    jQuery(downloadSelector).click(function(event) {
        var queryParams = jQuery(formSelector).serialize();
        var downloadURL = bdrs.contextPath + bdrs.advancedReview.DOWNLOAD_URL + queryParams;
        window.document.location = downloadURL; 
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
