package au.com.gaiaresources.bdrs.model.portal;

import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;

public interface PortalDAO extends TransactionDAO {

    Portal save(Portal portal) throws Exception;
    
    Portal save(Session sesh, Portal portal) throws Exception;
    
    Portal getPortal(Session sesh, Integer portalId);
    Portal getPortal(Integer portalId);
    
    Portal getPortalByName(Session sesh, String portalName);

    /**
     * Returns the Portal identified by the supplied alias, or null if none exists.
     * @param session the Session to use for the query.
     * @param alias the alias to search for.
     * @return the Poral with the supplied alias, or null if none exists.
     */
    Portal getPortalByUrlPrefix(Session session, String alias);

    List<Portal> getPortals();
    List<Portal> getPortals(Session sesh);
    
    /**
     * Returns a list of all active portals.
     * @return a list of all active portals.
     */
    List<Portal> getActivePortals();
    /**
     * Returns a list of all active/inactive portals using the specified session.
     * @param sesh the session to be used to retrive portals. 
     * @param isActive true if all returned portals are active, false otherwise.
     * @return a list of all active/inactive portals.
     */
    List<Portal> getActivePortals(Session sesh, boolean isActive);
    
    List<PortalEntryPoint> getPortalEntryPoints(Session sesh, Portal portal);
    List<PortalEntryPoint> getPortalEntryPoints(Portal portal);
    
    PortalEntryPoint getPortalEntryPointByPattern(Session sesh, Integer portalId, String pattern);
    PortalEntryPoint getPortalEntryPointByPattern(Integer portalId, String pattern);
    
    PortalEntryPoint save(Session sesh, PortalEntryPoint entryPoint);
    
    PortalEntryPoint save(PortalEntryPoint entryPoint);
    
    
    Portal getDefaultPortal(Session sesh);
    Portal getDefaultPortal();

    PortalEntryPoint getPortalEntryPoint(Integer id);

    void delete(PortalEntryPoint value);

    Portal save(PortalInitialiser portalInitialiser, Session sesh, Portal portal) throws Exception;
}
