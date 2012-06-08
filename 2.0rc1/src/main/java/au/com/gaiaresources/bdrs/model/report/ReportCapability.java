package au.com.gaiaresources.bdrs.model.report;

/**
 * The <code>ReportCapability</code> represents the ability of a report to consume data when the Python
 * reporting function <code>Report.content(...)</code> is invoked.
 */
public enum ReportCapability {
    /**
     * A scrollable records instance may be passed to the report.
     */
    SCROLLABLE_RECORDS
}
