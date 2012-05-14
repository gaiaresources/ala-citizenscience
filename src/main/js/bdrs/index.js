bdrs.index = {};

/**
 * Template for a row in the table listing index schedules.
 */
bdrs.index.TABLE_ROW_TMPL = '\
<tr>\
    <td>\
        <input type="hidden" name="index" value="${ id }"/>${ className }\
    </td>\
    <td class="textcenter">\
        ${ type }\
    </td>\
    <td class="textcenter">\
        ${ dateString }\
    </td>\
    <td class="textcenter">\
        <span id="lastRun_${id}">${ lastRunString }</span>\
    </td>\
    <td class="textcenter">\
        <c:choose>{{if (fullRebuild)}}\
                       Yes\
                  {{else}}\
                       No\
                  {{/if}}\
    </td>\
    <td class="textcenter">\
        <a href="${ contextPath }/admin/index/dataIndexSchedule.htm?indexId=${ id }">Edit</a>\
    </td>\
    <td class="textcenter">\
        <a id="delete_${id}" href="javascript: void(0);" onclick="jQuery(this).parents(\'tr\').hide().find(\'select, input, textarea\').attr(\'disabled\', \'disabled\'); return false;"><img src="${contextPath}/images/icons/delete.png" alt="Delete" class="vertmiddle"/></a>\
    </td>\
</tr>';

/**
 * Run the indexes now
 */
bdrs.index.runIndex = function() {
    jQuery.blockUI({ message: '<h1 id="blockerMessage">Building Indexes</h1>' });
    
    jQuery.ajax({
        url: bdrs.contextPath + "/admin/index/runIndex.htm",
        type: "GET",
        data: jQuery("form").serialize(),
        success: function(data) {
            // finished processing indexes
            jQuery('#blockerMessage').text("Finished Building Indexes");
            // update the last run times or insert new rows into the table
            // compile the template
            var body = jQuery("#index_listing").find("tbody");
            body.empty();
            var compiled_tmpl = jQuery.template(bdrs.index.TABLE_ROW_TMPL);
            for (var i = 0; i < data.length; i++) {
                var index = data[i];
                console.log(index);
                // create a new row
                index.contextPath = bdrs.contextPath;
                var row = jQuery.tmpl(compiled_tmpl, index);
                row.data('indexSchedule', index);
                
                // Buffering
                body.append(row);
            }
        },
        error: function() {
            errorList.push("Error communicating with the server.");
        },
        complete: function() {
            jQuery('#blockerMessage').text("Finished Building Indexes");
            jQuery.unblockUI();
            bdrs.message.set("Indexes have been rebuilt");
        }
    });
};

/**
 * Check that something is selected for indexing before starting index build.
 */
bdrs.index.saveIndex = function() {
    // make sure that one index class is checked
    // or that there is a hidden input value for indexClass
    var indexClasses = jQuery("[name=indexClass]:checked");
    var indexClass = jQuery("input[name=indexClass][type=hidden]").val();
    if (indexClasses.length > 0 || (indexClass && indexClass.length > 0)) {
        jQuery("form").submit();
    } else {
        alert('You must select at least one thing to index!');
    }
};
