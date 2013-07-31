package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.TaxonLibException;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;

import java.io.InputStream;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 25/06/13
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class BdrsNswFaunaImporter extends BdrsNswFloraImporter {

    public static final String TAXON_GROUP_NAME = "NSW Fauna";

    private Logger log = Logger.getLogger(getClass());

    public BdrsNswFaunaImporter(ITaxonLibSession session, Date now, SessionFactory sessionFactory, TaxaDAO taxaDAO, SpeciesProfileDAO spDAO)
            throws TaxonLibException {
        super(session, now, sessionFactory, taxaDAO, spDAO);
    }

    @Override
    protected String getTaxonGroupName() {
        return TAXON_GROUP_NAME;
    }
}
