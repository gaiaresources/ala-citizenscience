<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<tiles:useAttribute name="baseLayer" classname="au.com.gaiaresources.bdrs.model.map.BaseMapLayer" ignore="true"/>

  <c:if test="${ baseLayer.id != null }">
    <input type="hidden" value="${baseLayer.id}" name="id_${ baseLayer.layerSource }"/>
  </c:if>
    <tr>
      <td class="drag_handle">
        <input type="hidden" value="${ baseLayer.weight }" class="sort_weight" name="weight_${ baseLayer.layerSource }"/>
      </td>
      <td class="textcenter checkColumn">
        <input id="selected_${ baseLayer.layerSource }" 
               type="checkbox" 
               name="selected_${ baseLayer.layerSource }"
          <c:if test="${ baseLayer.showOnMap }">checked="true"</c:if>
        />
      </td>
      <td class="textcenter">
        <span>${ baseLayer.layerSource.name }</span>
      </td>
      <td class="textcenter checkColumn">
        <input id="default_${ baseLayer.layerSource }" 
               type="radio" 
               name="default"
               value="${ baseLayer.layerSource }"
               onChange="jQuery('#selected_${ baseLayer.layerSource }').prop('checked', this.checked);"
          <c:if test="${ baseLayer.default }">checked="true"</c:if>
        />
      </td>
    </tr>