package au.com.gaiaresources.bdrs.model.portal.impl;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalEntryPoint;

@Repository
public class PortalDAOImpl extends AbstractDAOImpl implements PortalDAO {

    private Logger log = Logger.getLogger(getClass());

    @Override
    public Portal getPortal(Integer portalId) {
        return this.getPortal(null, portalId);
    }

    @Override
    public Portal getPortal(Session sesh, Integer portalId) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        return (Portal)sesh.get(Portal.class, portalId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Portal getPortalByName(Session sesh, String portalName) {

        return findByUniqueProperty(sesh, "name", portalName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Portal getPortalByUrlPrefix(Session session, String alias) {

        return findByUniqueProperty(session, "urlPrefix", alias);
    }

    /**
     * Performs the supplied query expecting 0 or 1 results.  If more than 1 is returned a warning is issued
     * and the first result returned.
     * @param session the Session to use, may be null.
     * @param propertyName the name of the unique property to query on.
     * @param propertyValue the value of the property to query on.
     * @return the Portal with the matching property or null if no such Portal exists.
     */
    private Portal findByUniqueProperty(Session session, String propertyName, String propertyValue) {
        if(session == null) {
            session = getSessionFactory().getCurrentSession();
        }

        String query =  "from Portal p where p."+propertyName+" = ?";

        List<Portal> portalList = find(session, query, propertyValue);
        if(portalList.isEmpty()) {
            return null;
        }

        if(portalList.size() > 1) {
            String warning = "More than one portals with the property \"%s\" found. Returning the first.";
            log.warn(String.format(warning, propertyName));
        }

        return portalList.get(0);
    }

    @Override
    public List<Portal> getPortals() {
        return this.getPortals(null);
    }

    @Override
    public List<Portal> getPortals(Session sesh) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        return super.find(sesh, "from Portal p order by p.name");
    }
    
    @Override
    public List<Portal> getActivePortals() {
        return this.getActivePortals(null, true);
    }
    
    @Override
    public List<Portal> getActivePortals(Session sesh, boolean isActive) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        return super.find(sesh, "from Portal p where p.active = ? order by p.name", isActive);
    }
    
    @Override
    public PortalEntryPoint getPortalEntryPointByPattern(Integer portalId,
            String pattern) {
        return this.getPortalEntryPointByPattern(null, portalId, pattern);
    }

    @Override
    public PortalEntryPoint getPortalEntryPointByPattern(Session sesh,
            Integer portalId, String pattern) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        Query query = sesh.createQuery("select e from PortalEntryPoint e where e.portal.id = :portalId and e.pattern = :pattern");
        query.setParameter("portalId", portalId);
        query.setParameter("pattern", pattern);
        List<PortalEntryPoint> entryPointList = query.list();
        if (entryPointList.isEmpty()) {
            return null;
        } else {
            if (entryPointList.size() > 1) {
                log.warn(String.format("More than one PortalEntryPoint returned for the Portal ID: %d and pattern \"%s\". Returning the first.", portalId, pattern));
            }
            return entryPointList.get(0);
        }
    }
    
    @Override
    public List<PortalEntryPoint> getPortalEntryPoints(Portal portal) {
        return this.getPortalEntryPoints(null, portal);
    }
    
    @Override
    public PortalEntryPoint getPortalEntryPoint(Integer id) {
        return super.getByID(PortalEntryPoint.class, id);
    }
    
    @Override
    public void delete(PortalEntryPoint portalEntryPoint) {
        super.delete(portalEntryPoint);
    }

    @Override
    public List<PortalEntryPoint> getPortalEntryPoints(Session sesh, Portal portal) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        return find(sesh, "from PortalEntryPoint e where e.portal = ? order by id asc", portal);
    }

    @Override
    public Portal save(Portal portal) throws Exception {
        return this.save(null, portal);
    }

    /**
     * {@inheritDoc}
     * @throws Exception 
     */
    @Override
    public Portal save(Session sesh, Portal portal) throws Exception {
        return save(null, sesh, portal);
    }

    @Override
    public Portal save(PortalInitialiser portalInitialiser, Session sesh,
                Portal portal) throws Exception {
            
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        if (portalInitialiser == null) {
            portalInitialiser = new PortalInitialiser();
        }
        
        // if the portal is already persisted...
        if (portal.getId() != null) {
            return super.save(sesh, portal);
        }
            
        // else do the initialisation
        Portal p = super.save(sesh, portal);
        try {
            // seed portal with essential data...
            portalInitialiser.init(sesh, p);
        } catch (Exception e) {
            log.error("Could not initialise portal. Rolling back transaction...", e);
            throw e;
        }
        return p;
    }

    @Override
    public PortalEntryPoint save(Session sesh, PortalEntryPoint entryPoint) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        return super.save(sesh, entryPoint);
    }

    @Override
    public PortalEntryPoint save(PortalEntryPoint entryPoint) {
        return this.save(null, entryPoint);
    }
    
    @Override
    public Portal getDefaultPortal() {
        return this.getDefaultPortal(null);
    }

    @Override
    public Portal getDefaultPortal(Session sesh) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        List<Portal> portalList = find(sesh, "from Portal p where p.default = true and p.active = true");
        
        if(portalList.isEmpty()) {
            return null;
        }
        
        if(portalList.size() > 1) {
            log.warn("More than one active and default portals found. Returning the first.");
        }
        
        return portalList.get(0);
    }
}
