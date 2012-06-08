<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<h1>Admin Menu</h1>

<p>This is where you can manage people, projects, data, and the BDRS system.</p>

<ul>
    <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_ROOT,ROLE_SUPERVISOR">
	    <li>
	        <a href="http://localhost:8080/BDRS/bdrs/admin/managePeople.htm">Manage People</a>
	        - Click here to edit users and user groups or to approve user accounts or email users.
	    </li>
    </sec:authorize>
    <li>
        <a href="http://localhost:8080/BDRS/bdrs/admin/manageProjects.htm">Manage Projects</a>
        - Click here to add a new project, edit an existing project or manage census methods.
    </li>
    <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_ROOT">
	    <li>
	        <a href="http://localhost:8080/BDRS/bdrs/admin/manageData.htm">Manage Data</a>
	        - Click here to manage data preferences and thresholds or to download data and species pages.
	    </li>
	    <li>
	        <a href="http://localhost:8080/BDRS/bdrs/admin/managePortal.htm">Manage Portal</a>
	        - Click here to manage portal preferences, themes, content, taxonomy, maps, files, and widgets.
	    </li>
    </sec:authorize>
    <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_ROOT,ROLE_SUPERVISOR,ROLE_POWER_USER">
	    <li>
	        <a href="http://localhost:8080/BDRS/bdrs/admin/embeddedWidgets.htm">Embedded Widgets</a>
	        - Click here to create embedded widgets and create/edit image galleries for 'Image Slideshow' widgets.
	    </li>
    </sec:authorize>
</ul>

<p>If you need help with any of the options, please consult the <a href="javascript:bdrs.underDev();">Administration Guide</a>.</p>