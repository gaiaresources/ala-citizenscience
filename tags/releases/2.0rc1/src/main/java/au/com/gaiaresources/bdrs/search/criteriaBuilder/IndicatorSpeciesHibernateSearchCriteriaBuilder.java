package au.com.gaiaresources.bdrs.search.criteriaBuilder;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.search.HibernateSearchCriteriaBuilder;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.search.FullTextSession;

/**
 * Creates a targeted criteria instance for <code>IndicatorSpecies</code> eager fetching the
 * <code>SpeciesProfile</code>.
 */
public class IndicatorSpeciesHibernateSearchCriteriaBuilder implements HibernateSearchCriteriaBuilder {

    public static final Class<?> INDEXED_CLASS = IndicatorSpecies.class;

    @Override
    public Criteria createCriteria(FullTextSession fullTextSession) {
        return fullTextSession.createCriteria(INDEXED_CLASS).setFetchMode("infoItems", FetchMode.JOIN);
    }

    @Override
    public Class<?> getIndexedClass() {
        return INDEXED_CLASS;
    }
}
