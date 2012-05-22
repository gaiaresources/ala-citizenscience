package au.com.gaiaresources.bdrs.servlet.filter;

import au.com.gaiaresources.bdrs.controller.portal.PortalPrefixValidator;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalEntryPoint;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.Pair;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class PortalSelectionFilter implements Filter {
    public static final String PORTAL_KEY = "currentPortal";
    public static final String DEFAULT_REDIRECT_URL = "/authenticated/redirect.htm";
    

    
    private Logger log = Logger.getLogger(getClass());

    private PortalDAO portalDAO;
    private SessionFactory sessionFactory;
    private WebApplicationContext webApplicationContext;
    private PortalSelectionFilterMatcher portalMatcher;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        // We can only add a session variable to a HttpServletRequest
        if (request instanceof HttpServletRequest) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            RequestContext requestContext = configureRequestContext(httpRequest);

            Pair<Session, Boolean> result = configureHibernateSession();
            Session sesh = result.getFirst();

            // This method has a side effect of sending a redirect if the current Portal has been invalidated.
            Portal currentPortal = getCurrentPortal(httpRequest, response, sesh);

            String url = httpRequest.getRequestURL().toString();
            Portal urlSpecifiedPortal = portalMatcher.matchURL(currentPortal, httpRequest.getServletPath(), sesh);

            if(urlSpecifiedPortal != null && urlSpecifiedPortal.isActive()) {
                httpRequest.getSession().setAttribute(PORTAL_KEY, urlSpecifiedPortal);
                requestContext.setPortal(urlSpecifiedPortal);

                // If we are currently using one portal and enter the URL for another, invalidate the session
                // and redirect the user to the new Portal.
                if(currentPortal != null && !urlSpecifiedPortal.getId().equals(currentPortal.getId())) {
                    redirectToNewPortal(response, httpRequest, url);
                }
            } else {
                List<Portal> portalList = portalDAO.getActivePortals(sesh, true);
                if (!portalList.isEmpty() && !response.isCommitted()) {

                    matchPortalEntryPoints(response, httpRequest, requestContext, sesh, currentPortal, url, urlSpecifiedPortal, portalList);
    
                } else {
                    log.error("No portals defined. Unable to set Portal ID");
                }
            }
            boolean rollbackRequired = result.getSecond();
            if (rollbackRequired) {
                sesh.getTransaction().rollback();
                sesh.close();
            }  
        } else {
            log.error("Unsupported request type: " + request.getClass());
            log.error("Not setting Portal ID");
            // Otherwise pass through?
        }

        // The response may be committed if a redirect has been set. If so
        // there is no point progressing with the filter chain. Your fate
        // has been sealed.
        if (!response.isCommitted()) {
            chain.doFilter(request, response);
        }
    }

    /**
     * Invalidates the HTTP Session and redirects the user to the portal specified by their URL.
     * @param response the HTTP response we are creating.
     * @param httpRequest the HTTP request we are processing.
     * @param url the URL (minus query parameters) to redirect to.
     * @throws IOException if there is an error writing the response.
     */
    private void redirectToNewPortal(ServletResponse response, HttpServletRequest httpRequest, String url) throws IOException {
        httpRequest.getSession().invalidate();
        String queryString = httpRequest.getQueryString();
        String redirect = url + (queryString != null ? "?"+queryString : "");
        sendRedirect(response, redirect);
    }

    private void matchPortalEntryPoints(ServletResponse response, HttpServletRequest httpRequest, RequestContext requestContext, Session sesh, Portal currentPortal, String url, Portal urlSpecifiedPortal, List<Portal> portalList) throws IOException {
        PortalMatches matches = portalMatcher.matchEntryPoints(sesh, url);
        Portal defaultPortal = matches.getDefaultPortal();
        Portal matchedPortal = matches.getMatchedPortal();
        PortalEntryPoint matchedEntryPoint = matches.getMatchedEntryPoint();

        // For each portal, test the entry points.
        if (defaultPortal == null) {
            defaultPortal = portalList.get(0);
        }

        if (matchedPortal == null) {
            if (currentPortal == null) {
                if(urlSpecifiedPortal != null && !urlSpecifiedPortal.isActive()) {
                    // The URL contains an invalid portal ID,
                    httpRequest.getSession().setAttribute(PORTAL_KEY, urlSpecifiedPortal);
                    sendRedirectToPortalHome(httpRequest, response, defaultPortal);
                } else {
                    log.debug("URL does not match any known portal entry pattern. Using default portal.");
                    matchedPortal = defaultPortal;
                }
            } else {
                if(urlSpecifiedPortal != null && !urlSpecifiedPortal.isActive()) {
                    // We got here because we have a RESTful URL
                    // but the portal is no longer active
                    requestContext.setPortal(currentPortal);
                    httpRequest.getSession().setAttribute(PORTAL_KEY, currentPortal);
                    sendRedirectToPortalHome(httpRequest, response, currentPortal);
                    //matchedPortal = null;
                } else {
                    // The Portal ID has been set so there is nothing left to do.
                    requestContext.setPortal(currentPortal);
                    matchedPortal = null;
                }
            }
        } else {
            // Assume that they are logged into Portal 1 and type in the URL
            // that matches Portal 2, we want to log them out of Portal 1
            // and redirect them to their desired URL which will take them
            // to Portal 2.
            if (currentPortal != null && !matchedPortal.getId().equals(currentPortal.getId())) {
                redirectToNewPortal(response, httpRequest, url);
            }
            // else set the Portal ID for the matched portal (below)
        }

        // If we have not already decided to perform a redirect, and
        // we have a matched portal.
        if (!response.isCommitted() && matchedPortal != null) {
            // Set the portalID session attribute
            httpRequest.getSession().setAttribute(PORTAL_KEY, matchedPortal);
            requestContext.setPortal(matchedPortal);

            // Redirect for the matched portal if needed.
            if (matchedEntryPoint != null) {
                String redirect;
                if (matchedEntryPoint.getRedirect().isEmpty()) {
                    redirect = httpRequest.getContextPath()+matchedPortal.getPortalContextPath()
                            + DEFAULT_REDIRECT_URL;
                } else {
                    redirect = matchedEntryPoint.getRedirect();
                }
                sendRedirect(response, redirect);
            }
            // otherwise fall through to the requested url
            // if it did not exactly match a portal entry point.

        }
    }

    /**
     * Retrieves the Portal the user is using.  If the current Portal has become deactivated someone, this method
     * will invalidate the session and redirect the user to the default Portal.
     *
     * @param httpRequest the HTTP request we are processing.
     * @param response the HTTP response we are producing.
     * @param sesh the Hibernate session to use for queries.
     * @return the Portal the user is currently using or null if this is a new user (HTTP) session.
     * @throws IOException if there is an error returning the HTTP redirect.
     */
    private Portal getCurrentPortal(HttpServletRequest httpRequest, ServletResponse response, Session sesh) throws IOException {
        // Get the portal the user is currently accessing from the session
        Portal currentPortal = (Portal)httpRequest.getSession().getAttribute(PORTAL_KEY);
        if(currentPortal != null) {
            currentPortal= portalDAO.getPortal(sesh, currentPortal.getId());
            // Determine if the raw portal is inactive
            if(currentPortal == null) {
                // If the raw portal cannot be found for whatever reason,
                httpRequest.getSession().removeAttribute(PORTAL_KEY);
            }
            else if(!currentPortal.isActive()) {
                // if the raw portal is found but is deactivated
                currentPortal = null;
                httpRequest.getSession().invalidate();
                sendRedirectToPortalHome(httpRequest, response, portalDAO.getDefaultPortal(sesh));
            }
        }
        return currentPortal;
    }

    private void sendRedirectToPortalHome(HttpServletRequest httpRequest, 
                                            ServletResponse response, 
                                            Portal portal) throws IOException {
        sendRedirect(response, String.format("%s/portal/%d/home.htm", httpRequest.getContextPath(), portal.getId()));
    }

    private void sendRedirect(ServletResponse response, String url)
            throws IOException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = ((HttpServletResponse) response);
            httpResponse.sendRedirect(url);
        } else {
            log.error(String.format("Unable to redirect to \"%s\" because the response is a \"%s\" and not a \"%s\"", url, response.getClass().toString(), HttpServletResponse.class.toString()));
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        ServletContext servletContext = filterConfig.getServletContext();
        webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        AutowireCapableBeanFactory autowireCapableBeanFactory = webApplicationContext.getAutowireCapableBeanFactory();
        portalDAO = autowireCapableBeanFactory.getBean(PortalDAO.class);
        sessionFactory = autowireCapableBeanFactory.getBean(SessionFactory.class);
        
        portalMatcher = new PortalSelectionFilterMatcher(portalDAO, webApplicationContext.getBean(PortalPrefixValidator.class));
    }

    /**
     * Retrieve the stored RequestContext from the session attribute
     * and store it in the RequestContextHolder. It is very important
     * to do this before creating any Hibernate Sessions because
     * the hibernate session will use the RequestContext in the
     * RequestContextHolder to retrieve the Portal ID and enable and
     * configure the portal filter.
     * @param httpRequest the HTTP request being processed
     * @return the configured RequestContext object.
     */
    private RequestContext configureRequestContext(HttpServletRequest httpRequest) {

        RequestContext requestContext = (RequestContext) httpRequest.getAttribute(RequestContext.REQUEST_CONTEXT_SESSION_ATTRIBUTE_KEY);
        if (requestContext == null) {
            requestContext = new RequestContext(httpRequest,
                    webApplicationContext);
            httpRequest.setAttribute(RequestContext.REQUEST_CONTEXT_SESSION_ATTRIBUTE_KEY, requestContext);
        }
        RequestContextHolder.set(requestContext);
        return requestContext;
    }

    /**
     * Checks for the existence of a current transaction which indicates the execution context is unit tests.
     * Otherwise opens a temporary hibernate session and starts a transaction for the purposes of doing the
     * portal selection.
     * @return a Pair<Session, Boolean> containing the Hibernate session to use and whether the transaction
     * should be rolled back at the end of processing. (which it will be in production, but not in a unit test
     * environment).
     */
    private Pair<Session, Boolean> configureHibernateSession() {
        boolean rollbackRequired;
        Session sesh;
        if (sessionFactory.getCurrentSession().getTransaction().isActive()) {
            // This section will run if we are unit testing.
            sesh = sessionFactory.getCurrentSession();

            rollbackRequired = false;
        } else {
            // This section will run under normal conditions.
            sesh = sessionFactory.openSession();
            sesh.beginTransaction();
            rollbackRequired = true;
        }

        return new Pair<Session, Boolean>(sesh, rollbackRequired);
    }
}
