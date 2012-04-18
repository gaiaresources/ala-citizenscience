package au.com.gaiaresources.bdrs.db.impl;

import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.hibernate.Session;

import java.util.Collection;

/**
 * Helper class that iterates over a Collection of persistent entities,
 * flushing and clearing the hibernate session periodically.
 * @param <T> The type of entity being iterated over.
 */
public abstract class BatchUpdateHelper<T> {

    /** Identifies how often to flush and clear the session */
    public static final int BATCH_SIZE = 20;

    /**
     * To be overridden to do the work on each entity.
     * @param entity the persistent entity to update.
     */
    public abstract void invoke(T entity);

    /**
     * Iterates over the supplied Collection, calling the invoke method for each element.
     * Flushes and clears the Session every BATCH_SIZE iterations.
     * @param entities the persistent entities to update.
     */
    public void doBatched(Collection<T> entities) {
        Session session = getSession();

        int count = 0;
        for (T entity : entities) {
            invoke(entity);
            if (++count % BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }
        }
    }

    protected Session getSession() {
        return RequestContextHolder.getContext().getHibernate();
    }
}