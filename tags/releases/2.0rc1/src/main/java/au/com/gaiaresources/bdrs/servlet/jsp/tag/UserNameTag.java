package au.com.gaiaresources.bdrs.servlet.jsp.tag;


import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * The UserNameTag renders the full name of a User identified by an ID.
 */
public class UserNameTag extends TagSupport {

    /** The ID of the User we are interested in */
    private int userId;

    /**
     * Writes the full name of the User identified by the <code>userId</code> property.
     * @return always returns SKIP_BODY
     */
    @Override
    public int doStartTag() throws JspException {
        
        JspWriter out = pageContext.getOut();
        try {
            User user = getUserDAO().getUser(userId);
            out.print(user.getFullName());

        } catch (Exception e) {
            throw new JspException("Error retrieving user details for id="+userId, e);
        }
        return SKIP_BODY;
    }

    /**
     * Does nothing.
     * @return always returns EVAL_PAGE.
     */
    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    /**
     * Specifies the User whose name is to be displayed.
     * @param userId the ID of the User.
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * @return the ID of the User whose name is to be displayed.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Looks up the userDAO in the Spring Application Context and returns it.
     * @return an instance of UserDAO.
     */
    private UserDAO getUserDAO() {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());
        return ctx.getBean(UserDAO.class);
    }
    
}
