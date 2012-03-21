package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.InputStream;
import java.util.Date;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.TaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;

public class BdrsMaxImporter {

	private MaxImporter taxonLibImporter;

	public BdrsMaxImporter(TaxonLibSession taxonLibSession, Date now, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
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
		BdrsMaxImporterRowHandler handler = new BdrsMaxImporterRowHandler(taxonLibSession, now, taxaDAO, spDAO);
		taxonLibImporter = new MaxImporter(taxonLibSession, handler, now);
	}
	
	public void runImport(InputStream familyCsv, InputStream generaCsv, InputStream nameCsv, InputStream xrefCsv) throws Exception {
		taxonLibImporter.runImport(familyCsv, generaCsv, nameCsv, xrefCsv);
	}
}
