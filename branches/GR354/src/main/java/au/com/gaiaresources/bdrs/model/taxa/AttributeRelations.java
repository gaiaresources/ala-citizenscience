package au.com.gaiaresources.bdrs.model.taxa;

/**
 * Object for returning the result of querying the parent type of an Attribute object.
 *
 */
public class AttributeRelations {

    public Integer censusMethodId;
    public Integer surveyId;
    public Integer taxonGroupId;
    
    public AttributeRelations(Integer censusMethodId, Integer surveyId, Integer taxonGroupId) {
        this.censusMethodId = censusMethodId;
        this.surveyId = surveyId;
        this.taxonGroupId = taxonGroupId;
    }
    
    /**
     * If the parent is a census method, the id of that census method. 
     * Otherwise null.
     * @return id of census method row.
     */
    public Integer getCensusMethodId() {
        return censusMethodId;
    }

    /**
     * If the parent is a survey, the id of that survey.
     * Otherwise null.
     * @return id of survey row.
     */
    public Integer getSurveyId() {
        return surveyId;
    }

    /**
     * If the parent is a taxon group, the id of that taxon group.
     * Otherwise null.
     * @return id of taxon group row.
     */
    public Integer getTaxonGroupId() {
        return taxonGroupId;
    }
    
    /**
     * Convenience method for finding the parent type of an Attribute.
     * @return AttributeOwnerType enum.
     */
    public AttributeOwnerType getAttributeOwnerType() {
        if (censusMethodId != null) {
            return AttributeOwnerType.CENSUS_METHOD;
        } else if (surveyId != null) {
            return AttributeOwnerType.SURVEY;
        } else if (taxonGroupId != null) {
            return AttributeOwnerType.TAXON_GROUP;
        } else {
            // the attribute is an orphan! I.e., the attribute used to be one of the above types
            // but it has been removed from the owner. Because we have no way of knowing what it used to be,
            // treat it like a survey attribute in client code.
            return AttributeOwnerType.NONE;
        }
    }
}
