/**
 * 
 */
package au.com.gaiaresources.bdrs.service.facet.location;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.UserFacetOption;

/**
 * A facet option that filters locations to a specified user.
 * 
 * @author stephanie
 *
 */
public class LocationUserFacetOption extends UserFacetOption {

    public LocationUserFacetOption(User user, Long count, String[] selectedOpts) {
        super(user, count, selectedOpts);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.option.UserFacetOption#getMyDisplayText()
     */
    @Override
    protected String getMyDisplayText() {
        return "My Locations Only";
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.option.FacetOption#getPredicate()
     */
    @Override
    public Predicate getPredicate() {
        Predicate p = Predicate.eq("user.id", user.getId());
        return p;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.option.FacetOption#getIndexedQueryString()
     */
    @Override
    public String getIndexedQueryString() {
        return "user.id:"+user.getId();
    }
}
