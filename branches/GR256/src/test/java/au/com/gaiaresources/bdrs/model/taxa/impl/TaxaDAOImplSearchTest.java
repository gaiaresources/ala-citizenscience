package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.test.AbstractTransactionalTest;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 8/10/13
 * Time: 10:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class TaxaDAOImplSearchTest extends AbstractTransactionalTest {

    @Autowired
    private TaxaDAO taxaDAO;

    private TaxonGroup primaryGroup;
    private TaxonGroup secondaryGroup;
    private IndicatorSpecies species1;
    private IndicatorSpecies species2;

    private Logger log = Logger.getLogger(getClass());

    @Before
    public void setup() {

        primaryGroup = new TaxonGroup();
        primaryGroup.setName("primary group");
        taxaDAO.save(primaryGroup);

        secondaryGroup = new TaxonGroup();
        secondaryGroup.setName("secondary group");
        taxaDAO.save(secondaryGroup);

        species1 = new IndicatorSpecies();
        species1.setScientificName("sp sci one");
        species1.setCommonName("sp common one");
        species1.setTaxonGroup(primaryGroup);
        taxaDAO.save(species1);

        species2 = new IndicatorSpecies();
        species2.setScientificName("sp sci two");
        species2.setCommonName("sp common two");
        species2.setTaxonGroup(primaryGroup);
        species2.addSecondaryGroup(secondaryGroup);
        taxaDAO.save(species2);
    }

    @Test
    public void testSearchSpeciesByGroupName() {
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(primaryGroup.getName(), null, true);
            Assert.assertEquals("wrong size", 2, result.size());
            assertContains(result, species1);
            assertContains(result, species2);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(primaryGroup.getName(), null, false);
            Assert.assertEquals("wrong size", 2, result.size());
            assertContains(result, species1);
            assertContains(result, species2);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(secondaryGroup.getName(), null, true);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species2);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(secondaryGroup.getName(), null, false);
            Assert.assertEquals("wrong size", 0, result.size());
        }
    }

    @Test
    public void testSearchSpeciesBySpeciesName() {
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(null, species1.getScientificName(), true);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(null, species1.getCommonName(), true);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(null, species1.getScientificName(), false);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(null, species1.getCommonName(), false);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
    }

    @Test
    public void testSearchSpeciesBySpeciesNameAndGroupName() {
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(primaryGroup.getName(),
                    species1.getScientificName(), false);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk("non existent group",
                    species1.getScientificName(), false);
            Assert.assertEquals("wrong size", 0, result.size());
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(primaryGroup.getName(),
                    species1.getScientificName(), true);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species1);
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(secondaryGroup.getName(),
                    species1.getScientificName(), true);
            Assert.assertEquals("wrong size", 0, result.size());
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(secondaryGroup.getName(),
                    species2.getScientificName(), false);
            Assert.assertEquals("wrong size", 0, result.size());
        }
        {
            List<Integer> result = taxaDAO.searchIndicatorSpeciesPk(secondaryGroup.getName(),
                    species2.getScientificName(), true);
            Assert.assertEquals("wrong size", 1, result.size());
            assertContains(result, species2);
        }
    }


    private void assertContains(List<Integer> speciesList, IndicatorSpecies sp) {

        Assert.assertTrue("missing species pk for : " + sp.getScientificName(),
                speciesList.contains(sp.getId()));
    }
}
