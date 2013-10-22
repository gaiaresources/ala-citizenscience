package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 9/10/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RecordGroupDAO extends TransactionDAO {

    /**
     * Get record group by pk
     * @param pk Primary key
     * @return RecordGroup
     */
    public RecordGroup getRecordGroup(Integer pk);

    /**
     * Returns the record associated with the specified client id (such as
     * the mobile record primary key)
     * @param clientID the client identifier
     * @return the record associated with the specified client ID.
     */
    public RecordGroup getRecordGroupByClientID(String clientID);


    /**
     * Gets the specified record group meta data or lazy creates it.
     * Does not save metadata or adds it to the set for the RecordGroup
     *
     * @param recordGroup RecordGroup that owns metadata
     * @param metadataKey Metadata key
     * @return Metadata item with matching key
     */
    public Metadata getRecordGroupMetadataForKey(RecordGroup recordGroup, String metadataKey);
}
