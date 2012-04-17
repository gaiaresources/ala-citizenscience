package au.com.gaiaresources.bdrs.controller.customform;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests that the example custom form can be loaded, rendered and records can be added and edited.
 */
public class ExampleCustomFormTest extends AbstractGridControllerTest {
    public static final String EXAMPLE_FORM_DIR = "forms/ExampleForm/";
    public static final String EXAMPLE_FORM_NAME = "Example Custom Form";

    @Autowired
    private CustomFormDAO customFormDAO;

    /**
     * Uploads the Example Custom Form to the BDRS.
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});

        String testCustomFormName = "ExampleForm";

        request.setMethod("POST");
        request.setRequestURI(CustomFormController.FORM_ADD_URL);
        request.addParameter(BdrsWebConstants.PARAM_SURVEY_ID, String.valueOf(survey1.getId()));
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        File formDir = new File(EXAMPLE_FORM_DIR);
        req.addFile(CustomFormTestUtil.getTestForm(formDir, testCustomFormName));

        ModelAndView mv = handle(request, response);
        RedirectView redirect = (RedirectView) mv.getView();
        Assert.assertEquals(CustomFormController.FORM_LISTING_URL, redirect.getUrl());

        JSONObject config = CustomFormTestUtil.getConfigFile(formDir);
        String formName = config.getString(CustomFormController.JSON_CONFIG_NAME);
        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, formName);

        Assert.assertEquals(config.getString(CustomFormController.JSON_CONFIG_NAME), form.getName());
        Assert.assertEquals(config.getString(CustomFormController.JSON_CONFIG_DESCRIPTION), form.getDescription());

        // Should have exactly 1 message indicating that the form was added succesfully.
        Assert.assertEquals(1, getRequestContext().getMessageContents().size());
    }

    /**
     * Tests that the form can be rendered.
     * @throws Exception
     */
    @Test
    public void testExampleFormRender() throws Exception {
        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, EXAMPLE_FORM_NAME);
        String renderURL = CustomFormTestUtil.getRenderURL(form);

        // Render the report start page
        request.setMethod("GET");
        request.setRequestURI(renderURL);
        handle(request, response);
        for (String msg : getRequestContext().getMessageContents()) {
            System.err.println(msg);
        }
        // If everything works as desired, there should be no messages
        Assert.assertTrue("When a form render correctly there should be no messages",
                getRequestContext().getMessageContents().isEmpty());
    }

    /**
     * Tests that a record can be added using the custom form.
     * @throws Exception
     */
    @Test
    public void testExampleFormAdd() throws Exception {
        Set<User> userSet = new HashSet();
        userSet.add(currentUser);
        for (Record rec : recordDAO.getRecords(survey1, userSet)) {
            recordDAO.deleteById(rec.getId());
        }
        surveyDAO.save(survey1);
        Assert.assertTrue(recordDAO.getRecords(survey1, userSet).isEmpty());

        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, EXAMPLE_FORM_NAME);
        String renderURL = CustomFormTestUtil.getRenderURL(form);

        request.setMethod("POST");
        request.setRequestURI(renderURL);
        request.addParameter("submit", "submit");
        CustomFormTestUtil.populateCustomFormPOSTParameters(request, survey1, nyanCat);

        int initialRecordCount = recordDAO.getRecords(survey1, userSet).size();
        handle(request, response);

        List<Record> surveyRecords = recordDAO.getRecords(survey1, userSet);
        Assert.assertEquals(initialRecordCount + 1, surveyRecords.size());
        CustomFormTestUtil.validateRecord(request, survey1, nyanCat, surveyRecords.get(0), 0);
    }

    /**
     * Tests that a record can be edited using the custom form.
     * @throws Exception
     */
    @Test
    public void testExampleFormEdit() throws Exception {
        Set<User> userSet = new HashSet();
        userSet.add(currentUser);
        List<Record> recordList = recordDAO.getRecords(survey1, userSet);
        Assert.assertFalse(recordList.isEmpty());
        Record record = recordList.get(0);

        CustomForm form = CustomFormTestUtil.getCustomFormByName(customFormDAO, EXAMPLE_FORM_NAME);
        String renderURL = CustomFormTestUtil.getRenderURL(form);

        request.setMethod("POST");
        request.setRequestURI(renderURL);
        request.addParameter("submit", "submit");
        CustomFormTestUtil.populateCustomFormPOSTParameters(request, survey1, record, nyanCat);

        int initialRecordCount = recordDAO.getRecords(survey1, userSet).size();
        handle(request, response);

        List<Record> surveyRecords = recordDAO.getRecords(survey1, userSet);

        // Same number of records.
        Assert.assertEquals(initialRecordCount, surveyRecords.size());

        CustomFormTestUtil.validateRecord(request, survey1, nyanCat, recordDAO.getRecord(record.getId()), 0);
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
