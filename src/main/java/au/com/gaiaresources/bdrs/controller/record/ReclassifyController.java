package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.review.sightings.AdvancedReviewSightingsController;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 7/01/14
 */

@Controller
public class ReclassifyController extends AbstractController {


    public static final String RECLASSIFY_URL = "/bdrs/user/reclassifyRecords.htm";
    public static final String PARAM_REDIRECT_URL = "redirecturl";
    public static final String DEFAULT_REDIRECT_URL = AdvancedReviewSightingsController.ADVANCED_REVIEW_URL;
    public static final String PARAM_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    public static final String PARAM_SPECIES_ID = "speciesId";

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private TaxaDAO taxaDAO;

    @SuppressWarnings("unchecked")
    @RequestMapping(value = RECLASSIFY_URL, method = {RequestMethod.POST})
    @RolesAllowed({Role.ROOT, Role.ADMIN})
    public String reclassifyRecords(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = PARAM_RECORD_ID, required = false)
            Integer[] recordIds,
            @RequestParam(value = PARAM_SPECIES_ID, required = false)
            Integer speciesId,
            @RequestParam(value = PARAM_REDIRECT_URL, required = false, defaultValue = DEFAULT_REDIRECT_URL)
            String redirectURL
    ) throws IOException {

        if (speciesId != null && recordIds != null) {
            IndicatorSpecies species = taxaDAO.getIndicatorSpecies(speciesId);
            if (species != null) {
                for (int id : recordIds) {
                    Record record = recordDAO.getRecord(id);
                    if (record != null) {
                        record.setSpecies(species);
                        recordDAO.saveRecord(record);
                    }
                }
                // Flush session before redirecting.
                // If this is not done the last record is not saved (bug ???)
                RequestContextHolder.getContext().getHibernate().flush();
            }
        }
        if (redirectURL == null){
            redirectURL = DEFAULT_REDIRECT_URL;
        }
        return "forward:" + redirectURL;
    }


    @RequestMapping(value = RECLASSIFY_URL, method = {RequestMethod.GET})
    public String reclassifyRecordsGet(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = PARAM_REDIRECT_URL, required = false, defaultValue = DEFAULT_REDIRECT_URL)
                    String redirectURL
    ) {
        if (redirectURL == null){
            redirectURL = DEFAULT_REDIRECT_URL;
        }
        return "forward:" + redirectURL;
    }
}
