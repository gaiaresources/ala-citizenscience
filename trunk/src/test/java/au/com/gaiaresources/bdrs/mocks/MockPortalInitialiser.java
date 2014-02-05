package au.com.gaiaresources.bdrs.mocks;

import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.service.theme.ThemeService;

import javax.servlet.ServletContextEvent;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 4/02/14
 * <p/>
 * A PortalInitialiser with a pluggable ThemeService
 */
public class MockPortalInitialiser extends PortalInitialiser {

    public MockPortalInitialiser(ThemeService themeService) {
        super.themeService = themeService;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {

    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
    }
}
