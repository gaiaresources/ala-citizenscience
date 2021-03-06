package au.com.gaiaresources.bdrs.model.map;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;

@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "ASSIGNED_GEO_MAP_LAYER")
@AttributeOverride(name = "id", column = @Column(name = "ASSIGNED_GEO_MAP_LAYER_ID"))
public class AssignedGeoMapLayer extends PortalPersistentImpl implements MapLayer {

    private GeoMapLayer layer;
    private GeoMap map;
    private boolean visible = false;
    private Integer upperZoomLimit = null;
    private Integer lowerZoomLimit = null;
    private boolean showOnMap = false;
    
    // we will use the 'weight' member to record order

    /**
     * The GeoMapLayer this assigned layer represents
     */
    @ManyToOne
    @JoinColumn(name="GEO_MAP_LAYER_ID", nullable=false)
    @ForeignKey(name = "ASSIGNED_GEO_MAP_LAYER_TO_GEO_MAP_LAYER_FK")
    public GeoMapLayer getLayer() {
        return layer;
    }

    public void setLayer(GeoMapLayer layer) {
        this.layer = layer;
    }
    
    /**
     * The GeoMap this assigned layer belongs to
     * @return
     */
    @ManyToOne
    @JoinColumn(name="GEO_MAP_ID", nullable=true)
    @ForeignKey(name = "ASSIGNED_GEO_MAP_LAYER_TO_GEO_MAP_FK")
    public GeoMap getMap() {
        return map;
    }

    public void setMap(GeoMap map) {
        this.map = map;
    }
    
    /**
     * Whether the layer will be visible upon map loading
     * @return
     */
    @Column(name="visible", nullable=false)
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * The upper zoom limit that the map layer will appear
     * 
     * null means there is no upper limit
     * @return
     */
    @Column(name="upperZoomLimit", nullable=true)
    public Integer getUpperZoomLimit() {
        return upperZoomLimit;
    }

    public void setUpperZoomLimit(Integer upperZoomLimit) {
        this.upperZoomLimit = upperZoomLimit;
    }

    /**
     * The lower zoom limit that the map layer will appear
     * 
     * null means there is no lower limit
     * @return
     */
    @Column(name="lowerZoomLimit", nullable=true)
    public Integer getLowerZoomLimit() {
        return lowerZoomLimit;
    }

    public void setLowerZoomLimit(Integer lowerZoomLimit) {
        this.lowerZoomLimit = lowerZoomLimit;
    }
	
	/**
     * Sets the selected status of the layer.
     * @param showOnMap selected status of the layer.
     */
    public void setShowOnMap(boolean showOnMap) {
        this.showOnMap = showOnMap;
    }
    
    /**
     * Gets the selected status of the layer.
     * @return true if selected.
     */
    @Transient
    public boolean getShowOnMap() {
        return showOnMap;
    }

	@Override
	public int compareTo(MapLayer other) {
		// compare the weights
        int compareVal = this.getWeight().compareTo(((AssignedGeoMapLayer)other).getWeight());
        // if they are equal, use the names for sorting
        if (compareVal == 0) {
            compareVal = this.getLayer().compareTo(((AssignedGeoMapLayer)other).getLayer());
        } else {
         // if one of the weights is 0, that value always comes last
            if (this.getWeight() == 0) {
                compareVal = 1;
            } else if (((AssignedGeoMapLayer)other).getWeight() == 0) {
                compareVal = -1;
            }
        }
        return compareVal;
	}

	@Transient
	@Override
	public MapLayerSource getLayerSource() {
		return layer.getLayerSource();
	}

	@Transient
	@Override
	public void setLayerSource(MapLayerSource source) {
		layer.setLayerSource(source);
	}
}
