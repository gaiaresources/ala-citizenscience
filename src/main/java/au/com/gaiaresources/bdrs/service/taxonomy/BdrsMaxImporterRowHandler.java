package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.TaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.max.MaxFamilyRow;
import au.com.gaiaresources.taxonlib.importer.max.MaxGeneraRow;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporterRowHandler;
import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;
import au.com.gaiaresources.taxonlib.importer.max.MaxXrefRow;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsMaxImporterRowHandler implements MaxImporterRowHandler {
	
	private Map<String, IndicatorSpecies> iSpeciesCache = new HashMap<String, IndicatorSpecies>();
	private TaxaDAO taxaDAO;
	private SpeciesProfileDAO spDAO;
	private ITemporalContext temporalContext;
	
	public static final String MAX_GROUP_NAME = "MAX";
	
	public static final String INFO_ITEM_IS_CURRENT = "IS_CURRENT";
	public static final String INFO_ITEM_NATURALISED = "NATURALISED";
	public static final String INFO_ITEM_NATURALISED_STATUS = "NATURALISED_STATUS";
	public static final String INFO_ITEM_COMMENT = "COMMENT";
	public static final String INFO_ITEM_NATURALISED_CERTAINTY = "NATURALISED_CERTAINTY";
	public static final String INFO_ITEM_IS_ERADICATED = "IS_ERADICATED";
	public static final String INFO_ITEM_NATURALISED_COMMENTS = "NATURALISED_COMMENTS";
	public static final String INFO_ITEM_INFORMAL = "INFORMAL";
	public static final String INFO_ITEM_CONSV_CODE = "CONSV_CODE";
	
	private TaxonGroup group;
	
	public BdrsMaxImporterRowHandler(TaxonLibSession taxonLibSession, Date now, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
		if (taxonLibSession == null) {
			throw new IllegalArgumentException("TaxonLibSession cannot be null");
		}
		if (taxaDAO == null) {
			throw new IllegalArgumentException("TaxaDAO cannot be null");
		}
		if (spDAO == null) {
			throw new IllegalArgumentException("SpeciesProfileDAO cannot be null");
		}
		this.temporalContext = taxonLibSession.getTemporalContext(now);
		this.taxaDAO = taxaDAO;
		this.spDAO = spDAO;
		
		group = taxaDAO.getTaxonGroup(MAX_GROUP_NAME);
		if (group == null) {
			group = new TaxonGroup();
			group.setName(MAX_GROUP_NAME);
			group.setBehaviourIncluded(false);
			group.setLastAppearanceIncluded(false);
			group.setNumberIncluded(false);
			group.setWeatherIncluded(false);
			group.setHabitatIncluded(false);
			group = taxaDAO.save(group);
		}
	}

	@Override
	public void processFamilyRow(MaxFamilyRow row, ITaxonConcept kingdom,
			ITaxonConcept division, ITaxonConcept clazz, ITaxonConcept order,
			ITaxonConcept family) {

		saveIndicatorSpecies(kingdom, null);
		saveIndicatorSpecies(division, null);
		saveIndicatorSpecies(clazz, null);
		saveIndicatorSpecies(order, null);
		saveIndicatorSpecies(family, null);
	}
	
	@Override
	public void processGeneraRow(MaxGeneraRow row, ITaxonConcept genus) {
		saveIndicatorSpecies(genus, null);
	}

	@Override
	public void processNameRow(MaxNameRow row, ITaxonConcept species) {
		saveIndicatorSpecies(species, row);
	}
	
	private void saveIndicatorSpecies(ITaxonConcept concept, MaxNameRow nameRow) {
		if (concept != null) {
			ITaxonName tn = concept.getName();
			IndicatorSpecies iSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(null, MaxImporter.MAX_SOURCE, tn.getSourceId());
			if (iSpecies == null) {
				iSpecies = new IndicatorSpecies();
                iSpecies.setScientificName(tn.getDisplayName());
                
                if (nameRow != null) {
                	List<ITaxonName> commonNames = temporalContext.getCommonNames(concept);
                	if (commonNames != null && !commonNames.isEmpty()) {
                		// there should only ever be 1 commonName in the Max data set so...
                		String cn = commonNames.get(0).getName();
                		iSpecies.setCommonName(cn);
                	} else {
                		iSpecies.setCommonName("");
                	}
                } else {
                	iSpecies.setCommonName("");	
                }
                
                iSpecies.setAuthor(tn.getAuthor());
                iSpecies.setTaxonGroup(group);
                iSpecies.setSource(MaxImporter.MAX_SOURCE);
                iSpecies.setSourceId(tn.getSourceId());
                
                if (concept.getParent() != null) {
                	IndicatorSpecies iSpeciesParent = getIndicatorSpecies(concept.getParent().getSourceId());
                	iSpecies.setParent(iSpeciesParent);
                } else {
                	iSpecies.setParent(null);
                }
                
                // do teh funky cast....
                // There are 2 duplicate TaxonRank enums. One in the BDRS namespace and one in the TaxonLib namespace...
                TaxonRank rank = TaxonRank.valueOf(tn.getRank().toString());
                if (rank == null) {
                	throw new IllegalStateException("Something has gone wrong with our funky casting");
                }
                iSpecies.setTaxonRank(rank);
                
                if (nameRow != null) {
                	List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();
                	// add species profile...
                	addProfileInfoItem(infoItems, INFO_ITEM_IS_CURRENT, INFO_ITEM_IS_CURRENT, 
                			"Is Current", 
                			nameRow.getValue(MaxNameRow.ColumnName.IS_CURRENT));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_NATURALISED, INFO_ITEM_NATURALISED, 
                			"Naturalised", 
                			nameRow.getValue(MaxNameRow.ColumnName.NATURALISED));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_NATURALISED_STATUS, INFO_ITEM_NATURALISED_STATUS, 
                			"Naturalised Status", 
                			nameRow.getValue(MaxNameRow.ColumnName.NATURALISED_STATUS));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_NATURALISED_CERTAINTY, INFO_ITEM_NATURALISED_CERTAINTY, 
                			"Naturalised Certainty", 
                			nameRow.getValue(MaxNameRow.ColumnName.NATURALISED_CERTAINTY));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_NATURALISED_COMMENTS, INFO_ITEM_NATURALISED_COMMENTS, 
                			"Naturalised Comments", 
                			nameRow.getValue(MaxNameRow.ColumnName.NATURALISED_COMMENTS));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_IS_ERADICATED, INFO_ITEM_IS_ERADICATED, 
                			"Is Eradicated", 
                			nameRow.getValue(MaxNameRow.ColumnName.IS_ERADICATED));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_INFORMAL, INFO_ITEM_INFORMAL, 
                			"Informal", 
                			nameRow.getValue(MaxNameRow.ColumnName.INFORMAL));
                			
                	addProfileInfoItem(infoItems, INFO_ITEM_CONSV_CODE, INFO_ITEM_CONSV_CODE, 
                			"Conservation Code", 
                			nameRow.getValue(MaxNameRow.ColumnName.CONSV_CODE));
                	
                	addProfileInfoItem(infoItems, INFO_ITEM_COMMENT, INFO_ITEM_COMMENT, 
                			"Comments", 
                			nameRow.getValue(MaxNameRow.ColumnName.COMMENTS));
                	
                	iSpecies.setInfoItems(infoItems);
                }
                
                taxaDAO.save(iSpecies);
			}
		}
	}
	
	@Override
	public void processXrefRow(MaxXrefRow row, ITaxonConcept oldConcept,
			ITaxonConcept newConcept) {
		
		IndicatorSpecies species = taxaDAO.getIndicatorSpeciesBySourceDataID(null, MaxImporter.MAX_SOURCE, oldConcept.getName().getSourceId());
		if (species != null) {
			species.setCurrent(false);
		}
	}
	
    private void addProfileInfoItem(List<SpeciesProfile> infoItems,
            String type, String header, String description,
            String content) {
        SpeciesProfile sp = new SpeciesProfile();
        sp.setType(type);
        sp.setHeader(MaxImporter.MAX_SOURCE + "_" + header);
        sp.setDescription(description);
        sp.setContent(!StringUtils.isEmpty(content) ? content.trim() : "");
        // prevent duplicates
        spDAO.save(sp);
        infoItems.add(sp);
    }
	
	private IndicatorSpecies getIndicatorSpecies(String sourceId) {
		if (iSpeciesCache.containsKey(sourceId)) {
			return iSpeciesCache.get(sourceId);
		}
		IndicatorSpecies result = taxaDAO.getIndicatorSpeciesBySourceDataID(null, MaxImporter.MAX_SOURCE, sourceId);
		if (result != null) {
			iSpeciesCache.put(sourceId, result);
		}
		return result;
	}
}
