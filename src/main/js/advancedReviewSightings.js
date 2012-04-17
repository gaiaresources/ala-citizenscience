/*global jQuery: false OpenLayers: false */
if(window.bdrs === undefined) {
    bdrs = {};
}

if(window.bdrs.advancedReview === undefined) {
	bdrs.advancedReview = {};
}

bdrs.advancedReview.JSON_URL = "/review/sightings/advancedReviewJSONSightings.htm?";
bdrs.advancedReview.KML_URL = "/review/sightings/advancedReviewKMLSightings.htm?";
bdrs.advancedReview.DOWNLOAD_URL = "/review/sightings/advancedReviewDownload.htm?";

bdrs.advancedReview.TABLE_ROW_TMPL = '\
<tr id="record_row_${ id }">\
    <td>${ censusMethod ? censusMethod.type : "Observation" }</td>\
    <td><a href="${ contextPath }/bdrs/user/surveyRenderRedirect.htm?surveyId=${ survey.id }&recordId=${ id }">${ _when }</a></td>\
    <td class=\"scientificName\">${ species ? species.scientificName : "N/A" }</td>\
    <td class=\"commonName\">${ species ? species.commonName : "N/A" }</td>\
    <td>${ geometry ? latitude : "N/A" }, ${ geometry ? longitude : "N/A" }</td>\
    <td>${ user.name }</td>\
    {{if authenticated}}\
       <td><input title=\"Select / deselect this record\" type=\"checkbox\" class=\"recordIdCheckbox\" value=\"${ id }\" \
       {{if user.id !== authenticatedUserId && !(authenticatedRole === \"ROLE_ROOT\" || authenticatedRole === \"ROLE_ADMIN\" || authenticatedRole === \"ROLE_SUPERVISOR\") }}\
           disabled=\"true\"\
       {{/if}}\
       /></td>\
    {{/if}}\
</tr>';

/**
 * Gets the checked state of an item in the records list on page load.
 * This is always false for records.
 */
bdrs.advancedReview.getCheckedState = function(id) {
	return false;
}

/**
 * Get the function used to handle the result of 'get json records'
 * Used for the 'div' view style
 * 
 * @param {Object} tableSelector - selector used to append the dom elements 
 * created from the downloaded json records
 */
bdrs.advancedReview.getInitViewStyleDivFcn = function(tableSelector) {
    return function(recordArray) {
        var html = [];
        for(var i=0; i<recordArray.length; i++) {
            var record = recordArray[i];
            // Start of sighting
    
            html.push('<div class="sighting">');
            
            // Start of first line
            html.push('<div>');
            // Record Type
            html.push('<span class="recordType">');
            if(record.censusMethod !== null && record.censusMethod !== undefined) {
                html.push(record.censusMethod.type);
                html.push(':&nbsp;');
            } else {
                html.push("Observation:&nbsp;");   
            }
            html.push("</span>");
            
            // Date
            html.push('<span class="nowrap">');
            if (bdrs.isAdmin || bdrs.authenticatedUserId == record.user.id) {
                html.push('<a href="');
                html.push(bdrs.contextPath);
                html.push('/bdrs/user/surveyRenderRedirect.htm?surveyId=');
                html.push(record.survey.id);
                html.push('&recordId=');
                html.push(record.id);
                html.push('">');
            }
            html.push(bdrs.util.formatDate(new Date(record.when)));
            if (bdrs.isAdmin || bdrs.authenticatedUserId == record.user.id) {
                html.push('</a>');
            }
            html.push('</span>');
            
            // Scientific Name
            if(record.species !== null && record.species !== undefined) {
                html.push('&nbsp;&mdash;&nbsp;');
                
                html.push('<span class="taxonRank">');
                html.push(titleCaps(record.species.taxonRank.toLowerCase()));
                html.push(':&nbsp;</span>');
                
                html.push('<span class="scientificName">');
                html.push(record.species.scientificName);
                html.push('</span>');
                
                html.push('&nbsp;|&nbsp;');
                
                html.push('<span class="commonName">');
                html.push(record.species.commonName);
                html.push('</span>');
            }
            
            // End of first line
            html.push('</div>');
            
            // Start of second line
            html.push('<div>');
            
            // Location
            html.push('<span class="location">');
            if(record.location !== null && record.location !== undefined) {
                html.push('Location:&nbsp;');
                html.push(record.location.name);
            } else {
                html.push('Coordinate:&nbsp;');
                html.push(record.latitude);
                html.push(',&nbsp;');
                html.push(record.longitude);
            }
            html.push('</span>');
            
            html.push('<span class="username">');
            if(record.user !== null && record.user !== undefined) {
                html.push('&nbsp;&nbsp;|&nbsp;&nbsp;');
                html.push('User:&nbsp;');
                html.push(record.user.name.replace(/@\S+/i, ""));
            }
            html.push('</span>');
            
            // only show when user is logged in
            if (bdrs.authenticated) {
                // select record checkbox
                html.push('<div class="right"><input title="select / deselect this record" type="checkbox" class="recordIdCheckbox" value="' + record.id + '" /></div>');   
            }
            
            // End of second line
            html.push("</div>");
            
            // only show when user is logged in
            if (bdrs.authenticated) {
                // clearing div for the select record checkbox
                html.push('<div class="clear"></div>'); 
            }
            
            // End of sighting
            html.push("</div>");
            
        }
        jQuery(tableSelector).append(html.join(''));
    };
};

/**
 * Performs the bulk delete. Will return to current page with current facet settings
 */
bdrs.advancedReview.bulkDelete = function() {
    bdrs.advancedReview.doBulkAction("No records selected for deletion", 
                 'Are you sure you want to delete the selected record(s)?', 
                 "/bdrs/user/deleteRecord.htm");
};

/**
 * Performs the bulk hold/release. Will return to current page with current facet settings
 */
bdrs.advancedReview.bulkModerate = function(hold) {
    var holdString = hold ? "hold" : "release";
    bdrs.advancedReview.doBulkAction("No records selected for "+holdString, 
                 'Are you sure you want to '+holdString+' the selected record(s)?', 
                 "/bdrs/user/moderateRecord.htm", { hold: hold });
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
    var kmlURL = bdrs.contextPath + "/review/sightings/advancedReviewKMLSightings.htm?" + queryParams;
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