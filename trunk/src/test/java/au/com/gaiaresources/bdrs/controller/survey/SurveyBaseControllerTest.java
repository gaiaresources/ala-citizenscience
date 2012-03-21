package au.com.gaiaresources.bdrs.controller.survey;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayer;
import au.com.gaiaresources.bdrs.model.map.BaseMapLayerSource;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class SurveyBaseControllerTest extends AbstractGridControllerTest {
    
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private GeoMapLayerDAO geoMapLayerDAO;

    @Test
    public void testListSurveys() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/listing.htm");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "surveyListing");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "surveyList");
    }

    @Test
    public void testAddSurvey() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/edit.htm");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "surveyEdit");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "publish");
    }

    /**
     * Tests the basic use case of creating a new survey and clicking save.
     */
    @Test
    public void testAddSurveySubmitWideLogo() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/edit.htm");

        BufferedImage wideImage = new BufferedImage(1024, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D)wideImage.getGraphics();
        g2.setColor(Color.GREEN);
        g2.fillRect(0,0,wideImage.getWidth(), wideImage.getHeight());
        File imgTmp = File.createTempFile("SurveyBaseControllerTest.testAddSurveySubmit", ".png");
        String targetFilename = String.format("%s%s%s", FilenameUtils.getBaseName(imgTmp.getName()), FilenameUtils.EXTENSION_SEPARATOR, SurveyBaseController.TARGET_LOGO_IMAGE_FORMAT);
        ImageIO.write(wideImage, "png", imgTmp);
        
        MockMultipartFile logoFile = new MockMultipartFile(Metadata.SURVEY_LOGO+"_file", imgTmp.getName(), "image/png", new FileInputStream(imgTmp));
        MockMultipartHttpServletRequest multipartRequest = (MockMultipartHttpServletRequest)request;
        multipartRequest.addFile(logoFile);
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", "Test Survey 1234");
        params.put("description", "This is a test survey");
        params.put("surveyDate", "10 Nov 2010");
        params.put("SurveyLogo", "");
        params.put("rendererType", "DEFAULT");
        params.put(Metadata.SURVEY_LOGO, imgTmp.getName());
        params.put(SurveyBaseController.PARAM_DEFAULT_RECORD_VISIBILITY, RecordVisibility.CONTROLLED.toString());
        params.put(SurveyBaseController.PARAM_FORM_SUBMIT_ACTION, SurveyFormSubmitAction.STAY_ON_FORM.toString());
        
        // no value...
        //params.put(SurveyBaseController.PARAM_RECORD_VISIBILITY_MODIFIABLE, "false");
        
        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        Assert.assertEquals("/bdrs/admin/survey/listing.htm", redirect.getUrl());
        
        Survey survey = surveyDAO.getSurveyByName(params.get("name"));
        Assert.assertEquals(survey.getName(), params.get("name"));
        Assert.assertEquals(survey.getDescription(), params.get("description"));
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2010, 10, 10, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(survey.getStartDate().getTime(), cal.getTime().getTime());
        
        Metadata md = survey.getMetadataByKey(Metadata.SURVEY_LOGO);
        Assert.assertEquals(targetFilename, md.getValue());
        
        Assert.assertEquals(RecordVisibility.CONTROLLED, survey.getDefaultRecordVisibility());
        Assert.assertEquals(false, survey.isRecordVisibilityModifiable());
        Assert.assertEquals("form submit action mismatch", SurveyFormSubmitAction.STAY_ON_FORM, survey.getFormSubmitAction());
    }
    
    /**
     * Tests the basic use case of creating a new survey and clicking saveAndContinue.
     */
    @Test
    public void testAddSurveySubmitTallLogo() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/edit.htm");

        BufferedImage tallImage = new BufferedImage(40, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D)tallImage.getGraphics();
        g2.setColor(Color.GREEN);
        g2.fillRect(0,0,tallImage.getWidth(), tallImage.getHeight());
        File imgTmp = File.createTempFile("SurveyBaseControllerTest.testAddSurveySubmit", ".png");
        String targetFilename = String.format("%s%s%s", FilenameUtils.getBaseName(imgTmp.getName()), FilenameUtils.EXTENSION_SEPARATOR, SurveyBaseController.TARGET_LOGO_IMAGE_FORMAT);
        ImageIO.write(tallImage, "png", imgTmp);
        
        MockMultipartFile logoFile = new MockMultipartFile(Metadata.SURVEY_LOGO+"_file", imgTmp.getName(), "image/png", new FileInputStream(imgTmp));
        MockMultipartHttpServletRequest multipartRequest = (MockMultipartHttpServletRequest)request;
        multipartRequest.addFile(logoFile);
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", "Test Survey 1234");
        params.put("description", "This is a test survey");
        params.put("surveyDate", "10 Nov 2010");
        params.put("SurveyLogo", "");
        params.put("rendererType", "DEFAULT");
        params.put("saveAndContinue", "saveAndContinue");
        params.put(Metadata.SURVEY_LOGO, imgTmp.getName());
        params.put(SurveyBaseController.PARAM_DEFAULT_RECORD_VISIBILITY, RecordVisibility.CONTROLLED.toString());
        params.put(SurveyBaseController.PARAM_RECORD_VISIBILITY_MODIFIABLE, "true");
        params.put(SurveyBaseController.PARAM_FORM_SUBMIT_ACTION, SurveyFormSubmitAction.MY_SIGHTINGS.toString());
        
        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        Assert.assertEquals("/bdrs/admin/survey/editTaxonomy.htm", redirect.getUrl());
        
        Survey survey = surveyDAO.getSurveyByName(params.get("name"));
        Assert.assertEquals(survey.getName(), params.get("name"));
        Assert.assertEquals(survey.getDescription(), params.get("description"));
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2010, 10, 10, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(survey.getStartDate().getTime(), cal.getTime().getTime());
        
        Metadata md = survey.getMetadataByKey(Metadata.SURVEY_LOGO);
        Assert.assertEquals(targetFilename, md.getValue());
        
        Assert.assertEquals(RecordVisibility.CONTROLLED, survey.getDefaultRecordVisibility());
        Assert.assertEquals(true, survey.isRecordVisibilityModifiable());
        Assert.assertEquals("form submit action mismatch", SurveyFormSubmitAction.MY_SIGHTINGS, survey.getFormSubmitAction());
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
    
    /**
     * Test that the survey map editing interface can be correctly opened.
     * @throws Exception
     */
    @Test
    public void testEditMap() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/survey/editMap.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(survey1.getId()));
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "surveyEditMap");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "baseLayers");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "bdrsLayers");
    }
    
    /**
     * Test that survey map settings can be saved and that they show on the record entry form.
     * @throws Exception
     */
    @Test
    public void testSaveMapSettings() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/survey/editMap.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(survey1.getId()));
        request.setParameter("zoomLevel", "10");
        request.setParameter("mapCenter", "POINT(140, -28)");
        request.setParameter("weight_"+BaseMapLayerSource.G_HYBRID_MAP, "1");
        request.setParameter("selected_"+BaseMapLayerSource.G_HYBRID_MAP, "true");
        request.setParameter("default", BaseMapLayerSource.G_HYBRID_MAP.toString());
        request.setParameter("saveAndContinue", "true");
        
        ModelAndView mv = handle(request, response);
        Assert.assertTrue(mv.getView() instanceof RedirectView);
        RedirectView redirect = (RedirectView)mv.getView();
        Assert.assertEquals("/bdrs/admin/survey/locationListing.htm", redirect.getUrl());
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "surveyId");
        
        // assert that the survey settings have been saved properly
        Assert.assertEquals("POINT(140, -28)", survey1.getMapCenter());
        Assert.assertEquals("10", survey1.getMapZoom());
        List<BaseMapLayer> baseLayers = survey1.getBaseMapLayers();
        // there should be one base layer and it should be as above
        Assert.assertEquals(1, baseLayers.size());
        BaseMapLayer layer = baseLayers.get(0);
        Assert.assertEquals(BaseMapLayerSource.G_HYBRID_MAP, layer.getLayerSource());
        Assert.assertEquals(1, layer.getWeight());
        Assert.assertEquals(true, layer.isDefault());
    }
}
