package au.com.gaiaresources.bdrs.model.taxa.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeRelations;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.service.db.DeleteCascadeHandler;
import au.com.gaiaresources.bdrs.service.db.DeletionService;

@Repository
public class AttributeDAOImpl extends AbstractDAOImpl implements AttributeDAO {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private DeletionService delService;

    @PostConstruct
    public void init() throws Exception {
        delService.registerDeleteCascadeHandler(Attribute.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((Attribute) instance);
            }
        });
        delService.registerDeleteCascadeHandler(AttributeOption.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((AttributeOption) instance);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAttributeValues(Attribute attr, String searchString) {
        Object[] obs = new Object[2];
        obs[0] = attr;
        obs[1] = "%" + searchString.toLowerCase() + "%";
        List<AttributeValue> attrs = find("from AttributeValue r where r.attribute = ? and lower(r.stringValue) like ?", obs);
        HashSet<String> values = new HashSet<String>();
        for (AttributeValue ra : attrs) {
            values.add(ra.getStringValue());
        }
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAttributeValues(int attributePK, String searchString) {
        Attribute attr = getByID(Attribute.class, attributePK);
        return getAttributeValues(attr, searchString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAttributeValues(Attribute attr) {
        List<AttributeValue> attrs = find("from AttributeValue r where r.attribute = ?", attr);
        HashSet<String> values = new HashSet<String>();
        for (AttributeValue ra : attrs) {
            values.add(ra.getStringValue());
        }
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAttributeValues(int attributePK) {
        Attribute attr = getByID(Attribute.class, attributePK);
        return getAttributeValues(attr);
    }

    @Override
    public Attribute save(Attribute attribute) {
        return super.save(attribute);
    }

    @Override
    public AttributeOption save(AttributeOption attributeOption) {
        return super.save(attributeOption);
    }

    @Override
    public void delete(AttributeOption option) {
        deleteByQuery(option);
    }

    @Override
    public void delete(Attribute attr) {
        List<AttributeOption> optionList = new ArrayList<AttributeOption>(
                attr.getOptions());
        attr.getOptions().clear();
        save(attr);

        DeleteCascadeHandler cascadeHandler = delService.getDeleteCascadeHandlerFor(AttributeOption.class);
        for (AttributeOption option : optionList) {
            cascadeHandler.deleteCascade(option);
        }

        deleteByQuery(attr);
    }

    @Override
    public Attribute get(Integer pk) {
        return this.getByID(Attribute.class, pk);
    }

    @Override
    public <T extends TypedAttributeValue> T save(T av) {
        return super.save(av);
    }

    @Override
    public <T extends TypedAttributeValue> T update(T av) {
        return super.update(av);
    }

    @Override
    public <T extends TypedAttributeValue> void delete(T av) {
        deleteByQuery(av);
    }

    @Override
    public <T extends TypedAttributeValue> List<T> getAttributeValueObjects(
            Attribute attr) {
        return getAttributeValueObjects(getSession(), attr);
    }

    @Override
    public void delete(Session sesh, Attribute attr) {
        super.delete(sesh, attr);
    }

    @Override
    public <T extends TypedAttributeValue> void delete(Session sesh, T av) {
        super.delete(sesh, av);

    }

    @Override
    public <T extends TypedAttributeValue> List<T> getAttributeValueObjects(
            Session sesh, Attribute attr) {
        return find(sesh, "from AttributeValue r where r.attribute = ?", attr);
    }

    @Override
    public Attribute save(Session sesh, Attribute attr) {
        return super.save(sesh, attr);
    }

    @Override
    public <T extends TypedAttributeValue> T save(Session sesh, T av) {
        return super.save(sesh, av);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * au.com.gaiaresources.bdrs.model.taxa.AttributeDAO#getAttributeRelations
     * (org.hibernate.Session, java.util.List)
     */
    @Override
    public Map<Integer, AttributeRelations> getAttributeRelations(Session sesh, List<Attribute> attrList) {
        if (attrList.isEmpty()) {
            return Collections.emptyMap();
        }
        if (sesh == null) {
            sesh = getSession();
        }
        List<Integer> attrIds = new ArrayList<Integer>(attrList.size());
        for (Attribute a : attrList) {
            if (a.getId() != null) {
                attrIds.add(a.getId());
            }
        }
        
        Map<Integer, AttributeRelations> resultMap = new HashMap<Integer, AttributeRelations>();
        
        String sqlQuery = "select a.attribute_id, cm.census_method_census_method_id," 
                        + " survey.survey_survey_id, tg.taxon_group_taxon_group_id" 
                        + " from attribute a left outer" 
                        + " join census_method_attribute cm on cm.attributes_attribute_id=a.attribute_id left outer" 
                        + " join survey_attribute survey on survey.attributes_attribute_id=a.attribute_id left outer" 
                        + " join taxon_group_attribute tg on tg.attributes_attribute_id=a.attribute_id"
                        + " where a.attribute_id in (:attrIds);";
        
        SQLQuery q = sesh.createSQLQuery(sqlQuery);
        q.setParameterList("attrIds", attrIds);
        List<Object[]> resultRows = q.list();
        
        for (Object[] r : resultRows) {
            Integer attrId = (Integer)r[0];
            Integer cmId = (Integer)r[1];
            Integer surveyId = (Integer)r[2];
            Integer taxonGroupId = (Integer)r[3];
            AttributeRelations rel = new AttributeRelations(cmId, surveyId, taxonGroupId);
            resultMap.put(attrId,  rel);
        }
        
        return resultMap;
    }
    
    public Map<Integer, AttributeRelations> getAttributeRelationsByAttributeValue(Session sesh, List<AttributeValue> avList) {
        if (avList.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        List<Attribute> attrList = new ArrayList<Attribute>(avList.size());
        for (AttributeValue av : avList) {
            attrList.add(av.getAttribute());
        }
        return getAttributeRelations(sesh, attrList);
    }
    
}
