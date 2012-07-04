package au.com.gaiaresources.bdrs.attribute;

import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;

public interface AttributeDictionaryFactory {
    
    /**
     * Creates a unique string identifier for each attribute in the argument objects
     * 
     * @param survey
     * @param taxonGroup
     * @param censusMethod
     * @return
     */
    public Map<Attribute, Object> createNameKeyDictionary(Record record, Survey survey, Location location, TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap);
    
    
    public Map<Attribute, Object> createNameKeyDictionary(List<Survey> survey, TaxonGroup taxonGroup, List<CensusMethod> censusMethod);
    
    /**
     * Creates a unique string identifier for each attribute in the argument objects
     * 
     * @param survey
     * @param taxonGroup
     * @param censusMethod
     * @return
     */
    public Map<Attribute, Object> createFileKeyDictionary(Record record, Survey survey, Location location, TaxonGroup taxonGroup, CensusMethod censusMethod, Set<AttributeScope> scope, Map<String, String[]> dataMap);
    
    public Map<Attribute, Object> createFileKeyDictionary(List<Survey> survey, TaxonGroup taxonGroup, List<CensusMethod> censusMethod);


    public Map<Attribute, Object> createFileKeyDictionary(Record record, Survey survey, Location location,
            TaxonGroup taxonGroup, CensusMethod censusMethod, Map<String, String[]> dataMap);
    
    public Set<AttributeScope> getDictionaryAttributeScope();

    public Map<Attribute, Object> createNameKeyDictionary(Record record, Survey survey, Location location, 
            TaxonGroup taxonGroup, CensusMethod censusMethod, Map<String, String[]> dataMap);
}
