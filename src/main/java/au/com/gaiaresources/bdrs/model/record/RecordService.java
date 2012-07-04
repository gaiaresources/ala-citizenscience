package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.user.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

/**
 * Service for dealing with records.
 * @author Tim Carpenter
 */
public interface RecordService {
    void deleteRecord(Integer id);

    /**
     * Get all records that have been created by the given user.
     * @param user {@link User}
     * @return {@link List} of {@link Record}
     */
    List<Record> getRecords(User user);

    /**
     * Get all records that have been entered for the given location.
     * @param userLocation {@link Location}
     * @return {@link List} of {@link Record}
     */
    List<Record> getRecords(Location userLocation);

    /**
     * Get all records in a given radius around a location.
     * @param userLocation {@link Location}
     * @param extendKms The radius in km around the location to get records for.
     * @return {@link List} of {@link Record}
     */
    List<Record> getRecords(Location userLocation, Integer extendKms);

    /**
     * Get all records that have been entered for a species.
     * @param species {@link IndicatorSpecies}
     * @return {@link List} of {@link Record}
     */
    List<Record> getRecords(IndicatorSpecies species);

    /**
     * Count the number of records for locations entered by a user. If a location has not been
     * used yet, it is still returned in the map.
     * @param user {@link User}
     * @return {@link Map}
     */
    Map<Location, Integer> countLocationRecords(User user);

    Record getRecord(Integer id);

    void saveRecord(Record r);

    void updateRecord(Record r);

    /**
     * Returns the User that last updated the supplied Record, or null if the updatedBy property is null.
     *
     * @param record the Record of interest.
     * @return the User that last updated the supplied Record.
     */
    public User getUpdatedByUser(Record record);
}
