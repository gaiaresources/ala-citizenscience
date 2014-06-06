package au.com.gaiaresources.bdrs.controller.review.sightings;

import au.com.gaiaresources.bdrs.controller.map.RecordDownloadFormat;
import au.com.gaiaresources.bdrs.controller.map.RecordDownloadWriter;
import au.com.gaiaresources.bdrs.controller.record.RecordController;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService;
import au.com.gaiaresources.bdrs.util.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SightingsController extends RecordController {
    
    @SuppressWarnings("UnusedDeclaration")
    private Logger log = Logger.getLogger(getClass());

    public static final String QUERY_PARAM_DOWNLOAD_FORMAT = "download_format";
    private static final String KML_FILENAME = "Records.kml";
    private static final String SHAPEFILE_ZIP_ENTRY_FORMAT = "shp/Survey%d_%s.zip";
    private static final String XLS_ZIP_ENTRY_FORMAT = "xls/Survey%d_%s.xls";
    
    public static final String SIGHTINGS_DOWNLOAD_CONTENT_TYPE = "application/zip";
    
    @Autowired
    protected BulkDataService bulkDataService;
    @Autowired
    private PreferenceDAO preferenceDAO;
    
    /**
     * For some scrollable records, create files in the requested download format and
     * zip them up
     *
     * @param response - the http response object.
     * @param downloadFormat - array containing the download formats.
     * @param sc - the scrollable results object.
     * @param surveyList - the list of surveys to download.
     * @throws Exception
     */
    protected void downloadSightings(
            HttpServletResponse response, 
            String[] downloadFormat, 
            ScrollableResults<Record> sc, 
            List<Survey> surveyList) throws Exception {
        
        User user = getRequestContext().getUser();

        if (response.isCommitted()) {
            return;
        }

        response.setContentType(SIGHTINGS_DOWNLOAD_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment;filename=sightings_"
                + System.currentTimeMillis() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
        try {
            if (downloadFormat != null) {
                String serverURL = getRequestContext().getServerURL();
                Session sesh = getRequestContext().getHibernate();
                BDRSKMLWriter kmlWriter = new BDRSKMLWriter(preferenceDAO, serverURL, null);
                RecordDownloadWriter downloadWriter =
                        new RecordDownloadWriter(preferenceDAO, serverURL, true);
                for (String format : downloadFormat) {

                    RecordDownloadFormat rdf = RecordDownloadFormat.valueOf(format);
                    switch (rdf) {
                    case KML: {
                        // make sure scrollable records is rewound and ready to go!
                        sc.rewind();
                        ZipEntry kmlEntry = new ZipEntry(KML_FILENAME);
                        zos.putNextEntry(kmlEntry);
                        writeKML(zos, sesh, kmlWriter, user, sc, true);
                        zos.closeEntry();
                        break;
                    }
                    case SHAPEFILE: {
                        for (Survey survey : surveyList) {
                            
                            // make sure scrollable records is rewound and ready to go!
                            sc.rewind();
                            
                            ZipEntry shpEntry = new ZipEntry(
                                    String.format(SHAPEFILE_ZIP_ENTRY_FORMAT, survey.getId(), FileUtils.getSafeFilename(survey.getName())));
                            zos.putNextEntry(shpEntry);

                            // The writer impl will flush the session and disconnect the survey.
                            // There's something strange going on with the sessions here. Make sure the 
                            // survey object we are using belongs to the correct session. If the survey
                            // already belongs to the correct session this will have no effect.
                            survey = surveyDAO.getSurvey(sesh, survey.getId());
                            
                            downloadWriter.write(bulkDataService, rdf, zos, sesh, survey, user, sc);
                            zos.closeEntry();
                        }
                        break;
                    }
                    case XLS: {
                        for (Survey survey : surveyList) {
                            
                            // make sure scrollable records is rewound and ready to go!
                            sc.rewind();
                            
                            ZipEntry shpEntry = new ZipEntry(
                                    String.format(XLS_ZIP_ENTRY_FORMAT, survey.getId(), FileUtils.getSafeFilename(survey.getName())));
                            zos.putNextEntry(shpEntry);

                            // The writer impl will flush the session and disconnect the survey.
                            // There's something strange going on with the sessions here. Make sure the 
                            // survey object we are using belongs to the correct session. If the survey
                            // already belongs to the correct session this will have no effect.
                            survey = surveyDAO.getSurvey(sesh, survey.getId());
                            
                            downloadWriter.write(bulkDataService, rdf, zos, sesh, survey, user, sc);
                            zos.closeEntry();
                        }
                        break;
                    }
                    default:
                        // Do Nothing
                        break;
                    }
                }
            }

        } finally {
            zos.flush();
            zos.close();
        }
    }
    
    /**
     * Write KML
     * 
     * @param zos ZipOutputStream
     * @param sesh Hibernate session
     * @param writer BDRSKMLWriter
     * @param user User requesting the write
     * @param sc Hibernate scrollable record results
     * @param serializeAttributes Whether to include record attributes as json which is embedded in the KML.
     * Very slow database access for large numbers of records and may also cause heap problems!
     * @throws JAXBException
     */
    private void writeKML(ZipOutputStream zos, Session sesh, BDRSKMLWriter writer, User user, ScrollableResults<Record> sc,
            boolean serializeAttributes) throws JAXBException {
        int recordCount = 0;
        List<Record> rList = new ArrayList<Record>(ScrollableResults.RESULTS_BATCH_SIZE);

        while (sc.hasMoreElements()) {
            rList.add(sc.nextElement());
            // evict to ensure garbage collection
            if (++recordCount % ScrollableResults.RESULTS_BATCH_SIZE == 0) {

                writer.writeRecords(user, rList, serializeAttributes);

                rList.clear();
                sesh.clear();
            }
        }
        
        // Flush the remainder out of the list.
        writer.writeRecords(user, rList, serializeAttributes);
        sesh.clear();
        
        writer.write(false, zos);
    }
}
