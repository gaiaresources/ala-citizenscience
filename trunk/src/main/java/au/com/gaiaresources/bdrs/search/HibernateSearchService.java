package au.com.gaiaresources.bdrs.search;

import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.model.index.IndexUtil;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.search.criteriaBuilder.IndicatorSpeciesHibernateSearchCriteriaBuilder;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.Criteria;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that implements a Hibernate Search Service.  Handles the creation and  
 * deletion of indexes and searching on them.
 * 
 * @author stephanie
 */
@SuppressWarnings(value={"unchecked","rawtypes"})
@Service
public class HibernateSearchService implements SearchService {

    private static Map<Class<?>, HibernateSearchCriteriaBuilder> INDEX_CANDIDATE_MAP;
    static {
        HibernateSearchCriteriaBuilder[] builders = {
            new IndicatorSpeciesHibernateSearchCriteriaBuilder()
        };

        Map<Class<?>, HibernateSearchCriteriaBuilder> temp = new HashMap<Class<?>, HibernateSearchCriteriaBuilder>(2);
        for(HibernateSearchCriteriaBuilder builder : builders) {
            temp.put(builder.getIndexedClass(), builder);
        }

        INDEX_CANDIDATE_MAP = Collections.unmodifiableMap(temp);
    }

    /** Identifies the portal id field in indexes entities */
    private static final String PORTAL_ID_FIELD_NAME = "portal.id";
    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private PortalDAO portalDAO;
    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndexes(Session sesh, Portal portal) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.beginTransaction();
        for (Class indexedClass : IndexUtil.getIndexedClasses()) {
            buildIndex(fullTextSession, indexedClass, portal);
        }
    }

    /**
     * Builds an index for the specified class.
     * @param fullTextSession the session to use when indexing
     * @param clazz the Class of the entity to index.
     */
    private void buildIndex(FullTextSession fullTextSession,
            Class<?> clazz, Portal portal) {
        //Scrollable results will avoid loading too many objects in memory
        HibernateSearchCriteriaBuilder builder = INDEX_CANDIDATE_MAP.get(clazz);
        Criteria criteria;
        if(builder == null) {
            criteria = fullTextSession.createCriteria(clazz);
        } else {
            criteria = builder.createCriteria(fullTextSession);
        }

        criteria.add(Restrictions.eq("portal", portal));
        ScrollableResults results = criteria.setFetchSize(INDEX_BATCH_SIZE).scroll(ScrollMode.FORWARD_ONLY);
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
    public void deleteIndexes(Session sesh, Portal portal) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        for (Class indexedClass : IndexUtil.getIndexedClasses()) {
            deleteIndex(indexedClass, portal);
        }
        fullTextSession.flushToIndexes();
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

         searchTerm = applyPortalIdTerm(sesh, searchTerm);

         org.apache.lucene.search.Query query = parser.parse(searchTerm);

         return fullTextSession.createFullTextQuery(query, entities);
     }

    /**
     * Modifies the supplied query by appending the portal id as a required term.
     * @param sesh the current hibernate session - used to retrieve the current portal id.
     * @param searchTerm the search term to modify.
     * @return a new search query that will only match results from the current portal.
     */
    private String applyPortalIdTerm(Session sesh, String searchTerm) {
        Integer portalId = FilterManager.getFilteredPortalId(sesh);
        if (portalId != null) {
            searchTerm += " +" + PORTAL_ID_FIELD_NAME + ":" +portalId;
        }
        return searchTerm;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndexes()
     */
    @Override
    public void deleteIndexes(Portal portal) {
        deleteIndexes(null, portal);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndexes()
     */
    @Override
    public void createIndexes(Portal portal) {
        createIndexes(null, portal);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndex(java.lang.Class)
     */
    @Override
    public void deleteIndex(Class<?> clazz, Portal portal) {
        deleteIndex(null, clazz, portal);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndex(java.lang.Class)
     */
    @Override
    public void createIndex(Class<?> clazz, Portal portal) {
        createIndex(null, clazz, portal);
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#deleteIndex(org.hibernate.Session, java.lang.Class)
     */
    @Override
    public void deleteIndex(Session sesh, Class<?> indexedClass, Portal portal) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        ScrollableResults results = null;
        FullTextQuery q;
        try {
            q = getQuery(sesh, new String[]{"portal.id"}, new StandardAnalyzer(), 
                         "+portal.id:" + portal.getId(), indexedClass);
            results = q.scroll(ScrollMode.FORWARD_ONLY);
            int index = 0;
            while(results.next()) {
                index++;
                try {
                    Object[] row = results.get();
                    Object element = row[0];
                    if (element instanceof PersistentImpl) {
                        PersistentImpl entity = (PersistentImpl)element;
                        fullTextSession.purge(indexedClass, entity.getId());
                        if (index % INDEX_BATCH_SIZE == 0) {
                            fullTextSession.flushToIndexes(); //apply changes to indexes
                            fullTextSession.clear(); //free memory since the queue is processed
                        }
                    }
                } catch (NullPointerException e) {
                    log.error("Couldn't delete indexed entity for item at " + index, e);
                }
            }
        } catch (ParseException e) {
            log.error("Couldn't retrieve values to delete from index.", e);
        }
        
        fullTextSession.flushToIndexes();
        fullTextSession.clear();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.search.SearchService#createIndex(org.hibernate.Session, java.lang.Class)
     */
    @Override
    public void createIndex(Session sesh, Class<?> indexedClass, Portal portal) {
        if (sesh == null) {
            sesh = portalDAO.getSessionFactory().getCurrentSession();
        }
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.beginTransaction();
        buildIndex(fullTextSession, indexedClass, portal);
    }

}