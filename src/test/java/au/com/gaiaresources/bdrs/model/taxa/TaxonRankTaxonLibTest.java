/**
 * 
 */
package au.com.gaiaresources.bdrs.model.taxa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import au.com.gaiaresources.taxonlib.model.TaxonRank;

/**
 * @author kehan
 * 
 */
public class TaxonRankTaxonLibTest {
    /**
     * Ensures that we have equivalent comparable TaxonRank entries in both
     * The BDRS TaxonRank has to implement JSONEnum which is why we cannot share the enum between the two
     * {@linkplain TaxonRank} and {@link au.com.gaiaresources.bdrs.model.taxa.TaxonRank}
     */
    @Test
    public void test() {
        au.com.gaiaresources.bdrs.model.taxa.TaxonRank[] bdrsRanks = au.com.gaiaresources.bdrs.model.taxa.TaxonRank
                .values();
        for (au.com.gaiaresources.bdrs.model.taxa.TaxonRank rank : bdrsRanks) {
            TaxonRank taxonLibRank = au.com.gaiaresources.taxonlib.model.TaxonRank
                    .findByIdentifier(rank.getIdentifier());
            assertTrue(rank.getIdentifier()
                    .equals(taxonLibRank.getIdentifier()));
            assertTrue(rank.getDescription().equals(
                    taxonLibRank.getDescription()));
            assertEquals(rank.getDescription(), taxonLibRank.getDescription());
            assertEquals(rank.getIdentifier(), taxonLibRank.getIdentifier());
            assertEquals(rank.getSource(), taxonLibRank.getSource());
            assertTrue("Equivalent taxonLib rank should exist",
                    rank.isEquivalentRank(taxonLibRank));
        }
        //TODO: Add test for array length equality
    }
}
