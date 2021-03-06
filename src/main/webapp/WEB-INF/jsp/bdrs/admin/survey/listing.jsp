<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Edit Projects</h1>

<cw:getContent key="admin/editProjects" />

<div id="projectListingContainer">

    <div class="buttonpanel textright">
        <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#projectListingContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
        <input class="form_action" type="button" value="Add Project" onclick="window.document.location='${portalContextPath}/bdrs/admin/survey/edit.htm';"/>
    </div>
    
    <table class="datatable textcenter">
        <thead>
            <tr>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Name</th>
                <th>Taxonomy</th>
                <th>Form</th>
                <th>Map</th>
                <th>Locations</th>
                <th>Access</th>
                <th>Publish</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var='survey' items='${surveyList}'>
                <tr>
                    <td>
                        <fmt:formatDate pattern="dd MMM yyyy" value="${survey.startDate}"/>
                    </td>
                    <td>
                        <fmt:formatDate pattern="dd MMM yyyy" value="${survey.endDate}"/>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/edit.htm?surveyId=${survey.id}">
                            <c:out value="${survey.name}"/>
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/editTaxonomy.htm?surveyId=${survey.id}">
                            Edit
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/editAttributes.htm?surveyId=${survey.id}">
                            Edit
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/editMap.htm?surveyId=${survey.id}">
                            Edit
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/locationListing.htm?surveyId=${survey.id}">
                            Edit
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/editUsers.htm?surveyId=${survey.id}">
                            Edit
                        </a>
                    </td>
                    <td>
                        <a href="${portalContextPath}/bdrs/admin/survey/edit.htm?surveyId=${survey.id}&publish=publish">
                            Edit
                        </a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

</div>
