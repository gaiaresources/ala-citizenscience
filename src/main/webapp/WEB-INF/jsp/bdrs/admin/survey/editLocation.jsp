<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<%@page import="au.com.gaiaresources.bdrs.model.metadata.Metadata" %>
<%@page import="java.util.Set" %>
<%@page import="java.util.HashSet" %>
<%@page import="au.com.gaiaresources.bdrs.model.location.Location" %>
<%@page import="au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem"%>
<jsp:useBean id="location" type="au.com.gaiaresources.bdrs.model.location.Location" scope="request"/>
<jsp:useBean id="survey" type="au.com.gaiaresources.bdrs.model.survey.Survey" scope="request"/>
<jsp:useBean id="webMap" type="au.com.gaiaresources.bdrs.controller.map.WebMap" scope="request"/>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="showScientificName" value="<%= bdrsPluginFacade.getPreferenceBooleanValue(\"taxon.showScientificName\") %>" />

<c:set var="required" value="<%= Boolean.TRUE %>" />

<c:choose>
	<c:when test="${location.id != null}">
		<h1>Edit Location</h1>
	</c:when>
	<c:otherwise>
		<h1>Create Location</h1>
	</c:otherwise>
</c:choose>

<cw:getContent key="admin/editProject/editLocation" />

<c:set var="crs" value="${ webMap.map.crs }" />
<c:set var="selectedSrid" value="<%= location != null && location.getLocation() != null ? location.getLocation().getSRID() : 0 %>" />
<c:set var="xCoord" value="<%= location != null && location.getLocation() != null ? location.getLocation().getCentroid().getX() : null %>" />
<c:set var="yCoord" value="<%= location != null && location.getLocation() != null ? location.getLocation().getCentroid().getY() : null %>" />
<c:set var="wkt" value="<%= location != null && location.getLocation() != null? location.getLocation().toText() : \"\" %>" />
<c:set var="required" value="<%= Boolean.TRUE %>" />
<c:set var="editEnabled" value="<%= Boolean.TRUE %>" />
<c:set var="readOnly" value="<%= Boolean.FALSE %>" />
<%
    BdrsCoordReferenceSystem crs = (BdrsCoordReferenceSystem)pageContext.getAttribute("crs");
    Integer selectedSrid = (Integer)pageContext.getAttribute("selectedSrid");
	pageContext.setAttribute("crsFieldRequired", crs.isZoneRequired() || (selectedSrid != 0 && (crs.getSrid() != selectedSrid.intValue())));
	pageContext.setAttribute("selectedCrs", selectedSrid != 0 ? BdrsCoordReferenceSystem.getBySRID(selectedSrid) : crs);
%>

<form method="POST" action="${portalContextPath}/bdrs/admin/survey/editLocation.htm" enctype="multipart/form-data">
    <input type="hidden" name="surveyId" value="${survey.id}"/>
    <c:if test="${location != null}">
        <input type="hidden" name="locationId" value="${location.id}"/>
    </c:if>
    <div id="locationsContainer" class="input_container locationsContainer">
        <div id="locationMetadata">
            <table>
                <tr>
                    <th>
                        <label class="textright" for="locationName">
                            Site Name
                        </label>
                    </th>
                    <td>
                        <input type="text" name="locationName" id="locationName" class="locationMetadata validate(required)"
                        <c:if test="${location != null}">
                            value="<c:out value="${location.name}"/>"
                        </c:if>/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="textright" for="locationDescription">
                            Site Description
                        </label>
                    </th>
                    <td>
                        <textarea maxlength="255" rows="2" name="locationDescription" id="locationDescription" class="locationMetadata"><c:if test="${location != null}"><c:out value="${location.description}"/></c:if></textarea>
                    </td>
                </tr>
            </table>
        </div>
        <div class="clear">
        </div>
        <div class="locations_container attributesContainer form_table locationAttributes">
            <tiles:insertDefinition name="locationEntryMap">
                <tiles:putAttribute name="survey" value="${survey}"/>
            </tiles:insertDefinition>
            <input id="locationWkt" type="hidden" name="location_WKT" value="${ wkt }" />
            <c:if test="${ not crsFieldRequired }">
	       	    <input type="hidden" name="srid" value="${ crs.srid }" />
	        </c:if>
            <table>
            	<c:if test="${ crsFieldRequired }">
	                <tr>
	                    <th><label for="srid">Zone</label></th>
	                    <td>
	                        <tiles:insertDefinition name="coordFormField">
	                            <tiles:putAttribute name="crs" value="${ crs }"/>
	                            <tiles:putAttribute name="isZone" value="true"/>
	                            <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
	                            <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
	                            <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
	                            <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
	                            <tiles:putAttribute name="xCoord" value="${ xCoord }" />
	                            <tiles:putAttribute name="yCoord" value="${ yCoord }" />
	                            <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
	                            <tiles:putAttribute name="required" value="${ required }" />
	                        </tiles:insertDefinition>
	                    </td>
	                </tr>
                </c:if>
                <tr>
                    <th>
                        <label class="right textright" for="latitude">
                            ${ selectedCrs.yname }
                        </label>
                    </th>
                    <td>
                        <tiles:insertDefinition name="coordFormField">
                            <tiles:putAttribute name="crs" value="${ crs }"/>
                            <tiles:putAttribute name="isLatitude" value="true"/>
                            <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                            <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                            <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
                            <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
                            <tiles:putAttribute name="xCoord" value="${ xCoord }" />
                            <tiles:putAttribute name="yCoord" value="${ yCoord }" />
                            <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                            <tiles:putAttribute name="required" value="${ required }" />
                        </tiles:insertDefinition>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="right textright" for="longitude">
                            ${ selectedCrs.xname }
                        </label>
                    </th>
                    <td>
                        <tiles:insertDefinition name="coordFormField">
                            <tiles:putAttribute name="crs" value="${ crs }"/>
                            <tiles:putAttribute name="isLongitude" value="true"/>
                            <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                            <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                            <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
                            <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
                            <tiles:putAttribute name="xCoord" value="${ xCoord }" />
                            <tiles:putAttribute name="yCoord" value="${ yCoord }" />
                            <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                            <tiles:putAttribute name="required" value="${ required }" />
                        </tiles:insertDefinition>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="right textright" for="locationArea">
                            Site Area (Ha)
                        </label>
                    </th>
                    <td>
                        <input type="text" name="locationArea" readonly="readonly" />
                    </td>
                </tr>
            </table><!-- create a set for the location -->
            <c:forEach items="${locationFormFieldList}" var="formField">
                <tiles:insertDefinition name="formFieldVerticalRenderer">
                    <tiles:putAttribute name="formField" value="${ formField }"/><tiles:putAttribute name="errorMap" value="${ errorMap }"/><tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                </tiles:insertDefinition>
            </c:forEach>
        </div>
        <div class="clear">
        </div>
        <div class="buttonpanel">
            <div class="textright buttonpanel">
                <span class="error" id="wktErrorMessage"></span>
                <input type="button" class="form_action" name="goback" value="Go Back" onclick="window.document.location='${portalContextPath}/bdrs/admin/survey/locationListing.htm?surveyId=${survey.id}'"/>
                <input id="saveLocation" type="submit" class="form_action" name="save" value="Save"/>
            </div>
        </div>
    </div>
</form>
<script type="text/javascript">
    jQuery(function(){
        bdrs.map.initWktOnChangeValidation("#locationWkt", "#wktErrorMessage", // valid handler 
     function(){
            jQuery("#saveLocation").prop("disabled", false);
        }, // not valid handler
     function(){
            jQuery("#saveLocation").prop("disabled", true);
        });
    });
</script>
