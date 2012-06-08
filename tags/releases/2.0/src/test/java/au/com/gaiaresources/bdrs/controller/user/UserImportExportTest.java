package au.com.gaiaresources.bdrs.controller.user;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.admin.EditUsersController;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.StringUtils;
import au.com.gaiaresources.bdrs.util.ZipUtils;

public class UserImportExportTest extends AbstractGridControllerTest {

    @Autowired
    private UserDAO userDAO;

    private void createNewRequest() {
        request = createMockHttpServletRequest();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
    }

    @Test
    public void testImportExportDuplicates() throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        List<User> users = userDAO.getUsers();

        request.setMethod("GET");
        request.setRequestURI(EditUsersController.USER_EXPORT_URL);
        
        handle(request, response);
        Assert.assertEquals(ZipUtils.ZIP_CONTENT_TYPE, response.getContentType());
        byte[] exportContent = response.getContentAsByteArray();

//        // Uncomment if you want to capture the test file
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/tmp/test_survey_export_" + survey.getId() + ".zip"));
//        bos.write(exportContent);
//        bos.flush();
//        bos.close();

        response = new MockHttpServletResponse();
        createNewRequest();
        Assert.assertTrue(response.getContentAsByteArray().length == 0);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.setMethod("POST");
        req.setRequestURI(EditUsersController.USER_IMPORT_URL);
        req.addFile(new MockMultipartFile(EditUsersController.POST_KEY_USER_IMPORT_FILE, exportContent));
        handle(request, response);
        
        // Test we have no additional users as it should not overwrite existing accounts
        Assert.assertEquals(users.size(), userDAO.getUsers().size());
    }

    @Test
    public void testImportExportNewUsers() throws Exception {
        requestDropDatabase();
        // create two new user accounts for the test
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "someone@somewhere.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        String registrationKey = passwordEncoder.encodePassword(StringUtils.generateRandomString(10, 50), emailAddr);

        User newUser = userDAO.createUser("somebody", "Somebody", "Special", emailAddr, encodedPassword, registrationKey, new String[] { Role.USER });
        User newUser2 = userDAO.createUser("nobody", "Nobody", "Nada", "noone@example.com.au", encodedPassword, registrationKey, new String[] { Role.POWERUSER });
        
        List<User> users = userDAO.getUsers();

        response = new MockHttpServletResponse();
        createNewRequest();
        login("admin", "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI(EditUsersController.USER_EXPORT_URL);
        
        handle(request, response);
        Assert.assertEquals(ZipUtils.ZIP_CONTENT_TYPE, response.getContentType());
        byte[] exportContent = response.getContentAsByteArray();
        

//      // Uncomment if you want to capture the test file
//      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/tmp/test_user_export.zip"));
//      bos.write(exportContent);
//      bos.flush();
//      bos.close();
        
        // now delete all the new users
        userDAO.delete(newUser);
        userDAO.delete(newUser2);
        
        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
        
        // should only have 2 less users
        Assert.assertEquals(users.size() - 2, userDAO.getUsers().size());

        response = new MockHttpServletResponse();
        createNewRequest();
        Assert.assertTrue(response.getContentAsByteArray().length == 0);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        // have to log in again for the new request
        login("admin", "password", new String[]{Role.ADMIN});
        
        req.setMethod("POST");
        req.setRequestURI(EditUsersController.USER_IMPORT_URL);
        req.addFile(new MockMultipartFile(EditUsersController.POST_KEY_USER_IMPORT_FILE, exportContent));
        handle(request, response);
        
        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());

        // should match the initial user list
        List<User> theseUsers = userDAO.getUsers();
        Assert.assertEquals(users.size(), theseUsers.size());
        
        for (User expected : users) {
            // can't use .equals here because the ids will be different
            User actual = userDAO.getUser(expected.getName());
            Assert.assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
            Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
            Assert.assertEquals(expected.getFullName(), actual.getFullName());
            Assert.assertEquals(expected.getLastName(), actual.getLastName());
            Assert.assertEquals(expected.getPassword(), actual.getPassword());
            Assert.assertEquals(expected.getRegistrationKey(), actual.getRegistrationKey());
            Assert.assertEquals(expected.getActive(), actual.getActive());
            Assert.assertArrayEquals(expected.getRoles(), actual.getRoles());
        }
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
