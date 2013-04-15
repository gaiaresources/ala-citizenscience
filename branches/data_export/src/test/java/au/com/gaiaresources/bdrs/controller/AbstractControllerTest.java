package au.com.gaiaresources.bdrs.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.gaiaresources.bdrs.controller.survey.SurveyBaseController;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
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
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.view.RedirectView;

import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.message.Message;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.Interceptor;
import au.com.gaiaresources.bdrs.servlet.RecaptchaInterceptor;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.servlet.view.FileView;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.DateFormatter;


@Transactional
public abstract class AbstractControllerTest extends AbstractTransactionalTest {
    protected static final Integer INTEGER_WITH_RANGE_LOWER_LIMIT = 5;

    protected static final Integer INTEGER_WITH_RANGE_UPPER_LIMIT = 10;

    @SuppressWarnings("unused")
    protected Logger log = Logger.getLogger(getClass());

    @Autowired
    @Qualifier(value = "org.springframework.security.authenticationManager")
    protected ProviderManager authProviderManager;
    @Autowired
    protected UserDetailsService authenticationService;
    @Autowired
    protected UserDAO userDAO;
    @Autowired
    protected PortalDAO portalDAO;
    @Autowired
    protected TaxaDAO taxaDAO;
    @Autowired
    protected AttributeDAO attributeDAO;
    @Autowired
    protected CensusMethodDAO cmDAO;
    @Autowired
    protected RecordDAO recDAO;

    @Autowired
    protected SurveyDAO surveyDAO;
    
    protected CensusMethod attrCm;
    
    protected MockHttpServletResponse response;

    private ModelAndView mv;
    private Object controller;
    private HandlerInterceptor[] interceptors;

    protected SimpleDateFormat bdrsDateFormat = new SimpleDateFormat("dd MMM yyyy");

    protected List<IndicatorSpecies> speciesList;

    /**
     * An arbitrary value. If you require greater/less tolerance in the subclassed tests, just write
     * over this protected variable!
     */
    protected double DEFAULT_TOLERANCE = 0.0001;
    
    protected User currentUser = null;
    
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

    public void doSetup() {
        createCensusMethodForAttributes();
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
        assertRedirect(mav, url, true);
    }
    
    protected void assertFileView(ModelAndView mav) {
        Assert.assertTrue("Must be an instance of FileView", mav.getView() instanceof FileView);
    }
    protected void assertFileView(ModelAndView mav, String contentType) {
        assertFileView(mav);
        FileView fileView = (FileView) mav.getView();
        Assert.assertEquals("Content types must be equal", contentType, fileView.getContentType());
    }

    protected void assertRedirect(ModelAndView mav, String url, boolean prependPortalToExpected) {
        Assert.assertTrue("should be redirect view", mav.getView() instanceof RedirectView);
        RedirectView view = (RedirectView)mav.getView();
        assertUrlEquals("assert redirect url", url, view.getUrl(), prependPortalToExpected);
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
        SimpleServerProxy container = new SimpleServerProxy(getRequestContext().getPortal().getId());
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

    /**
     * Checks a URL, first pre-pending the portal id to the expected URL.
     * @param expected the expected URL, minus the portal prefix.
     * @param actual the actual URL.
     */
    protected void assertUrlEquals(String expected, String actual) {
        assertUrlEquals("", expected, actual);
    }

    /**
     * Checks a URL, first pre-pending the portal id to the expected URL.
     * @param comment the JUnit test description
     * @param expected the expected URL, minus the portal prefix.
     * @param actual the actual URL.
     */
    protected void assertUrlEquals(String comment, String expected, String actual) {
        assertUrlEquals(comment, expected, actual, true);
    }

    /**
     * Checks a URL, first pre-pending the portal id to the expected URL.
     * @param comment the JUnit test description
     * @param expected the expected URL, minus the portal prefix.
     * @param actual the actual URL.
     * @param prependPortalToExpected set to true if /portal/{id} should be prepended to the expected URL.
     */
    protected void assertUrlEquals(String comment, String expected, String actual, boolean prependPortalToExpected) {
        if (prependPortalToExpected) {
            expected = "/portal/"+defaultPortal.getId()+expected;
        }
        Assert.assertEquals(comment, expected, actual);
    }



    private class SimpleServerProxy implements Container {

        private int portalId;

        /**
         * Creates a new SimpleServerProxy.
         * @param portalId the primary key of the portal to be used by the listening server.
         */
        public SimpleServerProxy(int portalId) {
            this.portalId = portalId;
        }

        @Override
        public void handle(Request simpleRequest, Response simpleResponse) {
            try {
                // Need to re-create the request object before sending the 
                // request back into the test framework.
                if (sessionFactory.getCurrentSession().getTransaction().isActive()) {
                    sessionFactory.getCurrentSession().getTransaction().commit();
                }
                beginTransaction();
                getRequestContext().setPortal(portalDAO.getPortal(portalId));
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
    
    public void createCensusMethodForAttributes() {
        CensusMethod cm = new CensusMethod();
        cm.setName("Test Attribute Census Method");
        cm.setDescription("Test Attribute Census Method");
        cm.setTaxonomic(Taxonomic.NONTAXONOMIC);
        
        cm.setRunThreshold(false);
        
        // create at least one attribute for the census method to allow it to 
        // be added to forms
        List<Attribute> attributes = new ArrayList<Attribute>(1);
        AttributeType attrType = AttributeType.STRING;
        Attribute attr = new Attribute();
        attr.setRequired(true);
        String attName = "attribute_" + attrType.toString();
        attr.setName(attName);
        attr.setDescription(attName);
        attr.setTypeCode(attrType.getCode());
        attr.setScope(null);
        attr = taxaDAO.save(attr);
        attributes.add(attr);
        cm.setAttributes(attributes);
        
        attrCm = cmDAO.save(cm);
    }
    
    private void createAttributes(List<Attribute> attributes, Set<AttributeValue> attVals, 
            int seed, boolean save, Map<Attribute, Object> valueObj, String prefix, Map<String, String> requestMap) {
        AttributeValue attrVal;
        for(Attribute attr : attributes) {
            attrVal = new AttributeValue();
            attrVal.setAttribute(attr);
            generateAttributeValue(attr, seed++, true, save, valueObj, prefix, requestMap);
            if (save) {
                attrVal = attributeDAO.save(attrVal);
            }
            attVals.add(attrVal);
        }
    }
    
    protected Object genRandomAttributeValue(Attribute a, int seed, Map<Attribute, Object> attParamMap, String prefix, Map<String, String> requestMap) {
        AttributeValue value = new AttributeValue();
        value.setAttribute(a);
        return genRandomAttributeValue(value, seed, false, false, attParamMap, prefix, requestMap);
    }
    
    protected Object setSpecificAttributeValue(Attribute a, String strVal, Map<Attribute, Object> attParamMap, String prefix, Map<String, String> requestMap) {
        AttributeValue value = new AttributeValue();
        value.setAttribute(a);
        return setSpecificAttributeValue(value, strVal, false, false, attParamMap, prefix, requestMap);
    }
    
    /**
     * Assigns a new generated value to an attribute value and returns a string representation of
     * the value for later assertion
     * 
     * Intended for use with subclasses that need to alter and assert an AttributeValue
     * 
     * @param av - AttributeValue
     * @param seed - seed used to generate random data
     * @param assign - boolean - if true will assign the value to the attribute value. else will just
     * return the string value
     * @param attParamMap 
     * @return the string representation of the generated value. e.g. boolean true will be returned as 'true'
     * @throws IOException 
     * @throws ParseException 
     */
    protected Object genRandomAttributeValue(AttributeValue av, int seed, boolean assign, boolean save, Map<Attribute, Object> attParamMap, String prefix, Map<String, String> requestMap) {
        Attribute a = av.getAttribute();
        Object valueObj = null;
        String paramName = WebFormAttributeParser.getParamKey(prefix,  a);
        switch (a.getType()) {
            case INTEGER:
            case DECIMAL:
                if (assign) {
                    av.setNumericValue(new BigDecimal(Double.valueOf(seed)));
                }
                valueObj = Integer.toString(seed);
                break;
            case INTEGER_WITH_RANGE:
            {
                Integer lower = Integer.parseInt(a.getOptions().get(0).getValue());
                Integer upper = Integer.parseInt(a.getOptions().get(1).getValue());
                Integer value = (seed%(upper-lower)) + lower;
                if (assign) {
                    av.setNumericValue(new BigDecimal(value));
                }
                valueObj = Integer.toString(value);
                break;
            }
            case DATE:
            {
                Date d = getDate(2010, 10, seed%30);
                if (assign) {
                    av.setDateValue(d);
                }
                valueObj = DateFormatter.format(d, DateFormatter.DAY_MONTH_YEAR);
                break;
            }
            case TIME:
            {
                Date d = getDate(2010, 10, seed%30, seed%24, seed%60);
                String time = DateFormatter.format(d, DateFormatter.TIME);
                valueObj = time;
                break;
            }
            case REGEX:
            case BARCODE:
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            {
                String text = String.format("seed is : %d", seed);
                valueObj = text;
                break;
            }
            case HTML:
            case HTML_NO_VALIDATION:
            case HTML_COMMENT:
            case HTML_HORIZONTAL_RULE:
            {
                String text = String.format("<p>seed is : %d</p>", seed);
                valueObj = text;
                break;
            }
            case STRING_WITH_VALID_VALUES:
            case MULTI_CHECKBOX:
            case MULTI_SELECT:
            {
                int listIdx = seed % a.getOptions().size();
                String text = a.getOptions().get(listIdx).getValue();
                valueObj = text;
                break;
            }
            case SINGLE_CHECKBOX:
            {
                String boolText = Boolean.toString((seed % 2) == 0);
                if (assign) {
                    av.setBooleanValue(boolText);
                }
                valueObj = boolText;
                break;
            }
            case IMAGE:
            case AUDIO:
            case FILE:
            {
                // the string value becomes the file name
                // putting a space in here deliberately
                String filenameText = "filename " + Integer.toString(seed) + ".bleh"; 
                valueObj = filenameText;
                break;
            }
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                valueObj = new LinkedHashMap<Attribute, Object>();
                Set<Record> recs = createRecordList(av, seed, save, (Map<Attribute, Object>) valueObj, paramName, requestMap);
                if (assign) {
                    av.setRecords(recs);
                }
                if (save) {
                    attributeDAO.save(av);
                }
                break;
            case SPECIES:
            {
                valueObj = getRandomSpecies(seed).getScientificName();
                break;
            }
            default:
                // not handled. fail the test to notify the test writer
                Assert.fail("Attribute type : " + a.getTypeCode() + " is not handled. Fix it!");
                // we will never hit this return null but eclipse doesn't like the non return.
                return null;
        }
        if (assign) {
            if (valueObj instanceof String) {
                av.setStringValue((String)valueObj);
            } else {
                av.setStringValue(null);
            }
        }
        if (attParamMap != null) {
            attParamMap.put(av.getAttribute(), valueObj);
        }
        if (requestMap != null && valueObj != null) {
            if (!AttributeType.isCensusMethodType(a.getType())) {
                requestMap.put(paramName, String.valueOf(valueObj));
            }
        }
        return valueObj;
    }

    protected Object setSpecificAttributeValue(AttributeValue av, String value, boolean assign, boolean save, Map<Attribute, Object> attParamMap, String prefix, Map<String, String> requestMap) {
        Attribute a = av.getAttribute();
        Object valueObj = null;
        String paramName = WebFormAttributeParser.getParamKey(prefix,  a);
        switch (a.getType()) {
            case INTEGER:
            case DECIMAL:
                if (assign) {
                    av.setNumericValue(new BigDecimal(Double.valueOf(value)));
                }
                valueObj = value;
                break;
            case INTEGER_WITH_RANGE:
            {
                if (assign) {
                    av.setNumericValue(new BigDecimal(value));
                }
                valueObj = value;
                break;
            }
            case DATE:
            {
                Date d = DateFormatter.parse(value, DateFormatter.DAY_MONTH_YEAR);
                if (assign) {
                    av.setDateValue(d);
                }
                valueObj = value;
                break;
            }
            case TIME:
            {
                valueObj = value;
                break;
            }
            case REGEX:
            case BARCODE:
            case STRING:
            case STRING_AUTOCOMPLETE:
            case TEXT:
            {
                valueObj = value;
                break;
            }
            case HTML:
            case HTML_NO_VALIDATION:
            case HTML_COMMENT:
            case HTML_HORIZONTAL_RULE:
            {
                valueObj = value;
                break;
            }
            case STRING_WITH_VALID_VALUES:
            case MULTI_CHECKBOX:
            case MULTI_SELECT:
            {
                valueObj = value;
                break;
            }
            case SINGLE_CHECKBOX:
            {
                if (assign) {
                    av.setBooleanValue(value);
                }
                valueObj = value;
                break;
            }
            case IMAGE:
            case AUDIO:
            case FILE:
            {
                // the string value becomes the file name
                // putting a space in here deliberately
                valueObj = value;
                break;
            }
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                valueObj = new LinkedHashMap<Attribute, Object>();
                int seed = 0;
                try {
                    seed = Integer.valueOf(value);
                } catch (Exception e) {
                    for (int i = 0; i < value.length(); i++) {
                        seed += (int) value.charAt(i);
                    }
                }
                Set<Record> recs = createRecordList(av, seed, save, (Map<Attribute, Object>) valueObj, paramName, requestMap);
                if (assign) {
                    av.setRecords(recs);
                }
                if (save) {
                    attributeDAO.save(av);
                }
                break;
            case SPECIES:
            {
                valueObj = value;
                break;
            }
            default:
                // not handled. fail the test to notify the test writer
                Assert.fail("Attribute type : " + a.getTypeCode() + " is not handled. Fix it!");
                // we will never hit this return null but eclipse doesn't like the non return.
                return null;
        }
        if (assign) {
            if (valueObj instanceof String) {
                av.setStringValue((String)valueObj);
            } else {
                av.setStringValue(null);
            }
        }
        if (attParamMap != null) {
            attParamMap.put(av.getAttribute(), valueObj);
        }
        if (requestMap != null && valueObj != null) {
            if (!AttributeType.isCensusMethodType(a.getType())) {
                requestMap.put(paramName, String.valueOf(valueObj));
            }
        }
        return valueObj;
    }
    
    private Set<Record> createRecordList(AttributeValue attrValue, int seed, boolean save, Map<Attribute, Object> valueObj, String prefix, Map<String, String> requestMap) {
        Set<Record> records = new LinkedHashSet<Record>();
        if (attrValue != null) {
            Attribute att = attrValue.getAttribute();
            // the type of the attribute must be census method type
            if (AttributeType.isCensusMethodType(att.getType())) {
                if (attrValue.getRecords() != null && !attrValue.getRecords().isEmpty()) {
                    int recCount = 0;
                    for (Record rec : attrValue.getRecords()) {
                        rec.setUser(getUser());
                        rec.setAttributeValue(attrValue);
                        CensusMethod cm = att.getCensusMethod();
                        createAttributes(cm.getAttributes(), rec.getAttributes(), seed, save, valueObj, prefix+
                                         String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, rec.getId()+"_"), requestMap);
                        if (save) {
                            rec = recDAO.saveRecord(rec);
                        }
                        records.add(rec);
                        recCount++;
                    }
                } else {
                    Record rec = new Record();
                    rec.setUser(getUser());
                    rec.setAttributeValue(attrValue);
                    CensusMethod cm = att.getCensusMethod();
                    createAttributes(cm.getAttributes(), rec.getAttributes(), seed, save, valueObj, prefix+
                                     String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, ""), requestMap);
                    if (save) {
                        rec = recDAO.saveRecord(rec);
                    }
                    records.add(rec);
                }
            }
        }
        return records;
    }
    
    private User getUser() {
        if (currentUser == null) {
            currentUser = userDAO.getUser("admin");
        }
        return currentUser;
    }

    /**
     * Returns a random species.  Used for setting SPECIES type attribute values
     * @param seed a random integer to help pick the species
     * @return a persisted IndicatorSpecies to use for an AttributeValue
     */
    protected IndicatorSpecies getRandomSpecies(int seed) {
        if (speciesList == null) {
            speciesList = taxaDAO.getIndicatorSpecies();
        }
        return this.speciesList.get(seed % speciesList.size());
    }

    /**
     * Just a way to make test data without writing lots of code 
     * 
     * @param a
     * @param seed
     * @param valueObj 
     * @param requestMap 
     * @param prefix 
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    protected AttributeValue generateAttributeValue(Attribute a, int seed, 
            boolean generateAv, boolean save, Map<Attribute, Object> valueObj, String prefix, Map<String, String> requestMap) {
        AttributeValue av = new AttributeValue();
        av.setAttribute(a);
        if (generateAv) {
            genRandomAttributeValue(av, seed, true, save, valueObj, prefix, requestMap);
        }
        
        av.setRunThreshold(false);
        
        if (save) {
            attributeDAO.save(av);
        }
        return av;
    }
    

    protected Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }

    protected Date getDate(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, hour, minute);
        return cal.getTime();
    }
    
    protected void assertAttributes(TypedAttributeValue recAttr, Map<String, String> params, String key) {
        Attribute attr = recAttr.getAttribute();
        if (AttributeType.isCensusMethodType(attr.getType())) {
            // assert all of the child record attributes
            String attPrefix = AttributeType.CENSUS_METHOD_COL.equals(attr.getType()) ? 0 + "_" : "";
            int recCount = 0;
            if (recAttr.getRecords() != null) {
                for (Record rec : recAttr.getRecords()) {
                    String recPrefix = AttributeType.CENSUS_METHOD_COL.equals(attr.getType()) ? recCount + "_" : "";
                    for (TypedAttributeValue recVal : rec.getAttributes()) {
                        String recKey = WebFormAttributeParser.getParamKey(attPrefix, attr)+
                                String.format(AttributeParser.ATTRIBUTE_RECORD_NAME_FORMAT, rec.getId())+
                                WebFormAttributeParser.getParamKey(recPrefix, recVal.getAttribute());
                        assertAttributeValue(recVal, params.get(recKey));
                    }
                }
            }
        } else {
            assertAttributeValue(recAttr, params.get(key));
        }
    }
    
    /**
     * Asserts the attribute value based on the attributes type. The expectedValue
     * string will be cast appropriately. For best results use in conjunction with assignAttributeValue 
     * 
     * Intended for use with classes that need to alter and assert an attribute value
     * 
     * Warning: DOES NOT FAIL GRACEFULLY. if you pass in strings that don't parse to the desired type
     * exceptions will be thrown.
     * 
     * @param recAttr - AttributeValue
     * @param expectedValue - String representation of the expected value.
     */
    protected void assertAttributeValue(TypedAttributeValue recAttr, Object expectedValue) {
        Attribute a = recAttr.getAttribute();
        switch (a.getType()) {
        case INTEGER:
        case INTEGER_WITH_RANGE:
            Assert.assertEquals("integer av should be equal. type = " + a.getTypeCode(), 
                                Integer.parseInt((String)expectedValue), 
                                recAttr.getNumericValue().intValue());
            break;
        case DECIMAL:
            Assert.assertEquals("decimal av should be equal = " + a.getTypeCode(), 
                                Double.parseDouble((String)expectedValue), recAttr.getNumericValue().doubleValue(), 
                                DEFAULT_TOLERANCE);
            break;
        
        case DATE:
            Assert.assertEquals("date av should be equal = " + a.getTypeCode(), 
                                DateFormatter.parse((String)expectedValue, DateFormatter.DAY_MONTH_YEAR), 
                                recAttr.getDateValue());
            break;
            
        case REGEX:
        case BARCODE:
        case TIME:
        case STRING:
        case STRING_AUTOCOMPLETE:
        case TEXT:
        case HTML:
        case HTML_NO_VALIDATION:
        case HTML_COMMENT:
        case HTML_HORIZONTAL_RULE:
        case STRING_WITH_VALID_VALUES:
        case MULTI_CHECKBOX:
        case MULTI_SELECT:
        case IMAGE:
        case AUDIO:
        case FILE:
            Assert.assertEquals("string av should be equal = " + a.getTypeCode(), (String)expectedValue, recAttr.getStringValue());
            break;
        case CENSUS_METHOD_ROW:
        case CENSUS_METHOD_COL:
            // test the census method attribute values if the expected value is a map
            if (expectedValue instanceof Map) {
                Map<Attribute, Object> expectedValues = (Map<Attribute, Object>) expectedValue;
                Set<Record> records = recAttr.getRecords();
                for (Record record : records) {
                    for (AttributeValue recAV : record.getAttributes()) {
                        assertAttributeValue(recAV, expectedValues.get(recAV.getAttribute()));
                    }
                }
            }
            Assert.assertEquals("census method types should have null string value but type " + 
                              a.getTypeCode() + " had value '"+recAttr.getStringValue()+"'", null, recAttr.getStringValue());
            break;
        case SINGLE_CHECKBOX:
            Assert.assertEquals("bool av should be equal = " + a.getTypeCode(), Boolean.valueOf((String)expectedValue), recAttr.getBooleanValue());
            break;

        case SPECIES:
                // taxon type but the string value should be equal to the verbatim name.
                Assert.assertEquals("string av should be equal = " + a.getTypeCode(), (String)expectedValue, recAttr.getStringValue());
                break;

        default:
            // not handled. fail the test to notify the test writer
            Assert.fail("Attribute type : " + a.getTypeCode() + " is not handled. Fix it!");
            // we will never hit this return null but eclipse doesn't like the non return.
        }
    }
    
    protected List<Attribute> createAttrList(String namePrefix, boolean attrRequired, AttributeScope[] scopes) {
        return createAttrList(namePrefix, attrRequired, scopes, false);
    }
    
    protected List<Attribute> createAttrList(String namePrefix, boolean attrRequired, AttributeScope[] scopes, boolean isTag) {
        List<Attribute> attrs = new ArrayList<Attribute>();
        for (AttributeScope scope : scopes) {
            attrs.addAll(createAttrList(namePrefix, attrRequired, scope, true, isTag));
        }
        return attrs;
    }
    
    protected List<Attribute> createAttrList(String namePrefix, boolean attrRequired, AttributeScope scope) {
        return createAttrList(namePrefix, attrRequired, scope, true, false);
    }
    
    protected List<Attribute> createAttrList(String namePrefix, boolean attrRequired, AttributeScope scope, boolean hasCMAttrs, boolean isTag) {
        List<Attribute> attrList = new LinkedList<Attribute>();
        int attIndex = 0;
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.INTEGER, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.INTEGER_WITH_RANGE, attrRequired, scope,
                                     new String[] { String.valueOf(INTEGER_WITH_RANGE_LOWER_LIMIT), String.valueOf(INTEGER_WITH_RANGE_UPPER_LIMIT) } , isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.DECIMAL, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.BARCODE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.DATE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.TIME, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.STRING, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.STRING_AUTOCOMPLETE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.TEXT, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.STRING_WITH_VALID_VALUES, attrRequired, scope, new String[] { "hello", "world", "goodbye"} , isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.FILE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.IMAGE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.HTML, attrRequired, scope, isTag));
        // HTML comments do not have a name in the database, use an empty string.
        attrList.add(createAttribute("", AttributeType.HTML_COMMENT, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.HTML_HORIZONTAL_RULE, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.REGEX, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.HTML_NO_VALIDATION, attrRequired, scope, isTag));
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.AUDIO, attrRequired, scope, isTag));
        if (hasCMAttrs) {
            attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.CENSUS_METHOD_ROW, attrRequired, scope, isTag));
            attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.CENSUS_METHOD_COL, attrRequired, scope, isTag));
        }
        attrList.add(createAttribute(namePrefix + "_" + (attIndex++), AttributeType.SPECIES, attrRequired, scope, isTag));
        return attrList;
    }
    
    protected Attribute createAttribute(String name, AttributeType type, boolean required, AttributeScope scope, boolean isTag) {
        return createAttribute(name, type, required, scope, null, isTag);
    }
    
    protected Attribute createAttribute(String name, AttributeType type, boolean required, AttributeScope scope, String[] args, boolean isTag) {
        Attribute a = new Attribute();
        
        if (args != null) {
            List<AttributeOption> options = new ArrayList<AttributeOption>();
            for (String s : args) {
                AttributeOption attrOpt = new AttributeOption();
                attrOpt.setRunThreshold(false);
                attrOpt.setValue(s);
                options.add(attrOpt);
                
                taxaDAO.save(attrOpt);
            }
            a.setOptions(options);
        }
        
        a.setScope(scope);
        a.setName(name);
        a.setDescription(name + " desc");
        a.setRequired(required);
        a.setTag(isTag);
        a.setTypeCode(type.getCode());
        
        a.setRunThreshold(false);
        
        if (AttributeType.isCensusMethodType(type)) {
            a.setCensusMethod(attrCm);
        }
        
        taxaDAO.save(a);
        return a;
    }

    /**
     * Imports a previously exported survey.
     *
     * @param surveyExportFile the file to be imported.
     */
    protected void importSurvey(String username, String password, File surveyExportFile) throws Exception {

        // Since we will be committing data, we need to drop the database after we are finished.
        requestDropDatabase();

        FileInputStream fis = null;
        try {
            // Commit the existing session before starting the import.
            sessionFactory.getCurrentSession().getTransaction().commit();
            sessionFactory.getCurrentSession().beginTransaction();
            resetRequestContext();

            // Clean up and prepare for the request
            response = new MockHttpServletResponse();
            createNewRequest();

            // Build the request
            fis = new FileInputStream(surveyExportFile);
            MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
            req.setMethod("POST");
            req.setRequestURI(SurveyBaseController.SURVEY_IMPORT_URL);
            req.addFile(new MockMultipartFile(SurveyBaseController.POST_KEY_SURVEY_IMPORT_FILE, fis));

            // Issue the request
            login(username, password, new String[]{Role.ADMIN});
            handle(request, response);
            assertMessageCode("bdrs.survey.import.success");

            // Commit and clean up
            sessionFactory.getCurrentSession().getTransaction().commit();
            sessionFactory.getCurrentSession().beginTransaction();
            resetRequestContext();

            response = new MockHttpServletResponse();
            createNewRequest();

        } catch(IOException ioe) {
            throw ioe;
        } finally {
            if (fis != null ) {
                try {
                    fis.close();
                    fis = null;
                } catch(IOException ioex) {
                    throw ioex;
                }
            }
        }
    }

    private void createNewRequest() {
        request = createMockHttpServletRequest();
        resetRequestContext();
    }

    private void resetRequestContext() {
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
        getRequestContext().setPortal(defaultPortal);
    }
}
