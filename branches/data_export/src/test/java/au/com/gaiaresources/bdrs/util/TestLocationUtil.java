package au.com.gaiaresources.bdrs.util;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class TestLocationUtil {

	private Logger log = Logger.getLogger(getClass());
	
	@Test
	public void testTransform() {
		
		SpatialUtil wgsUtil = new SpatialUtilFactory().getLocationUtil(4326);
		SpatialUtil mgaUtil = new SpatialUtilFactory().getLocationUtil(28350);
		
		Point mgaPoint = mgaUtil.createPoint(7000000, 550000); 
		
		Geometry result = wgsUtil.transform(mgaPoint);

		Assert.assertNotNull("geom should not be null", result);
		Assert.assertEquals("wrong srid", 4326, result.getSRID());
		Assert.assertEquals("wrong longitude", 117.5, result.getCentroid().getX(), 0.1);
		Assert.assertEquals("wrong latitude", -27.1, result.getCentroid().getY(), 0.1);
	}
}
