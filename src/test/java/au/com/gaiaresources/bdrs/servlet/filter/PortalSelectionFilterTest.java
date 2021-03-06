package au.com.gaiaresources.bdrs.servlet.filter;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.portal.PortalPrefixValidator;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalEntryPoint;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.hibernate.Filter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterChain;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PortalSelectionFilterTest extends AbstractControllerTest {

    private Logger log = Logger.getLogger(this.getClass());
    
    @Autowired
    private PortalDAO portalDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private PortalPrefixValidator validator;
    
    private MockFilterChain chain;
    private PortalSelectionFilter filter;
    
    @Before
    public void setUp() throws Exception {      
        chain = new MockFilterChain();
        filter = new PortalSelectionFilter();
        
        // Manual Dependency Injection (and some duck punching)
        Object value = null;
        Field[] fieldArray = PortalSelectionFilter.class.getDeclaredFields();
        for(Field field : fieldArray) {
            value = null;
            if("portalDAO".equals(field.getName())) {
                value = portalDAO;
            } else if ("sessionFactory".equals(field.getName())) {
                value = sessionFactory;
            } else if("portalMatcher".equals(field.getName())) {
                value = new PortalSelectionFilterMatcher(portalDAO, validator);
            }

            if(value != null) {
                field.setAccessible(true);
                field.set(filter, value);
            }
        }
    }

    /**
     * Tests the normal use case where the url matches a portal and no redirect
     * is specified.
     */
    @Test
    public void testPortalId() throws Exception {
        
        createTestPortals(true, "");
        
        Portal expectedPortal = portalDAO.getPortalByName(null, "myportal");
       
        request.setRequestURI(request.getContextPath()+"/myportal/");
        filter.doFilter(request, response, chain);
        
        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertEquals(REQUEST_CONTEXT_PATH + "/portal/"+ expectedPortal.getId() + PortalSelectionFilter.DEFAULT_REDIRECT_URL, response.getRedirectedUrl());
    }
    
    /**
     * Tests the normal use case where the url matches a portal and a redirect
     * is specified.
     */
    @Test
    public void testPortalIdWithRedirect() throws Exception {
        
        String customRedirect = "/custom_redirect/";
        createTestPortals(true, customRedirect);
        
        Portal expectedPortal = portalDAO.getPortalByName(null, "myportal");
       
        request.setRequestURI(request.getContextPath()+"/myportal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertEquals(customRedirect, response.getRedirectedUrl());
    }
    
    /**
     * Test that when a match occurs and a redirect is available, the request
     * will get redirected.
     */
    @Test
    public void testPortalRedirect() throws Exception {
        
        String redirectURL = "redirect_url";
        createTestPortals(true, "redirect_url");
        
        Portal expectedPortal = portalDAO.getPortalByName(null, "myportal");
       
        request.setRequestURI(request.getContextPath()+"/myportal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertEquals(redirectURL,response.getRedirectedUrl());
    }
    
    /**
     * Tests that when there are no default portals (and one is required), the
     * first portal is determined to be the default portal.
     */
    @Test
    public void testNoDefaultPortal() throws Exception {
        
        createTestPortals(false, "");
        
        Portal expectedPortal = portalDAO.getPortals(null).get(0);
       
        request.setRequestURI(request.getContextPath()+"/non_matching_portal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertFalse(response.isCommitted());
        Assert.assertNull(response.getRedirectedUrl());
    }
    
    /**
     * Tests that when there are no default portals (and one is required), the
     * first portal is determined to be the default portal.
     */
    @Test
    public void testNoDefaultPortalWithRedirect() throws Exception {
        
        createTestPortals(false, "");
        
        String customRedirect = "/customRedirect/";
        Portal expectedPortal = portalDAO.getPortals(null).get(0);
        PortalEntryPoint entryPoint = portalDAO.getPortalEntryPoints(expectedPortal).get(0);
        entryPoint.setRedirect(customRedirect);
        portalDAO.save(entryPoint);
       
        request.setRequestURI(request.getContextPath()+"/non_matching_portal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertFalse(response.isCommitted());
        Assert.assertNull(response.getRedirectedUrl());
    }
    
    /**
     * Tests that when there are no matched portals, the default portal is used.
     */
    @Test
    public void testNoMatchedPortal() throws Exception {
        
        createTestPortals(true, "");
        
        Portal expectedPortal = portalDAO.getPortalByName(null, "default");
       
        request.setRequestURI(request.getContextPath()+"/non_matching_portal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertFalse(response.isCommitted());
        Assert.assertNull(response.getRedirectedUrl());
    }
    
    /**
     * Tests that when there are multiple default portals, 
     * the first default portal is used.
     */
    @Test
    public void testMultipleDefaultPortals() throws Exception {
        
        createTestPortals(false, "");
        
        // They are all default.
        for(Portal p : portalDAO.getPortals(null)) {
            p.setDefault(true);
            portalDAO.save(p);
        }
       
        Portal expectedPortal = portalDAO.getPortals(null).get(0);
       
        request.setRequestURI(request.getContextPath()+"/non_matching_portal/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertFalse(response.isCommitted());
        Assert.assertNull(response.getRedirectedUrl());    }
    
    /**
     * Tests that when there are multiple matched portals, 
     * the first portal is used.
     */
    @Test
    public void testMultipleMatchedPortals() throws Exception {
        
        createTestPortals(true, "");
        
        // They are all default.
        for(Portal p : portalDAO.getPortals(null)) {
            PortalEntryPoint entryPoint = new PortalEntryPoint();
            entryPoint.setPattern(".*");
            entryPoint.setRedirect("");
            entryPoint.setPortal(p);
            entryPoint = portalDAO.save(entryPoint);
        }
       
        Portal expectedPortal = portalDAO.getPortals(null).get(0);
       
        request.setRequestURI(request.getContextPath()+"/does_not_matter/");
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
        Assert.assertEquals(REQUEST_CONTEXT_PATH + "/portal/"+ expectedPortal.getId()+ PortalSelectionFilter.DEFAULT_REDIRECT_URL, response.getRedirectedUrl());
    }
    
    @Test
    public void testQueryFilter() throws Exception {
        createTestPortals(true, "");
        
        Portal currentPortal = RequestContextHolder.getContext().getPortal();
        Portal otherPortal = portalDAO.getPortalByName(null, "myportal");
        
        // Testing metadata because it is very simple to construct and test
        
        // This metadata item should be able to be queried because it will be
        // attached to the current portal.
        Metadata defaultMd = new Metadata("key", "value");
        defaultMd = metadataDAO.save(defaultMd);
        
        Metadata actualMd = metadataDAO.get(defaultMd.getId());
        Assert.assertEquals(defaultMd.getKey(), actualMd.getKey());
        Assert.assertEquals(defaultMd.getValue(), actualMd.getValue());
        Assert.assertEquals(currentPortal, actualMd.getPortal());
        
        Metadata otherMd = new Metadata("key2", "value2");
        otherMd.setPortal(otherPortal);
        otherMd = metadataDAO.save(otherMd);

        Assert.assertNull(metadataDAO.get(otherMd.getId()));
        Assert.assertEquals(1, metadataDAO.getMetadata().size());
    }
    
    @Test
    public void testRESTfulPortalSelection() throws Exception {
        createTestPortals(true, "");
        Portal currentPortal = RequestContextHolder.getContext().getPortal();
        
        request.setMethod("GET");
        request.setRequestURI(String.format("/portal/%d/home.htm", currentPortal.getId()));
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(currentPortal.getId(), portal.getId());

    }
    
    @Test
    public void testDeactivatedPortalSelection() throws Exception {
        createTestPortals(true, "");
        Portal defaultPortal = portalDAO.getPortalByName(null, "default");
        
        Portal deactivated = portalDAO.getPortalByName(null, "other");
        deactivated.setActive(false);
        deactivated = portalDAO.save(deactivated);
        
        request.setMethod("GET");
        request.setRequestURI(String.format("/portal/%d/home.htm", deactivated.getId()));
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(defaultPortal.getId(), portal.getId());

    }
    
    @Test
    public void testDeactivatedLoggedInPortalSelection() throws Exception {
        createTestPortals(true, "");
        Portal currentPortal = RequestContextHolder.getContext().getPortal();
        currentPortal.setActive(false);
        currentPortal = portalDAO.save(currentPortal);
        
        request.setMethod("GET");
        request.setRequestURI(String.format("/portal/%d/map/mySightings.htm", currentPortal.getId()));
        filter.doFilter(request, response, chain);
        
        Portal defaultPortal = portalDAO.getDefaultPortal();
        if(defaultPortal == null || (defaultPortal != null && !defaultPortal.isActive())) {
            defaultPortal = portalDAO.getActivePortals().get(0);
        }
        
        // Reassigned over to the default portal
        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(defaultPortal.getId(), portal.getId());
    }
    
    @Test
    public void testDeactivatedDefaultPortalSelection() throws Exception {
        createTestPortals(true, "");
        Portal defaultPortal = portalDAO.getPortalByName(null, "default");
        defaultPortal.setActive(false);
        defaultPortal = portalDAO.save(defaultPortal);
        
        request.setMethod("GET");
        request.setRequestURI(String.format("/portal/%d/home.htm", defaultPortal.getId()));
        filter.doFilter(request, response, chain);
        
        Portal expectedPortal = portalDAO.getDefaultPortal();
        if(expectedPortal == null || (expectedPortal != null && !expectedPortal.isActive())) {
            expectedPortal = portalDAO.getActivePortals().get(0);
        }
        
        // Reassigned over to the default portal
        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(expectedPortal.getId(), portal.getId());
    }
    
    @Test
    public void testRESTfulPortalSelectionWithAuthentication() throws Exception {
        createTestPortals(true, "");
        Portal currentPortal = RequestContextHolder.getContext().getPortal();
        
        request.setMethod("GET");
        request.setRequestURI(String.format("/portal/%d/map/mySightings.htm", currentPortal.getId()));
        filter.doFilter(request, response, chain);

        Portal portal = (Portal)request.getSession().getAttribute(PortalSelectionFilter.PORTAL_KEY);
        Assert.assertEquals(currentPortal.getId(), portal.getId());
    }
    
    private void createTestPortals(boolean includeDefault, String redirectUrl) throws Exception {
        
        // Make all existing portals non-default
        for(Portal p : portalDAO.getPortals(null)) {
            p.setDefault(false);
            portalDAO.save(p);
        }
        
        boolean isDefault = includeDefault;
        String contextPath = request.getServletContext().getContextPath();
        List<PortalEntryPoint> entryPointList;
        for (String name : new String[] { "default", "decoy", "other", "myportal"}) {
            
            Portal decoyPortal = new Portal();
            decoyPortal.setDefault(isDefault);
            isDefault = false;
            decoyPortal.setName(name);
            decoyPortal = portalDAO.save(decoyPortal);
            
            if(decoyPortal.isDefault()) {
                RequestContextHolder.getContext().setPortal(decoyPortal);
                request.setAttribute(PortalSelectionFilter.PORTAL_KEY, decoyPortal);
                Filter filter = sessionFactory.getCurrentSession().getEnabledFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
                filter.setParameter("portalId", decoyPortal.getId());
            }

            entryPointList = new ArrayList<PortalEntryPoint>();
            for(String pattern : new String[] { "^(\\S+)?\\/(%1$s\\/)?(%2$s){1}\\/$", 
                                                "^(%2$s\\.example\\.com)\\/(%1$s){1}\\/.*$" }) {
                PortalEntryPoint entryPoint = new PortalEntryPoint();
                entryPoint.setPattern(String.format(pattern, contextPath, name));
                entryPoint.setRedirect(redirectUrl);
                entryPoint.setPortal(decoyPortal);
                entryPoint = portalDAO.save(entryPoint);
                entryPointList.add(entryPoint);
            }
        }
    }
}

