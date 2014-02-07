package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.review.sightings.AdvancedReviewSightingsController;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.facet.FacetService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 7/01/14
 */

@Controller
public class ReclassifyController extends AbstractController {

    public static final Logger log = Logger.getLogger(ReclassifyController.class);

    public static final String RECLASSIFY_URL = "/bdrs/user/reclassifyRecords.htm";

    public static final String PARAM_REDIRECT_URL = "redirecturl";
    public static final String DEFAULT_REDIRECT_URL = AdvancedReviewSightingsController.ADVANCED_REVIEW_URL;
    public static final String PARAM_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    public static final String PARAM_SPECIES_ID = "speciesId";
    public static final String PARAM_MASS_RECLASSIFY = "massReclassify";
    public static final String DEFAULT_MASS_RECLASSIFY = "false";


    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private FacetService facetService;


    /**
     * The post method: That's were the reclassify is done.
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = RECLASSIFY_URL, method = {RequestMethod.POST})
    @RolesAllowed({Role.ROOT, Role.ADMIN})
    public String reclassifyRecordsPost(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = PARAM_SPECIES_ID, required = true)
            Integer speciesId,
            @RequestParam(value = PARAM_RECORD_ID, required = false)
            Integer[] recordIds,
            @RequestParam(value = PARAM_MASS_RECLASSIFY, required = false, defaultValue = DEFAULT_MASS_RECLASSIFY)
            boolean massReclassify,
            @RequestParam(value = PARAM_REDIRECT_URL, required = false, defaultValue = DEFAULT_REDIRECT_URL)
            String redirectURL
    ) throws IOException {
        if (speciesId != null) {
            IndicatorSpecies species = taxaDAO.getIndicatorSpecies(speciesId);
            if (species != null) {
                List<Record> records = new ArrayList<Record>(0);
                if (recordIds != null) {
                    records = getRecordsFromIds(recordIds);
                } else {
                    if (massReclassify) {
                        records = getMatchingRecords(request);
                        if (records.size() > 0) {
                            log.warn("Mass reclassify! " +
                                    records.size() + " records to reclassify into " + speciesId +
                                    "(" + species.getScientificName() + ")");
                        }
                    } else {
                        log.warn("Reclassify: No recordIds and no massReclassify flag");
                    }
                }
                if (records.size() > 0) {
                    doReclassify(records, species);
                } else {
                    log.warn("Reclassify: No records were matching the request");
                }
            }
        } else {
            log.warn("Reclassify: No species id specified");
        }
        if (redirectURL == null) {
            redirectURL = DEFAULT_REDIRECT_URL;
        }
        return "forward:" + redirectURL;
    }

    /**
     * The Get method: do nothing but forward to the Advanced Review controller
     * or the given redirect param if any
     *
     * @param request     the http req
     * @param response    the http resp
     * @param redirectURL the redirect URL (not required)
     * @return the forward url for Spring routing
     */
    @RequestMapping(value = RECLASSIFY_URL, method = {RequestMethod.GET})
    public String reclassifyRecordsGet(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = PARAM_REDIRECT_URL, required = false, defaultValue = DEFAULT_REDIRECT_URL)
            String redirectURL) {
        if (redirectURL == null) {
            redirectURL = DEFAULT_REDIRECT_URL;
        }
        return "forward:" + redirectURL;
    }


    private void doReclassify(List<Record> records, IndicatorSpecies species) {
        for (Record record : records) {
            record.setSpecies(species);
            recordDAO.saveRecord(record);
        }
        // Flush session before redirecting.
        // If this is not done the last record is not saved (bug ???)
        RequestContextHolder.getContext().getHibernate().flush();
    }

    private List<Record> getRecordsFromIds(Integer[] ids) {
        List<Record> result = new ArrayList<Record>(ids.length);
        for (Integer id : ids) {
            Record record = recordDAO.getRecord(id);
            if (record != null) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * For mass reclassify we obtain the records from the facet service
     *
     * @return List of records matching the facets.
     */
    private List<Record> getMatchingRecords(HttpServletRequest request) {
        return facetService.getMatchingRecords(request);
    }

}
