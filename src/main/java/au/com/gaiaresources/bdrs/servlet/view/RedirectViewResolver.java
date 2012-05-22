package au.com.gaiaresources.bdrs.servlet.view;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import java.util.Locale;

/**
 * Resolved String based view names prefixed with redirect: or redirectWithoutModel:.
 * The reason for handling redirect: is to allow the URL to be decorated with a prefix that identifies the
 * current portal.
 */
public class RedirectViewResolver implements ViewResolver, Ordered {

    // Uses this prefix to avoid interference with the default behaviour
    public static final String REDIRECT_URL_PREFIX = "redirectWithoutModel:";     
    // Have a highest priority by default
    private int order = Integer.MIN_VALUE; 

    /**
     * Used by spring to parse the redirection URL and instantiate the 
     * RedirectView appropriately.
     * 
     */
    public View resolveViewName(String viewName, Locale arg1) throws Exception {
        if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
            String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
            return new PortalRedirectView(redirectUrl, true, true, false);
        }
        else if (viewName.startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX)) {
            String redirectUrl = viewName.substring(UrlBasedViewResolver.REDIRECT_URL_PREFIX.length());
            return new PortalRedirectView(redirectUrl, true, true);
        }

        return null;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
