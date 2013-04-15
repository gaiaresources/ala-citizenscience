package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The Taxon Service provides a web API for Taxonomy and Taxonomy Group
 * based services.
 */
@Controller
public class TaxonomyService extends AbstractController {

    private Logger log = Logger.getLogger(getClass());
    @Autowired
    private TaxaDAO taxaDAO;

    @RequestMapping(value = "/webservice/taxon/searchTaxonGroup.htm", method = RequestMethod.GET)
    public void searchTaxonGroup(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        JSONArray array = new JSONArray();

        if(request.getParameter("q") != null) {
            List<TaxonGroup> taxonGroupList =
                taxaDAO.getTaxonGroupSearch(request.getParameter("q"));

            for(TaxonGroup group : taxonGroupList) {
                array.add(group.flatten());
            }
        }

        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    @RequestMapping(value = "/webservice/taxon/searchTaxon.htm", method = RequestMethod.GET)
    public void searchTaxon(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        JSONArray array = new JSONArray();

        if(request.getParameter("q") != null) {
            List<IndicatorSpecies> speciesList =
                taxaDAO.getIndicatorSpeciesByNameSearch(request.getParameter("q"), false);
            
            String depthStr = request.getParameter("depth");
            int depth = depthStr == null ? 0 : Integer.parseInt(depthStr);
            
            for(IndicatorSpecies species : speciesList) {
                array.add(species.flatten(depth));
            }
        }
        
        response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    @RequestMapping(value = "/webservice/taxon/getTaxonById.htm", method = RequestMethod.GET)
    public void getTaxonById(@RequestParam(value="id", defaultValue="0") int taxonPk,
                             @RequestParam(value="depth", defaultValue="0", required=false) int depth,
                             HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        if(taxonPk > 0) {
            String json = JSONObject.fromMapToString(taxaDAO.getIndicatorSpecies(taxonPk).flatten(depth));
            response.getWriter().write(json);
        }
    }
    
    @RequestMapping(value = "/webservice/taxon/getTaxonGroupById.htm", method = RequestMethod.GET)
    public void getTaxonGroupById(@RequestParam(value="id", defaultValue="0") int groupPk,
                            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        if(groupPk > 0) {
            String json = JSONObject.fromMapToString(taxaDAO.getTaxonGroup(groupPk).flatten());
            response.getWriter().write(json);
        }
    }
    
    @RequestMapping(value = "/webservice/taxon/getAllTaxonGroups.htm", method = RequestMethod.GET)
    public void getAllTaxonGroups(HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        JSONArray array = new JSONArray();
        for(TaxonGroup group : taxaDAO.getTaxonGroups()) {
            array.add(group);
        }
        
        response.getWriter().write(array.toString());
    }
    
    @RequestMapping(value = "/webservice/taxon/getSpeciesProfileById.htm", method = RequestMethod.GET)
    public void getSpeciesProfileById(@RequestParam(value="id", required=true) int profilePk,
                            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        String json = JSONObject.fromMapToString(taxaDAO.getSpeciesProfileById(profilePk).flatten());
        response.getWriter().write(json);
    }
    
    @RequestMapping(value = "/webservice/taxon/topSpecies.htm", method = RequestMethod.GET)
    public void getTopSpecies(@RequestParam(value="user", defaultValue="0") int userPk,
            @RequestParam(value="limit", defaultValue="5") int limit,
    		HttpServletRequest request, HttpServletResponse response)
    			throws IOException
    {
    	// RequestParam user - the user
    	// RequestParam limit - the number of species to return
    	// [{ species : {species}, count : <number> }, ... ]
    	JSONArray array = new JSONArray();
    	log.debug("Top Species");
    	try {
    	   	List<Pair<IndicatorSpecies, Integer>> counts = taxaDAO.getTopSpecies(userPk, limit);
    	    for (Pair<IndicatorSpecies, Integer> i : counts) {
    	    	JSONObject ob = new JSONObject();
    	    	ob.put("species", i.getFirst().flatten());
    	    	ob.put("count", i.getSecond());
    	    	array.add(ob);
    	    }
    	} catch (Exception e) {
    		log.error(e);
    	}
    	
    	response.setContentType("application/json");
        response.getWriter().write(array.toString());
    }

    /**
     * Returns JSON containing the taxa that:
     * <ol>
     *     <li>are in a Taxon Group that has a name containing the string supplied in the groupName parameter.</li>
     *     <li>have a common name or scientific name containing the sting supplied in the taxonName parameter.</li>
     * </ol>
     * The search is case insensitive.
     * @param groupName the search string to match against the Taxon Group name.
     * @param taxonName the search string to match against the Taxon scientific or common name.
     * @return taxa matching the search parameters in JSON format.
     */
    @RequestMapping(value = "/webservice/taxon/searchTaxaByTaxonGroupName", method = RequestMethod.GET)
    public void searchTaxaByTaxonGroupName(
            @RequestParam(value="groupName", required = true) String groupName,
            @RequestParam(value="taxonName", required = true) String taxonName,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        JSONArray array = new JSONArray();

        // The group name must be supplied, helps reduce the size of the query. The taxon name is allowed to be
        // empty to assist the auto-complete use case.
        if (StringUtils.notEmpty(groupName)) {
            try {
                List<IndicatorSpecies> taxa = taxaDAO.searchIndicatorSpeciesByGroupName(groupName, taxonName);
                for (IndicatorSpecies taxon : taxa) {
                    array.add(taxon.flatten());
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        writeJson(request, response, array.toString());
    }

    /**
     * Updates the primary group of the taxa identified by the supplied ids.
     * Additionally, the TaxonGroup will no longer be a secondary group of any of the supplied taxa.
     * @param taxonIds Identifies the IndicatorSpecies to update.
     * @param taxonGroupId The id of the new primary TaxonGroup for the identified species.
     * @param request the HTTP request to process
     * @param response the HTTP response.
     * @throws IOException if there is an error writing the response.
     */
    @RequestMapping(value = "/webservice/taxon/updatePrimaryGroup", method = RequestMethod.POST)
    public void reassignPrimaryGoup(
            @RequestParam(value="taxonId[]", required=true) int[] taxonIds,
            @RequestParam(value="taxonGroupId", required=true) int taxonGroupId,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        TaxonGroup group = taxaDAO.getTaxonGroup(taxonGroupId);
        List<Integer> idsList = Arrays.asList(ArrayUtils.toObject(taxonIds));
        taxaDAO.bulkUpdatePrimaryGroup(idsList, group);

        success(response);
    }

    /**
     * Adds the identified TaxonGroup to the list of configured secondary groups of the IndicatorSpecies identified
     * by the supplied array of ids.
     * IndicatorSpecies that have the supplied TaxonGroup as their primary group will not be updated.
     * @param taxonIds Identifies the IndicatorSpecies to update.
     * @param taxonGroupId The id of the new secondary TaxonGroup for the identified species.
     * @param request the HTTP request to process
     * @param response the HTTP response.
     * @throws IOException if there is an error writing the response.
     */
    @RequestMapping(value = "/webservice/taxon/addSecondaryGroup", method = RequestMethod.POST)
    public void addSecondaryGroup(
            @RequestParam(value="taxonId[]", required=true) int[] taxonIds,
            @RequestParam(value="taxonGroupId", required=true) int taxonGroupId,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        TaxonGroup group = taxaDAO.getTaxonGroup(taxonGroupId);
        List<Integer> idsList = Arrays.asList(ArrayUtils.toObject(taxonIds));
        taxaDAO.bulkAssignSecondaryGroup(idsList, group);

        success(response);
    }

    /**
     * Removes the identified TaxonGroup from the list of configured secondary groups of the IndicatorSpecies identified
     * by the supplied array of ids.
     * @param taxonIds Identifies the IndicatorSpecies to update.
     * @param taxonGroupId The id of the TaxonGroup to be removed from the identified IndicatorSpecies.
     * @param request the HTTP request to process
     * @param response the HTTP response.
     * @throws IOException if there is an error writing the response.
     */
    @RequestMapping(value = "/webservice/taxon/removeSecondaryGroup", method = RequestMethod.POST)
    public void removeSecondaryGroup(
            @RequestParam(value="taxonId[]", required=true) int[] taxonIds,
            @RequestParam(value="taxonGroupId", required=true) int taxonGroupId,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        TaxonGroup group = taxaDAO.getTaxonGroup(taxonGroupId);
        List<Integer> idsList = Arrays.asList(ArrayUtils.toObject(taxonIds));
        taxaDAO.bulkRemoveSecondaryGroup(idsList, group);

        success(response);
    }

    /**
     * Helper method that writes a JSON response of the form {success:"true"}
     * @param response the HTTP response to write the JSON to.
     * @throws IOException if there is an error writing to the HTTP response.
     */
    private void success(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JSONObject result = new JSONObject();
        result.put("success", "true");
        response.getWriter().write(result.toJSONString());
    }


}
