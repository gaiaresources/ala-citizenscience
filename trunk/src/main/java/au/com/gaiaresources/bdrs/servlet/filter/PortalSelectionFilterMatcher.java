package au.com.gaiaresources.bdrs.servlet.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.gaiaresources.bdrs.controller.portal.PortalPrefixValidator;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalEntryPoint;

public class PortalSelectionFilterMatcher {

    private Logger log = Logger.getLogger(getClass());
    
    private PortalDAO portalDAO;
    private PortalPrefixValidator validator;

    public static final String BASE_RESTFUL_PORTAL_PATTERN = "(/portal/){1}(\\d+)";
    public static final String RESTFUL_PORTAL_PATTERN_STR = "^" + BASE_RESTFUL_PORTAL_PATTERN +"(/{1}|$)";
    
    public PortalSelectionFilterMatcher(PortalDAO portalDAO, PortalPrefixValidator validator) {
        super();
        this.portalDAO = portalDAO;
        this.validator = validator;
    }

    /**
     * If the supplied URL has the format /portal/x/... the portal id will be extracted from the
     * URL and returned.
     * @param currentPortal supplied to allow optimisation of the most common case, URL matches the current Portal.
     * @param url the URL to match.
     * @param session the Hibernate Session to use to retrieve the portal.
     * @return the matched Portal or null if the URL didn't match or the portal id specified doesn't exist.
     */
    public Portal matchURL(Portal currentPortal, String url, Session session) {

        if (matchesCurrentPortal(currentPortal, url)) {
            return currentPortal;
        }

        Portal matchedPortal = null;
        // Test if the servlet path has the form "/portal/<portal_pk>/.../..."
        Pattern restfulPortalPattern = Pattern.compile(RESTFUL_PORTAL_PATTERN_STR);
        Matcher servletPathMatcher = restfulPortalPattern.matcher(url);

        if(servletPathMatcher.find()) {
            // Attempt to get the portal from the database.
            // Note that the portalPk may be invalid (an int but not a pk)
            int portalPk = Integer.parseInt(servletPathMatcher.group(2));
            matchedPortal = portalDAO.getPortal(session, portalPk);
        }
        else {
            // Attempt to match via Alias.
            String pattern = "^/{1}([\\w|-]+)(/{1}|$)";
            Matcher matcher = Pattern.compile(pattern).matcher(url);
            if (matcher.find()) {
                String alias = matcher.group(1);
                if (!validator.isReservedURLPrefix(alias)) {
                    matchedPortal = portalDAO.getPortalByUrlPrefix(session, alias);
                }
            }
        }

        return matchedPortal;
    }

    /**
     * Only matches the case where the current URL is the same as the Portal's preferred context path.
     * Other matches (e.g. the preferred path is /alcw and the URL has /portal/8) will still be found, but this
     * is designed to optimise the most common case.
     * @param currentPortal the Portal in the user's current Session.
     * @param url the URL being requested.
     * @return true if the URL matches the preferred URL of the current Portal.
     */
    private boolean matchesCurrentPortal(Portal currentPortal, String url) {
        if (currentPortal == null) {
            return false;
        }
        return url.startsWith(currentPortal.getPortalContextPath());
    }
    
    public PortalMatches matchEntryPoints(Session sesh, String url) {
        
        Portal matchedPortal = null;
        PortalEntryPoint matchedEntryPoint = null;
        Portal defaultPortal = null;
        
        List<PortalEntryPoint> invalidPatternList = new ArrayList<PortalEntryPoint>(); 

        Pattern pattern;
        Matcher matcher;
        
        for (Portal portal : portalDAO.getActivePortals(sesh, true)) {
            if (portal.isDefault()) {
                if (defaultPortal == null) {
                    defaultPortal = portal;
                } else {
                    log.debug("Multiple default portals located using the first.");
                }
            }

            for (PortalEntryPoint entryPoint : portalDAO.getPortalEntryPoints(sesh, portal)) {
                try {
                    pattern = Pattern.compile(entryPoint.getPattern());
                    matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        // Found a regex match.
                        if (matchedPortal == null
                                && matchedEntryPoint == null) {
                            matchedPortal = portal;
                            matchedEntryPoint = entryPoint;
                        } else {
                            log.debug("Multiple portal matches. Using the first.");
                            log.debug("URL = " + url);
                            log.debug("Pattern = "
                                    + entryPoint.getPattern());
                        }
                    }
                } catch(PatternSyntaxException e) {
                    invalidPatternList.add(entryPoint);
                }
            }
        }

        return new PortalMatches(matchedPortal, matchedEntryPoint, defaultPortal, invalidPatternList);
    }
}
