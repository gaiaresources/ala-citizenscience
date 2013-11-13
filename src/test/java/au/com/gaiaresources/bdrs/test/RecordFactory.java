package au.com.gaiaresources.bdrs.test;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.user.User;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 25/09/13
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordFactory {

    private RecordDAO recordDAO;
    private AttributeValueDAO avDAO;

    public RecordFactory(RecordDAO recordDAO, AttributeValueDAO avDAO) {
        this.recordDAO = recordDAO;
        this.avDAO = avDAO;
    }


    public Record create(Survey survey, User u) {

        Record r = new Record();
        r.setSurvey(survey);
        r.setUser(u);

        Set<AttributeValue> attrValueSet = new HashSet<AttributeValue>();
        for (Attribute a : survey.getAttributes()) {
            AttributeValue av = new AttributeValue();
            av.setAttribute(a);
            av = avDAO.save(av);
            attrValueSet.add(av);
        }

        r.setAttributes(attrValueSet);

        return recordDAO.save(r);
    }
}
