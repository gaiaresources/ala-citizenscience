package au.com.gaiaresources.bdrs.db;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.hibernate.Filter;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;

public class FilterManager {
    public static void setPortalFilter(org.hibernate.Session sesh, Portal portal) {
        Filter filter = sesh.enableFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
        filter.setParameter(PortalPersistentImpl.PORTAL_FILTER_PORTALID_PARAMETER_NAME, portal.getId());
    }
    
    public static void disablePortalFilter(org.hibernate.Session sesh) {
        sesh.disableFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
    }
    
    public static void enablePortalFilter(org.hibernate.Session sesh, Portal portal) {
        if (portal != null) {
            setPortalFilter(sesh, portal);
        }
    }

    /**
     * Enables the appropriate record filter on the supplied hibernate session.
     * @param session the hibernate session to apply the filter to.            
     * @param user the currently logged in User   
     */
    public static void enableRecordFilter(Session session, User user) {

        if (user == null) {
            session.enableFilter(Record.ANONYMOUS_RECORD_ACCESS_FILTER);
        }
        else if (!user.isAdmin()) {
            String filterName = Record.USER_ACCESS_FILTER;
            if (user.isModerator()) {
                filterName = Record.MODERATOR_ACCESS_FILTER;
            }
            session.enableFilter(filterName).setParameter(Record.FILTER_PARAMETER_USER, user.getId());
        }
                
    }
    
}
