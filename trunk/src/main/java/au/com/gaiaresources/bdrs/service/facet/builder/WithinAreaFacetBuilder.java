package au.com.gaiaresources.bdrs.service.facet.builder;

import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.Facet;
import au.com.gaiaresources.bdrs.service.facet.WithinAreaFacet;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.service.facet.option.WithinAreaFacetOption;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernatespatial.GeometryUserType;

import java.util.Map;

/**
 * User: serge
 * Date: 23/12/13
 * Time: 9:21 AM
 */
public class WithinAreaFacetBuilder extends AbstractFacetBuilder<WithinAreaFacet> {


    /**
     * Describes the function of this facet that will be used in the preference description.
     */
    public static final String FACET_DESCRIPTION = "Restricts records to an area drawn on the map.";

    /**
     * The human readable name of this facet.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Within Area";


    /**
     * Creates a new instance of this class.
     */
    public WithinAreaFacetBuilder() {
        super(WithinAreaFacet.class);
    }

    @Override
    protected Facet createFacet(FacetDAO dao, Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        WithinAreaFacet result = new WithinAreaFacet(getDefaultDisplayName(), parameterMap, userParams);
        //count the records matching the facet option and update it.
        // The no area is zero by convention.
        for (FacetOption facetOption : result.getFacetOptions()) {
            if (facetOption != WithinAreaFacetOption.NO_AREA) {
                facetOption.setCount(countMatchingRecords(facetOption));
            }
        }
        return result;
    }

    @Override
    public String getPreferenceDescription() {
        return buildPreferenceDescription(FACET_DESCRIPTION, getFacetParameterDescription());
    }

    @Override
    public String getDefaultDisplayName() {
        return DEFAULT_DISPLAY_NAME;
    }

    private Long countMatchingRecords(FacetOption option) {
        HqlQuery countQuery = new HqlQuery("select count(distinct record) from Record record");
        if (option.getPredicate() != null) {
            countQuery.and(option.getPredicate());
        }
        Query query = toHibernateQuery(countQuery);
        Object result = query.uniqueResult();
        return Long.parseLong(result.toString());
    }

    private Query toHibernateQuery(HqlQuery hqlQuery) {
        Session sesh = RequestContextHolder.getContext().getHibernate();
        Query query = sesh.createQuery(hqlQuery.getQueryString());
        Object[] parameterValues = hqlQuery.getParametersValue();
        for (int i = 0; i < parameterValues.length; i++) {
            Object param = parameterValues[i];
            if (param instanceof Geometry) {
                Type type = new CustomType(GeometryUserType.class, null);
                query.setParameter(i, parameterValues[i], type);
            } else {
                query.setParameter(i, parameterValues[i]);
            }
        }
        return query;
    }


}
