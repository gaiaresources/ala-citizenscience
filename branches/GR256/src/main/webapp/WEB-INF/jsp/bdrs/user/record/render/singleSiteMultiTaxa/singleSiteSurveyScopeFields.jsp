<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>

<c:choose>
	<%--  CSS_LAYOUT --%>
	<c:when test="${ survey.formRendererType.cssLayout }">
		<c:forEach
			items="${recordWebFormContext.namedFormFields['formFieldList']}"
			var="formField1">
			<jsp:useBean id="formField1"
				type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
			<c:if test="<%=formField1.isPropertyFormField()%>">
				<c:if test="${ formField1.scope == 'SURVEY'}">
					<tiles:insertDefinition name="divLayoutformFieldRenderer">
						<tiles:putAttribute name="formField" value="${formField1}" />
						<tiles:putAttribute name="locations" value="${locations}" />
						<tiles:putAttribute name="editEnabled"
							value="${ recordWebFormContext.editable }" />
						<tiles:putAttribute name="isModerationOnly"
							value="${ recordWebFormContext.moderateOnly }" />
					</tiles:insertDefinition>
				</c:if>
			</c:if>
			<c:if test="<%=formField1.isAttributeFormField()%>">
				<c:if
					test="${ formField1.attribute.scope == 'SURVEY' || formField1.attribute.scope == 'SURVEY_MODERATION'}">
					<tiles:insertDefinition name="divLayoutformFieldRenderer">
						<tiles:putAttribute name="formField" value="${formField1}" />
						<tiles:putAttribute name="locations" value="${locations}" />
						<tiles:putAttribute name="editEnabled"
							value="${ recordWebFormContext.editable }" />
						<tiles:putAttribute name="isModerationOnly"
							value="${ recordWebFormContext.moderateOnly }" />
					</tiles:insertDefinition>
				</c:if>
			</c:if>
		</c:forEach>
	</c:when>
	<%--  DEFAULT --%>
	<c:otherwise>
		<table class="form_table">
			<tbody>
				<c:forEach
					items="${recordWebFormContext.namedFormFields['formFieldList']}"
					var="formField2">
					<jsp:useBean id="formField2"
						type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
					<c:if test="<%=formField2.isPropertyFormField()%>">
						<c:if test="${ formField2.scope == 'SURVEY'}">
							<tiles:insertDefinition name="formFieldRenderer">
								<tiles:putAttribute name="formField" value="${formField2}" />
								<tiles:putAttribute name="locations" value="${locations}" />
								<tiles:putAttribute name="editEnabled"
									value="${ recordWebFormContext.editable }" />
								<tiles:putAttribute name="isModerationOnly"
									value="${ recordWebFormContext.moderateOnly }" />
							</tiles:insertDefinition>
						</c:if>
					</c:if>
					<c:if test="<%=formField2.isAttributeFormField()%>">
						<c:if
							test="${ formField2.attribute.scope == 'SURVEY' || formField2.attribute.scope == 'SURVEY_MODERATION'}">
							<tiles:insertDefinition name="formFieldRenderer">
								<tiles:putAttribute name="formField" value="${formField2}" />
								<tiles:putAttribute name="locations" value="${locations}" />
								<tiles:putAttribute name="editEnabled"
									value="${ recordWebFormContext.editable }" />
								<tiles:putAttribute name="isModerationOnly"
									value="${ recordWebFormContext.moderateOnly }" />
							</tiles:insertDefinition>
						</c:if>
					</c:if>
				</c:forEach>
			</tbody>
		</table>
	</c:otherwise>
</c:choose>