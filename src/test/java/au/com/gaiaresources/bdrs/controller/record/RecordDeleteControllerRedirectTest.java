package au.com.gaiaresources.bdrs.controller.record;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.review.sightings.SightingsController;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;

public class RecordDeleteControllerRedirectTest extends AbstractControllerTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private RecordDAO recDAO;
    @Autowired
    private UserDAO userDAO;
    
    private Survey survey;
    private Record r1;
    private Record r2;
    
    private User user;
    
    private static final String TEST_REDIRECT_URL = "www.testmycode.com/mytesturl/maps/go.htm";
    
    @Before
    public void setup() {
        userDAO.createUser("nonOwner", "nonOwnerFirst", "nonOwnerLast", "nonowner@nonowner.com", "password", "regkey", Role.USER);
        
        user = userDAO.getUser("admin");
        
        survey = new Survey();
        survey.setName("survey name");
        survey.setDescription("survey desc");
        surveyDAO.save(survey);
        
        r1 = new Record();
        r1.setSurvey(survey);
        r1.setUser(user);
        recDAO.saveRecord(r1);
        
        r2 = new Record();
        r2.setSurvey(survey);
        r2.setUser(user);
        recDAO.saveRecord(r2);
    }
    
    @Test
    public void testMultiDeleteSuccess() throws Exception {
        testRecordDeletionRedirect(true, true, false, RecordDeletionController.MSG_CODE_RECORD_MULTI_DELETE_SUCCESS, 2);
    }
    
    @Test
    public void testMultiDeleteRedirectSuccess() throws Exception {
        testRecordDeletionRedirect(true, true, true, RecordDeletionController.MSG_CODE_RECORD_MULTI_DELETE_REDIRECT_SUCCESS, 1);
    }
    
    @Test
    public void testSingleDeleteSuccess() throws Exception {
        testRecordDeletionRedirect(false, true, false, RecordDeletionController.MSG_CODE_RECORD_DELETE_SUCCESS, 0);
    }
    
    @Test
    public void testSingleDeleteRedirectSuccess() throws Exception {
        testRecordDeletionRedirect(false, true, true, RecordDeletionController.MSG_CODE_RECORD_DELETE_REDIRECT_SUCCESS, 0);
    }
    
    @Test
    public void testMultiDeleteAuthFail() throws Exception {
        testRecordDeletionRedirect(true, false, false, RecordDeletionController.MSG_CODE_RECORD_MULTI_DELETE_AUTHFAIL, 0);
    }
    
    @Test
    public void testMultiDeleteRedirectAuthFail() throws Exception {
        testRecordDeletionRedirect(true, false, true, RecordDeletionController.MSG_CODE_RECORD_MULTI_DELETE_REDIRECT_AUTHFAIL, 0);
    }
    
    @Test
    public void testSingleDeleteAuthFail() throws Exception {
        testRecordDeletionRedirect(false, false, false, RecordDeletionController.MSG_CODE_RECORD_DELETE_AUTHFAIL, 0);
    }
    
    @Test
    public void testSingleDeleteRedirectAuthFail() throws Exception {
        testRecordDeletionRedirect(false, false, true, RecordDeletionController.MSG_CODE_RECORD_DELETE_REDIRECT_AUTHFAIL, 0);
    }
    
    /**
     * Generalized test!
     * 
     * @param multi - true => delete more than 1 record, else only 1 record
     * @param authOk - true => login as user with auth to delete, else user with no auth
     * @param redirect - true => pass the redirect param, else no redirect param
     * @param msgCode - message code to look for
     * @param argCount - number of arguments to pass to the message
     * @throws Exception
     */
    private void testRecordDeletionRedirect(boolean multi, boolean authOk, boolean redirect, String msgCode, int argCount) throws Exception {
        if (authOk) {
            login("admin", "password", new String[] { Role.ADMIN });
        } else {
            login("nonOwner", "password", new String[] { Role.USER });
        }
        request.setMethod("POST");
        request.setRequestURI(RecordDeletionController.RECORD_DELETE_URL);
        request.addParameter(RecordDeletionController.PARAM_RECORD_ID, r1.getId().toString());
        if (multi) {
            request.addParameter(RecordDeletionController.PARAM_RECORD_ID, r2.getId().toString());
        }
        if (redirect) {
            request.setParameter(RecordDeletionController.PARAM_REDIRECT_URL, TEST_REDIRECT_URL);
        }
        
        ModelAndView mv = handle(request, response);
        
        if (redirect) {
            assertRedirect(mv, TEST_REDIRECT_URL);
        }
        
        if (authOk) {
            // create the arguments
            Object[] args = new Object[argCount];
            for (int i = 0; i < args.length; i++) {
                if (i == 0 && multi) {
                    args[i] = 2;
                } else {
                    args[i] = SightingsController.TABLE_TAB;
                }
            }
            if (args.length > 0) {
                this.assertMessageCodeAndArgs(msgCode, args);
            } else {
                this.assertMessageCode(msgCode);
            }    
        } else {
            this.assertMessageCode(msgCode);
        }
    }
}
