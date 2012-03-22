<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>


<a name="comment${tmpComment.id}"></a>
<li>
    <div class="commentHeader">
        <span class="userName"><cw:userFullName userId="${tmpComment.createdBy}"/></span> on <fmt:formatDate dateStyle="MEDIUM" type="date" value="${tmpComment.createdAt}"/> <fmt:formatDate pattern="HH:mm" value="${tmpComment.createdAt}"/> said:
        <c:if test="${recordWebFormContext.commentable && not tmpComment.deleted}">
          <span class="actions"><a href="#comment${tmpComment.id}" name="reply" onclick="javascript:jQuery('#addComment${tmpComment.id}').toggle(500); return false;">Reply</a>
          <sec:authorize ifAllGranted="ROLE_ADMIN"> | <a href="#comment${tmpComment.id}" onclick="javascript:jQuery('#deleteComment${tmpComment.id}').submit(); return false;">Delete</a></sec:authorize>
        </c:if>
        </span>

        <c:set var="formAction"><c:url value="/bdrs/user/comment/delete.htm"/></c:set>
        <form id="deleteComment${tmpComment.id}" action="${formAction}" method="POST">
            <input type="hidden" name="commentId" value="${tmpComment.id}"/>
            <input type="hidden" name="recordId" value="${record.id}"/>
        </form>
    </div>

    <c:choose>
        <c:when test="${tmpComment.deleted}">
            <div class="deleted">
                This comment has been deleted
            </div>
        </c:when>
        <c:otherwise>
            <div class="commentBody"><c:out value="${tmpComment.commentText}" escapeXml="true"/></div>
        </c:otherwise>
    </c:choose>


    <div id="addComment${tmpComment.id}" style="display:none">
        <tiles:insertDefinition name="addComment">
            <tiles:putAttribute name="commentId" value="${tmpComment.id}"/>
        </tiles:insertDefinition>
    </div>

    <c:forEach var="reply" items="${tmpComment.replies}">
        <ul>
            <c:set var="tmpComment" value="${reply}" scope="request"/>
            <jsp:include page="comment.jsp"/>
        </ul>
    </c:forEach>
</li>
