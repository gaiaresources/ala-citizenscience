package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;

/**
 * Implemented by all wrapped database access objects that is passed to Python, this class ensures
 * that the dao is capable of retrieving objects by primary key through a known method name.
 */
public abstract class AbstractPyDAO {

    /**
     * Gets the object represented by this DAO by primary key.
     *
     * @param pk the primary key of the object to be retrieved.
     * @return the object with the specified primary key.
     */
    public abstract String getById(int pk);

    /**
     * Retrieves an object with the specified class and primary key using the database access object provided.
     *
     * @param dao   performs the retrieval of database objects.
     * @param klazz the type of the object to be retrieved.
     * @param pk    the primary key of the object to be retrieved.
     * @return an object with the specified class and primary key using the database access object provided.
     */
    protected String getById(TransactionDAO dao, Class<? extends PersistentImpl> klazz, int pk) {
        PersistentImpl instance = (PersistentImpl) dao.getSessionFactory().getCurrentSession().get(klazz, pk);
        return PyDAOUtil.toJSON(instance).toString();
    }
}
