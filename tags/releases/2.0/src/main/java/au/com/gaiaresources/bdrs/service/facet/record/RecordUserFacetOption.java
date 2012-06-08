package au.com.gaiaresources.bdrs.service.facet.record;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.UserFacetOption;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

/**
 * The </code>RecordUserFacetOption</code> represents a single user whose records
 * will be retrieved. 
 */
public class RecordUserFacetOption extends UserFacetOption {
    
    /**
     * Creates a new instance of this class.
     * 
     * @param user the human readable name of this option.
     * @param count the number of records that match this option.
     * @param selectedOpts true if this option is applied, false otherwise.
     */
    public RecordUserFacetOption(User user, Long count, String[] selectedOpts) {
        super(user, count, selectedOpts);
    }

    /**
     * Returns the predicate represented by this option that may be applied to a
     * query.
     */
    public Predicate getPredicate() {
        Predicate p = Predicate.eq("record.user.id", user.getId());
        // if the request context user is null or not an admin, 
        // this is anonymous access and only public records for 
        // the user should be shown
        User accessor = RequestContextHolder.getContext().getUser();
        if (accessor == null || (!accessor.isAdmin() && !user.equals(accessor))) {
            p.and(Predicate.eq("record.recordVisibility", RecordVisibility.PUBLIC));
            // if the accessor is a moderator, they can also see held records
            // so they can moderate them
            if (accessor == null || !accessor.isModerator()) {
                p.and(Predicate.eq("record.held", false));
            }
        }
        return p;
    }

    @Override
    protected String getMyDisplayText() {
        return "My Records Only";
    }
}
