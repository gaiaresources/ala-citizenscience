package au.com.gaiaresources.bdrs.controller.map;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService;
import au.com.gaiaresources.bdrs.spatial.ShapeFileWriter;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a set of {@link Record}s to a xls, shapefile, or kml file.
 */
public class RecordDownloadWriter extends AbstractDownloadWriter<Record> {
    
    private boolean serializeLazyLoadedAttributes;
    
    /**
     * Create a new writer
     * @param preferenceDAO PreferenceDAO
     * @param serverURL The serverURL is a combination of domain, tomcat context path and
     * portal context path.
     * e.g. http://core.gaiaresources.com.au/bdrs-core/portal/1
     * e.g. http://core.gaiaresources.com.au/bdrs-core/erwa
     * @param serializeLazyLoadedAttributes Serialize lazy loaded attributes,
     *                                      is slow and can cause heap
     * problems for large numbers of records. 
     * When showing KML for maps we don't want to include attributes. 
     * When downloading the entire KML we want to include the attributes.
     */
    public RecordDownloadWriter(PreferenceDAO preferenceDAO,
                                String serverURL,
                                boolean serializeLazyLoadedAttributes) {
        super(preferenceDAO, serverURL);
        this.serializeLazyLoadedAttributes = serializeLazyLoadedAttributes;
    }
    
    @SuppressWarnings("UnusedDeclaration")
    private static Logger log = Logger.getLogger(RecordDownloadWriter.class);
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.map.AbstractDownloadWriter#writeXLSRecords(au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService, java.io.OutputStream, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.db.ScrollableResults, org.hibernate.Session)
     */
    @Override
    protected void writeXLSRecords(BulkDataService bulkDataService,
            OutputStream out, Survey survey,
            ScrollableResults<Record> sc, Session sesh) throws Exception {
        bulkDataService.exportSurveyRecords(sesh, survey, sc, out);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.map.AbstractDownloadWriter#writeSHPRecords(java.io.OutputStream, org.hibernate.Session, au.com.gaiaresources.bdrs.model.user.User, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.db.ScrollableResults)
     */
    @Override
    protected void writeSHPRecords(OutputStream out, Session sesh,
            User accessingUser, Survey survey, ScrollableResults<Record> sc) throws Exception {
        // This is not a good thing
        // -----
        // Yes, I believe I just made it worse
        List<Record> recordList = new ArrayList<Record>();

        while(sc.hasMoreElements()) {
            Record r = sc.nextElement();
            if (survey == null || r.getSurvey() == survey) {
                recordList.add(r);
            }
        }
        
        if (!recordList.isEmpty()) {
            
            // no point writing a non empty shapefile since the user is not
            // expecting a template in this download but a populated shapefile
            ShapeFileWriter writer = new ShapeFileWriter(serverURL);
            File zipFile = writer.exportRecords(recordList, accessingUser);
            
            sesh.clear();
            recordList.clear();
            
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(zipFile);
                IOUtils.copy(inStream, out);
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
            }    
        }
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.map.AbstractDownloadWriter#writeKMLRecords(java.io.OutputStream, org.hibernate.Session, java.lang.String, au.com.gaiaresources.bdrs.model.user.User, au.com.gaiaresources.bdrs.db.ScrollableResults)
     */
    @Override
    protected void writeKMLRecords(OutputStream out, Session sesh, User accessingUser, ScrollableResults<Record> sc)
            throws JAXBException {
        int recordCount = 0;
        List<Record> rList = new ArrayList<Record>(ScrollableResults.RESULTS_BATCH_SIZE);

        BDRSKMLWriter writer = new BDRSKMLWriter(preferenceDAO, serverURL, null);

        while (sc.hasMoreElements()) {
            rList.add(sc.nextElement());
            
            // evict to ensure garbage collection
            if (++recordCount % ScrollableResults.RESULTS_BATCH_SIZE == 0) {

                writer.writeRecords(accessingUser, rList, serializeLazyLoadedAttributes);

                rList.clear();
                sesh.clear();
            }
        }
        // Flush the remainder out of the list.
        writer.writeRecords(accessingUser, rList, serializeLazyLoadedAttributes);

        sesh.clear();
        writer.write(false, out);
    }
}
