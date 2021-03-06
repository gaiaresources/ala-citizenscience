<!-- list the user groups currently in the database  -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Manage Census Methods</h1>

<cw:getContent key="admin/censusMethodListing" />

<div class="input_container">
	<div class="buttonpanel textright">
	    <input class="form_action" type="button" value="Add Census Method" onclick="window.location = '${portalContextPath}/bdrs/admin/censusMethod/edit.htm';"/>
	</div>
	
	<tiles:insertDefinition name="censusMethodGrid">
	       <tiles:putAttribute name="widgetId" value="cmList"/>
	       <tiles:putAttribute name="multiselect" value="false"/>
	       <tiles:putAttribute name="scrollbars" value="false" />
	       <tiles:putAttribute name="showActions" value="true" />
	       <tiles:putAttribute name="editUrl" value="${portalContextPath}/bdrs/admin/censusMethod/edit.htm" />
	       <!--
	       deleting census methods is likely to cause cascade problems....not sure how we will handle this yet
	       <tiles:putAttribute name="deleteUrl" value="${portalContextPath}/bdrs/admin/group/delete.htm" />
	        -->
	</tiles:insertDefinition>
</div>