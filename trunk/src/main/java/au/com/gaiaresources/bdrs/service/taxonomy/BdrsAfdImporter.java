package au.com.gaiaresources.bdrs.service.taxonomy;



import java.io.InputStream;
import java.util.Date;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporter;
import au.com.gaiaresources.taxonlib.importer.afd.AfdImporterRowHandler;

public class BdrsAfdImporter {
	
	private AfdImporter importer;

	public BdrsAfdImporter(ITaxonLibSession taxonLibSession, Date now, Session sesh, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO) {
		if (taxonLibSession == null) {
			throw new IllegalArgumentException("TaxonLibSession cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
		if (sesh == null) {
			throw new IllegalArgumentException("Hibernate Session cannot be null");
		}
		if (taxaDAO == null) {
			throw new IllegalArgumentException("TaxaDAO cannot be null");
		}
		if (spDAO == null) {
			throw new IllegalArgumentException("SpeciesProfileDAO cannot be null");
		}
		
		sesh.setFlushMode(FlushMode.MANUAL);
		
		AfdImporterRowHandler rowHandler = new BdrsAfdImporterRowHandler(taxonLibSession, now, sesh, taxaDAO, spDAO);
		importer = new AfdImporter(taxonLibSession, rowHandler, now);
	}
	
	public void runImport(InputStream afdCsv) throws Exception {
		importer.runImport(afdCsv);
	}
}
