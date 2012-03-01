package au.com.gaiaresources.bdrs.controller.user;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

@Controller
public class UserHomePageController extends AbstractController {
    Logger log = Logger.getLogger(UserHomePageController.class);
    
    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private LocationDAO locationDAO;
    
    @Autowired
    private SurveyDAO surveyDAO;
    
    @RolesAllowed({Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN,Role.ROOT})
    @RequestMapping(value = "/user/home.htm", method = RequestMethod.GET)
    public ModelAndView render(HttpServletRequest request) {
        ModelAndView view = new ModelAndView("userHome");
        Record latestRecord = recordDAO.getLatestRecord();
        view.addObject("latestRecord", latestRecord);
        view.addObject("recordCount", recordDAO.countRecords(getRequestContext().getUser()));
        view.addObject("locationCount", locationDAO.countUserLocations(getRequestContext().getUser()));
        return view;
    }
    
    /**
     * Landing page for the Contribute menu item which shows a list of links to all survey contribution forms.
     * @param request
     * @return the landing page for the Contribute menu item
     */
    @RolesAllowed({Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN,Role.ROOT})
    @RequestMapping(value = "/user/contribute.htm", method = RequestMethod.GET)
    public ModelAndView renderContribute(HttpServletRequest request) {
        ModelAndView view = new ModelAndView("contribute");
        User user = RequestContextHolder.getContext().getUser();
        List<Survey> surveys = user != null ? surveyDAO.getActiveSurveysForUser(user) : surveyDAO.getActivePublicSurveys(false);
        view.addObject("surveys", surveys);
        return view;
    }
    
    /**
     * Landing page for the Profile menu item which shows links to all Profile menu items.
     * @param request
     * @return the landing page for the Profile menu item
     */
    @RolesAllowed({Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN,Role.ROOT})
    @RequestMapping(value = "/user/profile.htm", method = RequestMethod.GET)
    public ModelAndView renderProfile(HttpServletRequest request) {
        ModelAndView view = new ModelAndView("profile");
        return view;
    }
}
