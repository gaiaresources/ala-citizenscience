package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.taxonlib.Citation;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.IUntrackedContext;
import au.com.gaiaresources.taxonlib.TaxonLibException;
import au.com.gaiaresources.taxonlib.TaxonLibSession;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import au.com.gaiaresources.taxonlib.model.TaxonNameType;
import au.com.gaiaresources.taxonlib.model.TaxonRank;

import com.csvreader.CsvReader;

import edu.emory.mathcs.backport.java.util.Arrays;

public class NswFloraImporter {
    
    public static final String SPECIES_PROFILE_NSW_DISTRO = "NSW Distribution";
    public static final String SPECIES_PROFILE_DIST_OTHER = "Distributions in other States";
    public static final String SPECIES_PROFILE_NSW_TSC = "NSW Threatened Species Conservation";
    public static final String SPECIES_PROFILE_NATIVE_INTRODUCED = "Native / Introduced";
    public static final String SPECIES_PROFILE_EPBC_STATUS = "Environmental Protection and Biodiversity Conservation Status";

    private Logger log = Logger.getLogger(getClass());

    private ITemporalContext temporalContext = null;
    private IUntrackedContext untrackedContext = null;
    
    private TaxaDAO taxaDAO;
    private SpeciesProfileDAO spDAO;

    public NswFloraImporter(TaxonLibSession session, Date now, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO)
            throws TaxonLibException {
        if (session == null) {
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
        temporalContext = session.getTemporalContext(now);
        untrackedContext = session.getUntrackedContext();
        
        this.taxaDAO = taxaDAO;
        this.spDAO = spDAO;
    }

    public static final String EXPECTED_CHARSET = "UTF-8";

    private static final String RECORD_NO = "Record_No";
    private static final String FAMILY = "Family";
    private static final String GENUS = "Genus";
    private static final String SPECIES = "Species";
    private static final String INFRASPECIES = "Infraspecies";
    private static final String DISPLAY_NAME = "Display Name";
    private static final String COMMON_NAME = "Common Name";
    private static final String AUTHOR = "Author";
    private static final String NATIVE_INTRODUCED = "Native_Introduced";
    private static final String NSW_TSC = "NSW_TSC";
    private static final String EPBC_STATUS = "EPBC_Status";
    private static final String NSW_DISTRO = "NSW_Distribution";
    private static final String DIST_OTHER = "Dist_Other";
    private static final String SYNONYM = "Synonym";

    private Map<String, IndicatorSpecies> indicatorSpeciesCache = new HashMap<String, IndicatorSpecies>();
    private Map<String, ITaxonConcept> conceptCache = new HashMap<String, ITaxonConcept>();

    // if there is a parent that doesn't exist yet and we need to parse more of the file, store
    // the rows here...
    private Map<String, NswFloraRow> storedRows = new HashMap<String, NswFloraRow>();

    private Set<TaxonNameType> sciNameSet = null;
    private Set<String> sourceSet = null;
    private ITaxonConcept root = null;
    private Citation emptyCitation = new Citation("", "", "");

    public static final String NSW_FLORA_SOURCE = "NSW_FLORA";
    public static final String ROOT_NODE_SOURCE_ID = "NSW_FLORA_ROOT_NODE";
    public static final String NSW_FLORA_FAMILY_PREFIX = "FAMILY_";
    public static final String NSW_FLORA_GENUS_PREFIX = "GENUS_";
    public static final String NSW_FLORA_SPECIES_PREFIX = "SPECIES_";
    public static final String NSW_FLORA_INFRA_PREFIX = "INFRA_";
    
    public static final String NSW_PREFIX = "NSW";
    
    private static final String TAXON_GROUP_NAME = "NSW Flora";
    
    private TaxonGroup taxonGroup;

    private int cacheMiss = 0;

    public void runImport(InputStream csv) throws Exception {

        cacheHits = 0;
        cacheMiss = 0;

        CsvReader reader = new CsvReader(csv, Charset.forName(EXPECTED_CHARSET));
        reader.readHeaders();

        List<String> expectedColumns = new ArrayList<String>();

        expectedColumns.add(RECORD_NO);
        expectedColumns.add(FAMILY);
        expectedColumns.add(GENUS);
        expectedColumns.add(SPECIES);
        expectedColumns.add(INFRASPECIES);
        expectedColumns.add(DISPLAY_NAME);
        expectedColumns.add(COMMON_NAME);
        expectedColumns.add(AUTHOR);
        expectedColumns.add(NATIVE_INTRODUCED);
        expectedColumns.add(NSW_TSC);
        expectedColumns.add(EPBC_STATUS);
        expectedColumns.add(NSW_DISTRO);
        expectedColumns.add(DIST_OTHER);
        expectedColumns.add(SYNONYM);

        verifyHeaders(reader, expectedColumns);

        sciNameSet = new HashSet<TaxonNameType>();
        sciNameSet.add(TaxonNameType.SCIENTIFIC);
        sourceSet = new HashSet<String>();
        sourceSet.add(NSW_FLORA_SOURCE);

        root = getRootNode();
        if (root == null) {
            throw new Exception("root cannot be null");
        }

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

        log.debug("START NSW FLORA IMPORT");
        int rowCount = 0;
        while (reader.readRecord()) {
            NswFloraRow r = new NswFloraRow();
            r.setRecordNo(reader.get(RECORD_NO));
            r.setFamily(reader.get(FAMILY));
            r.setGenus(reader.get(GENUS));
            r.setSpecies(reader.get(SPECIES));
            r.setInfraspecies(reader.get(INFRASPECIES));
            r.setDisplayName(reader.get(DISPLAY_NAME));
            r.setCommonName(reader.get(COMMON_NAME));
            r.setAuthor(reader.get(AUTHOR));
            r.setNativeIntroduced(reader.get(NATIVE_INTRODUCED));
            r.setTsc(reader.get(NSW_TSC));
            r.setEpbc(reader.get(EPBC_STATUS));
            r.setNswDistro(reader.get(NSW_DISTRO));
            r.setDistOther(reader.get(DIST_OTHER));
            r.setSynonym(reader.get(SYNONYM));

            // I think 'Y' means that the name is current.
            if (r.getSynonym().trim().equals("Y")) {
                processRow(r);
            } else {
                storedRows.put(r.getRecordNo(), r);
            }

            ++rowCount;
            if (rowCount % 500 == 0) {
                log.info("row count : " + rowCount);
                log.info("stored rows size : " + storedRows.size());
                log.info("cache hits : " + this.cacheHits);
                log.info("cache miss : " + this.cacheMiss);
            }
        }

        int storedCount = storedRows.size();
        while (!storedRows.isEmpty()) {

            Set<Entry<String, NswFloraRow>> entries = storedRows.entrySet();
            storedRows = new HashMap<String, NswFloraRow>();
            for (Entry<String, NswFloraRow> entry : entries) {
                NswFloraRow r = entry.getValue();
 
                ITaxonConcept synonymTc = temporalContext.selectConceptByNameSourceId(NSW_FLORA_SOURCE, r.getSynonym().trim());

                Citation cit = new Citation(r.getAuthor(), "", "");

                if (synonymTc != null) {
                    // the synonym that this row points to is not null...
                    
                    ITaxonConcept rowTc = temporalContext.selectConceptByNameSourceId(NSW_FLORA_SOURCE, r.getRecordNo());
                    if (rowTc == null) {
                        log.debug("creating new concept for : " + r.getRecordNo() + ", syn : " + r.getSynonym());
                        processRow(r);
                        rowTc = temporalContext.selectConceptByNameSourceId(NSW_FLORA_SOURCE, r.getRecordNo());
                        if (rowTc == null) {
                            throw new IllegalStateException("wtf, we just made it");
                        }
                    }
                    
                    // rowTc has been marked as a synonym so....
                    log.debug("syn tc id : " + synonymTc.getId() + ", row tc id :" + rowTc.getId());
                    log.debug("redefine " + r.getRecordNo() + " to " + r.getSynonym());
                    
                    // only redefine if the taxonconcept is not already marked as old...
                    // It may already have been redefined in a previous upload.
                    if (rowTc.getEndDate() == null) {
                        this.redefineConcept(rowTc, synonymTc);    
                    }
                } else {
                    storedRows.put(entry.getKey(), r);
                }
            }

            if (storedCount == storedRows.size()) {
                // no more items could be removed from the stored rows..
                break;
            } else {
                storedCount = storedRows.size();
            }
        }
        
        log.debug("stored rows before load concepts with no valid synonyms : " + storedRows.size());

        // Add all concepts that have no valid synonyms left over...
        for (Entry<String, NswFloraRow> entry : storedRows.entrySet()) {
            NswFloraRow r = entry.getValue();
            processRow(r);
        }
        
        // All the concepts left over should be existing concepts with synonyms.
        log.debug("stored rows left over : " + storedRows.size());

        // and we have now dealt with all of the rows...
    }
    
    private void addProfileInfoItem(List<SpeciesProfile> infoItems,
            String type, String header, String description,
            String content) {
        SpeciesProfile sp = new SpeciesProfile();
        sp.setType(type);
        sp.setHeader(NSW_FLORA_SOURCE + "_" + header);
        sp.setDescription(description);
        sp.setContent(content);
        // prevent duplicates
        spDAO.save(sp);
        infoItems.add(sp);
    }
    
    private String joinSourceId(List<String> strList) {
        return org.apache.commons.lang.StringUtils.join(strList.toArray(), "_");
    }
    
    private String getConceptSourceId(NswFloraRow r, NswFloraRowType rType) {
        List<String> builder = new ArrayList<String>();
        builder.add(NSW_PREFIX);
        if (StringUtils.hasLength(r.getFamily())) {
            builder.add(r.getFamily().trim());
        }
        
        if (rType == NswFloraRowType.FAMILY) {
            return joinSourceId(builder);
        }
        
        if (StringUtils.hasLength(r.getGenus())) {
            builder.add(r.getGenus().trim());
        }
        
        if (rType == NswFloraRowType.GENUS) {
            return joinSourceId(builder);
        }
        
        if (StringUtils.hasLength(r.getSpecies())) {
            builder.add(r.getSpecies().trim());
        }
        
        if (rType == NswFloraRowType.SPECIES) {
            return joinSourceId(builder);
        }
        
        if (StringUtils.hasLength(r.getInfraspecies())) {
            builder.add(r.getInfraspecies().trim());
        }
        return joinSourceId(builder);
    }
    
    private ITaxonConcept getConcept(NswFloraRow r, NswFloraRowType rType) {
        String sourceId = getConceptSourceId(r, rType);
        if (conceptCache.containsKey(sourceId)) {
            return conceptCache.get(sourceId);
        }
        ITaxonConcept result = this.temporalContext.selectConcept(NSW_FLORA_SOURCE, sourceId);
        if (result != null) {
            conceptCache.put(sourceId, result);
        }
        return result;
    }
    
    private ITaxonConcept refreshCachedConcept(NswFloraRow r, NswFloraRowType rType) {
        String sourceId = getConceptSourceId(r, rType);
        ITaxonConcept result = this.temporalContext.selectConcept(NSW_FLORA_SOURCE, sourceId);
        if (result != null) {
            conceptCache.put(sourceId, result);
        } else {
            throw new IllegalStateException("we can't refresh a concept that doesn't exist");
        }
        return result;
    }
    
    private void processRow(NswFloraRow r) throws Exception {
        if (r == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }

        // update taxon lib...
        this.getConceptFromRow(r, NswFloraRowType.FAMILY);
        this.getConceptFromRow(r, NswFloraRowType.GENUS);
        this.getConceptFromRow(r, NswFloraRowType.SPECIES);
        this.getConceptFromRow(r, NswFloraRowType.INFRA);
        
        // update indicator species facade...
        createIndicatorSpecies(r, NswFloraRowType.FAMILY);
        createIndicatorSpecies(r, NswFloraRowType.GENUS);
        createIndicatorSpecies(r, NswFloraRowType.SPECIES);
        createIndicatorSpecies(r, NswFloraRowType.INFRA);
    }

    private int cacheHits = 0;
    
    private ITaxonConcept getConceptFromRow(NswFloraRow r, NswFloraRowType rType) {
        String sciName;
        ITaxonConcept parent;
        TaxonRank taxonRank;
        switch (rType) {
        case FAMILY:
            sciName = r.getFamily();
            parent = root;
            taxonRank = TaxonRank.FAMILY;
            break;
        case GENUS:
            sciName = r.getGenus();
            parent = this.getConcept(r, NswFloraRowType.FAMILY);
            taxonRank = TaxonRank.GENUS;
            break;
        case SPECIES:
            sciName = r.getSpecies();
            parent = getConcept(r, NswFloraRowType.GENUS);
            taxonRank = TaxonRank.SPECIES;
            break;
        case INFRA:
            sciName = r.getInfraspecies();
            parent = getConcept(r, NswFloraRowType.SPECIES);
            taxonRank = TaxonRank.SUBSPECIES;
            break;
            default:
                throw new IllegalStateException("case not handled : " + rType);
        }
        
        if (StringUtils.hasLength(sciName)) {
            if (parent == null) {
                throw new IllegalStateException("Parent cannot be null, record number : " + r.getRecordNo() + ", rank : " + rType);
            }
            ITaxonConcept result = this.getConcept(r, rType);
            if (result == null) {
                String conceptSourceId = this.getConceptSourceId(r, rType);
                String nameSourceId;
                String displayName;
                Citation citation;
                if (r.getRowType() == rType) {
                    nameSourceId = r.getRecordNo().trim();
                    displayName = r.getDisplayName().trim();
                    citation = new Citation(r.getAuthor(), "", "");
                } else {
                    nameSourceId = conceptSourceId;
                    displayName = sciName;
                    citation = emptyCitation;
                }
                result = temporalContext.createConcept(NSW_FLORA_SOURCE, conceptSourceId, nameSourceId, sciName, displayName, taxonRank, citation, parent);
                
                if (r.getRowType() == rType) {
                    addCommonName(r, result);
                }
            } else {
                // used when the parent has been previously constructed and a partial name has been assigned to the display name...
                if (r.getRowType() == rType) {
                    untrackedContext.updateTaxonName(result.getName(), r.getDisplayName(), r.getRecordNo());
                    addCommonName(r, result);
                    // reload updated tc
                    refreshCachedConcept(r, rType);
                }
            }
            return result;
        }
        return null;
    }
    
    private IndicatorSpecies createIndicatorSpecies(NswFloraRow r, NswFloraRowType rType) {
        
        String sourceId;
        String sciName;
        String commonName = ""; // defaults to blank
        IndicatorSpecies parent = null;
        au.com.gaiaresources.bdrs.model.taxa.TaxonRank taxonRank;
        
        ITaxonConcept tc = this.getConcept(r, rType);
        if (tc != null) {
            ITaxonName currentTaxonName = tc.getName();
            
            if (currentTaxonName.getId() == null) {
                throw new IllegalStateException("Taxon name should be persisted");
            }
            sourceId = currentTaxonName.getId().toString();
            
            ITaxonConcept parentTc = tc.getParent();
            if (parentTc != null) {
                String parentKey = parentTc.getName().getId().toString();
                if (indicatorSpeciesCache.containsKey(parentKey)) {
                    parent = indicatorSpeciesCache.get(parentKey);
                }
            }
            
            sciName = currentTaxonName.getDisplayName();
            
            switch (rType) {
            case FAMILY:
                taxonRank = au.com.gaiaresources.bdrs.model.taxa.TaxonRank.FAMILY;
                break;
            case GENUS:
                taxonRank = au.com.gaiaresources.bdrs.model.taxa.TaxonRank.GENUS;
                break;
            case SPECIES:
                taxonRank = au.com.gaiaresources.bdrs.model.taxa.TaxonRank.SPECIES;
                break;
            case INFRA:
                taxonRank = au.com.gaiaresources.bdrs.model.taxa.TaxonRank.INFRASPECIES;
                break;
                default:
                    throw new IllegalStateException("case not handled : " + rType);
            }
            
            if (r.getRowType() == rType) {
                commonName = r.getCommonName();
            }
            
            IndicatorSpecies iSpecies = indicatorSpeciesCache.get(sourceId);
            if (iSpecies == null) {
                iSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(null, NSW_FLORA_SOURCE, sourceId);
            }
            
            if (iSpecies == null) {
                // create new indicator species
                iSpecies = new IndicatorSpecies();
                iSpecies.setScientificName(sciName);
                iSpecies.setCommonName(commonName);
                iSpecies.setAuthor(r.getAuthor());
                iSpecies.setTaxonGroup(taxonGroup);
                iSpecies.setSource(NSW_FLORA_SOURCE);
                iSpecies.setSourceId(sourceId);
                iSpecies.setParent(parent);
                iSpecies.setTaxonRank(taxonRank);

                List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NSW_DISTRO, SPECIES_PROFILE_NSW_DISTRO, "NSW Distribution", r.getNswDistro());
                addProfileInfoItem(infoItems, SPECIES_PROFILE_DIST_OTHER, SPECIES_PROFILE_DIST_OTHER, "Distribution in other States", r.getDistOther());
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NSW_TSC, SPECIES_PROFILE_NSW_TSC, "NSW Threatened Species Conservation", r.getTsc());
                addProfileInfoItem(infoItems, SPECIES_PROFILE_NATIVE_INTRODUCED, SPECIES_PROFILE_NATIVE_INTRODUCED, "Native / Introduced", r.getNativeIntroduced());
                addProfileInfoItem(infoItems, SPECIES_PROFILE_EPBC_STATUS, SPECIES_PROFILE_EPBC_STATUS, "Environmental Protection and Biodiversity Conservation Status", r.getEpbc());

                iSpecies.setInfoItems(infoItems);
                
                iSpecies = taxaDAO.save(iSpecies);
            } else {
                if (r.getRowType() == rType) {
                    iSpecies.setCommonName(r.getCommonName());
                    iSpecies.setScientificName(currentTaxonName.getDisplayName());
                    taxaDAO.save(iSpecies);
                }
            }
            indicatorSpeciesCache.put(sourceId, iSpecies);
            return iSpecies;
        }
        return null;
    }
    
    private ITaxonName addCommonName(NswFloraRow r, ITaxonConcept tc) {
        if (StringUtils.hasLength(r.getCommonName())) {
            String commonName = r.getCommonName().trim();
            if (StringUtils.hasLength(commonName)) {
                List<ITaxonName> existingNames = temporalContext.getCommonNames(tc);
                // NSW flora can only have one common name per row - so, if the name
                // already exists in the db it can't possibly be changed.
                if (existingNames.isEmpty()) {
                    return temporalContext.addCommonName(tc, commonName);
                }
            }
        }
        return null;
    }

    private ITaxonConcept getRootNode() {
        ITaxonConcept root = temporalContext.selectConcept(NSW_FLORA_SOURCE, ROOT_NODE_SOURCE_ID);
        if (root == null) {
            Citation citation = new Citation("", "", "");
            root = temporalContext.createRootConcept(NSW_FLORA_SOURCE, ROOT_NODE_SOURCE_ID, ROOT_NODE_SOURCE_ID, null, citation);
        }
        return root;
    }

    private void verifyHeaders(CsvReader reader, List<String> expectedColumns)
            throws Exception {
        List<String> headers = Arrays.asList(reader.getHeaders());
        for (String str : expectedColumns) {
            if (!headers.contains(str)) {
                throw new Exception("Could not find expected header : " + str);
            }
        }
    }
    
    private void redefineConcept(ITaxonConcept oldTc, ITaxonConcept newTc) {
        Set<ITaxonConcept> srcSet = new HashSet<ITaxonConcept>();
        srcSet.add(oldTc);
        temporalContext.lump(srcSet, newTc);
    }

    private enum NswFloraRowType {
        FAMILY, GENUS, SPECIES, INFRA
    }

    private class NswFloraRow {
        private String recordNo;
        private String family;
        private String genus;
        private String species;
        private String infraspecies;
        private String displayName;
        private String commonName;
        private String author;
        private String nativeIntroduced;
        private String tsc;
        private String epbc;
        private String nswDistro;
        private String distOther;
        private String synonym;

        public String getRecordNo() {
            return recordNo;
        }

        public void setRecordNo(String recordNo) {
            this.recordNo = recordNo;
        }

        public String getFamily() {
            return family;
        }

        public void setFamily(String family) {
            this.family = family;
        }

        public String getGenus() {
            return genus;
        }

        public void setGenus(String genus) {
            this.genus = genus;
        }

        public String getInfraspecies() {
            return infraspecies;
        }

        public void setInfraspecies(String infraspecies) {
            this.infraspecies = infraspecies;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCommonName() {
            return commonName;
        }

        public void setCommonName(String commonName) {
            this.commonName = commonName;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getNativeIntroduced() {
            return nativeIntroduced;
        }

        public void setNativeIntroduced(String nativeIntroduced) {
            this.nativeIntroduced = nativeIntroduced;
        }

        public String getTsc() {
            return tsc;
        }

        public void setTsc(String tsc) {
            this.tsc = tsc;
        }

        public String getEpbc() {
            return epbc;
        }

        public void setEpbc(String epbc) {
            this.epbc = epbc;
        }

        public String getNswDistro() {
            return nswDistro;
        }

        public void setNswDistro(String nswDistro) {
            this.nswDistro = nswDistro;
        }

        public String getDistOther() {
            return distOther;
        }

        public void setDistOther(String distOther) {
            this.distOther = distOther;
        }

        public String getSynonym() {
            return synonym;
        }

        public void setSynonym(String synonym) {
            this.synonym = synonym;
        }

        public String getSpecies() {
            return species;
        }

        public void setSpecies(String species) {
            this.species = species;
        }

        public NswFloraRowType getRowType() {
            if (StringUtils.hasLength(this.infraspecies)) {
                return NswFloraRowType.INFRA;
            } else if (StringUtils.hasLength(this.species)) {
                return NswFloraRowType.SPECIES;
            } else if (StringUtils.hasLength(this.genus)) {
                return NswFloraRowType.GENUS;
            } else if (StringUtils.hasLength(this.family)) {
                return NswFloraRowType.FAMILY;
            } else {
                throw new IllegalStateException(
                        "NSW flora should haveo ne of these row types...");
            }
        }
    }
}
