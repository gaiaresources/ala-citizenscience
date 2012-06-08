<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- required attributes --%>
<tiles:useAttribute name="recordWebFormContext" />  <%-- RecordWebFormContext --%>

<c:choose>
    <c:when test="${ not recordWebFormContext.preview and not recordWebFormContext.editable and recordWebFormContext.existingRecord }">
    		<div class="buttonpanel textright">
                <c:if test="${record.survey.recordCommentsEnabled && (recordWebFormContext.anonymous || recordWebFormContext.commentable)}">
                    <a href="#addComment">Leave a comment</a>
                    <c:set var="commentLinkPresent" value="true"/>
                </c:if>
                <c:if test="${recordWebFormContext.unlockable}">
                    <c:if test="${commentLinkPresent}">|</c:if>

                    <tiles:insertDefinition name="unlockRecordWidget">
                        <tiles:putAttribute name="recordId" value="${recordWebFormContext.recordId}" />
                        <tiles:putAttribute name="surveyId" value="${recordWebFormContext.surveyId}" />
                    </tiles:insertDefinition>
                </c:if>
	        </div>

    </c:when>
</c:choose>