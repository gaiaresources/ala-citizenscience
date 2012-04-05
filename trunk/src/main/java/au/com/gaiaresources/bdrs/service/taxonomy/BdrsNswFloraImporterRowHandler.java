package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporter;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporterRowHandler;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraRow;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsNswFloraImporterRowHandler implements
		NswFloraImporterRowHandler {
	
	private Map<String, IndicatorSpecies> indicatorSpeciesCache = new HashMap<String, IndicatorSpecies>();
	private TaxaDAO taxaDAO;
	private SpeciesProfileDAO spDAO;
	private ITemporalContext temporalContext;
	
	private TaxonGroup taxonGroup;
	
	private static final String TAXON_GROUP_NAME = "NSW Flora";
	
	public static final String SPECIES_PROFILE_NSW_DISTRO = "NSW Distribution";
    public static final String SPECIES_PROFILE_DIST_OTHER = "Distributions in other States";
    public static final String SPECIES_PROFILE_NSW_TSC = "NSW Threatened Species Conservation";
    public static final String SPECIES_PROFILE_NATIVE_INTRODUCED = "Native / Introduced";
    public static final String SPECIES_PROFILE_EPBC_STATUS = "Environmental Protection and Biodiversity Conservation Status";

	public BdrsNswFloraImporterRowHandler(ITaxonLibSession taxonLibSession, Date now, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
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
		
		taxonGroup = taxaDAO.getTaxonGroup(TAXON_GROUP_NAME);
        if (taxonGroup == null) {
            // lazy create
            taxonGroup = new TaxonGroup();
            taxonGroup.setName(TAXON_GROUP_NAME);
            taxonGroup.setBehaviourIncluded(false);
            taxonGroup.setLastAppearanceIncluded(false);
            taxonGroup.setNumberIncluded(false);
            taxonGroup.setWeatherIncluded(false);
            taxonGroup.setHabitatIncluded(false);
            taxonGroup = taxaDAO.save(taxonGroup);
        }
	}
	
	@Override
	public void updateConcept(NswFloraRow row, ITaxonConcept concept) {
		String sourceId;
        String sciName;
        String commonName = ""; // defaults to blank
        IndicatorSpecies parent = null;
        
        ITaxonConcept tc = concept;
        if (tc != null) {
            ITaxonName currentTaxonName = tc.getName();
            
            if (currentTaxonName.getId() == null) {
                throw new IllegalStateException("Taxon name should be persisted");
            }
            sourceId = currentTaxonName.getId().toString();
            
            ITaxonConcept parentTc = tc.getParent();
            if (parentTc != null) {
                String parentKey = getSourceId(parentTc.getName());
                if (indicatorSpeciesCache.containsKey(parentKey)) {
                    parent = indicatorSpeciesCache.get(parentKey);
                }
            }
            
            sciName = currentTaxonName.getDisplayName();
            
            TaxonRank rank = TaxonRank.valueOf(currentTaxonName.getRank().toString());
            
            List<ITaxonName> commonNames = temporalContext.getCommonNames(concept);
            if (!commonNames.isEmpty()) {
            	// grab the first name
            	commonName = commonNames.get(0).getDisplayName();
            }
            
            IndicatorSpecies iSpecies = indicatorSpeciesCache.get(sourceId);
            if (iSpecies == null) {
                iSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(null, NswFloraImporter.SOURCE, sourceId);
            }
            
            if (iSpecies == null) {
                // create new indicator species
                iSpecies = new IndicatorSpecies();
                iSpecies.setScientificName(sciName);
                iSpecies.setCommonName(commonName);
                iSpecies.setAuthor(currentTaxonName.getAuthor());
                iSpecies.setTaxonGroup(taxonGroup);
                iSpecies.setSource(NswFloraImporter.SOURCE);
                iSpecies.setSourceId(sourceId);
                iSpecies.setParent(parent);
                iSpecies.setTaxonRank(rank);

                List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NSW_DISTRO, SPECIES_PROFILE_NSW_DISTRO, "NSW Distribution", row.getValue(NswFloraRow.ColumnName.NSW_DISTRO)); // .getNswDistro());
                addProfileInfoItem(infoItems, SPECIES_PROFILE_DIST_OTHER, SPECIES_PROFILE_DIST_OTHER, "Distribution in other States", row.getValue(NswFloraRow.ColumnName.DIST_OTHER));
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NSW_TSC, SPECIES_PROFILE_NSW_TSC, "NSW Threatened Species Conservation", row.getValue(NswFloraRow.ColumnName.NSW_TSC));
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NATIVE_INTRODUCED, SPECIES_PROFILE_NATIVE_INTRODUCED, "Native / Introduced", row.getValue(NswFloraRow.ColumnName.NATIVE_INTRODUCED));
                addProfileInfoItem(infoItems, SPECIES_PROFILE_EPBC_STATUS, SPECIES_PROFILE_EPBC_STATUS, "Environmental Protection and Biodiversity Conservation Status", row.getValue(NswFloraRow.ColumnName.EPBC_STATUS));

                iSpecies.setInfoItems(infoItems);
                
                iSpecies = taxaDAO.save(iSpecies);
            } else {
            	List<ITaxonName> commonNames2 = temporalContext.getCommonNames(concept);
                if (!commonNames2.isEmpty()) {
                	iSpecies.setCommonName(commonNames2.get(0).getDisplayName());                
                }
                iSpecies.setScientificName(currentTaxonName.getDisplayName());
                taxaDAO.save(iSpecies);
            }
            indicatorSpeciesCache.put(sourceId, iSpecies);
        }
	}
	
	private void addProfileInfoItem(List<SpeciesProfile> infoItems,
            String type, String header, String description,
            String content) {
        SpeciesProfile sp = new SpeciesProfile();
        sp.setType(type);
        sp.setHeader(NswFloraImporter.SOURCE + "_" + header);
        sp.setDescription(description);
        sp.setContent(content);
        // prevent duplicates
        spDAO.save(sp);
        infoItems.add(sp);
    }
	
	private String getSourceId(ITaxonName tn) {
		return tn.getId().toString();
	}
}
