package au.com.gaiaresources.bdrs.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * All DAOS provide these basic functions.
 */
public interface TransactionDAO {

	/**
	 * Save persistent object.
	 * @param sesh Hibernate session to use.
	 * @param instance Instance to save.
	 * @return Saved object.
	 */
    public <T extends Persistent> T save(Session sesh, T instance);
    
    /**
     * Update persistent object.
     * @param sesh Hibernate session to use.
     * @param instance Instance to save.
     * @return Updated object.
     */
    public <T extends Persistent> T update(Session sesh, T instance);
    
    /**
     * Save or update persistent object.
     * @param sesh Hibernate session to use.
     * @param instance Instance to save.
     * @return Saved object.
     */
    public <T extends Persistent> T saveOrUpdate(Session sesh, T instance);
    
    /**
     * Delete persistent object.
     * @param sesh Hibernate session to use.
     * @param instance Instance to delete.
     */
    public <T extends Persistent> void delete(Session sesh, T instance);
    
    /**
     * Count rows of persistent object in database.
     * @param clazz Class to count.
     * @return Count of rows.
     */
    public <T extends Persistent> Long count(Class<T> clazz);
    
    /**
     * Returns the session factory.
     * @return SessionFactory interface.
     */
    public SessionFactory getSessionFactory();
    
    /**
     * Save a persistent object.
     * @param instance Object to save.
     * @return Saved object.
     */
    public <T extends Persistent> T save(T instance);
    
    /**
     * Update a persistent object.
     * @param instance Object to update.
     * @return Updated object.
     */
    public <T extends Persistent> T update(T instance);
    
    /**
     * Save or update persistent object.
     * @param instance Instance to save or update.
     * @return Saved object.
     */
    public <T extends Persistent> T saveOrUpdate(T instance);
    
    /**
     * Delete persistent object.
     * @param instance Instance to delete.
     */
    public <T extends Persistent> void delete(T instance);
    
    /**
     * Re-read the state of the instance from the underlying database.
     * @param instance persistent object to refresh.
     */
    public <T extends Persistent> void refresh(T instance);
    
    /**
     * Re-read the state of the instance from the underlying database.
     * @param sesh Hibernate session.
     * @param instance persistent object to refresh.
     */
    public <T extends Persistent> void refresh(Session sesh, T instance);
}
