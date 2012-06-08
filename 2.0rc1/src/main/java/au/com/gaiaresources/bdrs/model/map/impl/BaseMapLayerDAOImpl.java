package au.com.gaiaresources.bdrs.model.map.impl;

import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerDAO;

/**
 * Provides a mechanism for retrieving {@link BaseMapLayer} from the database.
 * 
 * @author stephanie
 */
@Repository
public class BaseMapLayerDAOImpl extends AbstractDAOImpl implements BaseMapLayerDAO {
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.map.BaseMapLayerDAO#getBaseMapLayer(int)
     */
    @Override
    public BaseMapLayer getBaseMapLayer(int pk) {
        return getByID(BaseMapLayer.class, pk);
    }
}
