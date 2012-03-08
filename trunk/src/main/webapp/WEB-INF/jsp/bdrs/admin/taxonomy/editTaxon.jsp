<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<%@page import="au.com.gaiaresources.bdrs.model.taxa.TaxonRank"%>

<jsp:useBean id="taxon" type="au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies" scope="request"/>
<jsp:useBean id="taxonProfileList" type="java.util.List" scope="request"/>
<jsp:useBean id="formFieldList" type="java.util.List" scope="request"/>

<c:choose>
    <c:when test="${ taxon.id == null }">
        <h1>Add a New Taxon</h1>
    </c:when>
    <c:otherwise>
        <h1>Edit an Existing Taxon</h1>
    </c:otherwise>
</c:choose>

<cw:getContent key="admin/taxonomy/editTaxonomy" />
<form id="speciesForm" method="POST" action="${pageContext.request.contextPath}/bdrs/admin/taxonomy/edit.htm" enctype="multipart/form-data">
    <div class="textright buttonpanel">
        <input type="submit" class="form_action" value="Save"/>
    </div>

    <c:if test="${ taxon.id != null }">
       <input id="taxonPk" type="hidden" name="taxonPk" value="${ taxon.id }"/>
   </c:if>
   <div class="input_container">
        <table class="form_table taxon_edit_form_table">
            <tbody>
                <tr>
                    <th><label for="scientificName">Scientific Name</label></th>
                    <td><input class="validate(required)" type="text" name="scientificName" value="${ taxon.scientificName }"/></td>
                </tr>
                <tr>
                    <th><label for="commonName">Common Name</label></th>
                    <td><input class="validate(required)" type="text" name="commonName" value="${ taxon.commonName }"/></td>
                </tr>
                <tr>
                    <th><label for="rank">Rank</label></th>
                    <td>
                        <select name="taxonRank">
                            <c:forEach var="rankEnum" items="<%= TaxonRank.values() %>">
                                <jsp:useBean id="rankEnum" type="au.com.gaiaresources.bdrs.model.taxa.TaxonRank"/>
                                <option value="<%= rankEnum.toString() %>"
                                    <c:if test="<%= (taxon.getTaxonRank() != null && rankEnum.equals(taxon.getTaxonRank())) || (taxon.getTaxonRank() == null && TaxonRank.SPECIES.equals(rankEnum)) %>">
                                        selected="selected"
                                    </c:if>
                                >
                                    <%= rankEnum.getIdentifier() %>
                                </option>
                            </c:forEach>
                        </select>
                    </td>
                </tr>
                <tr>
                    <th><label for="parent">Parent</label></th>
                    <td>
                        <input id="parent" type="text" name="parent" value="${ taxon.parent.scientificName }"/>
                        <input id="parentPk" type="text" name="parentPk" value="${ taxon.parent.id }" style="visibility:hidden;width:1px;padding:2px 0 0 0;margin:0 0 0 -4px;border-width:0;"/>
                    </td>
                </tr>
                <tr>
                    <th><label for="taxonGroup">Group</label></th>
                    <td>
                        <input id="taxonGroup" class="" type="text" name="taxonGroup" value="${ taxon.taxonGroup.name }"/>
                        <input id="taxonGroupPk" class="validate(required)" type="text" name="taxonGroupPk" value="${ taxon.taxonGroup.id }" style="visibility:hidden;width:1px;padding:2px 0 0 0;margin:0 0 0 -4px;border-width:0;"/>
                    </td>
                </tr>
                <tr>
                    <th><label for="author">Author</label></th>
                    <td><input class="" type="text" name="author" value="${ taxon.author }"/></td>
                </tr>
                <tr>
                    <th><label for="year">Year</label></th>
                    <td><input class="" type="text" name="year" value="${ taxon.year }"/></td>
                </tr>
                <tr>
                    <th><label for="guid">Guid</label></th>
                    <td><input class="" type="text" name="guid" id="guid" value="${ taxon.sourceId }"/></td>
                </tr>
            </tbody>
        </table>
    </div>
    <h3>Taxon Profile</h3>
    <p>
       The taxon profile provides additional data about this taxon such as
       distinctive markings, identifying characteristics, habitat and biology. 
    </p>
    <div id="editTaxonomyContainer" class="input_container">
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#editTaxonomyContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
           <input type="button" class="form_action" value="Retrieve Profile from ALA" onclick="importALAProfile();"/>
           <input type="button" class="form_action" value="Add Profile" onclick="bdrs.taxonomy.addNewProfile('#newProfileIndex', '#taxonProfileTable');"/>
        </div>
        <table id="taxonProfileTable" class="datatable textcenter">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Type</th>
                    <th>Database Name</th>
                    <th>Title</th>
                    <th>Content</th>
                    <th>Delete</th>
                </tr>
            </thead>
            <tbody>
                <tiles:insertDefinition name="profileTableBody">
                    <tiles:putAttribute name="taxonProfileList" value="${ taxonProfileList }"/>
                    <tiles:putAttribute name="newProfileIndex" value="${ newProfileIndex }"/>
                </tiles:insertDefinition>
            </tbody>
        </table>
       <input id="newProfileIndex" type="hidden" value="<%= taxonProfileList.size() + 1 %>"/>
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#editTaxonomyContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
            <input type="button" class="form_action" value="Retrieve Profile from ALA" onclick="importALAProfile();"/>
            <input type="button" class="form_action" value="Add Profile" onclick="bdrs.taxonomy.addNewProfile('#newProfileIndex', '#taxonProfileTable');"/>
        </div>
    </div>
    <h3>Group Attributes</h3>
    <p>
        Group attributes are the custom attributes of this taxon as specified
        by this taxon group.
    </p>
    <div id="taxonAttributeTable"
        <c:if test="<%= formFieldList.size() > 0 %>">class="input_container"</c:if>
    >
        <tiles:insertDefinition name="taxonAttributeTable">
            <tiles:putAttribute name="formFieldList" value="${ formFieldList }"/>
        </tiles:insertDefinition>
    </div>
    
    <div class="textright buttonpanel">
       <input type="submit" class="form_action" value="Save"/>
    </div>
</form>


<jsp:include page="/WEB-INF/jsp/bdrs/admin/taxonomy/editTaxonDialogs.jsp"/>

<cw:speciesProfileType/>
<script type="text/javascript">
    jQuery(function() {
        bdrs.taxonomy.initEditTaxon("#parent","#parentPk", 
                                    "#taxonGroup", "#taxonGroupPk",
                                    "#taxonPk", "#taxonAttributeTable", 
                                    "#taxonProfileTable", "#newProfileIndex");
        // Render the html editor using a jquery dialog.
        $( "#htmlEditorDialog" ).dialog({
            width: 'auto',
            modal: true,
            autoOpen: false,
            buttons: {
                Cancel: function() {
                	bdrs.attribute.closeHtmlEditor($(this));
                },
                "Clear": function() {
                    $('#markItUp')[0].value = "";
                },
                "OK": function() {
                	bdrs.attribute.saveAndUpdateContent(jQuery("#markItUp")[0]);
                    bdrs.attribute.closeHtmlEditor(jQuery(this));
                }
            }
        });

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
                    jQuery(contentFieldBeingEdited).attr("value", selectedUuid);
                    closeAndUpdateRowPosition(this, "#selectPreferred");
                }
                else {
                    jQuery('#selectionError').show();
                }
            }
            else {

                if (isAddManagedFileFormValid()) {

                    var options = {
                        success: function(data){jQuery(contentFieldBeingEdited).attr("value", data.data.uuid);},
                        dataType : 'json'
                    };
                    jQuery("#saveManagedFile").ajaxSubmit(options);
                    closeAndUpdateRowPosition(this, '#addNewPreferred');
                }
            }
        };

        // Attach the event handler for when the OK button is selected on the file selection dialog.
        jQuery('#attachFileDialog').dialog("option", "buttons")[1].click = fileSelectionDialogOkPressed;
       
        $('#markItUp').markItUp(bdrs.admin.myHtmlSettings);

        // Attach focus event handlers to the text fields in the Content column of the profile table.
        // New rows in the table have different values for the name attribute, hence the requirement for
        // two event handlers.
        jQuery('#taxonProfileTable').delegate('input[name^=profile_content_]', 'focus', 'profile_type_', contentFocusHandler);
        jQuery('#taxonProfileTable').delegate('input[name^=new_profile_content_]', 'focus', 'new_profile_type_', contentFocusHandler);
    });


    var importALAProfile = function() {
        var taxonPk = jQuery('#taxonPk').val();
        var guid = jQuery('#guid').val();

        if (!guid && !taxonPk) {
            alert("You must enter a GUID or be editing an existing species in order to use this feature.");
        } else {
            var answer = confirm("Are you sure? Existing entries imported from ALA will be replaced!")
            if (answer) {
                var url = '${pageContext.request.contextPath}/bdrs/admin/taxonomy/import.htm?pk='
                    + (taxonPk ? taxonPk : '') +
                    (guid ? '&guid='+guid : '');
                window.document.location = url;
            }
        }
    };

    var showHtmlEditor = function(textField) {
        bdrs.attribute.showHtmlEditor($('#htmlEditorDialog'), $('#markItUp')[0], textField);
    };

    /**
     * Tracks the text field that is being updated via the #attachFileDialog dialog. This is so it's
     * value can be updated when the dialog is closed.
     */
    var contentFieldBeingEdited;

    /**
     * Responds to focus events fired by the text field in the Content column of the Species profile table.
     * @param event event.data must contain the prefix used by the name attribute of the select element in the
     * same row as the text field that fired this event.
     */
    var contentFocusHandler = function(event) {

        var type = jQuery(this).parents('tr:first').find('select[name^='+event.data+']').attr("value");

        if (bdrs.taxonomy.speciesProfileType.isTextType(type)) {
            showHtmlEditor(this);
        }
        else if (bdrs.taxonomy.speciesProfileType.isFileType(type) || bdrs.taxonomy.speciesProfileType.isImageType(type)) {
            contentFieldBeingEdited = this;
            showFileSelector();
        }
    };

    var showFileSelector = function() {
        jQuery('#attachFileDialog').dialog('option', 'height', getHeight());
        jQuery( "#attachFileDialog" ).dialog('open');
    };


    
</script>