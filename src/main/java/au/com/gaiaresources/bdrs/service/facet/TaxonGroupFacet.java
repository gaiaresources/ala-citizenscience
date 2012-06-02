package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.TaxonGroupFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Represents taxonomic records based on the {@link TaxonGroup} of the associated
 * @link {@link au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies}.
 */
public class TaxonGroupFacet extends AbstractFacet {
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "g";
    
    private Logger log = Logger.getLogger(getClass());
    
    private static final String SPECIES_ATTRIBUTE_GROUP_ALIAS = "taxonGroup_"+QUERY_PARAM_NAME;
    
    private static final String SPECIES_ALIAS = "species_" + QUERY_PARAM_NAME;
    
    private static final String AV_ALIAS = "av_" + QUERY_PARAM_NAME;

    /**
     * Creates a new instance.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link au.com.gaiaresources.bdrs.model.preference.Preference)}.
     */
    public TaxonGroupFacet(String defaultDisplayName, FacetDAO recordDAO, Map<String, String[]> parameterMap, User user, JSONObject userParams) {
        super(QUERY_PARAM_NAME, defaultDisplayName, userParams);
        String[] selectedOptions = processParameters(parameterMap);

        List<Pair<TaxonGroup, Long>> pairs = recordDAO.getDistinctTaxonGroups(null);
        for(Pair<TaxonGroup, Long> pair : pairs) {
            super.addFacetOption(new TaxonGroupFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions, SPECIES_ATTRIBUTE_GROUP_ALIAS));
        }
    }
    
    @Override
    public void applyCustomJoins(HqlQuery query) {
        super.applyCustomJoins(query);
        
        // attribute facet predicates create an additional join to the attributes/attribute 
        // tables to accomodate multiple attribute values
        query.leftJoin("record.attributes", AV_ALIAS);
        query.leftJoin(AV_ALIAS + ".species", SPECIES_ALIAS);
        query.leftJoin(SPECIES_ALIAS+".taxonGroup", SPECIES_ATTRIBUTE_GROUP_ALIAS);
    }
}
