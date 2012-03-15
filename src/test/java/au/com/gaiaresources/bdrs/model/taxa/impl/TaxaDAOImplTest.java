package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class TaxaDAOImplTest extends AbstractGridControllerTest {

    @Autowired
    private TaxaDAO taxaDAO;
    
    @Test
    public void testHqlInjection() {
        // will error if search is not implemented properly
        taxaDAO.getIndicatorSpeciesByNameSearch("%Lewin's%Honeyeater%");
    }
    
    @Test
    public void testHqlInjectionSemiColon() {
        // will error if search is not implemented properly
        taxaDAO.getIndicatorSpeciesByNameSearch("%Lewin's%Honey; select from User");
    }
    
    @Test
    public void testGetBySourceDataId() {
        String testSourceId = "testsourceid12345";
        
        TaxonGroup group = new TaxonGroup();
        group.setName("my taxon group");
        taxaDAO.save(group);
        
        IndicatorSpecies species = new IndicatorSpecies();
        species.setScientificName("my name");
        species.setCommonName("species common name");
        species.setSourceId(testSourceId);
        species.setTaxonGroup(group);
        
        taxaDAO.save(species);
        
        IndicatorSpecies result = taxaDAO.getIndicatorSpeciesBySourceDataID(this.sesh, testSourceId);
        Assert.assertNotNull(result);
        Assert.assertEquals("source id mismatch", testSourceId, result.getSourceId());
    }

    /**
     * Tests that the query returns all of the taxa in the g1 group when only the group is supplied.
     * Relies on IndicatorSpecies data being populated in the AbstractGridControllerTest.
     */
    @Test
    public void testSearchIndicatorOnlyGroupNameSupplied() {
        List<IndicatorSpecies> taxa = taxaDAO.searchIndicatorSpeciesByGroupName(g1.getName(), "");
        
        IndicatorSpecies[] species = {dropBear, nyanCat, hoopSnake, surfingBird};
        Assert.assertEquals(species.length, taxa.size());
        
        for (IndicatorSpecies taxon : species) {
            Assert.assertTrue(taxa.contains(taxon));
        }

        // Make sure we can match a subset of the name.
        taxa = taxaDAO.searchIndicatorSpeciesByGroupName("animu", "");
        Assert.assertEquals(species.length, taxa.size());
        for (IndicatorSpecies taxon : species) {
            Assert.assertTrue(taxa.contains(taxon));
        }
    }

    /**
     * Tests that the query returns only the dropBear when queried for it.
     * Relies on IndicatorSpecies data being populated in the AbstractGridControllerTest.
     */
    @Test
    public void testSearchIndicatorSpeciesGroupAndTaxonNameSupplied() {
        List<IndicatorSpecies> taxa = taxaDAO.searchIndicatorSpeciesByGroupName(g1.getName(), "bear");

        Assert.assertEquals(1, taxa.size());
        Assert.assertEquals(dropBear,  taxa.get(0));
    }

    /**
     * Tests a query for just a single character in the taxon name works correctly.
     * Relies on IndicatorSpecies data being populated in the AbstractGridControllerTest.
     */
    @Test
    public void testSearchIndicatorSpeciesTaxonNameSupplied() {

        List<IndicatorSpecies> taxa = taxaDAO.searchIndicatorSpeciesByGroupName("", "c");

        IndicatorSpecies[] species = {nyanCat, hoopSnake, surfingBird};
        Assert.assertEquals(species.length, taxa.size());

        for (IndicatorSpecies taxon : species) {
            Assert.assertTrue(taxa.contains(taxon));
        }
    }

    @Test
    public void testGetIndicatorSpeciesByScientificNameAndParent() {
        String source = "source";
        IndicatorSpecies parent = new IndicatorSpecies();
        parent.setTaxonGroup(g1);
        parent.setScientificName("parent sci");
        parent.setCommonName("parent common name");
        parent.setSource(source);
        parent.setTaxonRank(TaxonRank.FAMILY);
        taxaDAO.save(parent);
        IndicatorSpecies child = new IndicatorSpecies();
        child.setTaxonGroup(g1);
        child.setScientificName("child sci");
        child.setCommonName("child common name");
        child.setSource(source);
        child.setTaxonRank(TaxonRank.GENUS);
        child.setParent(parent);
        taxaDAO.save(child);
        
        IndicatorSpecies parent_r = taxaDAO.getIndicatorSpeciesByScientificNameAndParent(null, source, "parent sci", TaxonRank.FAMILY, null);
        IndicatorSpecies child_r = taxaDAO.getIndicatorSpeciesByScientificNameAndParent(null, source, "child sci", TaxonRank.GENUS, parent.getId());
        
        Assert.assertNotNull("parent_r cannot be null", parent_r);
        Assert.assertNotNull("child_r cannot be null", child_r);
        
        Assert.assertEquals("wrong id", parent.getId(), parent_r.getId());
        Assert.assertEquals("wrong child id", child.getId(), child_r.getId());
    }
}
