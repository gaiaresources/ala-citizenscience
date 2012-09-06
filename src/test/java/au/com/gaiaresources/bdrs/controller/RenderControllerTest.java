package au.com.gaiaresources.bdrs.controller;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.record.AtlasController;
import au.com.gaiaresources.bdrs.controller.record.SingleSiteAllTaxaController;
import au.com.gaiaresources.bdrs.controller.record.SingleSiteMultiTaxaController;
import au.com.gaiaresources.bdrs.controller.record.TrackerController;
import au.com.gaiaresources.bdrs.controller.record.YearlySightingsController;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.security.Role;

public class RenderControllerTest extends AbstractGridControllerTest {

    private Logger log = Logger.getLogger(getClass());
    
    private Survey trackerCssLayoutSurvey;
    private Survey ssmtCssLayoutSurvey;
    private Survey ssatCssLayoutSurvey;
    
    
    @Before
    public void setup() throws Exception {
        trackerCssLayoutSurvey = createTestSurvey("tracker css layout", SurveyFormRendererType.DEFAULT_CSS_LAYOUT);
        ssmtCssLayoutSurvey = createTestSurvey("ssmt css layout", SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA_CSS_LAYOUT);
        ssatCssLayoutSurvey = createTestSurvey("ssat css layout", SurveyFormRendererType.SINGLE_SITE_ALL_TAXA_CSS_LAYOUT);

        request.setMethod("GET");
        request.setRequestURI(RenderController.SURVEY_RENDER_REDIRECT_URL);
        
        this.login("admin", "password", new String[] { Role.USER });
    }
    
    private Survey createTestSurvey(String name, SurveyFormRendererType type) {
        Survey s = new Survey();
        s.setName(name);
        Metadata m = s.setFormRendererType(type);
        metaDAO.save(m);
        return surveyDAO.save(s);
    }
    
    @Test
    public void redirectToSingleSiteMultiTaxa() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.singleSiteMultiTaxaSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
    }
    
    @Test
    public void redirectToSingleSiteAllTaxa() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.singleSiteAllTaxaSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, SingleSiteAllTaxaController.SINGLE_SITE_ALL_TAXA_URL);
    }
    
    @Test
    public void redirectToAtlas() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.atlasSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, AtlasController.ATLAS_URL);
    }
    
    @Test
    public void redirectToTracker() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.survey1.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, TrackerController.EDIT_URL);
    }
    
    @Test
    public void redirectToYearlySightings() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.yearlySightingSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, YearlySightingsController.YEARLY_SIGHTINGS_URL);
    }
    
    @Test
    public void redirectToTrackerCssLayout() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.trackerCssLayoutSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, TrackerController.EDIT_URL);
    }
    
    @Test
    public void redirectToSingleSiteMultiTaxaCssLayout() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.ssmtCssLayoutSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, SingleSiteMultiTaxaController.SINGLE_SITE_MULTI_TAXA_URL);
    }
    
    @Test
    public void redirectToSingleSiteAllTaxaCssLayout() throws Exception {
        request.setParameter(RenderController.PARAM_SURVEY_ID, this.ssatCssLayoutSurvey.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        this.assertRedirect(mv, SingleSiteAllTaxaController.SINGLE_SITE_ALL_TAXA_URL);
    }
}
