package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import au.com.gaiaresources.taxonlib.model.StringSearchType;

public class NswFloraImporterTest extends TaxonomyImportTest {

    private Date now;

    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private SpeciesProfileDAO spDAO;

    private Logger log = Logger.getLogger(getClass());

    private void doImport(String file) throws Exception {
        InputStream csvStream = null;
        try {
            csvStream = getClass().getResourceAsStream(file);
            now = getDate(2000, 12, 12);

            NswFloraImporter importer = new NswFloraImporter(
                    this.taxonLibSession, now, taxaDAO, spDAO);
            importer.runImport(csvStream);
        } finally {
            if (csvStream != null) {
                try {
                    csvStream.close();
                } catch (IOException ioe) {
                    // could not close stream...
                }
            }
        }
    }

    // this just tests whether we have funky exceptions thrown and also to check how long this takes...
    // @Test
    public void testFullImport() throws Exception {
     //logging is for timing
        log.debug("NSW Flora Full import start");
        doImport("Flora_NSW_25Nov11_Final.csv");
        log.debug("NSW Flora full import end");
    }

    @Test
    public void testUpdate() throws Exception {
        // do the initial import
        doImport("Flora_NSW_TEST.csv");

        // do the updated import
        doImport("Flora_NSW_TEST_UPDATE.csv");

        ITemporalContext context = taxonLibSession.getTemporalContext(now);

        Set<String> sourceSet = new HashSet<String>();
        sourceSet.add(NswFloraImporter.NSW_FLORA_SOURCE);

        
        // Check concepts and names for the row numbers exist. Note this doesn't check the parents but we have a 
        // separate section for that
        {
            List<ITaxonName> expectedNames = new ArrayList<ITaxonName>();
            
            String[] expectedRecordNumbers = new String[] { "101", "100",
                    "102", "29324", "29331", "970", "130253", "24604", "47425",
                    "47426", "49409", "103", "104", "105" };

            for (String recNum : expectedRecordNumbers) {
                Assert.assertNotNull("expected concept for row : " + recNum, context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, recNum));
                ITaxonName n = context.selectNameBySourceId(NswFloraImporter.NSW_FLORA_SOURCE, recNum);
                Assert.assertNotNull("expected name for row : " + recNum, n);
                expectedNames.add(n);
            }
            
            for (ITaxonName tn : expectedNames) {
                IndicatorSpecies is = taxaDAO.getIndicatorSpeciesBySourceDataID(null, NswFloraImporter.NSW_FLORA_SOURCE, tn.getId().toString());
                Assert.assertNotNull("indicator species null for " + tn.getDisplayName(), is);
            }
        }
        
        {
            ITaxonConcept tc1 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "100");
            ITaxonConcept tc2 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "104");

            Assert.assertNotNull("tc1 end date should be set", tc1.getEndDate());
            
            // context.
            List<ITaxonConcept> synList = context.selectSynonymConcepts(tc2);
            Assert.assertEquals("wrong size", 1, synList.size());
            Assert.assertNotNull("expect contains tc1", TaxonTestUtils.getTaxonConceptById(synList, tc1.getId()));
        }
        
        {
            ITaxonConcept tc1 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "102");
            ITaxonConcept tc2 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "104");
            ITaxonConcept tc3 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "103");

            Assert.assertNotNull("tc1 end date should be set", tc1.getEndDate());
            Assert.assertNotNull("tc2 end date should be set", tc2.getEndDate());
            Assert.assertNull("tc3 end date should NOT be set", tc3.getEndDate());
            
            // context.
            List<ITaxonConcept> synList = context.selectSynonymConcepts(tc3);
            Assert.assertEquals("wrong size", 2, synList.size());
            Assert.assertNotNull("expect contains tc1", TaxonTestUtils.getTaxonConceptById(synList, tc1.getId()));
            Assert.assertNotNull("expect contains tc2", TaxonTestUtils.getTaxonConceptById(synList, tc2.getId()));
        }
    }

    @Test
    public void testDoubleImport() throws Exception {
        doImport("Flora_NSW_TEST.csv");
        //the second import should have no effect...
        doImport("Flora_NSW_TEST.csv");

        assertInitialImport();
    }

    @Test
    public void testImport() throws Exception {
        doImport("Flora_NSW_TEST.csv");

        assertInitialImport();
    }

    private void assertInitialImport() {
        ITemporalContext context = taxonLibSession.getTemporalContext(now);

        Set<String> sourceSet = new HashSet<String>();
        sourceSet.add(NswFloraImporter.NSW_FLORA_SOURCE);

        {
            List<ITaxonConcept> searchResult = context.searchByName("Rhodanthe microglossa forma. Awesome", null, StringSearchType.EXACT, null, null);
            Assert.assertEquals("wrong size", 1, searchResult.size());
            ITaxonConcept tc = searchResult.get(0);
            Assert.assertEquals("wrong author", "Recard.j.", tc.getAuthor());
            ITaxonConcept tc_microglossa = tc.getParent();
            Assert.assertNotNull("parent should not be null");
            Assert.assertNotNull("parent should not have null name", tc_microglossa.getName());
            Assert.assertEquals("wrong name for parent", "microglossa", tc_microglossa.getName().getName());
            Assert.assertEquals("wrong display name for parent", "Rhodanthe microglossa", tc_microglossa.getName().getDisplayName());

            // 2 items in different families.
            List<ITaxonConcept> searchResult2 = context.searchByName("Rhodanthe microglossa", null, StringSearchType.EXACT, null, null);
            Assert.assertEquals("wrong size", 2, searchResult2.size());

            Assert.assertNotNull("expect not null", TaxonTestUtils.getTaxonConceptById(searchResult2, tc_microglossa.getId()));

            List<ITaxonConcept> searchResult3 = context.searchByHistoricDisplayName("Helipterum microglossum", null, StringSearchType.EXACT, null, null);
            Assert.assertEquals("wrong size", 1, searchResult3.size());
        }
        
        // Check concepts and names for the row numbers exist. Note this doesn't check the parents but we have a 
        // separate section for that
        {
            List<ITaxonName> expectedNames = new ArrayList<ITaxonName>();
            String[] expectedRecordNumbers = new String[] { "101", "100",
                    "102", "29324", "29331", "970", "130253", "24604", "47425",
                    "47426", "49409" };

            for (String recNum : expectedRecordNumbers) {
                Assert.assertNotNull("expected concept for row : " + recNum, context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, recNum));
                ITaxonName n = context.selectNameBySourceId(NswFloraImporter.NSW_FLORA_SOURCE, recNum);
                Assert.assertNotNull("expected name for row : " + recNum, n);
                expectedNames.add(n);
            }
            
            for (ITaxonName tn : expectedNames) {
                IndicatorSpecies is = taxaDAO.getIndicatorSpeciesBySourceDataID(null, NswFloraImporter.NSW_FLORA_SOURCE, tn.getId().toString());
                Assert.assertNotNull("indicator species null for "
                        + tn.getDisplayName(), is);
            }
        }

        {
            ITaxonConcept tc1 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "29324");
            ITaxonConcept tc2 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "29331");
            ITaxonConcept tc3 = context.selectConceptByNameSourceId(NswFloraImporter.NSW_FLORA_SOURCE, "970");

            // context.
            List<ITaxonConcept> synList = context.selectSynonymConcepts(tc3);
            Assert.assertEquals("wrong size", 2, synList.size());
            Assert.assertNotNull("expect contains tc1", TaxonTestUtils.getTaxonConceptById(synList, tc1.getId()));
            Assert.assertNotNull("expect contains tc2", TaxonTestUtils.getTaxonConceptById(synList, tc2.getId()));
        }

        // test for common names...
        {
            ITaxonConcept tc2 = TaxonTestUtils.searchUniqueByDisplayName(context, "Rhodanthe microglossa forma. Awesome");
            ITaxonConcept tc3 = TaxonTestUtils.searchUniqueByDisplayName(context, "Rhodanthe microglossa forma. Cowabunga");

            Assert.assertNotNull("concept cannot be null", tc2);
            Assert.assertNotNull("concept cannot be null", tc3);

            ITaxonName tn2 = TaxonTestUtils.getUniqueCommonName(context, tc2);
            ITaxonName tn3 = TaxonTestUtils.getUniqueCommonName(context, tc3);

            Assert.assertNotNull("name cannot be null", tn2);
            Assert.assertNotNull("name cannot be null", tn3);

            Assert.assertEquals("wrong name", "common name two", tn2.getDisplayName());
            Assert.assertEquals("wrong name", "common name three", tn3.getDisplayName());
        }

        // make sure the indicator species are there...

        // the minus one is for the ITaxonConcept root node which doesn't exist for the indicator species tree.
        // Assert.assertEquals("wrong indicator species count", EXPECTED_CONCEPT_COUNT - 1, taxaDAO.countAllSpecies().intValue());
        {
            // 101 is our test item with the species profile items...
            IndicatorSpecies sp101 = taxaDAO.getIndicatorSpeciesByScientificName("Rhodanthe microglossa forma. Awesome");
            Assert.assertNotNull("expect indicator species", sp101);
            Assert.assertEquals("wrong common name", "common name two", sp101.getCommonName());
            assertSpeciesProfileValue(sp101.getInfoItems(), NswFloraImporter.SPECIES_PROFILE_DIST_OTHER, "WA");
            assertSpeciesProfileValue(sp101.getInfoItems(), NswFloraImporter.SPECIES_PROFILE_NSW_TSC, "hello");
            assertSpeciesProfileValue(sp101.getInfoItems(), NswFloraImporter.SPECIES_PROFILE_EPBC_STATUS, "world");
            assertSpeciesProfileValue(sp101.getInfoItems(), NswFloraImporter.SPECIES_PROFILE_NSW_DISTRO, "NC CC SC NT CT ST NWS CWS SWS NWP SWP");
            assertSpeciesProfileValue(sp101.getInfoItems(), NswFloraImporter.SPECIES_PROFILE_NATIVE_INTRODUCED, "I");
            Assert.assertEquals("wrong rank", au.com.gaiaresources.bdrs.model.taxa.TaxonRank.INFRASPECIES, sp101.getTaxonRank());

            IndicatorSpecies sp101_species = sp101.getParent();
            Assert.assertNotNull("sp101 species cant be null", sp101_species);
            Assert.assertEquals("wrong sci name", "Rhodanthe microglossa", sp101_species.getScientificName());
            Assert.assertEquals("wrong common name", "common name one", sp101_species.getCommonName());
            Assert.assertEquals("wrong rank", au.com.gaiaresources.bdrs.model.taxa.TaxonRank.SPECIES, sp101_species.getTaxonRank());

            IndicatorSpecies sp101_genus = sp101_species.getParent();
            Assert.assertNotNull("sp101 genus cant be null", sp101_genus);
            Assert.assertEquals("wrong sci name", "Rhodanthe", sp101_genus.getScientificName());
            Assert.assertEquals("wrong common name", "", sp101_genus.getCommonName());
            Assert.assertEquals("wrong rank", au.com.gaiaresources.bdrs.model.taxa.TaxonRank.GENUS, sp101_genus.getTaxonRank());

            IndicatorSpecies sp101_family = sp101_genus.getParent();
            Assert.assertNotNull("sp101 family cant be null", sp101_family);
            Assert.assertEquals("wrong sci name", "Afamily", sp101_family.getScientificName());
            Assert.assertEquals("wrong common name", "", sp101_family.getCommonName());
            Assert.assertEquals("wrong rank", au.com.gaiaresources.bdrs.model.taxa.TaxonRank.FAMILY, sp101_family.getTaxonRank());

            //IndicatorSpecies sp100 = taxaDAO.getIndicatorSpeciesBySourceDataID(null, NswFloraImporter.NSW_FLORA_SOURCE, "100");
            IndicatorSpecies sp100 = taxaDAO.getIndicatorSpeciesByScientificName("Rhodanthe microglossa");
            Assert.assertNotNull("expect indicator species", sp100);
            Assert.assertEquals("wrong rank", au.com.gaiaresources.bdrs.model.taxa.TaxonRank.SPECIES, sp100.getTaxonRank());

            // make sure 101 and 102 share a common parent..
            IndicatorSpecies sp102 = taxaDAO.getIndicatorSpeciesByScientificName("Rhodanthe microglossa forma. Cowabunga");
            Assert.assertNotNull("sp102 cant be null");
            IndicatorSpecies sp102_species = sp102.getParent();
            Assert.assertNotNull("sp102 species cant be null");
            Assert.assertEquals("wrong ids", sp102_species.getId(), sp101_species.getId());
        }
    }

    private SpeciesProfile getProfileItemByType(List<SpeciesProfile> spList,
            String type) {
        for (SpeciesProfile sp : spList) {
            if (sp.getType().equals(type)) {
                return sp;
            }
        }
        return null;
    }

    private void assertSpeciesProfileValue(List<SpeciesProfile> spList,
            String type, String expectedValue) {
        SpeciesProfile sp = getProfileItemByType(spList, type);
        Assert.assertNotNull("SpeciesProfile cannot be null", sp);
        Assert.assertEquals("wrong value", expectedValue, sp.getContent());
    }
}
