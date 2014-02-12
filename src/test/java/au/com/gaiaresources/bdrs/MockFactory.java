package au.com.gaiaresources.bdrs;

import au.com.gaiaresources.bdrs.config.AppContext;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.mocks.MockPortalInitialiser;
import au.com.gaiaresources.bdrs.mocks.MockThemeService;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.model.theme.ThemeDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.theme.ThemeService;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 4/02/14
 * Time: 3:23 PM
 * <p/>
 * Factory of Mock objects used for test and other util helper
 */
public class MockFactory {

    private static int counter = 0;

    public static PortalInitialiser newPortalInitialiser() {
        return new MockPortalInitialiser(newThemeService());
    }

    public static ThemeService newThemeService() {
        ThemeDAO themeDAO = AppContext.getBean(ThemeDAO.class);
        FileService fileService = AppContext.getBean(FileService.class);
        ManagedFileDAO managedFileDAO = AppContext.getBean(ManagedFileDAO.class);
        return new MockThemeService(themeDAO, fileService, managedFileDAO);
    }

    public static User createUser(UserDAO userDAO, String ... roles) {
        String first = "Mock" + (++counter);
        String userName =  first.toLowerCase();
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        String registrationKey = passwordEncoder.encodePassword(StringUtils.generateRandomString(10, 50), userName);
        String last =  "User";
        return userDAO.createUser(
                userName,
                first, last,
                first.toLowerCase() + "@" + last.toLowerCase() + ".com",
                encodedPassword, registrationKey,
                roles);
    }
}
