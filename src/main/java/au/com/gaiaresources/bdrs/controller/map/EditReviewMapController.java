package au.com.gaiaresources.bdrs.controller.map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.admin.AdminHomePageController;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;

@Controller
public class EditReviewMapController extends AbstractEditMapController {
    
    public static final String BASE_URL = "/bdrs/admin/reviewmap/";
    public static final String EDIT_URL = BASE_URL + "edit.htm";
    public static final String MSG_CODE_EDIT_SUCCESS = "bdrs.map.review.edit.success";

    @Autowired
    private GeoMapService geoMapService;
    
    /**
     * View for editing review map settings.  Allows the user to set up default zoom and 
     * centering, base layer and custom bdrs layers to show on the project maps.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return ModelAndView for map editing.
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = EDIT_URL, method = RequestMethod.GET)
    public ModelAndView editReviewMap(HttpServletRequest request, HttpServletResponse response) {
       return this.editMap(request, geoMapService.getForReview());
    }
    
    /**
     * Handler for submitting review map settings.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return ModelAndView
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = EDIT_URL, method = RequestMethod.POST)
    public ModelAndView submitReviewMap(HttpServletRequest request, HttpServletResponse response) {
        this.submitMap(request, geoMapService.getForReview());
        getRequestContext().addMessage(MSG_CODE_EDIT_SUCCESS);
        return this.redirect(AdminHomePageController.ADMIN_MAP_LANDING_URL);
    }
}
