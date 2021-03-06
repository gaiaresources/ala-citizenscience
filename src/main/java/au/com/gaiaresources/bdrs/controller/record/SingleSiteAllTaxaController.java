package au.com.gaiaresources.bdrs.controller.record;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * The <code>SingleSiteAllTaxa</code> controller is a record add form renderer
 * that allows multiple sightings of all survey taxa to be created for a single
 * location.
 * 
 * @author stephanie
 */

@Controller
public class SingleSiteAllTaxaController extends SingleSiteController {
    
    public static final String SINGLE_SITE_ALL_TAXA_URL = "/bdrs/user/singleSiteAllTaxa.htm";
    public static final String SINGLE_SITE_ALL_TAXA_VIEW_NAME = "singleSiteAllTaxa";
    
    /**
     * Provides a the attributes for a single row representing a sighting which 
     * will be turned into a table showing all the survey species with enterable 
     * attributes on the client side. This view is typically invoked by an AJAX 
     * request.
     * 
     * @param request the browser request
     * @param response the server response
     * @param surveyId the primary key of the survey where the record shall be added.
     * @param sightingIndex the row index where 0 is the first row.
     * @return 
     */
    @RequestMapping(value = "/bdrs/user/singleSiteAllTaxa/sightingTableAllTaxa.htm", method = RequestMethod.GET)
    public ModelAndView ajaxGetSightingsTable(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) int surveyId,
                                    @RequestParam(value="sightingIndex", defaultValue="0") int sightingIndex) {
        return super.ajaxGetSightingsTable(request, response, surveyId, sightingIndex);
    }
    
    /**
     * Displays a blank form displaying inputs for the latitude, longitude,
     * date, time and notes as well as a table listing all the species for 
     * the survey with enterable attributes.
     * 
     * @param request the browser request
     * @param response the server response
     * @param surveyId the primary key of the survey where the record shall be added.
     * @return
     */
    @RequestMapping(value = SINGLE_SITE_ALL_TAXA_URL, method = RequestMethod.GET)
    public ModelAndView addAllSpeciesRecord(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) int surveyId,
                                    @RequestParam(value = BdrsWebConstants.PARAM_CENSUS_METHOD_ID, required = false, defaultValue = "0") Integer censusMethodId) {
        return addRecord(request, response, surveyId, SINGLE_SITE_ALL_TAXA_VIEW_NAME, censusMethodId);
    }
    
    /**
     * Saves multiple records for a single site. Site information that is used
     * for all records are specified via parameters. Record specific data such
     * as the sighted taxon will be retrieved from the request parameters. 
     * Record specific parameters are prefixed by the row index 
     * 
     * @param request the browser request
     * @param response the server response
     * @param surveyId the primary key of the survey where the record shall be added
     * @param latitude the latitude of the sighting
     * @param longitude the longitude of the sighting
     * @param date the calendar date of the sighting
     * @param time the time when the sighting occured
     * @param notes additional notes to be attached to all records
     * @param sightingIndex the number of records to be saved.
     * @param rowIds an Array of prefix ids for each row
     * @return 
     * @throws ParseException throws if the date cannot be parsed
     * @throws IOException thrown if uploaded files cannot be saved
     */
    @RolesAllowed( {Role.USER,"ROLE_STUDENT","ROLE_POWERSTUDENT","ROLE_TEACHER",Role.ADMIN, Role.POWERUSER, Role.SUPERVISOR} )
    @RequestMapping(value = SINGLE_SITE_ALL_TAXA_URL, method = RequestMethod.POST)
    public ModelAndView saveRecord(MultipartHttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) int surveyId,
                                    @RequestParam(value=SingleSiteController.PARAM_ROW_PREFIX, required=true, defaultValue="") String[] rowIds) throws ParseException, IOException {
        return saveRecordHelper(request, response, surveyId, rowIds);
    }
    
    @Override
    protected List<Record> modifyRecordDisplayList(List<Record> recordsForFormInstance, Survey survey) {
        
        List<IndicatorSpecies> speciesList = new ArrayList<IndicatorSpecies>();
        if (survey.getSpecies().isEmpty()) {
            // all species must be in the form.... 
            speciesList = taxaDAO.getIndicatorSpecies();
        } else {
            speciesList.addAll(survey.getSpecies());
        }
           
        for (Record r : recordsForFormInstance) {
            IndicatorSpecies recordSpecies = r.getSpecies();
            if (recordSpecies != null) {
                speciesList.remove(recordSpecies);
            }
        }
        
        List<Record> result = new ArrayList<Record>();
        result.addAll(recordsForFormInstance);
        
        // for any left over species, create records and append to list ...
        for (IndicatorSpecies leftOverSpecies: speciesList) {
            Record newRec = new Record();
            // make the survey available to help render form fields.
            newRec.setSurvey(survey);
            newRec.setSpecies(leftOverSpecies);
            result.add(newRec);
        }
        return result;
    }
    
    @Override
    protected boolean canSaveRecord(Integer count) {
        // To avoid saving an extremely large number of records, don't save 
        // records where the count isn't set
        return count != null;
    }
}