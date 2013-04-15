package au.com.gaiaresources.bdrs.controller.admin;

import javax.annotation.security.RolesAllowed;

import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;

@Controller
public class AdminHomePageController extends AbstractController {

    public static final String ADMIN_TAXONOMY_LANDING_URL = "/bdrs/admin/manageTaxonomy.htm";
    public static final String ADMIN_MAP_LANDING_URL = "/bdrs/admin/manageMaps.htm";
    public static final String ADMIN_HOME_URL = "/admin/home.htm";

    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    


    @RolesAllowed({Role.ROOT, Role.ADMIN,Role.SUPERVISOR,Role.POWERUSER})
    @RequestMapping(value = ADMIN_HOME_URL, method = RequestMethod.GET)
    public ModelAndView render() {
        User user = null;
        ModelAndView view = new ModelAndView("adminHome");
        Record latestRecord = recordDAO.getLatestRecord();
        view.addObject("latestRecord", latestRecord);
        view.addObject("recordCount", recordDAO.countRecords(user));
        view.addObject("uniqueSpeciesCount", recordDAO.countUniqueSpecies());
        view.addObject("userCount", userDAO.countUsers());
        view.addObject("publicSurveys", surveyDAO.getActivePublicSurveys(true));
        return view;
    }
    
    // Misc landing pages for admin functionality - these don't do much so I'm lumping them with the home page controller.
    @RolesAllowed({Role.ROOT, Role.ADMIN,Role.SUPERVISOR})
    @RequestMapping(value="/bdrs/admin/managePeople.htm", method=RequestMethod.GET)
    public ModelAndView renderManagePeople() {
        return new ModelAndView("managePeople");
    }
    
    @RolesAllowed({Role.ROOT, Role.ADMIN})
    @RequestMapping(value="/bdrs/admin/manageData.htm", method=RequestMethod.GET)
    public ModelAndView renderManageData() {
        return new ModelAndView("manageData");
    }
    
    @RolesAllowed({Role.ROOT, Role.ADMIN})
    @RequestMapping(value="/bdrs/admin/managePortal.htm", method=RequestMethod.GET)
    public ModelAndView renderManagePortal() {
        return new ModelAndView("managePortal");
    }
    
    @RolesAllowed({ Role.ROOT, Role.ADMIN })
    @RequestMapping(value=ADMIN_TAXONOMY_LANDING_URL, method=RequestMethod.GET) 
    public ModelAndView renderManageTaxonomy() {
        return new ModelAndView("manageTaxonomy");
    }
    
    @RolesAllowed({ Role.ROOT, Role.ADMIN })
    @RequestMapping(value=ADMIN_MAP_LANDING_URL, method=RequestMethod.GET) 
    public ModelAndView renderManageMaps() {
        return new ModelAndView("manageMaps");
    }
    
    @RolesAllowed({ Role.ROOT, Role.ADMIN })
    @RequestMapping(value="/bdrs/admin/manageSite.htm", method=RequestMethod.GET) 
    public ModelAndView renderManageSite() {
        return new ModelAndView("manageSite");
    }
    
    @RolesAllowed({Role.ROOT, Role.ADMIN,Role.SUPERVISOR,Role.POWERUSER})
    @RequestMapping(value="/bdrs/admin/manageProjects.htm", method=RequestMethod.GET) 
    public ModelAndView renderManageProjects() {
        return new ModelAndView("manageProjects");
    }
    
    /**
     * Landing page for topmost Admin menu item.
     * @return the landing page for the Admin menu.
     */
    @RolesAllowed({Role.ROOT, Role.ADMIN,Role.SUPERVISOR,Role.POWERUSER})
    @RequestMapping(value="/bdrs/admin/adminMenu.htm", method=RequestMethod.GET) 
    public ModelAndView renderAdminMenu() {
        return new ModelAndView("adminMenu");
    }
}
