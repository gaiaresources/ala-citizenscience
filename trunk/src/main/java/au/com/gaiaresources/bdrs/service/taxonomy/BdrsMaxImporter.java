package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.InputStream;
import java.util.Date;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import org.hibernate.SessionFactory;

public class BdrsMaxImporter {

	private MaxImporter taxonLibImporter;

	public BdrsMaxImporter(ITaxonLibSession taxonLibSession, Date now,
                           SessionFactory sessionFactory, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
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
		BdrsMaxImporterRowHandler handler = new BdrsMaxImporterRowHandler(taxonLibSession, now,
                                                                            sessionFactory, taxaDAO, spDAO);
		taxonLibImporter = new MaxImporter(taxonLibSession, handler, now);
	}
	
	public void runImport(InputStream familyCsv, InputStream generaCsv, InputStream nameCsv, InputStream xrefCsv) throws Exception {
		taxonLibImporter.runImport(familyCsv, generaCsv, nameCsv, xrefCsv);
	}
}
