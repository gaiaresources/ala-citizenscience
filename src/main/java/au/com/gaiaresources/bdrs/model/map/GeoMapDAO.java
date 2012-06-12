package au.com.gaiaresources.bdrs.model.map;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.model.survey.Survey;

public interface GeoMapDAO extends TransactionDAO {
	/**
	 * Save the map.
	 * @param obj Persistent object to save.
	 * @return Saved object.
	 */
    public GeoMap save(GeoMap obj);
    /**
     * Update the map.
     * @param obj Persistent object to save.
     * @return Updated object.
     */
    public GeoMap update(GeoMap obj);
    /**
     * Get map by primary key.
     * @param pk primary key of map.
     * @return Map matching the requested primary key.
     */
    public GeoMap get(Integer pk);
    /**
     * Dlete map object.
     * @param obj Persistent object to delete.
     */
    public void delete(GeoMap obj);
    /**
     * Paged query for maps.
     * 
     * @param filter Filter that holds pagination details.
     * @param name Filter by map name.
     * @param description Filter by map description.
     * @param geoMapPk Filter by map primary key.
     * @param anonAccess Filter by map anonymous access.
     * @param publish Filter by map publish state.
     * @return Paged query result.
     */
    public PagedQueryResult<GeoMap> search(PaginationFilter filter, String name, String description, Integer geoMapPk, Boolean anonAccess,
                                    Boolean publish);
    
    /**
     * Get the map owned by a survey.
     * 
     * @param sesh Hibernate session to use for query.
     * @param survey Survey that owns the map.
     * @return Map owned by the requested survey.
     */
    public GeoMap getForSurvey(Session sesh, Survey survey);
    /**
     * Get the map owned by the owner.
     * 
     * Putting survey into here will result in an illegal argument exception.
     * Use 'getForSurvey' instead.
     * 
     * @param sesh Hibernate session to use for query.
     * @param owner Owner for the map.
     * @return Map owned by the requested owner.
     */
    public GeoMap getForOwner(Session sesh, MapOwner owner);
}
