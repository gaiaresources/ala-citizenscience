package au.com.gaiaresources.bdrs.controller.review.sightings;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.review.AdvancedReviewController;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
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
import au.com.gaiaresources.bdrs.security.Role;

/**
 * Only the JSON request mapping is tested here.
 */
public class AdvancedReviewSightingsControllerSearchTextTest extends
		AbstractControllerTest {

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
	
	private Survey survey1;
	private Survey survey2;
	private Survey survey3;
	
	private IndicatorSpecies s1;
	private IndicatorSpecies s2;
	private IndicatorSpecies s3;
	
	private Record r1;
	private Record r2;
	private Record r3;
	
	private User currentUser;
	
	private Attribute attr;
	
	private static final String USERNAME = "admin";
	
	private Logger log = Logger.getLogger(getClass());
	
	@Before
	public void setup() {
		
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
		
		currentUser = userDAO.getUser(USERNAME);
		
		r1 = new Record();
		r1.setSurvey(survey1);
		r1.setSpecies(s1);
		r1.setUser(currentUser);
		recordDAO.save(r1);
		
		r2 = new Record();
		r2.setSurvey(survey2);
		r2.setSpecies(s2);
		r2.setUser(currentUser);
		recordDAO.save(r2);
		
		r3 = new Record();
		r3.setSpecies(s3);
		r3.setSurvey(survey3);
		r3.setUser(currentUser);
		AttributeValue av = new AttributeValue();
		av.setAttribute(attr);
		av.setSpecies(s3);
		taxaDAO.save(av);
		Set<AttributeValue> avList = new HashSet<AttributeValue>();
		avList.add(av);
		r3.setAttributes(avList);
		recordDAO.save(r3);
		
		// must flush or subsequent tests will return 0 results
		// in their queries
		getSession().flush();
	}
	
	@Test
	public void testEmptySearch() throws Exception {
		login(USERNAME, "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewJSONSightings.htm");
        handle(request, response);
        JSONArray recJsonArray = JSONArray.fromString(response.getContentAsString());
        Assert.assertEquals("wrong result size", 3, recJsonArray.size());
	}
	
	@Test
	public void testSpeciesPropertySearch() throws Exception {
		login(USERNAME, "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewJSONSightings.htm");
        request.setParameter(AdvancedReviewController.SEARCH_QUERY_PARAM_NAME, "common one");
        handle(request, response);
        JSONArray recJsonArray = JSONArray.fromString(response.getContentAsString());
        Assert.assertEquals("wrong result size", 1, recJsonArray.size());
        JSONObject recJsonObj = recJsonArray.getJSONObject(0);
        Assert.assertEquals("wrong id", r1.getId().intValue(), recJsonObj.optInt("id"));
	}
	
	@Test
	public void testSpeciesAttributeSearch() throws Exception {
		login(USERNAME, "password", new String[]{Role.ADMIN});
        request.setMethod("GET");
        request.setRequestURI("/review/sightings/advancedReviewJSONSightings.htm");
        request.setParameter(AdvancedReviewController.SEARCH_QUERY_PARAM_NAME, "species three");
        handle(request, response);
        JSONArray recJsonArray = JSONArray.fromString(response.getContentAsString());
        Assert.assertEquals("wrong result size", 1, recJsonArray.size());
        JSONObject recJsonObj = recJsonArray.getJSONObject(0);
        Assert.assertEquals("wrong id", r3.getId().intValue(), recJsonObj.optInt("id"));
	}
}
