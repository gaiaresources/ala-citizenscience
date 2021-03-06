<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>


<%@page import="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType"%>
<tiles:useAttribute name="formField" classname="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField"/>
<tiles:useAttribute name="isLatitude" ignore="true"/>
<tiles:useAttribute name="isLongitude" ignore="true"/>

<tiles:useAttribute name="locations" classname="java.util.Set" ignore="true"/>

<tiles:useAttribute name="errorMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="valueMap" classname="java.util.Map" ignore="true"/>

<tiles:useAttribute name="formPrefix" ignore="true" />
<tiles:useAttribute name="editEnabled" ignore="true" />
<tiles:useAttribute name="speciesEditable" ignore="true" />

<%@page import="au.com.gaiaresources.bdrs.model.record.Record"%>
<%@page import="au.com.gaiaresources.bdrs.model.method.Taxonomic"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.GregorianCalendar"%>

<c:if test="${ formPrefix == null }">
    <!-- set formPrefix to the passed value -->
    <c:set var="formPrefix" value="${ formField.prefix }"></c:set>
</c:if>

<%-- put the global 'edit form' bool into a local variable... --%>
<c:set var="fieldEditable" value="${editEnabled}"></c:set>

<c:choose>
    <c:when test="<%= RecordPropertyType.SPECIES.getName().equals(formField.getPropertyName()) %>">
		<tiles:insertDefinition name="speciesFormField">
        	<tiles:putAttribute name="formField" value="${formField}"/>
            <tiles:putAttribute name="editEnabled" value="${ editEnabled && (speciesEditable == null || speciesEditable) }"/>
			<tiles:putAttribute name="errorMap" value="${ errorMap }"/>
			<tiles:putAttribute name="valueMap" value="${ valueMap }"/>
			<tiles:putAttribute name="isProperty" value="true" />
        </tiles:insertDefinition>
    </c:when>
    <c:when test="<%= RecordPropertyType.LOCATION.getName().equals(formField.getPropertyName()) %>">
    	<c:set var="crsSelector" value="[name=${ formPrefix }srid]" />
    
        <c:choose>
            <c:when test="${fieldEditable}">
                <select id="location" onchange="bdrs.survey.location.updateLocation(jQuery(this).val(), ${ formField.survey.id }, { attributeSelector: '#attributesContainer', crsSelector: '${ crsSelector }' });" name="${ formPrefix }location" >
                    <option value="-1"></option>
                    <c:forEach items="${ locations }" var="location">
                        <jsp:useBean id="location" type="au.com.gaiaresources.bdrs.model.location.Location"/>
                        <option value="${ location.id }"
                            <c:choose>
                                <c:when test="<%= valueMap != null && location.getId().toString().equals(valueMap.get(formField.getPrefix()+\"location\")) %>">
                                    selected="selected"
                                </c:when>
                                <c:when test="${ formField.record.location == location }">
                                    selected="selected"
                                </c:when>
                            </c:choose>
                        >
                            <c:out value="${ location.name }"/>
                        </option>
                        <c:if test="<%= valueMap != null && location.getId().toString().equals(valueMap.get(formField.getPrefix()+\"location\")) %>">
                        <script type="text/javascript">
                            <%-- 
                              The following snippet sets the initial value for the attributes based
                              on the location selection. 
                              
                              It is ok if the browser does not have javascript enabled because 
                              the hidden input is only present to support ketchup validation.
                            --%>
                            jQuery(window).load(function() {
                                bdrs.survey.location.updateLocationAttributes(${ location.id }, ${ formField.survey.id }, { 
                                	attributeSelector: '#attributesContainer',
                                	crsSelector: '${ crsSelector }'
                                });
                            });
                        </script>
                        </c:if>
                        <c:if test="${ formField.record.location == location }">
                        <script type="text/javascript">
                            <%-- 
                              The following snippet sets the initial value for the attributes based
                              on the location selection. 
                              
                              It is ok if the browser does not have javascript enabled because 
                              the hidden input is only present to support ketchup validation.
                            --%>
                            jQuery(window).load(function() {
                                bdrs.survey.location.updateLocationAttributes(${ location.id }, ${ formField.survey.id }, { attributeSelector: '#attributesContainer', crsSelector: '${ crsSelector }' });
                            });
                        </script>
                        </c:if>
                    </c:forEach>
                </select>
            </c:when>
            <c:otherwise>
                <c:if test="${ formField.record.location != null }">
                    <span><c:out value="${ formField.record.location.name }" /></span>
                    <script type="text/javascript">
                        <%-- 
                          The following snippet sets the initial value for the attributes based
                          on the location selection. 
                          
                          It is ok if the browser does not have javascript enabled because 
                          the hidden input is only present to support ketchup validation.
                        --%>
                        jQuery(window).load(function() {
                            bdrs.survey.location.updateLocationAttributes(${ formField.record.location.id }, ${ formField.survey.id }, { attributeSelector: '#attributesContainer', crsSelector: '${ crsSelector }' });
                        });
                        
                    </script>
                </c:if>
            </c:otherwise>
        </c:choose>
        <div id="attributesContainer"></div>
    </c:when>
    <c:when test="<%= RecordPropertyType.POINT.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">
                <c:choose>
                    <c:when test="${ isLatitude }">
                        <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"latitude\") %>">
                            <p class="error">
                                <c:out value="<%= errorMap.get(formField.getPrefix()+\"latitude\") %>"/>
                            </p>
                        </c:if>
                        <input id="latitude" type="text" name="${ formPrefix }latitude" class="validate(rangeOrBlank(-90,90), numberOrBlank<c:if test="${ formField.required }">,required</c:if>)"
                            <c:choose>
                                <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"latitude\") %>">
                                    value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"latitude\") %>"/>"
                                </c:when>
                                <c:when test="${ formField.record != null }">
                                    value="<%= formField.getRecord().getLatitude() == null ? "" : formField.getRecord().getLatitude() %>"
                                </c:when>
                            </c:choose>
                            <c:if test="<%= formField.getSurvey().isPredefinedLocationsOnly() %>">
                                readonly="readonly"
                            </c:if>
                        />
                    </c:when>
                    <c:when test="${ isLongitude }">
                        <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"longitude\") %>">
                            <p class="error">
                                <c:out value="<%= errorMap.get(formField.getPrefix()+\"longitude\") %>"/>
                            </p>
                        </c:if>
                        <input id="longitude" type="text" name="${ formPrefix }longitude" class="validate(rangeOrBlank(-180,180), numberOrBlank<c:if test="${ formField.required }">,required</c:if>)"
                            <c:choose>
                                <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"longitude\") %>">
                                    value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"longitude\") %>"/>"
                                </c:when>
                                <c:when test="${ formField.record != null }">
                                    value="<%= formField.getRecord().getLongitude() == null ? "" : formField.getRecord().getLongitude() %>"
                                </c:when>
                            </c:choose>
                            <c:if test="<%= formField.getSurvey().isPredefinedLocationsOnly() %>">
                                readonly="readonly"
                            </c:if>
                        />
                    </c:when>
                </c:choose>
            </c:when>
            <c:otherwise>
                <c:choose>
                    <c:when test="${ isLatitude }">
                        <span><c:out value="<%= formField.getRecord().getLatitude() == null ? \"\" : formField.getRecord().getLatitude() %>" /></span>
                        <input type="hidden" name="${formPrefix}latitude" value="${formField.record.latitude}" />
                    </c:when>
                    <c:when test="${ isLongitude }">
                        <span><c:out value="<%= formField.getRecord().getLongitude() == null ? \"\" : formField.getRecord().getLongitude() %>" /></span>
                        <input type="hidden" name="${formPrefix}longitude" value="${formField.record.longitude}" />
                    </c:when>
                </c:choose>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="<%= RecordPropertyType.WHEN.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">
                
                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"date\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"date\") %>"/>
                    </p>
                </c:if>
                <c:set var="calDate" value="<%= Calendar.getInstance() %>"/>
                <jsp:useBean id="calDate" type="java.util.Calendar"/>
                <%
                    if(formField.getRecord() != null && formField.getRecord().getWhen() != null) {
                        calDate.setTime(formField.getRecord().getWhen());
                    }
                %>
                <input id="date" class="datepicker_historical 
                    <c:choose>
                        <c:when test="${ formField.required }">
                            validate(date)
                        </c:when>
                        <c:otherwise>
                            validate(dateOrBlank)
                        </c:otherwise>
                    </c:choose>" type="text" name="${ formPrefix }date"
                    <c:choose>
                        <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"date\") %>">
                            value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"date\") %>"/>"
                        </c:when>
                        <c:when test="${ formField.record != null }">
                            value="<fmt:formatDate pattern="dd MMM yyyy" value="${ formField.record.when }"/>"
                        </c:when>
                        <c:otherwise>
                           value="<fmt:formatDate pattern="dd MMM yyyy" value="${ calDate }"/>"
                        </c:otherwise>
                    </c:choose>
                />
            </c:when>
            <c:otherwise>
                <c:if test="${ formField.record.when != null}">
                    <span><fmt:formatDate pattern="dd MMM yyyy" value="${ formField.record.when }"/></span>
                </c:if>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="<%= RecordPropertyType.TIME.getName().equals(formField.getPropertyName()) %>">
        <c:set var="cal" value="<%= new GregorianCalendar() %>"/>
        <jsp:useBean id="cal" type="java.util.Calendar"/>
        <%
            if(formField.getRecord() != null && formField.getRecord().getWhen() != null) {
                cal.setTime(formField.getRecord().getWhen());
            }
        %>
        <c:choose>
            <c:when test="${fieldEditable}">
                
                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"time\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"time\") %>"/>
                    </p>
                </c:if>
                
                <input type="text" name="${ formPrefix }time" 
                    class="timepicker
                    <c:choose>
                        <c:when test="${ formField.required }">
                            validate(time)
                        </c:when>
                        <c:otherwise>
                            validate(timeOrBlank)
                        </c:otherwise>
                    </c:choose>
                    "  <%-- end of the class attribute --%>
                value="<c:out value="<%= String.format(\"%02d\", cal.get(Calendar.HOUR_OF_DAY)) + \":\" + String.format(\"%02d\", cal.get(Calendar.MINUTE)) %>" />"
                 />  <%-- end of the time input field --%>
            </c:when>
            <c:otherwise>
                
                <c:if test="${ formField.record.when != null }">
                    <span><c:out value="<%= String.format(\"%02d:%02d\", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) %>" /></span>
                </c:if>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="<%= RecordPropertyType.NOTES.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">
                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"notes\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"notes\") %>"/>
                    </p>
                </c:if>
                <%-- Has to be on one line, otherwise there will be whitespacing in the text area --%>
                <textarea id="notes" name="${ formPrefix }notes"<c:if test="${ formField.required }"> class="validate(required)"</c:if>><c:choose><c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"notes\") %>"><c:out value="<%= valueMap.get(formField.getPrefix()+\"notes\") %>"/></c:when><c:when test="${ formField.record != null }"><c:out value="${ formField.record.notes }"/></c:when></c:choose></textarea>
            </c:when>
            <c:otherwise>
                <span class="prewrap"><c:out value="${ formField.record.notes }"/></span>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="<%= RecordPropertyType.NUMBER.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">
                
                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"number\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"number\") %>"/>
                    </p>
                </c:if>
                <input id="number" type="text" name="${ formPrefix }number" 
                
                    <c:choose>
                        <c:when test="${ formField.required }">
                            class="validate(positiveIntegerLessThanOneMillion)"
                        </c:when>
                        <c:otherwise>
                            class="validate(positiveIntegerLessThanOneMillionOrBlank)"
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${ formField.record != null}">
                        <c:choose>
                            <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"number\") %>">
                                value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"number\") %>"/>"
                            </c:when>
                            <c:when test="${ formField.record != null }">
                                value="<c:out value="${ formField.record.number }"/>"
                            </c:when>
                        </c:choose>
                    </c:if>
                />
            </c:when>
            <c:otherwise>
                <span><c:out value="${ formField.record.number }"/></span>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="<%= RecordPropertyType.ACCURACY.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">
                
                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"accuracyInMeters\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"accuracyInMeters\") %>"/>
                    </p>
                </c:if>
                <input id="accuracyInMeters" type="text" name="${ formPrefix }accuracyInMeters"
                     <c:choose>
                            <c:when test="${ not formField.required }">
                                class="validate(numberOrBlank)"
                            </c:when>
                            <c:otherwise>
                                class="validate(number)"
                            </c:otherwise>
                        </c:choose>
                    <c:if test="${ formField.record != null}">
                        <c:choose>
                            <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"accuracyInMeters\") %>">
                                value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"accuracyInMeters\") %>"/>"
                            </c:when>
                            <c:when test="${ formField.record != null }">
                                value="<c:out value="${ formField.record.accuracyInMeters }"/>"
                            </c:when>
                        </c:choose>
                    </c:if>
                />
            </c:when>
            <c:otherwise>
                <span><c:out value="${ formField.record.accuracyInMeters }"/></span>
            </c:otherwise>
        </c:choose>
          
    </c:when>

    <c:when test="<%= RecordPropertyType.GPS_ALTITUDE.getName().equals(formField.getPropertyName()) %>">
        <c:choose>
            <c:when test="${fieldEditable}">

                <c:if test="<%= errorMap != null && errorMap.containsKey(formField.getPrefix()+\"gpsAltitude\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formField.getPrefix()+\"gpsAltitude\") %>"/>
                    </p>
                </c:if>
                <input id="gpsAltitude" type="text" name="${ formPrefix }gpsAltitude"
                     <c:choose>
                            <c:when test="${ not formField.required }">
                                class="validate(numberOrBlank)"
                            </c:when>
                            <c:otherwise>
                                class="validate(number)"
                            </c:otherwise>
                        </c:choose>
                    <c:if test="${ formField.record != null}">
                        <c:choose>
                            <c:when test="<%= valueMap != null && valueMap.containsKey(formField.getPrefix()+\"gpsAltitude\") %>">
                                value="<c:out value="<%= valueMap.get(formField.getPrefix()+\"gpsAltitude\") %>"/>"
                            </c:when>
                            <c:when test="${ formField.record != null }">
                                value="<c:out value="${ formField.record.gpsAltitude }"/>"
                            </c:when>
                        </c:choose>
                    </c:if>
                />
            </c:when>
            <c:otherwise>
                <span><c:out value="${ formField.record.gpsAltitude }"/></span>
            </c:otherwise>
        </c:choose>
    </c:when>

    <c:when test="<%= RecordPropertyType.CREATED.getName().equals(formField.getPropertyName()) %>">
        <c:if test="${not empty record.createdAt}">
            <fmt:formatDate dateStyle="MEDIUM" type="date" value="${record.createdAt}"/> <fmt:formatDate pattern="HH:mm" value="${record.createdAt}"/> by <c:out value="${record.user.fullName}"/>
        </c:if>
    </c:when>
    <c:when test="<%= RecordPropertyType.UPDATED.getName().equals(formField.getPropertyName()) %>">
        <c:if test="${not empty record.updatedAt && not empty updatedBy}">
            <fmt:formatDate dateStyle="MEDIUM" type="date" value="${record.updatedAt}"/> <fmt:formatDate pattern="HH:mm" value="${record.updatedAt}"/> by <c:out value="${updatedBy.fullName}"/>
        </c:if>
    </c:when>
</c:choose>
