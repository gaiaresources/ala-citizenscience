package au.com.gaiaresources.bdrs.model.survey.impl;

import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormSubmitAction;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

/**
 * Tests the Survey class.
 */
public class SurveyTest extends AbstractTransactionalTest {

    @Autowired
    private MetadataDAO metaDAO;
    @Autowired 
    private SurveyDAO surveyDAO;
    
    /** Used to create Users so we can test Survey access */
    @Autowired
    private UserDAO userDAO;

    /** Helps us configure Survey access for our access control tests */
    @Autowired
    private GroupDAO groupDAO;

    private Survey survey;
    
    private Logger log = Logger.getLogger(getClass());
    
    @Before
    public void setup() {
        survey = new Survey();
        survey.setName("survey name");
        survey.setDescription("survey description");
        
        Calendar cal = Calendar.getInstance();
        cal.set(2007, 10, 10, 10, 10, 10);
        survey.setStartDate(cal.getTime());
        
        survey = surveyDAO.save(survey);
    }
    
    @Test
    public void testSurveyFormActionType() {
        Assert.assertEquals("expected value mismatch", Survey.DEFAULT_SURVEY_FORM_SUBMIT_ACTION, survey.getFormSubmitAction());
        
        survey.setFormSubmitAction(SurveyFormSubmitAction.STAY_ON_FORM, metaDAO);
        surveyDAO.save(survey);
        
        Assert.assertEquals("expected value mismatch", SurveyFormSubmitAction.STAY_ON_FORM, survey.getFormSubmitAction());
    }

    /**
     * Tests the "hasAccess(User)" method returns true when the User is an administrator.
     */
    @Test
    public void testHasAccessAdmin() {
        User admin = createUser("admin", true);

        // Note that the admin has not been configured as a user of this survey.
        User owner = createUser("owner", false);
        survey.setPublic(false);
        survey.getUsers().add(owner);
        
        Assert.assertTrue(survey.hasAccess(admin));
    }

    /**
     * Tests the "hasAccess(User)" method returns true when the User is logged in and the Survey is public.
     * (open to all Users).
     */
    @Test
    public void testHasAccessPublicSurvey() {

        User user = createUser("user", false);
        survey.setPublic(true);

        Assert.assertTrue(survey.hasAccess(user));
    }

    /**
     * Tests the "hasAccess(User)" method returns false when the User is not logged in.
     */
    @Test
    public void testHasAccessAnonymousUser() {
        
        survey.setPublic(true);
        
        Assert.assertFalse(survey.hasAccess(null));
    }

    /**
     * Tests that a user has access to a survey when that user is in the surveys "users" Set.
     */
    @Test
    public void testHasAccessInUsers() {
        User user = createUser("user", false);
        
        survey.setPublic(false);
        survey.getUsers().add(user);
        
        Assert.assertTrue(survey.hasAccess(user));
    }

    /**
     * Tests that a user has access to a survey when that user is member of a Group configured on the Survey.
     */
    @Test
    public void testHasAccessInGroup() {
        User user = createUser("user", false);
        Group group = createGroup("group");
        group.getUsers().add(user);
        survey.setPublic(false);
        survey.getGroups().add(group);
        
        Assert.assertTrue(survey.hasAccess(user));
    }

    /**
     * Tests that a user has access to a survey when that user is member of a nested Group configured on the Survey.
     * It also tests the case where the user is in the "admins" section of a group.
     */
    @Test
    public void testHasAccessInNestedGroup() {

        User user = createUser("user", false);
        Group group = createGroup("group");
        Group nestedGroup = createGroup("nestedGroup");
        nestedGroup.getAdmins().add(user);
        group.getGroups().add(nestedGroup);

        survey.setPublic(false);
        survey.getGroups().add(group);

        Assert.assertTrue(survey.hasAccess(user));
    }

    @Test
    public void testHasAccessNotInGroup() {
        User user = createUser("user", false);
        Group group = createGroup("group");
        Group nestedGroup = createGroup("nestedGroup");
        nestedGroup.getAdmins().add(user);
        group.getGroups().add(nestedGroup);

        survey.setPublic(false);
        survey.getGroups().add(group);
        
        User otherUser = createUser("other", false);
        Assert.assertFalse(survey.hasAccess(otherUser));
    }


    /**
     * Creates a system user with the supplied name.
     * @param userName the name of the user to create - used for pretty much all fields.
     * @param admin true if the new User should have the administrator role.
     * @return the new User.
     */
    private User createUser(String userName, boolean admin) {
        
        String[] roles;
        if (admin) {
            roles = new String[] {Role.USER, Role.ADMIN};
        }
        else {
            roles = new String[] {Role.USER};
        }
        return userDAO.createUser(userName, userName, userName, userName+"@user.com", userName, "key", roles);
    }

    /**
     * Creates a new Group.
     * @param name the name of the Group to create.
     * @return the newly created Group.
     */
    private Group createGroup(String name) {
        
        return groupDAO.createGroup(name);
    }

}
