package au.com.gaiaresources.bdrs.servlet;

import au.com.gaiaresources.bdrs.controller.insecure.HTTPErrorController;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Maps HTTP requests of the form /{portalAlias}/{restOfUrl} to a handler that simply forwards the request to
 * /{restOfUrl}.  This is to support the dynamic allocation of new allowed URLs via portal URL prefix
 * configuration (in the root portal).
 *
 */
public class PortalUrlHandlerMapping extends AbstractHandlerMapping {


    /**
     * A simple HttpRequestHandler that strips the portal prefix from the URL then forwards it so it can
     * be handled by the correct controller.
     */
    public class PortalPrefixUrlStripper implements HttpRequestHandler {

        @Override
        public void handleRequest(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            Portal portal = RequestContextHolder.getContext().getPortal();
            String path;
            if (portal != null) {
                int index = request.getServletPath().indexOf(portal.getPortalContextPath());
                path = request.getServletPath().substring(index+portal.getPortalContextPath().length());
            }
            else {
                path = HTTPErrorController.NOT_FOUND_URL;
            }

            request.getRequestDispatcher(path).forward(request, response);

        }

    }

    /**
     * Matches requests that begin with the context path of the current portal (if the current portal is using
     * an alias rather than the default portal/x/
     * e.g. If the current portal has an alias of alcw, this handler will match URLs of the form /alcw/**.
     * @param request the HTTP request to check.
     * @return if the request matches the current portal alias, a handler will be returned that simply strips the
     * portal information from the URL and forwards it on.
     * @throws Exception if there is an error during request processing.
     */
    @Override
    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

        Portal portal = RequestContextHolder.getContext().getPortal();
        if (portal != null) {

            if (request.getServletPath().startsWith(portal.getPortalContextPath())) {
                return new PortalPrefixUrlStripper();
            }
        }

        return null;
    }
}
