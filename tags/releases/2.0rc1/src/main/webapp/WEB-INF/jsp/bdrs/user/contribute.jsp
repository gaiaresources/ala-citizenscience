<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<h1>Contribute</h1>

<p>
The Contribue menu lists the available Projects for this Portal.  
By clicking on one of them, you will go to the relevant form for that Project, 
and you can contribute your own Sightings there.  
Currently, the list of Projects that you can access includes:
</p>

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