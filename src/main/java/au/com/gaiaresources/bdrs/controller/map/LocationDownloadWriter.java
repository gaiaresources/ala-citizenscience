package au.com.gaiaresources.bdrs.controller.map;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
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
 * Writes a set of {@link Location}s to a xls, shapefile, or kml file.
 * 
 * @author stephanie
 *
 */
public class LocationDownloadWriter extends AbstractDownloadWriter<Location> {
    
    @SuppressWarnings("UnusedDeclaration")
    private static Logger log = Logger.getLogger(LocationDownloadWriter.class);

    /**
     * Create a new LocationDownloadWriter
     * @param prefDAO PreferenceDAO
     * @param serverURL The serverURL is a combination of domain, tomcat context path and
     * portal context path.
     * e.g. http://core.gaiaresources.com.au/bdrs-core/portal/1
     * e.g. http://core.gaiaresources.com.au/bdrs-core/erwa
     */
    public LocationDownloadWriter(PreferenceDAO prefDAO, String serverURL) {
        super(prefDAO, serverURL);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.map.AbstractDownloadWriter#writeXLSRecords(au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService, java.io.OutputStream, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.db.ScrollableResults, org.hibernate.Session)
     */
    @Override
    protected void writeXLSRecords(BulkDataService bulkDataService,
            OutputStream out, Survey survey,
            ScrollableResults<Location> sc, Session sesh) throws Exception {
        bulkDataService.exportLocations(sesh, sc, out);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.map.AbstractDownloadWriter#writeSHPRecords(java.io.OutputStream, org.hibernate.Session, au.com.gaiaresources.bdrs.model.user.User, au.com.gaiaresources.bdrs.model.survey.Survey, au.com.gaiaresources.bdrs.db.ScrollableResults)
     */
    @Override
    protected void writeSHPRecords(OutputStream out, Session sesh,
            User accessingUser, Survey survey, ScrollableResults<Location> sc) throws Exception {
        // This is not a good thing
        // -----
        // Yes, I believe I just made it worse
        List<Location> locList = new ArrayList<Location>();

        while(sc.hasMoreElements()) {
            Location r = sc.nextElement();
            locList.add(r);
        }
        
        if (!locList.isEmpty()) {
            // no point writing a non empty shapefile since the user is not
            // expecting a template in this download but a populated shapefile
            ShapeFileWriter writer = new ShapeFileWriter(serverURL);
            
            // Currently the survey will ALWAYS be null - but if it's not in the future, use
            // the survey srid setting.
            int srid = survey != null ? survey.getMap().getSrid() : BdrsCoordReferenceSystem.DEFAULT_SRID;
            File zipFile = writer.exportLocations(locList, srid);
            
            sesh.clear();
            locList.clear();
            
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
    protected void writeKMLRecords(OutputStream out, Session sesh,
            User accessingUser, ScrollableResults<Location> sc)
            throws JAXBException {
        int recordCount = 0;
        List<Location> rList = new ArrayList<Location>(ScrollableResults.RESULTS_BATCH_SIZE);

        BDRSKMLWriter writer = new BDRSKMLWriter(preferenceDAO, serverURL, null);

        while (sc.hasMoreElements()) {
            rList.add(sc.nextElement());
            
            // evict to ensure garbage collection
            if (++recordCount % ScrollableResults.RESULTS_BATCH_SIZE == 0) {

                writer.writeLocations(rList);
                rList.clear();
                sesh.clear();
            }
        }
        // Flush the remainder out of the list.
        writer.writeLocations(rList);
        sesh.clear();
        writer.write(false, out);
    }
}
