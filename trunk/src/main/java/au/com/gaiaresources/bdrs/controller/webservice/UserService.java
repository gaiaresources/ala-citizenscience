package au.com.gaiaresources.bdrs.controller.webservice;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONException;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.json.JSONSerializer;

import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.group.GroupDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.bulkdata.AbstractBulkDataService;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.filter.PortalSelectionFilter;
import org.springframework.web.util.HtmlUtils;


/**
 * The User Service provides a web API for User, Group and Class based
 * services.
 */
@Controller
public class UserService extends AbstractController {

    private Logger log = Logger.getLogger(getClass());

    public static final String PARAM_ACTIVE = "active";
    public static final String PARAM_CONTAINS = "contains";
    public static final String PARAM_PARENT_GROUP_ID = "parentGroupId";

    private static final String BYTE_ENCODING = "UTF-8";

    public static final String MAGIC_KEY = "0a43f170d6c4282682511317d843d050";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private LocationDAO locationDAO;

    @Autowired
    private PortalDAO portalDAO;

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private AbstractBulkDataService bulkDataService;
    
    @Autowired
    @Qualifier("authenticationManager")
    AuthenticationManager authenticationManager;


    /**
     * <p>
     * Performs a query into the database for Users and Groups given the
     * username or group name, user firstname or user lastname.
     * </p>
     * <p/>
     * <p>
     * This function expects the following get parameters
     * </p>
     * <ul>
     * <li>
     * ident - The registration key of the user performing the request.
     * </li>
     * <li>
     * q - The name fragment
     * </li>
     * </ul>
     * <p/>
     * <p>
     * The function will return a JSON encoded representation of all
     * matching User and Group objects.
     * </p>
     * <p>
     * This function is restricted to administration users only.
     * </p>
     */
    @RequestMapping(value = "/webservice/user/searchUserAndGroup.htm", method = RequestMethod.GET)
    public void searchUserAndGroup(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String ident = request.getParameter("ident");
        String query = request.getParameter("q");

        JSONObject result = new JSONObject();

        if (ident != null) {
            User user = userDAO.getUserByRegistrationKey(ident);
            if (user != null && Role.isRoleHigherThanOrEqualTo(Role.getHighestRole(user.getRoles()), Role.POWERUSER)) {

                JSONArray array = new JSONArray();
                for (User u : userDAO.getUsersByNameSearch(query)) {
                    array.add(u.flatten());
                }
                result.put(User.class.getSimpleName(), array);

                array = new JSONArray();
                for (Group g : groupDAO.getGroupsByNameSearch(query)) {
                    array.add(g.flatten());
                }
                result.put(Group.class.getSimpleName(), array);
            } else {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }

        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }


    /* Ping service used by the client to see if there is a connection
     * Returns true
     */
    @RequestMapping(value = "/webservice/user/ping.htm")
    public void ping(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("ping received");
        String callback = request.getParameter("callback");
        StringBuilder output = new StringBuilder();
        callback = HtmlUtils.htmlEscape(callback);
        output.append(callback).append("({0:1});");

        response.setContentType("text/javascript");
        response.getWriter().write(output.toString());
    }


    /**
     * <p>
     * Performs a query into the database to see whether a given username is
     * available
     * </p>
     * <p/>
     * <p>
     * This function expects the following get parameters
     * </p>
     * <ul>
     * <li>
     * q - The desired username
     * </li>
     * </ul>
     * <p/>
     * <p>
     * The function will return a JSON encoded representation of the form:
     * { available : true } or { available : false } based on the
     * availability of the username
     * </p>
     * <p>
     * This function is unrestricted.
     * </p>
     */
    @RequestMapping(value = "/webservice/user/checkUsername.htm", method = RequestMethod.GET)
    public void searchUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String query = request.getParameter("q");

        JSONObject ob = new JSONObject();
        if (userDAO.getUser(query) != null) {
            ob.put("available", false);
        } else {
            ob.put("available", true);
        }

        response.setContentType("application/json");
        response.getWriter().write(ob.toString());
    }

    /**
     * <p>
     * Registers a user in the BDRS database
     * </p>
     * <p/>
     * <p>
     * This function expects the following get parameters
     * </p>
     * <ul>
     * <li>
     * details - a JSON representation of the user to register.
     * <ul>
     *     <li>name - (String) the username</li>
     *     <li>first_name - (String) first name of user</li>
     *     <li>last_name - (String) last name of user</li>
     *     <li>email_address - (String) email address of user</li>
     *     <li>password - (String) desired password</li>
     *     <li>active - if exists, will make user active</li>
     * </ul>
     * signature - MD5 Hash of details + signature
     * </li>
     * </ul>
     * <p/>
     * <p>
     * The function will return a JSON encoded representation of the form:
     * { user_id : <id of newly registered user>, ident : <user regKey } or { user_id : -1 } if
     * the registration failed.
     * </p>
     * <p>
     * This function is restricted by a secret.
     * </p>
     */
    @RequestMapping(value = "/webservice/user/registerUser.htm", method=RequestMethod.POST)
    public void registerUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String query = request.getParameter("details");
        JSONObject ob = new JSONObject();
        if (query != null && !query.isEmpty()) {
            String sig = request.getParameter("signature");
            try {
                JSONObject details = JSONObject.fromStringToJSONObject(query);

                MessageDigest m = MessageDigest.getInstance("MD5");
                String key = (query + MAGIC_KEY);
                m.update(key.getBytes(BYTE_ENCODING), 0, key.length());
                String mySig = new BigInteger(1, m.digest()).toString(16);
                while (mySig.length() < 32) {
                    mySig = "0" + mySig;
                }

                if (mySig.equals(sig)) {
                    User user = userDAO.getUser(String.valueOf(details.get("name")));

                    PasswordEncoder passwordEncoder = new Md5PasswordEncoder();

                    String encodedPassword = passwordEncoder
                            .encodePassword(String.valueOf(details.get("password")), null);

                    String username = String.valueOf(details.get("name")).trim();
                    String firstname = String.valueOf(details.get("first_name")).trim();
                    String lastname = String.valueOf(details.get("last_name")).trim();
                    String email = String.valueOf(details.get("email_address")).trim();

                    if (user == null) {
                        user = userDAO.createUser(username, firstname, lastname, email,
                                encodedPassword,
                                passwordEncoder.encodePassword(username, ""),
                                Role.USER);

                        ob.put("user_id", String.valueOf(user.getId()));
                        ob.put("ident", user.getRegistrationKey());
                        if (details.has("active")) {
                            userDAO.makeUserActive(user, true);
                        }
                    } else {
                        ob.put("userExists", true);
                    }
                } else {
                    log.warn("Attempted user creation from : " + request.getRemoteAddr() + ", Details : " + details +
                            ", My Sig : " + mySig + ", Their Sig : " + sig);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } catch (JSONException jse) {
                log.error("Failed to add user", jse);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } catch (NoSuchAlgorithmException nsae) {
                log.error("Failed to add user", nsae);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.warn("Attempted user creation from : " + request.getRemoteAddr() + ", No or invalid details supplied");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        response.setContentType("application/json");
        response.getWriter().write(ob.toString());
    }

    /**
     * TODO WE MUST ADD CRYPTO TO THIS. (another webservice to get a public key,
     * then this webservice will have to decrypt using a session based private key)
     *
     * @param userName
     * @param password
     * @param request
     * @param response
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @RequestMapping(value = "/webservice/user/validate.htm")
    public void validateUser(
            @RequestParam(value = "username", defaultValue = "") String userName,
            @RequestParam(value = "password", defaultValue = "") String password,
            @RequestParam(value = "portalName", required = false) String portalName,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        // This is a precaution to stop people using this service to brute force passwords.
        Thread.sleep(1000);

        RequestContext requestContext = getRequestContext();
        if (portalName != null) {
            Portal portal = portalDAO.getPortalByName(getRequestContext().getHibernate(),
                    portalName);
            if (portal != null) {
                requestContext.setPortal(portal);

                Session sesh = requestContext.getHibernate();
                Filter filter = sesh.getEnabledFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
                filter.setParameter(PortalPersistentImpl.PORTAL_FILTER_PORTALID_PARAMETER_NAME, portal.getId());

                request.getSession().setAttribute(PortalSelectionFilter.PORTAL_KEY, portal);
            }
        }

        JSONObject validationResponse = new JSONObject();
        User user = userDAO.getUser(userName);
        Md5PasswordEncoder encoder = new Md5PasswordEncoder();

        if (user != null && encoder.isPasswordValid(user.getPassword(), password, null)) {
            validationResponse.put("user", user.flatten(1, true, true));
            validationResponse.put("ident", user.getRegistrationKey());
            validationResponse.put("portal_id", user.getPortal().getId());
            JSONArray locations = new JSONArray();
            for (Location l : locationDAO.getUserLocations(user)) {
                locations.add(l.flatten(true, true));
            }
            validationResponse.put("location", locations);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // support for JSONP
        String callback = request.getParameter("callback");
        if (StringUtils.notEmpty(callback)) {
            callback = HtmlUtils.htmlEscape(callback);
            response.setContentType("application/javascript");
            response.getWriter().write(callback + "(");
        } else {
            response.setContentType("application/json");
        }

        response.getWriter().write(validationResponse.toString());
        if (StringUtils.notEmpty(callback)) {
            response.getWriter().write(");");
        }
    }


    /**
     * Web logging service for the mobile API. This takes and ident and if valid, prints a log message
     */
    @RequestMapping(value = "/webservice/user/log.htm")
    public void logRequest(
            @RequestParam(value = "ident", defaultValue = "") String ident,
            @RequestParam(value = "message", defaultValue = "") String message,
            @RequestParam(value = "level", defaultValue = "") String level,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (ident != null) {
            User user = userDAO.getUserByRegistrationKey(ident);
            if (user != null) {
                log.debug(user.getName() + " : " + message);

                String callback = request.getParameter("callback");
                if (StringUtils.notEmpty(callback)) {
                    callback = HtmlUtils.htmlEscape(callback);
                    response.setContentType("application/javascript");
                    response.getWriter().write(callback + "(");
                } else {
                    response.setContentType("application/json");
                }

                response.getWriter().write("{}");
                if (StringUtils.notEmpty(callback)) {
                    response.getWriter().write(");");
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @RolesAllowed({Role.USER, Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN, Role.ROOT})
    @RequestMapping(value = "/webservice/user/searchUsers.htm", method = RequestMethod.GET)
    public void searchUsers(
            @RequestParam(value = PARAM_PARENT_GROUP_ID, defaultValue = "") Integer parentGroupId,
            @RequestParam(value = PARAM_ACTIVE, required = false) Boolean active,
            @RequestParam(value = PARAM_CONTAINS, required = false) String contains,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);
        PaginationFilter filter = jqGridHelper.createFilter(request);

        User currentUser = this.getRequestContext().getUser();
        String[] allowedRolesToSearchFor = Role.getRolesLowerThanOrEqualTo(Role.getHighestRole(currentUser.getRoles()));
        String[] rolesToExclude = Role.getRolesHigherThan(Role.getHighestRole(currentUser.getRoles()));
        PagedQueryResult<User> queryResult = userDAO.search(contains, contains, contains, filter, allowedRolesToSearchFor, rolesToExclude, parentGroupId, active);

        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());

        if (queryResult.getCount() > 0) {
            for (User user : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(user.getId());
                row
                        .addValue("userName", user.getName())
                        .addValue("firstName", user.getFirstName())
                        .addValue("lastName", user.getLastName())
                        .addValue("emailAddress", user.getEmailAddress());
                builder.addRow(row);
            }
        }
        response.setContentType("application/json");
        response.getWriter().write(builder.toJson());
    }

    @RequestMapping(value = "/webservice/user/getUsers.htm", method = RequestMethod.GET)
    public void getUsers(
            @RequestParam(value = "queryType", defaultValue = "allUsers") String queryType,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        List queryList = new ArrayList();
        if (queryType.equals("allUsers")) {
            queryList = userDAO.getUsers();
        } else if (queryType.equals("group")) {
            queryList = groupDAO.getAllGroups();
        } else if (queryType.equals("project")) {
            queryList = surveyDAO.getActiveSurveysForUser(this.getRequestContext().getUser());
        }

        response.setContentType("application/json");
        response.getWriter().write(JSONSerializer.toJSON(queryList).toString());
    }

    @RequestMapping(value = "/webservice/user/downloadUsers.htm", method = RequestMethod.GET)
    public void downloadUsers(
            @RequestParam(value = "ident", defaultValue = "") String ident,
            @RequestParam(value = PARAM_ACTIVE, required = false) Boolean active,
            @RequestParam(value = PARAM_CONTAINS, required = false) String contains,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        User user;
        if (ident.isEmpty()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            user = userDAO.getUserByRegistrationKey(ident);
            if (user == null) {
                throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        // If you are not the administrator, you can not download users.
        if (!user.isAdmin()) {
            throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED);
        }

        //find the users
        PagedQueryResult<User> queryResult = userDAO.search(contains, contains, contains, null, null, null, null, active);
        List<User> userList = queryResult.getList();

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition",
                "attachment;filename=users_"
                        + Long.valueOf(System.currentTimeMillis()) + ".xls");
        bulkDataService.exportUsers(userList, response
                .getOutputStream());
    }

    /**
     * This method takes user credentials and performs authentication using spring-security. If the user is successfully
     * authenticated, the string 'success' is written to the outputstream.
     * On failure, a 401 is returned. Please note, this, like the user login, is best handled over https.
     * @param username post parameter with the user's name
     * @param password post parameter of the user's password
     * @param request the HttpServletRequest object
     * @param response the HttpServletResponse object.
     * @throws IOException in the case that something goes wrong with the browser connection.
     */
    @RequestMapping(value = "/webservice/user/ajaxAuthenticate.htm", method = RequestMethod.POST)
    public void login(@RequestParam("j_username") String username,
                      @RequestParam("j_password") String password,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
 
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
     
        try {
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);

            response.setContentType("text/plain");
            response.getWriter().write("1");

        } catch (BadCredentialsException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
