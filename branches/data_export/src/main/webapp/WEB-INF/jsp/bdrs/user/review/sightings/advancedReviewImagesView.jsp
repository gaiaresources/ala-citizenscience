<%@ page import="au.com.gaiaresources.bdrs.service.facet.MultimediaFacet" %>
<%@ page import="au.com.gaiaresources.bdrs.model.taxa.AttributeType" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="math" uri="/WEB-INF/math.tld" %>


<tiles:useAttribute name="sortBy" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="sortOrder" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="resultsPerPage" classname="java.lang.Integer" ignore="true"/>
<tiles:useAttribute name="pageCount" classname="java.lang.Long" ignore="true"/>
<tiles:useAttribute name="pageNumber" classname="java.lang.Long" ignore="true"/>
<tiles:useAttribute name="resultsType" classname="java.lang.String" ignore="true"/>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="showScientificName" value="<%= bdrsPluginFacade.getPreferenceValue(\"taxon.showScientificName\") %>" />

<%-- The view style will default to TABLE in the event of a missing or invalid portal preference value --%>

<div class="alaSightingsImagesViewContent">
    <div class="sortPanel">
        <%-- The results messages will contain any messages about the results on the page
             Currently, this is only used in the Location Review for a message 
             about map selection and a link to clear the map --%>
        <div id="resultsMessages" class="left"></div>
        <div class="right">
            <label for="resultsPerPage">Results per page</label>
            <select id="resultsPerPage" name="resultsPerPage">
                <option <c:if test="${resultsPerPage == 10}">selected="selected"</c:if>>10</option>
                <option <c:if test="${resultsPerPage == 20}">selected="selected"</c:if>>20</option>
                <option <c:if test="${resultsPerPage == 50}">selected="selected"</c:if>>50</option>
                <option <c:if test="${resultsPerPage == 100}">selected="selected"</c:if>>100</option>
            </select>
        </div>
            <div class="left">
                <label for="sortBy">Sort By</label>
                <select id="sortBy" name="sortBy">
                    <c:choose>
                        <c:when test="${showScientificName}">
                            <option value="species.scientificName" <c:if test="${sortBy == 'species.scientificName'}">selected</c:if> >Species Name</option>
                         </c:when>
                         <c:otherwise>
                             <option value="species.commonName" <c:if test="${sortBy == 'species.commonName'}">selected</c:if> >Common Name</option>
                         </c:otherwise>
                    </c:choose>
                    <option value="record.user" <c:if test="${sortBy == 'record.user'}">selected</c:if> >User Name</option>
                    <option value="record.when" <c:if test="${sortBy == 'record.when'}">selected</c:if> >Submit Date</option>

                </select>
            </div>

                <div class="left">
                    <label for="sortOrder">Sort Order</label>
                    <select id="sortOrder" name="sortOrder">
                        <option value="ASC"
                            <c:if test="${ 'ASC' == sortOrder }">
                                selected="selected"
                            </c:if>
                        >
                            ascending
                        </option>
                        <option value="DESC"
                            <c:if test="${ 'DESC' == sortOrder }">
                                selected="selected"
                            </c:if>
                        >
                            descending
                        </option>
                    </select>
                </div>
                


        <div class="clear"></div>
    </div>
    
    <div>
        <ol id="images">

        </ol>
    </div>
</div>
<div class="clear"></div>

<%-- may want to make paginationRange and maxPaginationItems portal prefs in the future --%>
<%-- default number of pages to look for on either side of current page --%>
<c:set var="paginationRange" value="4" />
<%-- maximum number of pages to show including the current page --%>
<c:set var="maxPaginationItems" value="9" />
<%-- min page number to show --%>
<c:set var="minPage" value="${math:max(pageNumber-paginationRange,1)}" />
<%-- max page number to show --%>
<c:set var="maxPage" value="${math:min(minPage+maxPaginationItems-1, pageCount)}" />

<div class="textcenter">
   <div id="searchNavBar">
     <input type="hidden" value="${ pageNumber }" name="pageNumber" id="pageNumber"/>
     <ul>
     <c:choose>
        <c:when test="${ pageNumber <= 1 }">
            <li id="prevPage">&#171;&nbsp;Previous</li>
        </c:when>
        <c:otherwise>
            <li id="prevPage"><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${pageNumber-1});">&#171;&nbsp;Previous</a></li>
        </c:otherwise>
     </c:choose>
     <c:forEach var="i" begin="${minPage}" end="${maxPage}" step="1" varStatus ="status">
         <c:choose>
            <c:when test="${ pageNumber == i }">
                <li class="currentPage"><c:out value="${i}"/></li>
            </c:when>
            <c:otherwise>
                <li><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${i});"><c:out value="${i}"/></a></li>
            </c:otherwise>
         </c:choose>
     </c:forEach>
     <c:choose>
        <c:when test="${ pageNumber == pageCount }">
            <li id="nextPage">Next&nbsp;&#187;</li>
        </c:when>
        <c:otherwise>
            <li id="nextPage"><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${pageNumber+1});">Next&nbsp;&#187;</a></li>
        </c:otherwise>
     </c:choose>
     </ul>
   </div>
</div>

<script type="text/javascript">
    jQuery(function() {
        var resultCount = ' (${imageCount} image<c:if test="${imageCount != 1}">s</c:if>)';

        jQuery('#imageCountPlaceholder').text(resultCount);
        bdrs.advancedReview.initImagesView('#facetForm',
            '#images', '#sortOrder', '#sortBy', '#<%=MultimediaFacet.QUERY_PARAM_NAME+"_"+AttributeType.IMAGE.getCode()%>',
            '#resultsPerPage', ${showScientificName});
    });
</script>
