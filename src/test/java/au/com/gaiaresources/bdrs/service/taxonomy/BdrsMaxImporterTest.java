package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.bdrs.service.taxonomy.max.SpeciesProfileTaxonNameConsvCodeBuilder;
import au.com.gaiaresources.bdrs.service.taxonomy.max.SpeciesProfileTaxonNameDateBuilder;
import au.com.gaiaresources.bdrs.service.taxonomy.max.SpeciesProfileTaxonNameInformalBuilder;
import au.com.gaiaresources.bdrs.service.taxonomy.max.SpeciesProfileTaxonNameNaturalisedStatusBuilder;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.MaxFamilyRow;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Tests the BdrsMaxImporter
 * 
 */
public class BdrsMaxImporterTest extends AbstractBdrsMaxImporterTest {

    private ITemporalContext temporalContext;

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private TaxaDAO taxaDAO;

    @Before
    public void setupAbstractBdrsMaxImporter() {
        temporalContext = taxonLibSession.getTemporalContext(now);
    }

    /**
     * Tests importing the same file twice to make sure we don't have duplicate
     * inserts.
     * 
     * @throws Exception
     */
    @Test
    public void testDoubleImport() throws Exception {
        runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv", "MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");

        Integer indicatorSpeciesCount = taxaDAO.countAllSpecies();

        runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv", "MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");

        assertImport();

        Assert.assertEquals("wrong species count", indicatorSpeciesCount, taxaDAO.countAllSpecies());
    }

    /**
     * Test insert and assert taxonomy.
     * 
     * @throws Exception
     */
    @Test
    public void testImport() throws Exception {
        runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv", "MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");
        assertImport();
    }

    /**
     * Assert taxonomy
     */
    private void assertImport() throws ParseException {

        // check ancestor branch
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "12783"));
            IndicatorSpecies genus = assertIndicatorSpecies(species, "SPECIES_12783", "Lycopodiella serpentina", "", "(Kunze) B.Ollg.", TaxonRank.SPECIES, true, true);
            IndicatorSpecies family = assertIndicatorSpecies(genus, "GENUS_20873", "Lycopodiella", "", "Holub", TaxonRank.GENUS, true, true);
            IndicatorSpecies order = assertIndicatorSpecies(family, "FAMILY_22715", "Lycopodiaceae", "", "Mirb.", TaxonRank.FAMILY, true, true);
            IndicatorSpecies clazz = assertIndicatorSpecies(order, "ORDER_38581", "Lycopodiales", "", "", TaxonRank.ORDER, true, true);
            IndicatorSpecies division = assertIndicatorSpecies(clazz, "CLASS_38532", "Lycopodiopsida", "", "", TaxonRank.CLASS, true, true);
            IndicatorSpecies kingdom = assertIndicatorSpecies(division, "DIVISION_38522", "Lycopodiophyta", "", "", TaxonRank.DIVISION, true, true);
            assertIndicatorSpecies(kingdom, "KINGDOM_3", "Plantae", "", "", TaxonRank.KINGDOM, false, true);
        }

        // check species profile
        SimpleDateFormat sourceFormatter = new SimpleDateFormat(
                SpeciesProfileTaxonNameDateBuilder.SOURCE_FORMAT_PATTERN);
        SimpleDateFormat targetFormatter = new SimpleDateFormat(
                SpeciesProfileTaxonNameDateBuilder.TARGET_FORMAT_PATTERN);
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "12783"));
            List<SpeciesProfile> infoItems = species.getInfoItems();

            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.AUTHOR.toString(), "(Kunze) B.Ollg.");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.EDITOR.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.REFERENCE.toString(), "Op.Bot. 92:176 (1987)");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.COMMENTS.toString(), "This name is to be used in the Fl.of A. fide Chinnock per.cm");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_CURRENT.toString(), "Yes");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED.toString(), "Yes");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_STATUS.toString(), SpeciesProfileTaxonNameNaturalisedStatusBuilder.CODE_LOOKUP.get("N"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_CERTAINTY.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_ERADICATED.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_COMMENTS.toString(), "Fozzy");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.INFORMAL.toString(), SpeciesProfileTaxonNameInformalBuilder.CODE_LOOKUP.get("MS"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.CONSV_CODE.toString(), SpeciesProfileTaxonNameConsvCodeBuilder.CODE_LOOKUP.get("T"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.ADDED_ON.toString(), targetFormatter.format(sourceFormatter.parse("31/10/1991")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.UPDATED_ON.toString(), targetFormatter.format(sourceFormatter.parse("10/12/2004")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.FAMILY_CODE.toString(), "2");
        }
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "12813"));
            List<SpeciesProfile> infoItems = species.getInfoItems();

            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.AUTHOR.toString(), "(L.) Pic.Serm.");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.EDITOR.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.REFERENCE.toString(), "Webbia 23:166 (1968)");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.COMMENTS.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_CURRENT.toString(), "Yes");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_STATUS.toString(), SpeciesProfileTaxonNameNaturalisedStatusBuilder.CODE_LOOKUP.get("A"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_CERTAINTY.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_ERADICATED.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_COMMENTS.toString(), "Wozzy");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.INFORMAL.toString(), SpeciesProfileTaxonNameInformalBuilder.CODE_LOOKUP.get("PN"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.CONSV_CODE.toString(), SpeciesProfileTaxonNameConsvCodeBuilder.CODE_LOOKUP.get("X"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.ADDED_ON.toString(), targetFormatter.format(sourceFormatter.parse("4/11/1991")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.UPDATED_ON.toString(), targetFormatter.format(sourceFormatter.parse("10/12/2004")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.FAMILY_CODE.toString(), "2");
        }
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "2"));
            List<SpeciesProfile> infoItems = species.getInfoItems();

            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.AUTHOR.toString(), "L.");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.EDITOR.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.REFERENCE.toString(), "Sp.Pl. 2:1103 (1753)");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.COMMENTS.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_CURRENT.toString(), "No");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED.toString(), null);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_STATUS.toString(), SpeciesProfileTaxonNameNaturalisedStatusBuilder.CODE_LOOKUP.get("M"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_CERTAINTY.toString(), "Yes");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.IS_ERADICATED.toString(), "Yes");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.NATURALISED_COMMENTS.toString(), "Bear");
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.INFORMAL.toString(), SpeciesProfileTaxonNameInformalBuilder.CODE_LOOKUP.get(""));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.CONSV_CODE.toString(), SpeciesProfileTaxonNameConsvCodeBuilder.CODE_LOOKUP.get("1"));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.ADDED_ON.toString(), targetFormatter.format(sourceFormatter.parse("1/01/1992")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.UPDATED_ON.toString(), targetFormatter.format(sourceFormatter.parse("10/12/2004")));
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.FAMILY_CODE.toString(), "2");
        }
        
        {
            // check for different family code
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "8"));
            List<SpeciesProfile> infoItems = species.getInfoItems();
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxNameRow.ColumnName.FAMILY_CODE.toString(), "4");
        }

        // check deprecated indicator species and common name
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "3"));

            assertIndicatorSpecies(species, "SPECIES_3", "Lycopodium serpentinum", "Bog Clubmoss", "Kunze", TaxonRank.SPECIES, true, false);
        }
        
        // check species profile for family item
        {
            IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.FAMILY_ID_PREFIX, "22715"));
            List<SpeciesProfile> infoItems = species.getInfoItems();
            Assert.assertNotNull("info items cannot be null", infoItems);
            TaxonTestUtils.assertSpeciesProfileValue(infoItems, MaxFamilyRow.ColumnName.FAMILY_CODE.toString(), "002");
        }
    }

    /**
     * Helper for retreving indicator species by source id.
     * 
     * @param sourceId
     *            source ID
     * @return
     */
    private IndicatorSpecies getIndicatorSpecies(String sourceId) {
        ITaxonName tn = temporalContext.selectNameBySourceId(MaxImporter.MAX_SOURCE, sourceId);
        Assert.assertNotNull("Taxon name cannot be null", tn);
        return taxaDAO.getIndicatorSpeciesBySourceDataID(null, MaxImporter.MAX_SOURCE, tn.getId().toString());
    }

    /**
     * Assert the indicator species.
     * 
     * @param is
     *            IndicatorSpecies to assert on.
     * @param sourceId
     *            expected source ID.
     * @param sciName
     *            expected scientific name.
     * @param commonName
     *            expected common name.
     * @param author
     *            expected author.
     * @param rank
     *            expected rank.
     * @param hasParent
     *            do we expect a parent?
     * @param isCurrent
     *            expected current status.
     * @return Parent if it exists and if @hasParent is true.
     */
    private IndicatorSpecies assertIndicatorSpecies(IndicatorSpecies is,
            String sourceId, String sciName, String commonName, String author,
            TaxonRank rank, boolean hasParent, Boolean isCurrent) {
        Assert.assertNotNull("Indicator species cannot be null", is);
        Assert.assertEquals("wrong rank", rank, is.getTaxonRank());
        Assert.assertEquals("wrong source", MaxImporter.MAX_SOURCE, is.getSource());
        Assert.assertEquals("wrong sci name", sciName, is.getScientificName());
        Assert.assertEquals("wrong common name", commonName, is.getCommonName());
        Assert.assertEquals("wrong author", author, is.getAuthor());
        if (hasParent) {
            Assert.assertNotNull("parent cannot be null", is.getParent());
        } else {
            Assert.assertNull("parent should be null", is.getParent());
        }
        Assert.assertEquals("wrong current status", isCurrent, is.getCurrent());
        return is.getParent();
    }
}
