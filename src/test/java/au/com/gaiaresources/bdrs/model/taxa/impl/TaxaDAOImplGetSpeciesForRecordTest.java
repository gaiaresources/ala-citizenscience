package au.com.gaiaresources.bdrs.model.taxa.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

public class TaxaDAOImplGetSpeciesForRecordTest extends
		AbstractTransactionalTest {

    @Autowired
    private RecordDAO recDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    
    private Survey survey;
    private Record rec;
    
    private IndicatorSpecies species;
    private IndicatorSpecies species2;
    private IndicatorSpecies species3;
    
    private TaxonGroup group1;
    private TaxonGroup group2;
    private TaxonGroup group3;
	
	@Before
    public void setup() {
		
		User admin = userDAO.getUser("admin");
		
		survey = new Survey();
		survey.setName("my survey");
		survey.setDescription("survey desc");
		surveyDAO.save(survey);
    	
    	group1 = new TaxonGroup();
    	group1.setName("group 1");
    	taxaDAO.save(group1);
    	
    	group2 = new TaxonGroup();
    	group2.setName("group 2");
    	taxaDAO.save(group2);
    	
    	group3 = new TaxonGroup();
    	group3.setName("group 3");
    	taxaDAO.save(group3);
    	
    	species = new IndicatorSpecies();
    	species.setTaxonGroup(group1);
    	species.setScientificName("sci name 1");
    	species.setCommonName("common name 1");
    	taxaDAO.save(species);
    	
    	species2 = new IndicatorSpecies();
    	species2.setTaxonGroup(group2);
    	species2.setScientificName("sci name 2");
    	species2.setCommonName("common name 2");
    	taxaDAO.save(species2);
    	
    	species3 = new IndicatorSpecies();
    	species3.setTaxonGroup(group2);
    	species3.setScientificName("sci name 3");
    	species3.setCommonName("common name 3");
    	taxaDAO.save(species3);
    	
    	Attribute speciesAttr = new Attribute();
    	speciesAttr.setName("speciesattr");
    	speciesAttr.setDescription("species attr desc");
    	speciesAttr.setTypeCode(AttributeType.SPECIES.getCode());
    	taxaDAO.save(speciesAttr);
    	
    	AttributeValue recAv1 = new AttributeValue();
    	recAv1.setAttribute(speciesAttr);
    	recAv1.setSpecies(species);
    	recAv1.setStringValue(species.getScientificName());
    	taxaDAO.save(recAv1);
    	
    	AttributeValue recAv2 = new AttributeValue();
    	recAv2.setAttribute(speciesAttr);
    	recAv2.setSpecies(species);
    	recAv2.setStringValue(species.getScientificName());
    	taxaDAO.save(recAv2);
    	
    	AttributeValue recAv3 = new AttributeValue();
    	recAv3.setAttribute(speciesAttr);
    	recAv3.setSpecies(species2);
    	recAv3.setStringValue(species2.getScientificName());
    	taxaDAO.save(recAv3);
    	
    	Set<AttributeValue> avSet = new HashSet<AttributeValue>();
    	avSet.add(recAv1);
    	avSet.add(recAv2);
    	avSet.add(recAv3);
    	
    	rec = new Record();
    	rec.setAttributes(avSet);
    	rec.setUser(admin);
    	rec.setSurvey(survey);
    	recDAO.save(rec);
    }
	
	@Test
	public void testGetSpeciesForRecord() {
		List<IndicatorSpecies> result = taxaDAO.getSpeciesForRecord(null, rec.getId());
		Assert.assertEquals("wrong size", 2, result.size());
		Assert.assertTrue("does not contain species", result.contains(species));
		Assert.assertTrue("does not contain species2", result.contains(species2));
	}
	
	@Test
	public void testGetDistinctSpeciesForSurvey() {
		List<IndicatorSpecies> result = taxaDAO.getDistinctRecordedTaxaForSurvey(survey.getId());
		Assert.assertEquals("wrong size", 2, result.size());
		Assert.assertTrue("does not contain species", result.contains(species));
		Assert.assertTrue("does not contain species2", result.contains(species2));
	}
}
