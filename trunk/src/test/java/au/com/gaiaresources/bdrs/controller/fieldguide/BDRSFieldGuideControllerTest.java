package au.com.gaiaresources.bdrs.controller.fieldguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import au.com.gaiaresources.bdrs.json.*;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.test.TestDataCreator;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.security.Role;

public class BDRSFieldGuideControllerTest extends AbstractControllerTest {
	
	Logger log = Logger.getLogger(this.getClass());
	
	private static final int NUMBER_OF_FIELDS_TO_TEST = 6;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;
    
    
    /*
     * Testing retrieving taxa by taxongroup id only.
     */
    
    @Test
    public void testSearchTaxaByTaxonGroupId() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        createTestData();
        
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        
        request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELDGUIDE_TAXA_URL);
        request.setParameter("groupId", expected.getId().toString());
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxonGroup");
        Assert.assertEquals(expected.getId(), ((TaxonGroup)mv.getModel().get("taxonGroup")).getId());
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxaPaginator");
        ModelAndViewAssert.assertViewName(mv, "fieldGuideTaxaListing");
    }
    

	/* 
	 * Testing retrieving taxa by using 'search in groups' value as query-parameter.
	 */
    
    private void testSearchInGroupsBy(String q, int expectedNumberOfSpecies, TaxonGroup expected) throws Exception {
        createIndicatorSpecies(expected);
        
        request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELGUIDE_LIST_TAXA_URL);
        request.setParameter("search_in_groups", q);
        
        handle(request, response);
        
        Assert.assertEquals("Content type should be application/json",
                "application/json", response.getContentType());
        
        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());
        int actualNumberOfSpecies = responseContent.getJSONArray("rows").size();
        
        Assert.assertEquals("Expected " + expectedNumberOfSpecies + " taxa.", expectedNumberOfSpecies, actualNumberOfSpecies);
    }
	
	@Test
	public void testSearchInGroupsByCommonName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testSearchInGroupsBy("Cn_1", 1, expected);
	}
	
	@Test
	public void testSearchInGroupsByScientificName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testSearchInGroupsBy("Sn_1", 1, expected);
	}
	
	@Test
	public void testSearchInGroupsByTaxonGroupName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testSearchInGroupsBy(expected.getName(), taxaDAO.getIndicatorSpecies(expected).size(), expected);
	}
	
	@Test
	public void testSearchInGroupsByDescription() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testSearchInGroupsBy("Des_1", 1, expected);
	}
	
	
	/* 
	 * Testing retrieving taxa by using a taxongroupId and 'search in result' value as query-parameters
	 */
	
	 private void testTaxonGroupIdSearchInResultBy(String searchInResult, int expectedNumberOfSpecies, TaxonGroup expected ) throws Exception {
    	 createIndicatorSpecies(expected);
    	 request.setMethod("GET");
         request.setRequestURI(BDRSFieldGuideController.FIELGUIDE_LIST_TAXA_URL);
         request.setParameter("groupId", expected.getId().toString());
         request.setParameter("search_in_result", searchInResult);
         
         handle(request, response);
         
         Assert.assertEquals("Content type should be application/json",
                 "application/json", response.getContentType());
        
         JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());
         int actualNumberOfSpecies = responseContent.getJSONArray("rows").size();
         
         Assert.assertEquals("Expected " + expectedNumberOfSpecies + " taxa.", expectedNumberOfSpecies, actualNumberOfSpecies);
    }
	 
	@Test
		public void testTaxonGroupIdSearchInResultByCommonName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testTaxonGroupIdSearchInResultBy("Cn_", NUMBER_OF_FIELDS_TO_TEST, expected);
	}

	@Test
	public void testTaxonGroupIdSearchInResultByScientificName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testTaxonGroupIdSearchInResultBy("Sn_", NUMBER_OF_FIELDS_TO_TEST, expected);
	}
	
	@Test
	public void testTaxonGroupIdSearchInResultByDescription() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testTaxonGroupIdSearchInResultBy("Des_1", 1, expected);
	}
	
	
	/*
	 * Testing retrieving taxa by 'search in groups' query-parameter and 'search in result' query-parameter
	 */
	
    private void testQuerySearchInResultBy(String q, String subq, int expectedNumberOfSpecies, TaxonGroup expected ) throws Exception {
    	createIndicatorSpecies(expected);
    	request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELGUIDE_LIST_TAXA_URL);
        request.setParameter("search_in_groups", q);
        request.setParameter("search_in_result", subq);
        
        handle(request, response);
        
        Assert.assertEquals("Content type should be application/json",
                "application/json", response.getContentType());
        
        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());
        int actualNumberOfSpecies = responseContent.getJSONArray("rows").size();
        
        Assert.assertEquals("Expected " + expectedNumberOfSpecies + " taxa.", expectedNumberOfSpecies, actualNumberOfSpecies);
    }
		
	@Test
	public void testQuerySearchInResultByCommonName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testQuerySearchInResultBy(expected.getName(),"Cn_2", 1, expected);
	}

	@Test
	public void testQuerySearchInResultByScientificName() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testQuerySearchInResultBy(expected.getName(),"Sn_2", 1, expected);
	}
	
	@Test
	public void testQuerySearchInResultByDescription() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		createTestData();
		TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
		testQuerySearchInResultBy(expected.getName(),"Des_2", 1, expected);
	}
	
	/*
	 * Additional tests
	 */
	
    @Test
    public void testListGroups() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        createTestData();
        request.setMethod("GET");
        request.setRequestURI("/fieldguide/groups.htm");
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxonGroups");
        List<TaxonGroup> taxonGroups = (List<TaxonGroup>)mv.getModel().get("taxonGroups");
        Assert.assertEquals(taxaDAO.getTaxonGroups().size(), taxonGroups.size());
        ModelAndViewAssert.assertViewName(mv, "fieldGuideGroupListing");
    }
    
    @Test
    public void testViewTaxa() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        createTestData();
        
        IndicatorSpecies expected = taxaDAO.getIndicatorSpecies().get(0);
        
        request.setMethod("GET");
        request.setRequestURI("/fieldguide/taxon.htm");
        request.setParameter("id", expected.getId().toString());
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxon");
        Assert.assertEquals(expected.getId(), ((IndicatorSpecies)mv.getModel().get("taxon")).getId());
        ModelAndViewAssert.assertViewName(mv, "fieldGuideViewTaxon");
    }
    
    /*
     * Helper functions
     */
    
    private void createTestData() throws Exception {
        ApplicationContext appContext = getRequestContext().getApplicationContext();
        TestDataCreator testDataCreator = new TestDataCreator(appContext);
        
        testDataCreator.createTaxonGroups(2, 0, true);
        testDataCreator.createTaxa(3, 0);
        testDataCreator.createTaxonProfile();
    }
    
    private void createIndicatorSpecies(TaxonGroup taxonGroup) throws Exception {
    	for (int i=0; i<NUMBER_OF_FIELDS_TO_TEST; i++){
	        
    		List<SpeciesProfile> profileItems = new ArrayList<SpeciesProfile>();
	        SpeciesProfile description = speciesProfileDAO.createSpeciesProfile("description_db", "description", "des_" + i, "text");
	        profileItems.add(speciesProfileDAO.save(description));
	        
	        IndicatorSpecies species = new IndicatorSpecies();
    	    species.setRunThreshold(false);
	        species.setScientificName("sn_" + i);
	        species.setCommonName("cn_" + i);
	        species.setTaxonGroup(taxonGroup);
	        species.setInfoItems(profileItems);
	        
	        taxaDAO.save(taxonGroup);
	        taxaDAO.save(species);
    	}
    }
    
}
