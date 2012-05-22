<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<tiles:useAttribute id="customJS" name="customJavaScript" classname="java.util.List"/>
<tiles:useAttribute id="customCSS" name="customCss" classname="java.util.List"/>
<jsp:useBean id="context" scope="request" type="au.com.gaiaresources.bdrs.servlet.RequestContext"></jsp:useBean>
<tiles:importAttribute name="maps"/>

<%@page import="au.com.gaiaresources.bdrs.model.theme.Theme"%>

<!DOCTYPE html>

<html>
    <head>
		<c:choose>
		    <c:when test="${not empty pageTitle}">
		        <title><tiles:getAsString name="siteName"/> | ${pageTitle}</title>
		    </c:when>
		    <c:otherwise>
		        <title><tiles:getAsString name="siteName"/> | <tiles:getAsString name="title"/></title>
		    </c:otherwise>
		</c:choose>

        <c:set var="portalContextPath" scope="request" value="${pageContext.request.contextPath}${currentPortal.portalContextPath}"/>

        <meta name="keywords" content="<tiles:getAsString name="metaKeywords"/>"/>
        <meta name="description" content="<tiles:getAsString name="metaDescription"/>"/>
        
        <!-- Reset all browser specific styles regardless of theming -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-reset.css" type="text/css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-fonts.css" type="text/css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-base.css" type="text/css">

        <!-- Include the BDRS default layout styles -->
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/bdrs/bdrs.css" type="text/css"/>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/ketchup-0.3.2/jquery.ketchup.css" type="text/css"/>
        
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery-1.6.2.min.js"></script>

		<!-- custom css goes in before theming css, allows theme to override custom css styling if required -->
        <c:forEach var="cssFile" items="${customCSS}">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/${cssFile}" type="text/css"/>
        </c:forEach>
		        
        <link rel="stylesheet" href="${pageContext.request.contextPath}/js${version}/colorpicker/css/colorpicker.css" type="text/css"/>
        <script src="${pageContext.request.contextPath}/js${version}/colorpicker/js/colorpicker.js" type="text/javascript"></script>
        
        <%--<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.js"></script>--%>
        <%--<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.messages.js"></script>--%>
        <%--<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.validations.basic.js"></script>--%>

        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup-0.3.2/jquery.ketchup.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.messages.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup-0.3.2/jquery.ketchup.validations.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup-0.3.2/scaffold.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup-0.3.2/jquery.ketchup.helpers.js"></script>


        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery.cj-simple-slideshow.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery.tablednd_0_5.js"></script>
		
		<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/state-machine.min.js"></script>
		
		<%-- see http://www.timdown.co.uk/jshashtable/index.html --%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jshashtable/jshashtable-2.1.js"></script>
		
		<%-- 
		    ninja date parsing, http://www.datejs.com/
			Note we are using the australian version. May want to make this
			part of the theme.
		--%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/date/date-en-AU.js"></script>
        
        <!--  JqGrid stuff the grid.local-en.js file needs t come before the jqGrid.min.js file -->
        <link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/js/jquery.jqGrid-4.0.0/css/ui.jqgrid.css" />
        <script src="${pageContext.request.contextPath}/js${version}/jquery.jqGrid-4.0.0/js/i18n/grid.locale-en.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js${version}/jquery.jqGrid-4.0.0/js/jquery.jqGrid.min.js" type="text/javascript"></script>
        
        <script src="${pageContext.request.contextPath}/js${version}/jquery-tmpl/jquery.tmpl.js" type="text/javascript"></script>
                
        <c:if test="${maps == true}">
            <script src="${pageContext.request.contextPath}/js${version}/ol/OpenLayers.js" type="text/javascript"></script>
            <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;&amp;sensor=false&amp;key=${bdrsGoogleMapsKey}" type="text/javascript"></script>
            <script src="${pageContext.request.contextPath}/js${version}/BdrsCluster.js" type="text/javascript"></script>
        </c:if>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/bdrs.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery.jcollapsible.min.js"></script>


        <!-- Theme css and js file includes -->
        <c:if test="${ theme != null }">
            <c:forEach items="${ theme.cssFiles }" var="cssFile">
	            <link rel="stylesheet" href="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/${ cssFile }" type="text/css">
	        </c:forEach>
	        <c:forEach items="${ theme.jsFiles }" var="jsFile">
	            <script type="text/javascript" src="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/${ jsFile }"></script>
	        </c:forEach>
        </c:if>
		
		<!-- IE7 specific styles and hard coded IE7 file in theme -->
        <!--[if IE 7]>
		    <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/bdrs/bdrs-ie7.css" type="text/css"/>
            <link rel="stylesheet" href="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/css/base-ie7.css" type="text/css">
        <![endif]-->
		
		<!-- Hard coded IE8 file in theme -->
        <!--[if IE 8]>
		    <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/bdrs/bdrs-ie8.css" type="text/css"/>
            <link rel="stylesheet" href="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/css/base-ie8.css" type="text/css">
        <![endif]-->
        
		<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery-blockui/jquery.blockUI.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jqPrint/jquery.jqprint.0.3.js"></script>
		<script src="${pageContext.request.contextPath}/js${version}/jquery.ui.autocomplete.html.js" type="text/javascript"></script>
		
		<%-- time picker --%>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/timepicker/jquery-ui-timepicker-addon.js"></script>

        
        <script type="text/javascript">
        	<%--
        	// override the datepicker format date class...
			// this allows the datepicker to be updated while the user inputs
			// their date in whatever funky format they desire.
			// if we decide to exclude this override the datepicker will only update
			// when it sees the expected format, 'dd MMM yyyy'
			--%>
			jQuery.datepicker.parseDate = function(format, value, setting) {
			    var d = Date.parse(value);
				if (!d) {
					// handled by caller.
					throw 'could not parse date';
				}
			    return d;
			};
			 
            jQuery(function () {
                bdrs.contextPath = '${pageContext.request.contextPath}';
                bdrs.portalContextPath = '${portalContextPath}';
                bdrs.ident = '<%= context.getUser() == null ? "" : context.getUser().getRegistrationKey() %>';
                bdrs.dateFormat = 'dd M yy';
				
				<sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_SUPERVISOR,ROLE_POWER_USER,ROLE_USER">
				<%--   
				    This stuff isn't used for anything extremely important.
				    Messing with these will allow you to pull some extra geom data
				    from mapserver
				--%>
                bdrs.authenticated = true;
                bdrs.authenticatedUserId = ${authenticatedUserId != null ? authenticatedUserId : 'null'};
                bdrs.isAdmin = ${isAdmin ? isAdmin : 'null'};
                bdrs.authenticatedRole = '${authenticatedRole != null ? authenticatedRole : ROLE_ANONYMOUS}';
                </sec:authorize>

                bdrs.init();
                

            });
            
        </script>
        <c:forEach var="jsFile" items="${customJS}">
            <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/${jsFile}"></script>
        </c:forEach>

    </head>

    <body>
        <div class="wrapper">
            <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_USER">
                <a id="signOut" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            </sec:authorize>

            <cw:getThemeTemplate key="template.header"/>

			<div class="dashboardContainer">
			    <c:if test="${not disableMenu}">
				    <cw:getThemeTemplate key="template.menu"/>
                </c:if>

				<cw:getThemeTemplate key="template.dashboard"/>

	            <div class="contentwrapper" id="contentwrapper">
	                <div class="messages">
	                    <c:forEach items="${context.messageContents}" var="message">
	                       <p class="message"><c:out value="${message}"/></p>
	                    </c:forEach>
	                </div>
	                <div class="content" id="content">
                        <c:if test="${not disableContent}">
	                        <tiles:insertAttribute name="content"/>
                        </c:if>
	                </div>
	            </div>

				<div class="dashboardFooter"></div>
            </div>

            <cw:getThemeTemplate key="template.footer"/>
        </div>
        <cw:getThemeTemplate key="template.page.footer"/>
    </body>
    <script type="text/javascript">
        bdrs.form.init_form_validation();
    </script>
</html>
