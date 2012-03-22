<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<c:if test="${record.survey.recordCommentsEnabled}">
<a name="addComment"></a>
<div class="comments">
    <h3><c:choose><c:when test="${empty record.comments}">No Comments</c:when><c:otherwise>Comments</c:otherwise></c:choose></h3>

    <div>
        <c:choose>
            <c:when test="${recordWebFormContext.commentable}">
                    <tiles:insertDefinition name="addComment">
                        <tiles:putAttribute name="commentId" value="-1"/>
                    </tiles:insertDefinition>
            </c:when>
            <c:otherwise>
            <c:if test="${recordWebFormContext.anonymous}">

                <c:url var="loginUrl" value="/home.htm">
                    <c:param name="signin" value="true"/>
                    <c:param name="redirectUrl" value=""/>
                </c:url>

                <input id="signIn" class="form_action" type="button" value="Sign in to leave a comment"/>
            </c:if>
            <c:if test="${not record.survey.public}"><div class="warning">Please note commenting on this survey has been restricted to survey members.</div></c:if>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="commentsList">
        <ul class="commentsList">

        <c:forEach var="comment" items="${record.comments}">
            <c:set var="tmpComment" value="${comment}" scope="request"/>
            <jsp:include page="comment.jsp"/>
        </c:forEach>
        </ul>
    </div>
</div>

<script type="text/javascript">
    jQuery(function() {
        jQuery('ul.commentsList').collapsible({defaulthide:false});
        var signIn = jQuery('#signIn');
        if (signIn.length) {
            var url = '${loginUrl}'+encodeURIComponent(document.location+'#addComment');
            signIn.click(function() {document.location = url});
        }
    });
</script>

</c:if>