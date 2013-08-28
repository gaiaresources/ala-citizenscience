<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>NSW Fauna Import</h1>

<div class="input_container">

	<p>
		Required format: <a href="http://en.wikipedia.org/wiki/Comma-separated_values">CSV</a>
	</p>
	<p>
		Save your NSW Fauna spreadsheet as a CSV file and upload it here.
	</p>
	<form method="POST" enctype="multipart/form-data" action="${portalContextPath}/bdrs/admin/taxonomy/taxonLibImport.htm"
	target="iframe_target">
	    <input type="hidden" name="importSource" value="NSW_FAUNA" />
	    <table class="form_table">
	        <tbody>
	            <tr>
	                <th>
	                    <label class="strong" for="taxonomySearch">
	                        NSW Fauna file:
	                    </label>
	                </th>
	                <td>
	                    <input class="validate(required)" type="file" name="taxonomyFile" id="taxonomyFile"/>
	                </td>
	            </tr>
	        </tbody>
	    </table>
	    <div id="buttonPanelBottom" class="buttonpanel textright">
	        <input class="form_action" type="submit" />
	    </div>
		<iframe id="iframe_target" name="iframe_target" src="" class="hidden"></iframe>
    </form>
</div>
<script type="text/javascript">
	jQuery(function() {
		jQuery('form').bind('onKetchup', function(ev, isTasty) {
			if (isTasty) {
				bdrs.message.set("Taxonomy import started. Please do not start another Import until you have received an import completion email.");
			}
		});
	});
</script>

