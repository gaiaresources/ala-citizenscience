package au.com.gaiaresources.bdrs.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.security.Role;

public class HomePageControllerTest extends AbstractControllerTest {

    @Before
    public void setup() {
        //Note that the device ids do not match a real life ua string
    }

    @Test
    public void testRenderIE9() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(HomePageController.HOME_URL);
        request.addHeader("user-agent", "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US))");
        testDesktop(request);
    }

    @Test
    public void testRenderIE8() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(HomePageController.HOME_URL);
        request.addHeader("user-agent", "Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; Media Center PC 4.0; SLCC1; .NET CLR 3.0.04320)");
        testDesktop(request);
    }

    @Test
    public void testRenderIE7() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(HomePageController.HOME_URL);
        request.addHeader("user-agent", "Mozilla/5.0 (Windows; U; MSIE 7.0; Windows NT 6.0; en-US)");
        testDesktop(request);
    }

    @Test
    public void testRenderFirefox() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(HomePageController.HOME_URL);
        request.addHeader("user-agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.1.19) Gecko/20081202 Firefox (Debian-2.0.0.19-0etch1)");
        testDesktop(request);
    }

    @Test
    public void testRenderChrome() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(HomePageController.HOME_URL);
        request.addHeader("user-agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0");
        testDesktop(request);
    }

    private void testDesktop(MockHttpServletRequest request) throws Exception {
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "home");

        login("admin", "password", new String[] { Role.ADMIN });
        mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "home");

        request.addParameter("signin", "true");
        mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "signin");
    }
}