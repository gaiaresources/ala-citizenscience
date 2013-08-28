package au.com.gaiaresources.bdrs.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.ActionQueue;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.event.EventListeners;
import org.hibernate.event.EventSource;
import org.hibernate.impl.AbstractSessionImpl;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

public class Session extends AbstractSessionImpl implements org.hibernate.classic.Session, EventSource {
    private static final long serialVersionUID = 1858108706839266005L;
    
    private transient Logger log = Logger.getLogger(getClass());
    private org.hibernate.classic.Session session;
    
    private EventListeners eventListeners;
    
    public Session(org.hibernate.classic.Session session, SessionFactoryImpl factory) {
        super(factory);
        eventListeners = factory.getEventListeners();
        this.session = session;
    }

    @Override
    public Query createSQLQuery(String sql, String returnAlias,
            Class returnClass) {
        return this.session.createSQLQuery(sql, returnAlias, returnClass);
    }

    @Override
    public Query createSQLQuery(String sql, String[] returnAliases,
            Class[] returnClasses) {
        return this.session.createSQLQuery(sql, returnAliases, returnClasses);
    }

    @Override
    public int delete(String query, Object value, Type type)
            throws HibernateException {
        return this.session.delete(query, value, type);
    }

    @Override
    public int delete(String query, Object[] values, Type[] types)
            throws HibernateException {
        return this.session.delete(query, values, types);
    }

    @Override
    public int delete(String query) throws HibernateException {
        return this.session.delete(query);
    }

    @Override
    public Collection filter(Object collection, String filter, Object value,
            Type type) throws HibernateException {
        return this.session.filter(collection, filter, value, type);
    }

    @Override
    public Collection filter(Object collection, String filter, Object[] values,
            Type[] types) throws HibernateException {
        return this.session.filter(collection, filter, values, types);
    }

    @Override
    public Collection filter(Object collection, String filter)
            throws HibernateException {
        return this.session.filter(collection, filter);
    }

    @Override
    public List find(String query, Object value, Type type)
            throws HibernateException {
        return this.session.find(query, value, type);
    }

    @Override
    public List find(String query, Object[] values, Type[] types)
            throws HibernateException {
        return this.session.find(query, values, types);
    }

    @Override
    public List find(String query) throws HibernateException {
        return this.session.find(query);
    }

    @Override
    public Iterator iterate(String query, Object value, Type type)
            throws HibernateException {
        return this.session.iterate(query, value, type);
    }

    @Override
    public Iterator iterate(String query, Object[] values, Type[] types)
            throws HibernateException {
        return this.session.iterate(query, values, types);
    }

    @Override
    public Iterator iterate(String query) throws HibernateException {
        return this.session.iterate(query);
    }

    @Override
    public void save(Object object, Serializable id) throws HibernateException {
        this.session.save(object, id);
    }

    @Override
    public void save(String entityName, Object object, Serializable id)
            throws HibernateException {
        this.session.save(entityName, object, id);
    }

    @Override
    public Object saveOrUpdateCopy(Object object, Serializable id)
            throws HibernateException {
        return this.session.saveOrUpdateCopy(object, id);
    }

    @Override
    public Object saveOrUpdateCopy(Object object) throws HibernateException {
        return this.session.saveOrUpdateCopy(object);
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object,
            Serializable id) throws HibernateException {
        return this.session.saveOrUpdateCopy(entityName, object, id);
    }

    @Override
    public Object saveOrUpdateCopy(String entityName, Object object)
            throws HibernateException {
        return this.session.saveOrUpdateCopy(entityName, object);
    }

    @Override
    public void update(Object object, Serializable id)
            throws HibernateException {
        this.session.update(object, id);
    }

    @Override
    public void update(String entityName, Object object, Serializable id)
            throws HibernateException {
        this.session.update(entityName, object, id);
    }

    @Override
    public Transaction beginTransaction() throws HibernateException {

        Transaction tx = this.session.beginTransaction();
        
        RequestContext context = RequestContextHolder.getContext();
        Portal portal = context.getPortal();

        if (portal != null) {
            FilterManager.setPortalFilter(this.session, portal);
            FilterManager.setPartialRecordCountFilter(this.session);
        } else {
        	//log.warn("Portal is not set. Not enabling hibernate portal filter.");
        }

        return tx;
    }

    @Override
    public void cancelQuery() throws HibernateException {
        this.session.cancelQuery();
    }

    @Override
    public void clear() {
        this.session.clear();
    }

    @Override
    public Connection close() throws HibernateException {
        return this.session.close();
    }

    @Override
    public Connection connection() throws HibernateException {
        return this.session.connection();
    }

    @Override
    public boolean contains(Object object) {
        return this.session.contains(object);
    }

    @Override
    public Criteria createCriteria(Class persistentClass, String alias) {
        return this.session.createCriteria(persistentClass, alias);
    }

    @Override
    public Criteria createCriteria(Class persistentClass) {
        return this.session.createCriteria(persistentClass);
    }

    @Override
    public Criteria createCriteria(String entityName, String alias) {
        return this.session.createCriteria(entityName, alias);
    }

    @Override
    public Criteria createCriteria(String entityName) {
        return this.session.createCriteria(entityName);
    }

    @Override
    public Query createFilter(Object collection, String queryString)
            throws HibernateException {
        return this.session.createFilter(collection, queryString);
    }

    @Override
    public Query createQuery(String queryString) throws HibernateException {
        return this.session.createQuery(queryString);
    }

    @Override
    public SQLQuery createSQLQuery(String queryString)
            throws HibernateException {
        return this.session.createSQLQuery(queryString);
    }

    @Override
    public void delete(Object object) throws HibernateException {
        this.session.delete(object);
    }

    @Override
    public void delete(String entityName, Object object)
            throws HibernateException {
        this.session.delete(entityName, object);
    }

    @Override
    public void disableFilter(String filterName) {
        this.session.disableFilter(filterName);
    }

    @Override
    public Connection disconnect() throws HibernateException {
        return this.session.disconnect();
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        this.session.doWork(work);
    }

    @Override
    public Filter enableFilter(String filterName) {
        return this.session.enableFilter(filterName);
    }

    @Override
    public void evict(Object object) throws HibernateException {
        this.session.evict(object);
    }

    @Override
    public void flush() throws HibernateException {
        this.session.flush();
    }

    @Override
    public Object get(Class clazz, Serializable id, LockMode lockMode)
            throws HibernateException {
        return this.session.get(clazz, id, lockMode);
    }

    @Override
    public Object get(Class clazz, Serializable id) throws HibernateException {
        return this.session.get(clazz, id);
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode)
            throws HibernateException {
        return this.session.get(entityName, id, lockMode);
    }

    @Override
    public Object get(String entityName, Serializable id)
            throws HibernateException {
        return this.session.get(entityName, id);
    }

    @Override
    public CacheMode getCacheMode() {
        return this.session.getCacheMode();
    }

    @Override
    public LockMode getCurrentLockMode(Object object) throws HibernateException {
        return this.session.getCurrentLockMode(object);
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return this.session.getEnabledFilter(filterName);
    }

    @Override
    public EntityMode getEntityMode() {
        return this.session.getEntityMode();
    }

    @Override
    public String getEntityName(Object object) throws HibernateException {
        return this.session.getEntityName(object);
    }

    @Override
    public FlushMode getFlushMode() {
        return this.session.getFlushMode();
    }

    @Override
    public Serializable getIdentifier(Object object) throws HibernateException {
        return this.session.getIdentifier(object);
    }

    @Override
    public Query getNamedQuery(String queryName) throws HibernateException {
        return this.session.getNamedQuery(queryName);
    }

    @Override
    public org.hibernate.Session getSession(EntityMode entityMode) {
        return this.session.getSession(entityMode);
    }

    @Override
    public SessionFactory getSessionFactory() {
        return this.session.getSessionFactory();
    }

    @Override
    public SessionStatistics getStatistics() {
        return this.session.getStatistics();
    }

    @Override
    public Transaction getTransaction() {
        return this.session.getTransaction();
    }

    @Override
    public boolean isConnected() {
        return this.session.isConnected();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        return this.session.isDirty();
    }

    @Override
    public boolean isOpen() {
        return this.session.isOpen();
    }

    @Override
    public Object load(Class theClass, Serializable id, LockMode lockMode)
            throws HibernateException {
        return this.session.load(theClass, id, lockMode);
    }

    @Override
    public Object load(Class theClass, Serializable id)
            throws HibernateException {
        return this.session.load(theClass, id);
    }

    @Override
    public void load(Object object, Serializable id) throws HibernateException {
        this.session.load(object, id);
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode)
            throws HibernateException {
        return this.session.load(entityName, id, lockMode);
    }

    @Override
    public Object load(String entityName, Serializable id)
            throws HibernateException {
        return this.session.load(entityName, id);
    }

    @Override
    public void lock(Object object, LockMode lockMode)
            throws HibernateException {
        this.session.lock(object, lockMode);
    }

    @Override
    public void lock(String entityName, Object object, LockMode lockMode)
            throws HibernateException {
        this.session.lock(entityName, object, lockMode);
    }

    @Override
    public Object merge(Object object) throws HibernateException {
        return this.session.merge(object);
    }

    @Override
    public Object merge(String entityName, Object object)
            throws HibernateException {
        return this.session.merge(entityName, object);
    }

    @Override
    public void persist(Object object) throws HibernateException {
        this.session.persist(object);
    }

    @Override
    public void persist(String entityName, Object object)
            throws HibernateException {
        this.session.persist(entityName, object);
    }

    @Override
    public void reconnect() throws HibernateException {
        this.session.reconnect();
    }

    @Override
    public void reconnect(Connection connection) throws HibernateException {
        this.session.reconnect(connection);
    }

    @Override
    public void refresh(Object object, LockMode lockMode)
            throws HibernateException {
        this.session.refresh(object, lockMode);
    }

    @Override
    public void refresh(Object object) throws HibernateException {
        this.session.refresh(object);
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode)
            throws HibernateException {
        this.session.replicate(object, replicationMode);
    }

    @Override
    public void replicate(String entityName, Object object,
            ReplicationMode replicationMode) throws HibernateException {
        this.session.replicate(entityName, object, replicationMode);
    }

    @Override
    public Serializable save(Object object) throws HibernateException {
        return this.session.save(object);
    }

    @Override
    public Serializable save(String entityName, Object object)
            throws HibernateException {
        return this.session.save(entityName, object);
    }

    @Override
    public void saveOrUpdate(Object object) throws HibernateException {
        this.session.saveOrUpdate(object);
    }

    @Override
    public void saveOrUpdate(String entityName, Object object)
            throws HibernateException {
        this.session.saveOrUpdate(entityName, object);
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        this.session.setCacheMode(cacheMode);
    }

    @Override
    public void setFlushMode(FlushMode flushMode) {
        this.session.setFlushMode(flushMode);
    }

    @Override
    public void setReadOnly(Object entity, boolean readOnly) {
        this.session.setReadOnly(entity, readOnly);
    }

    @Override
    public void update(Object object) throws HibernateException {
        this.session.update(object);
    }

    @Override
    public void update(String entityName, Object object)
            throws HibernateException {
        this.session.update(entityName, object);
    }

    @Override
    public void delete(String entityName, Object child,
            boolean isCascadeDeleteEnabled, Set transientEntities) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).delete(entityName, child, isCascadeDeleteEnabled, transientEntities);
        }
    }

    @Override
    public void forceFlush(EntityEntry e) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).forceFlush(e);
        }
    }

    @Override
    public ActionQueue getActionQueue() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getActionQueue();
        }
        else {
            return null;
        }
    }

    @Override
    public Object instantiate(EntityPersister persister, Serializable id)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).instantiate(persister, id);
        }
        else {
            return null;
        }
    }

    @Override
    public void merge(String entityName, Object object, Map copiedAlready)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).merge(entityName, object, copiedAlready);
        }
    }

    @Override
    public void persist(String entityName, Object object, Map createdAlready)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).persist(entityName, object, createdAlready);
        }
    }

    @Override
    public void persistOnFlush(String entityName, Object object,
            Map copiedAlready) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).persistOnFlush(entityName, object, copiedAlready);
        }
    }

    @Override
    public void refresh(Object object, Map refreshedAlready)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).refresh(object, refreshedAlready);
        }
    }

    @Override
    public void saveOrUpdateCopy(String entityName, Object object,
            Map copiedAlready) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).saveOrUpdateCopy(entityName, object, copiedAlready);
        }
    }

    @Override
    public void afterScrollOperation() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).afterScrollOperation();
        }
    }

    @Override
    public void afterTransactionCompletion(boolean successful, Transaction tx) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).afterTransactionCompletion(successful, tx);
        }
    }

    @Override
    public void beforeTransactionCompletion(Transaction tx) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).beforeTransactionCompletion(tx);
        }
    }

    @Override
    public String bestGuessEntityName(Object object) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).bestGuessEntityName(object);
        }
        else {
            return null;
        }
    }

    @Override
    public int executeNativeUpdate(NativeSQLQuerySpecification specification,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).executeNativeUpdate(specification, queryParameters);
        }
        else {
            return 0;
        }
    }

    @Override
    public int executeUpdate(String query, QueryParameters queryParameters)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).executeUpdate(query, queryParameters);
        }
        else {
            return 0;
        }
    }

    @Override
    public Batcher getBatcher() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getBatcher();
        }
        else {
            return null;
        }
    }

    @Override
    public Serializable getContextEntityIdentifier(Object object) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getContextEntityIdentifier(object);
        }
        else {
            return null;
        }
    }

    @Override
    public int getDontFlushFromFind() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getDontFlushFromFind();
        }
        else {
            return 0;
        }
    }

    @Override
    public Map getEnabledFilters() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getEnabledFilters();
        }
        else {
            return null;
        }
    }

    @Override
    public EntityPersister getEntityPersister(String entityName, Object object)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getEntityPersister(entityName, object);
        }
        else {
            return null;
        }
    }

    @Override
    public Object getEntityUsingInterceptor(EntityKey key)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getEntityUsingInterceptor(key);
        }
        else {
            return null;
        }
    }

    @Override
    public SessionFactoryImplementor getFactory() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getFactory();
        }
        else {
            return null;
        }
    }

    @Override
    public String getFetchProfile() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getFetchProfile();
        }
        else {
            return null;
        }
    }

    @Override
    public Type getFilterParameterType(String filterParameterName) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getFilterParameterType(filterParameterName);
        }
        else {
            return null;
        }
    }

    @Override
    public Object getFilterParameterValue(String filterParameterName) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getFilterParameterValue(filterParameterName);
        }
        else {
            return null;
        }
    }

    @Override
    public Interceptor getInterceptor() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getInterceptor();
        }
        else {
            return null;
        }
    }

    @Override
    public JDBCContext getJDBCContext() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getJDBCContext();
        }
        else {
            return null;
        }
    }

    @Override
    public EventListeners getListeners() {
        return this.eventListeners;
    }

    @Override
    public Query getNamedSQLQuery(String name) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getNamedQuery(name);
        }
        else {
            return null;
        }
    }

    @Override
    public PersistenceContext getPersistenceContext() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getPersistenceContext();
        }
        else {
            return null;
        }
    }

    @Override
    public long getTimestamp() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).getTimestamp();
        }
        else {
            return System.currentTimeMillis();
        }
    }

    @Override
    public String guessEntityName(Object entity) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).guessEntityName(entity);
        }
        else {
            return null;
        }
    }

    @Override
    public Object immediateLoad(String entityName, Serializable id)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).immediateLoad(entityName, id);
        }
        else {
            return null;
        }
    }

    @Override
    public void initializeCollection(PersistentCollection collection,
            boolean writing) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).initializeCollection(collection, writing);
        }
    }

    @Override
    public Object instantiate(String entityName, Serializable id)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).instantiate(entityName, id);
        }
        else {
            return null;
        }
    }

    @Override
    public Object internalLoad(String entityName, Serializable id,
            boolean eager, boolean nullable) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).internalLoad(entityName, id, eager, nullable);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean isClosed() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).isClosed();
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isEventSource() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).isEventSource();
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isTransactionInProgress() {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).isTransactionInProgress();
        }
        else {
            return false;
        }
    }

    @Override
    public Iterator iterate(String query, QueryParameters queryParameters)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).iterate(query, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public Iterator iterateFilter(Object collection, String filter,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).iterateFilter(collection, filter, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public List list(CriteriaImpl criteria) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).list(criteria);
        }
        else {
            return null;
        }
    }

    @Override
    public List list(String query, QueryParameters queryParameters)
            throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).list(query, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public List list(NativeSQLQuerySpecification spec,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).list(spec, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public List listCustomQuery(CustomQuery customQuery,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).listCustomQuery(customQuery, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public List listFilter(Object collection, String filter,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).listFilter(collection, filter, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public ScrollableResults scroll(String query,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).scroll(query, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).scroll(criteria, scrollMode);
        }
        else {
            return null;
        }
    }

    @Override
    public ScrollableResults scroll(NativeSQLQuerySpecification spec,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).scroll(spec, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public ScrollableResults scrollCustomQuery(CustomQuery customQuery,
            QueryParameters queryParameters) throws HibernateException {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            return ((SessionImpl)this.session).scrollCustomQuery(customQuery, queryParameters);
        }
        else {
            return null;
        }
    }

    @Override
    public void setAutoClear(boolean enabled) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).setAutoClear(enabled);
        }
    }

    @Override
    public void setFetchProfile(String name) {
        if (SessionImpl.class.isAssignableFrom(this.session.getClass())) {
            ((SessionImpl)this.session).setFetchProfile(name);
        }
    }

}
