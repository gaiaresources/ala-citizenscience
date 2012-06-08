package au.com.gaiaresources.bdrs.servlet;

import au.com.gaiaresources.bdrs.controller.RecaptchaController;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class RecaptchaInterceptor extends HandlerInterceptorAdapter {
    private static final String RECAPTCHA_PERFORMED_SESSION_ATTRIBUTE = "climatewatch.recaptcha.performed";
    private static final String REDIRECT_TO_AFTER_RECAPTCHA_SESSION_ATTRIBUTE = "climatewatch.after.recaptcha.redirect";
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private RedirectionService redirectionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler.getClass().getAnnotation(RecaptchaProtected.class) != null) {
            HttpSession s = request.getSession();
            Object recaptchaPerformed = s.getAttribute(RECAPTCHA_PERFORMED_SESSION_ATTRIBUTE);
            if (recaptchaPerformed == null) {
                String redirect = UrlAssembler.assembleUrlFor(request, RecaptchaController.RECAPTCHA_URL);
                String currentUrl = UrlAssembler.assembleUrlFor(request);
                response.sendRedirect(redirect);
                s.setAttribute(REDIRECT_TO_AFTER_RECAPTCHA_SESSION_ATTRIBUTE, currentUrl);
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception 
    {
        if (handler instanceof RecaptchaController) {
            if (modelAndView.getModel().containsKey("success")) {
                RedirectView rv = null;
                Boolean result = (Boolean) modelAndView.getModel().get("success");
                String redirectTo = null;
                if (result) {
                    request.getSession().setAttribute(RECAPTCHA_PERFORMED_SESSION_ATTRIBUTE, Boolean.TRUE);
                    redirectTo = (String) request.getSession().getAttribute(REDIRECT_TO_AFTER_RECAPTCHA_SESSION_ATTRIBUTE);
                    if (redirectTo == null || redirectTo.length() == 0) {
                        rv = new PortalRedirectView("/home.htm", true);
                    } else {
                        rv = new PortalRedirectView(redirectTo, true);
                    }
                } else {
                    rv = new PortalRedirectView(RecaptchaController.RECAPTCHA_URL, true);
                }
                modelAndView.getModel().clear();
                modelAndView.setView(rv);
            }
        }
    }
}
