<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<jsp:useBean id="fieldNameAttr" scope="request" type="au.com.gaiaresources.bdrs.model.taxa.Attribute" />
<jsp:useBean id="records" scope="request" type="java.util.List" />
<%@ page import="au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil" %>

<h1>Field Name Review</h1>

<p>This review page lists all records that have field names associated with them. You can assign scientific names
to these records from the table below.</p>

<h2>Choose Survey</h2>

<div id="field_name_review_choose_survey" class="sepBottomDotted">
	<form method="GET" action="${ portalContextPath }${ uri }">
	<select name="surveyId">
	    <c:forEach var="survey" items="${ surveys }">
	        <option value="${ survey.id }" 
	            <c:if test="${ survey.id == surveyId }"> selected="selected" </c:if>
	        ><c:out value="${ survey.name }"/></option>
	    </c:forEach>
	</select>
	<input type="submit" value="Refresh" class="form_action" />
	</form>
</div>

<h2>Edit Records</h2>

<c:choose>
	<c:when test="<%= records.isEmpty() %>">
        <p>There are no records with field names for this survey.<p>
	</c:when>
	<c:otherwise>
	    <c:set var="rowIdx" value="0" />
		<form method="POST" action="${ portalContextPath }${ uri }">
		    <input name="surveyId" value="${ surveyId }" type="hidden" />
		    <table class="datatable">
		        <thead>
		            <th>Record ID</th>
		            <th>Field Name</th>
		            <th>Scientific Name</th>
		        </thead>
		        <tbody>
		            <c:forEach var="rec" items="${ records }">
		                <jsp:useBean id="rec" type="au.com.gaiaresources.bdrs.model.record.Record" />
		                <tr>
		                    <td class="textright"><a href="${ portalContextPath }/bdrs/user/surveyRenderRedirect.htm?recordId=${ rec.id }">${ rec.id }</a></td>
		                    <td><%= AttributeValueUtil.getAttributeValue(fieldNameAttr, rec) != null ? AttributeValueUtil.getAttributeValue(fieldNameAttr, rec).getStringValue() : "" %></td>
		                    <td class="textcenter field_name_review_sci_name_cell">
		                        <c:set var="speciesNameInputId" value="sci_name_${ rowIdx }" />
		                        <c:set var="speciesIdInputId" value="species_id_${ rowIdx }" />
		                        <input type="hidden" name="rowIdx" value="${ rowIdx }" />
		                        <input type="hidden" name="rec_id_${ rowIdx }" value="${ rec.id }" />
		                        <input type="hidden" id="${ speciesIdInputId }" name="${ speciesIdInputId }" value="" />
		                        <input class="field_name_review_sci_name_input" type="text" id="${ speciesNameInputId }" name="${ speciesNameInputId }" value="" />
		                        <script type="text/javascript">
		                        <%-- a little bit of javascript to initialise each species form field --%>
		                        jQuery(function() {
		                            bdrs.contribute.initSpeciesAttributeAutocomplete('#${ speciesNameInputId }', '#${ speciesIdInputId }', ${ surveyId }, true);
		                        });
		                        </script>
		                    </td>
		                </tr>
		                <c:set var="rowIdx" value="${ rowIdx + 1 }" />
		            </c:forEach>
		        </tbody>
		    </table>
		    
		    <div class="textright buttonpanel">
		        <input type="submit" value="Save" class="form_action" />
		    </div>
		</form>
	</c:otherwise>
</c:choose>

