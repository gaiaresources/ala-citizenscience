package au.com.gaiaresources.bdrs;

import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.mocks.MockPortalInitialiser;
import au.com.gaiaresources.bdrs.mocks.MockThemeService;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.model.theme.ThemeDAO;
import au.com.gaiaresources.bdrs.service.theme.ThemeService;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 4/02/14
 * Time: 3:23 PM
 * <p/>
 * Factory of Mock objects used for test
 */
public class MockFactory {

    public static PortalInitialiser newPortalInitialiser() {
        return new MockPortalInitialiser(newThemeService());
    }

    public static ThemeService newThemeService() {
        ThemeDAO themeDAO = AppContext.getBean(ThemeDAO.class);
        FileService fileService = AppContext.getBean(FileService.class);
        ManagedFileDAO managedFileDAO = AppContext.getBean(ManagedFileDAO.class);
        return new MockThemeService(themeDAO, fileService, managedFileDAO);
    }
}
