package au.com.gaiaresources.bdrs.servlet.filter;

import au.com.gaiaresources.bdrs.controller.HomePageController;
import au.com.gaiaresources.bdrs.servlet.BdrsSavedRequestAwareAuthenticationSuccessHandler;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.apache.log4j.Logger;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Overrides the behaviour of the default Spring authentication filter to work nicely
 * with RESTFUL portal URLs
 * 
 * @author aaron
 *
 */
public class BdrsAuthenticationFilter extends
        UsernamePasswordAuthenticationFilter {

    private Logger log = Logger.getLogger(getClass());
    
    private static final String RESTFUL_PORTAL_PATTERN_STR = "(" + PortalSelectionFilterMatcher.BASE_RESTFUL_PORTAL_PATTERN + ")?";
    
    private Pattern authenticationPattern;
    private BdrsSavedRequestAwareAuthenticationSuccessHandler successHandler;
    
    public BdrsAuthenticationFilter() {
        super();
        // need to override default behaviour which is redirecting to '/'
        successHandler = new BdrsSavedRequestAwareAuthenticationSuccessHandler();
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler(HomePageController.LOGIN_FAILED_URL);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        compileAuthenticationPattern();
    }
    
    /**
     * Indicates whether this filter should attempt to process a login request for the current invocation.
     * <p>
     * It strips any parameters from the "path" section of the request URL (such
     * as the jsessionid parameter in
     * <em>http://host/myapp/index.html;jsessionid=blah</em>) before matching
     * against the <code>filterProcessesUrl</code> property.
     * <p>
     * Subclasses may override for special requirements, such as Tapestry integration.
     *
     * @return <code>true</code> if the filter should attempt authentication, <code>false</code> otherwise.
     */
    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        
        String uri = request.getRequestURI();
        int pathParamIndex = uri.indexOf(';');

        if (pathParamIndex > 0) {
            // strip everything after the first semi-colon
            uri = uri.substring(0, pathParamIndex);
        }
        Matcher m = authenticationPattern.matcher(uri);
        
        // if a request URL has been stored in the session, use that one, otherwise, use the default
        Object savedURL = request.getSession().getAttribute(BdrsWebConstants.SAVED_REQUEST_KEY);
        if (savedURL != null) {
            successHandler.setDefaultTargetUrl(savedURL.toString());
        } else {
            successHandler.setDefaultTargetUrl(HomePageController.AUTHENTICATED_REDIRECT_URL);
        }
        return m.find();
    }

    @Override
    public void setFilterProcessesUrl(String filterProcessesUrl) {
        super.setFilterProcessesUrl(filterProcessesUrl);
        compileAuthenticationPattern();
    }
    
    private void compileAuthenticationPattern() {
        String pattern = RESTFUL_PORTAL_PATTERN_STR + getFilterProcessesUrl();
        authenticationPattern = Pattern.compile(pattern);
    }
}
