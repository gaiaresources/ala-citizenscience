package au.com.gaiaresources.bdrs.model.map;

/**
 * Enum that defines a source for a map base layer.
 * 
 * @author stephanie
 */
public enum BaseMapLayerSource implements MapLayerSource, Comparable<BaseMapLayerSource> {
    G_PHYSICAL_MAP("Google Physical"),
    G_NORMAL_MAP("Google Streets"),
    G_HYBRID_MAP("Google Hybrid"),
    G_SATELLITE_MAP("Google Satellite"),
    OSM("Open Street Maps");
    
    private String name;
    
    BaseMapLayerSource(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
}
