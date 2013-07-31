package au.com.gaiaresources.bdrs.util;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

public class SpatialUtil {

    private static final int DECIMAL_PLACES_TO_TRUNCATE_TO = 6;
    
    private Logger log = Logger.getLogger(getClass());
    
    private GeometryFactory geometryFactory;
    private WKTReader wktReader;
    
    private CoordinateReferenceSystem dstCrs;
    
    private BdrsCoordReferenceSystem bdrsCrs;

    /**
     * ctor
     * @param locationDAO LocationDAO.
     * @param srid SRID to init GeometryFactory.
     */
    public SpatialUtil(int srid) {
        geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
        wktReader = new WKTReader(geometryFactory);
        bdrsCrs = BdrsCoordReferenceSystem.getBySRID(srid);
        try {
			dstCrs = CRS.decode(BdrsCoordReferenceSystem.sridToEpsg(srid));
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException("Could not decode requested SRID to epsg code : " + srid, e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException("Could not decode requested SRID to epsg code : " + srid, e);
		}
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    public Point createPoint(BigDecimal locationLatitude,
            BigDecimal locationLongitude) {
        return this.createPoint(locationLatitude.doubleValue(), locationLongitude.doubleValue());
    }

    public Point createPoint(double latitude, double longitude) {
        return this.geometryFactory.createPoint(new Coordinate(truncate(longitude),
                truncate(latitude)));
    }

    /**
     * Used for truncating lat / lon decimal places.
     * 
     * @param x Double to truncate.
     * @return Truncated double.
     */
    public double truncate(double x) {
    	if (bdrsCrs != null) {
    		return MathUtil.truncate(x, bdrsCrs.getTruncateDecimalPlaces());
    	} else {
    		return MathUtil.truncate(x, DECIMAL_PLACES_TO_TRUNCATE_TO);
    	}
    }

    /**
     * We can support point, polygon (which gets turned into a multi polygon) and
     * multi polygon.
     * 
     */
    public Geometry createGeometryFromWKT(String wktString) {
        
        Geometry geom = null;
        try {
            geom = wktReader.read(wktString);
        } catch (Exception e) {
            log.error("Error occurred parsing WKT string:", e);
        }
        return convertToMultiGeom(geom);
    }
    
    public Geometry convertToMultiGeom(Geometry geom) {
        if (geom instanceof MultiPoint) {
            throw new IllegalArgumentException("wkt string is multi point which is not supported");
        }
        
        // convert to multi linestring!
        if (geom instanceof LineString) {
            LineString ls = (LineString)geom;
            geom = new MultiLineString(new LineString[] { ls }, geometryFactory);
        }
        
        // we can't store polygons, convert to multi polygon!
        if (geom instanceof Polygon) {
            Polygon poly = (Polygon)geom;
            geom = new MultiPolygon(new Polygon[] { poly }, geometryFactory);
        }
        return geom;
    }

	public Geometry transform(Geometry geom) {
		try {
			if (geom.getSRID() != geometryFactory.getSRID()) {
				CoordinateReferenceSystem srcCrs = CRS.decode(BdrsCoordReferenceSystem.sridToEpsg(geom.getSRID()));
				MathTransform transform = CRS.findMathTransform(srcCrs, dstCrs, false);
				Geometry g = JTS.transform(geom, transform);
				// update the SRID of the new geometry
				g.setSRID(geometryFactory.getSRID());
				return g;
			} else {
				return geom;
			}
		} catch (FactoryException fe) {
			log.error("CRS factory error", fe);
			return null;
		} catch (TransformException te) {
			log.error("Could not perform transformation", te);
			return null;
		}
	}
	
	/**
	 * Converts the geom to EWKT format which includes the SRID.
	 * EWKT is a postgis specific format but is handy for when we 
	 * need to srid information.
	 * @param geom Geometry to convert.
	 * @return EWKT string.
	 */
	public static String toEWkt(Geometry geom) {
		if (geom == null) {
			return null;
		}
		return String.format("SRID=%d;%s", geom.getSRID(), geom.toText());
	}
}
