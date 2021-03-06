<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:useAttribute name="editEnabled" ignore="true"/>

<c:forEach items="${formFieldList}" var="formField">
	<tiles:insertDefinition name="formFieldRenderer">
		<tiles:putAttribute name="formField" value="${ formField }"/>
		<tiles:putAttribute name="errorMap" value="${ errorMap }"/>
		<tiles:putAttribute name="valueMap" value="${ valueMap }"/>
		<tiles:putAttribute name="editEnabled" value="${editEnabled}"/>
	</tiles:insertDefinition>
</c:forEach>

<script type="text/javascript">	
	jQuery(function() {
	    jQuery(".acomplete").autocomplete({
	        source: function(request, callback) {
	            var params = {};
	            params.ident = bdrs.ident;
	            params.q = request.term;
	            var bits = this.element[0].id.split('_');
	            params.attribute = bits[bits.length-1];
	
	            jQuery.getJSON('${portalContextPath}/webservice/attribute/searchValues.htm', params, function(data, textStatus) {
	                callback(data);
	            });
	        },
	        minLength: 2,
	        delay: 300,
	        html:true
	    });
	    jQuery('form').ketchup();
	    bdrs.initDatePicker();
	});
</script>
