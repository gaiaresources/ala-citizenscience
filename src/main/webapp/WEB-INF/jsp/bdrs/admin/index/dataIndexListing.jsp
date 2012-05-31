<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Add/Edit Indexes</h1>
<cw:getContent key="admin/editIndexes" />
<form method="POST" action="${portalContextPath}/admin/index/dataIndexListing.htm">
    <div class="input_container">
        <div class="left buttonpanel">
            <input class="vertmiddle" type="checkbox" name="deleteIndexes" id="deleteIndexes"
            <c:if test="${ indexSchedule.fullRebuild }">
                checked="true"
            </c:if>
            />
            <label class="vertmiddle" for="deleteIndexes">Delete Indexes Before Building?</label>
            <input class="form_action" type="button" name="indexNow" value="Index Now" onClick="bdrs.index.runIndex([name=deleteIndexes]);"/>
        </div>
        <div class="textright buttonpanel">
            <input title="Add an index build schedule." id="addIndexSchedule" class="form_action" type="button" value="Add Index Schedule" onclick="window.document.location='${portalContextPath}/admin/index/dataIndexSchedule.htm';"/>
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
                        First Date/Time
                    </th>
                    <th>
                        Last Run Date/Time
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
                            ${ indexSchedule.dateString } ${ indexSchedule.timeString }
                        </td>
                        <td class="textcenter">
                            <span id="lastRun_${indexSchedule.id}">${ indexSchedule.lastRunString }</span>
                        </td>
                        <td class="textcenter">
                            <c:choose><c:when test="${ indexSchedule.fullRebuild }">Yes</c:when><c:otherwise>No</c:otherwise></c:choose>
                        </td>
                        <td class="textcenter">
                            <a href="${portalContextPath}/admin/index/dataIndexSchedule.htm?indexId=${ indexSchedule.id }">Edit</a>
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