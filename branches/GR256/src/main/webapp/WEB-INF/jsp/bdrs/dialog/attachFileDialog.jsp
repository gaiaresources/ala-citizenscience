<div id="attachFileDialog" title="Upload/Select File">

    <h3><a href="#">Upload New File</a></h3>
    <div>
        <form id="saveManagedFile" method="POST" enctype="multipart/form-data" action="${portalContextPath}/bdrs/user/managedfile/service/edit.htm">

            A file attached here will be added to the managed files interface and be associated with the selected element.

            <table class="form_table">
                <tbody>

                <tr>
                    <th>File:</th>
                    <td>
                        <input type="file" name="file" onchange="jQuery('#filename').val(jQuery(this).val()); jQuery('#filename').blur();"/>
                        <input id="filename" type="text" class="validate(required)" style="visibility:hidden;height:0em;width:0em;" name="filename" value=""/>
                    </td>
                </tr>
                <tr>
                    <th>Description:</th>
                    <td>
                        <textarea name="description"  class="validate(required)"></textarea>
                    </td>
                </tr>
                <tr>
                    <th>Credit:</th>
                    <td>
                        <input type="text" name="credit" value=""/>
                    </td>
                </tr>
                <tr>
                    <th>License:</th>
                    <td>
                        <input type="text" name="license" value=""/>
                    </td>
                </tr>
                </tbody>
            </table>

        </form>

    </div>


    <h3><a href="#">Select from existing Managed Files</a></h3>
    <div id="managedFileSearchDiv">

        <form class="widgetSearchForm" id="searchForm" method="GET">
            <table >
                <tr>
                    <td class="formlabel">Search for</td>
                    <td><input type="text" name="fileSearchText" id="searchText" size="20"/></td>
                    <td class="formlabel">For user</td>
                    <td align="right"><input type="text" name="userSearchText" id="user" size="20"/></td>
                </tr>
                <tr>
                    <td class="formlabel">Images only</td>
                    <td> <input type="checkbox" name="imagesOnly" value="True"/></td>
                    <td colspan="2" align="right" style="align:right"><input type="button" id="managedFileSearch" class="form_action" value="Search"/></td>
                </tr>
                
            </table>

            <input id="selectedUuid" type="text" title="A single file selection is required." style="visibility:hidden;height:0em;width:0em;" name="selectedUuid" value=""/>
        </form>
        <div id="selectionError" class="error" style="display:none">
            Please select a Managed File from the search results.
        </div>

        <div id="searchResults" class="auto-row-height-jqgrid">
            <table id="managedFilesList"></table>
        </div>
        <div id="pager2"></div>

    </div>
</div>


<script type="text/javascript">
    // ***********************************************************
    // Setup the managed files search results table as a jq-grid.
    // ***********************************************************
    var downloadURL = function(file) {
        return '${portalContextPath}/files/download.htm?'+file;
    };

    var previewFormatter = function(cellvalue, options, rowObject) {
        if (rowObject.contentType.substring(0, 5) === 'image') {
            return "<img style='width:80px;height:80px;align:center' src='"+downloadURL(rowObject.fileURL)+"'></img>";
        }
        return "";
    };
    var filenameFormatter = function(cellvalue, options, rowObject) {
        return "<a href='"+downloadURL(rowObject.fileURL)+"'>"+rowObject.filename+"</a>";
    };
    
    jQuery(function() {
    jQuery("#managedFilesList").jqGrid({
        datatype: "json",
        mtype: "GET",
        colNames:['Id', 'uuid', 'Last Modified', 'User', 'Filename / Download', 'Description', 'Preview'],
        colModel:[
            {name:'id', hidden:true, key:true},
            {name:'uuid', hidden:true},
            {name:'updatedAt',index:'file.updatedAt', sorttype:'date', width:80},
            {name:'updatedBy.name',index:'updatedBy.firstName', width:100},
            {name:'filename', index:'filename', width:180, formatter:filenameFormatter},
            {name:'description', index:'description', width:300},
            {name:'fileURL', width:85, sortable: false, formatter:previewFormatter, valign:'center'}
        ],
        height: '100%',

        jsonReader : { repeatitems: false },
        rowNum:5,
        rowList:[5,10,20,30],
        shrinkToFit : true,
        pager: '#pager2',
        sortname: 'file.updatedAt',
        viewrecords: true,
        sortorder: "desc",
        caption:"Managed Files",
        onSelectRow: function(id, status) {
            jQuery('#selectedUuid').val(jQuery(this).getCell(id, 'uuid'));
            jQuery('#selectionError').hide();

        }

    });

    jQuery("#managedFilesList").jqGrid('navGrid','#pager2',{edit:false,add:false,del:false});
    

    jQuery("#managedFileSearch").click(gridReload);

    // Don't allow the form to be submitted as we need to do it via ajax, but we need to trigger the submit
    // function to make ketchup validate.
    jQuery('#saveManagedFile').submit(function() {
        return false;
    });
    

    jQuery("#attachFileDialog").accordion({
        clearStyle: true,
        autoHeight: false
    }).dialog({
                width: '850',
                height: '650',
                modal: true,
                autoOpen: false,
                open: dialogOpen,
                close: dialogClose,
                resizeStop : handleDialogResize,
                zIndex: bdrs.MODAL_DIALOG_Z_INDEX,
                buttons: [
                    {
                        id: "selectFileCancel",
                        text: "Cancel",
                        click: function() {
                            bdrs.attribute.closeHtmlEditor($(this));
                        }
                    },
                    {
                        id : "selectFileOk",
                        text: "OK"
                    }
                ]

            });

    // Attach the event handler for when the OK button is selected on the file selection dialog.
    jQuery('#attachFileDialog').dialog("option", "buttons")[1].click = fileSelectionDialogOkPressed;
   
   
    

    });
    var SEARCH_USER_URL = '${portalContextPath}/bdrs/user/managedfile/service/search.htm';

    function getUserSearchUrl() {
        var params = jQuery("#searchForm").serialize();
        return SEARCH_USER_URL + '?' + params;
    };

    function gridReload(){
        jQuery("#managedFilesList").jqGrid('setGridParam',{
            url:getUserSearchUrl(),
            page:1}).trigger("reloadGrid");
    };

    // ************************************************************
    // Wrap the content in an accordian then turn it into a dialog
    // ************************************************************
    var ACCORDION_NEW_FILE_TAB = 0;
    var ACCORDION_EXISTING_FILE_TAB = 1;

    // Resizes the search results table when the dialog is resized.
    var handleDialogResize = function(event, ui) {
        var grid =  jQuery("#managedFilesList").jqGrid();
        grid.setGridWidth(jQuery("#searchResults").width());
    };

    // A version of the ketchup error container positioning function that works correctly inside
    // the dialog.
    var positionContainerInDialog = function(errorContainer, field) {
        var fOffset = field.position();
        errorContainer.css({
            left: fOffset.left + field.width() - 10,
            top: fOffset.top - errorContainer.height()
        });
    };


    // Callback for when the dialog opens.  Configures ketchup to position errors properly inside the dialog.
    var dialogOpen = function(event, ui) {
        var form = jQuery('#saveManagedFile');
        form.find('.hasKetchup').unbind();
        form.find('.hasKetchup').removeClass('hasKetchup');
        form.ketchup({positionContainer:positionContainerInDialog, initialPositionContainer:positionContainerInDialog});
        resetDialogContents(event, ui);
    };

    // Callback when the dialog is closed.  Resets ketchup back to the defaults.
    var dialogClose = function(event, ui) {
        jQuery('#searchForm').ketchup();
    };

    // Clears form fields and search results when the dialog is opened.
    var resetDialogContents = function(event, ui) {
        selectedUuid = "";
        var form = jQuery('#saveManagedFile');
        form.resetForm();
        jQuery('#saveManagedFile .ketchup-error-container').hide();

        jQuery('#searchForm').resetForm();
        jQuery('#selectionError').hide();
        jQuery('#managedFilesList').jqGrid().clearGridData();

    };

    /**
     * Returns true if the form is valid.
     * This is done by checking if there are any visible ketchup dialogs....yuck.
     */
    var isAddManagedFileFormValid = function() {
        // Submit the form so ketchup does its thing.
        jQuery('#saveManagedFile').submit();

        var valid = true;
        jQuery('#saveManagedFile .ketchup-error-container').each(function(index, el) {
            valid = valid && (jQuery(el).css('display') === 'none');
        });
        return valid;
    }

    var getHeight = function() {
        return windowHeight =  jQuery(window).height()-50;
    }

    /**
     * Closes the attach file dialog and updates the position of the
     * row in the species profile table if the "make this the default profile
     * iamge" checkbox was selected.
     * @param dialog the dialog to close.
     * @param checkBoxSelector a jQuery selector that can be used to locate the
     * correct checkbox.
     */
     var closeAndUpdateRowPosition = function(dialog, checkBoxSelector) {
         jQuery(dialog).dialog('close');
         if (jQuery(checkBoxSelector).is(":checked")) {
             var row = jQuery(contentFieldBeingEdited).parents('tr:first');
             bdrs.taxonomy.moveRowToTop(row);
         }
     };

     /**
      * Callback when the OK button on the file selection dialog is pressed.
      * Checks if the selection is valid and performs the appropriate action
      * depending on whether a file was selected or a new file was added.
      */
     var fileSelectionDialogOkPressed = function() {
         var selected = jQuery('#attachFileDialog').accordion("option", "active");

         if (selected === ACCORDION_EXISTING_FILE_TAB) {
             var selectedUuid = jQuery('#selectedUuid').val();
             if (selectedUuid != null && selectedUuid.length > 0) {
                 //jQuery(contentFieldBeingEdited).attr("value", selectedUuid);
                 jQuery(contentFieldBeingEdited).val(selectedUuid);
                 closeAndUpdateRowPosition(this, "#selectPreferred");
             }
             else {
                 jQuery('#selectionError').show();
             }
         }
         else {
             if (isAddManagedFileFormValid()) {
                 var options = {
                     success: function(data){
                    	 jQuery(contentFieldBeingEdited).val(data.data.uuid);
                     },
                     error: function(msg){
                    	console.log(msg); 
                     },
                     dataType : 'json'
                 };
                 jQuery("#saveManagedFile").ajaxSubmit(options);
                 closeAndUpdateRowPosition(this, '#addNewPreferred');
             }
         }
     };

     var showFileSelector = function(element) {
        contentFieldBeingEdited = element;
        jQuery('#attachFileDialog').dialog('option', 'height', getHeight());
        jQuery( "#attachFileDialog" ).dialog('open');
    };

</script>
