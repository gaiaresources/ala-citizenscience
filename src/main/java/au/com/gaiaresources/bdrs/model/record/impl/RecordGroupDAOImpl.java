package au.com.gaiaresources.bdrs.model.record.impl;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.RecordGroupDAO;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 9/10/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class RecordGroupDAOImpl extends AbstractDAOImpl implements RecordGroupDAO {

    public RecordGroup getRecordGroup(Integer pk) {
        return getByID(RecordGroup.class, pk);
    }

    @Override
    public RecordGroup getRecordGroupByClientID(String clientID) {
        if(clientID == null) {
            throw new NullPointerException();
        }

        Session sesh = super.getSessionFactory().getCurrentSession();
        Query q = sesh.createQuery("select distinct r from RecordGroup r left join r.metadata md where md.key = :key and md.value = :value");
        q.setParameter("key", Metadata.RECORD_GROUP_CLIENT_ID_KEY);
        q.setParameter("value", clientID);

        q.setMaxResults(1);
        return (RecordGroup)q.uniqueResult();
    }

    @Override
    public  Metadata getRecordGroupMetadataForKey(RecordGroup recordGroup, String metadataKey){
        for(Metadata md: recordGroup.getMetadata()){
            if (md.getKey().equals(metadataKey)){
                return md;
            }
        }
        Metadata md = new Metadata();
        md.setKey(metadataKey);
        return md;
    }
}
