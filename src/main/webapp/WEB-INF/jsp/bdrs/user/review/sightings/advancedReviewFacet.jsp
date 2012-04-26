<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<tiles:useAttribute name="facet" classname="au.com.gaiaresources.bdrs.service.facet.Facet" ignore="true"/>

<div class="facet">
    
	<h4 class="${ facet.inputName }Header">
	   <%--
	   <div class='tree_node <c:choose><c:when test="${ !facet.containsSelected }">tree_node_collapsed</c:when><c:otherwise>tree_node_expanded</c:otherwise></c:choose>'></div>
       --%>
       <span class="left">
	       <div class='${ facet.inputName }_tree_node_handler cursorPointer tree_node <c:choose><c:when test="${ facet.displayName == 'Species Group' }">tree_node_collapsed</c:when><c:otherwise>tree_node_expanded</c:otherwise></c:choose>'></div>
	       <span class="${ facet.inputName }_tree_node_handler cursorPointer">
		       <c:out value="${ facet.displayName }"/>
	       </span>
       </span>
	   
	   <div class="select_all right">
	       <input id="${ facet.inputName }_select_all"
               class="vertmiddle"
               type="checkbox"
               <c:if test="${ facet.allSelected }">
                   checked="checked"
               </c:if>
	       />
	       <label class="vertmiddle" for="${ facet.inputName }_select_all">Select All</label>
       </div>
       
       <div class="clear"></div>
    </h4>
    
	<div class="facetOptions ${ facet.inputName  }OptContainer"
	    <%-- Open the facet if it contains a selected option --%>
	    <%-- c:if test="${ !facet.containsSelected }"> style="display:none"</c:if --%>
	    <c:if test="${ facet.displayName == 'Species Group' }"> style="display:none"</c:if>
	>
		<c:forEach var="facetOption" items="${ facet.facetOptions }" varStatus="status">
		    <div
                <c:if test="${status.count > facet.defaultVisibleOptionCount}">
                    class="overflow" style="display:none;"
                    <c:set var="overflow" value="true"/>
                </c:if>
            >
		        <input id="${ facet.inputName  }_${ facetOption.value }"
                    type="checkbox" 
                    value="${ facetOption.value }" 
                    name="${ facet.optionsParameterName  }"
                    <c:if test="${ facetOption.selected }">
                        checked="checked"
                    </c:if>
                />
		        <label for="${ facet.inputName  }_${ facetOption.value }">
		            <c:out value="${ facetOption.displayName }"/>&nbsp;(<c:out value="${ facetOption.count }"/>)
		        </label>
		    </div>
		</c:forEach>
        <c:if test="${not empty overflow}">
            <div class="showMore"><a href="javascript:void(0);">+ show more</a></div>
            <div class="showLess" style="display:none;"><a href="javascript:void(0);">- show less</a></div>
            <input type="hidden" name="${facet.expandedParameterName}" value="expanded"
                   <c:if test="${not facet.expanded}"> disabled="disabled"</c:if>
            />
        </c:if>

    </div>
</div>
