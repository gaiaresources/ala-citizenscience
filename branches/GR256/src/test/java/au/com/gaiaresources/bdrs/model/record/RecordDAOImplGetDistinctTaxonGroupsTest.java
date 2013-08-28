package au.com.gaiaresources.bdrs.model.record;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.FilterManager;
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
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import au.com.gaiaresources.bdrs.util.Pair;

public class RecordDAOImplGetDistinctTaxonGroupsTest extends
		AbstractTransactionalTest {
	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private RecordDAO recordDAO;
	@Autowired
	private UserDAO userDAO;
	@Autowired
	private SurveyDAO surveyDAO;
	
	private TaxonGroup g1;
	private TaxonGroup g2;
	private TaxonGroup g3;
	private TaxonGroup g4;
	
	private Survey survey1;
	private Survey survey2;
	private Survey survey3;
	
	private IndicatorSpecies s1;
	private IndicatorSpecies s2;
	private IndicatorSpecies s3;
	
	private Record r1;
	private Record r2;
	private Record r3;
	private Record r4;
	private Record r5;
	
	private User admin;
	private User normalUser;
	
	private Attribute attr;
	
	private static final String USERNAME = "admin";
	
	private Logger log = Logger.getLogger(getClass());
	
	@Before
	public void setup() {
		
		normalUser = userDAO.createUser("normalUser", "normalUser", "normalUser", "normalUser@normalUser.com", "password", "regkey", new String[] { Role.USER });
		
		survey1 = new Survey();
		survey1.setName("survey one");
		surveyDAO.save(survey1);
		
		survey2 = new Survey();
		survey2.setName("survey two");
		surveyDAO.save(survey2);
		
		survey3 = new Survey();
		survey3.setName("survey three");
		surveyDAO.save(survey3);
		
		attr = new Attribute();
		attr.setName("second_species");
		attr.setDescription("second species");
		attr.setTypeCode(AttributeType.SPECIES.getCode());
		taxaDAO.save(attr);
		
		g1 = new TaxonGroup();
		g1.setName("group one");
		taxaDAO.save(g1);
		g2 = new TaxonGroup();
		g2.setName("group two");
		taxaDAO.save(g2);
		g3 = new TaxonGroup();
		g3.setName("group three");
		taxaDAO.save(g3);
		g4 = new TaxonGroup();
		g4.setName("group four");
		taxaDAO.save(g4);
		
		s1 = new IndicatorSpecies();
		s1.setTaxonGroup(g1);
		s1.setScientificName("species one");
		s1.setCommonName("common one");
		taxaDAO.save(s1);
		
		s2 = new IndicatorSpecies();
		s2.setTaxonGroup(g2);
		s2.setScientificName("species two");
		s2.setCommonName("common two");
		taxaDAO.save(s2);
		
		s3 = new IndicatorSpecies();
		s3.setTaxonGroup(g3);
		s3.setScientificName("species three");
		s3.setCommonName("common three");
		taxaDAO.save(s3);
		
		admin = userDAO.getUser(USERNAME);
		
		r1 = new Record();
		r1.setSurvey(survey1);
		r1.setSpecies(s1);
		r1.setUser(admin);
		r1.setRecordVisibility(RecordVisibility.OWNER_ONLY);
		{
			// double attribute value.
			AttributeValue av = new AttributeValue();
			av.setAttribute(attr);
			av.setSpecies(s3);
			taxaDAO.save(av);
			Set<AttributeValue> avSet = new HashSet<AttributeValue>();
			avSet.add(av);
			
			av = new AttributeValue();
			av.setAttribute(attr);
			av.setSpecies(s3);
			taxaDAO.save(av);
			avSet.add(av);
			
			r1.setAttributes(avSet);
		}
		recordDAO.save(r1);
		
		// public record, record species only.
		r2 = new Record();
		r2.setSurvey(survey2);
		r2.setSpecies(s2);
		r2.setUser(admin);
		r2.setRecordVisibility(RecordVisibility.PUBLIC);
		recordDAO.save(r2);
		
		// public record, record species only.
		r3 = new Record();
		r3.setSpecies(s3);
		r3.setSurvey(survey3);
		r3.setUser(admin);
		r3.setRecordVisibility(RecordVisibility.PUBLIC);
		{
			AttributeValue av = new AttributeValue();
			av.setAttribute(attr);
			av.setSpecies(s3);
			taxaDAO.save(av);
			Set<AttributeValue> avSet = new HashSet<AttributeValue>();
			avSet.add(av);
			r3.setAttributes(avSet);
		}
		recordDAO.save(r3);
		
		r4 = new Record();
		r4.setSpecies(s3);
		r4.setSurvey(survey3);
		r4.setUser(admin);
		r4.setRecordVisibility(RecordVisibility.PUBLIC);
		recordDAO.save(r4);
		
		// r5 only has AV species set
		r5 = new Record();
		r5.setSurvey(survey3);
		r5.setUser(admin);
		r5.setRecordVisibility(RecordVisibility.OWNER_ONLY);
		{
			AttributeValue av = new AttributeValue();
			av.setAttribute(attr);
			av.setSpecies(s3);
			taxaDAO.save(av);
			Set<AttributeValue> avSet = new HashSet<AttributeValue>();
			avSet.add(av);
			r5.setAttributes(avSet);
		}
		recordDAO.save(r5);
		
		Record r6 = new Record();
		r6.setSurvey(survey3);
		r6.setUser(admin);
		r6.setRecordVisibility(RecordVisibility.OWNER_ONLY);
		r6.setSpecies(s3);
		recordDAO.save(r6);
		
		// no species in record or attribute value.
		Record r7 = new Record();
		r7.setSurvey(survey3);
		r7.setUser(admin);
		r7.setRecordVisibility(RecordVisibility.OWNER_ONLY);
		recordDAO.save(r7);
		
		// Ensure flush before querying.
		getSession().flush();
	}
	
	@Test
	public void testQueryAdmin() {
		FilterManager.enableRecordFilter(getSession(), admin);
		List<Pair<TaxonGroup, Long>> pairList = recordDAO.getDistinctTaxonGroups(getSession());
		
		Pair<TaxonGroup, Long> p1 = getPair(pairList, g1);
		Pair<TaxonGroup, Long> p2 = getPair(pairList, g2);
		Pair<TaxonGroup, Long> p3 = getPair(pairList, g3);
		Pair<TaxonGroup, Long> p4 = getPair(pairList, g4);
		
		Assert.assertNotNull("p1 should not be null", p1);
		Assert.assertNotNull("p2 should not be null", p2);
		Assert.assertNotNull("p3 should not be null", p3);
		Assert.assertNull("p4 should be null, 0 records assigned", p4);
		
		// taxon group 1 should exist in 1 record
		Assert.assertEquals("wrong count p1", 1, p1.getSecond().intValue());
		// taxon group 2 should exist in 1 record
		Assert.assertEquals("wrong count p2", 1, p2.getSecond().intValue());
		// taxon group 3 should exist in 3 records
		Assert.assertEquals("wrong count p3", 5, p3.getSecond().intValue());
	}
	
	@Test
	public void testQueryNormalUser() {
		FilterManager.enableRecordFilter(getSession(), normalUser);
		List<Pair<TaxonGroup, Long>> pairList = recordDAO.getDistinctTaxonGroups(getSession());
		
		Pair<TaxonGroup, Long> p1 = getPair(pairList, g1);
		Pair<TaxonGroup, Long> p2 = getPair(pairList, g2);
		Pair<TaxonGroup, Long> p3 = getPair(pairList, g3);
		Pair<TaxonGroup, Long> p4 = getPair(pairList, g4);
		
		Assert.assertNull("p1 should be null", p1);
		Assert.assertNotNull("p2 should not be null", p2);
		Assert.assertNotNull("p3 should not be null", p3);
		Assert.assertNull("p4 should be null, 0 records assigned", p4);
		
		// taxon group 2 should exist in 1 record
		Assert.assertEquals("wrong count p2", 1, p2.getSecond().intValue());
		// taxon group 3 should exist in 3 records
		Assert.assertEquals("wrong count p3", 2, p3.getSecond().intValue());
	}
	
	private Pair<TaxonGroup, Long> getPair(List<Pair<TaxonGroup, Long>> pairs, TaxonGroup g) {
		for (Pair<TaxonGroup, Long> p : pairs) {
			if (p.getFirst().equals(g)) {
				return p;
			}
		}
		return null;
	}
}
