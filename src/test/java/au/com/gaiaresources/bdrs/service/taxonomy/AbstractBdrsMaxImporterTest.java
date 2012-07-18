package au.com.gaiaresources.bdrs.service.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;

/**
 * Inherit from this test and use runDefaultImport() to create some basic taxonomic data
 * that uses taxonLib.
 *
 */
public abstract class AbstractBdrsMaxImporterTest extends TaxonomyImportTest {

    private Logger log = Logger.getLogger(getClass());
    
    /**
     * Date of the import.
     */
    protected Date now = getDate(2011, 12, 31);
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private SpeciesProfileDAO spDAO;

    protected void runDefaultImport() throws Exception {
        runImport("MAX_PlantFamilies_TEST.csv", "MAX_PlantGenera_TEST.csv",
                "MAX_PlantNames_TEST.csv", "MAX_PlantCrossRef_TEST.csv");
    }

    protected void runImport(String familyFile, String generaFile,
            String nameFile, String xrefFile) throws Exception {
        BdrsMaxImporter importer = new BdrsMaxImporter(taxonLibSession, now,
                sessionFactory, taxaDAO, spDAO);

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
}
