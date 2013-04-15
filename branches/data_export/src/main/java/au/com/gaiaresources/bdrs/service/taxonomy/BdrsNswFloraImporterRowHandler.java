package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.service.taxonomy.nsw.SpeciesProfileBuilder;
import au.com.gaiaresources.bdrs.service.taxonomy.nsw.SpeciesProfileNaturalisedStatusBuilder;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporter;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporterRowHandler;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraRow;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.sql.SQLException;
import java.util.*;

public class BdrsNswFloraImporterRowHandler implements
		NswFloraImporterRowHandler {

	private static final String TAXON_GROUP_NAME = "NSW Flora";

    static final SpeciesProfileBuilder[] PROFILE_BUILDER = new SpeciesProfileBuilder[] {
            new SpeciesProfileBuilder(NswFloraRow.ColumnName.AUTHOR, "Author"),
            new SpeciesProfileNaturalisedStatusBuilder("Naturalised Status"),
            new SpeciesProfileBuilder(NswFloraRow.ColumnName.NSW_TSC, "NSW Threatened Species Conservation"),
            new SpeciesProfileBuilder(NswFloraRow.ColumnName.EPBC_STATUS, "Environmental Protection and Biodiversity Conservation (EPBC) Status"),
            new SpeciesProfileBuilder(NswFloraRow.ColumnName.NSW_DISTRO, "NSW Distribution"),
            new SpeciesProfileBuilder(NswFloraRow.ColumnName.DIST_OTHER, "Distributions in other States")
    };

    private Logger log = Logger.getLogger(getClass());

    private ITemporalContext temporalContext;
    private ITaxonLibSession taxonLibSession;

    private TaxaDAO taxaDAO;
    private SpeciesProfileDAO spDAO;
    private SessionFactory sessionFactory;
    private Session operatingSession = null;

    private TaxonGroup taxonGroup;
    private Map<String, IndicatorSpecies> indicatorSpeciesCache = new HashMap<String, IndicatorSpecies>();

	public BdrsNswFloraImporterRowHandler(ITaxonLibSession taxonLibSession, Date now,  SessionFactory sessionFactory, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
		if (taxonLibSession == null) {
			throw new IllegalArgumentException("TaxonLibSession cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
        if(sessionFactory == null) {
            throw new IllegalArgumentException("SessionFactory cannot be null");
        }
		if (taxaDAO == null) {
			throw new IllegalArgumentException("TaxaDAO cannot be null");
		}
		if (spDAO == null) {
			throw new IllegalArgumentException("SpeciesProfileDAO cannot be null");
		}
        this.taxonLibSession = taxonLibSession;
		this.temporalContext = taxonLibSession.getTemporalContext(now);
        this.sessionFactory = sessionFactory;
		this.taxaDAO = taxaDAO;
		this.spDAO = spDAO;
	}

    private String getSourceId(ITaxonName tn) {
        return tn.getId().toString();
    }

    @Override
    public void precacheTaxa(ITaxonConcept family, ITaxonConcept genus, ITaxonConcept species, ITaxonConcept infraspecies) {

        List<String> sourceIdList = new ArrayList<String>(4);
        if(family != null) {
            sourceIdList.add(getSourceId(family.getName()));
        }
        if(genus != null) {
            sourceIdList.add(getSourceId(genus.getName()));
        }
        if(species != null) {
            sourceIdList.add(getSourceId(species.getName()));
        }
        if(infraspecies != null) {
            sourceIdList.add(getSourceId(infraspecies.getName()));
        }

        List<IndicatorSpecies> speciesList = taxaDAO.getIndicatorSpeciesBySourceDataID(
                this.operatingSession, NswFloraImporter.SOURCE, sourceIdList);

        for(IndicatorSpecies s : speciesList) {
            indicatorSpeciesCache.put(s.getSourceId(), s);
        }
    }
	
	@Override
	public void updateConcept(NswFloraRow row, ITaxonConcept concept) {
        if(concept == null) {
            return;
        }

		String sourceId;
        String sciName;
        String commonName = ""; // defaults to blank
        IndicatorSpecies parent = null;

        ITaxonName currentTaxonName = concept.getName();
        if (currentTaxonName.getId() == null) {
            throw new IllegalStateException("Taxon name should be persisted");
        }
        sourceId = currentTaxonName.getId().toString();

        ITaxonConcept parentTc = concept.getParent();
        if (parentTc != null) {
            String parentKey = getSourceId(parentTc.getName());
            parent = indicatorSpeciesCache.get(parentKey);
        }

        sciName = currentTaxonName.getDisplayName();
        TaxonRank rank = TaxonRank.valueOf(currentTaxonName.getRank().toString());

        ITaxonName taxonLibCommonName = temporalContext.getFirstCommonName(concept);
        if (taxonLibCommonName != null) {
            commonName = taxonLibCommonName.getDisplayName();
        }

        IndicatorSpecies iSpecies = indicatorSpeciesCache.get(sourceId);
        if (iSpecies == null) {
            // create new indicator species
            iSpecies = new IndicatorSpecies();
            iSpecies.setScientificName(sciName);
            iSpecies.setCommonName(commonName);
            iSpecies.setAuthor(currentTaxonName.getAuthor());
            iSpecies.setTaxonGroup(getTaxonGroup());
            iSpecies.setSource(NswFloraImporter.SOURCE);
            iSpecies.setSourceId(sourceId);
            iSpecies.setParent(parent);
            iSpecies.setTaxonRank(rank);

            List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();
            for(SpeciesProfileBuilder builder : PROFILE_BUILDER) {
                SpeciesProfile sp = builder.createProfile(row);
                if(sp != null) {
                    sp = spDAO.save(this.operatingSession, sp);
                    infoItems.add(sp);
                }
            }

            iSpecies.setInfoItems(infoItems);
            iSpecies = taxaDAO.save(this.operatingSession, iSpecies);
            indicatorSpeciesCache.put(sourceId, iSpecies);
        } else {
            ITaxonName taxonLibCommonName2 = temporalContext.getFirstCommonName(concept);
            if (taxonLibCommonName2 != null) {
                iSpecies.setCommonName(taxonLibCommonName2.getDisplayName());
            }
            iSpecies.setScientificName(currentTaxonName.getDisplayName());
            taxaDAO.save(this.operatingSession, iSpecies);
        }
	}

    @Override
    public void begin() throws SQLException {
        if(this.operatingSession != null) {
            this.save();
        }

        this.operatingSession = this.sessionFactory.openSession();
        this.operatingSession.beginTransaction();
    }

    @Override
    public void save() throws SQLException {
        if(this.operatingSession == null) {
            log.warn("Attempt to save before a session has been created.");
            return;
        }

        Transaction tx = this.operatingSession.getTransaction();
        if(tx.isActive()) {
            tx.commit();
        }
        taxonLibSession.commit();
        taxonLibSession.clearCache();

        this.operatingSession.close();
        this.operatingSession = null;
        this.taxonGroup = null;
        this.indicatorSpeciesCache.clear();
    }

    private TaxonGroup getTaxonGroup() {
        if(this.operatingSession == null) {
            throw new IllegalArgumentException("Operation session not started");
        }

        if(taxonGroup == null) {
            taxonGroup = taxaDAO.getTaxonGroup(this.operatingSession, TAXON_GROUP_NAME);
            if (taxonGroup == null) {
                // lazy create
                taxonGroup = new TaxonGroup();
                taxonGroup.setName(TAXON_GROUP_NAME);
                taxonGroup.setBehaviourIncluded(false);
                taxonGroup.setLastAppearanceIncluded(false);
                taxonGroup.setNumberIncluded(false);
                taxonGroup.setWeatherIncluded(false);
                taxonGroup.setHabitatIncluded(false);
                taxonGroup = taxaDAO.save(this.operatingSession, taxonGroup);
            }
        }

        return taxonGroup;
    }
}
