package au.com.gaiaresources.bdrs.controller.survey;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.FileUtils;

public class SurveyBaseControllerTest extends AbstractGridControllerTest {
    
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private MetadataDAO mdDAO;
    @Autowired
    private FileService fileService;

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
        params.put(SurveyBaseController.PARAM_CRS, BdrsCoordReferenceSystem.MGA.toString());
        
        // no value...
        //params.put(SurveyBaseController.PARAM_RECORD_VISIBILITY_MODIFIABLE, "false");
        
        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        assertRedirect(mv, "/bdrs/admin/survey/listing.htm");
        
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
        Assert.assertEquals("wrong crs", BdrsCoordReferenceSystem.MGA, survey.getMap().getCrs());
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
        params.put(SurveyBaseController.PARAM_CRS, BdrsCoordReferenceSystem.WGS84.toString());
        
        request.setParameters(params);
        
        ModelAndView mv = handle(request, response);
        
        assertRedirect(mv, "/bdrs/admin/survey/editTaxonomy.htm");
        
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
        Assert.assertEquals("wrong crs", BdrsCoordReferenceSystem.WGS84, survey.getMap().getCrs());
    }
    
    @Test
    public void saveCssFile() throws Exception {
        
        this.login("admin", "password", new String[] { Role.ADMIN });
        
        Survey survey = new Survey();
        survey.setName("my survey");
        survey.setDescription("my survey desc");
        
        String filename = "empty.css";
        String testCss = ".hello {\nwidth:100px;\n}";
        
        Metadata md = new Metadata();
        md.setKey(Metadata.SURVEY_CSS);
        md.setValue(filename);
        mdDAO.save(md);
        
        Set<Metadata> mdList = new HashSet<Metadata>();
        mdList.add(md);
        survey.setMetadata(mdList);
        
        surveyDAO.save(survey);
        
        MockMultipartFile testFile = new MockMultipartFile("file", filename, "text/css", "blah".getBytes());
        fileService.createFile(md, testFile);
        
        request.setRequestURI(SurveyBaseController.SURVEY_EDIT_CSS_LAYOUT_URL);
        request.setMethod("POST");
        request.setParameter(SurveyBaseController.PARAM_TEXT, testCss);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter("Save", "true");
        
        this.handle(request, response);
        
        // should have returned success!
        this.assertMessageCode(SurveyBaseController.MSG_KEY_FILE_WRITE_SUCCESS);
        
        FileDataSource fileDataSource = fileService.getFile(md, filename);
        Assert.assertNotNull("file data source cannot be null", fileDataSource);
        File file = fileDataSource.getFile();
        Assert.assertNotNull("file cannot be null", file);
        
        String contents = FileUtils.readFile(file.getAbsolutePath());
        Assert.assertNotNull("Contents cannot be null", contents);
        Assert.assertEquals("Wrong contents", testCss, contents);
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
    
    // Simulates the controller running in it's own transaction.
    @Override
    protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	Session sesh = getSession();
    	sesh.flush();
    	sesh.clear();
    	ModelAndView mv = super.handle(request, response);
    	sesh.flush();
    	sesh.clear();
    	return mv;
    }
}
