package au.com.gaiaresources.bdrs.service.facet.builder;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.service.facet.Facet;

/**
 * Provides handling for user configuration parameters that are common to all
 * facets.
 */
public abstract class AbstractFacetBuilder implements FacetBuilder {
    
    /**
     * Default facet configuration value for all subclasses.
     */
    public static final String DEFAULT_FACET_PREF_VALUE;

    static {
        try {
            JSONObject prefValue = new JSONObject();
            prefValue.put(Facet.JSON_ACTIVE_KEY, Facet.DEFAULT_ACTIVE_CONFIG);
            prefValue.put(Facet.JSON_WEIGHT_KEY, Facet.DEFAULT_WEIGHT_CONFIG);

            JSONArray configArray = new JSONArray();
            configArray.add(prefValue);

            DEFAULT_FACET_PREF_VALUE = configArray.toString();
        } catch (JSONException je) {
            // This is not possible;
            throw new IllegalArgumentException(je);
        }
    }

    private Class<? extends Facet> facetClass;
    
    /**
     * Creates a new instance of this class.
     * @param facetClass the class of facet that this builder shall create.
     */
    public AbstractFacetBuilder(Class<? extends Facet> facetClass) {
        this.facetClass = facetClass;
    }
    
    /**
     * Returns a list of descriptions of parameter values.
     * @return a list of descriptions of parameter values.
     */
    protected List<String> getFacetParameterDescription() {
        List<String> list = new ArrayList<String>();
        list.add(Facet.ACTIVE_CONFIG_DESCRIPTION);
        list.add(Facet.WEIGHT_CONFIG_DESCRIPTION);
        return list;
    }
    
    /**
     * Creates a description to populate the {@link Preference} object by combining
     * a passage of text describing the facet and a list of descriptions for each
     * parameter accepted by this facet.
     * 
     * @param content text describing the facet.
     * @param paramDescriptionList list of descriptions for each parameter 
     * accepted by this facet.
     * @return a description that is intended to populate the {@link Preference}.
     */
    protected String buildPreferenceDescription(String content, List<String> paramDescriptionList) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("<p>%s</p>", content));
        builder.append("<dl>");
        for(String paramDesc : paramDescriptionList) {
            builder.append(paramDesc);
        }
        builder.append("</dl>");
        return builder.toString();
    }
    
    @Override
    public Class<? extends Facet> getFacetClass() {
        return facetClass;
    }
    
    @Override
    public String getPreferenceKey() {
        return facetClass.getCanonicalName();
    }
    
    @Override
    public Preference getDefaultPreference(Portal portal, PreferenceCategory category) {
        Preference pref = new Preference();
        pref.setPortal(portal);
        pref.setPreferenceCategory(category);
        pref.setIsRequired(true);
        pref.setLocked(true);
        pref.setKey(getPreferenceKey());
        pref.setDescription(getPreferenceDescription());
        pref.setValue(DEFAULT_FACET_PREF_VALUE);
        return pref;
    }
}
