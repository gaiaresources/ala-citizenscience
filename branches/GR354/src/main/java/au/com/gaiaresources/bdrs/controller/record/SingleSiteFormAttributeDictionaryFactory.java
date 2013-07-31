package au.com.gaiaresources.bdrs.controller.record;

import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;

/**
 * @author stephanie
 */
public class SingleSiteFormAttributeDictionaryFactory extends
        AbstractAttributeDictionaryFactory {

    /**
     * These methods are not supported in the single site form input. At the moment it is impossible
     * to submit multiple records from different surveys and different census methods. If
     * this behaviour is required in the future these methods can be implemented. 
     */
    @Override
    public Map<Attribute, Object> createFileKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public Map<Attribute, Object> createNameKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public Set<AttributeScope> getDictionaryAttributeScope() {
        return SCOPE_RECORD_SURVEY;
    }
}
