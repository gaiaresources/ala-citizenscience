package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.record.RecordGroup;
import au.com.gaiaresources.bdrs.model.record.RecordGroupDAO;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 23/10/13
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class PyRecordGroupDAO extends AbstractPyDAO {

    private RecordGroupDAO recordGroupDAO;

    public PyRecordGroupDAO(RecordGroupDAO recordGroupDAO) {
        this.recordGroupDAO = recordGroupDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(recordGroupDAO, RecordGroup.class, pk);
    }
}
