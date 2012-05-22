<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:useAttribute name="commentId"/>
<c:choose>
    <c:when test="${commentId > 0}">
        <c:set var="elementId" value="commentInput${commentId}"/>
    </c:when>
    <c:otherwise>
        <c:set var="elementId" value="commentInput"/>
    </c:otherwise>
</c:choose>
<form id="addCommentForm" method="POST" action="${portalContextPath}/bdrs/user/comment/add.htm">
    <input type="hidden" name="recordId" value="${record.id}"/>
    <input type="hidden" name="commentId" value="${commentId}"/>
    <div><label for="${elementId}">Enter your comment (1000 characters or less)</label></div>
    <textarea id="${elementId}" rows="5" cols="40" name="commentText" class="validate(required, maxlength(1000))"></textarea>
    <div>
        <input class="form_action" type="submit" value="Add Comment"/>
    </div>
</form>

<script type="text/javascript">
    // A version of the ketchup error container positioning function that isn't broken by the position:relative of
    // the comment headers.
    var positionErrorContainer = function(errorContainer, field) {
        var fOffset = field.position();
        errorContainer.css({
            left: fOffset.left + field.width() - 10,
            top: fOffset.top - errorContainer.height()
        });
    };
    jQuery(function() {
        jQuery('#addCommentForm').ketchup({positionContainer:positionErrorContainer, initialPositionContainer:positionErrorContainer});
    });
</script>
