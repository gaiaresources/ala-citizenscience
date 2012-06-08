package au.com.gaiaresources.bdrs.search;

import org.hibernate.Criteria;
import org.hibernate.search.FullTextSession;

/**
 * This class provides a hook allowing for customized criteria to be created for specific
 * classes to be indexed. The customized criteria should be optimised to minimise database
 * query by eager fetching where necessary.
 */
public interface HibernateSearchCriteriaBuilder {

    /**
     * Creates a new search criteria using the specified session.
     * @param fullTextSession the session that will be used to execute the query.
     * @return the critera to be executed to retrieve instances to be indexed.
     */
    public Criteria createCriteria(FullTextSession fullTextSession);

    /**
     * @return the class that the criteria created by this builder will return.
     */
    public Class<?> getIndexedClass();
}
