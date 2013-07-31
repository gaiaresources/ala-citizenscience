package au.com.gaiaresources.bdrs.db;

import org.apache.log4j.Logger;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.impl.FilterImpl;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;

public class FilterManager {
	
	public static final String USER_ID = "userId";
	
	public static final String LOCATION_USER_ACCESS_FILTER = "locationUserAccessFilter";

	private static Logger log = Logger.getLogger(FilterManager.class);
	
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
            session.enableFilter(Record.USER_ACCESS_FILTER).setParameter(Record.FILTER_PARAMETER_USER, 0);
        } else if (!user.isAdmin()) {
            String filterName = Record.USER_ACCESS_FILTER;
            if (user.isModerator()) {
                filterName = Record.MODERATOR_ACCESS_FILTER;
            }
            session.enableFilter(filterName).setParameter(Record.FILTER_PARAMETER_USER, user.getId());
        }  
    }
    
    /**
     * Disables all of the record filters on the supplied hibernate session.
     * @param session disable filters on this Hibernate filter.
     */
    public static void disableRecordFilter(Session session) {
        session.disableFilter(Record.ANONYMOUS_RECORD_ACCESS_FILTER);
        session.disableFilter(Record.USER_ACCESS_FILTER);
        session.disableFilter(Record.MODERATOR_ACCESS_FILTER);
        session.disableFilter(Record.FILTER_PARAMETER_USER);
    }

    /**
     * Filters out records that are created as attribute values.
     * @param session
     */
    public static void setPartialRecordCountFilter(Session session) {
        session.enableFilter(Record.PARTIAL_RECORD_COUNT_FILTER);
    }
    
    /**
     * Turns off the filtering of records that are created as attribute values.
     * @param session
     */
    public static void disablePartialRecordCountFilter(Session session) {
        session.disableFilter(Record.PARTIAL_RECORD_COUNT_FILTER);
    }
    
    /**
     * Returns the ID of the Portal that has been supplied to the Portal Filter.  If the Portal Filter is not
     * in effect, null will be returned.
     * @param session the Session to check the Filter on.
     * @return the ID of the Portal used to filter Hibernate queries, or null if the Filter is not applied.
     */
    public static Integer getFilteredPortalId(Session session) {
        Integer portalId = null;
        Filter filter = session.getEnabledFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
        if (filter != null && filter instanceof FilterImpl) {
            portalId = (Integer)((FilterImpl)filter).getParameter(PortalPersistentImpl.PORTAL_FILTER_PORTALID_PARAMETER_NAME);
        }

        return portalId;
    }
    
    /**
     * Enables the Record filter that only returns Records with attributes of type
     * 'IMAGE'.
     * @param session the Session to enable the filter on.
     */
    public static void enableImagesFilter(Session session) {
        session.enableFilter(Record.IMAGE_FILTER);
    }
    
    /**
     * Disables all filters
     * @param session disable filters on this session.
     */
    public static void disableFilters(Session session) {
    	session.disableFilter(LOCATION_USER_ACCESS_FILTER);
    	session.disableFilter(Record.ANONYMOUS_RECORD_ACCESS_FILTER);
    	session.disableFilter(Record.MODERATOR_ACCESS_FILTER);
    	session.disableFilter(Record.PARTIAL_RECORD_COUNT_FILTER);
    	session.disableFilter(Record.USER_ACCESS_FILTER);
    }
    
    public static void enableLocationFilter(Session session, User user) {
        if (user == null) {
            // use 0 for the user id - won't match anything!
            session.enableFilter(LOCATION_USER_ACCESS_FILTER).setParameter(USER_ID, 0);
        } else if (!user.isAdmin() && !user.isModerator()) {
            session.enableFilter(LOCATION_USER_ACCESS_FILTER).setParameter(USER_ID, user.getId());
        }
    }
    
    public static void disableLocationFilter(Session session) {
        session.disableFilter(LOCATION_USER_ACCESS_FILTER);
    }
}
