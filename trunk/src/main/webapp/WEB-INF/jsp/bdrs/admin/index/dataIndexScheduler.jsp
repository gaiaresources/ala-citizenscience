<%@page import="au.com.gaiaresources.bdrs.model.index.IndexType"%>
<%@page import="java.util.Calendar"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<tiles:useAttribute name="indexSchedule" classname="au.com.gaiaresources.bdrs.model.index.IndexSchedule" ignore="true"/>

<h1>Edit Data Index Schedule</h1>

<cw:getContent key="admin/manageIndex" />

<form method="POST">

    <c:if test="${indexSchedule.id != null}">
        <input type="hidden" name="indexId" value="${indexSchedule.id}"/>
    </c:if>
    <div class="input_container">
        <div class="left">
            Select something to index:
            <c:forEach var="indexClass" items="${ indexClasses }">
                <jsp:useBean id="indexClass" type="java.lang.Class"/>
                <div style="padding: 2px;">
                <input type="checkbox" name="indexClass" id="index_class_<%= indexClass.getSimpleName() %>" value="<%= indexClass.getSimpleName() %>"
                    <c:if test="<%= indexClass.getSimpleName().equals(indexSchedule.getClassName()) %>">
                        checked="true"
                    </c:if>
                />
                <label for="index_class_<%= indexClass.getSimpleName() %>"><%= indexClass.getSimpleName() %></label>
                </div>
            </c:forEach>
        </div>
            <div class="right">
                <input class="left" type="checkbox" name="deleteIndexes" id="deleteIndexes"
                <c:if test="${ indexSchedule.fullRebuild }">
                    checked="true"
                </c:if>
                />
                <label for="deleteIndexes">Delete Indexes Before Building?</label>
            </div>
        <div class="clear">
	        <h3>Schedule a Recurring Build</h3>
	        <div class="scheduleRow">
	            <input type="radio" name="indexType" value="server_startup"
	            <c:if test="<%= IndexType.SERVER_STARTUP.equals(indexSchedule.getType()) %>">
	                checked="true"
	            </c:if>
	            >Server Startup</input>
	        </div>
	        <div class="scheduleRow">
	            <input type="radio" name="indexType" value="once"
	            <c:if test="<%= IndexType.ONCE.equals(indexSchedule.getType()) %>">
	                checked="true"
	            </c:if>
	            >Once</input> on 
	            <input type="text" name="date" class="datepicker validate(dateOrBlank)" 
	            <c:choose>
	                <c:when test="<%= !IndexType.ONCE.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.dateString }"
	                </c:otherwise>
	            </c:choose>
	            /> at 
	            <input type="text" name="time" class="timepicker validate(timeOrBlank)"  
	            <c:choose>
	                <c:when test="<%= !IndexType.ONCE.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.timeString }"
	                </c:otherwise>
	            </c:choose>
	            />
	        </div>
	        <div class="scheduleRow">
	            <input type="radio" name="indexType" value="daily"
	            <c:if test="<%= IndexType.DAILY.equals(indexSchedule.getType()) %>">
	                checked="true"
	            </c:if>
	            >Daily</input> at
	            <input type="text" name="time" class="timepicker validate(timeOrBlank)"  
	            <c:choose>
	                <c:when test="<%= !IndexType.DAILY.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.timeString }"
	                </c:otherwise>
	            </c:choose>
	            />
	        </div>
	        <div class="scheduleRow">
	            <input type="radio" name="indexType" value="weekly"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) %>">
	                checked="true"
	            </c:if>
	            >Weekly</input> at
	            <input type="text" name="time" class="timepicker validate(timeOrBlank)"  
	            <c:choose>
	                <c:when test="<%= !IndexType.WEEKLY.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.timeString }"
	                </c:otherwise>
	            </c:choose>
	            /> on 
	            <select name="weeklyDay"
	            <c:if test="<%= !IndexType.WEEKLY.equals(indexSchedule.getType()) %>">
	                disabled
	            </c:if>>
	            <option value="<%= Calendar.MONDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.MONDAY %>">
	                selected
	            </c:if>
	            >Monday</option>
	            <option value="<%= Calendar.TUESDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.TUESDAY %>">
	                selected
	            </c:if>
	            >Tuesday</option>
	            <option value="<%= Calendar.WEDNESDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.WEDNESDAY %>">
	                selected
	            </c:if>
	            >Wednesday</option>
	            <option value="<%= Calendar.THURSDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.THURSDAY %>">
	                selected
	            </c:if>
	            >Thursday</option>
	            <option value="<%= Calendar.FRIDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.FRIDAY %>">
	                selected
	            </c:if>
	            >Friday</option>
	            <option value="<%= Calendar.SATURDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.SATURDAY %>">
	                selected
	            </c:if>
	            >Saturday</option>
	            <option value="<%= Calendar.SUNDAY %>"
	            <c:if test="<%= IndexType.WEEKLY.equals(indexSchedule.getType()) && indexSchedule.getDayOfWeek() == Calendar.SUNDAY %>">
	                selected
	            </c:if>
	            >Sunday</option>
	            </select>
	        </div>
	        <div class="scheduleRow">
	            <input type="radio" name="indexType" value="monthly"
	            <c:if test="<%= IndexType.MONTHLY.equals(indexSchedule.getType()) %>">
	                checked="true"
	            </c:if>
	            >Monthly</input> on
	            <input type="text" name="date" class="datepicker validate(dateOrBlank)"   
	            <c:choose>
	                <c:when test="<%= !IndexType.MONTHLY.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.dateString }"
	                </c:otherwise>
	            </c:choose>
	            /> at 
	            <input type="text" name="time" class="timepicker validate(timeOrBlank)"   
	            <c:choose>
	                <c:when test="<%= !IndexType.MONTHLY.equals(indexSchedule.getType()) %>">
	                    disabled
	                </c:when>
	                <c:otherwise>
	                    value="${ indexSchedule.timeString }"
	                </c:otherwise>
	            </c:choose>
	            />
	        </div>
        </div>
    </div>

    <div class="buttonpanel textright">
        <input class="form_action" type="submit" value="Save" onclick="bdrs.index.saveIndex();return false;"/>
    </div>
</form>

<script type="text/javascript">
    // disable the inputs related to anything but the selected radio button
    jQuery("[name=indexType]").change(function() {
        // disable all of the checkboxes and text inputs in scheduleRow
        var inputs = jQuery(".scheduleRow input[type=text]");
        inputs.attr("disabled", true);
        inputs = jQuery(".scheduleRow input[type=checkbox]");
        inputs.attr("disabled", true);
        inputs = jQuery(".scheduleRow select");
        inputs.attr("disabled", true);
        // get the value of the selected item
        var selVal = jQuery(this).val();
        // get all of the inputs starting with that value and enable them
        var newinputs = jQuery("[name^="+selVal+"]");
        if (newinputs) {
            newinputs.removeAttr("disabled");
        }
        newinputs = jQuery(this).parent("div").find("[name=date]");
        if (newinputs) {
            newinputs.removeAttr("disabled");
        }
        newinputs = jQuery(this).parent("div").find("[name=time]");
        if (newinputs) {
            newinputs.removeAttr("disabled");
        }
        // also set their ketchup validations to required
    });
</script>