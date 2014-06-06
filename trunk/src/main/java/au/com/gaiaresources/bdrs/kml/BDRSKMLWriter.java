package au.com.gaiaresources.bdrs.kml;

import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Writes KML for BDRS records and locations
 * Replacement for KMLUtils - this has better encapsulation
 */
public class BDRSKMLWriter {

    private Logger log = Logger.getLogger(getClass());

    public static final String GET_RECORD_PLACEMARK_PNG_URL = "/bdrs/public/map/icon/record_placemark.png.htm";
    public static final String KML_RECORD_FOLDER = "Record";
    public static final String KML_LOCATION_FOLDER = "Location";
    public static final String KML_POINT_ICON_ID = "pointIcon";
    public static final String KML_POLYGON_STYLE = "polygonStyle";
    public static final String KML_POINT_ICON_ID_HIGHLIGHT = "pointIconHighlight";
    public static final String KML_POLYGON_STYLE_HIGHLIGHT = "polygonStyleHighlight";
    public static final String DEFAULT_PLACEMARK_COLOR = "EE9900";
    public static final String HIGHLIGHT_PLACEMARK_COLOR = "2500FF";

    public static final String KML_CONTENT_TYPE = "application/vnd.google-earth.kml+xml";

    private KMLWriter writer;
    private JsonService jsonService;

    /**
     * Create a new BDRSKMLWriter
     * @param preferenceDAO PreferenceDAO
     * @param serverURL The serverURL is a combination of domain, tomcat context path and
     * portal context path.
     * e.g. http://core.gaiaresources.com.au/bdrs-core/portal/1
     * e.g. http://core.gaiaresources.com.au/bdrs-core/erwa
     * @param placemarkColorHex colorhex of the placemark
     * @throws JAXBException
     */
    public BDRSKMLWriter(PreferenceDAO preferenceDAO, String serverURL,
                         String placemarkColorHex) throws JAXBException {

        jsonService = new JsonService(preferenceDAO, serverURL);

        writer = new KMLWriter();
        String placemark = serverURL + GET_RECORD_PLACEMARK_PNG_URL + "?color=";

        placemarkColorHex = placemarkColorHex == null ? DEFAULT_PLACEMARK_COLOR : placemarkColorHex;
        placemark = placemark + placemarkColorHex;

        writer.createStyleIcon(KML_POINT_ICON_ID, placemark, 16, 16);
        writer.createStylePoly(KML_POLYGON_STYLE, placemarkColorHex.toCharArray());

        // create a highlighted placemark
        String hlPlacemark = serverURL + GET_RECORD_PLACEMARK_PNG_URL + "?color=";

        hlPlacemark = hlPlacemark + HIGHLIGHT_PLACEMARK_COLOR;

        writer.createStyleIcon(KML_POINT_ICON_ID_HIGHLIGHT, hlPlacemark, 16, 16);
        writer.createStylePoly(KML_POLYGON_STYLE_HIGHLIGHT, HIGHLIGHT_PLACEMARK_COLOR.toCharArray());

        writer.createFolder(KML_RECORD_FOLDER);
        writer.createFolder(KML_LOCATION_FOLDER);
    }

    /**
     * Write records to the internal KML writer. Does not write to output stream until we call
     * BDRSKMLWriter.write()
     * @param currentUser The logged in user
     * @param recordList List of records to write
     * @param serializeLazyLoadedValues If true will lazy load all children and write them
     */
    public void writeRecords(User currentUser, List<Record> recordList, boolean serializeLazyLoadedValues) {

        String label;
        String description;

        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();

        for(Record record : recordList) {
            label = String.format("Record #%d", record.getId());
            AccessControlledRecordAdapter recAdapter = new AccessControlledRecordAdapter(record, currentUser);
            description = jsonService.toJson(recAdapter, spatialUtilFactory, serializeLazyLoadedValues).toString();

            Geometry geom = record.getGeometry();
            if (geom != null) {
                writePlacemark(KML_RECORD_FOLDER, label, description, String.valueOf(record.getId()), geom);
            } else if(record.getLocation() != null && record.getLocation().getLocation() != null) {
                writePlacemark(KML_RECORD_FOLDER, label, description, String.valueOf(record.getId()), record.getLocation().getLocation());
            } else {
                log.info("Cannot find coordinate for record");
            }
        }
    }

    /**
     * Write records to KML to specified output stream
     *
     * @param currentUser The logged in user
     * @param recordList List of records to write
     * @param outputStream Write to this output stream
     * @param serializeAttributes If true will lazy load all children and write them
     * @throws JAXBException
     */
    public void writeRecordsToKML(User currentUser, List<Record> recordList, OutputStream outputStream,
                                         boolean serializeAttributes) throws JAXBException {
        writeRecords(currentUser, recordList, serializeAttributes);
        writer.write(false, outputStream);
    }

    private void writePlacemark(String folderName, String label, String description, String id, Geometry geom) {
        if (geom instanceof Point) {
            writer.createPlacemark(folderName, label, description, id, geom, KML_POINT_ICON_ID);
        } else if (geom instanceof MultiPolygon) {
            writer.createPlacemark(folderName, label, description, id, geom, KML_POLYGON_STYLE);
        } else if (geom instanceof MultiLineString) {
            writer.createPlacemark(folderName, label, description, id, geom, KML_POLYGON_STYLE);
        } else {
            log.error("Geometry type not supported : " + geom.getClass().getName());
        }
    }

    /**
     * Write locations to KML. Will not write to output stream until BDRSKMLWrite.write() is called
     *
     * @param locationList List of locations to write
     */
    public void writeLocations(List<Location> locationList) {
        String label;
        String description;

        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();

        for(Location location : locationList) {
            label = String.format("Location #%d", location.getId());
            description = jsonService.toJson(location, spatialUtilFactory).toString();

            Geometry geom = location.getLocation();
            if (geom != null) {
                writePlacemark(KML_LOCATION_FOLDER, label, description, String.valueOf(location.getId()), geom);
            } else {
                log.info("Cannot find coordinate for location");
            }
        }
    }

    /**
     * Write KML to the specified stream
     * @param formatted Should the output be formatted?
     * @param outputStream Write to this output stream
     */
    public void write(boolean formatted, OutputStream outputStream) throws JAXBException {
        writer.write(formatted, outputStream);
    }

    /**
     * Write KML to the HTTP response
     * @param response Write to the output stream contained in this HttpServletResponse
     * @throws IOException
     * @throws JAXBException
     */
    public void write(HttpServletResponse response) throws IOException, JAXBException {
        response.setContentType(KML_CONTENT_TYPE);
        write(false, response.getOutputStream());
    }
}
