<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<cw:getContent key="user/contribute"/>

<ul>
    <c:forEach items="${surveys}" var="survey">
        <li>
            <a href="${portalContextPath}/bdrs/user/surveyRenderRedirect.htm?surveyId=${survey.id}">${ survey.name }</a>
            <c:if test="${survey.description != null && survey.description != ''}"> - ${survey.description}</c:if>
        </li>
    </c:forEach>
    <li>
        <a href="${portalContextPath}/bulkdata/bulkdata.htm">Bulk Data Entry</a>
        - Upload a spreadsheet or ESRI Shapefile containing sightings.
    </li>
</ul>