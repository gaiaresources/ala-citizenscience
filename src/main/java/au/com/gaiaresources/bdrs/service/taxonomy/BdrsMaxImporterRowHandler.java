package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.service.taxonomy.max.*;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.*;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.sql.SQLException;
import java.util.*;

public class BdrsMaxImporterRowHandler implements MaxImporterRowHandler {
    /**
     * Taxon group name
     */
    public static final String MAX_GROUP_NAME = "MAX";

    static final SpeciesProfileBuilder[] PROFILE_BUILDER = new SpeciesProfileBuilder[] {
            new SpeciesProfileBuilder(MaxNameRow.ColumnName.AUTHOR, "Author"),
            new SpeciesProfileBuilder(MaxNameRow.ColumnName.EDITOR, "Editor"),
            new SpeciesProfileBuilder(MaxNameRow.ColumnName.REFERENCE,
                    "Literature Reference"),
            new SpeciesProfileBuilder(MaxNameRow.ColumnName.COMMENTS,
                    "Comments"),
            new SpeciesProfileTaxonNameYesNoBuilder(
                    MaxNameRow.ColumnName.IS_CURRENT,
                    "Is currently accepted in Western Australia"),

            new SpeciesProfileTaxonNameNaturalisedBuilder(
                    "Is naturalised into the environment"),
            new SpeciesProfileTaxonNameNaturalisedStatusBuilder(
                    "Naturalised Status"),
            new SpeciesProfileTaxonNameYesNoBuilder(
                    MaxNameRow.ColumnName.NATURALISED_CERTAINTY,
                    "Naturalised Certainty"),
            new SpeciesProfileTaxonNameYesNoBuilder(
                    MaxNameRow.ColumnName.IS_ERADICATED, "Is Eradicated"),
            new SpeciesProfileBuilder(
                    MaxNameRow.ColumnName.NATURALISED_COMMENTS,
                    "Naturalised Comments"),

            new SpeciesProfileTaxonNameInformalBuilder(
                    "Informal (Manuscript, Phrase or Published Name)"),
            new SpeciesProfileTaxonNameConsvCodeBuilder(
                    "Conservation Code (assigned by Wildlife Branch)"),

            new SpeciesProfileTaxonNameDateBuilder(
                    MaxNameRow.ColumnName.ADDED_ON,
                    "Added On (The date this taxon was first saved in the Max database)"),
            new SpeciesProfileTaxonNameDateBuilder(
                    MaxNameRow.ColumnName.UPDATED_ON,
                    "Updated On (The date this taxon was last updated in the Max database)"),
            new SpeciesProfileBuilder(MaxNameRow.ColumnName.FAMILY_CODE, "Family Code"),
            };
    
    static final FamilyProfileBuilder[] FAMILY_PROFILE_BUILDER = new FamilyProfileBuilder[] {
        new FamilyProfileBuilder(MaxFamilyRow.ColumnName.FAMILY_CODE,
                "Family Code"),
    };

    private SessionFactory sessionFactory;
    private Logger log = Logger.getLogger(getClass());
    private Map<String, IndicatorSpecies> sourceIdToIndicatorSpeciesMap = new HashMap<String, IndicatorSpecies>(
            MaxImporter.REPORT_MOD);
    private TaxonGroup group = null;
    private Session operatingSession = null;

    private TaxaDAO taxaDAO;
    private SpeciesProfileDAO spDAO;

    private ITemporalContext temporalContext;
    private ITaxonLibSession taxonLibSession;

    /**
     * Create a new row handler.
     * 
     * @param taxonLibSession
     *            TaxonLibSession.
     * @param now
     *            The date we are running the import.
     * @param taxaDAO
     *            TaxaDAO.
     * @param spDAO
     *            SpeciesProfileDAO.
     */
    public BdrsMaxImporterRowHandler(ITaxonLibSession taxonLibSession,
            Date now, SessionFactory sessionFactory, TaxaDAO taxaDAO,
            SpeciesProfileDAO spDAO) {
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
            throw new IllegalArgumentException(
                    "SpeciesProfileDAO cannot be null");
        }
        this.taxonLibSession = taxonLibSession;
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
        saveIndicatorSpecies(family, null, row);
    }

    @Override
    public void processGeneraRow(MaxGeneraRow row, ITaxonConcept genus) {
        saveIndicatorSpecies(genus, null);
    }

    @Override
    public void precacheTaxa(List<String> sourceIdList) {
        List<IndicatorSpecies> speciesList = taxaDAO.getIndicatorSpeciesBySourceDataID(this.operatingSession, MaxImporter.MAX_SOURCE, sourceIdList);

        for (IndicatorSpecies s : speciesList) {
            sourceIdToIndicatorSpeciesMap.put(s.getSourceId(), s);
        }
    }

    private IndicatorSpecies getIndicatorSpeciesBySourceDataID(
            String sourceDataId) {
        IndicatorSpecies taxa = sourceIdToIndicatorSpeciesMap.get(sourceDataId);
        if (taxa == null) {
            taxa = taxaDAO.getIndicatorSpeciesBySourceDataID(this.operatingSession, MaxImporter.MAX_SOURCE, sourceDataId);
            sourceIdToIndicatorSpeciesMap.put(sourceDataId, taxa);
        }
        return taxa;
    }

    @Override
    public void begin() throws SQLException {
        if (this.operatingSession != null) {
            this.save();
        }

        this.operatingSession = this.sessionFactory.openSession();
        this.operatingSession.beginTransaction();
    }

    @Override
    public void save() throws SQLException {
        if (this.operatingSession == null) {
            log.warn("Attempt to save before a session has been created.");
            return;
        }

        Transaction tx = this.operatingSession.getTransaction();
        if (tx.isActive()) {
            tx.commit();
        }
        taxonLibSession.commit();
        taxonLibSession.clearCache();

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
        if (this.operatingSession == null) {
            throw new IllegalArgumentException("Operation session not started");
        }

        if (group == null) {
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
    
    private void saveIndicatorSpecies(ITaxonConcept concept, MaxNameRow nameRow) {
        saveIndicatorSpecies(concept, nameRow, null);
    }

    /**
     * Attempt to save the indicator species. If the species already exists we
     * will not create it again.
     * 
     * @param concept
     *            ITaxonConcept to create IndicatorSpecies from.
     * @param nameRow
     *            MaxNameRow Raw row from CSV file.
     */
    private void saveIndicatorSpecies(ITaxonConcept concept, MaxNameRow nameRow, MaxFamilyRow familyRow) {
        if (concept != null) {
            ITaxonName tn = concept.getName();
            IndicatorSpecies iSpecies = getIndicatorSpeciesBySourceDataID(getSourceId(tn));
            
            if (iSpecies == null) {
                iSpecies = new IndicatorSpecies();
                iSpecies.setTaxonGroup(getTaxonGroup());
                iSpecies.setSource(MaxImporter.MAX_SOURCE);
            }
            
            iSpecies.setScientificName(tn.getDisplayName());

            if (nameRow != null) {
                ITaxonName commonName = temporalContext.getFirstCommonName(concept);
                if (commonName != null) {
                    // there should only ever be 1 commonName in the Max data set so...
                    iSpecies.setCommonName(commonName.getName());
                } else {
                    iSpecies.setCommonName("");
                }
            } else {
                iSpecies.setCommonName("");
            }
            iSpecies.setAuthor(tn.getAuthor());
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

            // do the funky cast....
            // There are 2 duplicate TaxonRank enums. One in the BDRS namespace and one in the TaxonLib namespace...
            TaxonRank rank = TaxonRank.valueOf(tn.getRank().toString());
            if (rank == null) {
                throw new IllegalStateException(
                        "Something has gone wrong with our funky casting");
            }
            iSpecies.setTaxonRank(rank);
            
            List<SpeciesProfile> speciesProfileToDelete = new ArrayList<SpeciesProfile>();
            // save species profile items
            if (nameRow != null) {
                speciesProfileToDelete.addAll(iSpecies.getInfoItems());
                List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();

                for (SpeciesProfileBuilder builder : PROFILE_BUILDER) {
                    SpeciesProfile sp = builder.createProfile(nameRow);
                    if (sp != null) {
                        sp = spDAO.save(this.operatingSession, sp);
                        infoItems.add(sp);
                    }
                }
                iSpecies.getInfoItems().clear();
                iSpecies.setInfoItems(infoItems);
            } else if (familyRow != null) {
                speciesProfileToDelete.addAll(iSpecies.getInfoItems());
                List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();

                for (FamilyProfileBuilder builder : FAMILY_PROFILE_BUILDER) {
                    SpeciesProfile sp = builder.createProfile(familyRow);
                    if (sp != null) {
                        sp = spDAO.save(this.operatingSession, sp);
                        infoItems.add(sp);
                    }
                }
                iSpecies.getInfoItems().clear();
                iSpecies.setInfoItems(infoItems);
            }

            taxaDAO.save(this.operatingSession, iSpecies);
            
            for (SpeciesProfile sp : speciesProfileToDelete) {
                spDAO.delete(this.operatingSession, sp);
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
     * 
     * @param tn
     *            ITaxonName.
     * @return Source ID
     */
    private String getSourceId(ITaxonName tn) {
        return tn.getId().toString();
    }
}
