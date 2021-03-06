<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<jsp:useBean id="survey" scope="request" type="au.com.gaiaresources.bdrs.model.survey.Survey" />
<%@page import="au.com.gaiaresources.bdrs.model.taxa.Attribute"%>
<%@page import="au.com.gaiaresources.bdrs.model.taxa.AttributeScope"%>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="showScientificName" value="<%= bdrsPluginFacade.getPreferenceBooleanValue(\"taxon.showScientificName\") %>" />
<c:set var="hasRequiredTableCell" value="false"/>

<h1><c:out value="${survey.name}"/></h1>
<c:if test="${censusMethod != null}">
    <!-- using censusmethod description here in case we want to display no text / more indepth text -->
    <p><cw:validateHtml html="${censusMethod.description}"></cw:validateHtml></p>
</c:if>

<c:if test="${displayMap}">
    <tiles:insertDefinition name="recordEntryMap">
        <tiles:putAttribute name="survey" value="${survey}"/>
        <tiles:putAttribute name="censusMethod" value="${censusMethod}"/>
    </tiles:insertDefinition>
</c:if>

    <c:if test="${ not preview and recordWebFormContext.editable }">
        <form method="POST" action="${portalContextPath}/bdrs/user/singleSiteMultiTaxa.htm" enctype="multipart/form-data">
    </c:if>
    <input type="hidden" name="surveyId" value="${survey.id}"/>
            <%-- only include the wkt input if we are drawing lines or polygons 
           if the wkt key/value pair exists in the post dictionary, the lat/lon
           fields will be ignored as the wkt entry takes precedence.
        --%>
        <c:if test="<%= !survey.isPredefinedLocationsOnly() %>" >
           <c:if test="${censusMethod != null and (censusMethod.drawLineEnabled or censusMethod.drawPolygonEnabled)}">
             <input type="hidden" name="wkt" value="${wkt}" />
           </c:if>
        </c:if>
        
    <%-- the record form header contains the unlock form icon --%>
    <tiles:insertDefinition name="recordFormHeader">
        <tiles:putAttribute name="recordWebFormContext" value="${recordWebFormContext}" />
    </tiles:insertDefinition>
    
    <div id="tableContainer">
    
    <div id="surveyScopedFieldsContainer">
        <%-- Just a tile to help avoid DRY between SSMT and SSAT forms --%>
        <tiles:insertDefinition name="singleSiteSurveyScopeFields">
        </tiles:insertDefinition>
        <%-- Allows you to make this a clearing div at the end of the container --%>
        <div id="surveyScopedFieldsContainerEnd"></div>
	</div>
    
    <div id="sightingsContainer">
        
        <c:if test="${ not hideAddBtn and recordWebFormContext.editable and not recordWebFormContext.moderateOnly }">
            <!-- Add sightings description text -->
            <cw:getContent key="user/singleSiteMultiTaxaTable" />
            <div id="add_sighting_panel" class="buttonpanel textright">
                <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#sightingsContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
                <input type="hidden" id="sighting_index" name="sightingIndex" value="${fn:length(recordWebFormContext.namedCollections['recordFieldCollectionList'])}"/>
                <input class="form_action" type="button" value="Add Sighting" onclick="bdrs.contribute.singleSiteMultiTaxa.addSighting('#sighting_index', '[name=surveyId]', '#sightingTable tbody', false, false, ${showScientificName});"/>
            </div>
        </c:if>
        <table id="sightingTable" class="datatable">
            <thead>
               <tr>
                   <c:forEach items="${ recordWebFormContext.namedFormFields['sightingRowFormFieldList'] }" var="sightingRowFormField">
                       <jsp:useBean id="sightingRowFormField" type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
                       <c:if test="<%= sightingRowFormField.isPropertyFormField() %>">
                           <c:if test="${ sightingRowFormField.scope == 'RECORD' }">
                               <c:choose>
                                   <c:when test="${ not sightingRowFormField.hidden }">
                                    <th class="st_header_${ sightingRowFormField.name }">
                                       <c:out value="${ sightingRowFormField.description }" />
                                   </th>
                                   </c:when>
                               </c:choose>
                               <c:if test="${ not hasRequiredTableCell }">
                                   <c:set var="hasRequiredTableCell" value="<%= sightingRowFormField.isRequired() %>"/>
                               </c:if>
                           </c:if>
                       </c:if>
                       <c:if test="<%= sightingRowFormField.isAttributeFormField() %>">
                           <c:if test="${ sightingRowFormField.attribute.scope == 'RECORD' || sightingRowFormField.attribute.scope == 'RECORD_MODERATION' }"> 
                               <th class="st_header_${ sightingRowFormField.name }">
                               <c:out value="${ sightingRowFormField.attribute.description }" />
                               </th>
                               <c:if test="${ not hasRequiredTableCell }">
                                   <c:set var="hasRequiredTableCell" value="<%= sightingRowFormField.isRequired() %>"/>
                               </c:if>
                           </c:if>
                       </c:if>
                   </c:forEach>
                   <c:if test="${ not preview and recordWebFormContext.editable and not recordWebFormContext.moderateOnly }">
                       <th>Delete</th>
                   </c:if>
               </tr>
            </thead>
            <tbody>
                <%-- Insert existing records here. --%>
                <c:forEach items="${recordWebFormContext.namedCollections['recordFieldCollectionList']}" var="recordFormFieldCollection">
                    <tiles:insertDefinition name="singleSiteMultiTaxaRow">
                        <tiles:putAttribute name="recordFormFieldCollection" value="${recordFormFieldCollection}"/>
                        <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                        <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                        <tiles:putAttribute name="editEnabled" value="${ recordWebFormContext.editable }" />
                        <tiles:putAttribute name="isModerationOnly" value="${ recordWebFormContext.moderateOnly }" />
                    </tiles:insertDefinition>
                </c:forEach>
            </tbody>
        </table>
    </div>
    
    </div>
    
    <%-- the record form footer contains the 'form' close tag --%>
<tiles:insertDefinition name="recordFormFooter">
    <tiles:putAttribute name="recordWebFormContext" value="${recordWebFormContext}" />
</tiles:insertDefinition>

<noscript>
    <tiles:insertDefinition name="noscriptMessage"></tiles:insertDefinition>
</noscript>

<script type="text/javascript">
    jQuery(function() {
        jQuery("#script_content").removeClass("hidden");
    });
</script>

<script type="text/javascript">
    jQuery(window).load(function() {
        /**
         * Prepopulate fields
         */
		var surveyIdSelector =  '[name=surveyId]';
		var surveyId = jQuery(surveyIdSelector).val();
		 
        bdrs.form.prepopulate();
		
		// initialise the autocompletes for the rows.
        <c:if test="${fn:length(recordWebFormContext.namedCollections['recordFieldCollectionList']) < 1 && hasRequiredTableCell}">
        	bdrs.contribute.singleSiteMultiTaxa.addSighting('#sighting_index', surveyIdSelector, '#sightingTable tbody', false, false, ${showScientificName});
        </c:if>
        
        jQuery("form").submit(function() {
            // if there are no entries in the table, let the user know
            var rows = jQuery("#sightingTable").find("tbody").find("tr");
            if (rows.length < 1 && ${hasRequiredTableCell}) {
                alert("You must add at least 1 row to the table to submit this record!");
                return false;
            }
        });
    });
</script>
