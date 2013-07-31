package au.com.gaiaresources.bdrs.service.taxonomy;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.Before;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;

public abstract class TaxonomyImportTest extends AbstractControllerTest {

    protected ITaxonLibSession taxonLibSession;
    
    private static Logger log = Logger.getLogger(TaxonomyImportTest.class);
    
    @Before
    public void taxonomyImportTestSetup() throws Exception {
        taxonLibSession = getRequestContext().getTaxonLibSession();

        // Flush the current session so that the un-flushed persistent objects such as Portal
        // get saved to the database before we begin importing. If you do not flush here,
        // the importer will fail because it cannot find the Portal.
        commit();
        requestDropDatabase();
    }

    

    protected Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }
}
