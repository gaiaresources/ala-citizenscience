package au.com.gaiaresources.bdrs.model.region.impl;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.MultiPolygon;

import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.region.RegionDAO;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

public class RegionDAOImplTest extends AbstractTransactionalTest {

	@Autowired
	private RegionDAO regionDAO;
	
	private Region r1;
	private Region r2;
	
	private SpatialUtilFactory suFactory = new SpatialUtilFactory();
	private SpatialUtil wgsUtil = suFactory.getLocationUtil(4326);
	
	@Before
	public void setup() {
		GeometryBuilder gb = new GeometryBuilder(4326);
		
		r1 = new Region();
		r1.setRegionName("r1");
		r1.setBoundary((MultiPolygon)wgsUtil.convertToMultiGeom(gb.createRectangle(0, 0, 2, 2)));
		regionDAO.save(r1);
		
		r2 = new Region();
		r2.setRegionName("r2");
		r2.setBoundary((MultiPolygon)wgsUtil.convertToMultiGeom(gb.createRectangle(2, 2, 2, 2)));
		regionDAO.save(r2);
	}
	
	@Test
	public void testFindRegions() {
		List<Region> result = regionDAO.findRegions(wgsUtil.createPoint(1, 1));
		Assert.assertEquals("wrong list size", 1, result.size());
	}
}
