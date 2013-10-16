package au.com.gaiaresources.bdrs.geojson;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 1/10/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordMinimalMfFeature extends MfFeature {

    private Record record;
    private SpatialUtil spatialUtil;

    public RecordMinimalMfFeature(Record r, SpatialUtil spatialUtil) {
        if (r == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        if (spatialUtil == null) {
            throw new IllegalArgumentException("Spatial util cannot be null");
        }
        this.record = r;
        this.spatialUtil = spatialUtil;
    }

    @Override
    public String getFeatureId() {
        if (record.getId() == null) {
            return "0";
        }
        return record.getId().toString();
    }

    @Override
    public MfGeometry getMfGeometry() {
        if (record.getGeometry() == null) {
            return null;
        }
        return new MfGeometry(spatialUtil.transform(record.getGeometry()));
    }

    @Override
    public void toJSON(JSONWriter jsonWriter) throws JSONException {
        // only writes the bare minimum
        jsonWriter.key("user_id");
        User u = record.getUser();
        jsonWriter.value(u != null ? record.getUser().getId() : 0);

        jsonWriter.key("survey_id");
        Survey survey = record.getSurvey();
        jsonWriter.value(survey != null ? record.getSurvey().getId() : 0);

        jsonWriter.key("species_id");
        IndicatorSpecies species = record.getSpecies();
        jsonWriter.value(species != null ? record.getSpecies().getId() : 0);

    }
}
