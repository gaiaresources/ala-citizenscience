package au.com.gaiaresources.bdrs.geojson;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeometry;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 2/10/13
 * Time: 7:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordGroupLineMfFeature extends MfFeature {


    @Override
    public String getFeatureId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MfGeometry getMfGeometry() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void toJSON(JSONWriter jsonWriter) throws JSONException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
