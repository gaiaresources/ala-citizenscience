<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Max Import</h1>

<div class="input_container">
    <p>
        Required format: <a href="http://en.wikipedia.org/wiki/Comma-separated_values">CSV</a>
    </p>
	<p>
		This importer only supports loading of the flora data from Max.
	</p>
    <p> 
        The Max flora database has 4 tables that we need to export as CSV after which we can upload the files here.
    </p>
    <form method="POST" enctype="multipart/form-data" action="${portalContextPath}/bdrs/admin/taxonomy/taxonLibImport.htm"
          target="iframe_target">
        <input type="hidden" name="importSource" value="MAX"/>
        <table class="form_table">
            <tbody>
                <tr>
                    <th>
                        <label class="strong" for="maxFamilyFile">
                            Max Family file:
                        </label>
                    </th>
                    <td>
                        <input class="validate(required)" type="file" name="maxFamilyFile" id="maxFamilyFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxGeneraFile">
                            Max Genera file:
                        </label>
                    </th>
                    <td>
                        <input class="validate(required)" type="file" name="maxGeneraFile" id="maxGeneraFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxNameFile">
                            Max Name file:
                        </label>
                    </th>
                    <td>
                        <input class="validate(required)" type="file" name="maxNameFile" id="maxNameFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxXrefFile">
                            Max XRef file:
                        </label>
                    </th>
                    <td>
                        <input class="validate(required)" type="file" name="maxXrefFile" id="maxXrefFile"/>
                    </td>
                </tr>
            </tbody>
        </table>
        <div id="buttonPanelBottom" class="buttonpanel textright">
            <input class="form_action" type="submit"/>
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