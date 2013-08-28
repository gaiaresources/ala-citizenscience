<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<h1>Advanced Review</h1>

<!-- create the columns for the table view -->


<tiles:insertDefinition name="advancedReview">
    <tiles:putAttribute name="resultsType" value="record"/>
    <tiles:putAttribute name="resultCount" value="${ recordCount }"/>
    <tiles:putAttribute name="tableColumns" value="${ tableColumns }"/>
</tiles:insertDefinition>

<script type="text/javascript">
   var tableColumns = [{'sortName' : 'censusMethod.type',
                       'tdClass' : 'typeColumn',
                       'divClass' :  'left alaSightingsTableHeader',
                       'tooltip' : 'The record type',
                       'title' : 'Type'},
                      {'sortName' : 'record.when',
                       'tdClass' : 'dateColumn',
                       'displayClass' : 'dateColumn',
                       'divClass' :  'left alaSightingsTableHeader',
                       'tooltip' : 'The date the record was taken.',
                       'title' : 'Date'},
                      {'sortName' : 'species.scientificName',
                       'tdClass' : 'sciNameColumn',
                       'displayClass' : 'sciNameColumn',
                       'divClass' :  'left alaSightingsTableHeaderDoubleLine',
                       'tooltip' : 'The scientific name of the species recorded.',
                       'title' : 'Scientific<br/>Name'},
                      {'sortName' : 'species.commonName',
                       'tdClass' : 'commonNameColumn',
                       'displayClass' : 'commonNameColumn',
                       'divClass' :  'left alaSightingsTableHeaderDoubleLine',
                       'tooltip' : 'The common name of the species recorded.',
                       'title' : 'Common<br/>Name'},
                      {'sortName' : 'location.name',
                       'tdClass' : 'locationColumn',
                       'displayClass' : 'locationColumn ',
                       'divClass' :  'left alaSightingsTableHeader',
                       'tooltip' : 'The name of the location where the record was taken. If there was no named location, the latitude and longitude where the record was taken.',
                       'title' : 'Location'},
                      {'sortName' : 'record.user',
                       'tdClass' : 'userColumn',
                       'displayClass' : 'userColumn',
                       'divClass' :  'left alaSightingsTableHeader',
                       'tooltip' : 'The user that logged the record.',
                       'title' : 'User'}];
   
   jQuery(function() {
      
      
      <c:if test="${ downloadViewSelected }">
      bdrs.advancedReview.downloadSightingsWidgetInit("#facetForm");
       </c:if>
   });
</script>
