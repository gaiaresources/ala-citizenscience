<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<%@page import="au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType"%>
<%@page import="au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction"%>
<%@page import="au.com.gaiaresources.bdrs.model.metadata.Metadata"%>
<%@page import="au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem"%>
<jsp:useBean id="survey" type="au.com.gaiaresources.bdrs.model.survey.Survey" scope="request"/>
<c:choose>
    <c:when test="${survey.id == null }">
        <h1>Add Project</h1>
    </c:when>
    <c:otherwise>
        <h1>Edit Project: <c:out value="${survey.name}"/></h1>
    </c:otherwise>
</c:choose>

<cw:getContent key="admin/editProject" />

<c:choose>
    <c:when test="${survey.id != null }">
        <div class="textright">
            <a href="${portalContextPath}/bdrs/admin/survey/export.htm?surveyId=${ survey.id }"
                title="Exports Projects, Census Methods and Attributes">
                Export Project
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="buttonpanel sepBottom">
            <form method="POST" action="${portalContextPath}/bdrs/admin/survey/import.htm" enctype="multipart/form-data">
                <input id="import_survey_file" name="survey_file" type="file" style="visibility:hidden"/>
                <input id="import_survey_button" class="form_action right" type="button" value="Import Project"/>
            </form>
            <div class="clear"></div>
        </div>
    </c:otherwise>
</c:choose>

<form method="POST" action="${portalContextPath}/bdrs/admin/survey/edit.htm" enctype="multipart/form-data">
    <c:if test="${survey.id != null }">
        <input type="hidden" name="surveyId" value="${survey.id}"/>
    </c:if>
    <div class="input_container">
        <table class="form_table surveyOptionTable">
            <tbody>
                <tr>
                    <th>Name:</th>
                    <td>
                        <input type="text" name="name" class="validate(required)"
                            maxlength="255"
                            value="<c:out value="${survey.name}" default=""/>"
                        />
                    </td>
                </tr>
                <tr>
                    <th>Description:</th>
                    <td>
                        <textarea name="description" class="validate(maxlength(1023))"><c:out value="${survey.description}"/></textarea>
                    </td>
                </tr>
                <tr>
                    <th>Project Start Date:</th>
                    <td>
                        <input type="text" class="datepicker_range validate(date)" name="surveyDate" id="from"
                            <c:if test="${survey.startDate != null}">
                                value="<fmt:formatDate pattern="dd MMM yyyy" value="${survey.startDate}"/>"
                            </c:if>
                        />
                    </td>
                </tr>
                <tr>
                    <th>Project End Date:</th>
                    <td>
                        <input type="text" class="datepicker_range validate(dateOrBlank)" name="surveyEndDate" id="to"
                            <c:if test="${survey.endDate != null}">
                                value="<fmt:formatDate pattern="dd MMM yyyy" value="${survey.endDate}"/>"
                            </c:if>
                        />
                    </td>
                </tr>
                <%-- This is covered by the user access page
                <tr>
                    <th>Public:</th>
                    <td>
                        <input type="checkbox" name="publik"
                            <c:if test="${survey.public}">
                                checked="checked"
                            </c:if>
                        />
                    </td>
                </tr>
                --%>
                <tr>
                    <th>Project Logo:</th>
                    <td>
                        <c:set var="logo_md" value="<%= survey.getMetadataByKey(Metadata.SURVEY_LOGO) %>" scope="page"/>
                        <c:if test="${ logo_md != null }">
                            <jsp:useBean id="logo_md" type="au.com.gaiaresources.bdrs.model.metadata.Metadata" scope="page"/>
                            <div id="<%= Metadata.SURVEY_LOGO %>_img">
                                <a href="${portalContextPath}/files/download.htm?<%= logo_md.getFileURL() %>">
                                    <img width="250"
                                        src="${portalContextPath}/files/download.htm?<%= logo_md.getFileURL() %>"
                                        alt="Missing Image"/>
                                </a>
                            </div>
                        </c:if>
                        <div style="line-height: 0em;">
                            <input type="text"
                                id="<%= Metadata.SURVEY_LOGO %>"
                                name="<%= Metadata.SURVEY_LOGO %>"
                                style="visibility: hidden;height: 0em;"
                                <c:if test="${logo_md != null}">
                                    value="<c:out value="${logo_md.value}"/>"
                                </c:if>
                            />
                        </div>
                        <input type="file"
                            accept="image/gif,image/jpeg,image/png"
                            id="<%= Metadata.SURVEY_LOGO %>_file"
                            name="<%= Metadata.SURVEY_LOGO %>_file"
                            class="image_file"
                            onchange="bdrs.util.file.imageFileUploadChangeHandler(this);jQuery('#<%= Metadata.SURVEY_LOGO %>').val(jQuery(this).val());"
                        />
                        <a href="javascript: void(0)"
                            onclick="jQuery('#<%= Metadata.SURVEY_LOGO %>, #<%= Metadata.SURVEY_LOGO %>_file').attr('value','');jQuery('#<%= Metadata.SURVEY_LOGO %>_img').remove();">
                            Clear
                        </a>                    
                    </td>
                </tr>
                <tr>
                    <th title="Is the survey accessible for read access">Publish:</th>
                    <td>
                        <input type="checkbox" name="active"
                            <c:if test="${survey.active}">
                                checked="checked"
                            </c:if>
                        />
                    </td>
                </tr>
            </tbody>
        </table>
		
		<div class="right">
        <a id="advancedToggle" href="javascript: void(0);">Click here to see the advanced settings for your project</a>
        </div>
		<div class="clear"></div>
		
		<div id="advancedSettings" style="display:none">
			<table class="form_table surveyOptionTable">
				<tbody>
					<tr>
	                    <th>Form Type:</th>
	                    <td>
	                        <fieldset>
	                            <c:forEach items="<%=SurveyFormRendererType.values()%>" var="rendererType">
	                                <jsp:useBean id="rendererType" type="au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType" />
	                                <div title="<%= rendererType.getDescription() %>">
	                                    <input onchange="editSurveyPage.rendererTypeChanged(jQuery(this));" type="radio" class="vertmiddle" name="rendererType"
	                                        id="<%= rendererType.toString() %>"
	                                        value="<%= rendererType.toString() %>"
	                                        <c:if test="<%= rendererType.equals(survey.getFormRendererType()) || (survey.getFormRendererType() == null && SurveyFormRendererType.DEFAULT.equals(rendererType)) %>">
	                                            checked="checked"
	                                        </c:if>
	                                        <c:if test="<%= !rendererType.isEligible(survey) %>">
	                                            disabled="disabled"
	                                        </c:if>
	                                    />
	                                    <label for="<%= rendererType.toString() %>">
	                                        <%= rendererType.getName() %>
	                                    </label>
	                                </div>
	                            </c:forEach>

	                            <c:forEach items="${ customforms }" var="customform">
	                                <jsp:useBean id="customform" type="au.com.gaiaresources.bdrs.model.form.CustomForm" />
                                    <div title="${ customform.description }">
                                        <input onchange="editSurveyPage.rendererTypeChanged(jQuery(this));"
                                                id="customform_${ customform.id }"
                                                type="radio"
                                                class="vertmiddle"
                                                name="rendererType"
                                                value="${ customform.id }"
                                                <c:if test="<%= customform.equals(survey.getCustomForm()) %>">
                                                    checked="checked"
                                                </c:if>
                                        />
                                        <label for="customform_${ customform.id }">${ customform.name }</label>
                                    </div>
                                </c:forEach>
                               </fieldset>
                        </td>
                    </tr>
                    <tr>
                       <th>CSS Layout File:</th>
                       <td>
                        <c:set var="css_md" value="<%= survey.getMetadataByKey(Metadata.SURVEY_CSS) %>" scope="page"/>
                        <c:if test="${ css_md != null }">
                            <jsp:useBean id="css_md" type="au.com.gaiaresources.bdrs.model.metadata.Metadata" scope="page"/>
                            <div id="<%= Metadata.SURVEY_CSS %>_css">
                                <a href="${portalContextPath}/files/download.htm?<%= css_md.getFileURL() %>">
                                    <%-- filename here --%>
                                    ${ css_md.value }
                                </a>
                            </div>
                        </c:if>
                        <div style="line-height: 0em;">
                            <input type="text"
                                id="<%= Metadata.SURVEY_CSS %>"
                                name="<%= Metadata.SURVEY_CSS %>"
                                style="visibility: hidden;height: 0em;"
                                <c:if test="${css_md != null}">
                                    value="<c:out value="${css_md.value}"/>"
                                </c:if>
                            />
                        </div>
                        <input type="file"
                            accept="text/css"
                            id="<%= Metadata.SURVEY_CSS %>_file"
                            name="<%= Metadata.SURVEY_CSS %>_file"
                            onchange="jQuery('#<%= Metadata.SURVEY_CSS %>').val(jQuery(this).val());jQuery('#<%= Metadata.SURVEY_CSS %>_css').remove();"
                        />
                        <div>
                        <a href="javascript: void(0)"
                            onclick="jQuery('#<%= Metadata.SURVEY_CSS %>, #<%= Metadata.SURVEY_CSS %>_file').attr('value','');jQuery('#<%= Metadata.SURVEY_CSS %>_css').remove();">
                            Clear
                        </a>
                        <c:if test="${ survey.id != null }">
                            <a href="javascript:editCssLayoutFile()" class="cssLayoutFileActionLink">Edit</a>
                        </c:if>
                        </div>
                    </td>
                    </tr>
                    <tr>
                        <th title="The action the website will take after clicking the 'Submit' button on the recording form">Submit Action:</th>
                        <td>
                            <fieldset>
                                <c:forEach items="<%=SurveyFormSubmitAction.values()%>" var="formAction">
                                    <jsp:useBean id="formAction" type="au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction" />
                                    <div>
                                        <input type="radio" class="vertmiddle" name="formSubmitAction"
                                            id="<%= formAction.toString() %>"
	                                        value="<%= formAction.toString() %>"
	                                        <c:if test="<%= formAction.equals(survey.getFormSubmitAction()) %>">
	                                            checked="checked"
	                                        </c:if>
	                                    />
	                                    <label for="<%= formAction.toString() %>">
	                                        <%= formAction.getName() %>
	                                    </label>
	                                </div>
	                            </c:forEach>
	                        </fieldset>
	                    </td>
	                </tr>
					<tr>
	                    <th title="The default visibility setting when creating a new record">Default Record Visibility:</th>
	                    <td>
	                        <fieldset>
	                            <c:forEach items="<%=au.com.gaiaresources.bdrs.model.record.RecordVisibility.values()%>" var="recVis">
	                                <jsp:useBean id="recVis" type="au.com.gaiaresources.bdrs.model.record.RecordVisibility" />
	                                <div>
	                                    <input type="radio" class="vertmiddle" name="defaultRecordVis"
	                                        id="<%= recVis.toString() %>"
	                                        value="<%= recVis.toString() %>"
	                                        <c:if test="<%= recVis.equals(survey.getDefaultRecordVisibility()) %>">
	                                            checked="checked"
	                                        </c:if>
	                                    />
	                                    <label for="<%= recVis.toString() %>">
	                                        <%= recVis.getDescription() %>
	                                    </label>
	                                </div>
	                            </c:forEach>
	                        </fieldset>
	                    </td>
	                </tr>
					<tr>
	                    <th title="Whether non-admins can change the visibility of their records">Record visibility modifiable by users:</th>
	                    <td>
	                        <input type="checkbox" name="recordVisModifiable" value="true"
	                            <c:if test="${survey.recordVisibilityModifiable}">
	                                checked="checked"
	                            </c:if>
	                        />
	                    </td>
	                </tr>
                    <tr>
                        <th title="Controls if users can add comments to records created using this survey">Comments on records allowed:</th>
                        <td>
                            <input type="checkbox" name="recordCommentsEnabled" value="True"
                                <c:if test="${survey.recordCommentsEnabled}">
                                    checked="checked"
                                </c:if>
                            />
                        </td>
                    </tr>
                    <tr>
                        <th title="The coordinate reference system to use for this project.">Coordinate Reference System:</th>
                        <td>
                            <select name="crs">
                                <c:forEach items="<%=BdrsCoordReferenceSystem.values()%>" var="crs">
                                    <%-- calls toString on the enum object --%>
                                    <option value="${ crs }"
                                        <c:if test="${ survey.map.crs == crs }">selected="selected"</c:if>
                                    ><c:out value="${ crs.displayName }"/></option>
                                </c:forEach>
                            </select>
                        </td>
                    </tr>
				</tbody>
			</table>
		</div>
    
        <div class="textright buttonpanel">
            <input type="submit" class="form_action" value="Save"/>
            <c:if test="${publish == false}">
                <input type="submit" class="form_action" name="saveAndContinue" value="Save And Continue"/>
            </c:if>
        </div>
    </div>
</form>

<script type="text/javascript">
    editSurveyPage = {
       crsSelector: "select[name=crs]",
       crsHiddenSelector: "input[name=crs]",
       
       rendererTypeChanged: function() {
    	   var crsElem = jQuery(editSurveyPage.crsSelector);
    	   crsElem.prop("disabled", false);
    	   // remove hidden elem if it exists.
    	   jQuery(editSurveyPage.crsHiddenSelector).remove();
           var value = $('input:radio[name=rendererType]:checked').val();
           var recordVisCheckBox = jQuery('input[name=recordVisModifiable]');
           var isRecordVisEnabled = false;
           if (value === 'DEFAULT') {
              isRecordVisEnabled = true;
           } else if (value === 'SINGLE_SITE_MULTI_TAXA') {
              isRecordVisEnabled = false;
           } else if (value === 'SINGLE_SITE_ALL_TAXA') {
              isRecordVisEnabled = false;
           } else if (value === 'ATLAS') {
              isRecordVisEnabled = false;
              // must use lon/lat
              crsElem.val("WGS84");
              crsElem.prop("disabled", true);
              var crsHiddenElem = jQuery('<input type="hidden" name="crs" value="WGS84" />');
              crsElem.after(crsHiddenElem);
           } else {
              // If it is a custom form then allow the record visibility to be modifiable.
              // It is the responsibility of the custom form to provide this facility.
              if(!isNaN(value, 10)) {
                  isRecordVisEnabled = true;
              } else {
                  // not expected value. default to disable the control.
                  isRecordVisEnabled = false;
              }
           }

           if(isRecordVisEnabled) {
               recordVisCheckBox.removeAttr("disabled");
           } else {
               recordVisCheckBox.attr('disabled', 'disabled');
           }
       }    
    };
    
    jQuery(function() {
        editSurveyPage.rendererTypeChanged();
		
		// Census method expand/collapse
        jQuery("#advancedToggle").click(function() {
            var canSee = jQuery("#advancedSettings").css('display') === 'none';
            jQuery("#advancedToggle").text(canSee ? "Click here to hide the advanced settings for your project" : "Click here to see the advanced settings for your project");
            jQuery("#advancedSettings").slideToggle();
        });

        bdrs.survey.listing.init();
    });
    
    <c:if test="${ survey.id != null }">
    var editCssLayoutFile = function() {
    	var fileNode = jQuery('#<%= Metadata.SURVEY_CSS %>');
        if (!fileNode.val()) {
            alert('You must save a file before editing');
        } else {
        	window.location.href = bdrs.portalContextPath + "/bdrs/admin/survey/editCssLayout.htm?surveyId=" + ${ survey.id };
        }
    };
    </c:if>
</script>
