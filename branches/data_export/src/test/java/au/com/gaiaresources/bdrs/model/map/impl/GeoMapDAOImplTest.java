package au.com.gaiaresources.bdrs.model.map.impl;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;

public class GeoMapDAOImplTest extends AbstractControllerTest {

    @Autowired
    GeoMapDAO geoMapDAO;
    
    GeoMap map1;
    GeoMap map2;
    GeoMap map3;
    
    @Before
    public void setup() {
        map1 = new GeoMap();
        map1.setAnonymousAccess(true);
        map1.setPublish(false);
        map1.setOwner(MapOwner.SURVEY);
        
        map2 = new GeoMap();
        map2.setAnonymousAccess(true);
        map2.setPublish(true);
        map2.setOwner(MapOwner.NONE);
        
        map3 = new GeoMap();
        map3.setAnonymousAccess(false);
        map3.setPublish(true);
        map3.setOwner(MapOwner.REVIEW);
        
        GeoMap map4 = new GeoMap();
        map4.setAnonymousAccess(true);
        map4.setPublish(true);
        map4.setOwner(MapOwner.SURVEY);
        
        geoMapDAO.save(map1);
        geoMapDAO.save(map2);
        geoMapDAO.save(map3);
    }
    
    @Test
    public void testAutowire() {
        Assert.assertNotNull(geoMapDAO);
    }
    
    /**
     * We only expect maps with MapOwner.NONE to be counted in this search.
     */
    @Test
    public void testAnonPublish() {
        PagedQueryResult<GeoMap> result = geoMapDAO.search(null, null, null, null, true, true);
        Assert.assertEquals(1, result.getCount());
        Assert.assertEquals(map2, result.getList().get(0));
    }
    
    @Test
    public void testGetByOwner() {
    	GeoMap m = geoMapDAO.getForOwner(null, MapOwner.REVIEW);
    	Assert.assertNotNull("can't be null", m);
    	Assert.assertEquals("wrong id", map3.getId(), m.getId());
    }
}
