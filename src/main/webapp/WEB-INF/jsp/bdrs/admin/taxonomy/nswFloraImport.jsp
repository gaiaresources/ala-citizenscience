<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>NSW Flora Import</h1>

<div class="input_container">

	<p>
		Required format: <a href="http://en.wikipedia.org/wiki/Comma-separated_values">CSV</a>
	</p>
	<p>
		Save your NSW Flora spreadsheet as a CSV file and upload it here.
	</p>
	<form method="POST" enctype="multipart/form-data" action="${pageContext.request.contextPath}/bdrs/admin/taxonomy/taxonLibImport.htm">
	    <input type="hidden" name="importSource" value="NSW_FLORA" />
	    <table class="form_table">
	        <tbody>
	            <tr>
	                <th>
	                    <label class="strong" for="taxonomySearch">
	                        NSW Flora file:
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
	</form>
</div>

