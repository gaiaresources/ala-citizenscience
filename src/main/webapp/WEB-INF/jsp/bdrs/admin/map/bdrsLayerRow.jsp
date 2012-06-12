<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<tiles:useAttribute name="bdrsLayer" classname="au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer" ignore="true"/>

<c:set var="layerId" value="${ bdrsLayer.layer.id }"/>
<c:set var="layerOnMapCheckboxId" value="bdrs_selected_${ layerId }" />
<c:set var="visibleOnLoadCheckboxId" value="bdrs_visible_${ layerId }" />

<tr>
    <td class="drag_handle">
        
        <input type="hidden" value="${ bdrsLayer.weight }" class="sort_weight" name="weight_${ bdrsLayer.layer.id }"/>
        <input type="hidden" value="${ bdrsLayer.layer.id }" name="bdrsLayer">
        
    </td>
    <td class="textcenter checkColumn">
        
        <input id="${ layerOnMapCheckboxId }"
         type="checkbox"
         name="bdrs_selected_${ layerId }"
        <c:if test="${ bdrsLayer.showOnMap }">
            checked="true"
        </c:if>
        />
        
    </td>
    <td class="textcenter checkColumn">
        
        <input id="${ visibleOnLoadCheckboxId }"
         type="checkbox"
         name="bdrs_visible_${ layerId }"
        <c:if test="${ bdrsLayer.visible }">
            checked="true"
        </c:if>
        />
        
        <script type="text/javascript">
            jQuery(function() {
				var layerOnMapElem = jQuery("#${ layerOnMapCheckboxId }");
				layerOnMapElem.change(function() {
					jQuery("#${ visibleOnLoadCheckboxId }").prop("disabled", !layerOnMapElem.prop("checked"));
				});
				layerOnMapElem.change();
			});
        </script>
    </td>
    <td class="textcenter">
        <span>
            ${ bdrsLayer.layer.name }
        </span>
    </td>
    <td class="textcenter">
        <span>
            ${ bdrsLayer.layer.description }
        </span>
    </td>
</tr>
