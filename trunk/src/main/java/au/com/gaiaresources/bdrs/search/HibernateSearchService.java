package au.com.gaiaresources.bdrs.search;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.model.index.IndexUtil;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;

/**
 * Service that implements a Hibernate Search Service.  Handles the creation and  
 * deletion of indexes and searching on them.
 * 
 * @author stephanie
 */
@SuppressWarnings(value={"unchecked","rawtypes"})
@Service
public class HibernateSearchService implements SearchService {

    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private PortalDAO portalDAO;
    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndexes(Session sesh) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.beginTransaction();
        for (Class indexedClass : IndexUtil.getIndexedClasses()) {
            log.info("creating index for "+indexedClass.getName());
            buildIndex(fullTextSession, indexedClass);
        }
    }

    /**
     * Builds an index for the specified class.
     * @param fullTextSession
     * @param clazz
     */
    private void buildIndex(FullTextSession fullTextSession,
            Class<?> clazz) {
        //Scrollable results will avoid loading too many objects in memory
        ScrollableResults results = fullTextSession.createCriteria(clazz)
            .setFetchSize(INDEX_BATCH_SIZE)
            .scroll(ScrollMode.FORWARD_ONLY);
        int index = 0;
        while(results.next()) {
            index++;
            fullTextSession.index(results.get(0)); //index each element
            if (index % INDEX_BATCH_SIZE == 0) {
                fullTextSession.flushToIndexes(); //apply changes to indexes
                fullTextSession.clear(); //free memory since the queue is processed
            }
        }
        
        fullTextSession.flushToIndexes();
        fullTextSession.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIndexes(Session sesh) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        for (Class indexedClass : IndexUtil.getIndexedClasses()) {
            log.info("deleting index for "+indexedClass.getName());
            fullTextSession.purgeAll(indexedClass);
        }
        fullTextSession.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends PersistentImpl> search(
            Class<? extends PersistentImpl> object, String searchTerm) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PagedQueryResult searchPaged(Session sesh, 
                                        String[] fields,
                                        Analyzer analyzer,
                                        String searchTerm, 
                                        PaginationFilter filter,
                                        Class<?>... entities) throws ParseException {
         FullTextQuery hibQuery = getQuery(sesh, fields, analyzer, searchTerm, entities);
         PagedQueryResult results = new QueryPaginator().page(hibQuery, filter);
         log.debug("search for searchterm "+searchTerm+" into query "+hibQuery.getQueryString()+" yielded "+results.getList().size()+" results!");
         return results;
     }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FullTextQuery getQuery(Session sesh, 
                                  String[] fields,
                                  Analyzer analyzer,
                                  String searchTerm, 
                                  Class<?>... entities) throws ParseException {
         FullTextSession fullTextSession = Search.getFullTextSession(sesh);
         fullTextSession.beginTransaction();
         MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
         
         org.apache.lucene.search.Query query = parser.parse(searchTerm);
         
         FullTextQuery hibQuery = fullTextSession.createFullTextQuery(query, entities);
         return hibQuery;
     }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndexes()
     */
    @Override
    public void deleteIndexes() {
        deleteIndexes(null);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndexes()
     */
    @Override
    public void createIndexes() {
        createIndexes(null);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndex(java.lang.Class)
     */
    @Override
    public void deleteIndex(Class<?> clazz) {
        deleteIndex(null, clazz);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndex(java.lang.Class)
     */
    @Override
    public void createIndex(Class<?> clazz) {
        createIndex(null, clazz);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndex(org.hibernate.Session, java.lang.Class)
     */
    @Override
    public void deleteIndex(Session sesh, Class<?> indexedClass) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        log.info("deleting index for "+indexedClass.getName());
        fullTextSession.purgeAll(indexedClass);
        //fullTextSession.flush();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndex(org.hibernate.Session, java.lang.Class)
     */
    @Override
    public void createIndex(Session sesh, Class<?> indexedClass) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.beginTransaction();
        log.info("creating index for "+indexedClass.getName());
        buildIndex(fullTextSession, indexedClass);
    }
}