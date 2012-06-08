package au.com.gaiaresources.bdrs.controller.map;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.bulkdata.BulkDataService;
import au.com.gaiaresources.bdrs.util.KMLUtils;

/**
 * Abstract download writer class that contains the re-usable parts of writing files.
 * Used for writing both Record and Location files.
 */
public abstract class AbstractDownloadWriter<T> {
    
    private Logger log = Logger.getLogger(AbstractDownloadWriter.class);
    

    /**
     * Writes out the results using the specified format to the response. This
     * function primarily serves the use case where records exist in multiple
     * surveys or when the survey itself is irrelevant (e.g KML and Shapefile)
     * 
     * @param sesh the database session to retrieve the records.
     * @param request the browser request for encoded records.
     * @param response the server response to the browser.
     * @param sr the records to encode.
     * @param format the encoding format. Note that this function does not support XLS format. Use {@link #write(BulkDataService, RecordDownloadFormat, OutputStream, Session, String, Survey, User, ScrollableRecords)}
     * @param accessingUser the user requesting the encoding of records.
     * @throws Exception
     */
    public void write(Session sesh, HttpServletRequest request, HttpServletResponse response, ScrollableResults<T> sr, RecordDownloadFormat format, User accessingUser) throws Exception {
        
        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest, request, cannot be null");
        }
        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse, response, cannot be null");
        }
        if (sr == null) {
            throw new IllegalArgumentException("List<Record>, recordList, cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("RecordDownloadFormat, format, cannot be null");
        }
        
        switch (format) {
        case KML:
            response.setContentType(KMLUtils.KML_CONTENT_TYPE);
            response.setHeader("Content-Disposition", "attachment;filename=layer_"+System.currentTimeMillis()+".kml");
            break;
        case SHAPEFILE:
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=record_export_"+System.currentTimeMillis()+".zip");
            break;
        case XLS:
            throw new IllegalArgumentException("Cannot write XLS Records without a Survey.");
        default:
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            log.error("Records were requested to be downloaded in an invalid format : " + format);
            return;
        }
        
        try {
            write(null, format, response.getOutputStream(), sesh, request.getContextPath(), null, accessingUser, sr);
        } catch (IOException ioe) {
            // This may occur if the user has 
            // switched tabs before the kml has been provided.
        }
    }
    
    /**
     * Writes out the records using the specified format to the output stream.
     * 
     * @param bulkDataService only used for XLS encoding records. Can be null for other formats.
     * @param format the format to encode the records.
     * @param out the output stream where encoded records will be written.
     * @param sesh the database session to retrieve the records.
     * @param contextPath the context path of the web application.
     * @param survey the survey containing the records. Only used for XLS formay. Can be null for other formats.
     * @param accessingUser the user requesting the encoding of records.
     * @param sc the records to be encoded.
     * @throws Exception
     */
    public void write(BulkDataService bulkDataService, RecordDownloadFormat format, OutputStream out,
            Session sesh, String contextPath, Survey survey, User accessingUser,
            ScrollableResults<T> sc) throws Exception {
        
        switch (format) {
        case KML:
            writeKMLRecords(out, sesh, contextPath, accessingUser, sc);
            break;
        case SHAPEFILE:
            writeSHPRecords(out, sesh, accessingUser, survey, sc);
            break;
        case XLS:
            writeXLSRecords(bulkDataService, out, survey, sc, sesh);
            break;
        default:
            // Do nothing
            log.error("Unknown RecordDownloadFormat: "+format);
            break;
        }
    }
    
    /**
     * Writes the results to an xls file.
     * @param bulkDataService only used for XLS encoding records. Can be null for other formats.
     * @param out the output stream where encoded records will be written.
     * @param survey the survey containing the records. Only used for XLS formay. Can be null for other formats.
     * @param sc the records to be encoded.
     * @param sesh the database session to retrieve the records.
     */
    protected abstract void writeXLSRecords(BulkDataService bulkDataService,
            OutputStream out, Survey survey,
            ScrollableResults<T> sc, Session sesh) throws Exception;

    /**
     * Writes the results to a shapefile.
     * @param out the output stream where encoded records will be written.
     * @param sesh the database session to retrieve the records.
     * @param accessingUser the user requesting the encoding of records.
     * @param survey the survey containing the records. Only used for XLS formay. Can be null for other formats.
     * @param sc the records to be encoded.
     * @throws Exception
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected abstract void writeSHPRecords(OutputStream out, Session sesh,
            User accessingUser, Survey survey, ScrollableResults<T> sc) throws Exception,
            FileNotFoundException, IOException;

    /**
     * Writes the results to a kml file.
     * @param out the output stream where encoded records will be written.
     * @param sesh the database session to retrieve the records.
     * @param contextPath the context path of the web application.
     * @param accessingUser the user requesting the encoding of records.
     * @param sc the records to be encoded.
     * @throws JAXBException
     */
    protected abstract void writeKMLRecords(OutputStream out, Session sesh,
            String contextPath, User accessingUser, ScrollableResults<T> sc)
            throws JAXBException;
}
