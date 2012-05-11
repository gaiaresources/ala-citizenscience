package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.*;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.*;

public class BdrsMaxImporterRowHandler implements MaxImporterRowHandler {
    /**
	 * Taxon group name
	 */
	public static final String MAX_GROUP_NAME = "MAX";
	
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_IS_CURRENT = "IS_CURRENT";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_NATURALISED = "NATURALISED";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_NATURALISED_STATUS = "NATURALISED_STATUS";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_COMMENT = "COMMENT";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_NATURALISED_CERTAINTY = "NATURALISED_CERTAINTY";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_IS_ERADICATED = "IS_ERADICATED";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_NATURALISED_COMMENTS = "NATURALISED_COMMENTS";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_INFORMAL = "INFORMAL";
	/**
	 * Species profile item
	 */
	public static final String INFO_ITEM_CONSV_CODE = "CONSV_CODE";

    private SessionFactory sessionFactory;
    private Logger log = Logger.getLogger(getClass());
    private Map<String, IndicatorSpecies> sourceIdToIndicatorSpeciesMap =
        new HashMap<String, IndicatorSpecies>(MaxImporter.REPORT_MOD);
    private TaxonGroup group = null;
    private Session operatingSession = null;

    private TaxaDAO taxaDAO;
    private SpeciesProfileDAO spDAO;

    private ITemporalContext temporalContext;

    /**
	 * Create a new row handler.
	 * @param taxonLibSession TaxonLibSession.
	 * @param now The date we are running the import.
	 * @param taxaDAO TaxaDAO.
	 * @param spDAO SpeciesProfileDAO.
	 */
	public BdrsMaxImporterRowHandler(ITaxonLibSession taxonLibSession, Date now,
                                     SessionFactory sessionFactory, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
		if (taxonLibSession == null) {
			throw new IllegalArgumentException("TaxonLibSession cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Date cannot be null");
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
        this.sessionFactory = sessionFactory;
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
    public void precacheTaxa(List<String> sourceIdList) {
        List<IndicatorSpecies> speciesList = taxaDAO.getIndicatorSpeciesBySourceDataID(
                this.operatingSession, MaxImporter.MAX_SOURCE, sourceIdList);

        for(IndicatorSpecies s : speciesList) {
            sourceIdToIndicatorSpeciesMap.put(s.getSourceId(), s);
        }
    }

    private IndicatorSpecies getIndicatorSpeciesBySourceDataID(String sourceDataId) {
        IndicatorSpecies taxa = sourceIdToIndicatorSpeciesMap.get(sourceDataId);
        if(taxa == null) {
            taxa = taxaDAO.getIndicatorSpeciesBySourceDataID(this.operatingSession, MaxImporter.MAX_SOURCE, sourceDataId);
            sourceIdToIndicatorSpeciesMap.put(sourceDataId, taxa);
        }
        return taxa;
    }

    @Override
    public void begin() {
        if(this.operatingSession != null) {
            this.save();
        }

        this.operatingSession = this.sessionFactory.openSession();
        this.operatingSession.beginTransaction();
    }

    @Override
    public void save() {
        if(this.operatingSession == null) {
            log.warn("Attempt to save before a session has been created.");
            return;
        }

        Transaction tx = this.operatingSession.getTransaction();
        if(tx.isActive()) {
            tx.commit();
        }

        this.operatingSession.close();
        this.operatingSession = null;
        this.group = null;
        this.sourceIdToIndicatorSpeciesMap.clear();
    }

    @Override
	public void processNameRow(MaxNameRow row, ITaxonConcept species) {
		saveIndicatorSpecies(species, row);
	}

    private TaxonGroup getTaxonGroup() {
        if(this.operatingSession == null) {
            throw new IllegalArgumentException("Operation session not started");
        }

        if(group == null) {
            group = taxaDAO.getTaxonGroup(this.operatingSession, MAX_GROUP_NAME);

            if (group == null) {
                group = new TaxonGroup();
                group.setName(MAX_GROUP_NAME);
                group.setBehaviourIncluded(false);
                group.setLastAppearanceIncluded(false);
                group.setNumberIncluded(false);
                group.setWeatherIncluded(false);
                group.setHabitatIncluded(false);
                group = taxaDAO.save(this.operatingSession, group);
            }
        }

        return group;
    }
	
	/**
	 * Attempt to save the indicator species. If the species already exists
	 * we will not create it again.
	 * 
	 * @param concept ITaxonConcept to create IndicatorSpecies from.
	 * @param nameRow MaxNameRow Raw row from CSV file.
	 */
	private void saveIndicatorSpecies(ITaxonConcept concept, MaxNameRow nameRow) {
		if (concept != null) {
			ITaxonName tn = concept.getName();
			IndicatorSpecies iSpecies = getIndicatorSpeciesBySourceDataID(getSourceId(tn));
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
                iSpecies.setTaxonGroup(getTaxonGroup());
                iSpecies.setSource(MaxImporter.MAX_SOURCE);
                iSpecies.setSourceId(getSourceId(tn));
                
                if (concept.getParent() != null) {
                	if (concept.getParent().getName() == null) {
                		log.debug("parent concept name is null");
                	}
                	IndicatorSpecies iSpeciesParent = getIndicatorSpeciesBySourceDataID(getSourceId(concept.getParent().getName()));
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

                taxaDAO.save(this.operatingSession, iSpecies);
			}
		}
	}
	
	@Override
	public void processXrefRow(MaxXrefRow row, ITaxonConcept oldConcept,
			ITaxonConcept newConcept) {
		
		if (oldConcept.getName() == null) {
			log.debug("old concept name is null");
		}
		IndicatorSpecies species = taxaDAO.getIndicatorSpeciesBySourceDataID(this.operatingSession, MaxImporter.MAX_SOURCE, getSourceId(oldConcept.getName()));
		if (species != null) {
			species.setCurrent(false);
		}
	}
	
	/**
	 * Get the source ID to assign to the matching IndicatorSpecies object.
	 * @param tn ITaxonName.
	 * @return Source ID
	 */
	private String getSourceId(ITaxonName tn) {		
		return tn.getId().toString();
	}
	
	/**
	 * Helper for creating SpeciesProfile items.
	 * @param infoItems List of info items.
	 * @param type Type of info item.
	 * @param header Header of info item.
	 * @param description Description of info item.
	 * @param content Content of info item.
	 */
    private void addProfileInfoItem(List<SpeciesProfile> infoItems,
            String type, String header, String description,
            String content) {
        SpeciesProfile sp = new SpeciesProfile();
        sp.setType(type);
        sp.setHeader(MaxImporter.MAX_SOURCE + "_" + header);
        sp.setDescription(description);
        sp.setContent(!StringUtils.isEmpty(content) ? content.trim() : "");
        // prevent duplicates
        spDAO.save(this.operatingSession, sp);
        infoItems.add(sp);
    }
}
