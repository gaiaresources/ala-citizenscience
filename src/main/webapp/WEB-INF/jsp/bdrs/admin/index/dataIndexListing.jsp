<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Add/Edit Indexes</h1>
<cw:getContent key="admin/editIndexes" />
<form method="POST" action="${pageContext.request.contextPath}/admin/index/dataIndexListing.htm">
    <div class="input_container">
        <div class="textright buttonpanel">
            <input title="Add an index build schedule." id="addIndexSchedule" class="form_action" type="button" value="Add Index Schedule" onclick="window.document.location='${pageContext.request.contextPath}/admin/index/dataIndexSchedule.htm';"/>
        </div>
        <table id="index_listing" class="datatable">
            <thead>
                <tr>
                	<th>
                        Class
                    </th>
                    <th>
                        Frequency
                    </th>
                    <th>
                        First Date
                    </th>
                    <th>
                        Time
                    </th>
                    <th>
                        Delete Before Build?
                    </th>
                    <th>
                        
                    </th>
                    <th>
                        Delete?
                    </th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${indexSchedules}" var="indexSchedule">
                    <jsp:useBean id="indexSchedule" type="au.com.gaiaresources.bdrs.model.index.IndexSchedule"/>
                    <tr>
                        <td>
                            <input type="hidden" name="index" value="${ indexSchedule.id }"/>${ indexSchedule.className }
                        </td>
                        <td class="textcenter">
                            ${ indexSchedule.type }
                        </td>
                        <td class="textcenter">
                            ${ indexSchedule.dateString }
                        </td>
                        <td class="textcenter">
                            ${ indexSchedule.timeString }
                        </td>
                        <td class="textcenter">
                            <c:choose><c:when test="${ indexSchedule.fullRebuild }">Yes</c:when><c:otherwise>No</c:otherwise></c:choose>
                        </td>
                        <td class="textcenter">
                            <a href="${pageContext.request.contextPath}/admin/index/dataIndexSchedule.htm?indexId=${ indexSchedule.id }">Edit</a>
                        </td>
                        <td class="textcenter">
                            <a id="delete_${indexSchedule.id}" href="javascript: void(0);" onclick="jQuery(this).parents('tr').hide().find('select, input, textarea').attr('disabled', 'disabled'); return false;"><img src="${pageContext.request.contextPath}/images/icons/delete.png" alt="Delete" class="vertmiddle"/></a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="textright buttonpanel">
        <input type="submit" class="form_action" value="Save"/>
    </div>
</form>