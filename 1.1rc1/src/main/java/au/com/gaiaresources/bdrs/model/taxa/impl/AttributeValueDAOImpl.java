package au.com.gaiaresources.bdrs.model.taxa.impl;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;

@Repository
public class AttributeValueDAOImpl extends AbstractDAOImpl implements
        AttributeValueDAO {

    @Override
    public AttributeValue get(int id) {
        return this.get(getSession(), id);
    }

    @Override
    public AttributeValue get(Session sesh, int id) {
        return super.getByID(sesh, AttributeValue.class, id);
    }

    @Override
    public AttributeValue save(AttributeValue attrVal) {
        return this.save(getSession(), attrVal);
    }

    @Override
    public AttributeValue save(Session sesh, AttributeValue attrVal) {
        return super.save(sesh, attrVal);
    }

    @Override
    public AttributeValue update(AttributeValue attrVal) {
        return this.update(getSession(), attrVal);
    }

    @Override
    public AttributeValue update(Session sesh, AttributeValue attrVal) {
        return super.update(sesh, attrVal);
    }
    
    @Override
    public int delete(AttributeValue attrVal) {
        return super.deleteByQuery(attrVal);
    }
}