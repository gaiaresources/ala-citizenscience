package au.com.gaiaresources.bdrs.search;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;

/**
 * Interface for implementing an indexed search engine such as Apache Solr or Hibernate Search.
 * 
 * @author stephanie
 */
@SuppressWarnings("unchecked")
@Service
public interface SearchService {

    // arbitrarily chosen batch size for indexing
    public static final int INDEX_BATCH_SIZE = 100;
    
    /**
     * Creates the indexes for the search engine.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     * @param sesh the {@link Session} to use for the query
     */
    public void createIndexes(Session sesh);
    
    /**
     * Deletes the indexes for the search engine.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     * @param sesh the {@link Session} to use for the query
     */
    public void deleteIndexes(Session sesh);
    
    /**
     * Does a search for searchTerm in the indexes for Class object.
     * @param object The object index to search
     * @param searchTerm The term to search for
     * @return A list of Objects matching the search criteria
     */
    public List search(Class<? extends PersistentImpl> object, String searchTerm);

    /**
     * Returns a {@link PagedQueryResult} with the results of the query
     * @param sesh the {@link Session} to use for the query
     * @param fields the indexed fields to query on
     * @param analyzer the analyzer to use for tokenizing the query string
     * @param searchTerm the text to search for
     * @param filter the {@link PaginationFilter} which determines how pages are created
     * @param entities the classes to search the indexes of
     * @return
     * @throws ParseException
     */
    public PagedQueryResult searchPaged(Session sesh,
            String[] fields, Analyzer analyzer, String searchTerm, 
            PaginationFilter filter, Class<?>... entities) throws ParseException;

    /**
     * Returns a {@link Query} object for making an indexed search.
     * @param sesh the {@link Session} to use for the query
     * @param fields the indexed fields to query on
     * @param analyzer the analyzer to use for tokenizing the query string
     * @param searchTerm the text to search for
     * @param entities the classes to search the indexes of
     * @return a {@link Query} object for making an indexed search.
     * @throws ParseException
     */
    public Query getQuery(Session sesh, String[] fields, Analyzer analyzer,
            String searchTerm, Class<?>... entities)
            throws ParseException;

    /**
     * Deletes all defined indexes from the system.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     */
    public void deleteIndexes();

    /**
     * Creates indexes for all indexed classes returned by {@link IndexUtils::getIndexedClasses()}.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     */
    public void createIndexes();

    /**
     * Deletes the index for the specified class.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     * @param sesh the {@link Session} to use for the indexing
     * @param clazz the {@link Class} to index
     */
    public void deleteIndex(Session sesh, Class<?> clazz);

    /**
     * Creates the index for the specified class.
     * @param sesh the {@link Session} to use for the indexing
     * @param clazz the {@link Class} to index
     */
    public void createIndex(Session sesh, Class<?> clazz);
    
    /**
     * Deletes the index for the specified class.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     * @param clazz the {@link Class} to index
     */
    public void deleteIndex(Class<?> clazz);

    /**
     * Creates the index for the specified class.
     * WARNING: this method is non-transactional and has the side effect of clearing the current Hibernate Session.
     * Designed for administrative use only.
     * @param clazz the {@link Class} to index
     */
    public void createIndex(Class<?> clazz);
}
