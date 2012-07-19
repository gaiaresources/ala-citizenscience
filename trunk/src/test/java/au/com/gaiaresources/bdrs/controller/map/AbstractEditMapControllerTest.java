package au.com.gaiaresources.bdrs.controller.map;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerSource;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerSource;
import au.com.gaiaresources.bdrs.security.Role;

public abstract class AbstractEditMapControllerTest extends AbstractControllerTest {
	
	@Autowired
    private GeoMapDAO geoMapDAO;
    @Autowired
    private GeoMapLayerDAO geoMapLayerDAO;
    
    private GeoMapLayer newLayer;
    private GeoMapLayer deletingLayer;
    private GeoMapLayer editingLayer;
    private GeoMapLayer neverAssignedLayer;
    
    private AssignedGeoMapLayer existingAssignedLayer;
    private AssignedGeoMapLayer editAssignedLayer;
    
    private Logger log = Logger.getLogger(getClass());
    
    private static final String TEST_URL = "http://testserverurl.com";
    
    @Before
    public void abstractEditMapControllerTestSetup() {
    	GeoMap geoMap = createMap();
        
        newLayer = new GeoMapLayer();
        newLayer.setName("test layer1");
        newLayer.setDescription("test layer desc1");
        newLayer.setLayerSource(GeoMapLayerSource.WMS_SERVER);
        newLayer.setServerUrl(TEST_URL);
        geoMapLayerDAO.save(newLayer);
        
        deletingLayer = new GeoMapLayer();
        deletingLayer.setName("test layer2");
        deletingLayer.setDescription("test layer desc2");
        deletingLayer.setLayerSource(GeoMapLayerSource.WMS_SERVER);
        deletingLayer.setServerUrl(TEST_URL);
        geoMapLayerDAO.save(deletingLayer);
        
        editingLayer = new GeoMapLayer();
        editingLayer.setName("test layer3");
        editingLayer.setDescription("test layer desc3");
        editingLayer.setLayerSource(GeoMapLayerSource.WMS_SERVER);
        editingLayer.setServerUrl(TEST_URL);
        geoMapLayerDAO.save(editingLayer);
        
        neverAssignedLayer = new GeoMapLayer();
        neverAssignedLayer.setName("test layer4");
        neverAssignedLayer.setDescription("test layer desc4");
        neverAssignedLayer.setLayerSource(GeoMapLayerSource.WMS_SERVER);
        neverAssignedLayer.setServerUrl(TEST_URL);
        geoMapLayerDAO.save(neverAssignedLayer);
        
        existingAssignedLayer = new AssignedGeoMapLayer();
        existingAssignedLayer.setLayer(deletingLayer);
        existingAssignedLayer.setMap(geoMap);
        geoMapLayerDAO.save(existingAssignedLayer);
        
        editAssignedLayer = new AssignedGeoMapLayer();
        editAssignedLayer.setLayer(editingLayer);
        editAssignedLayer.setMap(geoMap);
        geoMapLayerDAO.save(editAssignedLayer);
        
        getSession().flush();
        
        geoMapDAO.refresh(geoMap);
    }
    
	/**
     * Test that the map editing interface can be correctly opened.
     * @throws Exception
     */
    public ModelAndView testEditMap(String url) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(url);
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, AbstractEditMapController.EDIT_MAP_VIEW_NAME);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, AbstractEditMapController.MV_GEO_MAP);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, AbstractEditMapController.MV_BASE_LAYERS);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, AbstractEditMapController.MV_BDRS_LAYERS);
        
        List<AssignedGeoMapLayer> assignedLayers = (List<AssignedGeoMapLayer>)mv.getModel().get(AbstractEditMapController.MV_BDRS_LAYERS);
        Assert.assertEquals("wrong list size", 4, assignedLayers.size());
        AssignedGeoMapLayer l1 = getAssignedLayer(assignedLayers, newLayer);
        Assert.assertEquals("wrong selected state 1", false, l1.getShowOnMap());
        AssignedGeoMapLayer l2 = getAssignedLayer(assignedLayers, editingLayer);
        Assert.assertEquals("wrong selected state 2", true, l2.getShowOnMap());
        AssignedGeoMapLayer l3 = getAssignedLayer(assignedLayers, this.deletingLayer);
        Assert.assertEquals("wrong selected state 3", true, l3.getShowOnMap());
        AssignedGeoMapLayer l4 = getAssignedLayer(assignedLayers, this.neverAssignedLayer);
        Assert.assertEquals("wrong selected state 4", false, l4.getShowOnMap());
        
        return mv;
    }
    
    /**
     * Test that map settings can be saved.
     * @throws Exception
     */
    public ModelAndView testSaveMapSettings(String url) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("POST");
        request.setRequestURI(url);
        request.setParameter(AbstractEditMapController.PARAM_ZOOM_LEVEL, "10");
        request.setParameter(AbstractEditMapController.PARAM_MAP_CENTER, "POINT (140 -28)");
        request.setParameter(AbstractEditMapController.PARAM_WEIGHT_PREFIX+BaseMapLayerSource.G_HYBRID_MAP, "1");
        request.setParameter(AbstractEditMapController.PARAM_BASE_LAYER_SELECTED_PREFIX+BaseMapLayerSource.G_HYBRID_MAP, "true");
        request.setParameter(AbstractEditMapController.PARAM_DEFAULT_BASE_MAP, BaseMapLayerSource.G_HYBRID_MAP.toString());
        addGeoMapLayer(request, newLayer, true, 0, true);
        addGeoMapLayer(request, editingLayer, true, 1, false);
        addGeoMapLayer(request, deletingLayer, false, 2, false);
        addGeoMapLayer(request, neverAssignedLayer, false, 3, false);
        
        ModelAndView mv = handle(request, response);
        
        GeoMap geoMap = getMap();
        // because this is a test and we are changing the map object and
        // reading it in the same transaction, refresh to make sure the
        // one to many relationships are correctly populated.
        
        geoMapDAO.refresh(geoMap);
        
        // Assert the map settings have been saved properly.
        Assert.assertNotNull("center should not be null", geoMap.getCenter());
        Assert.assertEquals("wrong center latitude", new Double(-28), Double.valueOf(geoMap.getCenter().getY()));
        Assert.assertEquals("wrong center longitude", new Double(140), Double.valueOf(geoMap.getCenter().getX()));
        Assert.assertEquals("wrong zoom", 10, geoMap.getZoom().intValue());
        List<BaseMapLayer> baseLayers = geoMap.getBaseMapLayers();
        // there should be one base layer and it should be as above
        Assert.assertEquals(1, baseLayers.size());
        BaseMapLayer layer = baseLayers.get(0);
        Assert.assertEquals(BaseMapLayerSource.G_HYBRID_MAP, layer.getLayerSource());
        Assert.assertEquals(1, layer.getWeight().intValue());
        Assert.assertEquals(true, layer.isDefault());
        
        List<AssignedGeoMapLayer> assignedLayers = geoMap.getAssignedLayers();
        Assert.assertEquals(2, assignedLayers.size());
        AssignedGeoMapLayer l1 = getAssignedLayer(assignedLayers, newLayer);
        Assert.assertEquals("wrong layer id", newLayer.getId(), l1.getLayer().getId());
        Assert.assertEquals("wrong visible value", true, l1.isVisible());
        AssignedGeoMapLayer l2 = getAssignedLayer(assignedLayers, editingLayer);
        Assert.assertEquals("wrong layer id", editingLayer.getId(), l2.getLayer().getId());
        Assert.assertEquals("wrong visible value", false, l2.isVisible());
        
        return mv;
    }
    
    private void addGeoMapLayer(MockHttpServletRequest request, GeoMapLayer layer, boolean selected, int weight, boolean visible) {
        request.addParameter(AbstractEditMapController.PARAM_BDRS_LAYER_IDS, layer.getId().toString());
        if (selected) {
        	request.addParameter(AbstractEditMapController.PARAM_BDRS_LAYER_SELECTED_PREFIX + layer.getId().toString(), "on");	
        }
        if (visible) {
        	request.addParameter(AbstractEditMapController.PARAM_BDRS_LAYER_VISIBLE_PREFIX + layer.getId().toString(), "on");
        }
        request.addParameter(AbstractEditMapController.PARAM_WEIGHT_PREFIX + layer.getId().toString(), Integer.toString(weight));	
    }
    
    private AssignedGeoMapLayer getAssignedLayer(List<AssignedGeoMapLayer> list, GeoMapLayer layer) {
    	for (AssignedGeoMapLayer al : list) {
    		if (al.getLayer().equals(layer)) {
    			return al;
    		}
    	}
    	return null;
    }
    
    /**
     * Create a new GeoMap object. assign owner according to test needs.
     * Is run at the beginning of setup
     * 
     * @return GeoMap
     */
    protected abstract GeoMap createMap();
    
    /**
     * Get the GeoMap object. It should return an object representing the
     * same database row as createMap()
     * @return GeoMap
     */
    protected abstract GeoMap getMap();
}
