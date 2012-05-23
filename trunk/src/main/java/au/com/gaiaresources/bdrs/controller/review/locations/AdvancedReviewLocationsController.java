package au.com.gaiaresources.bdrs.controller.review.locations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Sort;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.map.LocationDownloadWriter;
import au.com.gaiaresources.bdrs.controller.map.RecordDownloadFormat;
import au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.ScrollableResultsImpl;
import au.com.gaiaresources.bdrs.kml.KMLWriter;
import au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO;
import au.com.gaiaresources.bdrs.model.index.IndexUtil;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.util.DateFormatter;
import au.com.gaiaresources.bdrs.util.KMLUtils;
import au.com.gaiaresources.bdrs.util.StringUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author stephanie
 *
 */
@Controller
public class AdvancedReviewLocationsController extends AdvancedReviewController<Location> {

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private LocationDAO locationDAO;
    
    @Autowired
    private IndexScheduleDAO indexDAO;
    
    private static final String KML_FILENAME = "Locations.kml";
    private static final String SHAPEFILE_ZIP_ENTRY_FORMAT = "shp/Locations.zip";
    private static final String XLS_ZIP_ENTRY_FORMAT = "xls/Locations.xls";
    
    public static final Set<String> VALID_SORT_PROPERTIES;

    static {
        Set<String> temp = new HashSet<String>();
        temp.add("location.name");
        temp.add("location.description");
        temp.add("location.weight");
        temp.add("location.createdBy");
        temp.add("location.createdAt");
        temp.add("location.user");
        VALID_SORT_PROPERTIES = Collections.unmodifiableSet(temp);
    }
    
    /**
     * Provides a view of the facet listing and a skeleton of the map or list
     * view. The map or list view will populate itself via asynchronous 
     * javascript requests. 
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewLocations.htm", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView advancedReview(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                       @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) {
        configureHibernateSession();
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        
        String locations = getParameter(newParamMap, PARAM_LOCATIONS);
        if (!StringUtils.nullOrEmpty(locations)) {
            newParamMap.put(PARAM_LOCATIONS, locations.split(","));
        }
        String locationArea = getParameter(newParamMap, PARAM_LOCATION_AREA);
        // map view doesn't filter based on locations parameter
        List<Location> locList = null;
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), newParamMap);
        Long recordCount = countMatchingRecords(facetList,
                                                surveyId,
                                                getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME),
                                                locationArea, locList);
        
        ModelAndView mv = getAdvancedReviewView(newParamMap, surveyId, resultsPerPage, pageNumber, facetList, recordCount, "advancedReviewLocations");
        if (!StringUtils.nullOrEmpty(getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME))) {
            if (recordCount == 0) {
                Date indexDate = IndexUtil.getLastIndexBuildTime(indexDAO, Location.class);
                if (indexDate == null) {
                    getRequestContext().addMessage("bdrs.index.exist.error");
                } else if (recordCount < 1) {
                    getRequestContext().addMessage("bdrs.index.build.date", 
                                                   new Object[]{
                                                        DateFormatter.format(indexDate, DateFormatter.DAY_MONTH_YEAR_TIME)
                                                   });
                }
            }
        }
        
        if (!StringUtils.nullOrEmpty(locationArea)) {
            mv.addObject(PARAM_LOCATION_AREA, locationArea);
        }
        if (!StringUtils.nullOrEmpty(locations)) {
            mv.addObject(PARAM_LOCATIONS, locations);
        }
        return mv;
    }
    
    /**
     * Returns the number of results matching the query filtered by the facet list, 
     * searchText, locationArea, and location list.
     * @param facetList a list of facets to filter by
     * @param surveyId a survey id to filter by
     * @param searchText a text string to filter by
     * @param locationArea a geometric area (in WKT string) to filter by
     * @param locList a list of locations to filter the selection by
     * @return the number of results returned from filtered query
     */
    private Long countMatchingRecords(List<Facet> facetList, Integer surveyId,
            String searchText, String locationArea, List<Location> locList) {
        List<Location> locations = null;
        if (!StringUtils.nullOrEmpty(searchText)) {
            // use an indexed query for searchText
            try {
                Query indexedQuery = getIndexedQuery(facetList, surveyId, null, null, searchText, locationArea);
                locations = indexedQuery.list();
            } catch (Exception e) {
                log.error("Exception occurred creating query for search text '"+searchText+"'. Ignoring search criteria.", e);
            }
            
            //if it matched nothing when a search term was specified, return nothing
            if (locations == null || locations.isEmpty()) {
                return 0L;
            }
        }
        // add the location list parameter as query criteria
        if (locations == null) {
            locations = locList;
        } else if (locList != null) {
            locations.addAll(locList);
        }
        
        HqlQuery hqlQuery = new HqlQuery(getCountSelect());
        if (!StringUtils.nullOrEmpty(locationArea)) {
            hqlQuery.and(new Predicate("within(location.location, ?) = True"));
        }
        applyFacetsToQuery(hqlQuery, facetList, surveyId, searchText);

        if (locations != null && !locations.isEmpty()) {
            hqlQuery.and(new Predicate("location in (:locs)"));
        }
        
        Query query = toHibernateQuery(hqlQuery, locationArea, locations);
        Object result = query.uniqueResult();
        return Long.parseLong(result.toString());
    }

    /**
     * Returns the list of records matching the {@link Facet} criteria as KML.
     * @throws ParseException 
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewKMLLocations.htm", method = {RequestMethod.GET, RequestMethod.POST})
    public void advancedReviewKMLSightings(HttpServletRequest request, HttpServletResponse response) throws IOException, JAXBException, ParseException {
        configureHibernateSession();
        Integer surveyId = null;
        Map<String, String[]> newParamMap = decodeParamMap(request.getParameterMap());
        
        if(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), newParamMap);
        String locationArea = getParameter(newParamMap, PARAM_LOCATION_AREA);
        ScrollableResults<Location> sr = getScrollableResults(facetList, surveyId, 
                                                                     getParameter(newParamMap, SORT_BY_QUERY_PARAM_NAME), 
                                                                     getParameter(newParamMap, SORT_ORDER_QUERY_PARAM_NAME),
                                                                     getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME),
                                                                     locationArea, null);
        advancedReviewKMLSightings(request, response, facetList, sr);
    }
    
    /**
     * Applies the selection criteria represented by the provided {@link Facet}s
     * and the associated {@link FacetOption}s returning the matching {@link List}
     * of {@link Location}. 
     * 
     * @param facetList the {@link Facet}s providing the selection criteria.
     * @param surveyId the primary key of the survey containing all eligible locations.
     * The <code>surveyId</code> may be null if all surveys are allowed.
     * @param sortProperty the HQL property that should be used for sorting. 
     * The sortProperty may be null if no sorting is necessary.
     * @param sortOrder the sorting order
     * @param searchText textual restriction to be applied to matching records.
     * @return the Query to select the matching records {@link Record}s.
         * @throws ParseException 
     * 
     * @see SortOrder
     */
    protected ScrollableResults<Location> getScrollableResults(List<Facet> facetList,
                                                                    Integer surveyId,
                                                                    String sortProperty, 
                                                                    String sortOrder, 
                                                                    String searchText, 
                                                                    String locationArea,
                                                                    List<Location> locList) throws ParseException {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea, locList);
        return new ScrollableResultsImpl<Location>(query);
    }
    protected ScrollableResults<Location> getScrollableResults(List<Facet> facetList,
                                                                    Integer surveyId,
                                                                    String sortProperty, 
                                                                    String sortOrder, 
                                                                    String searchText,
                                                                    int pageNumber, 
                                                                    int entriesPerPage, 
                                                                    String locationArea,
                                                                    List<Location> locList) throws ParseException {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea, locList);
        return new ScrollableResultsImpl<Location>(query, pageNumber, entriesPerPage);
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
     * @return the Query to select the matching records {@link Record}s.
     * @throws ParseException 
     * 
     * @see SortOrder
     */
    protected Query getMatchingRecordsQuery(List<Facet> facetList,
                                            Integer surveyId,
                                            String sortProperty, 
                                            String sortOrder, 
                                            String searchText,
                                            String locationArea,
                                            List<Location> locations) throws ParseException {
        return createLocationFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea, locations);
    }
    
    /**
     * Returns a JSON array of records matching the {@link Facet} criteria.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewJSONLocations.htm", method = {RequestMethod.GET, RequestMethod.POST})
    public void advancedReviewJSONSightings(HttpServletRequest request, 
                                            HttpServletResponse response,
                                            @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                            @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) throws IOException {
        configureHibernateSession();
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        
        Integer surveyId = null;
        if(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        
        String locationArea = getParameter(newParamMap, PARAM_LOCATION_AREA);
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), newParamMap);
        try {
            ScrollableResults<Location> sc = getScrollableResults(facetList,
                                                                         surveyId,
                                                                         getParameter(newParamMap, SORT_BY_QUERY_PARAM_NAME), 
                                                                         getParameter(newParamMap, SORT_ORDER_QUERY_PARAM_NAME),
                                                                         getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME),
                                                                         pageNumber, resultsPerPage, 
                                                                         locationArea, null);
            advancedReviewJSONSightings(response, facetList, sc);
        } catch (ParseException e) {
            //TODO: return error
            log.error(e);
        }
    }

    /**
     * Returns an XLS representation of representation of locations matching the
     * {@link Facet} criteria.
     * @throws Exception 
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewDownloadLocations.htm", method = {RequestMethod.GET, RequestMethod.POST})
    public void advancedReviewDownload(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=QUERY_PARAM_DOWNLOAD_FORMAT, required=true) String[] downloadFormat) throws Exception {
        configureHibernateSession();
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        
        User currentUser = currentUser();
        // some locations have been selected, add them to the parameters as facet selections
        String locations = getParameter(newParamMap, PARAM_LOCATIONS);
        String locationArea = getParameter(newParamMap, PARAM_LOCATION_AREA);
        List<Location> locList = getLocationsFromParameter(locations);
        List<Facet> facetList = facetService.getLocationFacetList(currentUser, newParamMap);
        
        ScrollableResults<Location> sc = getScrollableResults(facetList, surveyId, 
                                                              getParameter(newParamMap, SORT_BY_QUERY_PARAM_NAME), 
                                                              getParameter(newParamMap, SORT_ORDER_QUERY_PARAM_NAME),
                                                              getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME), 
                                                              locationArea, locList);
        
        downloadLocations(request, response, downloadFormat, sc, locList);
    }

    /**
     * Returns a {@link List} of {@link Location}s matching the comma-separated
     * list of {@link Location} ids or null if a null or empty id string is passed
     * @param locations a comma-separated list of {@link Location} ids
     * @return a list of {@link Location}s matching the {@link List} of ids
     */
    private List<Location> getLocationsFromParameter(String locations) {
        List<Location> locList = null;
        if (!StringUtils.nullOrEmpty(locations)) {
            String[] ids = locations.split(",");
            Integer[] intIds = new Integer[ids.length];
            for (int i = 0; i < ids.length; i++) {
                intIds[i] = Integer.valueOf(ids[i]);
            }
            // list of locations to download
            locList = locationDAO.getLocations(Arrays.asList(intIds));
        }
        return locList;
    }

    /**
     * For some scrollable records, create files in the requested download format and
     * zip them up
     * 
     * @param request - the http request object.
     * @param response - the http response object.
     * @param downloadFormat - array containing the download formats.
     * @param locList - the list of locations to download.
     * @throws Exception
     */
    protected void downloadLocations(HttpServletRequest request, 
            HttpServletResponse response, 
            String[] downloadFormat,
            ScrollableResults<Location> sc, 
            List<Location> locList) throws Exception {
        User user = getRequestContext().getUser();

        if (response.isCommitted()) {
            return;
        }

        response.setContentType(SIGHTINGS_DOWNLOAD_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment;filename=locations_"
                + System.currentTimeMillis() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
        try {
            if (downloadFormat != null) {
                Session sesh = getRequestContext().getHibernate();
                String contextPath = request.getContextPath();
                LocationDownloadWriter downloadWriter = new LocationDownloadWriter();
                for (String format : downloadFormat) {
                    RecordDownloadFormat rdf = RecordDownloadFormat.valueOf(format);
                    switch (rdf) {
                    case KML: {
                        // make sure scrollable records is rewound and ready to go!
                        sc.rewind();
                        ZipEntry kmlEntry = new ZipEntry(KML_FILENAME);
                        zos.putNextEntry(kmlEntry);
                        writeKML(zos, sesh, contextPath, user, sc);
                        zos.closeEntry();
                        break;
                    }
                    case SHAPEFILE: {
                        // make sure scrollable records is rewound and ready to go!
                        sc.rewind();
                        
                        ZipEntry shpEntry = new ZipEntry(
                                String.format(SHAPEFILE_ZIP_ENTRY_FORMAT));
                        zos.putNextEntry(shpEntry);

                        downloadWriter.write(bulkDataService, rdf, zos, sesh, contextPath, null, user, sc);
                        zos.closeEntry();
                        break;
                    }
                    case XLS: {
                        // make sure scrollable records is rewound and ready to go!
                        sc.rewind();
                        
                        ZipEntry shpEntry = new ZipEntry(
                                String.format(XLS_ZIP_ENTRY_FORMAT));
                        zos.putNextEntry(shpEntry);

                        downloadWriter.write(bulkDataService, rdf, zos, sesh, contextPath, null, user, sc);
                        zos.closeEntry();
                        break;
                    }
                    default:
                        // Do Nothing
                        break;
                    }
                }
            }

        } finally {
            zos.flush();
            zos.close();
        }
    }
    
    @Override
    protected Query createFacetQuery(List<Facet> facetList, Integer surveyId,
            String sortProperty, String sortOrder, String searchText) {
        return createLocationFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, null, null);
    }

    @Override
    protected String getCountSelect() {
        return "select count(distinct location) from Location location";
    }

    @Override
    protected void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList,
            Integer surveyId, String searchText) {
        applyLocationFacetsToQuery(hqlQuery, facetList, surveyId, searchText);
    }

    @Override
    protected void writeKMLResults(KMLWriter writer, User currentUser,
            String contextPath, List<Location> rList) {
        KMLUtils.writeLocations(writer, contextPath, rList);
    }
    
    @Override
    protected String getDefaultSortString() {
        return "location.name";
    }
    
    private void writeKML(ZipOutputStream zos, Session sesh, String contextPath, User user, ScrollableResults<Location> sc) throws JAXBException {
        int recordCount = 0;
        List<Location> locList = new ArrayList<Location>(ScrollableResults.RESULTS_BATCH_SIZE);
        KMLWriter writer = KMLUtils.createKMLWriter(contextPath, null, getKMLFolderName());
        while (sc.hasMoreElements()) {
            locList.add(sc.nextElement());
            // evict to ensure garbage collection
            if (++recordCount % ScrollableResults.RESULTS_BATCH_SIZE == 0) {
                
                KMLUtils.writeLocations(writer, contextPath, locList);
                locList.clear();
                sesh.clear();
            }
        }
        
        // Flush the remainder out of the list.
        KMLUtils.writeLocations(writer, contextPath, locList);
        sesh.clear();
        
        writer.write(false, zos);
    }

    @Override
    protected List<Report> getReportList() {
        return Collections.emptyList();
    }

    @Override
    protected String getKMLFolderName() {
        return KMLUtils.KML_LOCATION_FOLDER;
    }
    
    /**
     * Returns a count of locations matching the {@link Facet} criteria.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewCountLocations.htm", method = {RequestMethod.GET, RequestMethod.POST})
    public void advancedReviewCountSightings(HttpServletRequest request, 
                                            HttpServletResponse response) throws IOException {
        configureHibernateSession();
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        
        Integer surveyId = null;
        if(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(getParameter(newParamMap, SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        String locations = getParameter(newParamMap, PARAM_LOCATIONS);
        String locationArea = getParameter(newParamMap, PARAM_LOCATION_AREA);
        List<Location> locList = getLocationsFromParameter(locations);
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), newParamMap);
        Long recordCount = countMatchingRecords(facetList,
                                                surveyId,
                                                getParameter(newParamMap, SEARCH_QUERY_PARAM_NAME),
                                                locationArea, locList);
        response.setContentType("text/plain");
        response.getWriter().write(recordCount.toString());
        response.getWriter().flush();
    }
}
