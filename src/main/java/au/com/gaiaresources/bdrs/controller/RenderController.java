package au.com.gaiaresources.bdrs.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.gaiaresources.bdrs.controller.customform.CustomFormController;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.record.AtlasController;
import au.com.gaiaresources.bdrs.controller.record.SingleSiteAllTaxaController;
import au.com.gaiaresources.bdrs.controller.record.SingleSiteMultiTaxaController;
import au.com.gaiaresources.bdrs.controller.record.TrackerController;
import au.com.gaiaresources.bdrs.controller.record.YearlySightingsController;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

@Controller
public class RenderController extends AbstractController {
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private PreferenceDAO prefsDAO;

    private Logger log = Logger.getLogger(this.getClass());

    public static final String SURVEY_RENDER_REDIRECT_URL = "/bdrs/user/surveyRenderRedirect.htm";
    public static final String PARAM_RECORD_ID = BdrsWebConstants.PARAM_RECORD_ID;
    public static final String PARAM_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;
    public static final String PARAM_COMMENT_ID = BdrsWebConstants.PARAM_COMMENT_ID;
    public static final String PARAM_REDIRECT_URL = "redirectUrl";
    
    /** The prefix of the page anchors that identify the position of comments on the Record page */
    private static final String COMMENT_ANCHOR_PREFIX = "#comment";

    /**
     * Redirects the request to the appropriate survey renderer depending upon
     * the <code>SurveyFormRendererType</code> of the survey.
     * 
     * @param request the http request.
     * @param response the http response.
     * @param recordId the ID of the Record to be displayed.
     * @param surveyPk the primary key of the survey in question.
     * @param commentId identifies the comment that should be displayed (this is done by specifying an anchor).
     * @return redirected view to the survey renderer.
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = SURVEY_RENDER_REDIRECT_URL, method = RequestMethod.GET)
    public ModelAndView surveyRendererRedirect(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value=PARAM_RECORD_ID, required=false) String recordId,
            @RequestParam(value=PARAM_SURVEY_ID, required=false) String surveyPk,
            @RequestParam(value=PARAM_COMMENT_ID, required=false) String commentId,
            @RequestParam(value=PARAM_REDIRECT_URL, required=false) String redirectURL) {

        int surveyId = 0;
        
        // if there is a record id present we shouldn't need to look for the
        // survey id separately.
        if (StringUtils.hasLength(recordId)) {
            try {
                Integer recId = Integer.valueOf(recordId);
                Record rec = recordDAO.getRecord(recId);
                if (rec != null && rec.getSurvey() != null && rec.getSurvey().getId() != null) {
                    surveyId = rec.getSurvey().getId().intValue();
                }
            } catch (NumberFormatException nfe) {
                log.error("Invalid record requested. recordId : " + request.getParameter(PARAM_RECORD_ID));
            }
        } 

	    if (surveyId == 0) {
            try {
                surveyId = Integer.parseInt(surveyPk);
            } catch (NumberFormatException nfe) {
                try {
                    log.debug("Default is : "
                            + prefsDAO.getPreferenceByKey("survey.default").getValue());
                    surveyId = Integer.parseInt(prefsDAO.getPreferenceByKey("survey.default").getValue());
                } catch (Exception e) {
                    // Either preference isn't set (nullpointer) or it's not an integer (numberformatexception)
                    try {
                        log.error("Default survey is incorrectly configured");
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ioe) {
                        log.error(ioe.getMessage(), ioe);
                    }
                    return null;
                }
            }   
		}

        Survey survey = surveyDAO.getSurvey(surveyId);
        if(redirectURL == null) {
            if(survey.getCustomForm() == null) {
                SurveyFormRendererType renderer = survey.getFormRendererType();
                renderer = renderer == null ? SurveyFormRendererType.DEFAULT : renderer;

                switch (renderer) {
                case YEARLY_SIGHTINGS:
                    redirectURL = YearlySightingsController.YEARLY_SIGHTINGS_URL;
                    break;
                case SINGLE_SITE_MULTI_TAXA:
                case SINGLE_SITE_MULTI_TAXA_CSS_LAYOUT:
                    redirectURL = SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL;
                    break;
                case SINGLE_SITE_ALL_TAXA:
                case SINGLE_SITE_ALL_TAXA_CSS_LAYOUT:
                    redirectURL = SingleSiteAllTaxaController.SINGLE_SITE_ALL_TAXA_URL;
                    break;
                case ATLAS:
                    redirectURL = AtlasController.ATLAS_URL;
                    break;
                case DEFAULT:
                case DEFAULT_CSS_LAYOUT:
                    // Fall through
                default:
                    redirectURL = TrackerController.EDIT_URL;
                }
            } else {
                CustomForm form = survey.getCustomForm();
                redirectURL = CustomFormController.FORM_RENDER_URL.replace("{formId}", String.valueOf(form.getId()));
            }

            if (StringUtils.hasLength(commentId)) {
                redirectURL = redirectURL + COMMENT_ANCHOR_PREFIX+commentId;
            }
        }
        ModelAndView mv = this.redirect(redirectURL);
        mv.addAllObjects(request.getParameterMap());

        if (!StringUtils.hasLength(request.getParameter(PARAM_SURVEY_ID))) {
            mv.addObject(PARAM_SURVEY_ID, Integer.toString(surveyId));    
        }
        return mv;
    }
}
