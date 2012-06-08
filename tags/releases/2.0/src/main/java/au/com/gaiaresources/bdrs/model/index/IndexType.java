package au.com.gaiaresources.bdrs.model.index;

/**
 * Enumeration specifying when an IndexSchedule should be scheduled.
 * 
 * @author stephanie
 *
 */
public enum IndexType {
    SERVER_STARTUP,
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}
