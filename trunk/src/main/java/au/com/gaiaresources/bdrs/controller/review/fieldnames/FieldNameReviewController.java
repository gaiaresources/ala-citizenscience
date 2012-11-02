package au.com.gaiaresources.bdrs.controller.review.fieldnames;

import java.util.Collections;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Provides an easy to use interface for reviewing and editing Field Species
 * for a given survey.
 *
 */
@Controller
public class FieldNameReviewController extends AbstractController {

    public static final String GET_RECORD_FIELD_NAMES_URL = "/review/fieldnames/showFieldNames.htm";

    public static final String MV_RECORDS = "records";

    public static final String FIELD_NAMES_REVIEW_VIEW = "fieldNameReview";

    public static final String MV_FIELD_NAME_ATTR = "fieldNameAttr";
    
    public static final String MV_SURVEY_ID = "surveyId";
    
    public static final String MV_URI = "uri";
    
    public static final String MV_SURVEYS = "surveys";

    /**
     * Identifies the row
     */
    public static final String PARAM_ROW_IDX = "rowIdx";
    /**
     * The replaced parameter is the row id
     */
    public static final String PARAM_RECORD_ID_TEMPLATE = "rec_id_%d";
    /**
     * The replaced parameter is the row id
     */
    public static final String PARAM_SCI_NAME_TEMPLATE = "sci_name_%d";
    /**
     * The replaced parameter is the row id
     */
    public static final String PARAM_SPECIES_ID_TEMPLATE = "species_id_%d";
    
    public static final String MSG_KEY_BAD_SPECIES_ID = "bdrs.review.fieldnames.badSpeciesId";
    public static final String MSG_KEY_BAD_SURVEY_ID = "bdrs.review.fieldnames.badSurveyId";
    public static final String MSG_KEY_BAD_RECORD_ID = "bdrs.review.fieldnames.badRecordId";
    public static final String MSG_KEY_MULTI_SPECIES_FOR_NAME = "bdrs.review.fieldnames.multiSpeciesForName";
    public static final String MSG_KEY_ZERO_SPECIES_FOR_NAME = "bdrs.review.fieldnames.zeroSpeciesForName";
    public static final String MSG_KEY_NOTHING_TO_SAVE = "bdrs.review.fieldnames.nothingToSave";

    @Autowired
    private RecordDAO recDAO;
    @Autowired
    private TaxaService taxaService;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private SurveyDAO surveyDAO;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Displays field names
     * 
     * @param request HttpRequest
     * @param response HttpResponse
     * @param surveyId Field species will be shown for this survey.
     * @return Standard model and view.
     */
    @RequestMapping(value = GET_RECORD_FIELD_NAMES_URL, method = RequestMethod.GET)
    @RolesAllowed({ Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN })
    public ModelAndView getForm(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, required = false) Integer surveyId) {

        User user = getRequestContext().getUser();
        if (user == null) {
            throw new IllegalStateException("User cannot be null");
        }
        
        Attribute fieldNameAttr = taxaService.getFieldNameAttribute();
        List<Record> fieldNameRecords = Collections.EMPTY_LIST;
        if (surveyId != null) {
            fieldNameRecords = recDAO.getFieldNameRecords(null, user.getId(), surveyId, taxaService);
        }
        
        List<Survey> surveys = surveyDAO.getActiveSurveysForUser(user);

        ModelAndView mv = new ModelAndView(FIELD_NAMES_REVIEW_VIEW);
        mv.addObject(MV_RECORDS, fieldNameRecords);
        mv.addObject(MV_SURVEYS, surveys);
        mv.addObject(MV_FIELD_NAME_ATTR, fieldNameAttr);
        mv.addObject(MV_SURVEY_ID, surveyId);
        mv.addObject(MV_URI, GET_RECORD_FIELD_NAMES_URL);

        return mv;
    }

    /**
     * Saves any changes to field species for records. Will give appropriate error
     * reporting if the species cannot be found in the database.
     * 
     * @param request HttpRequest
     * @param response HttpResponse
     * @param surveyId Survey to save field species for.
     * @param rowIdxArray Request parameter - array of integers required for parsing species ids etc.
     * @return ModelAndView - will redirect to the GET method.
     */
    @RequestMapping(value = GET_RECORD_FIELD_NAMES_URL, method = RequestMethod.POST)
    @RolesAllowed({ Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN })
    public ModelAndView saveForm(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = BdrsWebConstants.PARAM_SURVEY_ID, required = true) Integer surveyId,
            @RequestParam(value = PARAM_ROW_IDX) int[] rowIdxArray) {

        User user = getRequestContext().getUser();
        if (user == null) {
            throw new IllegalStateException("User cannot be null");
        }
        
        Survey s = surveyDAO.getSurvey(surveyId);
        if (s == null) {
            log.error("invalid survey for id : " + surveyId);
            getRequestContext().addMessage("bdrs.review.fieldnames.badSurveyId");
            return redirect(GET_RECORD_FIELD_NAMES_URL);
        }
        
        boolean nonEmptyEntry = false;

        if (rowIdxArray != null) {
            for (int rowIdx : rowIdxArray) {
                Integer recId = Integer.valueOf(request.getParameter(String.format(PARAM_RECORD_ID_TEMPLATE, rowIdx)));
                Integer speciesId = null;
                String speciesIdString = request.getParameter(String.format(PARAM_SPECIES_ID_TEMPLATE, rowIdx));
                if (StringUtils.hasLength(speciesIdString)) {
                    speciesId = Integer.valueOf(speciesIdString);
                }
                
                String sciName = request.getParameter(String.format(PARAM_SCI_NAME_TEMPLATE, rowIdx));
                Record r = recDAO.getRecord(recId);
                if (r != null) {
                    if (speciesId != null) {
                        nonEmptyEntry = true;
                        IndicatorSpecies species = taxaDAO.getIndicatorSpecies(speciesId);
                        if (species != null) {
                            setRecordSpecies(r, species);
                        } else {
                            // cannot update record - bad species ID
                            log.error("invalid species for id : " + speciesId);
                            getRequestContext().addMessage(MSG_KEY_BAD_SPECIES_ID);
                        }
                    } else {
                        if (StringUtils.hasLength(sciName)) {
                            nonEmptyEntry = true;
                            IndicatorSpecies species = taxaDAO.getSpeciesForSurvey(null, s, sciName);
                            if (species == null) {
                                // cannot update record, no species returned for sci name string
                                getRequestContext().addMessage(MSG_KEY_ZERO_SPECIES_FOR_NAME, new Object[] { sciName });
                            } else {
                                // exactly one record has been returned.
                                setRecordSpecies(r, species);
                            }
                        }
                    }
                } else {
                    log.error("Could not retrieve record with id : " + recId);
                    getRequestContext().addMessage(MSG_KEY_BAD_RECORD_ID);
                }
            }
        }
        
        if (!nonEmptyEntry) {
            getRequestContext().addMessage(MSG_KEY_NOTHING_TO_SAVE);
        }
        
        // show remaining records with field names.
        ModelAndView mv = redirect(GET_RECORD_FIELD_NAMES_URL);
        mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, surveyId);
        return mv;
    }
    
    private void setRecordSpecies(Record r, IndicatorSpecies species) {
        r.setSpecies(species);
        recDAO.save(r);
    }
}
