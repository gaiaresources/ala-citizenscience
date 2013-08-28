package au.com.gaiaresources.bdrs.service.facet.record;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.service.facet.option.SurveyFacetOption;

/**
 * Represents a single survey containing records. 
 */
public class RecordSurveyFacetOption extends SurveyFacetOption {
    /**
     * Creates a new instance of this class.
     * 
     * @param survey the human readable name of this option.
     * @param count the number of records that match this option.
     * @param selectedOpts true if this option is applied, false otherwise.
     */
    public RecordSurveyFacetOption(Survey survey, Long count, String[] selectedOpts) {
        super(survey, count, selectedOpts);
    }

    /**
     * Returns the predicate represented by this option that may be applied to a
     * query.
     */
    public Predicate getPredicate() {
        return Predicate.eq("record.survey.id", survey.getId());
    }
    
    /**
     * @return the survey that this facet option represents
     */
    public Survey getSurvey() {
        return survey;
    }
}
