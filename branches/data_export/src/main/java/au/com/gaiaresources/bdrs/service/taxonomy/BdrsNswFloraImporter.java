package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.InputStream;
import java.util.Date;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporter;
import org.hibernate.SessionFactory;

public class BdrsNswFloraImporter {
    
    private Logger log = Logger.getLogger(getClass());

    private NswFloraImporter importer;

    public BdrsNswFloraImporter(ITaxonLibSession session, Date now, SessionFactory sessionFactory, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO)
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
        BdrsNswFloraImporterRowHandler handler = new BdrsNswFloraImporterRowHandler(session, now, sessionFactory, taxaDAO, spDAO);
        importer = new NswFloraImporter(session, handler, now);
    }

    public void runImport(InputStream csv) throws Exception {
    	importer.runImport(csv);
    }
}
