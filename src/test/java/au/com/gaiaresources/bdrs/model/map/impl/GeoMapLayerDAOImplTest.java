package au.com.gaiaresources.bdrs.model.map.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerSource;
import au.com.gaiaresources.bdrs.model.map.MapOwner;

public class GeoMapLayerDAOImplTest extends AbstractControllerTest {

    @Autowired
    GeoMapLayerDAO layerDAO;
    @Autowired
    GeoMapDAO mapDAO;
    
    GeoMapLayer layer1;
    GeoMapLayer layer2;
    GeoMapLayer layer3;
    
    GeoMap map1;
    
    @Before
    public void setup() throws Exception {
        layer1 = new GeoMapLayer();
        layer1.setName("aaaa");
        layer1.setDescription("zzzz");
        layer1.setLayerSource(GeoMapLayerSource.KML);
        
        layer2 = new GeoMapLayer();
        layer2.setName("bbbb");
        layer2.setDescription("yyyy");
        layer2.setLayerSource(GeoMapLayerSource.KML);
        
        layer3 = new GeoMapLayer();
        layer3.setName("cccc");
        layer3.setDescription("xxxx");
        layer3.setLayerSource(GeoMapLayerSource.KML);
        
        layerDAO.save(layer1);
        layerDAO.save(layer2);
        layerDAO.save(layer3);
        
        map1 = new GeoMap();
        map1.setOwner(MapOwner.NONE);
        mapDAO.save(map1);
        
        List<AssignedGeoMapLayer> layerList = new ArrayList<AssignedGeoMapLayer>();
        layerList.add(createTestAssignedLayer(map1, layer1));
        layerList.add(createTestAssignedLayer(map1, layer2));
        layerDAO.save(layerList);
    }

    @Test
    public void testAutowire() {
        Assert.assertNotNull(layerDAO);
    }
    
    @Test
    public void testSearch() {
        PagedQueryResult<GeoMapLayer> result = layerDAO.search(null, "aa", "zz");
        Assert.assertEquals(1, result.getCount());
        Assert.assertEquals(layer1, result.getList().get(0));
    }
    
    @Test
    public void testGetAssignedLayerForLayer() {
        List<AssignedGeoMapLayer> assignedLayerList = layerDAO.getAssignedLayerByLayerId(layer1.getId().intValue());
        Assert.assertEquals(1, assignedLayerList.size());
        Assert.assertEquals(layer1.getId(), assignedLayerList.get(0).getLayer().getId());
        
        List<AssignedGeoMapLayer> assignedLayerList2 = layerDAO.getAssignedLayerByLayerId(layer2.getId().intValue());
        Assert.assertEquals(1, assignedLayerList2.size());
        Assert.assertEquals(layer2.getId(), assignedLayerList2.get(0).getLayer().getId());
    }
    
    private AssignedGeoMapLayer createTestAssignedLayer(GeoMap map, GeoMapLayer layer) {
        AssignedGeoMapLayer asLayer = new AssignedGeoMapLayer();
        asLayer.setLayer(layer);
        asLayer.setMap(map);
        return asLayer;
    }
}
