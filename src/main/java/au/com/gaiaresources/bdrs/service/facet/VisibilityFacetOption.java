package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;

/**
 * Each instance of VisibilityFacetOption represents a Facet that will filter on particular values of a
 * Records recordVisibility property. (e.g. PUBLIC, CONTROLLED or OWNER_ONLY).
 */
public class VisibilityFacetOption extends FacetOption {

    /** The record visibility to filter on. */
    private RecordVisibility recordVisibility;

    /**
     * Creates a new VisibilityFacetOption.
     * @param recordVisibility the record visibility value that this facet filters on.
     * @param count the number of records that match this option.
     * @param selected true if this facet has been selected.
     */
    public VisibilityFacetOption(RecordVisibility recordVisibility, Long count, boolean selected) {
        super(recordVisibility.getDescription(), Integer.toString(recordVisibility.ordinal()), count, selected);
        this.recordVisibility = recordVisibility;
    }

    @Override
    public Predicate getPredicate() {
        return Predicate.eq("record.recordVisibility", recordVisibility);
    }
}
package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;

/**
 * Each instance of VisibilityFacetOption represents a Facet that will filter on particular values of a
 * Records recordVisibility property. (e.g. PUBLIC, CONTROLLED or OWNER_ONLY).
 */
public class VisibilityFacetOption extends FacetOption {

    /** The record visibility to filter on. */
    private RecordVisibility recordVisibility;

    /**
     * Creates a new VisibilityFacetOption.
     * @param recordVisibility the record visibility value that this facet filters on.
     * @param count the number of records that match this option.
     * @param selected true if this facet has been selected.
     */
    public VisibilityFacetOption(RecordVisibility recordVisibility, Long count, boolean selected) {
        super(recordVisibility.getDescription(), Integer.toString(recordVisibility.ordinal()), count, selected);
        this.recordVisibility = recordVisibility;
    }

    @Override
    public Predicate getPredicate() {
        return Predicate.eq("record.recordVisibility", recordVisibility);
    }
}
