package au.com.gaiaresources.bdrs.model.taxa.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.region.RegionDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;

/**
 * Implementation of <code>TaxaService</code>.
 * @author Tim Carpenter
 *
 */
@Service
public class TaxaServiceImpl implements TaxaService {
	Logger log = Logger.getLogger(TaxaServiceImpl.class);
	
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RegionDAO regionDAO;
    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;
    @Autowired
    private AttributeDAO attributeDAO;  
    
    
    @Override
    public TaxonGroup createTaxonGroup(String name, boolean includeBehaviour, boolean includeFirstAppearance,
                                       boolean includeLastAppearance, boolean includeHabitat, boolean includeWeather,
                                       boolean includeNumber)
    {
        return taxaDAO.createTaxonGroup(name, includeBehaviour, includeFirstAppearance, includeLastAppearance,
                                        includeHabitat, includeWeather, includeNumber);
    }
    
    @Override
	public TaxonGroup createTaxonGroup(String name, boolean includeBehaviour,
			boolean includeFirstAppearance, boolean includeLastAppearance,
			boolean includeHabitat, boolean includeWeather,
			boolean includeNumber, String image, String thumbNail) {
		return taxaDAO.createTaxonGroup(name, includeBehaviour, includeFirstAppearance, includeLastAppearance, includeHabitat, includeWeather, includeNumber, image, thumbNail);
	}
    
    
    @Override
    public TaxonGroup updateTaxonGroup(Integer id, String name, boolean includeBehaviour, boolean includeFirstAppearance,
                                       boolean includeLastAppearance, boolean includeHabitat, boolean includeWeather,
                                       boolean includeNumber)
    {
        return taxaDAO.updateTaxonGroup(id, name, includeBehaviour, includeFirstAppearance, includeLastAppearance,
                                        includeHabitat, includeWeather, includeNumber);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TaxonGroup getTaxonGroup(Integer id) {
        return taxaDAO.getTaxonGroup(id);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TaxonGroup getTaxonGroup(String name) {
        return taxaDAO.getTaxonGroup(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends TaxonGroup> getTaxonGroups() {
        return taxaDAO.getTaxonGroups();
    }
    
    /**
     * {@inheritDoc}
     */
    public Attribute createAttribute(TaxonGroup group, String name, AttributeType type, boolean required) {
        return taxaDAO.createAttribute(group, name, type, required);
    }
    
    /**
     * {@inheritDoc}
     */
    public Attribute createAttribute(TaxonGroup group, String name, AttributeType type, boolean required, boolean isTag) {
        return taxaDAO.createAttribute(group, name, type, required, isTag);
    }
    
    /**
     * {@inheritDoc}
     */
    public AttributeOption createAttributeOption(Attribute attribute, String value) {
    	return taxaDAO.createAttributeOption(attribute, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public Attribute updateAttribute(Integer id, String name, AttributeType type, boolean required) {
        return taxaDAO.updateAttribute(id, name, type, required);
    }
    
    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute(Integer id) {
        return taxaDAO.getAttribute(id);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IndicatorSpecies createIndicatorSpecies(String scientificName, String commonName, TaxonGroup taxonGroup,
                                                   Collection<String> regionNames, List<SpeciesProfile> infoItems) 
    {
        return taxaDAO.createIndicatorSpecies(scientificName, commonName, taxonGroup, 
                                              regionDAO.getRegions(regionNames.toArray(new String[regionNames.size()])), infoItems);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IndicatorSpecies updateIndicatorSpecies(Integer id, String scientificName, String commonName, 
                                                   TaxonGroup taxonGroup, Collection<String> regionNames, List<SpeciesProfile> infoItems)
    {
        return taxaDAO.updateIndicatorSpecies(id, scientificName, commonName, taxonGroup, 
                                              regionDAO.getRegions(regionNames.toArray(new String[regionNames.size()])), infoItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IndicatorSpecies> getIndicatorSpecies() {
        return taxaDAO.getIndicatorSpecies();
    }
    
    public Map<TaxonGroup, List<IndicatorSpecies>> getGroupedIndicatorSpecies() {
        Map<TaxonGroup, List<IndicatorSpecies>> grouped = new HashMap<TaxonGroup, List<IndicatorSpecies>>();
        //TODO: change 'getIndicatorSpecies()' to 'getIndicatorSpecies(groupSurveyId)'
        for (IndicatorSpecies i : getIndicatorSpecies()) {
            if (!grouped.containsKey(i.getTaxonGroup())) {
                grouped.put(i.getTaxonGroup(), new ArrayList<IndicatorSpecies>());
            }
            grouped.get(i.getTaxonGroup()).add(i);
        }
        
        return grouped;
    }
    
    public Map<TaxonGroup, List<IndicatorSpecies>> getGroupedIndicatorSpecies(Region region) {
        Map<TaxonGroup, List<IndicatorSpecies>> grouped = new HashMap<TaxonGroup, List<IndicatorSpecies>>();
        
        for (IndicatorSpecies i : getIndicatorSpecies(region)) {
            if (!grouped.containsKey(i.getTaxonGroup())) {
                grouped.put(i.getTaxonGroup(), new ArrayList<IndicatorSpecies>());
            }
            grouped.get(i.getTaxonGroup()).add(i);
        }
        
        return grouped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IndicatorSpecies> getIndicatorSpecies(Region region) {
        return taxaDAO.getIndicatorSpecies(region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IndicatorSpecies> getIndicatorSpecies(TaxonGroup group) {
        return taxaDAO.getIndicatorSpecies(group);
    }
    
    @Override
    public IndicatorSpecies getIndicatorSpecies(Integer speciesID) {
        return taxaDAO.getIndicatorSpecies(speciesID);
    }
    
    @Override
    public List<? extends IndicatorSpecies> getIndicatorSpecies(String commonName) {
    	return taxaDAO.getIndicatorSpeciesByCommonName(commonName);
    }
    
    public List<String> getGroupedIndicatorSpeciesNames(){
    	return null;
    }

	@Override
	public List<String> getTaxonGroupNames(
			Map<TaxonGroup, List<IndicatorSpecies>> groupData) {
		List<String> names = new ArrayList<String>();
		for(Map.Entry<TaxonGroup, List<IndicatorSpecies>> entry : groupData.entrySet()){
			names.add(entry.getKey().getName());
		}
		return names;
	}
	
	@Override
	public List<? extends IndicatorSpecies> getIndicatorSpeciesByNameSearch(
			String name) {
		return taxaDAO.getIndicatorSpeciesByNameSearch(name, false);
	}

	@Override
	public SpeciesProfile createSpeciesProfile(String header, String content, String type) {
		return speciesProfileDAO.createSpeciesProfile(header, content, type);
	}
	
	@Override
	public List<SpeciesProfile> getSpeciesProfile(String type, IndicatorSpecies species){
		List <SpeciesProfile> infoItems_subset = new ArrayList<SpeciesProfile>();
		List<SpeciesProfile> infoItems  = species.getInfoItems();
		log.debug("SpeciesProfile_Items:"+infoItems.size());
    	for (SpeciesProfile s : infoItems){
    		if(s.getType().equalsIgnoreCase(type)){
    			infoItems_subset.add(s);
    		}
    	}
        return infoItems_subset;
    }

    @Override
    public Attribute getFieldNameAttribute() {
        return getFieldNameAttribute(null);
    }

    @Override
    public IndicatorSpecies getFieldSpecies() {
        return getFieldSpecies(null);
    }
    
    @Override
    public TaxonGroup getFieldNameGroup(Session sesh) {
        TaxonGroup fieldNamesGroup = taxaDAO.getTaxonGroup(null, TaxonGroup.FIELD_NAMES_GROUP_NAME);
        if (fieldNamesGroup == null) {
            fieldNamesGroup = createFieldNameTaxonGroup();
            fieldNamesGroup = taxaDAO.save(sesh, fieldNamesGroup);
        }
        return fieldNamesGroup;
    }
    
    @Override
    public TaxonGroup getFieldNameGroup() {
        return getFieldNameGroup(null);
    }

    @Override
    public IndicatorSpecies getFieldSpecies(Session sesh) {
        IndicatorSpecies fieldSpecies = taxaDAO.getIndicatorSpeciesByScientificName(sesh, IndicatorSpecies.FIELD_SPECIES_NAME);
        if (fieldSpecies == sesh) {
            TaxonGroup fieldNamesGroup = getFieldNameGroup(sesh);
            fieldSpecies = createFieldSpecies(fieldNamesGroup);
            fieldSpecies = taxaDAO.save(sesh, fieldSpecies);
        }

        return fieldSpecies;
    }

    @Override
    public Attribute getFieldNameAttribute(Session sesh) {
        IndicatorSpecies fieldSpecies = getFieldSpecies();
        TaxonGroup fieldNamesGroup = fieldSpecies.getTaxonGroup();

        for (Attribute attr : fieldNamesGroup.getAttributes()) {
            if (Attribute.FIELD_NAME_NAME.equals(attr.getName())
                    && Attribute.FIELD_NAME_DESC.equals(attr.getDescription())) {
                return attr;
            }
        }

        // if we got here, there isn't a field name attribute yet - so create one.
        Attribute attr = createFieldNameAttr(fieldNamesGroup);
        
        attr = attributeDAO.save(sesh, attr);
        taxaDAO.save(sesh, fieldNamesGroup);

        return attr;
    }
    
    /**
     * Creates a new instance of the field name TaxonGroup. Does not persist the object.
     * Note that there should be only one of these per portal.
     * 
     * @return Created TaxonGroup.
     */
    public static TaxonGroup createFieldNameTaxonGroup() {
        TaxonGroup fieldNamesGroup = new TaxonGroup();
        fieldNamesGroup.setName(TaxonGroup.FIELD_NAMES_GROUP_NAME);
        fieldNamesGroup.setBehaviourIncluded(false);
        fieldNamesGroup.setFirstAppearanceIncluded(false);
        fieldNamesGroup.setHabitatIncluded(false);
        fieldNamesGroup.setLastAppearanceIncluded(false);
        fieldNamesGroup.setNumberIncluded(false);
        fieldNamesGroup.setWeatherIncluded(false);
        return fieldNamesGroup;
    }
    
    /**
     * Creates a new instance of the field name IndicatorSpecies. Does not persist the object.
     * Note that there should be only one of these per portal.
     * 
     * @param fieldNamesGroup TaxonGroup that is assigned to the indicator species.
     * @return Created IndicatorSpecies.
     */
    public static IndicatorSpecies createFieldSpecies(TaxonGroup fieldNamesGroup) {
        IndicatorSpecies fieldSpecies = new IndicatorSpecies();
        fieldSpecies.setScientificName(IndicatorSpecies.FIELD_SPECIES_NAME);
        fieldSpecies.setCommonName(IndicatorSpecies.FIELD_SPECIES_NAME);

        fieldSpecies.setAuthor("");
        fieldSpecies.setScientificNameAndAuthor("");
        fieldSpecies.setTaxonGroup(fieldNamesGroup);
        fieldSpecies.setTaxonRank(TaxonRank.SPECIES);
        fieldSpecies.setYear("");
        return fieldSpecies;
    }
    
    /**
     * Creates a new instance of the field name attribute. Does not persist the object.
     * Note that there should be only one of these per portal.
     * 
     * @param fieldNamesGroup TaxonGroup to attach the attribute.
     * @return Field name Attribute
     */
    public static Attribute createFieldNameAttr(TaxonGroup fieldNamesGroup) {
        Attribute attr = new Attribute();
        attr.setDescription(Attribute.FIELD_NAME_DESC);
        attr.setName(Attribute.FIELD_NAME_NAME);
        attr.setRequired(false);
        attr.setTag(false);
        attr.setTypeCode(AttributeType.STRING.getCode());
        fieldNamesGroup.getAttributes().add(attr);
        return attr;
    }
}
