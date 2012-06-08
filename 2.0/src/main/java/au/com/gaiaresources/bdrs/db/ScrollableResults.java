package au.com.gaiaresources.bdrs.db;

import java.util.Enumeration;

/**
 * Represents a lazy loaded enumeration of an Object of type T. The purpose of the
 * <code>ScrollableResults</code> interface is the provide a single api
 * for retrieving Objects of type T from a result set.
 *  
 * @author stephanie
 */
public interface ScrollableResults<T> extends Enumeration<T> {
    public static final int RESULTS_BATCH_SIZE = 500;
    
    /**
     * Rewinds the underlying iterator
     */
    public void rewind();
}
