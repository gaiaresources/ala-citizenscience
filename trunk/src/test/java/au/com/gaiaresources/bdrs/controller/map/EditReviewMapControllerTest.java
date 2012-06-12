package au.com.gaiaresources.bdrs.controller.map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.admin.AdminHomePageController;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;

public class EditReviewMapControllerTest extends AbstractEditMapControllerTest {   
    
	@Autowired
	private GeoMapDAO geoMapDAO;
	
	/**
     * Test that the survey map editing interface can be correctly opened.
     * @throws Exception
     */
    @Test
    public void testEditMap() throws Exception {
        this.testEditMap(EditReviewMapController.EDIT_URL);
    }
    
    /**
     * Test that survey map settings can be saved and that they show on the record entry form.
     * @throws Exception
     */
    @Test
    public void testSaveMapSettings() throws Exception {
    	ModelAndView mv = this.testSaveMapSettings(EditReviewMapController.EDIT_URL);
    	assertMessageCode(EditReviewMapController.MSG_CODE_EDIT_SUCCESS);
    	assertRedirect(mv, AdminHomePageController.ADMIN_MAP_LANDING_URL);
    }

	@Override
	protected GeoMap createMap() {
    	GeoMap geoMap = new GeoMap();
        geoMap.setOwner(MapOwner.REVIEW);
        return geoMapDAO.save(geoMap);
	}

	@Override
	protected GeoMap getMap() {
		return geoMapDAO.getForOwner(sesh, MapOwner.REVIEW);
	}
}
