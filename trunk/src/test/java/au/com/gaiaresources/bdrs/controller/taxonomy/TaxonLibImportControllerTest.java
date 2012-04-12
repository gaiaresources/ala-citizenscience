package au.com.gaiaresources.bdrs.controller.taxonomy;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.security.Role;

/**
 * Runs basic tests for testing the rendering of the TaxonLib import controller.
 *
 */
public class TaxonLibImportControllerTest extends AbstractControllerTest {

	/**
	 * Basic test for page rendering.
	 * @throws Exception
	 */
	@Test
	public void testRenderLandingPage() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		
		request.setRequestURI(TaxonLibImportController.TAXON_LIB_IMPORT_URL);
		request.setMethod("GET");
		
		ModelAndView mv = handle(request, response);
		
		assertViewName(mv, TaxonLibImportController.TAXON_LIB_SELECT_IMPORT_VIEW);
	}
	
	/**
	 * Basic test for page rendering.
	 * @throws Exception
	 */
	@Test
	public void testRenderNswFlora() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		
		request.setRequestURI(TaxonLibImportController.NSW_FLORA_IMPORT_URL);
		request.setMethod("GET");
		
		ModelAndView mv = handle(request, response);
		
		assertViewName(mv, TaxonLibImportController.NSW_IMPORT_VIEW);
	}
	
	/**
	 * Basic test for page rendering.
	 * @throws Exception
	 */
	@Test
	public void testRenderMax() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		
		request.setRequestURI(TaxonLibImportController.MAX_IMPORT_URL);
		request.setMethod("GET");
		
		ModelAndView mv = handle(request, response);
		
		assertViewName(mv, TaxonLibImportController.MAX_IMPORT_VIEW);
	}
	
	/**
	 * Basic test for page rendering.
	 * @throws Exception
	 */
	@Test
	public void testRenderAfd() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		
		request.setRequestURI(TaxonLibImportController.AFD_IMPORT_URL);
		request.setMethod("GET");
		
		ModelAndView mv = handle(request, response);
		
		assertViewName(mv, TaxonLibImportController.AFD_IMPORT_VIEW);
	}
	
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
