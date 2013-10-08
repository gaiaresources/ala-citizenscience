package au.com.gaiaresources.bdrs.model.record;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.SortingCriteria;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;

import com.vividsolutions.jts.geom.Geometry;

public interface RecordDAO extends FacetDAO {
	List<Record> getRecords(Survey survey, Set<User> users);

	List<Record> getRecords(User user);

	List<Record> getRecords(Location userLocation);

	/**
	 * Retrieves the records for the specified survey in the specified location.
	 * 
	 * @param user
	 *            the person who created the records.
	 * @param survey
	 *            the containing survey for the records to be retrieved.
	 * @param location
	 *            the location of the records to be retrieved.
	 * @param startDate
	 *            the start date (inclusive) of the records
	 * @param endDate
	 *            the end date (inclusive) of the records
	 * @return the records for the specified survey in the specified location.
	 */
	List<Record> getRecords(User user, Survey survey, Location location, Date startDate, Date endDate);

	/**
	 * Retrieves the records for a survey where a certain species is recorded
	 * 
	 * @param surveyId id of the survey
	 * @param speciesId id of the species
	 * @return List of records.
	 */
	List<Record> getRecordBySurveySpecies(int surveyId, int speciesId);

	List<Record> getRecords(Geometry withinGeom);

	List<Record> getRecords(IndicatorSpecies species);

	Integer countAllRecords();

	Integer countUniqueSpecies();

    /**
     * Gets last modified record for a user
     * @param user
     * @return Record if one exists. Null otherwise.
     */
    Record getLatestRecord(User user);

    /**
     * Gets last modified record on the server
     * @return Record if one exists. Null otherwise.
     */
	Record getLatestRecord();

	/**
	 * Counts all of the records owned by the given user.  If you want the count 
	 * of records that the user has access to, use {@link #countAllRecords(User)} instead.
	 * @param user The owner of the records
	 * @return A count of all records owned by the user
	 */
	public Integer countRecords(User user);

	Integer countRecordsForSpecies(IndicatorSpecies species);

	Integer countSpecies(User user);

	/**
	 * Count the number of records entered for the locations created by a user.
	 * 
	 * @param user
	 *            {@link User}
	 * @return {@link Map}
	 */
	Map<Location, Integer> countLocationRecords(User user);

	Record getRecord(Session sesh, Integer id);

	Record getRecord(Integer id);

	void deleteById(Integer id);

	Record saveRecord(Record r);

	Record updateRecord(Record r);

	TypedAttributeValue updateAttribute(Integer id, BigDecimal numeric,
			String value, Date date);

	void saveRecordList(List<Record> records);

	List<Record> getRecord(User user, int groupId, int surveyId,
			int taxonGroupId, Date startDate, Date endDate,
			String speciesScientificNameSearch, int limit);

	List<Record> getRecord(User user, int groupId, int surveyId,
			int taxonGroupId, Date startDate, Date endDate,
			String speciesScientificNameSearch, int limit, boolean fetch);
	
	ScrollableRecords getScrollableRecords(User user, int groupPk, int surveyPk,
	            int taxonGroupPk, Date startDate, Date endDate, String species);
	
	ScrollableRecords getScrollableRecords(User user, int groupPk, int surveyPk,
                int taxonGroupPk, Date startDate, Date endDate, String species,
                int pageNumber, int entriesPerPage);

    /**
     * Get scrollable records with the following criteria
     * A null or empty parameter will ignore that parameter.
     *
     * @param user Record owner
     * @param surveys Survey of the record
     * @param species Primary species of the record
     * @param startDate Start date range
     * @param endDate End date range
     * @param pageNumber Page number to return
     * @param entriesPerPage Limits the number of returned records
     * @return ScrollableRecords
     */
    ScrollableRecords getScrollableRecords(User user, List<Survey> surveys,
                                           List<Integer> species,
                                           Date startDate, Date endDate,
                                           int pageNumber, int entriesPerPage);
	
	/**
	 * Stopping the madness of too many args when filtering for records. Encapsulate all
	 * future querying in the RecordFilter object
	 * 
	 * @param recFilter
	 * @return
	 */
	ScrollableRecords getScrollableRecords(RecordFilter recFilter);
	
	/**
         * Queries for Records based on the filter arguments applying the sort
         * options specified by <code>sortCriteria</code>.
         * 
         * @param recFilter a scrollable list of records.
         * @param sortCriteria a list of column name, order type 
         * (ascending or descending) pairs to apply to the query.
         * @return a scrollable list of records.
         */
	ScrollableRecords getScrollableRecords(RecordFilter recFilter,
	            List<SortingCriteria> sortCriteria);
	
	/**
	 * Returns the number of records that match the specified filter.
	 * 
	 * Note: if you don't use AdvancedRecordCountFilter as the concrete impl
	 * of RecordFilter, this method will break in RecordDAOImpl
	 * 
	 * @param recFilter
	 * @return the number of records that match the filter.
	 */
	int countRecords(RecordFilter recFilter);
	
	/**
	 * Returns a list of dates when a record was made for the specified
	 * scientific name fragment.
	 * 
	 * @param scientificNameSearch
	 *            the fragment of the scientific name.
	 */
	List<Date> getRecordDatesByScientificNameSearch(String scientificNameSearch);

	/**
	 * Retrieves records that were created by the specified user ordered by when
	 * they were created.
	 * 
	 * @param user
	 *            The creator of the records
	 * @param scientificNameSearch
	 *            the fragment of the scientific name.
	 * @param limit
	 *            The maximum number of records to return
	 * @return records created by the specified user.
	 */
	List<Record> getLatestRecords(User user, String scientificNameSearch,
			int limit);

	/**
	 * 
	 * @param recAttr
	 * @return
	 * @deprecated Use {@link AttributeValueDAO#save(AttributeValue)} instead.
	 */
	@Deprecated
	AttributeValue saveAttributeValue(AttributeValue recAttr);
	
	/**
	 * 
	 * @param recAttr
	 * @return
	 * @deprecated Use {@link AttributeValueDAO#update(AttributeValue) instead.
	 */
	@Deprecated
	AttributeValue updateAttributeValue(AttributeValue recAttr);

	List<Record> getRecords(String userRegistrationKey, int surveyPk,
			int locationPk);

	/**
	 * Finds potential duplicates for records based on configurable distance,
	 * time and record properties
	 * 
	 * @param record
	 * @param extendMetres
	 *            - how far to buffer around the record
	 * @param calendarField
	 *            - Integer eg Calendar.MINUTE - the Calendar field type to
	 *            buffer time by (other options include Calendar.SECOND,
	 *            Calendar.HOUR
	 * @param extendTime
	 *            - Amount of time to buffer by
	 * @param excludeRecordIds
	 *            - an array of record ids that you want to specifically exclude
	 *            from search
	 * @return
	 */
	HashSet<Record> getDuplicateRecords(Record record, double extendMetres,
			int calendarField, int extendTime, Integer[] excludeRecordIds,
			Integer[] includeRecordIds);

	Metadata getRecordMetadataForKey(Record record, String metadataKey);

	/**
	 * Returns the <code>AttributeValue</code> with the specified primary key
	 * or null if one does not exist.
	 * 
	 * @param recordAttributePk
	 *            primary key of the <code>AttributeValue</code> to be
	 *            retrieved.
	 * @return the <code>AttributeValue</code> with the provided primary key or
	 *         null if one does not exist.
	 * @deprecated Use {@link AttributeValueDAO#get(int)} instead.
	 */
	@Deprecated
	AttributeValue getAttributeValue(int recordAttributePk);

	/**
	 * Returns the <code>Record</code> that contains a
	 * <code>AttributeValue</code> with the specified primary key.
	 * 
	 * @param sesh
	 *            the session to use to retrieve the <code>Record</code>.
	 * @param id
	 *            the primary key of the <code>AttributeValue</code>.
	 * @return the <code>Record</code>
	 */
	PersistentImpl getRecordForAttributeValueId(Session sesh, Integer id);

	/**
	 * Returns a list of the latest species recorded for a user.
	 * @param userPk the user
	 * @return
	 */
    List<IndicatorSpecies> getLastSpecies(int userPk, int limit);

    void delete(Record record);

    /**
     * 
     * @param recAttr
     * @deprecated Use {@link AttributeValueDAO#delete(AttributeValue)} instead
     */
    @Deprecated
    void delete(AttributeValue recAttr);
    
    PagedQueryResult<Record> search(PaginationFilter filter, Integer surveyPk, List<Integer> userId);
    
    /**
     * Get all child records. Can filter by census method id.
     * 
     * @param filter - pagination filter
     * @param parentId - id of parent record
     * @param censusMethodId - id of census method of the child record
     * @return The child records in pagination friendly form.
     */
    PagedQueryResult<Record> getChildRecords(PaginationFilter filter, Integer parentId, Integer censusMethodId, User accessingUser);

    /**
     * spatial query for records
     * 
     * @param mapLayerId - the map layer id
     * @param intersectGeom - the geometry to intersect with
     * @param isPrivate - whether the record has to be private. true: publish is OWNER_ONLY
     * false: publish is anything but OWNER_ONLY. null: don't care
     * @param userId - The id of the owner of the record. If the user passed here matches the owner of the record
     * the record will be returned regardless of the isPrivate flag.
     * @return
     */
    List<Record> find(Integer[] mapLayerId, Geometry intersectGeom, Boolean isPrivate, Integer userId);
    
    /**
     * Returns a count number of records, ordered by id, starting with offset.
     * @param count
     * @param offset
     * @return
     */
    public List<Record> getRecords(int count, int offset);

    /**
     * Returns the record associated with the specified client id (such as
     * the mobile record primary key)
     * @param clientID the client identifier
     * @return the record associated with the specified client ID.
     */
    public Record getRecordByClientID(String clientID);

    /**
     * Counts all of the records that this user has access to view.
     * @param accessor the account that is accessing the records
     * @return A count of all records that the user has access to.
     */
    public Integer countAllRecords(User accessor);

    /**
     * Returns a list of public records that intersect the specified geometry.
     * @param intersectGeom the polygon that records must intersect
     * @return a list of public records that intersect the specified geometry.
     */
    List<Record> getRecordIntersect(Geometry intersectGeom);

    /**
     * Returns a list of records that intersect the specified geometry.
     * @param intersectGeom the polygon that records must intersect
     * @param visibility the visibility of all records to be returned.
     * @param held true if the record is held, false otherwise.
     * @return a list of records that intersect the specified geometry.
     */
    List<Record> getRecordIntersect(Geometry intersectGeom,
            RecordVisibility visibility, boolean held);
    
    /**
     * Query for records where an attribute value has an attribute name.
     * with a given string value.
     * 
     * @param sesh session - can be null.
     * @param surveyId survey ID to limit search. Not nullable.
     * @param attrName attribute name to limit search. Not Nullable or empty.
     * @param attrVal attribute STRING value to limit search. Will not work for numeric values! Not nullable or empty.
     * @return List of records that matches parameters.
     */
    List<Record> getRecordByAttributeValue(Session sesh, Integer surveyId, String attrName, String attrVal);
    
    /**
     * Query for records where an attribute value has an attribute id.
     * Orders results by alphabetic attribute value string value.
     * 
     * @param sesh Hibernate session - nullable.
     * @param userId Owner of the record. Not nullable.
     * @param surveyId Survey records must belong to. Not nullable.
     * @param attrId Attribute ID of an AttributeValue that the record owns. Not nullable.
     * @return List of matching records.
     */
    List<Record> getRecordByAttribute(Session sesh, Integer userId, Integer surveyId, Integer attrId);
    
    /**
     * Query for records that have field species assigned.
     * Orders results by alphabetic field name.
     * 
     * @param sesh Hibernate session - nullable.
     * @param userId Owner of the record. Not nullable.
     * @param surveyId Survey records must belong to. Not nullable.
     * @param taxaService TaxaService to get IndicatorSpecies for field species.
     * @return List of matching records.
     */
    List<Record> getFieldNameRecords(Session sesh, Integer userId, Integer surveyId, TaxaService taxaService);
}