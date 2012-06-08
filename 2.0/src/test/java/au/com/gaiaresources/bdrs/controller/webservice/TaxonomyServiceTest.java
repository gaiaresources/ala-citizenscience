package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Tests the TaxonomyService class.
 */
public class TaxonomyServiceTest extends AbstractGridControllerTest {



    /**
     * Tests that the web service is invoking the DAO and returning correctly formatted JSON.
     * Relies on IndicatorSpecies data being populated in the AbstractGridControllerTest.
     */
    @Test
    public void testSearchTaxaByTaxonGroupName() throws Exception{
        request.setMethod("GET");
        request.setRequestURI("/webservice/taxon/searchTaxaByTaxonGroupName");
        request.setParameter("groupName", "animus");
        request.setParameter("taxonName", "drop");

        handle(request, response);

        Assert.assertEquals("application/json", response.getContentType());
        JSONArray responseContent = JSONArray.fromString(response.getContentAsString());

        Assert.assertEquals(1, responseContent.size());
        JSONObject result = responseContent.getJSONObject(0);
        Assert.assertEquals("drop bear", result.get("commonName"));

    }


    /**
     * Tests the service requires both parameters.
     */
    @Test
    public void testSearchTaxaByTaxonGroupNameRequiredParameters() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/webservice/taxon/searchTaxaByTaxonGroupName");
        request.setParameter("groupName", "animus");

        try {
            handle(request, response);
            Assert.fail("An exception should have been thrown");
        }
        catch (MissingServletRequestParameterException e) {
            Assert.assertEquals("taxonName", e.getParameterName());
        }

        request.removeAllParameters();
        request.setParameter("taxonName", "drop");

        try {
            handle(request, response);
            Assert.fail("An exception should have been thrown");
        }
        catch (MissingServletRequestParameterException e) {
            Assert.assertEquals("groupName", e.getParameterName());
        }
    }
}
