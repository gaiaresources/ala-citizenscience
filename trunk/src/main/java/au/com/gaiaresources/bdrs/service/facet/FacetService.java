package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
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
import au.com.gaiaresources.bdrs.service.facet.builder.*;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.StringUtils;
import com.vividsolutions.jts.geom.Geometry;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernatespatial.GeometryUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Facet Service is the one stop shop for retrieving Facets. The FacetService
 * is responsible for instantiating Facets and ensuring that they have valid
 * Preferences.
 */
@Service
public class FacetService {

    /*
        Some SQL strings used for the facet query
     */
    public static final String SPECIES_ALIAS = "species";
    public static final String ATTRIBUTE_VALUE_SPECIES_ALIAS = "attributeValueSpecies";
    public static final String ATTRIBUTE_VALUE_ALIAS = AbstractFacet.ATTRIBUTE_VALUE_QUERY_ALIAS;
    public static final String BASE_FACET_QUERY = "select distinct record, "
            + SPECIES_ALIAS + ".scientificName, " + SPECIES_ALIAS
            + ".commonName, location.name, censusMethod.type from Record record";

    public static final String PARAM_SEARCH_TEXT = "searchText";

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
        temp.add(new WithinAreaFacetBuilder());

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
     *
     * @param user         the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @return the ordered {@link List} of {@link Facet}s.
     */
    public List<Facet> getFacetList(User user, Map<String, String[]> parameterMap) {
        return getFacetList(user, parameterMap, Record.class);
    }

    /**
     * Generates the {@link List} of {@link Facet}s for {@link Location}. Each facet will be configured
     * with the necessary {@link FacetOption}s and selection state.
     *
     * @param user         the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @return the ordered {@link List} of {@link Facet}s.
     */
    public List<Facet> getLocationFacetList(User user, Map<String, String[]> parameterMap) {
        return getFacetList(user, parameterMap, Location.class);
    }

    /**
     * Generates the {@link List} of {@link Facet}s. Each facet will be configured
     * with the necessary {@link FacetOption}s and selection state.
     *
     * @param user         the user requesting the record list.
     * @param parameterMap a mapping of query parameters.
     * @param facetClass   the Class for which to retrieve facets (Record or Location)
     * @return the ordered {@link List} of {@link Facet}s.
     */
    public List<Facet> getFacetList(User user, Map<String, String[]> parameterMap, Class facetClass) {
        List<Facet> facetList = new ArrayList<Facet>();

        if (FACET_BUILDER_CLASS_REGISTRY.containsKey(facetClass)) {
            FacetDAO dao = Record.class.equals(facetClass) ? recordDAO :
                    (Location.class.equals(facetClass) ? locationDAO : null);
            for (FacetBuilder builder : FACET_BUILDER_CLASS_REGISTRY.get(facetClass)) {
                Preference pref = prefDAO.getPreferenceByKey(builder.getPreferenceKey());
                if (pref == null || StringUtils.nullOrEmpty(pref.getValue())) {
                    log.error("Cannot create facet. Cannot find preference with key: " + builder.getPreferenceKey());
                } else {
                    try {
                        JSONArray configArray = JSONArray.fromString(pref.getValue());
                        for (int i = 0; i < configArray.size(); i++) {
                            try {
                                JSONObject configParams = configArray.getJSONObject(i);
                                if (configArray.size() > 1) {
                                    // don't unnecessarily add a prefix to the options
                                    configParams.put(Facet.JSON_PREFIX_KEY, String.valueOf(i) + "_");
                                }

                                Facet facet = builder.createFacet(dao, parameterMap, user, configParams, facetClass);
                                if (facet != null) {
                                    facetList.add(facet);
                                }
                            } catch (JSONException ex) {
                                log.error(String.format("The configuration parameter at index %d for preference key %s is not a JSON object or is improperly configured: %s", i, pref.getKey(), pref.getValue()), ex);
                            }
                        }
                    } catch (JSONException je) {
                        // Improperly configured JSON String.
                        log.error("Improperly configured JSON String for preference key: " + pref.getKey());
                    } catch (Exception e) {
                        log.error("Error while building Facet list. Probably a concurrency problem: " + pref.getValue(), e);
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
     *
     * @param facetList  - the list of facets to search.
     * @param facetClazz - the facet class to return
     * @return the facet if found, otherwise null
     */
    public <C extends Facet> C getFacetByType(List<Facet> facetList, Class<C> facetClazz) {
        for (Facet f : facetList) {
            if (f.getClass().equals(facetClazz)) {
                return (C) f;
            }
        }
        return null;
    }

    /**
     * Performs the creation of default preferences for each facet if one
     * does not already exist.
     *
     * @param sesh   the session used to get and/or create Preferences.
     * @param portal the portal to be associated with the created preferences.
     * @params category the category that contains facet preferences.
     */
    public void initFacetPreferences(Session sesh, Portal portal, PreferenceCategory category) {
        for (List<FacetBuilder> builderList : FACET_BUILDER_CLASS_REGISTRY.values()) {
            for (FacetBuilder builder : builderList) {
                Preference pref = prefDAO.getPreferenceByKey(sesh, builder.getPreferenceKey(), portal);
                if (pref == null) {
                    pref = builder.getDefaultPreference(portal, category);
                    prefDAO.save(sesh, pref);
                }
            }
        }
    }


    /**
     * Return the Records matching an http request containing Facets parameters.
     *
     * @param request the client request
     * @return matching records as a Scrollable result.
     */
    @SuppressWarnings("unchecked")
    public ScrollableResults<Record> getMatchingRecordsAsScrollable(HttpServletRequest request) {
        //defensive copy of the params.
        // Normally the request param map is unmodifiable but let's be safe.
        Map<String, String[]> params = new HashMap<String, String[]>(request.getParameterMap());
        List<Facet> facets = getFacetList(RequestContextHolder.getContext().getUser(), params);
        String searchText = getParameter(request.getParameterMap(), PARAM_SEARCH_TEXT);
        Query query = createFacetQuery(facets, searchText);
        return new ScrollableResultsImpl<Record>(query);
    }

    /**
     * Same as above but return all the records in a List.
     * Unlike the ScrollableResults, all the records are in memory which could be a problem.
     *
     * @param request the client request
     * @return matching records in a List
     */
    public List<Record> getMatchingRecords(HttpServletRequest request) {
        ScrollableResults<Record> sc = getMatchingRecordsAsScrollable(request);
        List<Record> result = new ArrayList<Record>();
        while (sc.hasMoreElements()) {
            result.add(sc.nextElement());
        }
        return result;
    }

    private String getParameter(Map<String, String[]> requestMap, String paramKey) {
        String[] values = requestMap.get(paramKey);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    private Query createFacetQuery(List<Facet> facetList, String searchText) {
        // extra columns in select are used for ordering
        HqlQuery hqlQuery = new HqlQuery(BASE_FACET_QUERY);
        applyFacetsToQuery(hqlQuery, facetList, searchText);
        return toHibernateQuery(hqlQuery);
    }

    private void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList, String searchText) {
        applyJoinsForBaseQuery(hqlQuery);
        // If we are doing a text search, add a few extra joins.
        if (searchText != null && !searchText.isEmpty()) {
            if (!hqlQuery.hasAlias(ATTRIBUTE_VALUE_ALIAS)) {
                hqlQuery.leftJoin("record.attributes", ATTRIBUTE_VALUE_ALIAS);
            }
            hqlQuery.leftJoin(ATTRIBUTE_VALUE_ALIAS + ".species", ATTRIBUTE_VALUE_SPECIES_ALIAS);
        }

        for (Facet f : facetList) {
            if (f.isActive()) {
                Predicate p = f.getPredicate();
                if (p != null) {
                    f.applyCustomJoins(hqlQuery);
                    hqlQuery.and(p);
                }
            }
        }

        if (searchText != null && !searchText.isEmpty()) {
            String formattedSearchText = String.format("%%%s%%", searchText);
            Predicate searchPredicate = Predicate.ilike("record.notes", formattedSearchText);
            searchPredicate.or(Predicate.ilike("record.user.name", String.format("%%%s%%", searchText)));
            searchPredicate.or(Predicate.ilike(SPECIES_ALIAS + ".scientificName", formattedSearchText));
            searchPredicate.or(Predicate.ilike(SPECIES_ALIAS + ".commonName", formattedSearchText));
            searchPredicate.or(Predicate.ilike(ATTRIBUTE_VALUE_SPECIES_ALIAS + ".scientificName", formattedSearchText));
            searchPredicate.or(Predicate.ilike(ATTRIBUTE_VALUE_SPECIES_ALIAS + ".commonName", formattedSearchText));

            hqlQuery.and(searchPredicate);
        }
    }

    /**
     * Applies the minimum of left joins required to do a facet query.
     */
    private static void applyJoinsForBaseQuery(HqlQuery hqlQuery) {
        hqlQuery.leftJoin("record.location", "location");
        hqlQuery.leftJoin("record.species", SPECIES_ALIAS);
        hqlQuery.leftJoin("record.censusMethod", "censusMethod");
    }

    private Query toHibernateQuery(HqlQuery hqlQuery) {
        Session sesh = RequestContextHolder.getContext().getHibernate();
        Query query = sesh.createQuery(hqlQuery.getQueryString());
        Object[] parameterValues = hqlQuery.getParametersValue();
        for (int i = 0; i < parameterValues.length; i++) {
            Object param = parameterValues[i];
            if (param instanceof Geometry) {
                Type type = new CustomType(GeometryUserType.class, null);
                query.setParameter(i, parameterValues[i], type);
            } else {
                query.setParameter(i, parameterValues[i]);
            }
        }
        return query;
    }

}
