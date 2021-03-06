package au.com.gaiaresources.bdrs.model.map;

import java.io.IOException;
import java.io.Writer;

import au.com.gaiaresources.bdrs.json.JSONEnum;
import au.com.gaiaresources.bdrs.json.JSONEnumUtil;
import au.com.gaiaresources.bdrs.model.record.Record;

/**
 * The <code>AttributeScope</code> defines the suggested extent where this
 * attribute is applicable. For example an attribute may have a scope of
 * {@link #SURVEY} indicating that the value for this attribute applies for all
 * records in a survey. Likewise a scope of {@link Record} indicates that
 * each record contains a different value for the attribute.
 */
public enum GeoMapLayerSource implements JSONEnum, MapLayerSource {
    KML,
    SHAPEFILE,
    SURVEY_KML,
    SURVEY_MAPSERVER,
    WMS_SERVER;
    
    public static GeoMapLayerSource fromString(String value) {
        if (GeoMapLayerSource.KML.toString().equals(value)) {
            return GeoMapLayerSource.KML;
        } else if (GeoMapLayerSource.SHAPEFILE.toString().equals(value)) {
            return GeoMapLayerSource.SHAPEFILE;
        } else if (GeoMapLayerSource.SURVEY_KML.toString().equals(value)) {
            return GeoMapLayerSource.SURVEY_KML;
        } else if (GeoMapLayerSource.SURVEY_MAPSERVER.toString().equals(value)) {
            return GeoMapLayerSource.SURVEY_MAPSERVER;
        } else if (GeoMapLayerSource.WMS_SERVER.toString().equals(value)) {
        	return GeoMapLayerSource.WMS_SERVER;
        } else {
            throw new IllegalArgumentException(value + " is not a valid String representation of GeoMapLayerSource");
        }
    }
    
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONEnumUtil.writeJSONString(out, this);
    }

    @Override
    public String toJSONString() {
        return JSONEnumUtil.toJSONString(this);
    }
}
