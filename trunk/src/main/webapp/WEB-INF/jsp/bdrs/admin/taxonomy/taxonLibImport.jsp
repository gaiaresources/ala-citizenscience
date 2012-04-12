<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>
    Import Taxon Information From Files
</h1>
<h3 style="color:red">
    WARNING
</h3>
<div>
    <p>
        These updates can take hours to run (up to 10 hours for very large datasets).
        Once the update has started it cannot be stopped.
        Your taxonomic data may also be inconsistent
        while the update is running.
    </p>
</div>

<ul>
    <li>
        <a href="${pageContext.request.contextPath}/bdrs/admin/taxonomy/nswFloraImport.htm">
            NSW Flora
        </a>
    </li>
    <li>
        <a href="${pageContext.request.contextPath}/bdrs/admin/taxonomy/maxImport.htm">
            Max
        </a>
    </li>
    <li>
        <a href="${pageContext.request.contextPath}/bdrs/admin/taxonomy/afdImport.htm">
            AFD - Australian Faunal Directory
        </a>
    </li>
</ul>