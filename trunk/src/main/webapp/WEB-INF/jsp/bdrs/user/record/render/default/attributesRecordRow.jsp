<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<%-- used for SingleSiteMultiTaxa AND SingleSiteAllTaxa --%>


<%@page import="au.com.gaiaresources.bdrs.servlet.RequestContextHolder"%>
<tiles:useAttribute name="recordFormFieldCollection" classname="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordFormFieldCollection" ignore="true" />
<tiles:useAttribute name="editEnabled" ignore="true" />
<tiles:useAttribute name="isModerationOnly" classname="java.lang.Boolean" ignore="true" />
<tiles:useAttribute name="attributeId" ignore="true" />
<tiles:useAttribute name="formPrefix" ignore="true" />

<%-- when there is a record form field collection use it, it's filled with data. Otherwise use the formFieldList object to create the empty row --%>

<c:choose>
    <c:when test="${recordFormFieldCollection != null}">
        <c:set var="ffList" value="${ recordFormFieldCollection.formFields }"></c:set>
        <c:set var="highlight" value="${recordFormFieldCollection.highlight}"></c:set>
        <c:set var="rowRecordId" value="${recordFormFieldCollection.recordId}"></c:set>
        <c:set var="replaceString" value="attribute_${attributeId}"/>
        <c:set var="recIdPrefix" value="${fn:substringBefore(recordFormFieldCollection.prefix, replaceString)}"/>
    </c:when>
    <c:otherwise>
        <c:set var="ffList" value="${ recordWebFormContext.namedFormFields['formFieldList'] }"></c:set>
        <c:set var="highlight" value="false"></c:set>
        <c:set var="rowRecordId" value="0"></c:set>
        <c:set var="recIdPrefix" value="${formPrefix}${rowIndex}"/>
    </c:otherwise>
</c:choose>

<tr class="textcenter <c:if test="${highlight}">bdrsHighlight</c:if>">
    <input name="${recordFormFieldCollection.prefix}${rowIndex}recordId" type="hidden" value="${rowRecordId}" />
    <input name="${recIdPrefix}attribute_${attributeId}_recordId" type="hidden" value="${rowRecordId}" />
    <input name="${formPrefix}attribute_${attributeId}_rowPrefix" type="hidden" value="${recordFormFieldCollection.prefix}${rowIndex}" />
    <c:set var="fieldEditable" value="${editEnabled}" ></c:set>
    <c:forEach items="${ffList}" var="formField">
        <jsp:useBean id="formField" type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractFormField" />
        <c:choose>
           <c:when test="<%= formField.isModerationFormField() %>">
               <c:set var="recordFormField" value="${formField }"/>
               <jsp:useBean id="recordFormField" type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
               <c:if test="${editEnabled}">
                   <c:choose>
                       <c:when test="${isModerationOnly}">
                           <c:set var="fieldEditable" value="<%= recordFormField.getRecord().getId() != null && RequestContextHolder.getContext().getUser().isModerator() %>"/>
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
       <c:if test="<%= formField.isAttributeFormField() %>">
               <td>
                   <tiles:insertDefinition name="attributeRenderer">
                       <tiles:putAttribute name="formField" value="${ formField }"/>
                       <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                       <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                       <tiles:putAttribute name="editEnabled" value="${ fieldEditable }" />
                       <tiles:putAttribute name="isModerationOnly" value="${ isModerationOnly }" />
                       <tiles:putAttribute name="recordId" value="${recordFormFieldCollection.recordId}" />
                       <tiles:putAttribute name="formPrefix" value="${ formField.prefix }"/>
                   </tiles:insertDefinition>
               </td>
        </c:if>
    </c:forEach>
     <c:if test="${ editEnabled and not preview and not isModerationOnly}">
        <td class="delete_col">
             <a href="javascript: void(0);" onclick="bdrs.survey.deleteAjaxRecord('${ident}', '${recordFormFieldCollection.recordId}', jQuery(this).closest('tr'), '.messages', jQuery('div[name=${ formPrefix }]').find('[name=attributeRecordIndex]'));" tabIndex="-1">
               <img src="${pageContext.request.contextPath}/images/icons/delete.png" alt="Delete" class="vertmiddle"/>
            </a>
        </td>
    </c:if>
</tr>


