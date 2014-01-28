<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<jsp:useBean id="maps" scope="request" type="java.util.List" />

<cw:getContent key="public/review"/>

<span id="public_review_screen_description">
    <c:set var="divWidth" value="48"></c:set>
    <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_SUPERVISOR,ROLE_POWER_USER,ROLE_USER">
        <c:set var="divWidth" value="100"></c:set>
        <div class="left" style="width: 48%; padding: 5px;">
            <a href="${portalContextPath}/map/mySightings.htm">
                <h2>My Sightings</h2>
            </a>
            <p>This form shows you your own Sightings, and lets you filter it by a few simple parameters.
            <a href="${portalContextPath}/map/mySightings.htm">Click here</a>
            </p>
            <a href="${portalContextPath}/map/mySightings.htm">
                <img style="width: 100%;" src="${pageContext.request.contextPath}/images/bdrs/review/my_sightings.png">
            </a>
        </div>
    </sec:authorize>
    <div class="left" style="width: 48%; padding: 5px;">
        <a href="${portalContextPath}/review/sightings/advancedReview.htm">
            <h2>Advanced Review</h2>
        </a>
        <p>This form lets you filter your Sightings and other users' public Sightings across a wider range of parameters.
        <a href="${portalContextPath}/review/sightings/advancedReview.htm">Click here</a>
        </p>
        <a href="${portalContextPath}/review/sightings/advancedReview.htm">
            <img style="width: 100%;" src="${pageContext.request.contextPath}/images/bdrs/review/advanced_review.png">
        </a>
    </div>
    <c:if test="<%= maps != null && !maps.isEmpty() %>">
        <div class="left" style="width: ${divWidth}%; padding: 5px;">
            <h2>Map View</h2>
            <p>This form shows a map with predefined sets of sightings on it and can be published publicly.  The maps you can view are:</p>
            <ul>
                <c:forEach items="${maps}" var="map">
                    <li>
                        <a href="${portalContextPath}/bdrs/map/view.htm?geoMapId=${map.id}">${ map.name }</a>
                        <c:if test="${map.description != null}"> - ${map.description}</c:if>
                    </li>
                </c:forEach>
            </ul>
        </div>
    </c:if>
</span>