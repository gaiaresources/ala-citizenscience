package au.com.gaiaresources.bdrs.service.facet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.builder.AttributeFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.CensusMethodTypeFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.FacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.LocationAttributeFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.LocationFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.ModerationFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.MonthFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.MultimediaFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.SurveyFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.TaxonGroupFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.UserFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.VisibilityFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.builder.YearFacetBuilder;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * The Facet Service is the one stop shop for retrieving Facets. The FacetService
 * is responsible for instantiating Facets and ensuring that they have valid
 * Preferences.
 */
@Service
public class FacetService {
    /**
     * The name of the {@link PreferenceCategory} where facet related preferences
     * shall be grouped.
     */
    public static final String FACET_CATEGORY_NAME = "category.facets";
    
    /**
     * An unmodifiable list of all {@link FacetBuilder}s that shall be used
     * in creating default preferences and instantiating new {@link Facet}s.
     */
    public static final List<FacetBuilder> FACET_BUILDER_REGISTRY;
    public static final Map<Class, List<FacetBuilder>> FACET_BUILDER_CLASS_REGISTRY;
    static {
        List<FacetBuilder> temp = new ArrayList<FacetBuilder>();
        
        temp.add(new UserFacetBuilder(Record.class));
        temp.add(new TaxonGroupFacetBuilder());
        temp.add(new MonthFacetBuilder());
        temp.add(new YearFacetBuilder());
        temp.add(new LocationFacetBuilder());
        temp.add(new SurveyFacetBuilder(Record.class));
        temp.add(new MultimediaFacetBuilder());
        temp.add(new CensusMethodTypeFacetBuilder());
        temp.add(new AttributeFacetBuilder());
        temp.add(new ModerationFacetBuilder());
        temp.add(new LocationAttributeFacetBuilder());
        temp.add(new VisibilityFacetBuilder());
        
        FACET_BUILDER_REGISTRY = Collections.unmodifiableList(temp);
        
        List<FacetBuilder> locationFacets = new ArrayList<FacetBuilder>();
        
        locationFacets.add(new UserFacetBuilder(Location.class));
        locationFacets.add(new SurveyFacetBuilder(Location.class));
        locationFacets.add(new LocationAttributeFacetBuilder());
        
        Map<Class, List<FacetBuilder>> tmpMap = new HashMap<Class, List<FacetBuilder>>(2);
        tmpMap.put(Record.class, temp);
        tmpMap.put(Location.class, locationFacets);
        FACET_BUILDER_CLASS_REGISTRY = Collections.unmodifiableMap(tmpMap);
    }
    
    private Logger log = Logger.getLogger(getClass());
    @Autowired
    private PreferenceDAO prefDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private LocationDAO locationDAO;
    /**
     * Generates the {@link List} of {@link Facet}s for {@link Record}. Each facet will be configured
     * with the necessary {@link FacetOption}s and selection state.
     * @param user the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @return the ordered {@link List} of {@link Facet}s. 
     */
    public List<Facet> getFacetList(User user, Map<String, String[]> parameterMap) {
        return getFacetList(user, parameterMap, Record.class);
    }
    /**
     * Generates the {@link List} of {@link Facet}s for {@link Location}. Each facet will be configured
     * with the necessary {@link FacetOption}s and selection state.
     * @param user the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @return the ordered {@link List} of {@link Facet}s. 
     */
    public List<Facet> getLocationFacetList(User user, Map<String, String[]> parameterMap) {
        return getFacetList(user, parameterMap, Location.class);
    }
    /**
     * Generates the {@link List} of {@link Facet}s. Each facet will be configured
     * with the necessary {@link FacetOption}s and selection state.
     * @param user the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @param facetClass the Class for which to retrieve facets (Record or Location)
     * @return the ordered {@link List} of {@link Facet}s. 
     */
    public List<Facet> getFacetList(User user, Map<String, String[]> parameterMap, Class facetClass) {
        List<Facet> facetList = new ArrayList<Facet>();
        
        if (FACET_BUILDER_CLASS_REGISTRY.containsKey(facetClass)) {
            FacetDAO dao = Record.class.equals(facetClass) ? recordDAO : 
                           (Location.class.equals(facetClass) ? locationDAO : null);
            for(FacetBuilder builder : FACET_BUILDER_CLASS_REGISTRY.get(facetClass)) {
                Preference pref = prefDAO.getPreferenceByKey(builder.getPreferenceKey());
                if(pref == null) {
                    log.error("Cannot create facet. Cannot find preference with key: "+builder.getPreferenceKey());
                } else {
                    try {
                        JSONArray configArray = JSONArray.fromString(pref.getValue());
                        for(int i=0; i<configArray.size(); i++) {
                            try {
                                JSONObject configParams = configArray.getJSONObject(i);
                                configParams.put(Facet.JSON_PREFIX_KEY, String.valueOf(i));
                                
                                Facet facet = builder.createFacet(dao, parameterMap, user, configParams, facetClass);
                                facetList.add(facet);
                            } catch(JSONException ex) {
                                log.error(String.format("The configuration parameter at index %d for preference key %s is not a JSON object or is improperly configured: %s", i, pref.getKey(), pref.getValue()), ex);
                            }
                        }
                    } catch(JSONException je) {
                        // Improperly configured JSON String.
                        log.error("Improperly configured JSON String for preference key: "+pref.getKey());
                    }
                } 
            }
            Collections.sort(facetList, new FacetWeightComparator());
        } else {
            log.warn("Requested facets for class not found in registry!");
        }
        return facetList;
    }
    
    /**
     * Gets a facet by type from the specified list of facets.
     * @param facetList - the list of facets to search.
     * @param facetClazz - the facet class to return
     * @return the facet if found, otherwise null
     */
    public <C extends Facet> C getFacetByType(List<Facet> facetList, Class<C> facetClazz) {
        for (Facet f : facetList) {
            if (f.getClass().equals(facetClazz)) {
                return (C)f;
            }
        }
        return null;
    }
    
    /**
     * Performs the creation of default preferences for each facet if one 
     * does not already exist.
     * @param sesh the session used to get and/or create Preferences.
     * @param portal the portal to be associated with the created preferences.
     * @params category the category that contains facet preferences.
     */
    public void initFacetPreferences(Session sesh, Portal portal, PreferenceCategory category) {
        for(List<FacetBuilder> builderList : FACET_BUILDER_CLASS_REGISTRY.values()) {
            for(FacetBuilder builder : builderList) {
                Preference pref = prefDAO.getPreferenceByKey(sesh, builder.getPreferenceKey(), portal);
                if(pref == null) {
                    pref = builder.getDefaultPreference(portal, category);
                    prefDAO.save(sesh, pref);
                }
            }
        }
    }
}
