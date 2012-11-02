/**
 * 
 */
package au.com.gaiaresources.bdrs.model.portal;

import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.index.IndexSchedule;
import au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO;
import au.com.gaiaresources.bdrs.model.index.IndexTask;
import au.com.gaiaresources.bdrs.model.index.IndexType;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.impl.TaxaServiceImpl;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;
import au.com.gaiaresources.bdrs.model.user.RegistrationService;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.FacetService;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.ModerationUtil;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author stephanie
 *
 */
public class PortalUtil {

    private static Logger log = Logger.getLogger(PortalUtil.class);
    
    public static final String DEFAULT_PREFERENCES = "preferences.json";

    /** The preferences.json file is UTF-8 encoded, as is the maven resources plugin configuration */
    public static final String PREFERENCES_CHARACTER_ENCODING = "UTF-8";
    
    protected PortalUtil() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Does root portal specific data initialisation. Note this does not do the
     * default theme initialisation for a portal. Separated so during testing
     * we don't need to create a theme
     * 
     * @param sesh
     * @param p
     * @throws IOException
     */
    public static void initPortalData(Session sesh, Portal p) throws IOException {
        
        if (sesh == null) {
            throw new IllegalArgumentException("Session, sesh, cannot be null");
        }
        if (p == null) {
            throw new IllegalArgumentException("Portal, p, cannot be null");
        }
        
        RegistrationService registrationService = AppContext.getBean(RegistrationService.class);
        User u = registrationService.signUp("admin", "admin@gaiabdrs.com", "admin", "admin", "password", null, Role.ADMIN, true);
        u.setPortal(p);
        initPortalPreferences(sesh, p, false);
        initModerationThreshold(sesh, p);
        initTaxa(sesh, p);
    }
    
    public static void initTaxa(Session sesh, Portal p) {
        TaxonGroup fieldGroup = TaxaServiceImpl.createFieldNameTaxonGroup();
        fieldGroup.setPortal(p);
        IndicatorSpecies fieldNameSpecies = TaxaServiceImpl.createFieldSpecies(fieldGroup);
        fieldNameSpecies.setPortal(p);
        Attribute fieldNameAttr = TaxaServiceImpl.createFieldNameAttr(fieldGroup);
        fieldNameAttr.setPortal(p);
        sesh.save(fieldGroup);
        sesh.save(fieldNameSpecies);
        sesh.save(fieldNameAttr);
    }

    /**
     * Initialise the portal preferences. The preferences are read from the preferences.json file in the resources.
     * The description and display name for preference categories are always overwritten with the contents
     * from preferences.json
     * Preference descriptions for a given key, for locked preferences only, are always overwritten from the
     * preferences.json file.
     * 
     * @param sesh
     * @param portal
     * @param lazyInit
     * @throws IOException
     */
    public static void initPortalPreferences(Session sesh, Portal portal, boolean lazyInit) throws IOException {
        if (sesh == null) {
            throw new IllegalArgumentException("Session, sesh, cannot be null");
        }
        if (portal == null) {
            throw new IllegalArgumentException("Portal, portal, cannot be null");
        }
        
        JSONArray jsonArray = getPreferenceJson(DEFAULT_PREFERENCES);

        List<JSONObject> catJsonObjList = new ArrayList<JSONObject>(
                jsonArray.size());
        List<JSONObject> prefJsonObjList = new ArrayList<JSONObject>(
                jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String className = jsonObject.getString("classname");

            if (PreferenceCategory.class.getCanonicalName().equals(className)) {
                catJsonObjList.add(jsonObject);
            } else if (Preference.class.getCanonicalName().equals(className)) {
                prefJsonObjList.add(jsonObject);
            } else {
                if (!Portal.class.getCanonicalName().equals(className) &&
                        !PortalEntryPoint.class.getCanonicalName().equals(className)) {
                    log.warn(String.format("Unknown preference class \"%s\" found. Ignoring.", className));
                }
            }
        }
                
        Map<String, PreferenceCategory> catMap = new HashMap<String, PreferenceCategory>();
        for (JSONObject jsonObject : catJsonObjList) {
            PreferenceCategory cat = PortalUtil.createCategory(sesh, portal, jsonObject);
            catMap.put(cat.getName(), cat);
        }

        for (JSONObject jsonObject : prefJsonObjList) {
            createPreference(sesh, portal, catMap, jsonObject, lazyInit);
        }
        
        FacetService facetService = AppContext.getBean(FacetService.class);
        if(facetService != null) {
            facetService.initFacetPreferences(sesh, portal, catMap.get(FacetService.FACET_CATEGORY_NAME));
        }
    }

    /**
     * Helper for reading .json files with settings in them
     * 
     * @param filename
     * @return
     * @throws IOException
     */
    public static JSONArray getPreferenceJson(String filename) throws IOException {
        // Load the JSON data from file.
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                PortalInitialiser.class.getResourceAsStream(filename), Charset.forName(PREFERENCES_CHARACTER_ENCODING)));
        StringBuilder builder = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            builder.append(line);
        }
        reader.close();
        return JSONArray.fromString(builder.toString());
    }

    /**
     * helper for creating preference categories.
     * 
     * @param sesh
     * @param portal
     * @param jsonObject
     * @return
     */
    private static PreferenceCategory createCategory(Session sesh, Portal portal,
            JSONObject jsonObject) {
        PreferenceDAO prefDAO = AppContext.getBean(PreferenceDAO.class);
        
        String name = jsonObject.getString("name");
        String displayName = jsonObject.getString("displayName");
        String description = jsonObject.getString("description");

        // always overwrite the preference category with more up to date descriptions / display names
        PreferenceCategory existingPrefCat = prefDAO.getPreferenceCategoryByName(sesh, name, portal);
        if (existingPrefCat != null) {
            existingPrefCat.setRunThreshold(false);
            existingPrefCat.setDisplayName(displayName);
            existingPrefCat.setDescription(description);
            return prefDAO.update(sesh, existingPrefCat);
        }
        // else continue with initialisation as normal...
        
        PreferenceCategory cat = new PreferenceCategory();
        cat.setRunThreshold(false);
        cat.setName(name);
        cat.setPortal(portal);
        cat.setDisplayName(displayName);
        cat.setDescription(description);
        return prefDAO.save(sesh, cat);
    }

    /**
     * helper for creating preferences.
     * 
     * @param sesh
     * @param portal
     * @param catMap
     * @param jsonObject
     * @param lazyInit
     * @return
     */
    private static Preference createPreference(Session sesh, Portal portal,
            Map<String, PreferenceCategory> catMap, JSONObject jsonObject, boolean lazyInit) {
        PreferenceDAO prefDAO = AppContext.getBean(PreferenceDAO.class);
        String key = jsonObject.getString("key");
        String value = jsonObject.getString("value");
        boolean locked = jsonObject.getBoolean("locked");
        String description = jsonObject.getString("description");
        String prefCatName = jsonObject.getString("preferenceCategoryName");
        boolean isRequired = jsonObject.getBoolean("isRequired");
        int weight = jsonObject.optInt("weight");   // Defaults to 0 if not present.
        
        if (lazyInit) {
            Preference existingPref = prefDAO.getPreferenceByKey(sesh, key, portal);
            if (existingPref != null) {
                // locked field, update the description
                if (existingPref.isLocked()) {
                    existingPref.setDescription(description);
                    return prefDAO.update(sesh, existingPref);
                }
                return existingPref;
            }
            // else continue with initialisation as normal...
        }

        Preference pref = new Preference();
        pref.setRunThreshold(false);
        pref.setLocked(locked);
        pref.setPortal(portal);
        pref.setPreferenceCategory(catMap.get(prefCatName));
        pref.setKey(key);
        pref.setValue(value);
        pref.setDescription(description);
        pref.setIsRequired(isRequired);
        pref.setWeight(weight);
        return prefDAO.save(sesh, pref);
    }
    
    /**
     * Initializes the threshold for the moderation actions.  The moderation actions 
     * are automatic holding of records with moderation attributes and automatic 
     * emails about moderation of Records.
     * @param sesh 
     */
    public static void initModerationThreshold(Session sesh, Portal portal) {
        ThresholdDAO thresholdDAO = AppContext.getBean(ThresholdDAO.class);
        ModerationUtil modService = new ModerationUtil(thresholdDAO);
        List<Threshold> modTholds = thresholdDAO.getThresholdsByName(sesh, ModerationUtil.MODERATION_THRESHOLD_NAME, portal);
        if (modTholds == null || modTholds.isEmpty()) {
            modService.createModerationThreshold(sesh, portal);
        }
    }
    
    /**
     * Initialize the portal indexes.
     * @param sesh
     * @param portal
     */
    public static void initIndexes(Session sesh, Portal portal) {
        // get schedule from db
        IndexScheduleDAO indexScheduleDAO = AppContext.getBean(IndexScheduleDAO.class);
        List<IndexSchedule> schedules = indexScheduleDAO.getIndexSchedules(portal);
        SearchService service = AppContext.getBean(SearchService.class);
        
        if (schedules != null) {
            TaskScheduler scheduler = AppContext.getBean(TaskScheduler.class);
            // must call initialize in order for scheduled tasks to work
            ((ThreadPoolTaskScheduler)scheduler).initialize();
            for (IndexSchedule indexSchedule : schedules) {
                IndexTask task = new IndexTask(sesh.getSessionFactory(), service, indexScheduleDAO, indexSchedule.getId());
                
                if (IndexType.SERVER_STARTUP.equals(indexSchedule.getType())) {
                    // schedule the task to run now
                    scheduler.schedule(task, new Date());
                } else if (!(IndexType.ONCE.equals(indexSchedule.getType()) && indexSchedule.getDate().before(new Date()))) {
                    // otherwise schedule indexes accordingly as long as it is 
                    // not a one time only task that was scheduled for sometime 
                    // before right now
                    if (!IndexType.ONCE.equals(indexSchedule.getType())) {
                        scheduler.scheduleAtFixedRate(task, indexSchedule.getDate(), indexSchedule.getPeriod());
                    } else if (indexSchedule.getLastRun() == null) {
                        // only schedule the ONCE index if it has not yet been run
                        scheduler.schedule(task, indexSchedule.getDate());
                    }
                }
            }
        }
        
    }
}
