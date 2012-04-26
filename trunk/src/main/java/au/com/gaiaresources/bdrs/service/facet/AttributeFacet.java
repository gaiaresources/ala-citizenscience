package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.facet.option.StringAttributeFacetOption;
import au.com.gaiaresources.bdrs.util.Pair;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Creates a {@link Facet} for showing records by attribute values.
 * @author stephanie
 */
public class AttributeFacet extends AbstractFacet {
    
    /**
     * The base name of the query parameter.
     */
    public static final String QUERY_PARAM_NAME = "attribute_%s";
    
    /**
     * The expected JSON key from user preferences indicating the 
     * name of the attribute to be queried in the predicate. 
     */
    public static final String JSON_ATTRIBUTE_NAME_KEY = "attributeName";
    
    private Logger log = Logger.getLogger(getClass());
    
    /**
     * The name of the attribute that this facet will filter by.
     */
    private String attributeName;
    
    private int facetIndex;
    
    /**
     * Creates an instance of this facet.
     * 
     * @param defaultDisplayName the default human readable name of this facet.
     * @param recordDAO used for retrieving the count of matching records.
     * @param parameterMap the map of query parameters from the browser.
     * @param user the user that is accessing the records.
     * @param userParams user configurable parameters provided in via the {@link Preference)}.
     */
    public AttributeFacet(String defaultDisplayName, FacetDAO recordDAO, Map<String, String[]> parameterMap, User user,
            JSONObject userParams, int facetIndex) {
        // The query param name being passed to the super constructor here is
        // just a placeholder. We need to check if the 'attributeName' attribute
        // exists in the userParms. If it does not exist, the facet will be
        // deactivated.
        super(String.format(QUERY_PARAM_NAME, userParams.optString(JSON_ATTRIBUTE_NAME_KEY, "")), 
              userParams.optString(JSON_ATTRIBUTE_NAME_KEY, defaultDisplayName), userParams);
        
        if(userParams.has(JSON_ATTRIBUTE_NAME_KEY)) {
            this.attributeName = userParams.getString(JSON_ATTRIBUTE_NAME_KEY);
            this.facetIndex = facetIndex;
            
            String[] selectedOptions = processParameters(parameterMap);
            
            // for now this just handles String type attributes, 
            // later it should retrieve attribute objects vs count
            // and determine which type of attribute options to add 
            // based on the type of the attribute
            final int NO_LIMIT = 0;
            for(Pair<String, Long> pair : recordDAO.getDistinctAttributeValues(null, this.attributeName, NO_LIMIT)) {
                super.addFacetOption(new StringAttributeFacetOption(pair.getFirst(), pair.getSecond(), selectedOptions, facetIndex));
            }
        } else {
            // The JSON object is malformed.
            super.setActive(false);
            log.info(String.format("Deactivating the AttributeFacet because the JSON configuration is missing the \"%s\" attribute.", JSON_ATTRIBUTE_NAME_KEY));
        }
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.facet.AbstractFacet#getPredicate()
     */
    @Override
    public Predicate getPredicate() {
        Predicate facetPredicate = super.getPredicate();
        
        if(facetPredicate != null) {
            return Predicate.enclose(facetPredicate.and(
                                     Predicate.eq("attribute"+facetIndex+".description", this.attributeName)));
        } else {
            return null;
        }
    }

    public int getFacetIndex() {
        return facetIndex;
    }

    @Override
    public void applyCustomJoins(HqlQuery query) {
        super.applyCustomJoins(query);
        
        // attribute facet predicates create an additional join to the attributes/attribute 
        // tables to accomodate multiple attribute values
        query.leftJoin("record.attributes", "recordAttribute" + facetIndex);
        query.leftJoin("recordAttribute" + facetIndex + ".attribute", "attribute" + facetIndex);
    }
}
