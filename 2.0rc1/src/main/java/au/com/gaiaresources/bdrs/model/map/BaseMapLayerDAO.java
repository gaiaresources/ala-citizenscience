package au.com.gaiaresources.bdrs.model.map;

import au.com.gaiaresources.bdrs.db.TransactionDAO;

/**
 * Provides a mechanism for retrieving {@link BaseMapLayer}.
 * 
 * @author stephanie
 */
public interface BaseMapLayerDAO extends TransactionDAO {
    /**
     * Gets the {@link BaseMapLayer} by id
     * @param pk the database id of the {@link BaseMapLayer}
     * @return a {@link BaseMapLayer} if the id is found in the database, null otherwise
     */
    public BaseMapLayer getBaseMapLayer(int pk);
}
