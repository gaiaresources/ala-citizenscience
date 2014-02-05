package au.com.gaiaresources.bdrs.mocks;

import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.theme.Theme;
import au.com.gaiaresources.bdrs.model.theme.ThemeDAO;
import au.com.gaiaresources.bdrs.service.theme.ThemeService;
import au.com.gaiaresources.bdrs.util.file.FileSystem;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: serge
 * Date: 4/02/14
 * <p/>
 * A ThemeService that doesn't deploy (write) the theme files on the file system
 */
public class MockThemeService extends ThemeService {

    public MockThemeService(ThemeDAO themeDAO, FileService fileService, ManagedFileDAO managedFileDAO) {
        super.themeDAO = themeDAO;
        super.fileService = fileService;
        super.managedFileDAO = managedFileDAO;
    }

    @Override
    public void processThemeData(Theme theme, FileSystem themeFiles, File destDir, String assetContext) throws IOException {
        // do nothing
    }
}
