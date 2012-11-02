package au.com.gaiaresources.bdrs.model.taxa.impl;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import junit.framework.Assert;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaxaDAOImplTest extends AbstractGridControllerTest {

    @Autowired
    private TaxaDAO taxaDAO;

    private List<TaxonGroup> groups = new ArrayList<TaxonGroup>();

    @Before
    public void setupTaxonGroups() {
        int numGroups = 10;
        for (int i=0; i<numGroups; i++) {
            TaxonGroup group = new TaxonGroup();
            group.setName(Integer.toString(i));
            taxaDAO.save(group);
            groups.add(group);
        }
    }


    @Test
    public void testHqlInjection() {
        // will error if search is not implemented properly
        taxaDAO.getIndicatorSpeciesByNameSearch("%Lewin's%Honeyeater%", false);
    }
    
    @Test
    public void testHqlInjectionSemiColon() {
        // will error if search is not implemented properly
        taxaDAO.getIndicatorSpeciesByNameSearch("%Lewin's%Honey; select from User", false);
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
        
        IndicatorSpecies result = taxaDAO.getIndicatorSpeciesBySourceDataID(getSession(), testSourceId);
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

    /**
     * Tests the build group assignment feature works in the simple case.
     */
    @Test
    public void testBulkAssignSecondaryGroup() {

        List<IndicatorSpecies> taxa = taxaDAO.getIndicatorSpecies();
        List<Integer> ids = new ArrayList<Integer>(taxa.size());
        for (IndicatorSpecies taxon : taxa) {
            ids.add(taxon.getId());
        }

        taxaDAO.bulkAssignSecondaryGroup(ids, groups.get(0));

        Session session = getRequestContext().getHibernate();
        session.flush();

        for (IndicatorSpecies taxon : taxa) {
            Assert.assertTrue(taxon.getSecondaryGroups().contains(groups.get(0)));
        }
    }


    /**
     * This test ensures the secondary group won't be added to IndicatorSpecies that have the selected group
     * as the primary group.
     */
    @Test
    public void testBulkAssignSecondaryGroupHasGroupAsPrimary() {

        List<IndicatorSpecies> taxa = taxaDAO.getIndicatorSpecies();
        List<Integer> ids = new ArrayList<Integer>(taxa.size());
        for (IndicatorSpecies taxon : taxa) {
            ids.add(taxon.getId());
        }

        // Update the primary group of the drop bear.
        dropBear.setTaxonGroup(groups.get(0));

        taxaDAO.bulkAssignSecondaryGroup(ids, groups.get(0));

        Session session = getRequestContext().getHibernate();
        session.flush();

        for (IndicatorSpecies taxon : taxa) {
            Assert.assertEquals(!taxon.equals(dropBear), taxon.getSecondaryGroups().contains(groups.get(0)));
        }
    }

    /**
     * This test ensures the secondary group won't be added a second time to IndicatorSpecies that already have
     * the group as a secondary group.
     */
    @Test
    public void testBulkAssignSecondaryGroupHasGroupAsSecondary() {

        List<IndicatorSpecies> taxa = taxaDAO.getIndicatorSpecies();
        List<Integer> ids = new ArrayList<Integer>(taxa.size());
        for (IndicatorSpecies taxon : taxa) {
            ids.add(taxon.getId());
        }

        // Update the primary group of the drop bear.
        dropBear.addSecondaryGroup(groups.get(0));

        taxaDAO.bulkAssignSecondaryGroup(ids, groups.get(0));

        Session session = getRequestContext().getHibernate();
        session.flush();

        for (IndicatorSpecies taxon : taxa) {
            Assert.assertTrue(taxon.getSecondaryGroups().contains(groups.get(0)));
            Assert.assertEquals(1, taxon.getSecondaryGroups().size());
        }
    }


    /**
     * Tests the primary group assignment feature works in the simple case.
     */
    @Test
    public void testAssignPrimaryGroup() {

        IndicatorSpecies[] toReassign = {dropBear, hoopSnake};

        List<Integer> ids = new ArrayList<Integer>(toReassign.length);
        for (IndicatorSpecies taxon : toReassign) {
            ids.add(taxon.getId());
        }

        taxaDAO.bulkUpdatePrimaryGroup(ids, groups.get(0));

        Session session = getRequestContext().getHibernate();
        session.flush();

        List<IndicatorSpecies> taxa = taxaDAO.getIndicatorSpecies();
        for (IndicatorSpecies taxon : taxa) {
            TaxonGroup expected;
            if (Arrays.asList(toReassign).contains(taxon)) {
                expected = groups.get(0);
            }
            else {
                expected = g1;
            }
            Assert.assertEquals("Taxon: "+taxon.getCommonName()+" expected group: "+expected.getName()+ " was: "+taxon.getTaxonGroup().getName(), expected, taxon.getTaxonGroup());
        }
    }

    /**
     * Tests the primary group assignment feature works in the case that the new primary group is currently
     * a secondary group of one or more of the taxa.
     */
    @Test
    public void testAssignPrimaryGroupAlreadySecondaryGroup() {

        IndicatorSpecies[] toReassign = {dropBear, hoopSnake};

        List<Integer> ids = new ArrayList<Integer>(toReassign.length);
        for (IndicatorSpecies taxon : toReassign) {
            ids.add(taxon.getId());
        }

        dropBear.addSecondaryGroup(groups.get(0));

        taxaDAO.bulkUpdatePrimaryGroup(ids, groups.get(0));

        Session session = getRequestContext().getHibernate();
        session.flush();

        List<IndicatorSpecies> taxa = taxaDAO.getIndicatorSpecies();
        for (IndicatorSpecies taxon : taxa) {
            TaxonGroup expected;
            if (Arrays.asList(toReassign).contains(taxon)) {
                expected = groups.get(0);
            }
            else {
                expected = g1;
            }
            Assert.assertEquals("Taxon: "+taxon.getCommonName()+" expected group: "+expected.getName()+ " was: "+taxon.getTaxonGroup().getName(), expected, taxon.getTaxonGroup());
            Assert.assertFalse(taxon.getSecondaryGroups().contains(groups.get(0)));
        }
    }
}
