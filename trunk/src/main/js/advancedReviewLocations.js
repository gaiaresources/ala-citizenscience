/*global jQuery: false OpenLayers: false */
if(window.bdrs === undefined) {
    bdrs = {};
}

if(window.bdrs.advancedReview === undefined) {
	bdrs.advancedReview = {};
}

bdrs.advancedReview.TABLE_ROW_TMPL = '\
<tr id="location_row_${ id }">\
    <td>${ name }</td>\
    <td>${ description }</td>\
    <td>${ latitude ? latitude : "N/A" }, ${ longitude ? longitude : "N/A" }</td>\
    <td>${ user ? user.name : "N/A" }</td>\
    {{if authenticated}}\
       <td><input title=\"Select / deselect this location\" type=\"checkbox\" class=\"recordIdCheckbox\" value=\"${ id }\" \
	/></td>\
    {{/if}}\
</tr>';

bdrs.advancedReview.JSON_URL = "/review/sightings/advancedReviewJSONLocations.htm?";
bdrs.advancedReview.KML_URL = "/review/sightings/advancedReviewKMLLocations.htm?";
bdrs.advancedReview.DOWNLOAD_URL = "/review/sightings/advancedReviewDownloadLocations.htm?";

/**
 * Gets the checked state of an item in the location list on page load.
 * This is true for any previously selected location, stored in the "locations" variable.
 */
bdrs.advancedReview.getCheckedState = function(id) {
	return locations.indexOf(id) != -1;
}

/**
 * Get the function used to handle the result of 'get json locations'
 * Used for the 'div' view style
 * 
 * @param {Object} tableSelector - selector used to append the dom elements 
 * created from the downloaded json locations
 */
bdrs.advancedReview.getInitViewStyleDivFcn = function(tableSelector) {
    return function(locArray) {
        var html = [];
        for(var i=0; i<locArray.length; i++) {
            var location = locArray[i];
            // Start of sighting
    
            html.push('<div class="sighting">');
            
            // Start of first line
            html.push('<div>');
            // Location Name
            html.push('<span>');
            html.push(location.name);
            html.push("</span>");
         	// Location Description
            html.push('<span>');
            html.push(location.description);
            html.push("</span>");
            // Location geometry
            html.push('<span class="location">');
            html.push(location.latitude);
            html.push(',&nbsp;');
            html.push(location.longitude);
            html.push('</span>');
            
            html.push('<span class="username">');
            if(location.user !== null && location.user !== undefined) {
                html.push('&nbsp;&nbsp;|&nbsp;&nbsp;');
                html.push('User:&nbsp;');
                html.push(location.user.name.replace(/@\S+/i, ""));
            }
            html.push('</span>');
            
            // only show when user is logged in
            if (bdrs.authenticated) {
                // select location checkbox
                html.push('<div class="right"><input title="select / deselect this location" type="checkbox" class="recordIdCheckbox" value="' + location.id + '" /></div>');   
            }
            
            // End of second line
            html.push("</div>");
            
            // only show when user is logged in
            if (bdrs.authenticated) {
                // clearing div for the select location checkbox
                html.push('<div class="clear"></div>'); 
            }
            
            // End of sighting
            html.push("</div>");
            
        }
        jQuery(tableSelector).append(html.join(''));
    };
};