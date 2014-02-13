package au.com.gaiaresources.bdrs.controller;

import au.com.gaiaresources.bdrs.message.Message;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Controller
public class HomePageController extends AbstractController {

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;

    Logger log = Logger.getLogger(getClass());

    public static final String HOME_URL = "/home.htm";
    public static final String REDIRECT_HOME_URL = "/redirectHome.htm";
    public static final String AUTHENTICATED_REDIRECT_URL = "/authenticated/redirect.htm";
    public static final String LOGIN_FAILED_URL = "/loginfailed.htm";

    @RequestMapping(value = HOME_URL)
    public ModelAndView render(HttpServletRequest request,
            HttpServletResponse response) {
        ModelAndView view = new ModelAndView();
        if (request.getParameter("signin") != null) {
            view.setViewName("signin");
            configureRedirectAfterLogin(request);
        } else {
            view.setViewName("home");
            Record latestRecord = recordDAO.getLatestRecord();
            view.addObject("recordCount", recordDAO.countAllRecords());
            view.addObject("latestRecord", latestRecord);
            view.addObject("uniqueSpeciesCount", recordDAO.countUniqueSpecies());
            view.addObject("userCount", userDAO.countUsers());
            view.addObject("publicSurveys", surveyDAO.getActivePublicSurveys(true));
        }
        return view;
    }

    /**
     * Sends a redirect to the home page.  Used after sign out to preserve the portal prefix.
     * @param request the HTTP request being processed.
     * @param response the HTTP response being produced.
     * @return returns a redirect to /home.htm applying the portal prefix if required.
     */
    @RequestMapping(value = REDIRECT_HOME_URL, method = RequestMethod.GET)
    public ModelAndView redirectHome(HttpServletRequest request,
                               HttpServletResponse response) {
        return new ModelAndView(new PortalRedirectView(HOME_URL, true, true, false));
    }


    /**
     * If the http request contains a parameter called "redirectUrl",  the user will be redirected to the
     * specified URL after signing in.
     * @param request the http request being processed.
     */
    private void configureRedirectAfterLogin(HttpServletRequest request) {
        String redirectTo = request.getParameter("redirectUrl");
        if (redirectTo != null) {

            // Prepend the request domain and context then do a quick validation of the URL
            try {
                URL url = new URL(redirectTo);
                // Make sure the redirect URL matches the current domain.
                URL requestUrl = new URL(request.getRequestURL().toString());
                if (url.getHost().equals(requestUrl.getHost())) {
                    request.getSession().setAttribute(BdrsWebConstants.SAVED_REQUEST_KEY, redirectTo);
                }
                else {
                    log.warn("An attempt to redirect to a different server was detected: "+url);
                }
            }
            catch (MalformedURLException e) {
                log.warn("A redirect to a malformed URL was requested after signing ing: "+redirectTo);
            }
        }
    }

    /**
     * Sends you back to the login page, passing on any URL parameters that were
     * in the original request.
     * 
     * @param request
     * @return
     */
    @RequestMapping(value = LOGIN_FAILED_URL, method = RequestMethod.GET)
    public ModelAndView renderLoginFailed(HttpServletRequest request) {
        getRequestContext().addMessage(new Message("login.failed"));

        String referer = request.getHeader("Referer");

        String urlparams = "";
        if (referer != null && referer.contains("?")) {
            urlparams = referer.substring(referer.indexOf('?'));
        }

        return new ModelAndView(new PortalRedirectView("/home.htm" + urlparams, true));
    }

    /**
     * Redirects logged in user to the appropriate page.
     * 
     * @param req
     * @param res
     * @return
     */
    @RequestMapping(value = AUTHENTICATED_REDIRECT_URL, method = RequestMethod.GET)
    public String redirectForRole(HttpServletRequest req,
            HttpServletResponse res) {
        // Catch a login redirect.
        if (req.getSession().getAttribute("login-redirect") != null) {
            String url = "redirect:"
                    + req.getSession().getAttribute("login-redirect").toString();
            req.getSession().removeAttribute("login-redirect");
            return url;
        }

//        boolean isMobile = mobileHeaderCheck(req);
//        if (isMobile || req.getSession().getAttribute(BdrsWebConstants.PARAM_SURVEY_ID) != null) {
//            return getRedirectSecureMobileHome();
//        } else 
        if (getRequestContext().getRoles() != null) {
            List<String> rolesList = Arrays.asList(getRequestContext().getRoles());
            if (rolesList.contains(Role.ADMIN)) {
                return getRedirectAdminHome();
            } else if (rolesList.contains(Role.SUPERVISOR)) {
                return "redirectWithoutModel:/user/home.htm";
            } else if (rolesList.contains(Role.POWERUSER)) {
                return "redirectWithoutModel:/user/home.htm";
            } else if (rolesList.contains(Role.USER)) {
                return "redirectWithoutModel:/user/home.htm";
            } 
        }
        return getRedirectHome();
    }

    /**
     * Sets a session variable "sessionType" to "desktop" and redirects the user
     * to the home page
     * 
     * @param req
     * @param res
     * @return
     */
    @RequestMapping(value = "/mobile/desktopSession.htm", method = RequestMethod.GET)
    public String setDesktopSession(HttpServletRequest req,
            HttpServletResponse res) {
        req.getSession().setAttribute("sessionType", "desktop");
        return "redirect:/home.htm";
    }

    /**
     * Sets a session variable "sessionType" to "mobile" and redirects the user
     * to the home page.
     * 
     * @param req
     * @param res
     * @return
     */
    @RequestMapping(value = "/mobileSession.htm", method = RequestMethod.GET)
    public String setMobileSession(HttpServletRequest req,
            HttpServletResponse res) {
//        req.getSession().setAttribute("sessionType", "mobile");
//        return "redirect:/mobile/";
        return setDesktopSession(req, res);
    }
    
	/**
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
    @RequestMapping(value = "/bdrs/mobile/home.htm", method = RequestMethod.GET)
	public ModelAndView getHome(HttpServletRequest request,
			HttpServletResponse response) {
//		request.getSession().setAttribute("sessionType", "mobile");
//		ModelAndView mv = new ModelAndView("mobilehome");
//		String ident = getRequestContext().getUser().getRegistrationKey();
//
//		mv.addObject("surveys", surveyDAO.getSurveys(getRequestContext().getUser()));
//		mv.addObject("hometype", "basic");
//
//		if ((request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID) != null)
//				&& (!request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID).isEmpty())) {
//			int id = new Integer(request.getParameter(BdrsWebConstants.PARAM_SURVEY_ID));
//			request.getSession().setAttribute(BdrsWebConstants.PARAM_SURVEY_ID, id);
//		}
//
//		mv.addObject("manifest", "mobile.manifest?ident=" + ident);
//
//		Cookie cookie = new Cookie("regkey", getRequestContext().getUser().getRegistrationKey());
//		response.addCookie(cookie);
//		
//		return mv;
	    return redirect(HOME_URL);
	}
}
