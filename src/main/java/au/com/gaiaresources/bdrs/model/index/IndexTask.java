package au.com.gaiaresources.bdrs.model.index;

import java.util.Date;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.search.SearchService;

/**
 * Provides an indexing task which will (optionally) delete indexes and 
 * rebuild them and can be scheduled.
 * 
 * @author stephanie
 *
 */
public class IndexTask implements Runnable {

    private static Logger log = Logger.getLogger(IndexTask.class);
    
    private boolean deleteIndex = false;
    
    private SearchService searchService;
    private String className;
    
    public IndexTask(SearchService searchService, boolean deleteIndex, String indexClass) {
        this.deleteIndex = deleteIndex;
        this.searchService = searchService;
        this.className = indexClass;
    }

    @Override
    public void run() {
        log.debug("running index task at: "+new Date(System.currentTimeMillis()));
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (deleteIndex) {
            if (clazz != null) {
                searchService.deleteIndex(clazz);
            } else {
                searchService.deleteIndexes();
            }
        }
        if (clazz != null) {
            searchService.createIndex(clazz);
        } else {
            searchService.createIndexes();
        }
    }
}
