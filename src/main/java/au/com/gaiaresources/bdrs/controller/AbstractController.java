package au.com.gaiaresources.bdrs.controller;

import au.com.gaiaresources.bdrs.db.TransactionCallback;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.Interceptor;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractController {
    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;
    
    public static final String REDIRECT_HOME = "redirectWithoutModel:/home.htm";
    public static final String REDIRECT_SECURE_HOME = "redirectWithoutModel:/secure/home.htm";
    public static final String REDIRECT_ADMIN_HOME = "redirectWithoutModel:/admin/home.htm";
    public static final String REDIRECT_MOBILE_HOME = "redirectWithoutModel:/bdrs/mobile/home.htm";

    private Logger log = Logger.getLogger(getClass());
    
    protected synchronized TransactionTemplate getTransactionTemplate() {
        if (transactionTemplate == null) {
            transactionTemplate = new TransactionTemplate(transactionManager);
        }
        return transactionTemplate;
    }
    
    @SuppressWarnings("unchecked")
    protected <C> C doInTransaction(TransactionCallback<C> callback) {
        return (C) getTransactionTemplate().execute(callback);
    }
    
    protected RequestContext getRequestContext() {
        return RequestContextHolder.getContext();
    }
    
    protected String getControllerRequestMapping(Class<? extends AbstractController> controllerClass) {
        RequestMapping mapping = controllerClass.getAnnotation(RequestMapping.class);
        if (mapping != null) {
            return mapping.value()[0];
        }
        return "";
    }
    
    /**
     * Writes out our json to the http response. Contains support for JSONP
     * 
     * @param request - the http request object
     * @param response - the http response object
     * @param json - the entire json string
     * @throws IOException
     */
    protected void writeJson(HttpServletRequest request, HttpServletResponse response, String json) throws IOException {
        // support for JSONP

        String callback = request.getParameter(BdrsWebConstants.JSONP_CALLBACK_PARAM);
        // support for JSONP
        if (StringUtils.notEmpty(callback)) {
            callback = HtmlUtils.htmlEscape(callback);
            response.setContentType("application/javascript");
            response.getWriter().write(callback + "(");
        } else {
            response.setContentType("application/json");
        }

        // write our content
        response.getWriter().write(json);
        
        if (StringUtils.notEmpty(callback)) {
            response.getWriter().write(");");
        }
    }

    /**
     * Write json to the http response.
     * @param response HttpServletResponse.
     * @param json JSON formatted string.
     * @throws IOException write error.
     */
    protected void writeJson(HttpServletResponse response, String json) throws IOException {
    	response.setContentType("application/json");
	    response.getWriter().write(json);
	    response.getWriter().flush();
    }

    protected void writeText(HttpServletResponse response, String text) {
        response.setContentType("text/plain");
        try {
            response.getWriter().write(text);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Could not write to response stream", e);
        }
    }
    
    protected String getRedirectHome() {
        return "redirectWithoutModel:/home.htm";
    }
    
    protected String getRedirectSecureHome() {
        return "redirectWithoutModel:/secure/home.htm";
    }
    
    protected String getRedirectAdminHome() {
        return "redirectWithoutModel:/admin/home.htm";
    }
    
    protected String getRedirectSecureMobileHome(){
        return "redirectWithoutModel:/bdrs/mobile/home.htm";
    }
    
    protected void requestRollback(HttpServletRequest request) {
        Interceptor.requestRollback(request);
    }
    
    /**
     * The request returns an unmodifiable map which is undesirable in certain situations
     * 
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String[]> getModifiableParameterMap(HttpServletRequest request) {
        Map<String, String[]> result = new HashMap<String, String[]>(request.getParameterMap());
        return result;
    }
    
    /**
     * helper method for redirecting
     * 
     * @param url
     * @return
     */
    protected ModelAndView redirect(String url) {
        return new ModelAndView(new PortalRedirectView(url, true));
    }
}
