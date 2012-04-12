package au.com.gaiaresources.bdrs.service.facet.builder;

import java.util.Map;

import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.location.LocationSurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.record.RecordSurveyFacet;

/**
 * The concrete implementation of the {@link AbstractFacetBuilder} that creates
 * {@link RecordSurveyFacet}s.
 */
public class SurveyFacetBuilder extends AbstractFacetBuilder<SurveyFacet> {
    
    /**
     * Describes the function of this facet that will be used in the preference description.
     */
    public static final String FACET_DESCRIPTION = "Represents records on a per survey basis.";
    
    /**
     * The human readable name of this facet.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Survey";
    
    /**
     * Creaes a new instance.
     */
    public SurveyFacetBuilder(Class applyClass) {
        super(SurveyFacet.class);
        if (Record.class.equals(applyClass)) {
            facetClass = RecordSurveyFacet.class;
        } else if (Location.class.equals(applyClass)) {
            facetClass = LocationSurveyFacet.class;
        } 
    }
    
    @Override
    public String getPreferenceDescription() {
        return buildPreferenceDescription(FACET_DESCRIPTION, getFacetParameterDescription());
    }
    
    @Override
    public Facet createFacet(FacetDAO transDAO, Map<String, String[]> parameterMap, User user, JSONObject userParams, Class applyClass) {
        if (Record.class.equals(applyClass)) {
            return new RecordSurveyFacet(DEFAULT_DISPLAY_NAME, transDAO, parameterMap, user, userParams);
        } else if (Location.class.equals(applyClass)) {
            return new LocationSurveyFacet(DEFAULT_DISPLAY_NAME, transDAO, parameterMap, user, userParams);
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
        if (Record.class.equals(facetClass)) {
            return new RecordSurveyFacet(DEFAULT_DISPLAY_NAME, dao, parameterMap, user, userParams);
        } else if (Location.class.equals(facetClass)) {
            return new LocationSurveyFacet(DEFAULT_DISPLAY_NAME, dao, parameterMap, user, userParams);
        } else {
            throw new IllegalArgumentException("facetClass must be one of Record or Location, but was "+facetClass.getName());
        }
    }
}