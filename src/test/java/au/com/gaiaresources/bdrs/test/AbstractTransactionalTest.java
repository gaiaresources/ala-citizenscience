package au.com.gaiaresources.bdrs.test;

import au.com.gaiaresources.bdrs.MockFactory;
import au.com.gaiaresources.bdrs.controller.BdrsMockHttpServletRequest;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.service.taxonomy.FileTaxonLibSessionFactory;
import au.com.gaiaresources.bdrs.service.taxonomy.TaxonLibSessionFactory;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;


@Transactional
public abstract class AbstractTransactionalTest extends
        AbstractSpringContextTest {

    private static Logger log = Logger.getLogger(AbstractTransactionalTest.class);

    protected static final String REQUEST_SCHEME = "http";
    protected static final String REQUEST_SERVER_NAME = "www.mybdrs.com.au";
    protected static final int REQUEST_SERVER_PORT = 9096;
    protected static final int REQUEST_LOCAL_PORT = 9096;
    protected static final String REQUEST_CONTEXT_PATH = "/CONTEXTPATH";

    private static final String SQL_DELIMITER = "(;(\r)?\n)|(--\n)";

    // unfortunately we need the request in this class (instead of AbstractControllerTest)
    // because our database read and writes rely on the RequestContext, which requires
    // a request object to instantiate properly.
    protected MockHttpServletRequest request;
    protected Portal defaultPortal;

    @Autowired
    protected SessionFactory sessionFactory;

    private boolean dropDatabase = false;

    private TaxonLibSessionFactory tlSeshFactory;


    @BeforeTransaction
    public final void beginTransaction() throws Exception {
        tlSeshFactory = new FileTaxonLibSessionFactory();
        // create the session that we will use for the whole test unless
        // the test does its own session management in which case you need to be careful!
        Session sesh = sessionFactory.getCurrentSession();
        request = createMockHttpServletRequest();
        RequestContext c = new RequestContext(request, applicationContext);
        RequestContextHolder.set(c);
        c.setHibernate(sesh);
        c.setTaxonLibSessionFactory(tlSeshFactory);
        sesh.beginTransaction();
    }

    @Before
    public void primeDatabase() throws Exception {
        dropDatabase = false;
        Session sesh = getSession();
        PortalInitialiser portalInitialiser = MockFactory.newPortalInitialiser();
        Portal rootPortal = portalInitialiser.initRootPortal(sesh, null);
        defaultPortal = getDefaultPortal(sesh, rootPortal);
        FilterManager.setPortalFilter(sesh, defaultPortal);
        FilterManager.setPartialRecordCountFilter(sesh);
        RequestContextHolder.getContext().setPortal(defaultPortal);
    }

    /**
     * This method exists to provide a hook for subclasses to override if the
     * default portal is not the root portal.
     * <p/>
     * WARNING!! Even if this method is not used inside the core,
     * some sub-projects use it (e.g GR256-Ecological).
     *
     * @param sesh       the session to be used to retrieve the default portal if necessary.
     * @param rootPortal the root portal that has been automatically initialised.
     * @return the default portal to be set on the request context.
     */
    protected Portal getDefaultPortal(Session sesh, Portal rootPortal) {
        return rootPortal;
    }

    @AfterTransaction
    public void rollbackTransaction() throws Exception {
        Session sesh = getSession();
        if (RequestContextHolder.getContext().getTaxonLibSessionFactory() == null) {
            // In order to get around new requestContext being created
            RequestContextHolder.getContext().setTaxonLibSessionFactory(tlSeshFactory);
        }
        ITaxonLibSession taxonLibSession = RequestContextHolder
                .getContext().getTaxonLibSession();
        if (dropDatabase) {
            InputStream sqlStream = null;
            try {
                sqlStream = ITaxonLibSession.class
                        .getResourceAsStream("taxonlib.sql");
                importSQL(taxonLibSession.getConnection(), sqlStream);
            } finally {
                if (sqlStream != null) {
                    sqlStream.close();
                }
            }
            taxonLibSession.commit();
        } else {
            taxonLibSession.rollback();
        }
        taxonLibSession.getConnection().close();
        if (dropDatabase) {

            // the session may have been closed...
            closeSession(sesh);

            // check the current session in the session factory in case
            // another session was opened without our knowledge.
            // I have observed this happening in the BulkDataServiceTest.
            closeSession(sessionFactory.getCurrentSession());

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

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    private void rollbackSession(Session sesh) {
        if (sesh.isOpen()) {
            if (sesh.getTransaction().isActive()) {
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

    private void closeSession(Session sesh) {
        if (sesh.isOpen()) {
            sesh.close();
        }
    }

    protected final void requestDropDatabase() {
        dropDatabase = true;
    }

    protected void commit() {
        Session sesh = getSession();
        if (sesh.isOpen()) {
            sesh.getTransaction().commit();
        } else {
            log.warn("Session is already closed, cannot commit. The session was probably committed earlier which caused the session to close");
        }

        if (!sesh.isOpen()) {
            // should open a new session
            sesh = sessionFactory.getCurrentSession();
        }
        sesh.beginTransaction();
        RequestContextHolder.getContext().setHibernate(sesh);
    }

    /**
     * This function should be overridden by tests that require a multi-part
     * request.
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

    private static void importSQL(Connection conn, InputStream in)
            throws SQLException {
        Scanner s = new Scanner(in);
        s.useDelimiter(SQL_DELIMITER);
        Statement st = null;
        try {
            st = conn.createStatement();
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line
                            .substring(i + 1, line.length() - " */".length());
                }
                if (line.trim().length() > 0) {
                    st.execute(line);
                }
            }
        } finally {
            if (st != null)
                st.close();
        }
    }
}
