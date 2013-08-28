<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Edit Preferences</h1>

<cw:getContent key="admin/editPreferences" />

<form method="POST" action="${portalContextPath}/bdrs/admin/preference/preference.htm">

    <div class="buttonpanel textright">
        <input class="form_action" type="submit" value="Save Preferences"/>
    </div>
    <input id="index" type="hidden" value="0"/>
    <c:forEach var="categoryToPrefEntry" items="${ categoryMap }">
        <c:set var="category" value="${ categoryToPrefEntry.key }"/>
        <c:set var="preferenceList" value="${ categoryToPrefEntry.value }"/>
        <div class="preference_category">
            <div id="preferences${ category.id }Container">
                <table>
                    <tr>
                        <td><h2 class="left"><c:out value="${ category.displayName }"/></h2></td>
                        <td valign="middle">
                            <a id="category_toggle_${ category.id }" name="category_toggle_${ category.id }" class="left" href="javascript: void(0);">Show Preferences List</a>
                        </td>
                    </tr>
                </table>
                <p class="clear">
                    <!-- not escaping xml as this text is only settable server side -->
                    <c:out value="${ category.description }" escapeXml="false" />
                </p>
                <div id="preference_category_${ category.id }" name="preference_category_${ category.id }">
                    <div class="textright buttonpanel">
                        <a id="maximiseLink${ category.id }" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink${ category.id }', '#preferences${ category.id }Container', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
                        <input type="button" class="form_action" value="Add <c:out value="${ category.displayName }"/> Preference" onclick="bdrs.preferences.addPreferenceRow( ${ category.id }, '#index', '#category_${ category.id }' );"/>
                    </div>
                    <table id="category_${ category.id }" class="preference_table datatable textjustify">
                        <thead>
                            <tr>
                                <th>Description</th>
                                <th>Key</th>
                                <th>Value</th>
                                <th>Delete</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="pref" items="${ preferenceList }">
                                <tiles:insertDefinition name="preferenceRow">
                                    <tiles:putAttribute name="pref" value="${ pref }"/>
                                </tiles:insertDefinition>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            jQuery(function() {
                jQuery("#category_toggle_${ category.id }").click(function() {
                    jQuery("#preference_category_${ category.id }").slideToggle(function() {
                        var canSee = jQuery("#preference_category_${ category.id }").css('display') === 'none';
                        jQuery("#category_toggle_${ category.id }").text(canSee ? "Show Preferences List" : "Hide Preferences List");
                    });
                });
            });
        </script>
    </c:forEach>
    
    <div class="buttonpanel textright">
        <input class="form_action" type="submit" value="Save Preferences"/>
    </div>
</form>

<script type="text/javascript">
    jQuery(window).load(function() {
        // hide the preferences here so ketchup is rendered in the appropriate position
        jQuery("[name^='preference_category']").css('display', 'none');
    });
</script>
