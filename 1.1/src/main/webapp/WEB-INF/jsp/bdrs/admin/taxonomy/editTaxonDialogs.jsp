
<!--  DIALOGS -->
<div id="htmlEditorDialog" title="HTML Editor">
    <label>Edit the HTML content that you want to display in the editor below: </label>
    <textarea id="markItUp"></textarea>
</div>

<div id="attachFileDialog" title="Attach File to Taxon Profile">

    <h3><a href="#">Upload New File</a></h3>
    <div>
        <form id="saveManagedFile" method="POST" enctype="multipart/form-data" action="${pageContext.request.contextPath}/bdrs/user/managedfile/service/edit.htm">

            A file attached here will be added to the managed files interface and be associated with the selected Taxon Profile Element.

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
                <tr>
                    <th>Make this the preferred profile image</th>
                    <td>
                        <input type="checkbox" name="addNewPreferred" id="addNewPreferred"/>
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
                <tr>
                    <td class="formLabel">Make this the preferred profile image</td>
                    <td><input type="checkbox" name="selectPreferred" id="selectPreferred"/></td>
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
        return '${pageContext.request.contextPath}/files/download.htm?'+file;
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

    var SEARCH_USER_URL = '${pageContext.request.contextPath}/bdrs/user/managedfile/service/search.htm';

    function getUserSearchUrl() {
        var params = jQuery("#searchForm").serialize();
        return SEARCH_USER_URL + '?' + params;
    };

    function gridReload(){
        jQuery("#managedFilesList").jqGrid('setGridParam',{
            url:getUserSearchUrl(),
            page:1}).trigger("reloadGrid");
    };

    jQuery("#managedFileSearch").click(gridReload);

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

        jQuery('#saveManagedFile').find('.hasKetchup').unbind();
        jQuery('#saveManagedFile').find('.hasKetchup').removeClass('hasKetchup');
        jQuery('#saveManagedFile').ketchup({positionContainer:positionContainerInDialog, initialPositionContainer:positionContainerInDialog});
        resetDialogContents(event, ui);
    };

    // Callback when the dialog is closed.  Resets ketchup back to the defaults.
    var dialogClose = function(event, ui) {
        jQuery('#searchForm').ketchup();
    };

    // Clears form fields and search results when the dialog is opened.
    var resetDialogContents = function(event, ui) {
        selectedUuid = "";
        jQuery('#saveManagedFile').resetForm();
        jQuery('#saveManagedFile .ketchup-error-container').hide();

        jQuery('#searchForm').resetForm();
        jQuery('#selectionError').hide();
        jQuery('#managedFilesList').jqGrid().clearGridData();

    };

    // Don't allow the form to be submitted as we need to do it via ajax, but we need to trigger the submit
    // function to make ketchup validate.
    jQuery('#saveManagedFile').submit(function() {
        return false;
    });

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


</script>