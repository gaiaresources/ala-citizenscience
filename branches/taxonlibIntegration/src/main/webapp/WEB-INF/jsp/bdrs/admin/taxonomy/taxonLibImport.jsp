<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<h1>Import Taxonomy to TaxonLib</h1>
<div class="input_container">
    <h2>NSW Flora</h2>
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