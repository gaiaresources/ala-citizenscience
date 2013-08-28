<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec"%>

<h1>Custom Forms</h1>

<cw:getContent key="user/customform/listing"/>

<sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_ROOT">
	<div class="buttonpanel sepBottom">
	    <form method="POST" action="${portalContextPath}/customform/add.htm" enctype="multipart/form-data">
	        <input id="add_customform_file" name="form_file" type="file" style="visibility:hidden"/>
		    <input id="add_customform_button" class="form_action right" type="button" value="Add Custom Form"/>
	    </form>
	    <div class="clear"></div>
	</div>
</sec:authorize>

<c:choose>
    <c:when test="${not empty customforms}">
        <c:forEach items="${ customforms }" var="customform">
            <div class="customform_item sepBottomDotted">
                <div class="left customform_descriptor">
                    <h5><c:out value="${ customform.name }"/></h5>
                    <div class="left clear customform_description"><c:out value="${ customform.description }"/></div>
                </div>
                <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_ROOT">
        	        <div class="right delete_customform_container">
        	            <form method="POST" action="${portalContextPath}/customform/delete.htm">
        	                <input type="hidden" name="formId" value="${ customform.id }"/>
        	                <a href="javascript:void(0);" class="delete delete_customform">Delete</a>
        	            </form>
        	        </div>
                </sec:authorize>
                <div class="clear"></div>
            </div>
        </c:forEach>
    </c:when>
    <c:otherwise>
        <p>There are no custom forms</p>
    </c:otherwise>
</c:choose>


<script type="text/javascript">
    jQuery(window).load(function() {
        bdrs.customform.listing.init();
    });
</script>
