package au.com.gaiaresources.bdrs.controller.review.locations;

import java.io.IOException;
import java.util.ArrayList;
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
import org.hibernate.type.CustomType;
import org.hibernatespatial.GeometryUserType;
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
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.record.RecordSurveyFacet;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.KMLUtils;
import au.com.gaiaresources.bdrs.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;

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
    private LocationService locationService;

    @Autowired
    private LocationDAO locationDAO;
    
    @Autowired
    private SearchService searchService;
    
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
    @RequestMapping(value = "/review/sightings/advancedReviewLocations.htm", method = RequestMethod.GET)
    public ModelAndView advancedReview(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                       @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) {
        //configureHibernateSession();
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), (Map<String, String[]>)request.getParameterMap());
        Long recordCount = countMatchingRecords(facetList,
                                                surveyId,
                                                request.getParameter(SEARCH_QUERY_PARAM_NAME));
        
        ModelAndView mv = getAdvancedReviewView(request, surveyId, resultsPerPage, pageNumber, facetList, recordCount, "advancedReviewLocations");
        return mv;
    }
    
    /**
     * Returns the list of records matching the {@link Facet} criteria as KML.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewKMLLocations.htm", method = RequestMethod.GET)
    public void advancedReviewKMLSightings(HttpServletRequest request, HttpServletResponse response) throws IOException, JAXBException {
        configureHibernateSession();
        Integer surveyId = null;
        if(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), (Map<String, String[]>)request.getParameterMap());
        
        ScrollableResults<Location> sr = getScrollableResults(facetList, surveyId, 
                                                                     request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                                     request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                                     request.getParameter(SEARCH_QUERY_PARAM_NAME));
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
                                                                    String locationArea) throws ParseException {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea);
        return new ScrollableResultsImpl<Location>(query);
    }
    protected ScrollableResults<Location> getScrollableResults(List<Facet> facetList,
                                                               Integer surveyId,
                                                               String sortProperty, 
                                                               String sortOrder, 
                                                               String searchText, 
                                                               List<Location> locList) throws ParseException {
       Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locList);
       return new ScrollableResultsImpl<Location>(query);
    }
    protected ScrollableResults<Location> getScrollableResults(List<Facet> facetList,
                                                                    Integer surveyId,
                                                                    String sortProperty, 
                                                                    String sortOrder, 
                                                                    String searchText,
                                                                    int pageNumber, int entriesPerPage, String locationArea) throws ParseException {
        Query query = getMatchingRecordsQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea);
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
                                            String locationArea) throws ParseException {
        return createFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea, null);
    }
    
    protected Query getMatchingRecordsQuery(List<Facet> facetList,
            Integer surveyId,
            String sortProperty, 
            String sortOrder, 
            String searchText,
            List<Location> locList) {
        return createFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locList);
    }
    
    /**
     * Returns a JSON array of records matching the {@link Facet} criteria.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR, Role.USER})
    @RequestMapping(value = "/review/sightings/advancedReviewJSONLocations.htm", method = RequestMethod.GET)
    public void advancedReviewJSONSightings(HttpServletRequest request, 
                                            HttpServletResponse response,
                                            @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                            @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) throws IOException {
       configureHibernateSession();
        
        Integer surveyId = null;
        if(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        List<Facet> facetList = facetService.getLocationFacetList(currentUser(), (Map<String, String[]>)request.getParameterMap());
        try {
            ScrollableResults<Location> sc = getScrollableResults(facetList,
                                                                         surveyId,
                                                                         request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                                         request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                                         request.getParameter(SEARCH_QUERY_PARAM_NAME),
                                                                         pageNumber, resultsPerPage, request.getParameter("locationArea"));
            advancedReviewJSONSightings(request, response, facetList, sc);
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
    @RequestMapping(value = "/review/sightings/advancedReviewDownloadLocations.htm", method = RequestMethod.GET)
    public void advancedReviewDownload(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=QUERY_PARAM_DOWNLOAD_FORMAT, required=true) String[] downloadFormat) throws Exception {
        configureHibernateSession();
        
        User currentUser = currentUser();
        // some locations have been selected, add them to the parameters as facet selections
        String locations = request.getParameter("locations");
        List<Location> locList = getLocationsFromParameter(locations);
        List<Facet> facetList = facetService.getFacetList(currentUser, (Map<String, String[]>)request.getParameterMap());
        
        ScrollableResults<Location> sc = getScrollableResults(facetList, surveyId, 
                                                              request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                              request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                              request.getParameter(SEARCH_QUERY_PARAM_NAME), locList);
        
        downloadLocations(request, response, downloadFormat, sc, locList);
    }

    private List<Location> getLocationsFromParameter(String locations) {
        List<Location> locList = null;
        if (StringUtils.nullOrEmpty(locations)) {
            locList = locationDAO.getLocations();
        } else {
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
    
    @Override
    protected Query createFacetQuery(List<Facet> facetList, Integer surveyId,
            String sortProperty, String sortOrder, String searchText) {
        return createFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, null, null);
    }
    
    protected Query createFacetQuery(List<Facet> facetList, Integer surveyId,
            String sortProperty, String sortOrder, String searchText, List<Location> locList) {
        return createFacetQuery(facetList, surveyId, sortProperty, sortOrder, searchText, null, locList);
    }
    
    protected Query createFacetQuery(List<Facet> facetList, Integer surveyId,
            String sortProperty, String sortOrder, String searchText, String locationArea, List<Location> locList) {
        List<Location> locations = null;
        if (!StringUtils.nullOrEmpty(searchText)) {
            // use an indexed query for searchText
            try {
                Query indexedQuery = getIndexedQuery(facetList, surveyId, sortProperty, sortOrder, searchText, locationArea);
                locations = indexedQuery.list();
            } catch (Exception e) {
                log.error("Exception occurred creating query for search text '"+searchText+"'. Ignoring search criteria.", e);
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
            hqlQuery.and(new Predicate("within(location.location, ?) = True"));
        }
        applyFacetsToQuery(hqlQuery, facetList, surveyId, searchText);

        if (locations != null && !locations.isEmpty()) {
            hqlQuery.and(new Predicate("location in (:locs)"));
        }
        // NO SQL injection for you
        if(sortProperty != null && sortOrder != null && VALID_SORT_PROPERTIES.contains(sortProperty)) {
            hqlQuery.order(sortProperty, 
                           SortOrder.valueOf(sortOrder).name(),
                           null);
        }
        Query query = toHibernateQuery(hqlQuery, locationArea, locations);
        return query;
    }
    
    private Query getIndexedQuery(List<Facet> facetList, Integer surveyId,
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
            Sort sort = new Sort(sortProperty.substring(sortProperty.indexOf(".")+1)+"_sort", sortOrder == null ? false : SortOrder.valueOf(sortOrder) == SortOrder.DESC);
            
            ((FullTextQuery)query).setSort(sort);
        }
        log.debug("query is: "+query.getQueryString());
        return query;
    }

    private String buildSearchTerm(List<Facet> facetList, Integer surveyId, String searchText,
            String locationArea) {
        StringBuilder sb = new StringBuilder();
        
        // add the faceting
        for(Facet f : facetList) {
            if(f.isActive()) {
                String s = f.getIndexedQueryString();
                if (!StringUtils.nullOrEmpty(s)) {
                    sb.append(" +" + s);
                }
            }
        }
        
        if (surveyId != null) {
            // add the survey
            sb.append(" +surveys.id:"+surveyId);
        }
        
        // add the search text to the location name, description, and attribute value text
        sb.append(" +(name:"+searchText+" description:"+searchText+" attributes.stringValue:"+searchText+" user.name:"+searchText+")");
        
        // add the spatial query
        
        return sb.toString();
    }

    @Override
    protected String getCountSelect() {
        return "select count(distinct location) from Location location";
    }

    @Override
    protected void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList,
            Integer surveyId, String searchText) {
        hqlQuery.leftJoin("location.attributes", "locAttributeVal");
        hqlQuery.leftJoin("location.user", "user");
        
        hqlQuery.leftJoin("location.regions", "regions");
        hqlQuery.leftJoin("location.metadata", "metadata");
        
        hqlQuery.leftJoin("locAttributeVal.attribute", "locAttribute");
        hqlQuery.leftJoin("location.surveys", "survey");
        for(Facet f : facetList) {
            if(f.isActive()) {
                Predicate p = f.getPredicate();
                if (p != null) {
                    f.applyCustomJoins(hqlQuery);
                    hqlQuery.and(p);
                }
            }
        }
        
        if(surveyId != null) {
            hqlQuery.and(Predicate.eq("survey.id", surveyId));
        }
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
    
    private static void writeKML(ZipOutputStream zos, Session sesh, String contextPath, User user, ScrollableResults<Location> sc) throws JAXBException {
        int recordCount = 0;
        List<Location> locList = new ArrayList<Location>(ScrollableResults.RESULTS_BATCH_SIZE);
        KMLWriter writer = KMLUtils.createKMLWriter(contextPath, null);
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
}
