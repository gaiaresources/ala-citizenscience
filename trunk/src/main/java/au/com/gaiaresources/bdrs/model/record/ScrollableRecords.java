package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.db.ScrollableResults;

/**
 * Represents a lazy loaded enumeration of Records. The purpose of the
 * <code>ScrollableRecords</code> interface is the provide a single api
 * for retrieving records from a result set. 
 */
public interface ScrollableRecords extends ScrollableResults<Record> {

}
