package au.com.gaiaresources.bdrs.controller.taxonomy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Handle creation of attribute to parameter name mapping for taxon group attributes.
 * 
 * @author stephanie
 */
@SuppressWarnings("unchecked")
public class TaxonomyAttributeDictionaryFactory extends
        AbstractAttributeDictionaryFactory {
    
    protected static final Set<AttributeScope> SCOPE_NULL;
    
    static {
        Set<AttributeScope> tmp = new HashSet<AttributeScope>(1);
        tmp.add(null);
        SCOPE_NULL = Collections.unmodifiableSet(tmp);
    }
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory#createFileKeyDictionary(java.util.List, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, java.util.List)
     */
    @Override
    public Map<Attribute, Object> createFileKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod) {
        throw new IllegalStateException("not supported");
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory#createNameKeyDictionary(java.util.List, au.com.gaiaresources.bdrs.model.taxa.TaxonGroup, java.util.List)
     */
    @Override
    public Map<Attribute, Object> createNameKeyDictionary(List<Survey> survey,
            TaxonGroup taxonGroup, List<CensusMethod> censusMethod) {
        throw new IllegalStateException("not supported");
    }

    /**
     * Create a name key dictionary for the files of the given species, scope and dataMap.
     * @param species the {@link IndicatorSpecies} that contains the attributes to map
     * @param scope a {@link Set} of {@link AttributeScope} to filter the {@link Attribute}s to map
     * @param dataMap the parameter map of the request
     * @return A name key dictionary for the species from the dataMap values
     */
    public Map<Attribute, Object> createNameKeyDictionary(IndicatorSpecies species, Set<AttributeScope> scope,
                                                          Map<String, String[]> dataMap) {
        return createDictionary(species, scope, dataMap, false);
    }

    /**
     * Create a name key dictionary for the files of the given species, scope and dataMap.
     * @param species the {@link IndicatorSpecies} that contains the attributes to map
     * @param scope a {@link Set} of {@link AttributeScope} to filter the {@link Attribute}s to map
     * @param dataMap the parameter map of the request
     * @return A name key dictionary for the species from the dataMap values
     */
    public Map<Attribute, Object> createFileKeyDictionary(IndicatorSpecies species, Set<AttributeScope> scope,
                                                          Map<String, String[]> dataMap) {
        return createDictionary(species, scope, dataMap, true);
    }
    
    /**
     * Create a name key dictionary for the given species, scope and dataMap.
     * @param species the {@link IndicatorSpecies} that contains the attributes to map
     * @param scope a {@link Set} of {@link AttributeScope} to filter the {@link Attribute}s to map
     * @param dataMap the parameter map of the request
     * @param isFileDictionary boolean flag indicating if the parameter names should be in file format or not
     * @return A name key dictionary for the species from the dataMap values
     */
    protected Map<Attribute, Object> createDictionary(IndicatorSpecies species,
                                                      Set<AttributeScope> scope,
                                                      Map<String, String[]> dataMap, 
                                                      boolean isFileDictionary) {

          Map<Attribute, Object> result = new HashMap<Attribute, Object>();
          Set<String> check = new HashSet<String>();
          
          HashSet<Integer> existingCMs = new HashSet<Integer>();
          Map<Attribute, AttributeValue> attrValMap = new HashMap<Attribute, AttributeValue>();

          TaxonGroup taxonGroup = null;
          if (species != null) {
              taxonGroup = species.getTaxonGroup();
              for (AttributeValue value : species.getAttributes()) {
                  attrValMap.put(value.getAttribute(), value);
              }
          }
          if (taxonGroup != null && taxonGroup.getAttributes() != null) {
              addAttributesToMap(result, attrValMap, taxonGroup.getAttributes(), scope, check, "", "Taxon group", dataMap, existingCMs, isFileDictionary);
              existingCMs.clear();
          }
          
          return result;
      }
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.attribute.AbstractAttributeDictionaryFactory#getDictionaryAttributeScope()
     */
    @Override
    public Set<AttributeScope> getDictionaryAttributeScope() {
        return SCOPE_NULL;
    }

}
