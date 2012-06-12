package au.com.gaiaresources.bdrs.model.map.impl;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.map.GeoMapDAO;
import au.com.gaiaresources.bdrs.model.map.MapOwner;
import au.com.gaiaresources.bdrs.model.survey.Survey;

@Repository
public class GeoMapDAOImpl extends AbstractDAOImpl implements GeoMapDAO {

    @Override
    public GeoMap get(Integer pk) {
        return super.getByID(GeoMap.class, pk);
    }

    @Override
    public GeoMap save(GeoMap obj) {
        return super.save(obj);
    }

    @Override
    public PagedQueryResult<GeoMap> search(PaginationFilter filter,
            String name, String description, Integer geoMapPk,
            Boolean anonAccess, Boolean publish) {
        HqlQuery q = new HqlQuery("from GeoMap gm");
        if (StringUtils.hasLength(name)) {
            q.and(Predicate.ilike("gm.name", name + "%"));
        }
        if (StringUtils.hasLength(description)) {
            q.and(Predicate.ilike("gm.description", name + "%"));
        }
        if (geoMapPk != null) {
            q.and(Predicate.eq("gm.id", geoMapPk));
        }
        if (anonAccess != null) {
            q.and(Predicate.eq("gm.anonymousAccess", anonAccess));
        }
        if (publish != null) {
            q.and(Predicate.eq("gm.publish", publish));
        }
        q.and(Predicate.eq("gm.owner", MapOwner.NONE));
        return new QueryPaginator<GeoMap>().page(this.getSession(), q.getQueryString(), q.getParametersValue(), filter, "gm");
    }

    @Override
    public GeoMap update(GeoMap obj) {
        return super.update(obj);
    }

    @Override 
    public void delete(GeoMap obj) {
        super.delete(obj);
    }
    
    @Override
    public GeoMap getForSurvey(Session sesh, Survey survey) {
    	if (sesh == null) {
    		sesh = this.getSession();
    	}
    	Query q = sesh.createQuery("from GeoMap where survey = :survey");
    	q.setParameter("survey", survey);
    	return (GeoMap)q.uniqueResult();
    }

	@Override
	public GeoMap getForOwner(Session sesh, MapOwner owner) {
		if (sesh == null) {
    		sesh = this.getSession();
    	}
    	Query q = sesh.createQuery("from GeoMap where owner = :owner");
    	q.setParameter("owner", owner);
    	return (GeoMap)q.uniqueResult();
	}
}
