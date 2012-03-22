package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.record.Comment;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests the RecordCommentController.
 */
public class RecordCommentControllerTest extends AbstractGridControllerTest {

    /** Helps us configure Survey access for our access control tests */
    @Autowired
    private GroupDAO groupDAO;

    @Before
    public void setUpNormalUser() {
        userDAO.createUser("normal", "Normal", "User", "normal@normal.com", "normal", "key", Role.USER);
    }
    /**
     * Tests that a Comment can be added to a Record.
     */
    @Test
    public void testAddComment() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        String commentText = "Test";

        configureAddCommentRequest(r1, commentText);
        ModelAndView mav = handle(request, response);

        Comment comment = r1.getComments().get(0);
        Assert.assertEquals(commentText, comment.getCommentText());

        RedirectionService service = new RedirectionService("");
        Assert.assertEquals(service.getViewRecordUrl(r1, comment.getId()), ((RedirectView)mav.getView()).getUrl());
    }

    @Test
    public void testHttpGET() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        configureAddCommentRequest(r1, "Doesn't matter");
        request.setMethod("GET");

        try {
            handle(request, response);
            Assert.fail("Should not support the GET method.");
        }
        catch (HttpRequestMethodNotSupportedException e) {
            // all good.
        }

    }

    /**
     * Tests that an anonymous user cannot add a comment.
     */
    @Test
    public void testAnonymousUserCannotAddComment() throws Exception {
        // Don't login....
        configureAddCommentRequest(r1, "Test");
        try {
            handle(request, response);
            Assert.fail("An anonymous user should not be able to comment on a Record");
        }
        catch (AccessDeniedException e) {
            // all good.
        }
    }

    /**
     * Tests a normal user (i.e. not an admin/power user/supervisor) can comment on a Record attached to a
     * survey that allows comments.
     */
    @Test
    public void testNormalUserCanCommentOnAllUsersSurvey() throws Exception {
        login("normal", "normal", new String[] { Role.USER });

        String commentText = "Test";
        r1.getSurvey().setPublic(true);
        configureAddCommentRequest(r1, commentText);
        ModelAndView mav = handle(request, response);

        Comment comment =  r1.getComments().get(0);
        RedirectionService service = new RedirectionService("");
        Assert.assertEquals(commentText, comment.getCommentText());
        Assert.assertEquals(service.getViewRecordUrl(r1, comment.getId()), ((RedirectView)mav.getView()).getUrl());
    }

    /**
     * Tests that a normal user cannot comment on a Record attached to a survey that has access controls applied.
     */
    @Test
    public void testSurveyLevelAccessControl() throws Exception {
        login("normal", "normal", new String[] { Role.USER });
        configureAddCommentRequest(r1, "Test");
        r1.getSurvey().setPublic(false);
        configureUserGroup(r1.getSurvey(), poweruser);

        try {
            handle(request, response);
            Assert.fail("The normal user should not be able to comment on the Record");
        }
        catch (AccessDeniedException e) {
            // all good.
        }
    }


    /**
     * Tests adding a nested comment. (Replying to a comment).
     */
    @Test
    public void testReplyToComment() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Comment parent = r1.addComment("Top level comment.");
        
        // Flush so we can get the ID of the parent comment.
        RequestContextHolder.getContext().getHibernate().flush();
        String commentText = "Test Reply";

        configureAddCommentRequest(r1, commentText);
        request.addParameter(BdrsWebConstants.PARAM_COMMENT_ID, Integer.toString(parent.getId()));
        
        ModelAndView mav = handle(request, response);

        Comment comment = r1.getComments().get(0).getReplies().get(0);
        Assert.assertEquals(commentText, comment.getCommentText());

        RedirectionService service = new RedirectionService("");
        Assert.assertEquals(service.getViewRecordUrl(r1, comment.getId()), ((RedirectView)mav.getView()).getUrl());
    }

    /**
     * Tests that only an admin can delete a record.
     */
    @Test
    public void testNonAdminDeleteFails() throws Exception {
        login("normal", "normal", new String[] { Role.USER });
        
        Comment comment = r1.addComment("Test");
        // Flush so we can get the ID of the comment.
        RequestContextHolder.getContext().getHibernate().flush();

        configureDeleteCommentRequest(r1, comment);
    
        try {
            handle(request, response);
            Assert.fail("An exception should have been thrown.");
        }
        catch (AccessDeniedException e) {
            // all good.
        }
    }

    /**
     * Tests deleting a comment.
     */
    @Test
    public void testDeleteComment() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Comment comment = r1.addComment("Test");
        // Flush so we can get the ID of the comment.
        RequestContextHolder.getContext().getHibernate().flush();

        configureDeleteCommentRequest(r1, comment);


        ModelAndView mav = handle(request, response);
        Assert.assertTrue(comment.isDeleted());

        RedirectionService service = new RedirectionService("");
        Assert.assertEquals(service.getViewRecordUrl(r1, comment.getId()), ((RedirectView)mav.getView()).getUrl());
    }

    /**
     * Configures the http request to simulate a request to add a comment to a record.
     * @param record the Record to add a comment to.
     * @param comment the text of the comment to add.
     */
    private void configureAddCommentRequest(Record record, String comment) {
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+RecordCommentController.ADD_COMMENT_URL);
        request.setParameter(BdrsWebConstants.PARAM_RECORD_ID, Integer.toString(record.getId()));
        request.setParameter("commentText", comment);
    }

    /**
     * Configures the http request to simulate a request to delete a comment.
     * @param record the Record to delete the comment from.
     * @param comment the Comment to delete.
     */
    private void configureDeleteCommentRequest(Record record, Comment comment) {
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+RecordCommentController.DELETE_COMMENT_URL);
        request.setParameter(BdrsWebConstants.PARAM_RECORD_ID, Integer.toString(record.getId()));
        request.setParameter(BdrsWebConstants.PARAM_COMMENT_ID, Integer.toString(comment.getId()));
    }
 
    private void configureUserGroup(Survey survey, User... users) {
        Group group = groupDAO.createGroup("group");
        Set<User> userSet = new HashSet<User>();
        for (User user : users) {
            userSet.add(user);
        }
        group.setUsers(userSet);
        
        survey.setPublic(false);
        survey.getGroups().add(group);
    }
    
}
