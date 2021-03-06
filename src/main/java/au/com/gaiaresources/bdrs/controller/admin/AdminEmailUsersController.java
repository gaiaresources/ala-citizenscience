package au.com.gaiaresources.bdrs.controller.admin;

import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.email.EmailService;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.content.ContentService;

/**
 * Controller for sending emails to users.
 * @author stephanie
 */
@RolesAllowed({Role.ADMIN,Role.SUPERVISOR})
@Controller
public class AdminEmailUsersController extends AbstractController {


    /**
     * Content access for retrieving and saving email templates.
     */
    @Autowired
    private ContentService contentService;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private GroupDAO groupDAO;
    @Autowired
    private EmailService emailService;
    
    /**
     * Renders the email users interface.
     * @param request
     * @param response
     * @return
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/admin/emailUsers.htm", method = RequestMethod.GET)
    public ModelAndView renderPage(HttpServletRequest request,
            HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("adminEmailUsers");
        mav.addObject("keys", contentService.getKeysStartingWith("email"));
        return mav;
    }

    /**
     * Sends an email with the given subject and message content from the 
     * logged in user to a list of email addresses
     * @param request
     * @param response
     * @param to The address(es) to send the message to
     * @param subject The subject of the message
     * @param content The content of the message
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/admin/sendMessage.htm", method = RequestMethod.POST)
    public void sendMessage(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "to", required = true) String to,
            @RequestParam(value = "subject", required = true) String subject,
            @RequestParam(value = "content", required = true) String content) {
        // send the message to each of the addresses in the to field
        String[] addresses = to.split(",");
        User fromUser = getRequestContext().getUser();
        String from = fromUser.getEmailAddress();
        
        for (String string : addresses) {
            String address = string.trim();
            User toUser = userDAO.getUserByEmailAddress(address);
            Map<String, Object> subParams = createSubstitutionParams(toUser, fromUser, content);
            emailService.sendMessage(address, from, subject, content, subParams);
        }
    }
    
    /**
     * Creates a map of subsitution key-value pairs to be used when rendering the velocity template message.
     * @param toUser The recipient of the message
     * @param fromUser The sender of the message
     * @param content The content of the message
     * @return A map of key-value pairs to be used when rendering the velocity template message.
     */
    private Map<String, Object> createSubstitutionParams(User toUser,
            User fromUser, String content) {
        Map<String, Object> subParams = ContentService.getContentParams();
        // find each variable in the message and replace with appropriate user variable
        int start = content.indexOf("${"), end = content.indexOf("}", start);
        for (; 
                 start > 0 && start < content.length() && end > 0 && end > start && end < content.length(); 
                 start = content.indexOf("${", end+1), end = content.indexOf("}", start)) {
            String replaceName = content.substring(start+2, end);
            
            // get the actual variable name
            String varName = replaceName.lastIndexOf(".") > 0 ? replaceName.substring(replaceName.lastIndexOf(".")+1) : replaceName;
            // it is a user field, get the prefix and add the user object
            String prefix = replaceName.lastIndexOf(".") > 0 ? replaceName.substring(0, replaceName.lastIndexOf(".")) : replaceName;
            if (varName.matches("firstName|lastName|emailAddress|name")) {
                // if it contains another ".", it is not just a simple user and may be an expert
                if (!prefix.contains(".")) {
                    if (prefix.matches("admin|from|teacher"))
                        subParams.put(prefix, fromUser);
                    else
                        subParams.put(prefix, toUser);
                }
            } else if (varName.matches("groups")) {
                List<Group> groups = groupDAO.getGroupsForUser(toUser);
                subParams.put(varName, groups);
            } else if (varName.matches("studentName|fullName")) {
                String fullname;
                fullname = toUser.getFirstName();
                if(toUser.getLastName() != null && !toUser.getLastName().isEmpty()) {
                    fullname = fullname + " " + toUser.getLastName();
                }
                subParams.put(varName, fullname);
            }
        }
        return subParams;
    }
}
