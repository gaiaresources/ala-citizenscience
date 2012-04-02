package au.com.gaiaresources.bdrs.controller.fieldguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import au.com.gaiaresources.bdrs.json.*;

import org.apache.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.admin.setup.SetupController;
import au.com.gaiaresources.bdrs.controller.test.TestDataCreator;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataHelper;
import au.com.gaiaresources.bdrs.model.portal.impl.PortalInitialiser;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.security.Role;

public class BDRSFieldGuideControllerTest extends AbstractControllerTest {
    
    Logger log = Logger.getLogger(this.getClass());
    
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;
    @Autowired
    private SearchService searchService;
    
    private static final Random RANDOM = new Random(23458743);

    // keywords to use for building profiles and testing keyword search
    private static final String[] PROFILE_KEYWORDS = new String[] {
        "habitat", "australia", "water", "land", "trees", "reefs", "reef", 
        "diet", "leaves", "insects", "eggs", 
        "activity", "swim", "walk", "run", "hunt",
        "features", "gills", 
        "status", "endangered", "threatened",
        "height", "grow", "meters", "metres", "inches", "feet"
    };
    
    private static final String[] KEYWORD_STOP_WORDS = new String[] {
        "of", "and", "the", "into", "a", "they"
    };
    // keep track of the keyword use by taxonomy
    private Map<String, Integer> keywordUseByTaxonomy = new HashMap<String, Integer>(PROFILE_KEYWORDS.length);
    // keep track of the total keyword use
    private Map<String, Integer> keywordUse = new HashMap<String, Integer>(PROFILE_KEYWORDS.length);
    
    /*
     * Testing retrieving taxa by taxongroup id only.
     */
    
    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        
    }
    
    @Before
    public void setup() throws Exception {
        for (String keyword : PROFILE_KEYWORDS) {
            keywordUse.put(keyword, 0);
        }
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        // delete the indexes to make sure they are not saved from last time
        searchService.deleteIndexes(sesh);
        searchService.createIndexes(sesh);
    }
    
    @After
    public void tearDown() throws Exception {
        keywordUse.clear();
        keywordUseByTaxonomy.clear();
    }

    @Test
    public void testSearchTaxaByTaxonGroupId() throws Exception {
        
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
        request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELGUIDE_LIST_TAXA_URL);
        request.setParameter("search_in_groups", q);
        // add this parameter to keep the paging from affecting the results if 
        // expectedNumberOfSpecies > pageSize
        if (expectedNumberOfSpecies >= 1) {
            request.setParameter(JqGridDataHelper.MAX_PER_PAGE_PARAM, String.valueOf(expectedNumberOfSpecies));
        }
        handle(request, response);
        
        Assert.assertEquals("Content type should be application/json",
                "application/json", response.getContentType());
        
        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());
        int actualNumberOfSpecies = responseContent.getJSONArray("rows").size();
        
        Assert.assertEquals("Expected " + expectedNumberOfSpecies + " taxa.", expectedNumberOfSpecies, actualNumberOfSpecies);
    }
    
    @Test
    public void testSearchInGroupsByCommonName() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testSearchInGroupsBy("\""+getRandomSpecies(expected).getCommonName()+"\"", 1, expected);
    }
    
    @Test
    public void testSearchInGroupsByScientificName() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testSearchInGroupsBy("\""+getRandomSpecies(expected).getScientificName()+"\"", 1, expected);
    }
    
    @Test
    public void testSearchInGroupsByTaxonGroupName() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testSearchInGroupsBy(expected.getName(), taxaDAO.getIndicatorSpecies(expected).size(), expected);
    }
    
    @Test
    public void testSearchInGroupsByDescription() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        // get a random keyword to search for
        String keyword = getRandomUsedKeyword(expected);
        // use the total keyword count here as this search is not dependent on the taxon group
        testSearchInGroupsBy(keyword, keywordUse.get(keyword), expected);
    }

    /* 
     * Testing retrieving taxa by using a taxongroupId and 'search in result' value as query-parameters
     */
    
     private void testTaxonGroupIdSearchInResultBy(String searchInResult, int expectedNumberOfSpecies, TaxonGroup expected ) throws Exception {
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
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testTaxonGroupIdSearchInResultBy("\""+getRandomSpecies(expected).getCommonName()+"\"", 1, expected);
    }

    @Test
    public void testTaxonGroupIdSearchInResultByScientificName() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testTaxonGroupIdSearchInResultBy("\""+getRandomSpecies(expected).getScientificName()+"\"", 1, expected);
    }
    
    @Test
    public void testTaxonGroupIdSearchInResultByDescription() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        String keyword = getRandomUsedKeyword(expected);
        Integer count = keywordUseByTaxonomy.get(expected.getName()+"_"+keyword);
        testTaxonGroupIdSearchInResultBy(keyword, count == null ? 0 : count, expected);
    }
    
    
    /*
     * Testing retrieving taxa by 'search in groups' query-parameter and 'search in result' query-parameter
     */
    
    private void testQuerySearchInResultBy(String q, String subq, int expectedNumberOfSpecies, TaxonGroup expected ) throws Exception {
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
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testQuerySearchInResultBy(expected.getName(),"\""+getRandomSpecies(expected).getCommonName()+"\"", 1, expected);
    }

    @Test
    public void testQuerySearchInResultByScientificName() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        testQuerySearchInResultBy(expected.getName(),"\""+getRandomSpecies(expected).getScientificName()+"\"", 1, expected);
    }
    
    @Test
    public void testQuerySearchInResultByDescription() throws Exception {
        TaxonGroup expected = taxaDAO.getTaxonGroups().get(0);
        String keyword = getRandomUsedKeyword(expected);
        Integer count = keywordUseByTaxonomy.get(expected.getName()+"_"+keyword);
        testQuerySearchInResultBy(expected.getName(),keyword, count == null ? 0 : count, expected);
    }
    
    /*
     * Additional tests
     */
    
    @Test
    public void testListGroups() throws Exception {
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
        TaxonGroup group = new TaxonGroup();
        group.setName("Mammals");
        taxaDAO.save(group);
        group = new TaxonGroup();
        group.setName("Reptiles");
        taxaDAO.save(group);
        
        String[] colors = new String[]{"Green", "Purple", "Blue", "Black", "White", "Yellow", "Red"};
        String[] latinColors = new String[]{"Pratinus", "Purpureus", "Caeruleus", "Fuscus", "Albus", "Flavus", "Roseus"};
        
        for (TaxonGroup taxonGroup : taxaDAO.getTaxonGroups()) {
            String animal = taxonGroup.getName().equals("Mammals") ? "Monkey" : "Dragon";
            String latinAnimal = taxonGroup.getName().equals("Mammals") ? "Alouatta" : "Ctenophorus";
            
            for (int i = 0; i < colors.length; i++) {
                IndicatorSpecies species = new IndicatorSpecies();
                species.setScientificName(latinColors[i] + " " + latinAnimal);
                species.setCommonName(colors[i] + " " + animal);
                List<SpeciesProfile> profileItems = new ArrayList<SpeciesProfile>();
                SpeciesProfile description = speciesProfileDAO.createSpeciesProfile(
                                                "description_db", "description", 
                                                createDescription(taxonGroup.getName()), "text");
                profileItems.add(speciesProfileDAO.save(description));
                
                species.setRunThreshold(false);
                species.setInfoItems(profileItems);
                species.setTaxonGroup(taxonGroup);
                
                taxaDAO.save(species);
            }
        }
    }

    /**
     * Create a random description of keywords for keyword searches.
     * @param name Name of the TaxonGroup, for keeping track of keyword usage
     * @return The randomly created description
     */
    private String createDescription(String name) {
        // get a random number of terms to add to the content
        StringBuilder sb = new StringBuilder();
        // keep track of the used words, so they are only added to the counts once
        List<String> usedWords = new ArrayList<String>();
        for (int j = 0; j < 24; j++) {
            String keyword = PROFILE_KEYWORDS[RANDOM.nextInt(PROFILE_KEYWORDS.length-1)];
            // increment the keyword usage map
            if (!usedWords.contains(keyword)) {
                Integer count = keywordUseByTaxonomy.remove(name+"_"+keyword);
                if (count == null) {
                    count = 0;
                }
                count++;
                keywordUseByTaxonomy.put(name+"_"+keyword, count);
                
                count = keywordUse.remove(keyword);
                if (count == null) {
                    count = 0;
                }
                count++;
                keywordUse.put(keyword, count);
                usedWords.add(keyword);
            }
            sb.append(keyword);
            sb.append(" ");
            sb.append(KEYWORD_STOP_WORDS[RANDOM.nextInt(KEYWORD_STOP_WORDS.length-1)]);
            sb.append(" ");
        }
        return sb.toString();
    }
    

    /**
     * Gets a random keyword from the list that has been used in a species profile 
     * for a species in the given TaxonGroup
     * @param expected The TaxonGroup
     * @return A random keyword
     */
    private String getRandomUsedKeyword(TaxonGroup expected) {
        String keyword = PROFILE_KEYWORDS[RANDOM.nextInt(PROFILE_KEYWORDS.length-1)];
        Integer count = keywordUseByTaxonomy.get(expected+"_"+keyword);
        // limit the number of tries to find a word
        int tries = 0;
        while ((count == null || count < 1) && tries < 10) {
            keyword = PROFILE_KEYWORDS[RANDOM.nextInt(PROFILE_KEYWORDS.length-1)];
            count = keywordUseByTaxonomy.get(expected+"_"+keyword);
            tries++;
        }
        return keyword;
    }
    

    /**
     * Gets a random species for testing
     * @param expected
     * @return
     */
    private IndicatorSpecies getRandomSpecies(TaxonGroup taxonGroup) {
        List<IndicatorSpecies> speciesList = taxaDAO.getIndicatorSpecies(taxonGroup);
        return speciesList.get(RANDOM.nextInt(speciesList.size()-1));
    }
}
