package au.com.gaiaresources.bdrs.controller.review;

import au.com.gaiaresources.bdrs.controller.map.WebMap;
import au.com.gaiaresources.bdrs.controller.review.locations.AdvancedReviewLocationsController;
import au.com.gaiaresources.bdrs.controller.review.sightings.SightingsController;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceUtil;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.FacetService;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.python.report.ReportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.StringUtils;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Sort;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.Search;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernatespatial.GeometryUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * An advanced review view has a group of facets and 3 views: table, map, download.
 *
 * @author stephanie
 */
public abstract class AdvancedReviewController<T> extends SightingsController {

    private Logger log = Logger.getLogger(getClass());

    public static final String VIEW_TYPE_TABLE = "table";
    public static final String VIEW_TYPE_MAP = "map";
    public static final String VIEW_TYPE_DOWNLOAD = "download";

    public static final String PARAM_VIEW_TYPE = "viewType";

    public static final String SORT_BY_QUERY_PARAM_NAME = "sortBy";
    public static final String SORT_ORDER_QUERY_PARAM_NAME = "sortOrder";
    public static final String SEARCH_QUERY_PARAM_NAME = "searchText";
    public static final String RESULTS_PER_PAGE_QUERY_PARAM_NAME = "resultsPerPage";
    public static final String PAGE_NUMBER_QUERY_PARAM_NAME = "pageNumber";
    public static final String DEFAULT_RESULTS_PER_PAGE = "20";
    public static final String DEFAULT_PAGE_NUMBER = "1";

    public static final String MODEL_DOWNLOAD_VIEW_SELECTED = "downloadViewSelected";
    public static final String MODEL_TABLE_VIEW_SELECTED = "tableViewSelected";
    public static final String MODEL_MAP_VIEW_SELECTED = "mapViewSelected";

    /**
     * ModelAndView model key.
     */
    public static final String MV_KEY_RECORD_COUNT = "recordCount";
    /**
     * Constants for request parameters
     */
    public static final String PARAM_LOCATION_AREA = "locationArea";
    public static final String PARAM_LOCATIONS = "locations";


    protected static final String ATTRIBUTE_KML_PARAMS = "KMLParameters";

    @Autowired
    protected FacetService facetService;

    /**
     * Used to retrieve the default search results view (map or list)
     */
    @Autowired
    protected PreferenceDAO preferenceDAO;

    /**
     * Used to retrieve the reports eligible for the advanced review page.
     */
    @Autowired
    protected ReportDAO reportDAO;

    @Autowired
    protected ReportService reportService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GeoMapDAO geoMapDAO;

    // defaults to using WGS84 / lonlat/ 4326 (they are all the same thing)
    private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    /**
     * Returns the view for the request
     *
     * @param newParamMap    the request for the view
     * @param surveyId       the id of the survey to get results for
     * @param resultsPerPage the number of results to display per page
     * @param pageNumber     the page number to start on
     * @param facetList      the list of facets by which to filter the results
     * @param recordCount    the number of results that will be returned by the query
     * @param viewName       the name of the view to return
     * @return a ModelAndView
     */
    protected ModelAndView getAdvancedReviewView(Map<String, String[]> newParamMap,
                                                 Integer surveyId, Integer resultsPerPage, Integer pageNumber, List<Facet> facetList, Long recordCount, String viewName) {
        long pageCount = countPages(resultsPerPage, recordCount);

        ModelAndView mv = new ModelAndView(viewName);

        mv.addObject(getViewType(getParameter(newParamMap, PARAM_VIEW_TYPE)), true);

        String sortBy = getParameter(newParamMap, SORT_BY_QUERY_PARAM_NAME);
        String sortOrder = getParameter(newParamMap, SORT_ORDER_QUERY_PARAM_NAME);

        mv.addObject(PARAM_LOCATIONS, getParameter(newParamMap, PARAM_LOCATIONS));

        mv.addObject("facetList", facetList);
        mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));

        // set sortBy or use default if none requested.
        mv.addObject("sortBy", sortBy != null ? sortBy : getDefaultSortString());
        // set sortOrder or use default if none requested.
        mv.addObject("sortOrder", sortOrder != null ? sortOrder : "DESC");

        mv.addObject("searchText", getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME));
        mv.addObject(MV_KEY_RECORD_COUNT, recordCount);
        mv.addObject("resultsPerPage", resultsPerPage);
        mv.addObject("pageCount", pageCount);
        mv.addObject("reportList", getReportList());

        // Avoid the situation where the number of results per page is increased
        // thereby leaving a page number higher than the total page count.
        mv.addObject("pageNumber", Math.min(pageCount, pageNumber.longValue()));

        GeoMap geoMap = geoMapDAO.getForOwner(null, MapOwner.REVIEW);
        mv.addObject(BdrsWebConstants.MV_WEB_MAP, geoMap != null ? new WebMap(geoMap) : null);

        User user = getRequestContext().getUser();
        // A limited user should not see the download tab
        mv.addObject("hideDownload", user != null ? user.isLimitedUser() : true);

        return mv;
    }

    protected long countPages(Integer resultsPerPage, Long recordCount) {
        long pageCount = recordCount / resultsPerPage;
        if ((recordCount % resultsPerPage) > 0) {
            pageCount += 1;
        }
        return pageCount;
    }

    /**
     * Returns a list of reports that are available to the view.
     *
     * @return
     */
    protected abstract List<Report> getReportList();

    /**
     * Returns the default sorting parameter for the view to be used for the initial sort.
     *
     * @return
     */
    protected abstract String getDefaultSortString();

    /**
     * Returns the list of results matching the {@link Facet} criteria as KML.
     */
    public void advancedReviewKMLSightings(HttpServletRequest request, HttpServletResponse response,
                                           List<Facet> facetList, ScrollableResults<T> sr, boolean serializeAttributes) throws IOException, JAXBException {

        BDRSKMLWriter writer = new BDRSKMLWriter(preferenceDAO,
                getRequestContext().getServerURL(), null);

        User currentUser = getRequestContext().getUser();
        Session sesh = getRequestContext().getHibernate();

        int recordCount = 0;
        List<T> rList = new ArrayList<T>(ScrollableRecords.RESULTS_BATCH_SIZE);
        while (sr.hasMoreElements()) {
            rList.add(sr.nextElement());

            // evict to ensure garbage collection
            if (++recordCount % ScrollableRecords.RESULTS_BATCH_SIZE == 0) {
                writeKMLResults(writer, currentUser, rList, serializeAttributes);
                rList.clear();
                sesh.clear();
            }
        }
        writeKMLResults(writer, currentUser, rList, serializeAttributes);

        response.setContentType(BdrsWebConstants.KML_CONTENT_TYPE);
        writer.write(false, response.getOutputStream());
    }

    /**
     * Returns the name of the root folder to use in the kml file.
     *
     * @return
     */
    protected abstract String getKMLFolderName();

    /**
     * Writes a list of results to KML.
     *
     * @param writer              the writer to use for writing
     * @param currentUser         the logged in user
     * @param rList               the list of results to write
     * @param serializeAttributes whether to serialize attributes as json which is embedded in the KML. Is slow
     *                            and can cause heap problems for large datasets
     */
    protected abstract void writeKMLResults(BDRSKMLWriter writer, User currentUser,
                                            List<T> rList, boolean serializeAttributes);

    /**
     * Turns the supplied PortalPersistentImpl into an Map containing it's properties.
     *
     * @param persistent the object to flatten.
     * @return a Map containing the PortalPersistentImpl property names as keys and property values as the values.
     */
    protected Map<String, Object> flatten(PortalPersistentImpl persistent) {
        return persistent.flatten(2);
    }

    /**
     * Configures the flush mode and installs an appropriate Record filter on the current hibernate session.
     */
    protected void configureHibernateSession() {
        // We are changing the flush mode here to prevent checking for dirty
        // objects in the session cache. Normally this is desireable so that
        // you will not receive stale objects however in this situation
        // the controller will only be performing reads and the objects cannot
        // be stale. We are explicitly setting the flush mode here because
        // we are potentially loading a lot of objects into the session cache
        // and continually checking if it is dirty is prohibitively expensive.
        // https://forum.hibernate.org/viewtopic.php?f=1&t=936174&view=next
        RequestContext requestContext = getRequestContext();
        requestContext.getHibernate().setFlushMode(FlushMode.MANUAL);
        User user = requestContext.getUser();

        // Enabling this filter users from seeing results not allowed by their current role.
        FilterManager.enableRecordFilter(requestContext.getHibernate(), user);
        FilterManager.enableLocationFilter(requestContext.getHibernate(), user);
    }

    /**
     * @return the authenticated user from the request context, or null if the request originated from an
     *         anonymous user.
     */
    protected User currentUser() {
        return getRequestContext().getUser();
    }

    /**
     * Returns the default view model to use if a view type has not been specified in the request.
     * The default is determined by the value of the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     *
     * @return the default view model name to use.
     */
    private String defaultView() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(preferenceDAO);
        boolean useMap = preferenceUtil.getBooleanPreference(Preference.ADVANCED_REVIEW_DEFAULT_VIEW_KEY);
        return useMap ? MODEL_MAP_VIEW_SELECTED : MODEL_TABLE_VIEW_SELECTED;
    }

    /**
     * Returns the model object that represents the view to display based on the supplied request parameter.
     *
     * @param viewTypeParameter the value of the request parameter used to request a particular view type.  May be null,
     *                          in which case the default is determined by the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     * @return the name of the view model object used by the page to select the correct view.
     */
    protected String getViewType(String viewTypeParameter) {
        if (VIEW_TYPE_DOWNLOAD.equals(viewTypeParameter)) {
            return MODEL_DOWNLOAD_VIEW_SELECTED;
        } else if (VIEW_TYPE_TABLE.equals(viewTypeParameter)) {
            return MODEL_TABLE_VIEW_SELECTED;
        } else if (VIEW_TYPE_MAP.equals(viewTypeParameter)) {
            return MODEL_MAP_VIEW_SELECTED;
        } else {
            return defaultView();
        }
    }

    /**
     * Creates a {@link Query} object based on the selections in the {@link Facet}s.
     *
     * @param facetList    The list of {@link Facet} to filter by
     * @param surveyId     surveyId (optional) surveyId to filter by
     * @param sortProperty the HQL property that should be used for sorting.
     *                     The sortProperty may be null if no sorting is necessary.
     * @param sortOrder    the sorting order
     * @param searchText   textual restriction to be applied to matching results
     * @return a {@link Query} representing the selections in the {@link Facet}s
     */
    protected abstract Query createFacetQuery(List<Facet> facetList, Integer surveyId, String sortProperty, String sortOrder, String searchText);

    /**
     * Converts the {@link HqlQuery} to a {@ Query} representation.
     */
    protected Query toHibernateQuery(HqlQuery hqlQuery) {
        Session sesh = getRequestContext().getHibernate();
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

    /**
     * Counts the number of values that match the facet selections, the surveyId, and the searchText
     *
     * @param facetList  The list of {@link Facet} on the screen
     * @param surveyId   (optional) surveyId to filter by
     * @param searchText (optional) value to search entries for
     * @return The number of values matching the facet selections, surveyId, and searchText
     */
    protected long countMatchingRecords(List<Facet> facetList, Integer surveyId, String searchText) {
        return countMatchingRecords(new HqlQuery(getCountSelect()), facetList, surveyId, searchText);
    }

    /**
     * Counts the number of values that match the supplied query, facet selections, the surveyId, and the searchText
     *
     * @param hqlQuery   The query to run to perform the count.
     * @param facetList  The list of {@link Facet} on the screen
     * @param surveyId   (optional) surveyId to filter by
     * @param searchText (optional) value to search entries for
     * @return The number of values matching the facet selections, surveyId, and searchText
     */
    protected long countMatchingRecords(HqlQuery hqlQuery, List<Facet> facetList, Integer surveyId, String searchText) {

        applyFacetsToQuery(hqlQuery, facetList, surveyId, searchText);

        Query query = toHibernateQuery(hqlQuery);
        Object result = query.uniqueResult();
        return Long.parseLong(result.toString());
    }

    /**
     * Returns a select statement that specifies how to count the results for the controller.
     */
    protected abstract String getCountSelect();

    /**
     * Adds the {@link Query} {@link au.com.gaiaresources.bdrs.db.impl.Predicate}s from the facets to the
     * {@link HqlQuery} parameter.
     *
     * @param hqlQuery   the query to apply the facet predicates to
     * @param facetList  the list of facets to apply to the query
     * @param surveyId   the surveyId to limit the results by
     * @param searchText textual restriction to be applied to matching results
     */
    protected abstract void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList,
                                               Integer surveyId, String searchText);


    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of {@link Record}.
     *
     * @param facetList    the {@link Facet}s providing the selection criteria.
     * @param surveyId     the primary key of the survey containing all eligible results.
     *                     The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting.
     *                     The sortProperty may be null if no sorting is necessary.
     * @param sortOrder    the sorting order
     * @param searchText   textual restriction to be applied to matching results.
     * @return the Query to select the matching results {@link Record}s.
     * @see SortOrder
     */
    protected Query getMatchingRecordsQuery(List<Facet> facetList,
                                            Integer surveyId,
                                            String sortProperty,
                                            String sortOrder,
                                            String searchText) {
        return createFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText);
    }


    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of results.
     *
     * @param facetList    the {@link Facet}s providing the selection criteria.
     * @param surveyId     the primary key of the survey containing all eligible results.
     *                     The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting.
     *                     The sortProperty may be null if no sorting is necessary.
     * @param sortOrder    the sorting order
     * @param searchText   textual restriction to be applied to matching results.
     * @return the matching results as a scrollable result set
     * @see SortOrder
     */
    protected ScrollableResults<T> getScrollableResults(List<Facet> facetList,
                                                        Integer surveyId,
                                                        String sortProperty,
                                                        String sortOrder,
                                                        String searchText) {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText);
        return new ScrollableResultsImpl<T>(query);
    }

    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of results.
     *
     * @param facetList      the {@link Facet}s providing the selection criteria.
     * @param surveyId       the primary key of the survey containing all eligible results.
     *                       The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty   the HQL property that should be used for sorting.
     *                       The sortProperty may be null if no sorting is necessary.
     * @param sortOrder      the sorting order
     * @param searchText     textual restriction to be applied to matching results.
     * @param pageNumber     the page number to retrieve results for
     * @param entriesPerPage the number of results to display per page
     * @return the matching results as a scrollable result set
     */
    protected ScrollableResults<T> getScrollableResults(List<Facet> facetList,
                                                        Integer surveyId,
                                                        String sortProperty,
                                                        String sortOrder,
                                                        String searchText,
                                                        int pageNumber, int entriesPerPage) {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText);
        return new ScrollableResultsImpl<T>(query, pageNumber, entriesPerPage);
    }


    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of {@link Record}.
     *
     * @param facetList    the {@link Facet}s providing the selection criteria.
     * @param surveyId     the primary key of the survey containing all eligible records.
     *                     The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting.
     *                     The sortProperty may be null if no sorting is necessary.
     * @param sortOrder    the sorting order
     * @param searchText   textual restriction to be applied to matching records.
     * @return the {@link List} of matching {@link Record}s.
     * @see SortOrder
     */
    public List<T> getMatchingRecordsAsList(List<Facet> facetList,
                                            Integer surveyId,
                                            String sortProperty,
                                            String sortOrder,
                                            String searchText,
                                            Integer resultsPerPage,
                                            Integer pageNumber) {


        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText);
        if (resultsPerPage != null && pageNumber != null && resultsPerPage > 0 && pageNumber > 0) {
            query.setFirstResult((pageNumber - 1) * resultsPerPage);
            query.setMaxResults(resultsPerPage);
        }

        List<Object[]> rowList = query.list();
        List<T> recordList = new ArrayList<T>(rowList.size());
        for (Object[] rowObj : rowList) {
            recordList.add((T) rowObj[0]);
        }

        return recordList;
    }

    /**
     * Converts the {@link HqlQuery} to a {@ Query} representation.
     *
     * @param locations
     */
    protected Query toHibernateQuery(HqlQuery hqlQuery, String locationArea, List<Location> locations) {
        Session sesh = getRequestContext().getHibernate();
        Query query = sesh.createQuery(hqlQuery.getQueryString());

        // the geometry comes first
        CustomType geometryType = new CustomType(GeometryUserType.class, null);
        int index = 0;
        if (!StringUtils.nullOrEmpty(locationArea)) {
            // geometry created will be in SRID = 4326 aka lonlats
            Geometry geometry = spatialUtil.createGeometryFromWKT(locationArea);
            query.setParameter(index++, geometry, geometryType);
        }

        Object[] parameterValues = hqlQuery.getParametersValue();
        for (int i = 0; i < parameterValues.length; i++) {
            query.setParameter(index++, parameterValues[i]);
        }

        if (locations != null && !locations.isEmpty()) {
            query.setParameterList("locs", locations);
        }
        return query;
    }

    /**
     * Retrieves the parameter with the name paramKey from the request map.
     *
     * @param requestMap the map containing the request parameters
     * @param paramKey   the name of the parameter to retrieve
     * @return the value of the parameter if it exists, null otherwise
     *         if there are multiple parameter values, returns the first
     */
    protected String getParameter(Map<String, String[]> requestMap,
                                  String paramKey) {
        String[] values = requestMap.get(paramKey);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    /**
     * Retrieves multiple parameter values for the parameter with the given name from the request map
     *
     * @param requestMap the map to get the values from
     * @param paramKey   the key to get values for
     * @return an array of values from the requestMap, null if no value for paramKey exists
     */
    protected String[] getParameterValues(HashMap<String, String[]> requestMap,
                                          String paramKey) {
        String[] values = requestMap.get(paramKey);
        if (values != null && values.length > 0) {
            return values;
        }
        return null;
    }

    /**
     * Decodes the parameters in the request mapping into the parameters expected by the controller.
     * This splits each array of length one into an array of it's comma separated values and
     * URL decodes it.  The URL encoding/decoding is done to ensure that commas in the
     * parameter values remain intact.
     *
     * @param parameterMap the request parameter map
     * @return a Map that is decoded as described
     */
    protected Map<String, String[]> decodeParamMap(Map<String, String[]> parameterMap) {
        Map<String, String[]> newParamMap = new HashMap<String, String[]>(parameterMap.size());
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] value = entry.getValue();
            if (value.length == 1) {
                value = value[0].split(",");
            }
            int i = 0;
            for (String s : value) {
                try {
                    value[i++] = URLDecoder.decode(s, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("unable to decode parameter " + entry.getKey() + " value " + Arrays.toString(value), e);
                }
            }
            newParamMap.put(entry.getKey(), value);
        }
        return newParamMap;
    }

    protected Query createLocationFacetQuery(List<Facet> facetList, Integer surveyId,
                                             String sortProperty, String sortOrder, String searchText, String locationArea, List<Location> locList) {
        List<Location> locations = null;
        if (!StringUtils.nullOrEmpty(searchText)) {
            // use an indexed query for searchText
            try {
                Query indexedQuery = getIndexedQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea);
                locations = indexedQuery.list();
            } catch (Exception e) {
                log.error("Exception occurred creating query for search text '" + searchText + "'. Ignoring search criteria.", e);
            }

            //if it matched nothing when a search term was specified, return nothing
            if (locations == null || locations.isEmpty()) {
                return null;
            }
        }
        // add the location list parameter as query criteria
        if (locations == null) {
            locations = locList;
        } else if (locList != null) {
            locations.addAll(locList);
        }

        // extra columns in select are used for ordering
        HqlQuery hqlQuery = new HqlQuery("select distinct location, " +
                "location.name, location.description, location.weight, location.createdBy, " +
                "location.createdAt, location.user from Location location");

        if (!StringUtils.nullOrEmpty(locationArea)) {
            // IMPORTANT!
            // we expect location area to be in lat/lons i.e. srid = 4326. Since the database can now
            // contain geomtries of varying SRIDs, we need to transform them to all be the same.
            // 4326 is the most logical choice since that is the same SRID as our geometry
            // argument.
            hqlQuery.and(new Predicate("within(transform(location.location," + BdrsCoordReferenceSystem.DEFAULT_SRID + "), ?) = True"));
        }
        applyLocationFacetsToQuery(hqlQuery, facetList, surveyId, searchText);

        if (locations != null && !locations.isEmpty()) {
            hqlQuery.and(new Predicate("location in (:locs)"));
        }
        // NO SQL injection for you
        if (sortProperty != null && sortOrder != null && AdvancedReviewLocationsController.VALID_SORT_PROPERTIES.contains(sortProperty)) {
            hqlQuery.order(sortProperty,
                    SortOrder.valueOf(sortOrder).name(),
                    null);
        }
        Query query = toHibernateQuery(hqlQuery, locationArea, locations);
        return query;
    }

    protected Query getIndexedQuery(List<Facet> facetList, Integer surveyId,
                                    String sortProperty, String sortOrder, String searchText,
                                    String locationArea) throws ParseException {
        // the fields to search on
        String[] fields = new String[]{};
        Session sesh = getRequestContext().getHibernate();

        Analyzer customAnalyzer = Search.getFullTextSession(sesh).getSearchFactory().getAnalyzer(IndexingConstants.FULL_TEXT_ANALYZER);
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(customAnalyzer);

        String searchTerm = buildSearchTerm(facetList, surveyId, searchText, locationArea);

        Query query = searchService.getQuery(sesh, fields, analyzer, searchTerm, Location.class);
        if (sortProperty != null && query instanceof FullTextQuery) {
            // add the sorting
            Sort sort = new Sort(sortProperty.substring(sortProperty.indexOf(".") + 1) + "_sort", sortOrder == null ? false : SortOrder.valueOf(sortOrder) == SortOrder.DESC);

            ((FullTextQuery) query).setSort(sort);
        }
        return query;
    }

    private String buildSearchTerm(List<Facet> facetList, Integer surveyId, String searchText,
                                   String locationArea) {
        StringBuilder sb = new StringBuilder();

        // add the faceting
        for (Facet f : facetList) {
            if (f.isActive()) {
                String s = f.getIndexedQueryString();
                if (!StringUtils.nullOrEmpty(s)) {
                    sb.append(" +" + s);
                }
            }
        }

        if (surveyId != null) {
            // add the survey
            sb.append(" +surveys.id:" + surveyId);
        }

        // add the search text to the location name, description, and attribute value text
        sb.append(" +(name:" + searchText + " description:" + searchText + " attributes.stringValue:" + searchText + " user.name:" + searchText + ")");

        // add the spatial query

        return sb.toString();
    }

    protected void applyLocationFacetsToQuery(HqlQuery hqlQuery,
                                              List<Facet> facetList, Integer surveyId, String searchText) {
        hqlQuery.leftJoin("location.attributes", "locAttributeVal");
        hqlQuery.leftJoin("location.user", "user");

        hqlQuery.leftJoin("location.regions", "regions");
        hqlQuery.leftJoin("location.metadata", "metadata");

        hqlQuery.leftJoin("locAttributeVal.attribute", "locAttribute");
        hqlQuery.leftJoin("location.surveys", "survey");
        for (Facet f : facetList) {
            if (f.isActive()) {
                Predicate p = f.getPredicate();
                if (p != null) {
                    f.applyCustomJoins(hqlQuery);
                    hqlQuery.and(p);
                }
            }
        }

        if (surveyId != null) {
            hqlQuery.and(Predicate.eq("survey.id", surveyId));
        }
    }

}
