package au.com.gaiaresources.bdrs.controller.portal;

import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates portal prefixes.
 * Not annotated as a @Component because the bean is declared using xml to inject the current application version
 * via maven substitution.
 */
public class PortalPrefixValidator {

    /**
     * These paths are used for static resources and the mobile application so won't be forwarded to the
     * DispatcherServlet so are unsuitable as a portal prefix.
     */
    private static final String[] STATIC_WEB_APPLICATION_PATHS = {"images", "mobile", "data", "js", "css"};

    /**
     * These paths are used in conjunction with the current application version to enable browser caching
     * of static resources which can be updated each release without requiring the user to invalidate the cache.
     */
    private static final String[] VERSIONED_WEB_APPLICATION_PATHS = {"js", "css"};

    /**
     * Stores the set of reserved prefixes, which includes the WEB_APPLICATION_PATHS above, plus the first path
     * element of any URL that the DispatcherServlet can handle. (e.g. /bdrs, /admin, /review etc.)
     */
    private Set<String> reservedPrefixes;

    @Autowired
    private PortalDAO portalDAO;

    /** The current application version - used to construct versioned paths */
    private String version;

    /**
     * Retrieves and stores the URLs known to the DispatcherServlet in an unmodifiable Set.
     */
    @PostConstruct
    public void init() {

        Set<String> paths = new HashSet<String>();
        paths.addAll(Arrays.asList(STATIC_WEB_APPLICATION_PATHS));
        for (String path : VERSIONED_WEB_APPLICATION_PATHS) {
            paths.add(path+version);
        }


        Map<String, AbstractUrlHandlerMapping> mappings = AppContext.getApplicationContext().getBeansOfType(AbstractUrlHandlerMapping.class);

        for (AbstractUrlHandlerMapping mapping : mappings.values()) {
            Map<String, Object> map = mapping.getHandlerMap();

            for (String key : map.keySet()) {
                int index = 0;
                if (key.startsWith("/")) {
                    index = 1;
                }
                int end = key.indexOf("/", index);
                if (end >= 0) {
                    paths.add(key.substring(index, end));
                }
            }
        }

        reservedPrefixes = Collections.unmodifiableSet(paths);
    }

    /**
     * Returns true if the supplied prefix is allowed to be used for the portal identified by the supplied id.
     * @param portalId the id of the Portal we are checking the prefix for.
     * @param prefix the prefix to check.
     * @return true if the prefix may be used, false otherwise.
     */
    public boolean isURLPrefixValid(int portalId, String prefix) {
        if (isReservedURLPrefix(prefix)) {
            return false;
        }

        Portal portalWithSameAlias = portalDAO.getPortalByUrlPrefix(null, prefix);
        return (portalWithSameAlias == null || portalWithSameAlias.getId() == portalId);
    }

    /**
     * Returns true if the supplied prefix has been reserved because it is a part of a valid URL understood by the
     * system (and hence cannot be reused as a portal prefix.)
     * @param prefix the prefix to check.
     * @return true if the prefix is reserved.
     */
    public boolean isReservedURLPrefix(String prefix) {
        return reservedPrefixes.contains(prefix);
    }

    /**
     * Sets the application version used to determine paths to validate.
     * @param version the current application version.
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
