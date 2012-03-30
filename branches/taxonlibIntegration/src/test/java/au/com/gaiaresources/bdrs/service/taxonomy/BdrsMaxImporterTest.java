package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsMaxImporterTest extends TaxonomyImportTest {

	private Date now = getDate(2011, 12, 31);
	private ITemporalContext temporalContext;

	private Logger log = Logger.getLogger(getClass());

	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private SpeciesProfileDAO spDAO;
	
	@Before
	public void setup() {
		temporalContext = taxonLibSession.getTemporalContext(now);
	}
	
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

	@Test
	public void testImport() throws Exception {
		runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv",
				"MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");
		assertImport();
	}

	private void runImport(String familyFile, String generaFile,
			String nameFile, String xrefFile) throws Exception {
		BdrsMaxImporter importer = new BdrsMaxImporter(taxonLibSession, now,
				taxaDAO, spDAO);

		List<InputStream> streamsToClose = new ArrayList<InputStream>();
		try {
			InputStream familyStream = MaxImporter.class
					.getResourceAsStream(familyFile);
			streamsToClose.add(familyStream);
			InputStream generaStream = MaxImporter.class
					.getResourceAsStream(generaFile);
			streamsToClose.add(generaStream);
			InputStream nameStream = MaxImporter.class
					.getResourceAsStream(nameFile);
			streamsToClose.add(nameStream);
			InputStream xrefStream = MaxImporter.class
					.getResourceAsStream(xrefFile);
			streamsToClose.add(xrefStream);

			importer.runImport(familyStream, generaStream, nameStream,
					xrefStream);

		} finally {
			for (InputStream is : streamsToClose) {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException ioe) {
					log.error("Could not close stream", ioe);
				}
			}
		}
	}

	private void assertImport() {
		
		temporalContext.dumpDatabase();
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
	
	private IndicatorSpecies getIndicatorSpecies(String sourceId) {
		ITaxonName tn = temporalContext.selectNameBySourceId(MaxImporter.MAX_SOURCE, sourceId);
		Assert.assertNotNull("Taxon name cannot be null", tn);
		return taxaDAO.getIndicatorSpeciesBySourceDataID(null,
				MaxImporter.MAX_SOURCE, tn.getId().toString());
	}

	// returns the parent if it exists
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
