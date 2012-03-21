<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<tiles:useAttribute name="bdrsLayer" classname="au.com.gaiaresources.bdrs.model.map.GeoMapLayer" ignore="true"/>

<tr>
  <td class="drag_handle">
    <input type="hidden" value="${ bdrsLayer.weight }" class="sort_weight" name="weight_${ bdrsLayer.layer.id }"/>
    <input type="hidden" value="${ bdrsLayer.layer.id }" name="bdrsLayer">
  </td>
  <td class="textcenter checkColumn">
    <input id="bdrs_selected_${ bdrsLayer.layer.id }" 
           type="checkbox" 
           name="bdrs_selected_${ bdrsLayer.layer.id }"
      <c:if test="${ bdrsLayer.showOnMap }">checked="true"</c:if>
    />
  </td>
  <td class="textcenter">
    <span>${ bdrsLayer.layer.name }</span>
  </td>
  <td class="textcenter">
    <span>${ bdrsLayer.layer.description }</span>
  </td>
</tr>