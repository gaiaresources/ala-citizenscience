package au.com.gaiaresources.bdrs.servlet;

import au.com.gaiaresources.bdrs.model.portal.Portal;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Helper class that can build URLs including the portal context path.
 */
public class UrlAssembler {

    /**
     * Creates a String representation of an HttpRequest with parameters.
     * @param request the request to construct the url from
     * @return a String representation of an HttpRequest, including parameters
     */
    public static String assembleUrlFor(HttpServletRequest request) {
        // reconstruct the url with parameters
        Portal portal = RequestContextHolder.getContext().getPortal();
        StringBuilder builder = new StringBuilder();
        if (portal != null) {
            builder.append(portal.getPortalContextPath());
        }
        builder.append(request.getServletPath());

        Map<String, String[]> map = request.getParameterMap();
        if (!map.isEmpty()) {
            builder.append("?");
        }
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String key = entry.getKey();
            String[] valueList = entry.getValue();
            for (String string : valueList) {
                if (builder.length() > 1) {
                    builder.append("&");
                }
                builder.append(key);
                builder.append("=");
                builder.append(string);
            }
        }

        return builder.toString();
    }


    /**
     * Returns a URL of the form {servletContextPath/portalContextPath/portalContextRelativeUrl}.
     * If there is no Portal in the current context, that part of the URL will be omitted.
     *
     * @param request the HTTP request being processed.  This parameter is used to determine the web application
     *                context path.
     * @param portalContextRelativeUrl the URL path to appear after the part that selects the portal.
     * @return a URL of the form {servletContextPath/portalContextPath/portalContextRelativeUrl}.
     */
    public static String assembleUrlFor(HttpServletRequest request, String portalContextRelativeUrl) {
        return assembleUrlFor(portalContextRelativeUrl, request.getContextPath());
    }

    /**
     * Returns a web application context relative URL of the form {/portalContextPath/portalContextRelativeUrl}.
     * If there is no Portal in the current context, that part of the URL will be omitted.
     *
     * @param portalContextRelativeUrl the URL path to appear after the part that selects the portal.
     * @return a URL of the form {/portalContextPath/portalContextRelativeUrl}.
     */
    public static String assembleUrlFor(String portalContextRelativeUrl) {
        return assembleUrlFor(portalContextRelativeUrl, "");
    }

    /**
     * Returns a URL of the form {servletContextPath/portalContextPath/portalContextRelativeUrl}.
     * If there is no Portal in the current context, that part of the URL will be omitted.
     *
     * @param portalContextRelativeUrl the URL path to appear after the part that selects the portal.
     * @param webApplicationContextPath the path that selects the current web application. It will be the first
     *                                  path element in the returned URL.
     * @return a URL of the form {servletContextPath/portalContextPath/portalContextRelativeUrl}.
     */
    private static String assembleUrlFor(String portalContextRelativeUrl, String webApplicationContextPath) {
        StringBuilder builder = new StringBuilder(webApplicationContextPath);
        Portal portal = RequestContextHolder.getContext().getPortal();

        if (portal != null) {
            String portalContextPath = portal.getPortalContextPath();
            if (portalContextRelativeUrl.startsWith("/") && !portalContextRelativeUrl.startsWith(portalContextPath)) {
                builder.append(portalContextPath);            }
        }
        builder.append(portalContextRelativeUrl);
        return builder.toString();
    }
}
