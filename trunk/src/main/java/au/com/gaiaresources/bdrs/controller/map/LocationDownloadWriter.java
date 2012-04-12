package au.com.gaiaresources.bdrs.controller.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.kml.KMLWriter;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService;
import au.com.gaiaresources.bdrs.spatial.ShapeFileWriter;
import au.com.gaiaresources.bdrs.util.KMLUtils;

/**
 * Writes a set of {@link Location}s to a xls, shapefile, or kml file.
 * 
 * @author stephanie
 *
 */
public class LocationDownloadWriter extends AbstractDownloadWriter<Location> {
    
    private static Logger log = Logger.getLogger(LocationDownloadWriter.class);
    
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
            User accessingUser, Survey survey, ScrollableResults<Location> sc) throws Exception,
            FileNotFoundException, IOException {
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
            ShapeFileWriter writer = new ShapeFileWriter();
            File zipFile = writer.exportLocations(locList);
            
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
            String contextPath, User accessingUser, ScrollableResults<Location> sc)
            throws JAXBException {
        int recordCount = 0;
        List<Location> rList = new ArrayList<Location>(ScrollableResults.RESULTS_BATCH_SIZE);
        KMLWriter writer = KMLUtils.createKMLWriter(contextPath, null);
        while (sc.hasMoreElements()) {
            rList.add(sc.nextElement());
            
            // evict to ensure garbage collection
            if (++recordCount % ScrollableResults.RESULTS_BATCH_SIZE == 0) {
                
                KMLUtils.writeLocations(writer, contextPath, rList);
                rList.clear();
                sesh.clear();
            }
        }
        // Flush the remainder out of the list.
        KMLUtils.writeLocations(writer, contextPath, rList);
        sesh.clear();
        writer.write(false, out);
    }
}
