package au.com.gaiaresources.bdrs.service.taxonomy;

import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraImporter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 25/06/13
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class BdrsNswFaunaImporterTest extends BdrsNswFloraImporterTest {

    /**
     * Do the import
     * @param file File to import
     * @throws Exception Exception thrown during import
     */
    protected void doImport(String file) throws Exception {
        InputStream csvStream = null;
        try {
            csvStream = NswFloraImporter.class.getResourceAsStream(file);
            now = getDate(2000, 12, 12);

            BdrsNswFaunaImporter importer = new BdrsNswFaunaImporter(
                    this.taxonLibSession, now, sessionFactory, taxaDAO, spDAO);
            importer.runImport(csvStream);
        } finally {
            if (csvStream != null) {
                try {
                    csvStream.close();
                } catch (IOException ioe) {
                    // could not close stream...
                }
            }
        }
    }

    /**
     * Get the taxon group name to which we are importing.
     * @return taxon group name
     */
    protected String getTaxonGroupName() {
        return BdrsNswFaunaImporter.TAXON_GROUP_NAME;
    }

}
