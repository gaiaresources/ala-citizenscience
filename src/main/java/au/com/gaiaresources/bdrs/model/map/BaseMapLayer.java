package au.com.gaiaresources.bdrs.model.map;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyGeoMapLayer;

/**
 * Represents a layer to use as the base layer for record entry maps.
 * 
 * @author stephanie
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "BASE_MAP_LAYER")
@AttributeOverride(name = "id", column = @Column(name = "BASE_MAP_LAYER_ID"))
public class BaseMapLayer extends PortalPersistentImpl implements MapLayer {
    /**
     * The {@link Survey} to apply the map settings to.
     */
    private Survey survey = null;
    /**
     * The {@link BaseMapLayerSource} for the base layer
     */
    private BaseMapLayerSource layerSrc = null;
    /**
     * Boolean flag indicating if the base map is the one shown on the map by default.
     */
    private boolean isDefault;
    /**
     * Boolean flag indicating if the layer is "selected", meaning whether or not it 
     * will show in the layer switcher
     */
    private boolean showOnMap;
    
    /**
     * Default constructor
     */
    public BaseMapLayer() {
        
    }
    
    /**
     * Constructor that sets the fields for the object
     * @param survey The survey the layer will be used for
     * @param layerSrc The {@link BaseMapLayerSource} that defines the source of the map images
     * @param isDefault boolean flag indicating if this one is shown by default
     * @param showOnMap boolean flag indicating if this one shows in the layer switcher
     */
    public BaseMapLayer(Survey survey, BaseMapLayerSource layerSrc, boolean isDefault, boolean showOnMap) {
        this.survey = survey;
        this.layerSrc = layerSrc;
        this.isDefault = isDefault;
        this.showOnMap = showOnMap;
    }
    
    /**
     * Constructor that sets the fields for the object.  Note that this calls 
     * {@code BaseMapLayer(Survey survey, BaseMapLayerSource layerSrc, boolean isDefault, boolean showOnMap)}
     * and sets isDefault and showOnMap to false
     * @param survey The survey the layer will be used for
     * @param layerSrc The {@link BaseMapLayerSource} that defines the source of the map images
     */
    public BaseMapLayer(Survey survey, BaseMapLayerSource layerSource) {
        this(survey, layerSource, false, false);
    }
    /**
     * Gets the {@link Survey} to apply the map settings to.
     */
    @ManyToOne
    @JoinColumn(name = "SURVEY_ID", nullable = false)
    @ForeignKey(name = "BASE_MAP_LAYER_TO_SURVEY_FK")
    public Survey getSurvey() {
        return survey;
    }
    /**
     * Sets the {@link Survey} to apply the map settings to.
     */
    public void setSurvey(Survey survey) {
        this.survey = survey;
    }
    
    /**
     * Get the {@link BaseMapLayerSource} for the layer
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "LAYER_SOURCE", nullable=true)
    public BaseMapLayerSource getLayerSource() {
        return this.layerSrc;
    }
    /**
     * Set the {@link BaseMapLayerSource} for the layer
     */
    public void setLayerSource(MapLayerSource value) {
        if (value instanceof BaseMapLayerSource) {
            this.layerSrc = (BaseMapLayerSource) value;
        }
    }
    /**
     * Set the Boolean flag indicating if the base map is the one shown on the map by default.
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    /**
     * Get the Boolean flag indicating if the base map is the one shown on the map by default.
     */
    @Column(name = "DEFAULT_LAYER", nullable=false)
    public boolean isDefault() {
        return isDefault;
    }
    /**
     * Set the Boolean flag indicating if the layer is "selected", meaning whether or not it 
     * will show in the layer switcher or not
     */
    public void setShowOnMap(boolean showOnMap) {
        this.showOnMap = showOnMap;
    }
    /**
     * Get the Boolean flag indicating if the layer is "selected", meaning whether or not it 
     * will show in the layer switcher or not
     */
    @Transient
    public boolean getShowOnMap() {
        return showOnMap;
    }
    
    @Override
    public int compareTo(MapLayer other) {
        if (other instanceof BaseMapLayer) {
            int compareVal = Integer.valueOf(this.getWeight()).compareTo(((BaseMapLayer)other).getWeight());
            // if they are equal, compare the layer sources
            if (compareVal == 0) {
                compareVal = this.getLayerSource().getName().compareTo(((BaseMapLayer)other).getLayerSource().getName());
            } else {
                // if one of the weights is 0, that value always comes last
                if (this.getWeight() == 0) {
                    compareVal = 1;
                } else if (((BaseMapLayer)other).getWeight() == 0) {
                    compareVal = -1;
                }
            }
            return compareVal;
        }
        // if it is not a BaseMapLayer, they cannot be compared and will just be considered equal
        return 0;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseMapLayer) || !super.equals(other)) {
            return false;
        }
        
        BaseMapLayer that = (BaseMapLayer) other;
        return this.getSurvey().equals(that.getSurvey()) && 
               this.getLayerSource().equals(that.getLayerSource()) && 
               this.getShowOnMap() == that.getShowOnMap() && 
               this.isDefault() == that.isDefault();
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() + survey.hashCode() + layerSrc.hashCode();
    }
}
