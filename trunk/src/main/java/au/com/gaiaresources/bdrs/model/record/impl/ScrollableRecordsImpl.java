package au.com.gaiaresources.bdrs.model.record.impl;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;

import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;

/**
 * Base implmentation of the {@link ScrollableRecords}. This implementation
 * converts a {@link Query} into a {@link ScrollableResults} and provides the
 * necessary casting to fulfill the {@link ScrollableRecords} implementation. 
 */
public class ScrollableRecordsImpl extends ScrollableResultsImpl<Record> implements ScrollableRecords {
    
    public ScrollableRecordsImpl(Query query) {
        super(query);
    }
    
    /**
     * Creates a new instance.   
     * @param query the query for a set of Records.
     * @param pageNumber the 1 indexed paging of results
     * @param entriesPerPage the number of records per page of data.  
     */
    public ScrollableRecordsImpl(Query query, int pageNumber, int entriesPerPage) {
        super(query, pageNumber, entriesPerPage);
    }

}
