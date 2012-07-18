/**
 * 
 */
package au.com.gaiaresources.bdrs.controller.fieldguide;

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.service.taxonomy.AbstractBdrsMaxImporterTest;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import au.com.gaiaresources.taxonlib.importer.max.MaxImporter;
import au.com.gaiaresources.taxonlib.model.ITaxonConcept;

/**
 * @author kehan
 *
 */
public class BDRSFieldGuideControllerTaxonLibTest extends
        AbstractBdrsMaxImporterTest {
    private ITemporalContext temporalContext;
    /*Lycopodiella cernua */
    private static String OLD_NAME_ID = "SPECIES_2";
    /* Lycopodium cernuum */
    private static String NEW_NAME_ID = "SPECIES_12813";
    /* Isoetes*/
    private static String TAXON_WITH_CHILDREN = "GENUS_20877";
    
    Logger log = Logger.getLogger(this.getClass());
    @Autowired
    TaxaDAO taxaDAO;
    
    @Before
    public void setup() throws Exception {

        runDefaultImport();
        temporalContext = taxonLibSession.getTemporalContext(now);
    }
    @Test
    public void testTaxonLibVis() throws Exception {

        request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELDGUIDE_TAXON_URL);

        ITaxonConcept expectedConcept = temporalContext.selectConcept(MaxImporter.MAX_SOURCE, OLD_NAME_ID);
        IndicatorSpecies expectedIndicatorSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(null, expectedConcept.getName().getId().toString());
        
        request.setParameter("id", expectedIndicatorSpecies.getId().toString());
        request.setParameter("timestamp", String.valueOf(now.getTime()));
        ModelAndView mv = handle(request,response);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "temporalContext");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "newSynonyms");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "oldSynonyms");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "conceptSpeciesMap");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "hierarchy");
        ITemporalContext actualTemporalContext = (ITemporalContext)mv.getModel().get("temporalContext");
        
        int expectedCount = temporalContext.countAllConcepts();
        int actualCount = actualTemporalContext.countAllConcepts();
        Assert.assertEquals("Number of concepts must be equal", expectedCount , actualCount);
        ModelAndViewAssert.assertModelAttributeValue(mv, "date", now);
        ITaxonConcept actualCurrentConcept = (ITaxonConcept)mv.getModel().get("currentConcept");
        Assert.assertEquals("Retrieved currentConcept must match", expectedConcept.getId(), actualCurrentConcept.getId());
        @SuppressWarnings("unchecked")
        List<ITaxonConcept> actualNewSynonyms = (List<ITaxonConcept>)mv.getModel().get("newSynonyms"); 
        ITaxonConcept expectedNewConcept = temporalContext.selectConcept(MaxImporter.MAX_SOURCE, NEW_NAME_ID);
        Assert.assertEquals("New Synonym Count should be equal", 1, actualNewSynonyms.size());
        ITaxonConcept actualNewConcept = actualNewSynonyms.get(0);
        Assert.assertEquals("New Synonym should have same id", expectedNewConcept.getId(), actualNewConcept.getId());
    }
    @Test
    public void testTaxonLibVisChildren() throws Exception {

        request.setMethod("GET");
        request.setRequestURI(BDRSFieldGuideController.FIELDGUIDE_TAXON_URL);

        ITaxonConcept expectedConcept = temporalContext.selectConcept(MaxImporter.MAX_SOURCE, TAXON_WITH_CHILDREN);
        IndicatorSpecies expectedIndicatorSpecies = taxaDAO.getIndicatorSpeciesBySourceDataID(null, MaxImporter.MAX_SOURCE, expectedConcept.getName().getId().toString());
        
        request.setParameter("id", expectedIndicatorSpecies.getId().toString());
        request.setParameter("timestamp", String.valueOf(now.getTime()));
        ModelAndView mv = handle(request,response);
        ITaxonConcept actualConcept = (ITaxonConcept)mv.getModel().get("currentConcept");
        Assert.assertEquals("Number of children must be equal", 12, actualConcept.getChildren().size());
    }
}
