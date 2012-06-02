<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:useAttribute name="formField" classname="au.com.gaiaresources.bdrs.controller.attribute.formfield.TypedAttributeValueFormField"/>
<tiles:useAttribute name="isProperty" />
<tiles:useAttribute name="errorMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="valueMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="formPrefix" ignore="true" />
<tiles:useAttribute name="editEnabled" ignore="true" />

<%-- put the global 'edit form' bool into a local variable... --%>
<c:set var="fieldEditable" value="${editEnabled}"></c:set>

<c:if test="${ formPrefix == null }">
    <%-- set formPrefix to the passed value --%>
    <c:set var="formPrefix" value="${ formField.prefix }"></c:set>
</c:if>

<%-- make sure ids start with a letter prefix --%>
<c:set var="idPrefix" value="id_" />

<c:choose>
	<c:when test="${isProperty eq 'true'}">
		<c:set var="speciesNameInputName" value="<%= formField.getPrefix()+\"survey_species_search\" %>" />
		<c:set var="speciesNameInputId" value="<%= pageContext.getAttribute(\"idPrefix\") + formField.getPrefix()+\"survey_species_search\" %>" />
		<c:set var="speciesIdInputName" value="${formPrefix}species" />
		<c:set var="speciesIdInputId" value="${idPrefix}${formPrefix}species_id" />
	</c:when>
	<c:otherwise>
		<%-- the species search string is element[0] in the post map entry --%>
		<c:set var="speciesNameInputName" value="<%= formField.getPrefix()+\"attribute_\"+formField.getAttribute().getId() %>" />
		<c:set var="speciesNameInputId" value="${idPrefix}${speciesNameInputName}_0" />
		<%-- the name is the same. the species pk is element[1] in post map entry --%>
		<c:set var="speciesIdInputName" value="${speciesNameInputName}" />
		<c:set var="speciesIdInputId" value="${idPrefix}${speciesIdInputName}_1" />
	</c:otherwise>
</c:choose>

<%--
	For all of the species inputs there are 2 inputs with the same name. 
	The first input must be for the species name, e.g. Cracticus Tibicen. 
	The second input must be for the IndicatorSpecies primary key.
--%>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="showScientificName" value="<%= bdrsPluginFacade.getPreferenceBooleanValue(\"taxon.showScientificName\") %>" />

<c:choose>
    <c:when test="${fieldEditable}">
        <c:choose>
            <%-- This is a temporary workaround to make species editable on a tracker survey without
                 breaking the single site all taxa survey.  The side effect is that the species
                 will not be editable on a single site multi taxa survey, which it should be. --%>
			<%-- Adding the extra checks for the contents of the form prefix to allow editing of species attribute
			     fields on the tracker form. Problem of the single site multi taxa survey being uneditable still exists
				 and now extends to species attributes --%>
			<%-- WARNING : reliance on string literals in the following when test!!!! --%>
            <c:when test="${ formField.species != null && ((not empty formPrefix && not (formPrefix == \"censusMethodAttr_\" || formPrefix == \"taxonGroupAttr_\" ) ) || not empty pageContext.request.parameterMap['speciesId'])}">
            	<%-- this empty field is required for species attribute parsing. it does not effect species property --%>
                <input type="hidden" id="${speciesNameInputId}" name="${speciesNameInputName}" value="<c:out value="${ formField.species.scientificName }"/>"/>
				<input type="hidden" id="${speciesIdInputId}" name="${speciesIdInputName}" value="${ formField.species.id }"/>
                <c:choose>
                    <c:when test="${showScientificName}">
                        <span class="scientificName"><c:out value="${ formField.species.scientificName }"/></span>
                    </c:when>
                    <c:otherwise>
                        <span class="commonName"><c:out value="${ formField.species.commonName }"/></span>
                    </c:otherwise>
                </c:choose>
            </c:when>
			<%-- note there is an additional check here for formField.isRequired. --%>
			<%-- If there is only a single species in the survey but it is not a required field --%>
			<%-- the field will NOT be pre populated --%>
            <c:when test="<%= (formField.getAllowableSpecies().size() > 1) || (formField.getAllowableSpecies().size() == 0) || (formField.isRequired() == false) %>">
                <c:if test="<%= errorMap != null && errorMap.containsKey(pageContext.getAttribute(\"speciesNameInputName\")) %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(pageContext.getAttribute(\"speciesNameInputName\")) %>"/>
                    </p>
                </c:if>
                <input id="${speciesNameInputId}" type="text" name="${speciesNameInputName}" 
					<c:if test="${not (isProperty eq 'true')}"> class="species_attribute" </c:if>
                    <c:choose>
                        <c:when test="<%= valueMap != null && valueMap.containsKey(pageContext.getAttribute(\"speciesNameInputName\")) %>">
							<%
							String[] splitStr = ((String)valueMap.get(pageContext.getAttribute("speciesNameInputName"))).split(",");
							String nameValue = splitStr.length > 0 ? splitStr[0].trim() : ""; 
							%>
							<c:if test="<%= !nameValue.isEmpty() %>">
								value="<% out.print(nameValue); %>"
							</c:if>
                        </c:when>
                        <c:when test="${ formField.species != null && showScientificName }">
                            value="<c:out value="${ formField.species.scientificName }"/>" 
                        </c:when>
                        <c:when test="${ formField.species != null && not showScientificName }">
                            value="<c:out value="${ formField.species.commonName }"/>"
                        </c:when>
                    </c:choose>
                />
				
				<%-- The styling below is to ensure the ketchup validation box popups up aligned to the input above --%>
            	<input type="text" class="speciesIdInput
                    <c:if test="${ formField.required }"> validate(required) </c:if>
					<c:if test="${not (isProperty eq 'true')}"> species_attribute_id </c:if>"
                	id="${speciesIdInputId}" name="${speciesIdInputName}"
				 	<c:choose>
                        <c:when test="<%= valueMap != null && valueMap.containsKey(pageContext.getAttribute(\"speciesIdInputName\")) %>">
                			<%
							String idValue = "";
							String speciesIdValue = ((String)valueMap.get(pageContext.getAttribute("speciesIdInputName")));
							if ("true".equals(isProperty)) {
								idValue = speciesIdValue;
							} else {
								String[] splitStr = speciesIdValue.split(",");
								if (splitStr.length > 1) {
									idValue = splitStr[1].trim();
								}
							}
							%>
							<c:if test="<%= !idValue.isEmpty() %>">
								value="<% out.print(idValue); %>"
							</c:if>
                        </c:when>
                        <c:when test="${formField.species != null}">
                        	value="<c:out value="${formField.species.id}"/>"
                        </c:when>
                    </c:choose>
				/>
            </c:when>
            <c:when test="<%= formField.getAllowableSpecies().size() == 1 %>">
            	<%-- dummy value for name is overriden by the id --%>
				<%-- the classes on the 2 input nodes here are to stop the yearly sightings form --%>
				<%-- from clearing out the values on location change.                            --%>
				<c:choose>
					<c:when test="${ formField.required }"> 
                        <%-- if not required pre populate --%>
						<input <c:if test="${not (isProperty eq 'true')}"> class="species_attribute" </c:if>
							type="hidden" id="${speciesNameInputId}" name="${speciesNameInputName}" 
							value="dummyvalue"/>
						<input <c:if test="${not (isProperty eq 'true')}"> class="species_attribute_id" </c:if> 
							type="hidden" id="${speciesIdInputId}" name="${speciesIdInputName}" 
							value="<%= formField.getAllowableSpecies().iterator().next().getId() %>"/>
		                <c:choose>
		                    <c:when test="${showScientificName}">
		                        <span class="scientificName"><c:out value="<%= formField.getAllowableSpecies().iterator().next().getScientificName() %>"/></span>
		                    </c:when>
		                    <c:otherwise>
		                        <span class="commonName"><c:out value="<%= formField.getAllowableSpecies().iterator().next().getCommonName() %>"/></span>
		                    </c:otherwise>
		                </c:choose>
                    </c:when>
					<c:otherwise>
						
					</c:otherwise>
				</c:choose>
            </c:when>
            <c:otherwise>
                Misconfigured Project. No species available.</br>
                <sec:authorize ifAnyGranted="ROLE_ADMIN">
                    <a href="${portalContextPath}/bdrs/admin/survey/editTaxonomy.htm?surveyId=${ formField.survey.id }">
                        Assign a species now.
                    </a>
                </sec:authorize>
                <sec:authorize ifNotGranted="ROLE_ADMIN">
                    Please contact the project administrator.
                </sec:authorize>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:otherwise>
        <%-- not editable... --%>
        <c:choose>
            <c:when test="${ formField.species != null }">
                <c:choose>
                    <c:when test="${showScientificName}">
                        <span class="scientificName"><c:out value="${ formField.species.scientificName }"/></span>
                    </c:when>
                    <c:otherwise>
                        <span class="commonName"><c:out value="${ formField.species.commonName }"/></span>
                    </c:otherwise>  
                </c:choose>
            </c:when>
            <c:otherwise>
                <span class="scientificName">No species recorded</span>
            </c:otherwise>
        </c:choose>
     </c:otherwise>       
</c:choose>

<%-- do appropriate initialisation --%>
<c:if test="${not isProperty eq 'true'}">
<script type="text/javascript">
	jQuery(function() {
		var node = jQuery('#'+'${speciesNameInputId}').parent(); 
		bdrs.contribute.initSpeciesAttributeAutocomplete(node, ${formField.survey != null ? formField.survey.id : 0}, ${showScientificName});
	});
</script>
</c:if>	
