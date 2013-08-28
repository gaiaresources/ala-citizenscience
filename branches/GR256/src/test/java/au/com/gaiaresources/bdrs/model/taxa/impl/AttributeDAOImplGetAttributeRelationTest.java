package au.com.gaiaresources.bdrs.model.taxa.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOwnerType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeRelations;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

/**
 * Tests a single method in the DAO - the method returns the owner for a given attribute.
 * The current valid values are census method, survey and taxon group.
 *
 */
public class AttributeDAOImplGetAttributeRelationTest extends
        AbstractTransactionalTest {

    @Autowired
    private AttributeDAO attrDAO;

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private CensusMethodDAO cmDAO;
    @Autowired
    private TaxaDAO taxaDAO;

    private Survey survey;
    private CensusMethod cm;
    private TaxonGroup tg;

    private Attribute surveyAttr;
    private Attribute cmAttr;
    private Attribute tgAttr;

    @Before
    public void setup() {
        survey = new Survey();
        survey.setName("test survey");
        survey.setDescription("test survey desc");
        survey.setAttributes(getTestAttrList("survey"));
        surveyDAO.save(survey);

        surveyAttr = survey.getAttributes().get(0);

        cm = new CensusMethod();
        cm.setName("cm name");
        cm.setDescription("cm desc");
        cm.setAttributes(getTestAttrList("cm"));
        cmDAO.save(cm);

        cmAttr = cm.getAttributes().get(0);

        tg = new TaxonGroup();
        tg.setName("tg name");
        tg.setAttributes(getTestAttrList("tg"));
        taxaDAO.save(tg);

        tgAttr = tg.getAttributes().get(0);
        
        // need to flush to write many to many relationships to the database
        // so we can then query.
        getSession().flush();
    }

    /**
     * Tests a single method in the DAO - the method returns the owner for a given attribute.
     * The current valid values are census method, survey and taxon group.
     */
    @Test
    public void testGetRelations() {
        List<Attribute> attrList = new ArrayList<Attribute>();
        attrList.add(surveyAttr);
        attrList.add(cmAttr);
        attrList.add(tgAttr);
        Map<Integer, AttributeRelations> resultMap = attrDAO.getAttributeRelations(null, attrList);

        Assert.assertEquals("wrong size", 3, resultMap.size());

        assertRelations(resultMap.get(surveyAttr.getId()), AttributeOwnerType.SURVEY, survey.getId(), null, null);
        assertRelations(resultMap.get(cmAttr.getId()), AttributeOwnerType.CENSUS_METHOD, null, cm.getId(), null);
        assertRelations(resultMap.get(tgAttr.getId()), AttributeOwnerType.TAXON_GROUP, null, null, tg.getId());
    }
    
    @Test
    public void testEmpty() {
        List<Attribute> attrList = new ArrayList<Attribute>();
        Map<Integer, AttributeRelations> resultMap = attrDAO.getAttributeRelations(null, attrList);
        Assert.assertEquals("wrong size", 0, resultMap.size());
    }

    /**
     * Convenience helper for assertion.
     * @param rel
     * @param expectedType
     * @param expectedSurveyId
     * @param expectedCensusMethodId
     * @param expectedTaxonGroupId
     */
    private void assertRelations(AttributeRelations rel,
            AttributeOwnerType expectedType, Integer expectedSurveyId,
            Integer expectedCensusMethodId, Integer expectedTaxonGroupId) {
        Assert.assertNotNull("AttributeRelations should not be null", rel);
        Assert.assertEquals("wrong owner type", expectedType, rel.getAttributeOwnerType());
        if (expectedSurveyId != null) {
            Assert.assertEquals("wrong survey id", expectedSurveyId, rel.getSurveyId());
        } else {
            Assert.assertNull("survey id should be null", rel.getSurveyId());
        }

        if (expectedCensusMethodId != null) {
            Assert.assertEquals("wrong cm id", expectedCensusMethodId, rel.getCensusMethodId());
        } else {
            Assert.assertNull("census method id should be null", rel.getCensusMethodId());
        }

        if (expectedTaxonGroupId != null) {
            Assert.assertEquals("wrong taxon group id", expectedTaxonGroupId, rel.getTaxonGroupId());
        } else {
            Assert.assertNull("taxon group id should be null", rel.getTaxonGroupId());
        }
    }

    /**
     * Convenience method to help test data generation.
     * @param prefix Prefix for names / descriptions of attr
     * @return List of attributes
     */
    private List<Attribute> getTestAttrList(String prefix) {
        List<Attribute> attrList = new ArrayList<Attribute>(1);
        Attribute a = new Attribute();
        a.setName(prefix + "_attrname");
        a.setDescription(prefix + "_attrdesc");
        a.setTypeCode(AttributeType.STRING.getCode());
        attrDAO.save(a);
        attrList.add(a);
        return attrList;
    }
}
