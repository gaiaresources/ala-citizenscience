<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<c:choose>
    <c:when test="${ geoMapLayer.id == null }">
        <h1>Add Map Layer</h1>
    </c:when>
    <c:otherwise>
        <h1>Edit Map Layer</h1>
    </c:otherwise>
</c:choose>

<cw:getContent key="admin/map/editMapLayer" />

<div class="input_container">
	<form id="mapLayerForm" action="${portalContextPath}/bdrs/admin/mapLayer/edit.htm" method="POST">
	    <c:if test="${ geoMapLayer.id != null }">
	    <input type="hidden" name="geoMapLayerPk" value="${geoMapLayer.id}" />
	    </c:if>
	    <c:if test="${ geoMapLayer.id == null }">
	    <input type="hidden" name="geoMapLayerPk" value="0" />
	    </c:if>
	    <table>
	    	<tbody>
	    		<tr title="The name of the map layer">
		            <th><label for="layerName">Map Layer Name:</label></th>
		            <td><input id="layerName" class="validate(required,maxlength(255)) long_input" type="text" name="name" value="<c:out value="${geoMapLayer.name}" />" size="40"  autocomplete="off"></td>
		        </tr>
		        <tr title="The description of the map, up to 1023 characters long">
		            <th><label for="layerDescription">Map Layer Description:</label></th>
		            <td><input id="layerDescription" class="validate(required,maxlength(1023)) long_input" type="text" name="desc" value="<c:out value="${geoMapLayer.description}" />" size="40"  autocomplete="off"></td>
		        </tr>
				<!-- hiding unused fields. consider removal when we have some time -->
		        <tr title="has no effect" style="display:none">
		            <td>Publish:</td>
		            <td><input type="checkbox" value="on" name="publish" <c:if test="${geoMapLayer.publish}">checked="checked"</c:if> /></td>
		        </tr>
				<!-- hiding unused fields. consider removal when we have some time-->
		        <tr title="has no effect" style="display:none">
		            <td>Hide Private Details:</td>
		            <td><input type="checkbox" value="on" name="hidePrivateDetails"	<c:if test="${geoMapLayer.hidePrivateDetails}">checked="checked"</c:if> /></td>
		        </tr>
		        <tr>
		            <th>Source data for layer</th>
		            <td>
		                <!-- survey is the default selection -->
						<span title="Generate a KML layer from a project">
		                    <input id="radio_survey_kml" type="radio" class="dataSourceSelector" name="layerSrc" value="SURVEY_KML" <c:if test="${ geoMapLayer.layerSource == \"SURVEY_KML\" }">checked="checked"</c:if> <c:if test="${ geoMapLayer.id == null }">checked="checked"</c:if> /><label for="radio_survey_kml">Project via KML</label><br />
						</span>
						<span title="Generate a WMS layer from a project">
		                    <input id="radio_survey_mapserver" type="radio" class="dataSourceSelector" name="layerSrc" value="SURVEY_MAPSERVER" <c:if test="${ geoMapLayer.layerSource == \"SURVEY_MAPSERVER\" }">checked="checked"</c:if> <c:if test="${ geoMapLayer.id == null }">checked="checked"</c:if> /><label for="radio_survey_mapserver">Project via MapServer (requires Mapserver installed)</label><br />
						</span>
						<span title="Generate a WMS layer from an uploaded shapefile">
		                    <input id="radio_shapefile" type="radio" class="dataSourceSelector" name="layerSrc" value="SHAPEFILE" <c:if test="${ geoMapLayer.layerSource == \"SHAPEFILE\" }">checked="checked"</c:if> /><label for="radio_shapefile">Shapefile (requires Mapserver installed)</label><br />
					    </span>
						<span title="Load a KML file from the managed file system">
		                    <input id="radio_kml" type="radio" class="dataSourceSelector" name="layerSrc" value="KML" <c:if test="${ geoMapLayer.layerSource == \"KML\" }">checked="checked"</c:if> /><label for="radio_kml">KML File</label><br />
						</span>
						<span title="Load tiles from a WMS server">
							<input id="radio_wms_server" type="radio" class="dataSourceSelector" name="layerSrc" value="WMS_SERVER" <c:if test="${ geoMapLayer.layerSource == \"WMS_SERVER\" }">checked="checked"</c:if> /><label for="radio_wms_server">WMS Server</label>
						</span>
		            </td>
		        </tr>
				<tr id="urlRow" title="URL to retrieve map data.">
					<th><label for="serverUrl">Server URL:</label></th>
					<td><input class="long_input" id="serverUrl" size="40" type="text" name="serverUrl" autocomplete="off" value="<c:out value="${geoMapLayer.serverUrl}" />" />
					</td>
				</tr>
		        <tr id="managedFileRow" title="A managed file UUID. Depending on the layer source it may be a KML file or a shapefile.">
		            <th><label for="fileId">File Identifier (UUID):</label></th>
		            <td><input id="fileId" type="text" class="long_input" name="mfuuid" value="<c:out value="${geoMapLayer.managedFileUUID}" />" size="40" autocomplete="off" onchange="setWriteFileToDatabase();" />
					     <div>Upload and browse your files in the <a href="${portalContextPath}/bdrs/user/managedfile/listing.htm" target="_blank">managed file interface. (Opens in new window)</a></div>
					     <div id="shapefile_instructions">The uploaded file must be a zipped up shapefile containing a minimum of the .shp, .dbf, .shx and .prj files.</div>
					</td>
		        </tr>
				<tr id="writeToDatabaseRow" title="If the layer source is a shape file, checking this box will cause a database overwrite of stored map features">
					<th><label for="writeToDatabase">Write file to Database:</label></th>
					<td><input id="writeToDatabase" type="checkbox" name="shpToDatabase" /></td>
				</tr>
		        <tr id="surveyRow" title="Select the project data to create map layer with" >
		            <th><label for="surveyId">Project:</label></th>
		            <td>
		                <select id="surveyId" name="surveyPk">
		                <c:forEach items="${surveyList}" var="survey">
		                    <option value="${survey.id}" 
		                    <c:if test="${survey.id == geoMapLayer.survey.id}">selected="selected"</c:if>
		                    ><c:out value="${survey.name}"/></option>
		                </c:forEach>
		                </select>
		            </td>
		        </tr>
				<tr id="strokeColorRow" title="Stroke colour parameter for map styling">
					<th><label for="strokeColor">Stroke Colour:</label></t>
					<td><div><input class="validate(required,color)" id="strokeColor" type="text" name="strokeColor" value="<c:out value="${geoMapLayer.strokeColor}" />" /></div></td>
				</tr>
				<tr id="fillColorRow" title="Fill color parameter for map styling">
					<th><label for="fillColor">Fill Colour:</label></t>
		            <td><input class="validate(required,color)" id="fillColor" type="text" name="fillColor" value="<c:out value="${geoMapLayer.fillColor}" />" /></td>
				</tr>
				<tr id="symbolSizeRow" title="Symbol size parameter for map styling">
		            <th><label for="symbolSize">Symbol Size (only effects point data):</label></t>
		            <td><input class="validate(required)" id="symbolSize" type="text" name="symbolSize" value="<c:out value="${geoMapLayer.symbolSize}" />" /></td>
		        </tr>
		        <tr id="strokeWidthRow" title="Stroke width parameter for map styling">
		            <th><label for="strokeWidth">Stroke Width:</label></th>
		            <td><input class="validate(required)" id="strokeWidth" type="text" name="strokeWidth" value="<c:out value="${geoMapLayer.strokeWidth}" />" /></td>
		        </tr>
			</tbody>
	    </table>
	
	    <div class="buttonpanel textright">
	        <input onclick="onSubmit();" class="form_action" type="button" value="Save" />
	     </div>
	</form>
</div>

<script type="text/javascript">

    jQuery(function() {

		bdrs.util.createColorPicker(jQuery('#strokeColor'));
		bdrs.util.createColorPicker(jQuery('#fillColor'));
		
        jQuery('.dataSourceSelector').change(function() {
            var selected = jQuery('input[name=layerSrc]:checked', '#mapLayerForm').val();
            setDataSource(selected);
        });
        
        // trigger the change event to do initialisation...
        jQuery('.dataSourceSelector').change();
    });
	
	var setWriteFileToDatabase = function() {
		jQuery('#writeToDatabase').attr("checked", "checked");
	};
	
	var onSubmit = function() {
		  if (jQuery('#radio_shapefile').attr('checked') && !jQuery('#writeToDatabase').attr('checked')) {
                bdrs.util.confirmExec("You have selected shapefile and a file but have not chosen to write the file to the database. Write the file to the database?\n\nNote: All other settings will still be saved!", setWriteFileToDatabase);
		  }
		  if (jQuery('#radio_shapefile').attr('checked') && jQuery('#writeToDatabase').attr('checked')) {
                var uuid = jQuery('#fileId').val();
				$.ajax("${portalContextPath}/bdrs/map/checkShapefile.htm", {
				    data: {
						mfuuid: uuid
					},
					success: function(json) {
					   if (json.status == "error") {
					   	   var msg = json.message.join("\n\n");
					       bdrs.message.set("Error with file: " + msg);
						   $.unblockUI();
					   } else if (json.status == "warn") {
					   	  var msg = json.message.join("\n\n");
						
						  if (bdrs.util.confirm(msg + "\n\nDo you wish to continue?")) {
						  	jQuery('#mapLayerForm').submit();
							$.blockUI({ message: '<h1>Writing shapefile to database...</h1>' });
						  } else {
						  	$.unblockUI();
						  }
					   } else if (json.status == "ok") {
					   	  jQuery('#mapLayerForm').submit();
						  $.blockUI({ message: '<h1>Writing shapefile to database...</h1>' });
					   } else {
					   	  $.unblockUI();
					   }
					},
					error: function() {
						bdrs.message.set("Error when trying to run the file check webservice");
						$.unblockUI();
					},
					beforeSend: function() {
						$.blockUI({ message: '<h1>Just a moment, checking shapefile...</h1>' });
					}
				});
          } else {
		      jQuery('#mapLayerForm').submit();
		  }
	};
	
	var enableRow = function(selector) {
		var row = jQuery(selector);
		row.find('input,select').prop('disabled', false);
		//row.show();
		//row.find(".ketchup-error-container").show();
	};
	
	var disableRow = function(selector) {
		var row = jQuery(selector);
		row.find('input,select').prop('disabled', true);
		//row.hide();
		//row.find(".ketchup-error-container").hide();
	};
    
    var setDataSource = function(src) {
        if (src === 'KML') {
            jQuery('#fileId').addClass("validate(required,maxlength(255),uuid)");
			jQuery('#shapefile_instructions').hide();
			jQuery('#serverUrl').removeClass("validate(required,url)");
			
			enableRow('#managedFileRow');
			disableRow('#writeToDatabaseRow');
			disableRow('#surveyRow');
			enableRow('#strokeColorRow');
			enableRow('#fillColorRow');
			enableRow('#symbolSizeRow');
			enableRow('#strokeWidthRow');
			disableRow('#urlRow');
			
        } else if (src === 'SHAPEFILE') {
            jQuery('#fileId').addClass("validate(required,maxlength(255),uuid)");
			jQuery('#shapefile_instructions').show();
			jQuery('#serverUrl').removeClass("validate(required,url)");
			
			enableRow('#managedFileRow');
			enableRow('#writeToDatabaseRow');
			disableRow('#surveyRow');
			enableRow('#strokeColorRow');
			enableRow('#fillColorRow');
			enableRow('#symbolSizeRow');
			enableRow('#strokeWidthRow');
			disableRow('#urlRow');
			
		} else if (src === 'SURVEY_KML' || src === 'SURVEY_MAPSERVER') {
            jQuery('#fileId').removeClass("validate(required,maxlength(255),uuid)");
			jQuery('#shapefile_instructions').hide();
			jQuery('#serverUrl').removeClass("validate(required,url)");
			
			disableRow('#managedFileRow');
			disableRow('#writeToDatabaseRow');
			enableRow('#surveyRow');
			enableRow('#strokeColorRow');
			enableRow('#fillColorRow');
			enableRow('#symbolSizeRow');
			enableRow('#strokeWidthRow');
			disableRow('#urlRow');
			
        } else if (src === 'WMS_SERVER') {
            jQuery('#fileId').removeClass("validate(required,maxlength(255),uuid)");
			jQuery('#shapefile_instructions').hide();
			jQuery('#serverUrl').addClass("validate(required,url)");
			
			disableRow('#managedFileRow');
			disableRow('#writeToDatabaseRow');
			disableRow('#surveyRow');
			disableRow('#strokeColorRow');
			disableRow('#fillColorRow');
			disableRow('#symbolSizeRow');
			disableRow('#strokeWidthRow');
			enableRow('#urlRow');
		}
		
        // do rebinding...
        jQuery('form').ketchup();
    };
</script>
