<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>AFD (Australian Faunal Directory) Import</h1>

<div class="input_container">
    <p>
        Required format: <a href="http://en.wikipedia.org/wiki/Comma-separated_values">CSV</a>
    </p>
	<p>
		See the <a href="http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/index.html">Australian Faunal Directory website</a>.
	</p>
    <p>
        The AFD has several files that already come in CSV format. You may upload each file individually or concatenate
        the CSV files (not including the headers) to do the upload in one http request.
    </p>
    <form method="POST" enctype="multipart/form-data" action="${pageContext.request.contextPath}/bdrs/admin/taxonomy/taxonLibImport.htm"
	target="iframe_target">
        <input type="hidden" name="importSource" value="AFD"/>
        <table class="form_table">
            <tbody>
                <tr>
                    <th>
                        <label class="strong" for="taxonomySearch">
                            AFD File:
                        </label>
                    </th>
                    <td>
                        <input class="validate(required)" type="file" name="taxonomyFile" id="taxonomyFile"/>
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