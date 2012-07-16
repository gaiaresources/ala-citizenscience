package au.com.gaiaresources.bdrs.test;

import au.com.gaiaresources.bdrs.controller.BdrsMockHttpServletRequest;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public abstract class AbstractTransactionalTest extends AbstractSpringContextTest {

    private Logger log = Logger.getLogger(getClass());

    protected static final String REQUEST_SCHEME = "http";
    protected static final String REQUEST_SERVER_NAME = "www.mybdrs.com.au";
    protected static final int REQUEST_SERVER_PORT = 9096;
    protected static final int REQUEST_LOCAL_PORT = 9096;
    protected static final String REQUEST_CONTEXT_PATH = "/CONTEXTPATH";
    
    // unfortunately we need the request in this class (instead of AbstractControllerTest)
    // because our database read and writes rely on the RequestContext, which requires
    // a request object to instantiate properly.
    protected MockHttpServletRequest request;
    
    protected Session sesh;
    
    @Autowired
    protected SessionFactory sessionFactory;
    
    protected Portal defaultPortal;
    
    private boolean dropDatabase = false;

    // @BeforeTransaction runs before @Before
    @BeforeTransaction
    public final void beginTransaction() throws Exception {
    	// create the session that we will use for the whole test unless
    	// the test does its own session management in which case you need to be careful!
    	sesh = sessionFactory.getCurrentSession();
        request = createMockHttpServletRequest();
        RequestContext c = new RequestContext(request, applicationContext);
        RequestContextHolder.set(c);
        c.setHibernate(sesh);
        sesh.beginTransaction();
    }
    
    @Before
    public void primeDatabase() {
        dropDatabase = false;
        try {
            Portal portal = new PortalInitialiser().initRootPortal(sesh, null);
            defaultPortal = portal;
            FilterManager.setPortalFilter(sesh, portal);
            FilterManager.setPartialRecordCountFilter(sesh);
            RequestContext c = RequestContextHolder.getContext();
            c.setPortal(defaultPortal);
        } catch (Exception e) {
            log.error("db setup error", e);
        }
    }
    
    @AfterTransaction
    public final void rollbackTransaction() throws Exception {
        if (dropDatabase) {
        	// the session may have been closed...
        	rollbackSession(sesh);
        	
        	// check the current session in the session factory incase
        	// another session was opened without our knowledge.
        	// I have observed this happening in the BulkDataServiceTest.
        	rollbackSession(sessionFactory.getCurrentSession());

            Session dropDatabaseSesh = sessionFactory.openSession();
            SQLQuery q = dropDatabaseSesh.createSQLQuery("truncate table portal cascade;");
            Transaction tx = dropDatabaseSesh.beginTransaction();
            q.executeUpdate();
            tx.commit();
            dropDatabaseSesh.close();
        } else {
            // do normal rollback...
            sesh.getTransaction().rollback();
        }
    }
    
    private void rollbackSession(Session sesh) {
    	if (sesh.isOpen()) {
    		if(sesh.getTransaction().isActive()) {
                sesh.getTransaction().rollback();
            }
    	}
    	// sessions may be closed automatically when their
    	// transactions are rolled back but it is not guaranteed.
    	// thus...
    	if (sesh.isOpen()) {
    		sesh.close();
    	}
    }
    
    protected final void requestDropDatabase() {
        dropDatabase = true;
    }
    
    protected void commit() {
        sesh.getTransaction().commit();
        
        if (!sesh.isOpen()) {
        	// should open a new session
        	sesh = sessionFactory.getCurrentSession();
        }
        sesh.beginTransaction();
        RequestContextHolder.getContext().setHibernate(sesh);
    }

    /**
     * This function should be overriden by tests that require a multipart
     * request.
     * 
     * @return
     */
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return createStandardRequest();
    }
    
    protected MockHttpServletRequest createStandardRequest() {
        // Use BdrsMockHttpServletRequest so we can manipulate the request parameters.
        MockHttpServletRequest request = new BdrsMockHttpServletRequest();
        request.setScheme(REQUEST_SCHEME);
        request.setServerName(REQUEST_SERVER_NAME);
        request.setContextPath(REQUEST_CONTEXT_PATH);
        request.setServerPort(REQUEST_SERVER_PORT);
        request.setLocalPort(REQUEST_LOCAL_PORT);
        MockHttpSession session = new MockHttpSession(); 
        request.setSession(session);
        return request;
    }
    
    protected MockHttpServletRequest createUploadRequest() {
        MockHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setScheme(REQUEST_SCHEME);
        request.setServerName(REQUEST_SERVER_NAME);
        request.setContextPath(REQUEST_CONTEXT_PATH);
        request.setServerPort(REQUEST_SERVER_PORT);
        request.setLocalPort(REQUEST_LOCAL_PORT);
        
        MockHttpSession session = new MockHttpSession(); 
        request.setSession(session);
        return request;
    }
}
