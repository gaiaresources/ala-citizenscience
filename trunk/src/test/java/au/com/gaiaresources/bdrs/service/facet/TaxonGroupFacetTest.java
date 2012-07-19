package au.com.gaiaresources.bdrs.service.facet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.review.sightings.AdvancedReviewSightingsController;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;

public class TaxonGroupFacetTest extends AbstractTransactionalTest {

	@Autowired
	private TaxaDAO taxaDAO;
	@Autowired
	private RecordDAO recordDAO;
	@Autowired
	private UserDAO userDAO;
	
	private TaxonGroup g1;
	private TaxonGroup g2;
	private TaxonGroup g3;
	
	private IndicatorSpecies s1;
	private IndicatorSpecies s2;
	private IndicatorSpecies s3;
	
	private Record r1;
	private Record r2;
	private Record r3;
	
	private User currentUser;
	
	private Attribute attr;
	
	/**
	 * Used to generate the option param map name. See the ctor for TaxonGroupFacet to see
	 * why I needed to do this. I could have changed the param map to be settable after
	 * object creation but I feel that would be too risky at the moment.
	 */
	private TaxonGroupFacet dummyTaxonGroupFacet;
	
	private Logger log = Logger.getLogger(getClass());
	
	@Before
	public void setup() {
		
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
		
		currentUser = userDAO.getUser("admin");
		
		r1 = new Record();
		r1.setSpecies(s1);
		r1.setUser(currentUser);
		recordDAO.save(r1);
		
		r2 = new Record();
		r2.setSpecies(s2);
		r2.setUser(currentUser);
		recordDAO.save(r2);
		
		r3 = new Record();
		r3.setUser(currentUser);
		AttributeValue av = new AttributeValue();
		av.setAttribute(attr);
		av.setSpecies(s3);
		taxaDAO.save(av);
		Set<AttributeValue> avList = new HashSet<AttributeValue>();
		avList.add(av);
		r3.setAttributes(avList);
		recordDAO.save(r3);
		
		dummyTaxonGroupFacet = new TaxonGroupFacet("dummy group", recordDAO, new HashMap<String, String[]>(), currentUser, new JSONObject());
	}
	
	@Test
	public void testFacet_noSpeciesAttribute() {
		testTaxonGroupFacet(r1, g1, true);
		testTaxonGroupFacet(r2, g2, true);
	}
	
	@Test
	public void testFacet_speciesAttribute() {
		testTaxonGroupFacet(r3, g3, false);
	}
	
	/**
	 * Tests whether the facet query returns the correct record.
	 * 
	 * @param expectedRecord - Record we are expecting
	 * @param expectedTaxonGroup - Group we are querying on
	 * @param isPrimarySpecies - Whether the group exists as part of the primary species, i.e. record property.
	 * if false the group exists as part of a species attribute value.
	 */
	private void testTaxonGroupFacet(Record expectedRecord, TaxonGroup expectedTaxonGroup, boolean isPrimarySpecies) {
		Map<String, String[]> paramMap = new HashMap<String, String[]>();
		paramMap.put(dummyTaxonGroupFacet.getOptionsParameterName(), new String[] { expectedTaxonGroup.getId().toString() });

		TaxonGroupFacet facet = new TaxonGroupFacet("taxon group", recordDAO, paramMap, currentUser, new JSONObject());
		
		Assert.assertEquals("wrong number of options", 3, facet.getFacetOptions().size());
		for (FacetOption opt : facet.getFacetOptions()) {
			Assert.assertEquals("each option should have 1 record", 1, opt.getCount().intValue());
		}
		
		Predicate facetPredicate = facet.getPredicate();
		HqlQuery hqlQuery = prepareBaseQuery();
		facet.applyCustomJoins(hqlQuery);
		hqlQuery.and(facetPredicate);
		
		Query q = toHibernateQuery(hqlQuery, getSession());
		
		List<Object[]> result = (List<Object[]>)q.list();
		
		Assert.assertEquals("wrong result size", 1, result.size());
		
		Record rec = (Record)result.get(0)[0];
		Assert.assertEquals("wrong rec id", expectedRecord.getId(), rec.getId());
		if (isPrimarySpecies) {
			// check the species record property...
			Assert.assertEquals("wrong taxon group id", expectedTaxonGroup.getId(), 
					rec.getSpecies().getTaxonGroup().getId());
		} else {
			// we know there's only 1 possible attribute value		
			Assert.assertEquals("wrong taxon group id", expectedTaxonGroup.getId(), 
					((AttributeValue)rec.getAttributes().toArray()[0]).getSpecies().getTaxonGroup().getId());
		}
	}

	private Query toHibernateQuery(HqlQuery hqlQuery, Session sesh) {
        Query query = sesh.createQuery(hqlQuery.getQueryString());
        Object[] parameterValues = hqlQuery.getParametersValue();
        for(int i=0; i<parameterValues.length; i++) {
            query.setParameter(i, parameterValues[i]);
        }
        return query;
	}
	
	private HqlQuery prepareBaseQuery() {
		HqlQuery hqlQuery = new HqlQuery(AdvancedReviewSightingsController.BASE_FACET_QUERY);
	    AdvancedReviewSightingsController.applyJoinsForBaseQuery(hqlQuery);
	    return hqlQuery;
	}
}
