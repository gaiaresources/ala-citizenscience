package au.com.gaiaresources.bdrs.controller.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.model.user.RegistrationService;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.user.UserImportExportService;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.ZipUtils;

@RolesAllowed({ Role.ADMIN, Role.SUPERVISOR })
@Controller
public class EditUsersController extends AbstractController {
    
    public static final String USER_LISTING_URL = "/admin/userSearch.htm";
    public static final String USER_IMPORT_URL = "/admin/importUsers.htm";
    public static final String USER_EXPORT_URL = "/admin/exportUsers.htm";
    
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    // need to change this from just 'name' as maven filtering was giving us conflicts
    public static final String OUTPUT_ARG_NAME = "userName";
    public static final String EMAIL = "emailAddress";
    public static final String FULL_NAME = "FULL_NAME";
    //public static final String SORT = "SORT";
    public static final String MAX_PER_PAGE = "MAX_PER_PAGE";
    
    // this can't change unless the model changes
    public static final String INPUT_ARG_NAME = "name";

    public static final Integer EXPECTED_NUM_SORT_PARAM = 2;

    public static final Integer SORT_ARG = 0;
    public static final Integer SORT_ORDER = 1;

    // "1" and "2" is returned by displaytag for ASC and DESC so...
    public static final String ASC_ORDER = "1";
    public static final String DESC_ORDER = "2";

    public static final String SEARCH = "search";

    public static final String TableID = "usersearchresults";

    public static final String PAGED_USER_RESULT = "pagedUserResult";

    public static final Integer DEFAULT_MAX_PER_PAGE = 10;
       
    public static final String APPROVE_USER_WEBSERVICE_URL = "/admin/approveUser.htm";
    public static final String PARAM_USER_PK = "userPk";

    private static final String USER_IMPORT_EXPORT_FILENAME = "users.json";

    public static final String POST_KEY_USER_IMPORT_FILE = "users_file";
    public static final String PARAM_ACTIVE = "active";
    public static final String PARAM_CONTAINS = "contains";
   
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private RegistrationService regService;

    @Autowired
    private UserImportExportService importExportService;

    /**
     * Provides access to hibernate sessions. This is used for survey import where the entire import executes
     * in a single transaction.
     */
    @Autowired
    private SessionFactory sessionFactory;
    
    private static ParamEncoder paramEncoder = new ParamEncoder(TableID);

    public static String getPageNumberParamName() {
        return paramEncoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE);
    }

    public static String getSortParamName() {
        return paramEncoder.encodeParameterName(TableTagParameters.PARAMETER_SORT);
    }

    public static String getOrderParamName() {
        return paramEncoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER);
    }

    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = USER_LISTING_URL, method = RequestMethod.GET)
    public ModelAndView searchUsers(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
        return new ModelAndView("editUsers");
    }
    
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = APPROVE_USER_WEBSERVICE_URL, method = RequestMethod.POST)
    public void approveUser(HttpServletRequest request, HttpServletResponse response, 
            @RequestParam(value=PARAM_USER_PK, required=true) int userPk) {
        
        User u = userDAO.getUser(userPk);
        if (u == null) {
            throw new NullPointerException("User returned by id is null : " + userPk);
        }
        u.setActive(true);
        userDAO.updateUser(u);

        // notify user that their account is active
        regService.notifyUserAccountActive(u, RequestContextHolder.getContext().getPortal());
    }
    
    @RolesAllowed({Role.ROOT, Role.ADMIN, Role.SUPERVISOR})
    @RequestMapping(value = USER_EXPORT_URL, method = RequestMethod.GET)
    public void exportUsers( 
            @RequestParam(value = PARAM_ACTIVE, required = false) Boolean active,
            @RequestParam(value = PARAM_CONTAINS, required = false) String contains,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        User currentUser = this.getRequestContext().getUser();
        String[] allowedRolesToSearchFor = Role.getRolesLowerThanOrEqualTo(Role.getHighestRole(currentUser.getRoles()));
        String[] rolesToExclude = Role.getRolesHigherThan(Role.getHighestRole(currentUser.getRoles()));
        
        //find the users
        PagedQueryResult<User> queryResult = userDAO.search(contains, contains, contains, null, allowedRolesToSearchFor, rolesToExclude, null, active);
        List<User> userList = queryResult.getList();

        JSONArray jsonUsers = importExportService.exportArray(userList);
        response.setContentType(ZipUtils.ZIP_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment;filename=user_export.zip");

        ZipEntry entry = new ZipEntry(USER_IMPORT_EXPORT_FILENAME);
        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.putNextEntry(entry);
        out.write(jsonUsers.toJSONString().getBytes(Charset.defaultCharset()));

        out.flush();
        out.close();
    }
    
    /**
     * Creates new users based upon an uploaded file.
     *
     * @param request  the client request.
     * @param response the server response.
     * @return The survey listing page with a message indicating success or failure.
     */
    @RolesAllowed({Role.ROOT, Role.ADMIN, Role.SUPERVISOR})
    @RequestMapping(value = USER_IMPORT_URL, method = RequestMethod.POST)
    public ModelAndView importUsers(
            MultipartHttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        try {
            JSONArray importData = null;
            {
                MultipartFile file = request.getFile(POST_KEY_USER_IMPORT_FILE);
                ZipInputStream zis = new ZipInputStream(file.getInputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                    if (USER_IMPORT_EXPORT_FILENAME.equals(entry.getName())) {
                        int read = zis.read(buffer, 0, buffer.length);
                        while (read > -1) {
                            baos.write(buffer, 0, read);
                            read = zis.read(buffer, 0, buffer.length);
                        }
                    }
                }
                zis.close();
                importData = JSONArray.fromString(new String(baos.toByteArray(), Charset.defaultCharset()));
            }

            if (importData != null) {
                Session sesh = null;
                Transaction tx = null;
                try {
                    sesh = sessionFactory.openSession();

                    tx = sesh.beginTransaction();
                    // clear the import messages before beginning
                    int importCount = importExportService.importArray(sesh, importData);
                    tx.commit();
                    getRequestContext().addMessage("bdrs.user.import.success", new Object[]{ importCount });
                    for (Entry<String, Object[]> entry : importExportService.getMessages().entrySet()) {
                        String key = entry.getKey();
                        if (key.matches("(\\w+\\.)+\\d+")) {
                            // strip the .# from the end of the key
                            key = key.substring(0, key.lastIndexOf("."));
                        }
                        getRequestContext().addMessage(key, entry.getValue());
                    }
                } catch (Throwable t) {
                    if (tx != null) {
                        tx.rollback();
                    }
                    log.error(t.getMessage(), t);
                    getRequestContext().addMessage("bdrs.user.import.error.unknown");
                } finally {
                    if (sesh != null) {
                        sesh.close();
                    }
                }
            } else {
                getRequestContext().addMessage("bdrs.user.import.error.missingFile");
            }
        } catch (JSONException je) {
            log.error(je.getMessage(), je);
            getRequestContext().addMessage("bdrs.user.import.error.json");
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            getRequestContext().addMessage("bdrs.user.import.error.io");
        }

        return new ModelAndView(new RedirectView(EditUsersController.USER_LISTING_URL, true));
    }
}
