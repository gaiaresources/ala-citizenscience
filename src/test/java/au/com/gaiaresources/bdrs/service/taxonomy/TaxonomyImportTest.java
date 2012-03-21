package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.TaxonLibSession;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;
import au.com.gaiaresources.taxonlib.model.StringSearchType;

public abstract class TaxonomyImportTest extends AbstractTransactionalTest {

    protected TaxonLibSession taxonLibSession;

    @Before
    public void taxonomyImportTestSetup() throws Exception {
        taxonLibSession = TaxonLibSessionFactory.getSession("localhost:5432/taxonlib", "postgres", "postgres");
    }

    @After
    public void taxonomyImportTestTeardown() {
        taxonLibSession.rollback();
    }

    protected Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }
}
