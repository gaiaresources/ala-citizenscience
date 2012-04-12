package au.com.gaiaresources.bdrs.model.index.impl;

import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.model.index.IndexSchedule;
import au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;

/**
 * Implementation that provides a mechanism for retrieving {@link IndexSchedule}s.
 * 
 * @author stephanie
 *
 */
@Repository
public class IndexScheduleDAOImpl extends AbstractDAOImpl implements IndexScheduleDAO {

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO#getIndexSchedules()
     */
    @Override
    public List<IndexSchedule> getIndexSchedules() {
        Query q = getSession().createQuery("from IndexSchedule");
        return q.list();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO#getIndexSchedules(au.com.gaiaresources.bdrs.model.portal.Portal)
     */
    @Override
    public List<IndexSchedule> getIndexSchedules(Portal portal) {
        try {
            disablePortalFilter();
            
            Query q = getSession().createQuery("from IndexSchedule where portal = :portal order by id asc");
            q.setParameter("portal", portal);
            
            return q.list();
        } catch(Error e) {
            throw e;
        } finally {
            enablePortalFilter();
        }
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO#getIndexSchedulesForClass(java.lang.Class)
     */
    @Override
    public List<IndexSchedule> getIndexSchedulesForClass(
            Class<? extends PersistentImpl> clazz) {
        Query q = getSession().createQuery("from IndexSchedule where className = :classname order by id asc");
        q.setParameter("classname", clazz.getName());
        return q.list();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO#getIndexSchedule(int)
     */
    @Override
    public IndexSchedule getIndexSchedule(int id) {
        return getByID(IndexSchedule.class, id);
    }
}
