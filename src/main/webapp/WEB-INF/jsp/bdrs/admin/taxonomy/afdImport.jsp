<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>AFD (Australian Faunal Directory) Import</h1>

<div class="input_container">
    <p>
        Required format: <a href="http://en.wikipedia.org/wiki/ZIP_(file_format)">Zip</a> file as provided by
        the <a href="http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/index.html">Australian Faunal Directory website</a>.
    </p>
    <form method="POST" enctype="multipart/form-data"
        action="${portalContextPath}/bdrs/admin/taxonomy/taxonLibImport.htm"
	    target="iframe_target">

        <input type="hidden" name="importSource" value="AFD"/>
        <table class="form_table">
            <tbody>
                <tr>
                    <th></th>
                    <td>
                        <input class="vertmiddle" type="checkbox" name="download" id="download" checked="checked"/>
                        <label class="vertmiddle" for="download">
                            Retrieve latest list from the
                            <a href="http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/index.html">AFD</a>
                        </label>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label for="lowerTaxa">Lower Taxa:</label>
                    </th>
                    <td>
                        <input type="file" name="lowerTaxa" id="lowerTaxa" class="afdZipUpload" disabled="disabled"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label for="higherTaxa">Higher Taxa:</label>
                    </th>
                    <td>
                        <input type="file" name="higherTaxa" id="higherTaxa" class="afdZipUpload" disabled="disabled"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label for="chordata">Chordata:</label>
                    </th>
                    <td>
                        <input type="file" name="chordata" id="chordata" class="afdZipUpload" disabled="disabled"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label for="insecta">Insecta:</label>
                    </th>
                    <td>
                        <input type="file" name="insecta" id="insecta" class="afdZipUpload" disabled="disabled"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label for="mollusca">Mollusca:</label>
                    </th>
                    <td>
                        <input type="file" name="mollusca" id="mollusca" class="afdZipUpload" disabled="disabled"/>
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

		jQuery('#download').change(function(ev) {
		    var downloadFromAFD = jQuery('#download:checked').length > 0;
		    jQuery(".afdZipUpload").prop('disabled', downloadFromAFD);
		});
	});
</script>