package au.com.gaiaresources.bdrs.model.location.impl;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.type.CustomType;
import org.hibernatespatial.GeometryUserType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.QueryOperation;
import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.db.DeleteCascadeHandler;
import au.com.gaiaresources.bdrs.service.db.DeletionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.util.SpatialUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Implementation of {@link LocationDAO} for dealing with {@link Location}
 * objects.
 * 
 * @author Tim Carpenter
 */

@Repository
public class LocationDAOImpl extends AbstractDAOImpl implements LocationDAO {
    private Logger log = Logger.getLogger(getClass().getName());
    
    @Autowired
    private DeletionService delService;
    
    @PostConstruct
    public void init() throws Exception {
        delService.registerDeleteCascadeHandler(Location.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((Location)instance);
            }
        });
    }

    @Override
    public Location getLocation(int pk) {
        return getLocation(super.getSession(), pk);
    }
    
    @Override
    public Location getLocation(org.hibernate.Session sesh, int pk) {
        if(sesh == null) {
            sesh = super.getSession();
        }
        return (Location)sesh.get(Location.class, pk);
    }
    
    @Override
    public Location getLocationByClientID(String clientID) {
        return getLocationByClientID(super.getSession(), clientID);
    }
    
    @Override
    public Location getLocationByClientID(org.hibernate.Session session, String clientID) {
        if(clientID == null) {
            throw new NullPointerException();
        }
        
        Query q = session.createQuery("select distinct loc from Location loc left join loc.metadata md where md.key = :key and md.value = :value");
        q.setParameter("key", Metadata.LOCATION_CLIENT_ID_KEY);
        q.setParameter("value", clientID);
        q.setMaxResults(1);
        return (Location)q.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Location> getLocations(List<Integer> ids) {
    	Query query = getSession().createQuery("FROM Location loc WHERE loc.id IN (:ids)");
    	query.setParameterList("ids", ids);
    	return query.list();
    }

    @Override
    public Location createUserLocation(User user, String locationName,
            Point location, Collection<? extends Region> regions) {
        Location l = new Location();
        l.setUser(user);
        l.setLocation(location);
        l.setName(locationName);
        Set<Region> regionSet = new HashSet<Region>();
        for (Region r : regions) {
            regionSet.add(r);
        }
        l.setRegions(regionSet);
        return save(l);
    }

    @Override
    public Location getUserLocation(User user, String locationName) {
        String query = "from Location loc where loc.user = :user and loc.name = :name";
        Query q = getSession().createQuery(query);
        q.setParameter("user", user);
        q.setParameter("name", locationName);
        List<Location> locList = q.list();
        if (locList.isEmpty()) {
            return null;
        } else {
            if (locList.size() > 1) {
                log.warn(String.format("More than one Location returned for the user \"%s\" with location name \"%s\" returning the first.", user.getName(), locationName));
            }
            return locList.get(0);
        }
    }

    @Override
    public Location getUserLocation(User user, Integer locationID) {
        Location l = getByID(Location.class, locationID);
        if (l.getUser().equals(user)) {
            return l;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Location> getUserLocations(User user) {
        return user != null ? find("from Location l where l.user = ? order by l.name", user) : Collections.emptyList();
    }

    @Override
    public List<Location> getUserLocations(User user, Region region) {
    	StringBuilder sb = new StringBuilder("from Location l where");
    	sb.append(" l.user = :user");
    	sb.append(" and (within(transform(l.location,"+BdrsCoordReferenceSystem.DEFAULT_SRID+"),:withinGeom) = true)");
    	sb.append(" order by l.name");
    	Query q = getSession().createQuery(sb.toString());
    	CustomType geometryType = new CustomType(GeometryUserType.class, null);
    	q.setParameter("withinGeom", region.getBoundary(), geometryType);
    	q.setParameter("user", user);
    	return q.list();
    }

    @Override
    public Location updateUserLocation(Integer locationID,
            String newLocationName, Point newLocation) {
        Location location = getByID(Location.class, locationID);
        location.setName(newLocationName);
        return update(location);
    }

    @Override
    public Integer countUserLocations(User user) {
        Query q = getSession().createQuery("select count(*) from Location l where l.user = ?");
        q.setParameter(0, user);
        Integer count = Integer.parseInt(q.list().get(0).toString(), 10);
        return count;
    }

    @Override
    public Location createLocation(Location loc) {
        return super.save(loc);
    }

    @Override
    public Location updateLocation(Location loc) {
        Object o = merge(loc);
        update((Location) o);
        return (Location) o;
    }

    @Override
    public Location save(Location loc) {
        if (loc.getId() == null || (loc.getId() != null && loc.getId() <= 0)) {
            return this.createLocation(loc);
        } else {
            return this.updateLocation(loc);
        }
    }

    @Override
    public Location getLocationByName(String surveyName, String locationName) {
        return getLocationByName(null, surveyName, locationName);
    }

    @Override
    public Location getLocationByName(Session sesh, String surveyName,
            String locationName) {
        if(sesh == null) {
            sesh = getSessionFactory().getCurrentSession();
        }
        
        List<Location> locations = find(sesh, "select loc from Survey s, Location loc where loc.id in (select id from s.locations) and s.name = ? and loc.name = ?", new Object[] {
                surveyName, locationName });
        if (locations.isEmpty()) {
            return null;
        } else {
            if (locations.size() > 1) {
                log.warn("Multiple locations with the same name found. Returning the first");

            }
            return locations.get(0);
        }
    }

    @Override
    public void delete(Location delLoc) {
        super.deleteByQuery(delLoc);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Location> getLocation(Survey survey, User user) {
    	// Warning : this _might_ cause issues when we select locations that
    	// contain geometries with different SRIDs.
        StringBuilder builder = new StringBuilder();
        builder.append(" select distinct loc ");
        builder.append(" from Survey s join s.locations loc ");
        builder.append(" where ");
        builder.append("     s = :survey and ");
        builder.append("     loc.user = :user and ");
        builder.append("     (s.public = true or :user in (select u from s.users u))");
        builder.append(" order by loc.name");

        Query q = getSession().createQuery(builder.toString());
        q.setParameter("survey", survey);
        q.setParameter("user", user);

        return q.list();
    }

    @Override
    public PagedQueryResult<Location> getSurveylocations(
            PaginationFilter filter, User user, int surveyId) {
        
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        boolean unrestricted = user.isPoweruser() || user.isSupervisor() || user.isAdmin();
        // This convoluted SQL expression is to avoid calling distinct on
        // a geometry object. Postgis does not allow spatial functions (i.e. st_xxx())
        // to be called on geometries with different SRIDs.
        StringBuilder builder = new StringBuilder();
        builder.append("from Location l where l.id in (");
        builder.append("select distinct loc.id ");
        builder.append(" from Survey s join s.locations loc");
        builder.append(" where s.id != :surveyId");
        if (!unrestricted) {
            builder.append( " and (s.public = true or :user in (select u from s.users u))");
        }
        builder.append(")");
        Map<String, Object> argMap = new HashMap<String, Object>();
        argMap.put("surveyId", surveyId);
        if (!unrestricted) {
            argMap.put("user", user);
        }
        return new QueryPaginator<Location>().page(this.getSession(), builder.toString(), argMap, filter, "l");
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctSurveys(org.hibernate.Session)
     */
    @Override
    public List<Pair<Survey, Long>> getDistinctSurveys(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select s, count(loc)");
        b.append(" from Location as loc join loc.surveys as s");

        b.append(" group by s.id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(Survey.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(Transient.class) == null) &&
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) {
                b.append(", s."+pd.getName());
            }
        }
        b.append(" order by s.weight asc, s.name asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == taxon group, Object[1] == record count
        List<Pair<Survey, Long>> results = 
            new ArrayList<Pair<Survey, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<Survey, Long>((Survey)row[0], (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctUsers(org.hibernate.Session)
     */
    @Override
    public List<Pair<User, Long>> getDistinctUsers(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select u, count(l)");
        b.append(" from Location as l join l.user as u");
        b.append(" group by u.id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(User.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(ForeignKey.class) == null) && // ignore other table joins
                (pd.getReadMethod().getAnnotation(Transient.class) == null || // ignore transients 
                        "active".equals(pd.getName())) &&                     // except active
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) 
            {
                b.append(", u."+pd.getName());
            }
        }
        b.append(" order by 2 desc, u.name asc");

        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == location, Object[1] == record count
        List<Pair<User, Long>> results = 
            new ArrayList<Pair<User, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<User, Long>((User)row[0], (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctAttributeValues(org.hibernate.Session, java.lang.String, int)
     */
    @Override
    public List<Pair<String, Long>> getDistinctAttributeValues(Session sesh,
            String attributeName, int limit) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#countNullCensusMethodRecords()
     */
    @Override
    public Integer countNullCensusMethodRecords() {
        // not applicable for location, throw an exception?
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctCensusMethodTypes(org.hibernate.Session)
     */
    @Override
    public List<Pair<String, Long>> getDistinctCensusMethodTypes(Session session) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocations(org.hibernate.Session, int)
     */
    @Override
    public List<Pair<Location, Long>> getDistinctLocations(Session session,
            int optionsLimit) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocations(org.hibernate.Session, int, java.lang.Integer[])
     */
    @Override
    public List<Pair<Location, Long>> getDistinctLocations(Session session,
            int optionsLimit, Integer[] selected) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#countRecords(au.com.gaiaresources.bdrs.model.record.impl.RecordFilter)
     */
    @Override
    public int countRecords(RecordFilter filter) {
        // not applicable for location, throw an exception?
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctMonths(org.hibernate.Session)
     */
    @Override
    public List<Pair<Long, Long>> getDistinctMonths(Session session) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctAttributeTypes(org.hibernate.Session, au.com.gaiaresources.bdrs.model.taxa.AttributeType[])
     */
    @Override
    public List<Pair<String, Long>> getDistinctAttributeTypes(Session sesh,
            AttributeType[] attributeTypes) {
        StringBuilder b = new StringBuilder();
        b.append(" select distinct a.typeCode, count(distinct loc)");
        b.append(" from Location as loc join loc.attributes as la join la.attribute as a");
        b.append(" where length(trim(la.stringValue)) > 0 and (1 = 2");
        for(AttributeType type : attributeTypes) {
            b.append(String.format(" or a.typeCode = '%s'", type.getCode()));
        }
        b.append(" )");

        b.append(" group by a.typeCode");
        b.append(" order by a.typeCode asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctTaxonGroups(org.hibernate.Session)
     */
    @Override
    public List<Pair<TaxonGroup, Long>> getDistinctTaxonGroups(Session session) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctRecordVisibilities()
     */
    @Override
    public List<Pair<RecordVisibility, Long>> getDistinctRecordVisibilities() {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctYears(org.hibernate.Session)
     */
    @Override
    public List<Pair<Long, Long>> getDistinctYears(Session session) {
        // not applicable for location, throw an exception?
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocationAttributeValues(org.hibernate.Session, java.lang.String, int)
     */
    @Override
    public List<Pair<String, Long>> getDistinctLocationAttributeValues(
            Session sesh, String attributeName, int limit) {
        StringBuilder b = new StringBuilder();
        
        b.append(" select distinct locAttrVal.stringValue, count(distinct loc)");
        b.append(" from Location as loc join loc.attributes as locAttrVal join locAttrVal.attribute as attr");
        b.append(" where ");
        b.append(String.format(" attr.description = '%s'", attributeName));
        // ignore empty string values
        b.append(" and locAttrVal.stringValue is not null and locAttrVal.stringValue != ''");
        b.append(" group by locAttrVal.stringValue");
        b.append(" order by 2 desc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        
        Query q = sesh.createQuery(b.toString());

        if (limit > 0) {
            q.setMaxResults(limit);
        }
        
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.location.LocationDAO#getLocations()
     */
    @Override
    public List<Location> getLocations() {
        return this.find("from Location");
    }
}
