package au.com.gaiaresources.bdrs.service.facet.location;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.service.facet.option.SurveyFacetOption;

/**
 * A facet option that represents a survey by which to restrict locations.
 * 
 * @author stephanie
 *
 */
public class LocationSurveyFacetOption extends SurveyFacetOption {

    public LocationSurveyFacetOption(Survey survey, Long count,
            String[] selectedOpts) {
        super(survey, count, selectedOpts);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.option.SurveyFacetOption#getPredicate()
     */
    @Override
    public Predicate getPredicate() {
        return Predicate.eq("survey.id", survey.getId());
    }

    @Override
    public String getIndexedQueryString() {
        return "surveys.id:"+survey.getId();
    }
}
