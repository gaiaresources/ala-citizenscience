<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>
    Import Taxon Information From Files
</h1>
<h3 class="warning_header">
    WARNING
</h3>
<div>
	<p>
		Before you run your taxonomy import, make sure your email is correctly set in your
		<a href="${pageContext.request.contextPath}/user/editProfile.htm">profile</a>.
	</p>
	<p>
		An email will be sent to you to report the outcome of your taxonomy import.
	</p>
    <p>
        Please be aware that depending upon the server speed this import may take up to several hours to complete.
        During this time the BDRS will be put into maintenance mode and inaccessible to all users except the root user.
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