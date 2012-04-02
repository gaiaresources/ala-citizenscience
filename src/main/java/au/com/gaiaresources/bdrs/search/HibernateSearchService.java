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
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;

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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndexes(Session sesh) {
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.beginTransaction();
        
        //Scrollable results will avoid loading too many objects in memory
        ScrollableResults results = fullTextSession.createCriteria(IndicatorSpecies.class)
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
        FullTextSession fullTextSession = Search.getFullTextSession(sesh);
        fullTextSession.purgeAll(IndicatorSpecies.class);
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
         FullTextSession fullTextSession = Search.getFullTextSession(sesh);
         org.hibernate.Transaction tx = fullTextSession.beginTransaction();
         MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
         
         org.apache.lucene.search.Query query = parser.parse(searchTerm);
         
         FullTextQuery hibQuery = fullTextSession.createFullTextQuery(query, entities);
         PagedQueryResult results = new QueryPaginator().page(hibQuery, filter);
         return results;
     }
}
