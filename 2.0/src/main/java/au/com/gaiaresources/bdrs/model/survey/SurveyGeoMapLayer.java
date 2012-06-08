package au.com.gaiaresources.bdrs.model.survey;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.MapLayer;
import au.com.gaiaresources.bdrs.model.map.MapLayerSource;

/**
 * Provides a link between a {@link Survey} and a {@link GeoMapLayer} for storing 
 * survey-specific map settings.
 * 
 * @author stephanie
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "SURVEY_GEO_MAP_LAYER")
@AttributeOverride(name = "id", column = @Column(name = "SURVEY_GEO_MAP_LAYER_ID"))
public class SurveyGeoMapLayer extends PortalPersistentImpl implements MapLayer {
    /**
     * The {@link Survey} to save the layer settings for
     */
    private Survey survey;
    /**
     * The {@link GeoMapLayer} to link to the {@link Survey}
     */
    private GeoMapLayer layer;
    
    /**
     * Boolean flag indicating whether or not this layer should be shown on survey maps.
     */
    private boolean showOnSurveyMap = false;
    
    /**
     * Default constructor for linking a {@link Survey} and {@link GeoMapLayer}
     */
    public SurveyGeoMapLayer() {
        super();
    }
    
    /**
     * Constructor linking the given {@link Survey} and {@link GeoMapLayer}
     * @param survey
     * @param layer
     */
    public SurveyGeoMapLayer(Survey survey, GeoMapLayer layer) {
        super();
        this.survey = survey;
        this.layer = layer;
    }
    /**
     * The {@link Survey} to save the layer settings for
     * @return the survey
     */
    @CompactAttribute
    @OneToOne()
    @IndexColumn(name="SURVEY_ID")
    public Survey getSurvey() {
        return survey;
    }
    /**
     * @param survey the survey to set
     */
    public void setSurvey(Survey survey) {
        this.survey = survey;
    }
    /**
     * The {@link GeoMapLayer} to link to the {@link Survey}
     * @return the layer
     */
    @CompactAttribute
    @OneToOne()
    @IndexColumn(name="GEO_MAP_LAYER_ID")
    public GeoMapLayer getLayer() {
        return layer;
    }
    /**
     * @param layer the layer to set
     */
    public void setLayer(GeoMapLayer layer) {
        this.layer = layer;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SurveyGeoMapLayer) {
            SurveyGeoMapLayer that = (SurveyGeoMapLayer) other;
            return this.getSurvey().equals(that.getSurvey()) && this.getLayer().equals(that.getLayer());
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return survey.hashCode() + layer.hashCode();
    }
    

    public void setShowOnMap(boolean showOnMap) {
        this.showOnSurveyMap = showOnMap;
    }
    
    @Transient
    public boolean getShowOnMap() {
        return showOnSurveyMap;
    }

    @Override
    @Transient
    public MapLayerSource getLayerSource() {
        return layer.getLayerSource();
    }

    @Override
    public void setLayerSource(MapLayerSource source) {
        layer.setLayerSource(source);
    }

    @Override
    public int compareTo(MapLayer other) {
        // compare the weights
        int compareVal = this.getWeight().compareTo(((SurveyGeoMapLayer)other).getWeight());
        // if they are equal, use the names for sorting
        if (compareVal == 0) {
            compareVal = this.getLayer().compareTo(((SurveyGeoMapLayer)other).getLayer());
        } else {
         // if one of the weights is 0, that value always comes last
            if (this.getWeight() == 0) {
                compareVal = 1;
            } else if (((SurveyGeoMapLayer)other).getWeight() == 0) {
                compareVal = -1;
            }
        }
        return compareVal;
    }
}
