package au.com.gaiaresources.bdrs.controller.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerSource;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.StringUtils;

import com.vividsolutions.jts.geom.Point;

public abstract class AbstractEditMapController extends AbstractController {
	
    /**
     * Query param for geo map layer ids that can be selected on the page.
     */
    public static final String PARAM_BDRS_LAYER_IDS = "bdrsLayer";
    /**
     * Query param prefix to denote base map layer selection.
     */
    public static final String PARAM_BASE_LAYER_SELECTED_PREFIX = "selected_";
    /**
     * Query param prefix to denote geo map layer selection.
     */
    public static final String PARAM_BDRS_LAYER_SELECTED_PREFIX = "bdrs_selected_";
    /**
     * Query param prefix to denote whether a map layer is visible on map load.
     */
    public static final String PARAM_BDRS_LAYER_VISIBLE_PREFIX = "bdrs_visible_";
    /**
     * Query param prefix to denote the list ordering of base layers or geo map layers.
     */
    public static final String PARAM_WEIGHT_PREFIX = "weight_";
    /**
     * Query param to denote the default base map.
     */
    public static final String PARAM_DEFAULT_BASE_MAP = "default";
    /**
     * Query param prefix to denote geo map layer id
     */
    public static final String PARAM_LAYER_ID_PREFIX = "id_";
    /**
     * Query param for map zoom level.
     */
    public static final String PARAM_ZOOM_LEVEL = "zoomLevel";
    /**
     * Query param for map center wkt string
     */
    public static final String PARAM_MAP_CENTER = "mapCenter";
    /**
     * Model and view key.
     */
    public static final String MV_BDRS_LAYERS = "bdrsLayers";
    /**
     * Model and view key.
     */
    public static final String MV_GEO_MAP = "geoMap";
    /**
     * Model and view key.
     */
    public static final String MV_WEB_MAP = "webMap";
    /**
     * Model and view key.
     */
    public static final String MV_BASE_LAYERS = "baseLayers";
    /**
     * View name for editing.
     */
    public static final String EDIT_MAP_VIEW_NAME = "surveyEditMap";
    
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private GeoMapLayerDAO geoMapLayerDAO;
    @Autowired
    private BaseMapLayerDAO baseMapLayerDAO;
    
	private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();
    
	/**
     * View for editing map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request HttpServletRequest
     * @param geoMap Map to edit.
     * @return ModelAndView for map editing.
     */
    public ModelAndView editMap(HttpServletRequest request, GeoMap geoMap) {
        
        ModelAndView mv = new ModelAndView(EDIT_MAP_VIEW_NAME);
        
        // get the base layers that have been set for the map
        List<BaseMapLayerSource> enumKeys = new ArrayList<BaseMapLayerSource>(BaseMapLayerSource.values().length);
        enumKeys.addAll((List<BaseMapLayerSource>) Arrays.asList(BaseMapLayerSource.values()));
        List<BaseMapLayer> assignedBaseLayers = geoMap.getBaseMapLayers();
        List<BaseMapLayer> baseLayers = new ArrayList<BaseMapLayer>(BaseMapLayerSource.values().length);
        BaseMapLayer defaultLayer = null;
        boolean showDefaults = assignedBaseLayers.isEmpty();
        for (BaseMapLayer baseMapLayer : assignedBaseLayers) {
            enumKeys.remove(baseMapLayer.getLayerSource());
            if (baseMapLayer.isDefault()) {
                defaultLayer = baseMapLayer;
            }
            baseMapLayer.setShowOnMap(true);
            baseLayers.add(baseMapLayer);
        }
        
        // create fake base layers for any remaining enum values
        for (BaseMapLayerSource baseMapLayerSource : enumKeys) {
            boolean isGoogleDefault = defaultLayer == null && BaseMapLayerSource.G_HYBRID_MAP.equals(baseMapLayerSource);
            boolean isDefault = defaultLayer != null && baseMapLayerSource.equals(defaultLayer.getLayerSource());
            // set the layer to visible if no values have been saved and it is a Google layer
            // or if there is no default and it is G_HYBRID_MAP (for the default)
            BaseMapLayer layer = new BaseMapLayer(geoMap, baseMapLayerSource, 
            		isGoogleDefault || isDefault, 
            		(showDefaults && BaseMapLayerSource.isGoogleLayerSource(baseMapLayerSource)) || isGoogleDefault || isDefault);
            baseLayers.add(layer);
        }
        
        // get the bdrs layers for the map
        List<GeoMapLayer> bdrsLayers = geoMapLayerDAO.getAllLayers();
        List<AssignedGeoMapLayer> selectedBdrsLayers = geoMap.getAssignedLayers();
        List<AssignedGeoMapLayer> allBdrsLayers = new ArrayList<AssignedGeoMapLayer>(bdrsLayers.size());
        for (GeoMapLayer geoMapLayer : bdrsLayers) {
        	AssignedGeoMapLayer thisLayer = getAssignedLayer(selectedBdrsLayers, geoMapLayer);
            if (thisLayer == null) {
            	// make a dummy layer. The dummy layer is not persisted.
            	thisLayer = new AssignedGeoMapLayer();
            	thisLayer.setLayer(geoMapLayer);
            	thisLayer.setShowOnMap(false);
            	thisLayer.setMap(geoMap);
            } else {
            	// the layer is selected. set the transient 'selected' property to true
            	thisLayer.setShowOnMap(true);
            }
            allBdrsLayers.add(thisLayer);
        }
        
        // sort the lists before adding to the view
        Collections.sort(baseLayers);
        Collections.sort(allBdrsLayers);
        
        mv.addObject("geoMap", geoMap);
        mv.addObject(MV_BASE_LAYERS, baseLayers);
        mv.addObject(MV_BDRS_LAYERS, allBdrsLayers);
        WebMap webMap = new WebMap(geoMap);
        mv.addObject(MV_WEB_MAP, webMap);
        return mv;
    }
    
    /**
     * Submit map settings.
     * @param request HttpServletRequest.
     * @param geoMap Map to update.
     */
    public void submitMap(HttpServletRequest request, GeoMap geoMap) {
        
        String centerWkt = request.getParameter(PARAM_MAP_CENTER);
        Point center = null;
        if (StringUtils.notEmpty(centerWkt)) {
        	center = (Point)spatialUtil.createGeometryFromWKT(centerWkt);
        }
        geoMap.setCenter(center);
        
        String zoomStr = request.getParameter(PARAM_ZOOM_LEVEL);
        Integer zoom = null;
        if (StringUtils.notEmpty(zoomStr)) {
        	try {
        		zoom = Integer.valueOf(zoomStr);
        	} catch (NumberFormatException nfe) {
                // Not readable, so don't set it.
        	}
        }
        
        geoMap.setZoom(zoom);
        
        String defaultLayerSource = request.getParameter("default");
        // save any changes to exisitng base layers and remove any that are no longer selected
        for (BaseMapLayerSource layerSource : BaseMapLayerSource.values()) {
            String layerId = request.getParameter(PARAM_LAYER_ID_PREFIX+layerSource);
            BaseMapLayer layer;
            if(layerId != null && !layerId.isEmpty()) {
                layer = baseMapLayerDAO.getBaseMapLayer(Integer.valueOf(layerId));
                if (layer == null) {
                    throw new NullPointerException("Null object returned for layer with id "+layerId);
                }
            } else {
                layer = new BaseMapLayer(geoMap, layerSource);
            }
            
            String selected = request.getParameter(PARAM_BASE_LAYER_SELECTED_PREFIX+layerSource);
            if (!StringUtils.nullOrEmpty(selected) || layerSource.toString().equals(defaultLayerSource)) {
                // the layer is selected, update the default setting
                layer.setDefault(layerSource.toString().equals(defaultLayerSource));
                layer.setWeight(Integer.valueOf(request.getParameter(PARAM_WEIGHT_PREFIX+layerSource)));
                baseMapLayerDAO.save(layer);
            } else {
                // the layer is not selected, delete it from the database
                if (layer.getId() != null) {
                    baseMapLayerDAO.delete(layer);
                }
            }
        }
        
        // save any bdrs layers that have been selected/deselected
        List<AssignedGeoMapLayer> assignedLayers = geoMap.getAssignedLayers();
        
        if(request.getParameterValues(PARAM_BDRS_LAYER_IDS) != null) {
            for(String layerId : request.getParameterValues(PARAM_BDRS_LAYER_IDS)) {
                if(layerId != null && !layerId.isEmpty()) {
                    GeoMapLayer layer = geoMapLayerDAO.get(Integer.valueOf(layerId));
                    if (layer != null) {
                        String selected = request.getParameter(PARAM_BDRS_LAYER_SELECTED_PREFIX+layerId);
                        String visible = request.getParameter(PARAM_BDRS_LAYER_VISIBLE_PREFIX+layerId);
                        AssignedGeoMapLayer thisLayer = getAssignedLayer(assignedLayers, layer);
                        if (thisLayer == null) {
                        	thisLayer = new AssignedGeoMapLayer();
                        	thisLayer.setLayer(layer);
                        	thisLayer.setMap(geoMap);
                        }
                        thisLayer.setVisible(!StringUtils.nullOrEmpty(visible));
                        if (!StringUtils.nullOrEmpty(selected)) {
                            // save the new layer or updated old one
                            int weight = Integer.valueOf(request.getParameter(PARAM_WEIGHT_PREFIX+layerId));
                            thisLayer.setWeight(weight);
                            geoMapLayerDAO.save(thisLayer);
                        } else {
                            // remove the layer if it exists
                            if (assignedLayers.contains(thisLayer)) {
                            	geoMapLayerDAO.delete(thisLayer);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get AssignedGeoMapLayer by the contained GeoMapLayer
     * 
     * @param list List of AssignedGeoMapLayers.
     * @param layer GeoMapLayer to search.
     * @return Matching AssignedGeoMapLayer or null if not found.
     */
    private AssignedGeoMapLayer getAssignedLayer(List<AssignedGeoMapLayer> list, GeoMapLayer layer) {
    	for (AssignedGeoMapLayer al : list) {
    		if (al.getLayer().equals(layer)) {
    			return al;
    		}
    	}
    	return null;
    }
}
