package au.com.gaiaresources.bdrs.model.facet;

import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.util.Pair;

/**
 * Interface for interacting with a DAO for faceting.  Provides methods that must be implemented for faceting.
 * 
 * @author stephanie
 *
 */
public interface FacetDAO extends TransactionDAO {

    /**
     * Gets a list of users and the count of results for each user.
     * @param sesh the session to use for this query.
     * @return
     */
    public List<Pair<User, Long>> getDistinctUsers(Session sesh);

    /**
     * Gets a lit of Location Attribute Values and the count of each
     * @param sesh the session to use for this query.
     * @param attributeName the name of the Attribute to retrieve values for
     * @param limit the limit on the number of options to return
     * @return
     */
    public List<Pair<String, Long>> getDistinctLocationAttributeValues(Session sesh,
            String attributeName, int limit);

    /**
     * Queries all records for an attribute with the given attributeName.
     * @param sesh the session to use for this query.
     * @param attributeName the name of the attribute for the query
     * @param limit the number of results to return
     * @return a list of distinct attribute value names and the count of records that match each one.
     */
    public List<Pair<String, Long>> getDistinctAttributeValues(Session sesh,
            String attributeName, int limit);

    /**
     * Counts all Records which do not have a Census Method.
     * @return the number of records without a census method.
     */
    public Integer countNullCensusMethodRecords();

    /**
     * Queries all records for a list of distinct census method types
     * and the count of records for each type filtered by user
     * @param sesh the session to use for this query.
     * @return the list of distinct census methods and count of records that match.
     */
    public List<Pair<String, Long>> getDistinctCensusMethodTypes(Session sesh);

    /**
     * Queries all records for a list of distinct locations and the count of records
     * for each location filtered by user.
     * @param sesh the session to use for this query.    
     * @param limit an optional limit to the number of results that are returned,
     *              ignored if it is less than 0
     * @return the list of distinct locations and the count of records that match.
     */
    public List<Pair<Location, Long>> getDistinctLocations(Session sesh, int limit);

    /**
     * Queries all records for a list of distinct locations and the count of records
     * for each location filtered by user.
     * @param sesh the session to use for this query.    
     * @param limit an optional limit to the number of results that are returned,
     *              ignored if it is less than 0
     * @param selectedOptions an optional collection of selected options to make sure are in the list,
     *              ignored if it is empty
     * @return the list of distinct locations and the count of records that match.
     */
    public List<Pair<Location, Long>> getDistinctLocations(Session sesh, int limit,
            Integer[] selectedOptions);

    /**
     * Returns the number of records that match the specified filter.
     * 
     * Note: if you don't use AdvancedRecordCountFilter as the concrete impl
     * of RecordFilter, this method will break in RecordDAOImpl
     * 
     * @param recFilter
     * @return the number of records that match the filter.
     */
    public int countRecords(RecordFilter filter);

    /**
     * Queries all records for a list of distinct months and the count of records
     * for that month filtered by user.
     * @param sesh the session to use for this query.
     * @return the list of distinct months and the count of records that match.
     */
    public List<Pair<Long, Long>> getDistinctMonths(Session sesh);

    /**
     * Queries all records for a list of distinct attribute types
     * and the count of records for each type
     * @param sesh the session to use for this query.
     * @return the list of distinct attribute types and count of records that match.
     */
    public List<Pair<String, Long>> getDistinctAttributeTypes(Session sesh,
            AttributeType[] attributeTypes);

    /**
     * Queries all records for a list of distinct surveys and the number of
     * records associated with that survey filtered by user.
     * @param sesh the session to use for this query.
     * @return the list of distinct surveys and record counts.
     */
    public List<Pair<Survey, Long>> getDistinctSurveys(Session sesh);

    /**
     * Queries all records for a list of distinct taxon groups and the number of
     * records associated with that taxon group filtered by user.
     * @param sesh the session to use for this query.
     * @return the list of distinct taxon groups and record counts.
     */
    public List<Pair<TaxonGroup, Long>> getDistinctTaxonGroups(Session sesh);

    /**
     * Queries all records to determine how many records exist with each of the valid RecordVisibility values.
     * <em>This method relies on an appropriate Record filter to enforce access control rules such as
     * record visibility and held status.</em>
     *
     * @return a list of distinct RecordVisibilities and the count of records that have that visibility.
     *
     */
    public List<Pair<RecordVisibility, Long>> getDistinctRecordVisibilities();

    /**
     * Queries all records for a list of distinct years and the count of records
     * for that month filtered by user.
     * @param sesh the session to use for this query.
     * @return the list of distinct years and the count of records that match.
     */
    public List<Pair<Long, Long>> getDistinctYears(Session sesh);

}
