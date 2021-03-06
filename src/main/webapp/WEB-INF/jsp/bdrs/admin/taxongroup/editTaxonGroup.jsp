<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<jsp:useBean id="taxonGroup" type="au.com.gaiaresources.bdrs.model.taxa.TaxonGroup" scope="request"/>

<c:choose>
    <c:when test="${ taxonGroup.id == null }">
        <h1>Add a New Taxonomic Group</h1>
    </c:when>
    <c:otherwise>
        <h1>Edit an Existing Taxonomic Group</h1>
    </c:otherwise>
</c:choose>

<cw:getContent key="admin/taxonomy/editTaxonomicGroups" />

<form method="POST" action="${portalContextPath}/bdrs/admin/taxongroup/edit.htm" enctype="multipart/form-data">
    <div class="textright buttonpanel">
        <input type="submit" class="form_action" value="Save"/>
    </div>
    <c:if test="${ taxonGroup.id != null }">
       <input id="taxonGroupPk" type="hidden" name="taxonGroupPk" value="${ taxonGroup.id }"/>
    </c:if>
    <div class="input_container">
	    <div class="left editTaxonGroupLeftColumn">
		    <table class="form_table">
		   	    <tbody>
		   	    	<tr>
		                <th><label for="name">Name</label></th>
		                <td><input id="name" class="validate(required)" type="text" name="name" value="${ taxonGroup.name }"/></td>
		            </tr>
		            <tr>
		                <th><label for="image">Image</label></th>
		                <td>
		                    <c:if test="${ taxonGroup.image != null }">
		                        <div id="img">
		                            <a href="${portalContextPath}/files/download.htm?<%= taxonGroup.getImageFileURL() %>">
		                                <img width="250"
		                                    src="${portalContextPath}/files/download.htm?<%= taxonGroup.getImageFileURL() %>"
		                                    alt="Missing Image"/>
		                            </a>
		                        </div>
		                    </c:if>
		                    <div style="line-height: 0em;">
		                        <input type="text"
		                            id="image"
		                            name="image"
		                            style="visibility: hidden;height: 0em;"
		                            <c:if test="${ taxonGroup.image != null}">
		                                value="<c:out value="${ taxonGroup.image }"/>"
		                            </c:if>
		                        />
		                    </div>
		                    <input type="file"
		                        accept="image/gif,image/jpeg,image/png"
		                        id="image_file"
		                        name="image_file"
		                        class="image_file"
		                        onchange="bdrs.util.file.imageFileUploadChangeHandler(this);jQuery('#image').val(jQuery(this).val());"
		                    />
		                    <script type="text/javascript">
		                        /**
		                         * This script creates a link that clears the image from the record
		                         * attribute. The link should only appear if javascript is enabled 
		                         * on the browser so we create an insert the script using javascript.
		                         * No script... no link.
		                         */
		                        jQuery(function() {
		                            var elem = jQuery("<a></a>");
		                            elem.attr({
		                                href: "javascript: void(0)",
		                            });
		                            elem.text('Clear');
		                            elem.click(function() {
		                                jQuery('#image, #image_file').attr('value','');
		                                jQuery('#img').remove();
		                            });
		                            jQuery('[name=image_file]').after(elem);
		                        });
		                    </script>
		                </td>
		            </tr>
		            <tr>
		                <th><label for="thumbNail">Thumbnail</label></th>
		                <td>
		                    <c:if test="${ taxonGroup.thumbNail != null }">
		                        <div id="thumb">
		                            <a href="${portalContextPath}/files/download.htm?<%= taxonGroup.getThumbnailFileURL() %>">
		                                <img width="250"
		                                    src="${portalContextPath}/files/download.htm?<%= taxonGroup.getThumbnailFileURL() %>"
		                                    alt="Missing Image"/>
		                            </a>
		                        </div>
		                    </c:if>
		                    <div style="line-height: 0em;">
		                        <input type="text"
		                            id="thumbNail"
		                            name="thumbNail"
		                            style="visibility: hidden;height: 0em;"
		                            <c:if test="${ taxonGroup.thumbNail != null}">
		                                value="<c:out value="${ taxonGroup.thumbNail }"/>"
		                            </c:if>
		                        />
		                    </div>
		                    <input type="file"
		                        accept="image/gif,image/jpeg,image/png"
		                        id="thumbNail_file"
		                        name="thumbNail_file"
		                        class="image_file"
		                        onchange="bdrs.util.file.imageFileUploadChangeHandler(this);jQuery('#thumbNail').val(jQuery(this).val());"
		                    />
		                    <script type="text/javascript">
		                        /**
		                         * This script creates a link that clears the image from the record
		                         * attribute. The link should only appear if javascript is enabled 
		                         * on the browser so we create an insert the script using javascript.
		                         * No script... no link.
		                         */
		                        jQuery(function() {
		                            var elem = jQuery("<a></a>");
		                            elem.attr({
		                                href: "javascript: void(0)",
		                            });
		                            elem.text('Clear');
		                            elem.click(function() {
		                                jQuery('#thumbNail, #thumbNail_file').attr('value','');
		                                jQuery('#thumb').remove();
		                            });
		                            jQuery('[name=thumbNail_file]').after(elem);
		                        });
		                    </script>
		                </td>
		            </tr>
		            <tr>
		                <th>&nbsp;</th>
		                <td>
		                    <span class="italics">Recommended size 250 x 140 px</span>
		                </td>
		            </tr>
		   	    </tbody>
			</table>
		</div>
	   
	    <div class="left editTaxonGroupRightColumn">
		    <table class="form_table">
		        <tbody>
		            <tr>
		                <th><label for="behaviourIncluded">Behaviour Included</label></th>
		                <td>
		                    <input id="behaviourIncluded" type="checkbox" name="behaviourIncluded" value="true"
		                        <c:if test="${ taxonGroup.behaviourIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		            <tr>
		                <th><label for="firstAppearanceIncluded">First Appearance Included</label></th>
		                <td>
		                    <input id="firstAppearanceIncluded" type="checkbox" name="firstAppearanceIncluded" value="true"
		                        <c:if test="${ taxonGroup.firstAppearanceIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		            <tr>
		                <th><label for="lastAppearanceIncluded">Last Appearance Included</label></th>
		                <td>
		                    <input id="lastAppearanceIncluded" type="checkbox" name="lastAppearanceIncluded" value="true"
		                        <c:if test="${ taxonGroup.lastAppearanceIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		            <tr>
		                <th><label for="habitatIncluded">Habitat Included</label></th>
		                <td>
		                    <input id="habitatIncluded" type="checkbox" name="habitatIncluded" value="true"
		                        <c:if test="${ taxonGroup.habitatIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		            <tr>
		                <th><label for="weatherIncluded">Weather Included</label></th>
		                <td>
		                    <input id="weatherIncluded" type="checkbox" name="weatherIncluded" value="true"
		                        <c:if test="${ taxonGroup.weatherIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		            <tr>
		                <th><label for="numberIncluded">Number Included</label></th>
		                <td>
		                    <input id="numberIncluded" type="checkbox" name="numberIncluded" value="true"
		                        <c:if test="${ taxonGroup.numberIncluded }">
		                            checked="checked"
		                        </c:if>
		                    />
		                </td>
		            </tr>
		        </tbody>
		    </table>
	    </div>
		<div class="clear"></div>
	</div>
    
    <h3>Attributes</h3>
    <p>
        Taxon Group Attributes are custom attributes for a sighting (record) that 
        is presented when a taxon from this group is selected. 
    </p>
    <div id="attributeContainer" class="input_container">
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#attributeContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
            <input type="button" class="form_action" value="Add Another Field" onclick="bdrs.attribute.addAttributeRow('#attribute_input_table', false, false, true)"/>
        </div>
    
        <table id="attribute_input_table" class="datatable attribute_input_table">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Description on the Form</th>
                    <th>Name in the Database</th>
                    <th>Field Type</th>
                    <th>Mandatory</th>
                    <th>Options (separated by comma)</th>
                    <th>Visibility</th>
                    <th>Delete</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${ attributeFormFieldList }" var="formField">
                    <tiles:insertDefinition name="attributeRow">
                        <tiles:putAttribute name="formField" value="${ formField }"/>
                        <tiles:putAttribute name="showScope" value="false"/>
                    </tiles:insertDefinition>
                </c:forEach>    
            </tbody>
        </table>
    </div>
    
    <h3>Identification</h3>
    <p>
        Identifications are customised attributes that are associated with each
        taxon from this group. For example a particular may contain the
        identification &#8220;Conservation Status&#8221;.
    </p>
    <div id="identificationContainer" class="input_container">
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#identificationContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
            <input type="button" class="form_action" value="Add Another Field" onclick="bdrs.attribute.addAttributeRow('#identification_input_table', false, true, false)"/>
        </div>
    
        <table id="identification_input_table" class="datatable attribute_input_table">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Description on the Form</th>
                    <th>Name in the Database</th>
                    <th>Field Type</th>
                    <th>Mandatory</th>
                    <th>Options (separated by comma)</th>
                    <th>Delete</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${ identificationFormFieldList }" var="formField">
                    <tiles:insertDefinition name="attributeRow">
                        <tiles:putAttribute name="formField" value="${ formField }"/>
                        <tiles:putAttribute name="showScope" value="false"/>
                        <tiles:putAttribute name="hideVisibilityColumn" value="true"/>
                    </tiles:insertDefinition>
                </c:forEach>    
            </tbody>
        </table>
    </div>
    
    <div class="textright buttonpanel">
       <input type="submit" class="form_action" value="Save"/>
    </div>
</form>

<div id="htmlEditorDialog" title="HTML Editor">
    <label>Edit the HTML content that you want to display in the editor below: </label>
    <textarea id="markItUp"></textarea>
</div>

<div id="censusMethodAttributeDialog" title="Add/Edit Census Method Attribute">
    <tiles:insertDefinition name="editCensusMethodAttribute">
    </tiles:insertDefinition>
</div>

<div>
    <h3>Group members</h3>
    The following table displays the taxa that have this Taxon Group as their primary group.
    <div id="taxonGroupMembers" class="input_container">

        <div id="searchTaxa">
          <label for="search_in_result" class="heading">Search group members:</label>
          <input name="search_in_result" id="search_in_result" type="text"/>
          <input id="search_in_result_button" class="form_action" type="button" value="Search">
        </div>

        <div id="bulkTaxaActions" class="buttonpanel">
          <span class="heading">Bulk change selected taxa:</span>

          <label for="groupAction">Action:</label>
          <select id="groupAction">
            <option value="updatePrimaryGroup">Reassign primary group</option>
            <option value="addSecondaryGroup">Assign secondary group</option>
            <option value="removeSecondaryGroup">Unassign secondary group</option>

          </select>
          <label for="actionGroup" style="margin-left: 1em;">Group:</label>  <input id="actionGroup" class="" type="text" name="taxonGroup" value=""/>
          <input id="actionGroupId" class="hiddenTextField validate(required)" type="text" name="taxonGroupPk" value=""/>
          <input type="button" id="performGroupAction" class="form_action" value="Assign Groups"/>

        </div>
    <div>
      <table id="taxaList"></table>
      <div id="pager2"></div>
    </div>
</div>

<script type="text/javascript">
    jQuery(function() {
        bdrs.dnd.attachTableDnD('#attribute_input_table');
        bdrs.dnd.attachTableDnD('#identification_input_table');
        
        jQuery( "#htmlEditorDialog" ).dialog({
            width: 'auto',
            modal: true,
            autoOpen: false,
            zIndex: bdrs.MODAL_DIALOG_Z_INDEX,
            resizable: false,
            buttons: {
                Cancel: function() {
                    jQuery( this ).dialog( "close" );
                },
                "Clear": function() {
                    jQuery('#markItUp')[0].value = "";
                },
                "OK": function() {
                    bdrs.attribute.saveAndUpdateContent(jQuery("#markItUp")[0]);
                    jQuery( this ).dialog( "close" );
                }
            }
        });
        bdrs.fixJqDialog("#htmlEditorDialog");
        jQuery('#markItUp').markItUp(bdrs.admin.myHtmlSettings);

        bdrs.taxonomy.initEditTaxonGroupMembers('${portalContextPath}', '${taxonGroup.id}');

    });


</script>
