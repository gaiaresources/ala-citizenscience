<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<h1>Import Taxonomy to TaxonLib</h1>
<h3 style="color:red">WARNING</h3>
<div>
	<p>
		Only click the submit button once or you can crash the server!
	</p>
	<p>
		These updates can take a LONG time, in the hours! During which your server
		will appear to be sluggish. Your taxonomic data may also be inconsistent
		during the the update period.
	</p>
</div>
<div class="input_container">
    <h2>NSW Flora</h2>
	<p>Required format: CSV</p>
	<p>
		Save your NSW Flora spreadsheet as a CSV file and upload it here.
	</p>
    <form method="POST" enctype="multipart/form-data">
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
                        <input type="file" name="taxonomyFile" id="taxonomyFile"/>
                    </td>
                </tr>
            </tbody>
        </table>
        <div id="buttonPanelBottom" class="buttonpanel textright">
            <input class="form_action" type="submit" />
        </div>
    </form>
</div>
<div class="input_container">
    <h2>MAX</h2>
	<p>Required format: CSV</p>
	<p>
		The MAX flora database has 4 tables that we need to export as CSV after which we can upload the files here.
	</p>
    <form method="POST" enctype="multipart/form-data">
    	<input type="hidden" name="importSource" value="MAX" />
        <table class="form_table">
            <tbody>
                <tr>
                    <th>
                        <label class="strong" for="maxFamilyFile">
                            MAX family file:
                        </label>
                    </th>
                    <td>
                        <input type="file" name="maxFamilyFile" id="maxFamilyFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxGeneraFile">
                            MAX Genera file:
                        </label>
                    </th>
                    <td>
                        <input type="file" name="maxGeneraFile" id="maxGeneraFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxNameFile">
                            MAX Name file:
                        </label>
                    </th>
                    <td>
                        <input type="file" name="maxNameFile" id="maxNameFile"/>
                    </td>
                </tr>
                <tr>
                    <th>
                        <label class="strong" for="maxXrefFile">
                            MAX XRef file:
                        </label>
                    </th>
                    <td>
                        <input type="file" name="maxXrefFile" id="maxXrefFile"/>
                    </td>
                </tr>
            </tbody>
        </table>
        <div id="buttonPanelBottom" class="buttonpanel textright">
            <input class="form_action" type="submit" />
        </div>
    </form>
</div>
<div class="input_container">
	<h2>AFD</h2>
	<p>Required format: CSV</p>
	<p>The AFD has several files that already come in CSV format. You may upload each file individually or concatenate
	the CSV files (not including the headers) to do the upload in one http request.
	</p>
    <form method="POST" enctype="multipart/form-data">
        <input type="hidden" name="importSource" value="AFD" />
        <table class="form_table">
            <tbody>
                <tr>
                    <th>
                        <label class="strong" for="taxonomySearch">
                            AFD File:
                        </label>
                    </th>
                    <td>
                        <input type="file" name="taxonomyFile" id="taxonomyFile"/>
                    </td>
                </tr>
            </tbody>
        </table>
        <div id="buttonPanelBottom" class="buttonpanel textright">
            <input class="form_action" type="submit" />
        </div>
    </form>
</div>