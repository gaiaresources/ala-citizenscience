package au.com.gaiaresources.bdrs.controller.fieldguide;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataBuilder;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataHelper;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataRow;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.SortOrder;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import org.apache.log4j.Logger;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;

/**
 * The <code>BDRSFieldGuide</code> controller is a taxongroup and taxa add form renderer
 * that allows multiple sightings of differing taxa to be created for a single
 * location.
 */

@Controller
public class BDRSFieldGuideController  extends AbstractController {

    private static final String TAXA_LISTING_TABLE_ID = "fieldGuideTaxaListingTable";
    private static final int TAXA_LISTING_PAGE_SIZE = 50;
    private static final String  FIELDGUIDE_GROUPS_URL= "/fieldguide/groups.htm";
    public static final String FIELGUIDE_LIST_TAXA_URL = "/fieldguide/listTaxa.htm";
    public static final String FIELDGUIDE_TAXA_URL = "/fieldguide/taxa.htm";
    
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());
    @Autowired
    private TaxaDAO taxaDAO;
    private ParamEncoder taxonListingParamEncoder = new ParamEncoder(TAXA_LISTING_TABLE_ID);
    
    /**
     * Displays a gridview of taxongroups.
     * 
     * @param request the browser request
     * @param response the server response
     * @return
     */
    @RequestMapping(value = FIELDGUIDE_GROUPS_URL, method = RequestMethod.GET)
    public ModelAndView listGroups(  HttpServletRequest request,
                                HttpServletResponse response) {
        
        ModelAndView mv = new ModelAndView("fieldGuideGroupListing");
        mv.addObject("taxonGroups", taxaDAO.getTaxonGroups());
        return mv;
    }
    
    /**
     * Displays a listview of taxa found by using the query parameter or the taxongroup id.
     * 
     * @param request the browser request
     * @param response the server response
     * @param searchInGroups the query parameter
     * @param groupId the taxongroup id
     * @return
     * @throws NullPointerException
     * @throws ParseException
     */
    @RequestMapping(value = FIELDGUIDE_TAXA_URL, method = RequestMethod.GET)
    public ModelAndView listTaxa(  HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value="search_in_groups",  required=false) String searchInGroups,
                                @RequestParam(value="groupId", required=false) Integer groupPk) throws NullPointerException, ParseException {
        
        ModelAndView mv = new ModelAndView("fieldGuideTaxaListing");
        
        //Creates a paginationFilter
        String pnArg = request.getParameter(getTaxonPageNumberParamName());
        int pageNum = pnArg != null && !pnArg.isEmpty() ? Integer.parseInt(pnArg) : 1;
        pageNum = pageNum < 1 ? 1 : pageNum;
        int start = (pageNum - 1) * TAXA_LISTING_PAGE_SIZE;
        PaginationFilter filter = new PaginationFilter(start, TAXA_LISTING_PAGE_SIZE);
        
        //Adds sorting criteria to filter
        if (StringUtils.hasLength(request.getParameter(getTaxonSortParamName()))
                && StringUtils.hasLength(request.getParameter(getTaxonOrderParamName()))) {
            
            String sortArg = request.getParameter(getTaxonSortParamName());
            String sortOrder = request.getParameter(getTaxonOrderParamName());
            
            filter.addSortingCriteria(sortArg, SortOrder.fromString(sortOrder));
        }
        
        if(groupPk != null) {
            TaxonGroup taxonGroup = taxaDAO.getTaxonGroup(groupPk);
            mv.addObject("taxonGroup", taxonGroup);
            mv.addObject("searchResultHeader", taxonGroup.getName());
        } else if(searchInGroups != null && StringUtils.hasLength(searchInGroups)) {
        mv.addObject("groupsQuery", searchInGroups);
            mv.addObject("searchResultHeader", "Search results for \"" + searchInGroups + "\"");
        }
        try {
            PagedQueryResult<IndicatorSpecies> pq = taxaDAO.searchTaxa(groupPk, TaxaDAO.TaxonGroupSearchType.PRIMARY_OR_SECONDARY, searchInGroups, null, filter);
            mv.addObject("taxaPaginator", pq);
        } catch (org.apache.lucene.queryParser.ParseException e) {
            log.error("Error finding taxa", e);
        }
        return mv;
    }
    
    /**
     * Returns a list of taxa based upon the query parameters. 
     * This function is typically invoked by an AJAX request.
     * 
     * @param request the browser request
     * @param response the server response
     * @param searchInGroups the query parameter
     * @param searchInResult the 'AND' query parameter
     * @param groupId the taxongroup id
     * @throws Exception
     */
    @RequestMapping(value = FIELGUIDE_LIST_TAXA_URL, method = RequestMethod.GET)
    public void asyncListTaxa(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value="search_in_groups",  required=false) String searchInGroups,
                                @RequestParam(value="search_in_result", required=false) String searchInResult,
                                @RequestParam(value="groupId", required=false) Integer groupId,
                                @RequestParam(value="primaryGroupOnly", required=false, defaultValue = "false") boolean primaryGroupOnly) throws Exception {
        
        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);       
        PaginationFilter filter = jqGridHelper.createFilter(request);
        TaxaDAO.TaxonGroupSearchType searchType = TaxaDAO.TaxonGroupSearchType.PRIMARY_OR_SECONDARY;
        if (primaryGroupOnly) {
            searchType = TaxaDAO.TaxonGroupSearchType.PRIMARY;
        }
        PagedQueryResult<IndicatorSpecies> queryResult = taxaDAO.searchTaxa(groupId, searchType, searchInGroups, searchInResult, filter);
        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());

        if (queryResult.getCount() > 0) {
            for (IndicatorSpecies species : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(species.getId());
                row
                .addValue("scientificName", species.getScientificName())
                .addValue("commonName", species.getCommonName())
                .addValue("thumbnail", getSpeciesThumbnail(species));
                builder.addRow(row);
            }
        }
        
        response.setContentType("application/json");
        response.getWriter().write(builder.toJson());
    }
    
    /**
     * Returns the uuid of the first thumbnail found in the profile of the species or an empty string when none is found.
     * 
     * @param species the indicatorspecies
     * @return
     */
    private String getSpeciesThumbnail(IndicatorSpecies species) {
        SpeciesProfile imgProfile = null;
        boolean isThumbnail = false;
        for(SpeciesProfile profile : species.getInfoItems()) {
            // use the first image found or first thumbnail found
            if((imgProfile == null && profile.isImgType()) || (!isThumbnail && profile.isThumbnailType())) {
                imgProfile = profile;
                isThumbnail = profile.isThumbnailType();
            }
        }
        
        if(imgProfile != null) {
            return imgProfile.getContent();
        } else {
            return "";
        }
    }

    @RequestMapping(value = "/fieldguide/taxon.htm", method = RequestMethod.GET)
    public ModelAndView viewTaxon(  HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value="id", required=true) int taxonPk) {
        ModelAndView mv = new ModelAndView("fieldGuideViewTaxon");
        mv.addObject("taxon", taxaDAO.getIndicatorSpecies(taxonPk));
        return mv;
    }
    
    public String getTaxonPageNumberParamName() {
        return taxonListingParamEncoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
    }

    public String getTaxonSortParamName() {
        return taxonListingParamEncoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
    }

    public String getTaxonOrderParamName() {
        return taxonListingParamEncoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
    }
}
