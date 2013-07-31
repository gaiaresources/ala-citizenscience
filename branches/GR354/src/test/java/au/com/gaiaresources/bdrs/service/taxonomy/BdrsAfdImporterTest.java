package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporter;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporter;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

public class BdrsAfdImporterTest extends TaxonomyImportTest {

	private Date now = getDate(2011, 12, 31);
	private ITemporalContext temporalContext;

	private Logger log = Logger.getLogger(getClass());

	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private SpeciesProfileDAO spDAO;
	
	@Before
	public void setupBdrsAfdImporterTest() {
		temporalContext = taxonLibSession.getTemporalContext(now);
	}
	
	@Test
	public void testDoubleImport() throws Exception {
		runImport("AFD_TEST.csv.zip");
		
		Integer indicatorSpeciesCount = taxaDAO.countAllSpecies();
		
		runImport("AFD_TEST.csv.zip");
		
		assertImport();
		
		Assert.assertEquals("wrong species count", indicatorSpeciesCount, taxaDAO.countAllSpecies());
	}

	@Test
	public void testImport() throws Exception {
		runImport("AFD_TEST.csv.zip");
		assertImport();
	}

	private void runImport(String afdFile) throws Exception {
		
		this.commit();
		
		Session sesh = null;
		try {
			sesh = sessionFactory.openSession();
			BdrsAfdImporter importer = new BdrsAfdImporter(taxonLibSession, now, sesh,
					taxaDAO, spDAO);

			List<InputStream> streamsToClose = new ArrayList<InputStream>();
			try {
				InputStream afdStream = AfdImporter.class
						.getResourceAsStream(afdFile);
				
				streamsToClose.add(afdStream);

				importer.runImport(new ZipInputStream(afdStream));

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
		} finally {
			if (sesh != null) {
				sesh.getTransaction().commit();
				sesh.close();
			}
		}
	}

	private void assertImport() {
		// check ancestor branch
		{
			IndicatorSpecies species = getIndicatorSpecies("50b888b7-4a6e-43d5-80ae-ef929f36f486");
			assertIndicatorSpecies(species, "50b888b7-4a6e-43d5-80ae-ef929f36f486", "Fritillaria borealis",
					"", "Lohmann", "1896", TaxonRank.SPECIES, true, true);
			IndicatorSpecies genus = species.getParent();
			assertIndicatorSpecies(genus, "aad6d11e-3309-466f-99af-eb3274a64d78", "Fritillaria",
					"", "Fol", "1872", TaxonRank.GENUS, true, true);
		}
		
		// check deprecated
		{
			IndicatorSpecies deprecated = getIndicatorSpecies("763a9c68-8b19-4f82-b1de-69a0bcabcecb");
			assertIndicatorSpecies(deprecated, "763a9c68-8b19-4f82-b1de-69a0bcabcecb", "Fritillaria",
					"", "Fol", "1872", TaxonRank.GENUS, false, false);
		}
		
		// check common name
		{
			IndicatorSpecies chordata = getIndicatorSpecies("ec3c5304-8f9c-4a2a-ad08-38bb712f5edb");
			assertIndicatorSpecies(chordata, "ec3c5304-8f9c-4a2a-ad08-38bb712f5edb", "CHORDATA", 
					"Chordates", "", "", TaxonRank.PHYLUM, true, true);
		}

        // check animalia
        {
            IndicatorSpecies animalia = getIndicatorSpecies("d74fcd5e-29c1-102b-9a4a-00304854f820");
            assertIndicatorSpecies(animalia, "d74fcd5e-29c1-102b-9a4a-00304854f820", "ANIMALIA",
                    "", "Linnaeus", "1758", TaxonRank.KINGDOM, false, true);
        }
	}
	
	private IndicatorSpecies getIndicatorSpecies(String sourceId) {
		ITaxonName tn = temporalContext.selectNameBySourceId(AfdImporter.SOURCE, sourceId);
		Assert.assertNotNull("Taxon name cannot be null", tn);
		return taxaDAO.getIndicatorSpeciesBySourceDataID(null,
				AfdImporter.SOURCE, tn.getId().toString());
	}

	// returns the parent if it exists
	private void assertIndicatorSpecies(IndicatorSpecies is,
			String sourceId, String sciName, String commonName, String author, String year,
			TaxonRank rank, boolean hasParent, Boolean isCurrent) {
		
		Assert.assertNotNull("Indicator species cannot be null", is);
		Assert.assertEquals("wrong rank", rank, is.getTaxonRank());
		Assert.assertEquals("wrong source", AfdImporter.SOURCE,
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
	}
}
