package au.com.gaiaresources.bdrs.service.facet.builder;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import au.com.gaiaresources.bdrs.json.JSONObject;

import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.UserFacet;
import au.com.gaiaresources.bdrs.service.facet.location.LocationUserFacet;
import au.com.gaiaresources.bdrs.service.facet.record.RecordUserFacet;

/**
 * The concrete implementation of the {@link AbstractFacetBuilder} that creates
 * {@link UserFacet}s.
 */
public class UserFacetBuilder extends AbstractFacetBuilder<UserFacet> {
    
    /**
     * Describes the function of this facet that will be used in the preference description.
     */
    public static final String FACET_DESCRIPTION = "Restricts records to the selected set of users.";
    
    /**
     * The human readable name of this facet.
     */
    public static final String DEFAULT_DISPLAY_NAME = "User";
    
    /**
     * Creaes a new instance.
     */
    public UserFacetBuilder(Class applyClass) {
        super(UserFacet.class);
        if (Record.class.equals(applyClass)) {
            facetClass = RecordUserFacet.class;
        } else if (Location.class.equals(applyClass)) {
            facetClass = LocationUserFacet.class;
        }
    }
    
    @Override
    public String getPreferenceDescription() {
        return buildPreferenceDescription(FACET_DESCRIPTION, getFacetParameterDescription());
    }

    @Override
    public Facet createFacet(FacetDAO facetDAO, Map<String, String[]> parameterMap, User user, JSONObject userParams, Class applyClass) {
        if (Record.class.equals(applyClass)) {
            return new RecordUserFacet(DEFAULT_DISPLAY_NAME, facetDAO, parameterMap, user, userParams);
        } else if (Location.class.equals(applyClass)) {
            return new LocationUserFacet(DEFAULT_DISPLAY_NAME, facetDAO, parameterMap, user, userParams);
        } else {
            throw new IllegalArgumentException("applyClass must be one of Record or Location, but was "+applyClass.getName());
        }
    }

    @Override
    public String getDefaultDisplayName() {
        return DEFAULT_DISPLAY_NAME;
    }

    @Override
    protected Facet createFacet(FacetDAO dao,
            Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        // not valid for this builder
        throw new NotImplementedException("The method createFacet(FacetDAO dao, " +
                "Map<String, String[]> parameterMap, User user, JSONObject userParams) " +
                "is not valid for UserFacetBuilder.  Use createFacet(FacetDAO facetDAO, " +
                "Map<String, String[]> parameterMap, User user, JSONObject userParams, Class applyClass) instead.");
    }
}