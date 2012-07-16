package au.com.gaiaresources.bdrs.service.map;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerSource;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;

/**
 * A service that provides access to commonly used map retrieval code.
 * @author stephanie
 */
@Service
public class GeoMapService {

    @Autowired
    private GeoMapDAO geoMapDAO;
    @Autowired
    private BaseMapLayerDAO baseMapLayerDAO;
    
    private Logger log = Logger.getLogger(getClass());
    
    /**
     * Gets all available maps for the user.
     * @param user The user requesting maps to view
     * @return A list of maps that the user can access
     */
    public List<GeoMap> getAvailableMaps(User user) {
        Boolean anonAccess = user == null ? true : null;
        
        PagedQueryResult<GeoMap> queryResult = geoMapDAO.search(null, null, null, null, anonAccess, true);
        
        return queryResult.getList();
    }
    
    /**
     * Gets the GeoMap for a survey. Handles lazy initialisation of the GeoMap.
     * @param survey Survey that owns the map.
     * @return Map for the survey.
     */
    public GeoMap getForSurvey(Survey survey) {
    	GeoMap geoMap = geoMapDAO.getForSurvey(null, survey);
    	if (geoMap == null) {
    		geoMap = createForSurvey(survey);
    	}
        return geoMap;
    }
    
    /**
	 * Gets the GeoMap for a survey. Handles lazy initialisation of the GeoMap.
     * @param survey Survey that owns the map.
     * @param sesh Hibernate session.
     * @param survey survey Survey that owns the map.
     * @return Map for the survey.
     */
    public GeoMap getForSurvey(Session sesh, Survey survey) {
    	if (sesh == null) {
    		throw new IllegalArgumentException("Session cannot be null");
    	}
    	GeoMap geoMap = geoMapDAO.getForSurvey(sesh, survey);
    	if (geoMap == null) {
    		geoMap = createForSurvey(sesh, survey);
    	}
        return geoMap;
    }
    
    /**
     * Gets the map settings for the review pages.
     * I.e. my sightings, advanced review and location review.
     * @return Map for the review pages.
     */
    public GeoMap getForReview() {
    	GeoMap map = geoMapDAO.getForOwner(null, MapOwner.REVIEW);
    	Session sesh = geoMapDAO.getSessionFactory().getCurrentSession();
    	if (map == null) {
    		map = new GeoMap();
    		map.setOwner(MapOwner.REVIEW);
    		geoMapDAO.save(map);
    		createDefaultBaseLayers(sesh, map);
    		// flush before refresh
    		sesh.flush();
            // refresh one to many relationships.
    		geoMapDAO.refresh(map);
    	}
    	return map;
    }
    
    /**
     * Create a new GeoMap for the given survey.
     * @param survey Survey that owns the map.
     * @return Map for the survey.
     */
    private GeoMap createForSurvey(Survey survey) {
    	Session sesh = geoMapDAO.getSessionFactory().getCurrentSession();
    	return createForSurvey(sesh, survey);
    }
    
    private GeoMap createForSurvey(Session sesh, Survey survey) {
    	
    	GeoMap geoMap = new GeoMap();
		geoMap.setOwner(MapOwner.SURVEY);
        geoMap.setSurvey(survey);
        geoMapDAO.save(sesh, geoMap);
        createDefaultBaseLayers(sesh, geoMap);
        // flush before refresh
        sesh.flush();
        // refresh one to many relationships.
        geoMapDAO.refresh(sesh, geoMap);
        // set the map to the survey - has the same effect as refreshing.
        survey.setMap(geoMap);
        return geoMap;
    }
    
    /**
	 * Create the base default layers for a map.
     * @param geoMap Map that owns the default layers.
     */
    private void createDefaultBaseLayers(Session sesh, GeoMap geoMap) {
        // create the default base layers
        for (BaseMapLayerSource baseMapLayerSource : BaseMapLayerSource.values()) {
            boolean isGoogleDefault = BaseMapLayerSource.G_HYBRID_MAP.equals(baseMapLayerSource);
            // set the layer to visible if no values have been saved and it is a Google layer
            // or if there is no default and it is G_HYBRID_MAP (for the default)
            BaseMapLayer layer = new BaseMapLayer(geoMap, baseMapLayerSource, isGoogleDefault, BaseMapLayerSource.isGoogleLayerSource(baseMapLayerSource) || isGoogleDefault);
            layer = baseMapLayerDAO.save(sesh, layer);
        }
    }
}
