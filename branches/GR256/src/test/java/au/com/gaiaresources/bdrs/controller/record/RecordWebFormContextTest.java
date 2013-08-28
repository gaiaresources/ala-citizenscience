package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.test.AbstractSpringContextTest;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

public class RecordWebFormContextTest extends AbstractTransactionalTest {

	@Autowired
	private RecordDAO recordDAO;
	@Autowired
	private UserDAO userDAO;
	@Autowired
	private SurveyDAO surveyDAO;
    @Autowired
    private GeoMapService geoMapService;
	
    private Record record;
    private User owner;
    private User nonOwner;
    private User admin;
    private Survey survey;
    
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    
    @Before
    public void setup() {
        
        // minimum test setup. these objects are not persistable due to null fields
        survey  = new Survey();
        surveyDAO.save(survey);
        
        owner = userDAO.createUser("owneruser", "ownerfirst", "ownerlast", "owner@owner.com", "password", "regkey", Role.USER);
        nonOwner = userDAO.createUser("nonowneruser", "nonownerfirst", "nonownerlast", "nonowner@nonowner.com", "password", "regkey", Role.USER);
        admin = userDAO.createUser("adminuser", "adminfirst", "adminlast", "admin@admin.com", "password", "regkey", Role.ADMIN);
        
        record = new Record();
        record.setId(1);
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        record.setUser(owner);
        record.setSurvey(survey);
        recordDAO.save(record);
    }
    
    @Test
    public void testFormGet_noUser_newRecord_edit() throws Exception {
        
        // attempting to view a new record results in trying to create
        // a new record (i.e. entering edit mode).
        // since non logged in users can never edit records, access
        // will always fail.
        testContextCreate(null, null, true, false, true, false, false, false);
    }
    
    @Test
    public void testFormGet_noUser_newRecord_view() throws Exception {

        // attempting to view a new record results in trying to create
        // a new record (i.e. entering edit mode).
        // since non logged in users can never edit records, access
        // will always fail.
        testContextCreate(null, null, false, false, true, false, false, false);
    }
    
    // view access by anonymous user
    
    @Test
    public void testFormGet_noUser_existingRecord_view_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(null, record, false, true, false, false, true, false);
    }
    
    @Test
    public void testFormGet_noUser_existingRecord_view_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(null, record, false, false, false, false, true, false);
    }
    
    @Test
    public void testFormGet_noUser_existingRecord_view_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(null, record, false, false, false, false, true, false);
    }
    
    // view access by non owner user - role admin
    
    @Test
    public void testFormGet_adminUser_existingRecord_view_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(admin, record, false, true, false, true, true, true);
    }
    
    @Test
    public void testFormGet_adminUser_existingRecord_view_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(admin, record, false, true, false, true, true, true);
    }
    
    @Test
    public void testFormGet_adminUser_existingRecord_view_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(admin, record, false, true, false, true, true, true);
    }
    
    // view access by non owner user - role user
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_view_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(nonOwner, record, false, true, false, false, true, true);
    }
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_view_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(nonOwner, record, false, false, false, false, true, false);
    }
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_view_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(nonOwner, record, false, false, false, false, true, false);
    }
    
    
    // view access by owning user - role user
    
    @Test
    public void testFormGet_ownerUser_existingRecord_view_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(owner, record, false, true, false, true, true, true);
    }
    
    @Test
    public void testFormGet_ownerUser_existingRecord_view_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(owner, record, false, true, false, true, true, true);
    }
    
    @Test
    public void testFormGet_ownerUser_existingRecord_view_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(owner, record, false, true, false, true, true, true);
    }
    
    // edit section follows
    
    // edit access by non owner user - role admin
    
    @Test
    public void testFormGet_adminUser_existingRecord_edit_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(admin, record, true, true, true, true, true, false);
    }
    
    @Test
    public void testFormGet_adminUser_existingRecord_edit_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(admin, record, true, true, true, true, true, false);
    }
    
    @Test
    public void testFormGet_adminUser_existingRecord_edit_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(admin, record, true, true, true, true, true, false);
    }
    
    // edit access by non owner user - role user
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_edit_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(nonOwner, record, true, false, true, false, true, false);
    }
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_edit_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(nonOwner, record, true, false, true, false, true, false);
    }
    
    @Test
    public void testFormGet_nonOwnerUser_existingRecord_edit_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(nonOwner, record, true, false, true, false, true, false);
    }
    
    
    // edit access by owning user - role user
    
    @Test
    public void testFormGet_ownerUser_existingRecord_edit_public() throws Exception {
        record.setRecordVisibility(RecordVisibility.PUBLIC);
        testContextCreate(owner, record, true, true, true, true, true, false);
    }
    
    @Test
    public void testFormGet_ownerUser_existingRecord_edit_controlled() throws Exception {
        record.setRecordVisibility(RecordVisibility.CONTROLLED);
        testContextCreate(owner, record, true, true, true, true, true, false);
    }
    
    @Test
    public void testFormGet_ownerUser_existingRecord_edit_ownerOnly() throws Exception {
        record.setRecordVisibility(RecordVisibility.OWNER_ONLY);
        testContextCreate(owner, record, true, true, true, true, true, false);
    }

    
    /**
     * parameterized test
     * 
     * @param loginUser - user to login as, can be null
     * @param rec - record to request for the form, can be null
     * @param requestEdit - value to pass to request record editing
     * @param expectedAccessResult - do we expect to be able to see the tracker form?
     * @param expectedFormEditState - should the tracker form's state be editable?
     * @param expectedCommentable - should the user be able to comment on the form?
     * @throws Exception
     */
    private void testContextCreate(User loginUser, Record rec, boolean requestEdit, 
            boolean expectedAccessResult, boolean expectedFormEditState, boolean expectedUnlock, boolean expectedExisting, boolean expectedCommentable) throws Exception {
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(RecordWebFormContext.PARAM_EDIT, Boolean.toString(requestEdit));
        
        if (!expectedAccessResult) {
            expectedEx.expect(AccessDeniedException.class);
            if (expectedFormEditState) {
                // edit mode
                expectedEx.expectMessage(RecordWebFormContext.MSG_CODE_EDIT_AUTHFAIL);
            } else {
                // view mode
                expectedEx.expectMessage(RecordWebFormContext.MSG_CODE_VIEW_AUTHFAIL);
            }
        }
        RecordWebFormContext context = new RecordWebFormContext(request, rec, loginUser, survey, geoMapService);
        Assert.assertEquals("editable state does not match expected", expectedFormEditState, context.isEditable());
        Assert.assertEquals("unlockable state does not match expected", expectedUnlock, context.isUnlockable());
        Assert.assertEquals("existing state does not match expected", expectedExisting, context.isExistingRecord());
        Assert.assertEquals("commentable state does not match expected", expectedCommentable, context.isCommentable());
        if (rec != null) {
            Assert.assertEquals("wrong record id", rec.getId(), context.getRecordId());
        }
        Assert.assertEquals("wrong survey id", survey.getId(), context.getSurveyId());
    }
}
