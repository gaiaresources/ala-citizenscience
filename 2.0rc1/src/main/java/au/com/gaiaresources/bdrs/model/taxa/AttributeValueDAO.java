package au.com.gaiaresources.bdrs.model.taxa;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;

/**
 * Performs all database access for <code>AttributeValue</code>s
 */
public interface AttributeValueDAO extends TransactionDAO {

    /**
     * Retrieves the {@link AttributeValue} with the specified primary key, 
     * or null if the {@link AttributeValue} cannot be found.
     * @param id the primary key of the {@link AttributeValue} to be retrieved.
     * @return the {@link AttributeValue} with the specified primary key, 
     * or null if the {@link AttributeValue} cannot be found.
     */
    public AttributeValue get(int id);

    /**
     * Retrieves the {@link AttributeValue} with the specified primary key, 
     * or null if the {@link AttributeValue} cannot be found.
     * @param sesh the session to be used to retrieve the {@link AttributeValue}
     * @param id the primary key of the {@link AttributeValue} to be retrieved.
     * @return the {@link AttributeValue} with the specified primary key, 
     * or null if the {@link AttributeValue} cannot be found.
     */
    public AttributeValue get(Session sesh, int id);

    /**
     * Creates the specified {@link AttributeValue}
     * @param attrVal the {@link AttributeValue} to be created.
     * @return the persisted {@link AttributeValue}
     */
    public AttributeValue save(AttributeValue attrVal);

    /**
     * Creates the specified {@link AttributeValue}
     * @param sesh the session to used in persisting the {@link AttributeValue}
     * @param attrVal the {@link AttributeValue} to be created.
     * @return the persisted {@link AttributeValue}
     */
    public AttributeValue save(Session sesh, AttributeValue attrVal);

    /**
     * Updates the persisted version of the specified {@link AttributeValue}.
     * @param attrVal the {@link AttributeValue} to be updated.
     * @return the persisted {@link AttributeValue}
     */
    public AttributeValue update(AttributeValue attrVal);

    /**
     * Updates the persisted version of the specified {@link AttributeValue}.
     * @param sesh the session to used in persisting the {@link AttributeValue}
     * @param attrVal the {@link AttributeValue} to be updated.
     * @return the persisted {@link AttributeValue}
     */
    public AttributeValue update(Session sesh, AttributeValue attrVal);

    /**
     * Deletes the specified {@link AttributeValue}
     * 
     * @param attrVal the {@link AttributeValue} to be removed permanently.
     * @return the number of items removed.
     */
    public int delete(AttributeValue attrVal);

}