package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;

/**
 * Represents a facade over the {@link PortalDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyPortalDAO extends AbstractPyDAO {
    private PortalDAO portalDAO;

    /**
     * Creates a new instance.
     *
     * @param portalDAO retrieves related data.
     */
    public PyPortalDAO(PortalDAO portalDAO) {
        this.portalDAO = portalDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(portalDAO, Portal.class, pk);
    }
}
