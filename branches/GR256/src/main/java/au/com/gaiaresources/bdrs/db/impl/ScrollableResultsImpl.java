package au.com.gaiaresources.bdrs.db.impl;

import java.util.NoSuchElementException;

import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;

/**
 * Base implmentation of the {@link ScrollableResults}. This implementation
 * converts a {@link Query} into a {@link ScrollableResults} and provides the
 * necessary casting to fulfill the {@link ScrollableResults} implementation. 
 * @author stephanie
 *
 */
public class ScrollableResultsImpl<T> implements ScrollableResults<T> {
    private org.hibernate.ScrollableResults results;
    
    private boolean hasMoreElements = INIT_HAS_MORE_ELEMENTS;
    private int entriesPerPage = -1;
    private int currentPageEntryIndex = INIT_CURRENT_PAGE_ENTRY_INDEX;
    private T result = null;
    
    private static final int INIT_CURRENT_PAGE_ENTRY_INDEX = -1;
    public static final boolean INIT_HAS_MORE_ELEMENTS = false;
    
    /**
     * Creates a new instance.
     * @param query the query for a set of T.
     */
    public ScrollableResultsImpl(Query query) {
        if (query == null) {
            results = null;
        } else {
            // not using 'forward only' scroll mode so we can rewind the scrollable results
            results = query.setCacheMode(CacheMode.IGNORE).setFetchSize(ScrollableRecords.RESULTS_BATCH_SIZE).scroll(ScrollMode.SCROLL_SENSITIVE);
            nextRecord();
        }
    }

    /**
     * Creates a new instance.   
     * @param query the query for a set of Records.
     * @param pageNumber the 1 indexed paging of results
     * @param entriesPerPage the number of records per page of data.  
     */
    public ScrollableResultsImpl(Query query, int pageNumber, int entriesPerPage) {
        if (query == null) {
            results = null;
        } else {
            results = query.setCacheMode(CacheMode.IGNORE).setFetchSize(ScrollableRecords.RESULTS_BATCH_SIZE).scroll();
            this.entriesPerPage = entriesPerPage;
            
            // To set the row number, the scoll mode cannot be forward only.
            
            // The currentPageEntryIndex is set to -1 so that after the invocation
            // to recordAt, it will be appropriately set to 0. If you do not do this,
            // hasMoreElements will return false.
            // currentPageEntryIndex = -1;
            recordAt((pageNumber-1) * entriesPerPage);
        }
    }

    @Override
    public boolean hasMoreElements() {
        if(result == null) {
            nextRecord();
        }
        boolean nextEntryAllowed = (entriesPerPage < 0) || (entriesPerPage > -1 && currentPageEntryIndex < entriesPerPage);
        
        return nextEntryAllowed && hasMoreElements;
    }

    @Override
    public T nextElement() {
        if(result == null) {
            throw new NoSuchElementException();
        } else {
            T r = result;
            result = null;
            return r;
        }
    }
    
    @Override
    public void rewind() {
        currentPageEntryIndex = INIT_CURRENT_PAGE_ENTRY_INDEX;
        hasMoreElements = INIT_HAS_MORE_ELEMENTS;
        // rewind the underlying ScrollableResults
        results.beforeFirst();
        nextRecord();
    }
    
    private void nextRecord() {
        if (results != null) {
            hasMoreElements = results.next();
            result = hasMoreElements ? (T)results.get(0) : null;
            currentPageEntryIndex++;
        }
    }
    
    private void recordAt(int rowNum) {
        if (results != null) {
            hasMoreElements = results.setRowNumber(rowNum);
            result = hasMoreElements ? (T)results.get(0) : null;
            currentPageEntryIndex++;
        }
    }
}
