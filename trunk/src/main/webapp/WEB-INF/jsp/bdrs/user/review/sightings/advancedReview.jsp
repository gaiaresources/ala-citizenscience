<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<tiles:useAttribute name="viewType" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="resultsType" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="resultCount" ignore="true"/>

<!-- for handling the page description in theme -->
<c:if test="${not empty pageDescription}">
    ${pageDescription}
</c:if>

<form id="facetForm" method="POST" action="">
    <c:if test="${resultsType == 'location' }">
        <input type="hidden" name="locations" id="locations" value = "${ locations }"/>
        <input type="hidden" name="locationArea" id="locationArea" value = "${ locationArea }"/>
    </c:if>
    <input type="hidden" name="recordId" value = "${ recordId }"/>
    <div class="alaSightingsContent">
        <div class="facetCol left">
            <div class="columnBanner">Refine results for</div>
            <c:forEach var="facet" items="${ facetList }">
                <jsp:useBean id="facet" type="au.com.gaiaresources.bdrs.service.facet.Facet" ></jsp:useBean>
                <c:if test="<%= facet.isActive() %>">
                    <tiles:insertDefinition name="advancedReviewFacet">
                        <tiles:putAttribute name="facet" value="${ facet }"/>
                    </tiles:insertDefinition>
                </c:if>
            </c:forEach>
        </div>
        <div class="resultCol right">
            <div class="columnBanner">
               <span id="count">
                   <c:out value="${ resultCount }"/>&nbsp;${resultsType}<c:if test="${ recordCount != 1 }">s</c:if><span id="imageCountPlaceholder"></span>&nbsp;returned
               </span>
            </div>
            
            <div class="tabPane">
                <div class="controlPanel">
                    <div>
                        <span class="searchContainer">
                            <input type="text" id="searchText" name="searchText" placeholder="Search within results" value="<c:out value="${ searchText }"/>"/>
                            <input id="searchButton" class="form_action" type="submit" name="facetUpdate" value="Search"/>
                        </span>
                        <span class="tabContainer right">
                            <input type="hidden" name="viewType"
                                <c:choose>
                                    <c:when test="${ tableViewSelected }">
                                       value="table"
                                    </c:when>
                                    <c:when test="${ downloadViewSelected }">
                                       value="download"
                                    </c:when>
                                    <c:when test="${ imagesViewSelected }">
                                        value="images"
                                    </c:when>
                                    <c:otherwise>
                                       value="map"
                                   </c:otherwise>
                                </c:choose> 
                            />
                            <a id="downloadViewTab" href="javascript: void(0);">
                                <div id="downloadViewTab" class="displayTab right <c:if test="${ downloadViewSelected }">displayTabSelected</c:if>">Download</div>
                            </a>
                            <a id="listViewTab" href="javascript: void(0);">
                               <div class="displayTab right <c:if test="${ tableViewSelected }">displayTabSelected</c:if>">List</div>
                            </a>
                            <a href="javascript: void(0);">
                               <div id="mapViewTab" class="displayTab right <c:if test="${ mapViewSelected }">displayTabSelected</c:if>">Map</div>
                            </a>
                            <c:if test="${resultsType == 'record'}">
                                <a href="javascript: void(0);">
                                    <div id="imagesViewTab" class="displayTab right <c:if test="${imagesViewSelected}">displayTabSelected</c:if>">Images</div>
                                </a>
                            </c:if>
                        </span>
                        <div class="clear"></div>
                    </div>
                </div>
                
                <c:choose>
                   
                   <c:when test="${ tableViewSelected }">
                       <tiles:insertDefinition name="advancedReviewTableView">
                            <tiles:putAttribute name="tableColumns" value="${ tableColumns }"/>
                            <tiles:putAttribute name="resultsType" value="${ resultsType }"/>
                       </tiles:insertDefinition>
                   </c:when>
                   <c:when test="${ downloadViewSelected }">
                           <tiles:insertDefinition name="downloadSightingsWidget" />

                           <!-- Reports -->
                           <sec:authorize ifAnyGranted="ROLE_USER, ROLE_POWER_USER, ROLE_SUPERVISOR, ROLE_ADMIN">
                               <c:if test="${ not empty reportList }">
                                <h3>Reports</h3>
                                <p>Reports provide you with a way to create graphs, charts and views of the data in the system.</p>
                                <c:forEach var="report" items="${ reportList }">
                                    <div>
                                        <a id="report_${ report.id }" href="javascript: bdrs.advancedReview.renderReport('#facetForm', ${ report.id })"/>
                                            <c:out value="${ report.name }"/>
                                            &nbsp;-&nbsp;
                                            <c:out value="${ report.description }"/>
                                        </a>
                                    </div>
                                </c:forEach>
                            </c:if>
                        </sec:authorize>
                   </c:when>
                    <c:when test="${imagesViewSelected}">
                        <tiles:insertDefinition name="advancedReviewImagesView" />
                    </c:when>
                    <c:otherwise>
                        <tiles:insertDefinition name="advancedReviewMapView">
                            <tiles:putAttribute name="recordId" value="${ recordId }"/>
                        </tiles:insertDefinition>
                   </c:otherwise>
                </c:choose>
            </div> 
        </div>
        <div class="clear"></div>
    </div>
</form>

<script type="text/javascript">
   jQuery(function() {
       bdrs.form.addPlaceholderSupport();
       // Insert click handlers to show and hide facet options
       <c:forEach var="facet" items="${ facetList }">
           jQuery(".${ facet.inputName }_tree_node_handler").click(function() {
               jQuery(".${ facet.inputName }OptContainer").slideToggle("fast", function() {
                   var collapsed = jQuery(".${ facet.inputName }OptContainer").css("display") === "none";
                   var treeNode = jQuery(".${ facet.inputName }Header .tree_node");
                   if(collapsed) {
                       treeNode.removeClass('tree_node_expanded');
                   } else {
                       treeNode.addClass('tree_node_expanded');
                   }
               });
           });
      </c:forEach>
      
      bdrs.advancedReview.initFacets('#facetForm', '.facet');
      bdrs.advancedReview.initTabHandlers();
   });

   <c:if test="${ mapViewSelected }">
   jQuery(window).load(function() {
      var initMapLayersFcn = <tiles:insertDefinition name="initMapLayersFcn">
                            <tiles:putAttribute name="webMap" value="${webMap}"/>
                        </tiles:insertDefinition>;    
        
      // only init the map view if the map view is selected
      bdrs.advancedReview.initMapView('#facetForm',  
              'atlasSightingsMap', { geocode: { selector: '#geocode' }}, '#recordId', initMapLayersFcn);
      
   });
       <tiles:insertDefinition name="initBaseMapLayersFcn">
        <tiles:putAttribute name="webMap" value="${webMap}" />
    </tiles:insertDefinition>
   </c:if>
</script>
