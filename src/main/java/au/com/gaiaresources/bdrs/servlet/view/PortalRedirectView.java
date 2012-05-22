package au.com.gaiaresources.bdrs.servlet.view;


import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.UrlAssembler;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Extends the RedirectView class to prepend the URL to redirect to with information that identifies the current
 * Portal instance.
 */
public class PortalRedirectView extends RedirectView {
    public PortalRedirectView() {
        super();
    }

    public PortalRedirectView(String url) {
        super(url);
    }

    public PortalRedirectView(String url, boolean contextRelative) {
        super(url, contextRelative);
    }

    public PortalRedirectView(String url, boolean contextRelative, boolean http10Compatible) {
        super(url, contextRelative, http10Compatible);
    }

    public PortalRedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
        super(url, contextRelative, http10Compatible, exposeModelAttributes);
    }

    /**
     * Returns the URL supplied in the constructor prefixed with a path that identifies the current Portal.
     * for example, a URL: /bdrs/admin/test.htm will become /portal/1/bdrs/admin/test.htm if the user is accessing
     * portal 1.
     * Relative URLs are returned unmodified.
     * @return he URL supplied in the constructor prefixed with a path that identifies the current Portal.
     */
    @Override
    public String getUrl() {
        String url = super.getUrl();
        url = UrlAssembler.assembleUrlFor(url);
        return url;
    }

}
