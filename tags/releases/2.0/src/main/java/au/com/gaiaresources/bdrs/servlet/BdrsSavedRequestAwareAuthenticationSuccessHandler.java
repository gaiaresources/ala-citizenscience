package au.com.gaiaresources.bdrs.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * SuccessHandler that removes the {@link BdrsWebConstants.SAVED_REQUEST_KEY}
 * attribute from the session on successful login.
 * 
 * @author stephanie
 *
 */
public class BdrsSavedRequestAwareAuthenticationSuccessHandler extends
        SavedRequestAwareAuthenticationSuccessHandler {
    
    /*
     * (non-Javadoc)
     * @see org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler#onAuthenticationSuccess(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.core.Authentication)
     */
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
        // remove the session attribute so we don't try to use it again
        request.getSession().removeAttribute(BdrsWebConstants.SAVED_REQUEST_KEY);
    }
}
