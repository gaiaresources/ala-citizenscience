package au.com.gaiaresources.bdrs.search;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
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
     */
    public void createIndexes(Session sesh);
    
    /**
     * Deletes the indexes for the search engine.
     */
    public void deleteIndexes(Session sesh);
    
    /**
     * Does a search for searchTerm in the indexes for Class object.
     * @param object The object index to search
     * @param searchTerm The term to search for
     * @return A list of Objects matching the search criteria
     * 
     */
    public List search(Class<? extends PersistentImpl> object, String searchTerm);

    /**
     * 
     * @param sesh
     * @param fields
     * @param analyzer
     * @param groupId
     * @param searchTerm
     * @param filter
     * @param entities
     * @return
     * @throws ParseException
     */
    public PagedQueryResult searchPaged(Session sesh,
            String[] fields, Analyzer analyzer, String searchTerm, 
            PaginationFilter filter, Class<?>... entities) throws ParseException;
}
