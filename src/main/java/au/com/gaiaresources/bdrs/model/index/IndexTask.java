package au.com.gaiaresources.bdrs.model.index;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.util.TransactionHelper;

/**
 * Provides an indexing task which will (optionally) delete indexes and 
 * rebuild them and can be scheduled.
 * 
 * @author stephanie
 *
 */
public class IndexTask implements Runnable {

    private static Logger log = Logger.getLogger(IndexTask.class);
    
    private Integer scheduleId;
    private SearchService searchService;
    private IndexScheduleDAO indexDAO;
    private SessionFactory sessionFactory;
    
    /**
     * Create a task that can be scheduled to build an index.
     * @param sessionFactory   sessionfactory for creating a new session
     * @param service          the SearchService which creates the indexes
     * @param indexScheduleDAO the dao to retrieve a thread-local copy of the IndexSchedule
     * @param scheduleId       the id of the persisted schedule 
     */
    public IndexTask(SessionFactory sessionFactory, SearchService service, IndexScheduleDAO indexScheduleDAO, Integer scheduleId) {
        this.sessionFactory = sessionFactory;
        this.searchService = service;
        this.indexDAO = indexScheduleDAO;
        this.scheduleId = scheduleId;
    }

    @Override
    public void run() {
        // open a new session for updating the time of the schedule
        Session sesh = sessionFactory.openSession();
        Transaction tx = sesh.beginTransaction();
        // first get a thread local copy of the schedule from the dao
        IndexSchedule schedule = indexDAO.getIndexSchedule(sesh, scheduleId);
        
        try {
            if (schedule != null) {
                Class clazz = null;
                try {
                    Map<String,String> names = IndexUtil.getFullNamesForIndexedClasses(new String[]{schedule.getClassName()});
                    String className = names.get(schedule.getClassName());
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    log.warn("Could not find requested index class: "+schedule.getClassName()+", rebuilding indexes for all classes", e);
                }
                if (schedule.isFullRebuild()) {
                    if (clazz != null) {
                        searchService.deleteIndex(sesh, clazz, schedule.getPortal());
                    } else {
                        searchService.deleteIndexes(sesh, schedule.getPortal());
                    }
                }
                if (clazz != null) {
                    searchService.createIndex(sesh, clazz, schedule.getPortal());
                } else {
                    searchService.createIndexes(sesh, schedule.getPortal());
                }
        
                // update the last run time of the schedule
                schedule.setLastRun(new Date());
                schedule = indexDAO.update(sesh, schedule);
                // commit the transaction and close the session
                sesh.flush();
                sesh.clear();
                
                TransactionHelper.commit(tx, sesh);
            }
        } catch (Exception e) {
            log.error("An error occurred updating the run time for index "+(schedule != null ? schedule.getId() : null), e);
            tx.rollback();
        } finally {
            sesh.close();
        }
    }
}
