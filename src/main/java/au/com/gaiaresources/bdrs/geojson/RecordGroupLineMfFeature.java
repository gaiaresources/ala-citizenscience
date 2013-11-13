package au.com.gaiaresources.bdrs.geojson;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 2/10/13
 * Time: 7:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordGroupLineMfFeature extends MfFeature {

    private RecordGroup recordGroup;
    private ScrollableRecords scrollableRecords;
    private SpatialUtil spatialUtil;
    private Session session;

    private static final String ID_TEMPLATE = "group_%d";

    /**
     * RecordGroup is used to serialize information about the record group.
     * We also need a recordList to define the order of records when we
     * create a line. RecordGroup is not aware of the order of its associated
     * records.
     * @param recordGroup record group that we are serializing
     * @param scrollableRecords records to turn into a line
     * @param spatialUtil SpatialUtil object for doing geometry transformations
     *                    and defining output projection
     * @param session session associated with scrollableRecords. Used to clean up
     *                persistent objects as we are iterating
     */
    public RecordGroupLineMfFeature(RecordGroup recordGroup,
                                    ScrollableRecords scrollableRecords,
                                    SpatialUtil spatialUtil,
                                    Session session) {
        this.recordGroup = recordGroup;
        this.scrollableRecords = scrollableRecords;
        this.spatialUtil = spatialUtil;
        this.session = session;
    }

    @Override
    public String getFeatureId() {
        if (recordGroup.getId() == null) {
            return String.format(ID_TEMPLATE, 0);
        }
        return String.format(ID_TEMPLATE, recordGroup.getId());
    }

    @Override
    public MfGeometry getMfGeometry() {

        // Because we are making a static array in memory there's a chance
        // this can blow the heap. We have attempted to make it as safe as possible,
        // by making this object handle hibernate memory management,
        // however, due to time constraints I'm stopping the optimization here.
        // a.low 2013-10-16
        List<Coordinate> coordinateList = new ArrayList<Coordinate>();

        while (scrollableRecords.hasMoreElements()) {
            Record r = scrollableRecords.nextElement();
            if (r.getGeometry() != null) {
                coordinateList.add(r.getGeometry().getCoordinate());
            }
            session.clear();
        }

        GeometryFactory geomFactory = spatialUtil.getGeometryFactory();

        // we can only draw a line with 2 or more points.
        if (coordinateList.size() > 1) {
            Coordinate[] coordinateArray = coordinateList.toArray(new Coordinate[0]);
            LineString lineString = new LineString(new CoordinateArraySequence(coordinateArray),
                    geomFactory);

            return new MfGeometry(lineString);
        } else {
            // ignore this record group
            return null;
        }
    }

    @Override
    public void toJSON(JSONWriter jsonWriter) throws JSONException {
        // We don't need to serialize anything in RecordGroup
        // The feature ID has all the information we need for
        // subsequent requests to get detailed RecordGroup information
        // (You need to write the web service as it does not exist yet)
    }
}
