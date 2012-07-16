<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@page import="au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>

<tiles:useAttribute name="isLatitude" ignore="true"/>
<tiles:useAttribute name="isLongitude" ignore="true"/>
<tiles:useAttribute name="isZone" ignore="true"/>
<%-- The CRS of the survey --%>
<tiles:useAttribute name="crs" classname="au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem" />

<tiles:useAttribute name="errorMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="valueMap" classname="java.util.Map" ignore="true"/>

<tiles:useAttribute name="formPrefix" ignore="true" />
<tiles:useAttribute name="editEnabled" ignore="true" />
<tiles:useAttribute name="required" ignore="true" />

<%-- inputs set to 'readonly' --%>
<tiles:useAttribute name="readOnly" classname="java.lang.Boolean" ignore="true"/>

<tiles:useAttribute name="xCoord" ignore="true" />
<tiles:useAttribute name="yCoord" ignore="true" />
<%-- The CRS of the geometry object --%>
<tiles:useAttribute name="selectedSrid" classname="java.lang.Integer" ignore="true" />

<c:set var="hasGeom" value="${ not empty xCoord and not empty yCoord and not empty selectedSrid }" />

<%-- put the global 'edit form' bool into a local variable... --%>
<c:set var="fieldEditable" value="${editEnabled}"></c:set>

<c:if test="${ formPrefix == null }">
    <%-- set formPrefix to the passed value --%>
    <c:set var="formPrefix" value=""></c:set>
</c:if>

<c:set var="xDisplayValue" value="<%= xCoord != null ? xCoord : \"\" %>" />
<c:set var="yDisplayValue" value="<%= yCoord != null ? yCoord : \"\" %>" />

<c:set var="selectedCrs" value="<%= BdrsCoordReferenceSystem.getBySRID(selectedSrid) != null ? BdrsCoordReferenceSystem.getBySRID(selectedSrid) : crs  %>" />

<c:choose>
    <c:when test="${fieldEditable}">
        <c:choose>
            <c:when test="${ isZone }">
                <select id="srid" name="${ formPrefix }srid">
                    <%
                    	Boolean hasGeom = (Boolean)pageContext.getAttribute("hasGeom");
                        String zoneKey = formPrefix+"srid";
                        BdrsCoordReferenceSystem selectedCrs = null;
                        if (valueMap != null && valueMap.containsKey(zoneKey)) {
                            String selectedCrsString = (String)valueMap.get(zoneKey);
                            selectedCrs = BdrsCoordReferenceSystem.getBySRID(Integer.valueOf(selectedCrsString));
                        } else if (hasGeom) {
                            selectedCrs = BdrsCoordReferenceSystem.getBySRID(selectedSrid);
                        }
                        
                        // if the valid zones does not contain our selected CRS, add it to the start of the list.
                        // make a copy of the list
                        List<BdrsCoordReferenceSystem> validZones = new ArrayList<BdrsCoordReferenceSystem>();
                        if (crs.isZoneRequired()) {
                        	validZones.addAll(crs.getZones());	
                        } else {
                        	validZones.add(crs);
                        }
                        
                        // add if required
                        if (selectedCrs != null && !validZones.contains(selectedCrs)) {
                        	validZones.add(0, selectedCrs);
                        }
                        
                        for (BdrsCoordReferenceSystem c : validZones) {
                            out.print("<option value=\"");
                            out.print(c.getSrid());
                            out.print("\" ");
                            if (selectedCrs != null && c != null) {
                                if (selectedCrs.getSrid() == c.getSrid()) {
                                    out.print("selected=\"selected\"");
                                }
                            }
                            out.print(">");
                            out.print(c.getDisplayName());
                            out.println("</option>");
                        }
                    %>
                </select>
            </c:when>
            <c:when test="${ isLatitude }">
                <c:if test="<%= errorMap != null && errorMap.containsKey(formPrefix+\"latitude\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formPrefix+\"latitude\") %>"/>
                    </p>
                </c:if>
                <input id="latitude" type="text" name="${ formPrefix }latitude" class="validate(numberOrBlank
                    <c:if test="${ not empty selectedCrs.minY and not empty selectedCrs.maxY }">,rangeOrBlank(${ selectedCrs.minY },${ selectedCrs.maxY })</c:if>
                    <c:if test="${ required }">,required</c:if>
                    )"
                    <c:choose>
                        <c:when test="<%= valueMap != null && valueMap.containsKey(formPrefix+\"latitude\") %>">
                            value="<c:out value="<%= valueMap.get(formPrefix+\"latitude\") %>"/>"
                        </c:when>
                        <c:otherwise>
                            value="${ yDisplayValue }"
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${ readOnly }">
                        readonly="readonly"
                    </c:if>
                />
            </c:when>
            <c:when test="${ isLongitude }">
                <c:if test="<%= errorMap != null && errorMap.containsKey(formPrefix+\"longitude\") %>">
                    <p class="error">
                        <c:out value="<%= errorMap.get(formPrefix+\"longitude\") %>"/>
                    </p>
                </c:if>
                <input id="longitude" type="text" name="${ formPrefix }longitude" class="validate(numberOrBlank
                    <c:if test="${ not empty selectedCrs.minX and not empty selectedCrs.maxX }">,rangeOrBlank(${ selectedCrs.minX },${ selectedCrs.maxX })</c:if>
                    <c:if test="${ required }">,required</c:if>
                    )"
                    <c:choose>
                        <c:when test="<%= valueMap != null && valueMap.containsKey(formPrefix+\"longitude\") %>">
                            value="<c:out value="<%= valueMap.get(formPrefix+\"longitude\") %>"/>"
                        </c:when>
                        <c:otherwise>
                            value="${ xDisplayValue }"
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${ readOnly }">
                        readonly="readonly"
                    </c:if>
                />
            </c:when>
        </c:choose>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${ isZone }">
                <span><c:out value="${ selectedCrs.displayName }"/></span>
                <input type="hidden" name="${formPrefix}srid" value="${ selectedSrid }"/>
            </c:when>
            <c:when test="${ isLatitude }">
                <span><c:out value="${ yDisplayValue }" /></span>
                <input type="hidden" name="${formPrefix}latitude" value="${ yDisplayValue }" />
            </c:when>
            <c:when test="${ isLongitude }">
                <span><c:out value="${ xDisplayValue }" /></span>
                <input type="hidden" name="${formPrefix}longitude" value="${ xDisplayValue }" />
            </c:when>
        </c:choose>
    </c:otherwise>
</c:choose>