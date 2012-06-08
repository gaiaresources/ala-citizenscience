<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<tiles:useAttribute name="group" ignore="true"/>
<tr>
    <td>
        <input type="text" class="secondaryGroupSearch" name="secondaryGroupSearch" value="${group.name}"/>
        <input type="text" class="hiddenTextField secondaryGroupId validate(required)" name="secondaryGroups" value="${group.id}"/>
    </td>

    <td class="deleteSecondaryGroup">
        <a href="javascript: void(0);">
            <img width="15" height="15" src="${pageContext.request.contextPath}/images/icons/delete.png" alt="Delete" class="vertmiddle"/>
        </a>

    </td>

</tr>