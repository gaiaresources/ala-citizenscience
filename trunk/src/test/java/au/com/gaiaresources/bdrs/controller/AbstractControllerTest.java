package au.com.gaiaresources.bdrs.controller;

import au.com.gaiaresources.bdrs.message.Message;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.Interceptor;
import au.com.gaiaresources.bdrs.servlet.RecaptchaInterceptor;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.simpleframework.http.Form;
import org.simpleframework.http.Part;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;


@Transactional
public abstract class AbstractControllerTest extends AbstractTransactionalTest {
    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    @Qualifier(value = "org.springframework.security.authenticationManager")
    protected ProviderManager authProviderManager;
    @Autowired
    protected UserDetailsService authenticationService;
    @Autowired
    protected UserDAO userDAO;
    @Autowired
    protected PortalDAO portalDAO;

    protected MockHttpServletResponse response;

    private ModelAndView mv;
    private Object controller;
    private HandlerInterceptor[] interceptors;

    @BeforeTransaction
    public final void beforeTx() throws Exception {
        response = new MockHttpServletResponse();

        // Override the security provider.
        List<TestingAuthenticationProvider> providerList = new ArrayList<TestingAuthenticationProvider>();
        providerList.add(new TestingAuthenticationProvider());
        authProviderManager.setProviders(providerList);

        // The following block would normally be done by the interceptor.
        RequestContext c = RequestContextHolder.getContext();
        request.setAttribute(RequestContext.REQUEST_CONTEXT_SESSION_ATTRIBUTE_KEY, c);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if ((securityContext.getAuthentication() != null)
                && (securityContext.getAuthentication().getPrincipal() instanceof UserDetails)) {
            c.setUserDetails((au.com.gaiaresources.bdrs.security.UserDetails) securityContext.getAuthentication().getPrincipal());
        } else {
            // handle an anonymous user
            List<GrantedAuthority> grantedAuth = new ArrayList<GrantedAuthority>(1);
            grantedAuth.add(new GrantedAuthorityImpl(Role.ANONYMOUS));
            // null principal and credentials could pose an issue if they are requested later
            TestingAuthenticationToken token = new TestingAuthenticationToken(null, null, grantedAuth);
            token.setAuthenticated(true);
            securityContext.setAuthentication(token);
            SecurityContextHolder.setContext(securityContext);
        }
    }

    protected RequestContext getRequestContext() {
        return RequestContextHolder.getContext();
    }

    protected void login(String username, String password, String[] roles)
            throws Exception {
        List<GrantedAuthority> grantedAuth = new ArrayList<GrantedAuthority>(
                roles.length);
        for (String role : roles) {
            grantedAuth.add(new GrantedAuthorityImpl(role));
        }

        SecurityContextImpl secureContext = new SecurityContextImpl();
        TestingAuthenticationToken token = new TestingAuthenticationToken(
                username, password, grantedAuth);
        secureContext.setAuthentication(token);
        SecurityContextHolder.setContext(secureContext);
        UserDetails userDetails = authenticationService.loadUserByUsername(username);
        RequestContextHolder.getContext().setUserDetails(userDetails);
    }

    protected ModelAndView handle(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // The introduction of the mvc:resources for serving static content has introduced extra mappings
        // and an extra HandlerAdapter, hence the requirement to get the correct Handlers by name/implementation class.
        final HandlerMapping handlerMapping = (HandlerMapping)applicationContext.getBean("handlerMapping");
        final HandlerAdapter handlerAdapter = applicationContext.getBean(AnnotationMethodHandlerAdapter.class);
        final HandlerExecutionChain handler = handlerMapping.getHandler(request);
        Assert.assertNotNull("No handler found for request, check you request mapping", handler);

        controller = handler.getHandler();
        // if you want to override any injected attributes do it here

        interceptors = handlerMapping.getHandler(request).getInterceptors();
        for (HandlerInterceptor interceptor : interceptors) {
            if (handleInterceptor(interceptor)) {
                final boolean carryOn = interceptor.preHandle(request, response, controller);
                if (!carryOn) {
                    return null;
                }
            }
        }
        mv = handlerAdapter.handle(request, response, controller);
        
        return mv;
    }

    protected Object getController(HttpServletRequest request) throws Exception {
        final HandlerMapping handlerMapping = applicationContext.getBean(HandlerMapping.class);
        final HandlerExecutionChain handler = handlerMapping.getHandler(request);
        Assert.assertNotNull("No handler found for request, check you request mapping", handler);
        return handler.getHandler();
    }

    @AfterTransaction
    public final void afterTx() throws Exception {
        try {
            HandlerInterceptor interceptor;
            if (interceptors != null) {
                for (int i = interceptors.length - 1; i > -1; i--) {
                    interceptor = interceptors[i];
                    if (handleInterceptor(interceptor)) {
                        interceptor.postHandle(request, response, controller, mv);
                    }
                }

                Exception viewException = null;
                for (int i = interceptors.length - 1; i > -1; i--) {
                    interceptor = interceptors[i];
                    if (handleInterceptor(interceptor)) {
                        interceptor.afterCompletion(request, response, controller, viewException);
                    }
                }
            }
        } finally {
            // Normally done by the interceptor.
            RequestContextHolder.clear();
            // equivalent to logging out
            SecurityContextHolder.clearContext();
        }
    }

    private boolean handleInterceptor(HandlerInterceptor interceptor) {
        return !(interceptor instanceof Interceptor)
                && !(interceptor instanceof RecaptchaInterceptor);
    }
    
    protected void assertNotRedirect(ModelAndView mav) {
        Assert.assertFalse("should not be redirect view", mav.getView() instanceof RedirectView);
    }
    
    protected void assertViewName(ModelAndView mav, String viewName) {
        assertNotRedirect(mav);
        Assert.assertNotNull("model and view should not be null", mav);
        Assert.assertEquals("view name does not match", viewName, mav.getViewName());
    }
    
    protected void assertRedirect(ModelAndView mav, String url) {
        Assert.assertTrue("should be redirect view", mav.getView() instanceof RedirectView);
        RedirectView view = (RedirectView)mav.getView();
        Assert.assertEquals("assert redirect url", url, view.getUrl());
    }
    
    protected void assertRedirectAndErrorCode(ModelAndView mav, String url, String errorCode) {
        assertRedirect(mav, url);
        assertMessageCode(errorCode);
    }
    
    /**
     * Checks whether the code exists in the request context
     * @param code - the string code to check for
     */
    protected void assertMessageCode(String code) {
        List<String> msgCodes = getRequestContext().getMessageCodes();
        Assert.assertTrue("Expect error key '" + code + "' in context", listContains(msgCodes, code));
    }
    
    protected void assertMessageCodeAndArgs(String code, Object[] args) {
        List<Message> messageList = getRequestContext().getMessages();
        // first find the message code...
        Message mFound = null;
        for (Message m : messageList) {
            if (m.getCode().equals(code)) {
                mFound = m;
                break;
            }
        }
        Assert.assertNotNull("Could not find code : " + code, mFound);
        // no need to compare for args....
        if (args == null) {
            return;
        }
        
        Object[] mArgs = mFound.getArguments();
        // now assert the message parameters...
        Assert.assertNotNull("message args should not be null", mArgs);
        Assert.assertEquals("arg list length different", args.length, mArgs.length);
        // iterate through and make sure they are the same by position...
        // this assertion method only handles boxes primitives, the .equals method
        // will probably fail for other object types...
        
        for (int i=0; i<args.length; ++i) {
            Assert.assertEquals("arg different at index: " + i, args[i], mArgs[i]);
        }
    }
    
    private boolean listContains(List<String> list, String str) {
        for (String ls : list) {
            if (ls.equals(str)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * To be used by any request that will create an actual http connection 
     * to the server (eg report uploads), this function will create a 
     * temporary webserver that captures these requests, and delegates them 
     * back into the test framework. When a result is received from the test
     * framework, the response will be sent back to the original request.
     * 
     * @param request the client request
     * @param response the server response
     * @throws Exception thrown if an error occurs.
     */
    protected void handleProxyRequest(MockHttpServletRequest request,
            MockHttpServletResponse response) throws Exception {

        SimpleServerProxy container = new SimpleServerProxy();
        Connection connection = new SocketConnection(container);
        SocketAddress address = new InetSocketAddress(request.getServerPort());

        try {
            connection.connect(address);
            handle(request, response);
        } finally {
            connection.close();
            commit();
        }
    }
    
    private class SimpleServerProxy implements Container {
        @Override
        public void handle(Request simpleRequest, Response simpleResponse) {
            try {
                // Need to re-create the request object before sending the 
                // request back into the test framework.
                if (sessionFactory.getCurrentSession().getTransaction().isActive()) {
                    sessionFactory.getCurrentSession().getTransaction().commit();
                }
                beginTransaction();
                // ---------------------------------
                // SimpleRequest -> BDRS Request
                // ---------------------------------
                MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
                req.setMethod(simpleRequest.getMethod());
                req.setRequestURI(simpleRequest.getPath().getPath());

                Form form = simpleRequest.getForm();
                for (String key : form.keySet()) {
                    req.addParameter(key, form.getAll(key).toArray(new String[] {}));
                }

                for (Part part : form.getParts()) {
                    MockMultipartFile mockMultipartFile = new MockMultipartFile(
                            part.getName(), part.getFileName(),
                            part.getContentType().toString(),
                            part.getInputStream());
                    req.addFile(mockMultipartFile);
                }

                boolean completed = false;
                while (!completed) {
                    String path = req.getRequestURI();
                    // Need to set up the servlet path otherwise the restful URL 
                    // will not get interpreted correctly and you will be sent to 
                    // the 404 page.

                    // simpleRequest.getPath().getPath() = <contextpath>/<portal>/<portal_id>/blah.htm
                    // servletPath = /<portal>/<portal_id>/blah.htm
                    String servletPath = path;
                    if (servletPath.startsWith(REQUEST_CONTEXT_PATH)) {
                        servletPath = servletPath.substring(REQUEST_CONTEXT_PATH.length());
                    }
                    req.setServletPath(servletPath);

                    response = new MockHttpServletResponse();

                    AbstractControllerTest.this.handle(req, response);
                    
                    if (response.getRedirectedUrl() != null) {
                        req.setRequestURI(response.getRedirectedUrl());
                    } else if (response.getForwardedUrl() != null) {
                        req.setRequestURI(response.getForwardedUrl());
                    } else {
                        completed = true;
                    }
                }
                if (sessionFactory.getCurrentSession().getTransaction().isActive()) {
                    sessionFactory.getCurrentSession().getTransaction().commit();
                }

                // ---------------------------------
                // BDRS Response -> Simple Response
                // ---------------------------------
                simpleResponse.set("Content-Type", response.getContentType());
                simpleResponse.setCode(response.getStatus());
                simpleResponse.getOutputStream().write(response.getContentAsByteArray());
                simpleResponse.commit();
                simpleResponse.close();

            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
}