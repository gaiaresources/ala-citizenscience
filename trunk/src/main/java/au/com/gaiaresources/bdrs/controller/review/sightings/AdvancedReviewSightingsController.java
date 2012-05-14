package au.com.gaiaresources.bdrs.controller.review.sightings;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController;
import au.com.gaiaresources.bdrs.db.ScrollableResults;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery.SortOrder;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.kml.KMLWriter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportCapability;
import au.com.gaiaresources.bdrs.model.report.impl.ReportView;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.LocationFacet;
import au.com.gaiaresources.bdrs.service.facet.SurveyFacet;
import au.com.gaiaresources.bdrs.service.facet.record.RecordSurveyFacet;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.KMLUtils;
import au.com.gaiaresources.bdrs.util.StringUtils;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Provides view controllers for the Facet, Map and List view of the ALA
 * 'My Sightings' Page. 
 */
@SuppressWarnings("unchecked")
@Controller
public class AdvancedReviewSightingsController extends AdvancedReviewController<Record> {
    
    /**
     * Report URL.
     */
    public static final String  ADVANCED_REVIEW_REPORT_URL = "/review/sightings/advancedReviewReport.htm";

    /**
     * Parameter for latest record.
     */
    public static final String LATEST_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    
    /**
     * Set of Strings that are valid sorting parameters for the database query.
     */
    public static final Set<String> VALID_SORT_PROPERTIES;
    
    static {
        Set<String> temp = new HashSet<String>();
        temp.add("record.when");
        temp.add("species.scientificName");
        temp.add("species.commonName"); 
        temp.add("location.name");
        temp.add("censusMethod.type");
        temp.add("record.user");
        VALID_SORT_PROPERTIES = Collections.unmodifiableSet(temp);
    }
    
    /**
     * Parameter for report id in query.
     */
    public static final String QUERY_PARAM_REPORT_ID = "reportId";
    
    private Logger log = Logger.getLogger(getClass());

    /**
     * Provides a view of the facet listing and a skeleton of the map or list
     * view. The map or list view will populate itself via asynchronous 
     * javascript requests. 
     */
    @RequestMapping(value = "/review/sightings/advancedReview.htm", method = RequestMethod.GET)
    public ModelAndView advancedReview(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                       @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) {

        configureHibernateSession();
     
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        // some locations have been selected, add them to the parameters as facet selections
        String locations = request.getParameter("locations");
        
        // this code translates the locations parameter into facet selections
        // there is currently not a good way to get the input name (parameter 
        // selection name) from a facet before it's creation or to build a mock
        // facet for retrieving this parameter so the input name is hard-coded 
        // here
        String inputName = "0_"+LocationFacet.QUERY_PARAM_NAME+LocationFacet.OPTION_SUFFIX;
        if (!StringUtils.nullOrEmpty(locations) && !newParamMap.containsKey(inputName)) {
            newParamMap.put(inputName, locations.split(","));
        }
        List<Facet> facetList = facetService.getFacetList(currentUser(), newParamMap);
        Long recordCount = countMatchingRecords(facetList,
                                                surveyId,
                                                request.getParameter(SEARCH_QUERY_PARAM_NAME));
        ModelAndView mv = getAdvancedReviewView(request, surveyId, resultsPerPage, pageNumber, facetList, recordCount, "advancedReviewRecords");
        // Add an optional parameter to set a record to highlight
        if (!StringUtils.nullOrEmpty(request.getParameter(LATEST_RECORD_ID))) {
            mv.addObject(LATEST_RECORD_ID, request.getParameter(LATEST_RECORD_ID));
        }
        
        return mv;
    }
    
    /**
     * Returns the list of records matching the {@link Facet} criteria as KML.
     */
    @RequestMapping(value = "/review/sightings/advancedReviewKMLSightings.htm", method = RequestMethod.GET)
    public void advancedReviewKMLSightings(HttpServletRequest request, HttpServletResponse response) throws IOException, JAXBException {

        configureHibernateSession();

        Integer surveyId = null;
        if(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        // some locations have been selected, add them to the parameters as facet selections
        String locations = request.getParameter("locations");
        if (!StringUtils.nullOrEmpty(locations)) {
            String inputName = "0_"+LocationFacet.QUERY_PARAM_NAME;
            log.debug("putting locations to paramMap: "+locations);
            newParamMap.put(inputName, locations.split(","));
        }
        List<Facet> facetList = facetService.getFacetList(currentUser(), newParamMap);
        
        ScrollableResults<Record> sr = getScrollableResults(facetList, surveyId, 
                                                                     request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                                     request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                                     request.getParameter(SEARCH_QUERY_PARAM_NAME));
        advancedReviewKMLSightings(request, response, facetList, sr);
    }
    
    /**
     * Returns a JSON array of records matching the {@link Facet} criteria.
     */
    @RequestMapping(value = "/review/sightings/advancedReviewJSONSightings.htm", method = RequestMethod.GET)
    public void advancedReviewJSONSightings(HttpServletRequest request, 
                                            HttpServletResponse response,
                                            @RequestParam(value=RESULTS_PER_PAGE_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_RESULTS_PER_PAGE) Integer resultsPerPage,
                                            @RequestParam(value=PAGE_NUMBER_QUERY_PARAM_NAME, required=false, defaultValue=DEFAULT_PAGE_NUMBER) Integer pageNumber) throws IOException {
        configureHibernateSession();
        
        Integer surveyId = null;
        if(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME) != null) {
            surveyId = Integer.parseInt(request.getParameter(SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME));
        }
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        // some locations have been selected, add them to the parameters as facet selections
        String locations = request.getParameter("locations");
        if (!StringUtils.nullOrEmpty(locations)) {
            String inputName = "0_"+LocationFacet.QUERY_PARAM_NAME;
            log.debug("putting locations to paramMap: "+locations);
            newParamMap.put(inputName, locations.split(","));
        }
        
        List<Facet> facetList = facetService.getFacetList(currentUser(), newParamMap);
        ScrollableResults<Record> sc = getScrollableResults(facetList,
                                                                     surveyId,
                                                                     request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                                     request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                                     request.getParameter(SEARCH_QUERY_PARAM_NAME),
                                                                     pageNumber, resultsPerPage);
        advancedReviewJSONSightings(request, response, facetList, sc);
    }
    
    /**
     * Returns an XLS representation of representation of records matching the
     * {@link Facet} criteria. This function should only be used if the records
     * are part of a single survey.
     * @throws Exception 
     */
    @RequestMapping(value = "/review/sightings/advancedReviewDownload.htm", method = RequestMethod.GET)
    public void advancedReviewDownload(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       @RequestParam(value=SurveyFacet.SURVEY_ID_QUERY_PARAM_NAME, required=false) Integer surveyId,
                                       @RequestParam(value=QUERY_PARAM_DOWNLOAD_FORMAT, required=true) String[] downloadFormat) throws Exception {
        configureHibernateSession();
        
        User currentUser = currentUser();
        HashMap<String, String[]> newParamMap = new HashMap<String, String[]>(request.getParameterMap());
        // some locations have been selected, add them to the parameters as facet selections
        String locations = request.getParameter("locations");
        if (!StringUtils.nullOrEmpty(locations)) {
            String inputName = "0_"+LocationFacet.QUERY_PARAM_NAME;
            newParamMap.put(inputName, locations.split(","));
        }
        
        List<Facet> facetList = facetService.getFacetList(currentUser, newParamMap);
        
        SurveyFacet surveyFacet = facetService.getFacetByType(facetList, RecordSurveyFacet.class);
        
        // list of surveys to download
        List<Survey> surveyList = surveyFacet.getSelectedSurveys();
               
        // In the case that no surveys are selected to filter by - we will use
        // all the surveys available for the accessing user
        if (surveyList.isEmpty()) {
            surveyList = surveyDAO.getActiveSurveysForUser(currentUser);
        }
        
        // I think 'surveyId' is not used for AdvancedReview but is used for MySightings
        ScrollableResults<Record> sc = getScrollableResults(facetList,
                                                     surveyId,
                                                     request.getParameter(SORT_BY_QUERY_PARAM_NAME), 
                                                     request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                                                     request.getParameter(SEARCH_QUERY_PARAM_NAME));
        
        downloadSightings(request, response, downloadFormat, sc, surveyList);
    }

    /**
     * Creates a scrollable records instance from the facet selection and renders the specified report using the
     * scrollable records.
     *
     * @param request  the client request.
     * @param response the server response.
     * @param reportId the primary key of the report to be run.
     * @return the report view.
     * @throws Exception thrown if there is an error running the report.
     */
    @RequestMapping(value = ADVANCED_REVIEW_REPORT_URL, method = RequestMethod.GET)
    public ModelAndView advancedReviewReport(HttpServletRequest request,
                                             HttpServletResponse response,
                                             @RequestParam(value = QUERY_PARAM_REPORT_ID, required = true) int reportId) throws Exception {
        configureHibernateSession();

        User currentUser = currentUser();
        List<Facet> facetList = facetService.getFacetList(currentUser, (Map<String, String[]>) request.getParameterMap());

        Report report = reportDAO.getReport(reportId);

        ScrollableResults<Record> sc = getScrollableResults(facetList,
                null,
                request.getParameter(SORT_BY_QUERY_PARAM_NAME),
                request.getParameter(SORT_ORDER_QUERY_PARAM_NAME),
                request.getParameter(SEARCH_QUERY_PARAM_NAME));

        return reportService.renderReport(request, response, report, sc);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#getCountSelect()
     */
    protected String getCountSelect() {
        return "select count(distinct record) from Record record";
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#applyFacetsToQuery(au.com.gaiaresources.bdrs.db.impl.HqlQuery, java.util.List, java.lang.Integer, java.lang.String)
     */
    protected void applyFacetsToQuery(HqlQuery hqlQuery, List<Facet> facetList,
            Integer surveyId, String searchText) {

        hqlQuery.leftJoin("record.location", "location");
        hqlQuery.leftJoin("location.attributes", "locAttribute");
        
        hqlQuery.leftJoin("record.species", "species");
        hqlQuery.leftJoin("record.censusMethod", "censusMethod");
        
        hqlQuery.leftJoin("record.attributes", "recordAttributeVal");
        hqlQuery.leftJoin("recordAttributeVal.attribute", "recordAttribute");
        
        for(Facet f : facetList) {
            if(f.isActive()) {
                Predicate p = f.getPredicate();
                if (p != null) {
                    f.applyCustomJoins(hqlQuery);
                    hqlQuery.and(p);
                }
            }
        }
        
        if(searchText != null && !searchText.isEmpty()) {
            Predicate searchPredicate = Predicate.ilike("record.notes", String.format("%%%s%%", searchText));
            searchPredicate.or(Predicate.ilike("species.scientificName", String.format("%%%s%%", searchText)));
            searchPredicate.or(Predicate.ilike("species.commonName", String.format("%%%s%%", searchText)));
            
            hqlQuery.and(searchPredicate);
        }
        
        if(surveyId != null) {
            hqlQuery.and(Predicate.eq("record.survey.id", surveyId));
        }
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#createFacetQuery(java.util.List, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String)
     */
    protected Query createFacetQuery(List<Facet> facetList, Integer surveyId, String sortProperty, String sortOrder, String searchText) {
        // extra columns in select are used for ordering
        HqlQuery hqlQuery = new HqlQuery("select distinct record, species.scientificName, species.commonName, location.name, censusMethod.type from Record record");
        
        applyFacetsToQuery(hqlQuery, facetList, surveyId, searchText);

        // NO SQL injection for you
        if(sortProperty != null && sortOrder != null && VALID_SORT_PROPERTIES.contains(sortProperty)) {
            hqlQuery.order(sortProperty, 
                           SortOrder.valueOf(sortOrder).name(),
                           null);
        }
        return toHibernateQuery(hqlQuery);  
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#writeKMLResults(au.com.gaiaresources.bdrs.kml.KMLWriter, au.com.gaiaresources.bdrs.model.user.User, java.lang.String, java.util.List)
     */
    @Override
    protected void writeKMLResults(KMLWriter writer, User currentUser,
            String contextPath, List<Record> rList) {
        KMLUtils.writeRecords(writer, currentUser, contextPath, rList);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#getDefaultSortString()
     */
    @Override
    protected String getDefaultSortString() {
        return "record.when";
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController#getReportList()
     */
    @Override
    protected List<Report> getReportList() {
        return reportDAO.getReports(ReportCapability.SCROLLABLE_RECORDS, ReportView.ADVANCED_REVIEW);
    }

    @Override
    protected String getKMLFolderName() {
        return KMLUtils.KML_RECORD_FOLDER;
    }
}
