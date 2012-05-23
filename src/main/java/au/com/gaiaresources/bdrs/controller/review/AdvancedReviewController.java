package au.com.gaiaresources.bdrs.controller.review;

import au.com.gaiaresources.bdrs.controller.review.sightings.SightingsController;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.kml.KMLWriter;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceUtil;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.FacetService;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.python.report.ReportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.util.KMLUtils;
import au.com.gaiaresources.bdrs.util.StringUtils;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.CustomType;
import org.hibernatespatial.GeometryUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import com.vividsolutions.jts.geom.Geometry;

import edu.emory.mathcs.backport.java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An advanced review view has a group of facets and 3 views: table, map, download.
 * 
 * @author stephanie
 *
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
     * Constants for request parameters
     */
    public static final String PARAM_LOCATION_AREA = "locationArea";
    public static final String PARAM_LOCATIONS = "locations";

    protected static final String ATTRIBUTE_KML_PARAMS = "KMLParameters";
    
    @Autowired
    protected FacetService facetService;

    /** Used to retrieve the default search results view (map or list) */
    @Autowired
    protected PreferenceDAO preferenceDAO;

    /** Used to retrieve the reports eligible for the advanced review page. */
    @Autowired
    protected ReportDAO reportDAO;
    
    @Autowired
    protected ReportService reportService;
    
    @Autowired
    protected LocationService locationService;
    /**
     * Returns the view for the request
     * @param newParamMap the request for the view
     * @param surveyId the id of the survey to get results for
     * @param resultsPerPage the number of results to display per page
     * @param pageNumber the page number to start on
     * @param facetList the list of facets by which to filter the results
     * @param recordCount the number of results that will be returned by the query
     * @param viewName the name of the view to return
     * @return a ModelAndView
     */
    protected ModelAndView getAdvancedReviewView(Map<String, String[]> newParamMap, 
            Integer surveyId, Integer resultsPerPage, Integer pageNumber, List<Facet> facetList, Long recordCount, String viewName) {
        long pageCount = recordCount / resultsPerPage;
        if((recordCount % resultsPerPage) > 0) {
            pageCount += 1;
        }
        
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
        mv.addObject("recordCount", recordCount);
        mv.addObject("resultsPerPage", resultsPerPage);
        mv.addObject("pageCount", pageCount);
        mv.addObject("reportList", getReportList());
        
        // Avoid the situation where the number of results per page is increased
        // thereby leaving a page number higher than the total page count.
        mv.addObject("pageNumber", Math.min(pageCount, pageNumber.longValue()));

        return mv;
    }
    
    /**
     * Returns a list of reports that are available to the view.
     * @return
     */
    protected abstract List<Report> getReportList();

    /**
     * Returns the default sorting parameter for the view to be used for the initial sort.
     * @return
     */
    protected abstract String getDefaultSortString();

    /**
     * Returns the list of results matching the {@link Facet} criteria as KML.
     */
    public void advancedReviewKMLSightings(HttpServletRequest request, HttpServletResponse response, 
            List<Facet> facetList, ScrollableResults<T> sr) throws IOException, JAXBException {

        KMLWriter writer = KMLUtils.createKMLWriter(request.getContextPath(), null, getKMLFolderName());
        User currentUser = getRequestContext().getUser();
        String contextPath = request.getContextPath();
        Session sesh = getRequestContext().getHibernate();
        
        int recordCount = 0;
        List<T> rList = new ArrayList<T>(ScrollableRecords.RESULTS_BATCH_SIZE);
        while (sr.hasMoreElements()) {
            rList.add(sr.nextElement());
            
            // evict to ensure garbage collection
            if (++recordCount % ScrollableRecords.RESULTS_BATCH_SIZE == 0) {
                writeKMLResults(writer, currentUser, contextPath, rList);
                rList.clear();
                sesh.clear();
            }
        }
        writeKMLResults(writer, currentUser, contextPath, rList);
        
        response.setContentType(KMLUtils.KML_CONTENT_TYPE);
        writer.write(false, response.getOutputStream());
    }
    
    /**
     * Returns the name of the root folder to use in the kml file.
     * @return
     */
    protected abstract String getKMLFolderName();

    /**
     * Writes a list of results to KML.
     * @param writer the writer to use for writing
     * @param currentUser the logged in user
     * @param contextPath the contextPath of the application
     * @param rList the list of results to write
     */
    protected abstract void writeKMLResults(KMLWriter writer, User currentUser,
            String contextPath, List<T> rList);

    /**
     * Returns a JSON array of results matching the {@link Facet} criteria.
     */
    public void advancedReviewJSONSightings(HttpServletResponse response,
                                            List<Facet> facetList, ScrollableResults<? extends PortalPersistentImpl> sc) throws IOException {
        int recordCount = 0;
        JSONArray array = new JSONArray();
        PortalPersistentImpl r;
        while(sc.hasMoreElements()) {
            r = sc.nextElement();
            array.add(r.flatten(2));
            if (++recordCount % ScrollableRecords.RESULTS_BATCH_SIZE == 0) {
                getRequestContext().getHibernate().clear();
            }
        }
        
        response.setContentType("application/json");
        response.getWriter().write(array.toString());
        response.getWriter().flush();
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
    }

    /**
     * @return the authenticated user from the request context, or null if the request originated from an
     * anonymous user.
     */
    protected User currentUser() {
        return getRequestContext().getUser();
    }

    /**
     * Returns the default view model to use if a view type has not been specified in the request.
     * The default is determined by the value of the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     * @return the default view model name to use.
     */
    private String defaultView() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(preferenceDAO);
        boolean useMap = preferenceUtil.getBooleanPreference(Preference.ADVANCED_REVIEW_DEFAULT_VIEW_KEY);
        return useMap ? MODEL_MAP_VIEW_SELECTED : MODEL_TABLE_VIEW_SELECTED;
    }

    /**
     * Returns the model object that represents the view to display based on the supplied request parameter.
     * @param viewTypeParameter the value of the request parameter used to request a particular view type.  May be null,
     *                          in which case the default is determined by the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     * @return the name of the view model object used by the page to select the correct view.
     */
    private String getViewType(String viewTypeParameter) {
        if (VIEW_TYPE_DOWNLOAD.equals(viewTypeParameter)) {
           return MODEL_DOWNLOAD_VIEW_SELECTED;
        } else if (VIEW_TYPE_TABLE.equals(viewTypeParameter)) {
            return MODEL_TABLE_VIEW_SELECTED;
        } else if (VIEW_TYPE_MAP.equals(viewTypeParameter)) {
            return MODEL_MAP_VIEW_SELECTED;
        }
        else {
            return defaultView();
        }
    }
    
    /**
     * Creates a {@link Query} object based on the selections in the {@link Facet}s.
     * @param facetList The list of {@link Facet} to filter by
     * @param surveyId surveyId (optional) surveyId to filter by
     * @param sortProperty the HQL property that should be used for sorting.
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching results
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
        for(int i=0; i<parameterValues.length; i++) {
            query.setParameter(i, parameterValues[i]);
        }
        return query;
    }
    
    /**
     * Counts the number of values that match the facet selections, the surveyId, and the searchText
     * @param facetList The list of {@link Facet} on the screen
     * @param surveyId (optional) surveyId to filter by
     * @param searchText (optional) value to search entries for
     * @return The number of values matching the facet selections, surveyId, and searchText
     */
    protected long countMatchingRecords(List<Facet> facetList, Integer surveyId, String searchText) {
        HqlQuery hqlQuery = new HqlQuery(getCountSelect());
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
     * @param hqlQuery the query to apply the facet predicates to
     * @param facetList the list of facets to apply to the query
     * @param surveyId the surveyId to limit the results by
     * @param searchText textual restriction to be applied to matching results
     */
    protected abstract void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList,
            Integer surveyId, String searchText);
    
    
    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of {@link Record}. 
     * 
     * @param facetList the {@link Facet}s providing the selection criteria.
     * @param surveyId the primary key of the survey containing all eligible results.
     * The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting. 
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching results.
     * @return the Query to select the matching results {@link Record}s.
     * 
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
     * @param facetList the {@link Facet}s providing the selection criteria.
     * @param surveyId the primary key of the survey containing all eligible results.
     * The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting. 
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching results.
     * @return the matching results as a scrollable result set
     * 
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
     * @param facetList the {@link Facet}s providing the selection criteria.
     * @param surveyId the primary key of the survey containing all eligible results.
     * The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting. 
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching results.
     * @param pageNumber the page number to retrieve results for
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
     * @param facetList the {@link Facet}s providing the selection criteria.
     * @param surveyId the primary key of the survey containing all eligible records.
     * The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting. 
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching records.
     * @return the {@link List} of matching {@link Record}s.
     * 
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
        if(resultsPerPage != null && pageNumber != null && resultsPerPage > 0 && pageNumber > 0) {
            query.setFirstResult((pageNumber-1) * resultsPerPage);
            query.setMaxResults(resultsPerPage);
        }
        
        List<Object[]> rowList = query.list();
        List<T> recordList = new ArrayList<T>(rowList.size());
        for(Object[] rowObj : rowList) {
            recordList.add((T)rowObj[0]);
        }
        
        return recordList;
    }

    /**
     * Converts the {@link HqlQuery} to a {@ Query} representation.
     * @param locations 
     */
    protected Query toHibernateQuery(HqlQuery hqlQuery, String locationArea, List<Location> locations) {
        Session sesh = getRequestContext().getHibernate();
        Query query = sesh.createQuery(hqlQuery.getQueryString());
        
        // the geometry comes first
        CustomType geometryType = new CustomType(GeometryUserType.class, null);
        int index = 0;
        if (!StringUtils.nullOrEmpty(locationArea)) {
            Geometry geometry = locationService.createGeometryFromWKT(locationArea);
            query.setParameter(index++, geometry, geometryType);
        }
        
        Object[] parameterValues = hqlQuery.getParametersValue();
        for(int i=0; i<parameterValues.length; i++) {
            query.setParameter(index++, parameterValues[i]);
        }
        
        if (locations != null && !locations.isEmpty()) {
            query.setParameterList("locs", locations);
        }
        return query;
    }
    
    /**
     * Retrieves the parameter with the name paramKey from the request map.
     * @param requestMap the map containing the request parameters
     * @param paramKey the name of the parameter to retrieve
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
     * @param requestMap the map to get the values from
     * @param paramKey the key to get values for
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
     * @param parameterMap the request parameter map
     * @return a Map that is decoded as described
     */
    protected Map<String, String[]> decodeParamMap(Map<String,String[]> parameterMap) {
        Map<String, String[]> newParamMap = new HashMap<String,String[]>(parameterMap.size());
        for (Entry<String,String[]> entry : parameterMap.entrySet()) {
            String[] value = entry.getValue();
            if (value.length == 1) {
                value = value[0].split(",");
            }
            int i = 0;
            for (String s : value) {
                try {
                    value[i++] = URLDecoder.decode(s, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("unable to decode parameter "+entry.getKey()+" value "+Arrays.toString(value), e);
                }
            }
            newParamMap.put(entry.getKey(), value);
        }
        return newParamMap;
    }
}
