package au.com.gaiaresources.bdrs.model.index;

import java.util.List;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;

/**
 * Provides a mechanism for retrieving {@link IndexSchedule}s.
 * 
 * @author stephanie
 *
 */
public interface IndexScheduleDAO extends TransactionDAO {

    /**
     * @return a {@link List} of all the {@link IndexSchedule}s for the current portal.
     */
    public List<IndexSchedule> getIndexSchedules();

    /**
     * Gets a {@link List} of all the {@link IndexSchedule}s for the given portal
     * @param portal the portal to get indexing schedules for
     * @return a {@link List} of all the {@link IndexSchedule}s for the given portal
     */
    public List<IndexSchedule> getIndexSchedules(Portal portal);
    
    /**
     * Gets the index schedules for the given class.
     * @param clazz The class to get the schedules for
     * @return a list of schedules for the given class
     */
    public List<IndexSchedule> getIndexSchedulesForClass(Class<? extends PersistentImpl> clazz);
    
    /**
     * Gets the {@link IndexSchedule} with the given id.
     * @param id
     * @return
     */
    public IndexSchedule getIndexSchedule(int id);
}
