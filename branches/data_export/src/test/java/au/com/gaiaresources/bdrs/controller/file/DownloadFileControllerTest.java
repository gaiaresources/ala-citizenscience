/**
 * 
 */
package au.com.gaiaresources.bdrs.controller.file;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.service.managedFile.ManagedFileService;



/**
 * @author kehan
 *
 */
public class DownloadFileControllerTest extends AbstractControllerTest {
    private static final String CONTEXT_RELATIVE_URL_FRAGMENT = "images/species/profile/image.jpg";
    public static final String FILENAME = "jackalope.jpg";
    private static final String DESCRIPTION = "Picture of a Jackalope";
    private static final String CREDIT = "from the interwebs";
    private static final String LICENCE = "public domain";

    @Autowired
    private ManagedFileService managedFileService;
    private ManagedFile managedFile;
    
    @Before
    public void setupDownloadFileControllerTest() throws Exception{
        MockMultipartFile file = new MockMultipartFile(FILENAME, FILENAME, "image/jpeg", getClass().getResourceAsStream(FILENAME));
        managedFile = managedFileService.saveManagedFile("", DESCRIPTION, CREDIT, LICENCE, file);
    }
    @Test
    public void testUUIDValidator() throws Exception{
        /*login("admin", "password", new String[] { Role.ADMIN });*/
        
        request.setMethod(RequestMethod.GET.name());
        request.setRequestURI(DownloadFileController.DOWNLOAD_BY_UUID_URL);
        request.setParameter(DownloadFileController.UUID_PARAMETER, CONTEXT_RELATIVE_URL_FRAGMENT);
        
        
        ModelAndView mav = handle(request, response);
        assertRedirect(mav, request.getContextPath() + "/" + CONTEXT_RELATIVE_URL_FRAGMENT, false);
    }
    
    @Test
    public void testDownloadFileGet() throws Exception{
        downloadFile(RequestMethod.GET);
        
    }
    @Test
    public void testDownloadFileHead() throws Exception{
        downloadFile(RequestMethod.HEAD);
    }
    private void downloadFile(RequestMethod method) throws Exception{
        request.setMethod(method.name());
        request.setRequestURI(DownloadFileController.DOWNLOAD_BY_UUID_URL);
        request.setParameter(DownloadFileController.UUID_PARAMETER, managedFile.getUuid());
        ModelAndView mav = handle(request, response);
        assertFileView(mav, managedFile.getContentType());
    }
}
