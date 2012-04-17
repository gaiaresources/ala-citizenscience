package au.com.gaiaresources.bdrs.controller.customform;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.security.Role;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Tests all aspects of the <code>CustomForm</code>.
 */
public class CustomFormControllerTest extends AbstractGridControllerTest {
    /**
     * Performs the retrieval of custom forms from the database.
     */
    @Autowired
    private CustomFormDAO customFormDAO;

    /**
     * Tests that the listing view handler.
     *
     * @throws Exception
     */
    @Test
    public void testCustomFormListing() throws Exception {
        login("user", "password", new String[]{Role.USER});

        request.setMethod("GET");
        request.setRequestURI(CustomFormController.FORM_LISTING_URL);

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, CustomFormController.FORM_LISTING_VIEW);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "customforms");
        Assert.assertEquals(customFormDAO.getCustomForms().size(), ((List<CustomForm>) mv.getModel().get("customforms")).size());
    }

    /**
     * Tests that a valid custom form can be uploaded.
     *
     * @throws Exception
     */
    @Test
    public void testAddCustomFormValidCustomForm() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MinimalCustomForm";

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        JSONObject config = getConfigFile(testCustomFormName);
        String formName = config.getString(CustomFormController.JSON_CONFIG_NAME);
        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, formName);

        Assert.assertEquals(config.getString(CustomFormController.JSON_CONFIG_NAME), form.getName());
        Assert.assertEquals(config.getString(CustomFormController.JSON_CONFIG_DESCRIPTION), form.getDescription());
    }

    /**
     * Tests that a custom form missing the config file shows the appropriate error.
     *
     * @throws Exception
     */
    @Test
    public void testAddCustomFormMissingConfig() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MissingConfigForm";
        int origCustomFormCount = customFormDAO.getCustomForms().size();

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        // We have the correct number of error message
        Assert.assertEquals(1, getRequestContext().getMessages().size());
        Assert.assertEquals("bdrs.customform.add.missing_config", getRequestContext().getMessages().get(0).getCode());

        // No custom forms added
        Assert.assertEquals(origCustomFormCount, customFormDAO.getCustomForms().size());
    }

    /**
     * Tests that a custom form with a malformed config JSON file shows the
     * appropriate error.
     *
     * @throws Exception
     */
    @Test
    public void testAddCustomFormMalformedConfigJSON() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MalformedJSONConfig";
        int origCustomFormCount = customFormDAO.getCustomForms().size();

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        // We have the correct number of error message
        Assert.assertEquals(1, getRequestContext().getMessages().size());
        Assert.assertEquals("bdrs.customform.add.malformed_config", getRequestContext().getMessages().get(0).getCode());

        // No custom forms added
        Assert.assertEquals(origCustomFormCount, customFormDAO.getCustomForms().size());
    }

    /**
     * Tests that a custom form with a malformed config file shows the
     * appropriate error.
     *
     * @throws Exception
     */
    @Test
    public void testAddCustomFormMalformedConfig() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MalformedConfig";
        int origCustomFormCount = customFormDAO.getCustomForms().size();

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        // We have the correct number of error message
        Assert.assertEquals(1, getRequestContext().getMessages().size());
        Assert.assertEquals("bdrs.customform.add.error", getRequestContext().getMessages().get(0).getCode());

        // No custom forms added
        Assert.assertEquals(origCustomFormCount, customFormDAO.getCustomForms().size());
    }

    /**
     * Tests that a custom form with a missing form directory shows the
     * appropriate error.
     *
     * @throws Exception
     */
    @Test
    public void testAddCustomFormMissingCustomForm() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MissingForm";
        int origCustomFormCount = customFormDAO.getCustomForms().size();

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        // We have the correct number of error message
        Assert.assertEquals(1, getRequestContext().getMessages().size());
        Assert.assertEquals("bdrs.customform.add.error", getRequestContext().getMessages().get(0).getCode());

        // No custom forms added
        Assert.assertEquals(origCustomFormCount, customFormDAO.getCustomForms().size());
    }

    /**
     * Tests that a custom form can be deleted.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteCustomForm() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "MinimalCustomForm";

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);

        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.addFile(getTestCustomForm(testCustomFormName));

        handle(request, response);
        Assert.assertFalse(customFormDAO.getCustomForms().isEmpty());

        JSONObject config = getConfigFile(testCustomFormName);
        String formName = config.getString(CustomFormController.JSON_CONFIG_NAME);
        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, formName);

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_DELETE_URL);
        request.addParameter(CustomFormController.FORM_ID_PATH_VAR,
                String.valueOf(form.getId()));
        handle(request, response);
        Assert.assertTrue(customFormDAO.getCustomForms().isEmpty());
    }

    private MockMultipartFile getTestCustomForm(String formName) throws URISyntaxException, IOException {
        File dir = new File(getClass().getResource(formName).toURI());
        return CustomFormTestUtil.getTestForm(dir, formName);
    }

    private JSONObject getConfigFile(String formName) throws IOException, URISyntaxException {
        File dir = new File(getClass().getResource(formName).toURI());
        return CustomFormTestUtil.getConfigFile(dir);
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
