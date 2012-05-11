package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import au.com.gaiaresources.taxonlib.model.ITaxonName;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
	public void setup() {
		temporalContext = taxonLibSession.getTemporalContext(now);
	}
	
	/**
	 * Tests importing the same file twice to make sure we don't have duplicate inserts.
	 * @throws Exception
	 */
	@Test
	public void testDoubleImport() throws Exception {
		runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv",
				"MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");
		
		Integer indicatorSpeciesCount = taxaDAO.countAllSpecies();
		
		runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv",
				"MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");

		assertImport();
		
		Assert.assertEquals("wrong species count", indicatorSpeciesCount, taxaDAO.countAllSpecies());
	}

	/**
	 * Test insert and assert taxonomy.
	 * @throws Exception
	 */
	@Test
	public void testImport() throws Exception {
		runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv",
				"MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");
		assertImport();
	}

	/**
	 * Assert taxonomy
	 */
	private void assertImport() {

		// check ancestor branch
		{
			IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "12783"));
			IndicatorSpecies genus = assertIndicatorSpecies(species,
					"SPECIES_12783", "Lycopodiella serpentina", "",
					"(Kunze) B.Ollg.", TaxonRank.SPECIES, true, true);
			IndicatorSpecies family = assertIndicatorSpecies(genus,
					"GENUS_20873", "Lycopodiella", "", "Holub",
					TaxonRank.GENUS, true, true);
			IndicatorSpecies order = assertIndicatorSpecies(family,
					"FAMILY_22715", "Lycopodiaceae", "", "Mirb.",
					TaxonRank.FAMILY, true, true);
			IndicatorSpecies clazz = assertIndicatorSpecies(order,
					"ORDER_38581", "Lycopodiales", "", "", TaxonRank.ORDER,
					true, true);
			IndicatorSpecies division = assertIndicatorSpecies(clazz,
					"CLASS_38532", "Lycopodiopsida", "", "", TaxonRank.CLASS,
					true, true);
			IndicatorSpecies kingdom = assertIndicatorSpecies(division,
					"DIVISION_38522", "Lycopodiophyta", "", "",
					TaxonRank.DIVISION, true, true);
			assertIndicatorSpecies(kingdom, "KINGDOM_3", "Plantae", "", "",
					TaxonRank.KINGDOM, false, true);
		}

		// check species profile
		{
			IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(
									MaxImporter.SPECIES_ID_PREFIX, "12783"));
			List<SpeciesProfile> infoItems = species.getInfoItems();

			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_IS_CURRENT, "Y");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_NATURALISED, "aa");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_NATURALISED_STATUS, "N");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_COMMENT, "This name is to be used in the Fl.of A. fide Chinnock per.cm");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_NATURALISED_CERTAINTY, "bb");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_IS_ERADICATED, "cc");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_NATURALISED_COMMENTS, "dd");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_INFORMAL, "ee");
			TaxonTestUtils.assertSpeciesProfileValue(infoItems,
					BdrsMaxImporterRowHandler.INFO_ITEM_CONSV_CODE, "ff");

		}

		// check deprecated indicator species and common name
		{			
			IndicatorSpecies species = getIndicatorSpecies(MaxImporter.getId(MaxImporter.SPECIES_ID_PREFIX, "3"));
			
			assertIndicatorSpecies(species, "SPECIES_3", "Lycopodium serpentinum", "Bog Clubmoss", "Kunze", 
					TaxonRank.SPECIES, true, false);
		}
	}
	
	/**
	 * Helper for retreving indicator species by source id.
	 * @param sourceId source ID
	 * @return
	 */
	private IndicatorSpecies getIndicatorSpecies(String sourceId) {
		ITaxonName tn = temporalContext.selectNameBySourceId(MaxImporter.MAX_SOURCE, sourceId);
		Assert.assertNotNull("Taxon name cannot be null", tn);
		return taxaDAO.getIndicatorSpeciesBySourceDataID(null,
				MaxImporter.MAX_SOURCE, tn.getId().toString());
	}

	/**
	 * Assert the indicator species.
	 * 
	 * @param is IndicatorSpecies to assert on.
	 * @param sourceId expected source ID.
	 * @param sciName expected scientific name.
	 * @param commonName expected common name.
	 * @param author expected author.
	 * @param rank expected rank.
	 * @param hasParent do we expect a parent?
	 * @param isCurrent expected current status.
	 * @return Parent if it exists and if @hasParent is true.
	 */
	private IndicatorSpecies assertIndicatorSpecies(IndicatorSpecies is,
			String sourceId, String sciName, String commonName, String author,
			TaxonRank rank, boolean hasParent, Boolean isCurrent) {
		Assert.assertNotNull("Indicator species cannot be null", is);
		Assert.assertEquals("wrong rank", rank, is.getTaxonRank());
		Assert.assertEquals("wrong source", MaxImporter.MAX_SOURCE,
				is.getSource());
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
