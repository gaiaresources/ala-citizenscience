<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<%-- used for SingleSiteMultiTaxa AND SingleSiteAllTaxa --%>


<%@page import="au.com.gaiaresources.bdrs.servlet.RequestContextHolder"%>
<tiles:useAttribute name="recordFormFieldCollection" classname="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection" ignore="true" />
<tiles:useAttribute name="editEnabled" ignore="true" />
<tiles:useAttribute name="isModerationOnly" classname="java.lang.Boolean" ignore="true" />

<%-- when there is a record form field collection use it, it's filled with data. Otherwise use the formFieldList object to create the empty row --%>

<c:choose>
    <c:when test="${recordFormFieldCollection != null}">
        <c:set var="ffList" value="${ recordFormFieldCollection.formFields }"></c:set>
        <c:set var="highlight" value="${recordFormFieldCollection.highlight}"></c:set>
    </c:when>
    <c:otherwise>
        <c:set var="ffList" value="${ recordWebFormContext.namedFormFields['formFieldList'] }"></c:set>
        <c:set var="highlight" value="false"></c:set>
    </c:otherwise>
</c:choose>

<tr class="textcenter <c:if test="${highlight}">bdrsHighlight</c:if>"  >

    <input name="${recordFormFieldCollection.prefix}${sightingIndex}recordId" type="hidden" value="${recordFormFieldCollection.recordId}" class="recordRow" />
    <input name="rowPrefix" type="hidden" value="${recordFormFieldCollection.prefix}${sightingIndex}" />
    <c:set var="fieldEditable" value="${editEnabled}" ></c:set>
    <c:forEach items="${ffList}" var="formField">
        <jsp:useBean id="formField" type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
           
        <c:choose>
           <c:when test="<%= formField.isModerationFormField() %>">
               <c:if test="${editEnabled}">
                   <c:choose>
	                   <c:when test="${isModerationOnly}">
		                   <c:set var="fieldEditable" value="<%= formField.getRecord().getId() != null && RequestContextHolder.getContext().getUser().isModerator() %>"/>
	                  </c:when>
	                  <c:otherwise>
	                      <c:set var="fieldEditable" value="<%= RequestContextHolder.getContext().getUser().isModerator() %>"/>
	                  </c:otherwise>
                  </c:choose>
               </c:if>
           </c:when>
           <c:otherwise>
               <c:if test="${editEnabled}">
                   <c:set var="fieldEditable" value="${not isModerationOnly}"></c:set>
               </c:if>
           </c:otherwise>
       </c:choose>
       <c:choose>
           <c:when test="<%= formField.isPropertyFormField() %>">
               <c:if test="${ not formField.hidden }">
                   <td>
                       <tiles:insertDefinition name="propertyRenderer">
                           <tiles:putAttribute name="formField" value="${ formField }"/>
                           <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                           <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                           <tiles:putAttribute name="editEnabled" value="${ fieldEditable }" />
                       </tiles:insertDefinition>
                  </td>
               </c:if>
           </c:when>
           <c:when test="<%= formField.isAttributeFormField() %>">
               <td>
                   <tiles:insertDefinition name="attributeRenderer">
                       <tiles:putAttribute name="formField" value="${ formField }"/>
                       <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                       <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                       <tiles:putAttribute name="editEnabled" value="${ fieldEditable }" />
                       <tiles:putAttribute name="isModerationOnly" value="${ isModerationOnly }" />
                       <tiles:putAttribute name="recordId" value="${recordFormFieldCollection.recordId}" />
                   </tiles:insertDefinition>
               </td>
        </c:when>
    </c:choose>
    </c:forEach>
     <c:if test="${ editEnabled and not preview and not isModerationOnly}">
        <td class="delete_col">
             <a href="javascript: void(0);" onclick="bdrs.survey.deleteAjaxRecord('${ident}', '${recordFormFieldCollection.recordId}', jQuery(this).parents('tr'), '.messages');" tabIndex="-1">
               <img src="${pageContext.request.contextPath}/images/icons/delete.png" alt="Delete" class="vertmiddle"/>
            </a>
        </td>
    </c:if>
</tr>


