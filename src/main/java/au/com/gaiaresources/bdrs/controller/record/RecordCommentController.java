package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.record.Comment;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *The RecordController handles requests relating to Comments.
 */
@Controller
public class RecordCommentController extends AbstractController {

    /**  URL used to add a comment to a Record */
    public static final String ADD_COMMENT_URL = "/bdrs/user/comment/add.htm";

    /**  URL used to delete a comment from a Record */
    public static final String DELETE_COMMENT_URL = "/bdrs/user/comment/delete.htm";
    
    /** Name of the HTTP parameter that contains comment text */
    public static final String PARAM_COMMENT_TEXT = "commentText";

    /** The error message code used to indicate that a user cannot comment on a record due to authorisation failure */
    private static final String MSG_CODE_COMMENT_AUTHFAIL = "bdrs.record.comment.authfail";

    /** Used to retrieve Records for commenting */
    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private RedirectionService redirectionService;
    
    /**
     * Adds a comment to a Record.
     * @param request the HTTP request to be processed.
     * @param response the HTTP response being produced.
     * @param recordId the ID of the Record to add a comment to.
     * @param parentCommentId the ID of the Comment being replied to.  If this parameter is supplied, the new comment
     *                        will be added a reply to this comment.
     * @param commentText the text of the comment to add.
     * @return a ModelAndView that will display the commented Record.
     */
    @RolesAllowed( {  Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN })
    @RequestMapping(value = ADD_COMMENT_URL, method = RequestMethod.POST)
    public ModelAndView addComment(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam(value=BdrsWebConstants.PARAM_RECORD_ID, required=true) int recordId,
                                   @RequestParam(value=BdrsWebConstants.PARAM_COMMENT_ID, required=false, defaultValue="-1") int parentCommentId,
                                   @RequestParam(value=PARAM_COMMENT_TEXT, required=true) String commentText) throws ServletException, IOException {

        Record record = recordDAO.getRecord(recordId);
        checkAccess(request, record);

        Comment comment;
        // If a comment Id is not supplied add a new top level comment to the record.
        if (parentCommentId <= 0) {
            comment = record.addComment(commentText);
        }
        else {
            comment = record.getCommentById(parentCommentId);
            comment = comment.reply(commentText);
        }

        // The reason for this is we need to get hold of the ID of the newly created comment as we use it
        // as the page anchor when we redirect so the user doesn't have to scroll to see their new comment.
        RequestContextHolder.getContext().getHibernate().flush();
        
        String url = redirectionService.getViewRecordUrl(record, comment.getId());
        return redirect(url);
    }


    /**
     * Deletes a comment from a Record.
     * @param request the HTTP request to be processed.
     * @param response the HTTP response being produced.
     * @param recordId the ID of the Record the comment to delete belongs to.
     * @param commentId the ID of the Comment to delete.
     * @return a ModelAndView that will display the commented Record.
     */
    @RolesAllowed( {  Role.ADMIN })
    @RequestMapping(value = DELETE_COMMENT_URL, method = RequestMethod.POST)
    public ModelAndView deleteComment(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam(value=BdrsWebConstants.PARAM_RECORD_ID, required=true) int recordId,
                                   @RequestParam(value=BdrsWebConstants.PARAM_COMMENT_ID, required=true) int commentId) throws ServletException, IOException {

        Record record = recordDAO.getRecord(recordId);
        Comment comment = record.getCommentById(commentId);
        comment.softDelete();

        String url = redirectionService.getViewRecordUrl(record, comment.getId());
        return redirect(url);
    }
    /**
     * Checks if the logged in user is allowed to add comments to a Record.  The rule is that a user may comment
     * on a Record if they have permission to create a new Record against the Survey which the Record was
     * recorded against.
     * If the user does not have access, an exception is thrown.
     * @param request the HTTP request being processed.
     * @param record the Record to check access to.
     */
    private void checkAccess(HttpServletRequest request, Record record){
        User user = RequestContextHolder.getContext().getUser();
        RecordWebFormContext ctx = new RecordWebFormContext(request, record, user, record.getSurvey());
        if (!ctx.isCommentable()) {
            throw new AccessDeniedException(MSG_CODE_COMMENT_AUTHFAIL);
        }
    }

}
