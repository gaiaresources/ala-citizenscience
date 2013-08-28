package au.com.gaiaresources.bdrs.model.map;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Type;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.util.SpatialUtil;

import com.vividsolutions.jts.geom.Point;

@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "GEO_MAP")
@AttributeOverride(name = "id", column = @Column(name = "GEO_MAP_ID"))
public class GeoMap extends PortalPersistentImpl {
    
    String name = "";
    String description = "";
    boolean hidePrivateDetails = true; // if any...
    // Will probably do this using hierarchical roles.
    String roleRequired = ""; // can be null / empty string
    boolean publish = false;
    boolean anonymousAccess = false;
    private MapOwner owner = null; // must set
    private Survey survey;
    private List<BaseMapLayer> baseMapLayers = new ArrayList<BaseMapLayer>();
    private List<AssignedGeoMapLayer> assignedGeoMapLayers = new ArrayList<AssignedGeoMapLayer>();
    private Point center;
    private Integer zoom;
    
    private BdrsCoordReferenceSystem crs = BdrsCoordReferenceSystem.WGS84;
    
    /**
     * Get the {@link BaseMapLayer} for this map
     * @return
     */
    @OneToMany(mappedBy="map", fetch = FetchType.LAZY)
    public List<BaseMapLayer> getBaseMapLayers() {
        return baseMapLayers;
    }

    /**
     * Set the {@link BaseMapLayer} for this map.
     * @param newLayers
     */
    public void setBaseMapLayers(List<BaseMapLayer> newLayers) {
        baseMapLayers = newLayers;
    }
    
    /**
     * Get the assigned layers for this map.
     * @return list of assigned layers.
     */
    @OneToMany(mappedBy="map", fetch = FetchType.LAZY)
    public List<AssignedGeoMapLayer> getAssignedLayers() {
    	return this.assignedGeoMapLayers;
    }
    
    /**
     * Set the assigned layers for this map.
     * @param assignedLayers list of layers to assign.
     */
    public void setAssignedLayers(List<AssignedGeoMapLayer> assignedLayers) {
    	assignedGeoMapLayers = assignedLayers;
    }
    
    /**
     * The name of this map
     * {@inheritDoc}
     */
    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * The description of this map
     * {@inheritDoc}
     */
    @Column(name = "DESCRIPTION", length=1023, nullable = false)
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * whether to hide private details on this map. Mainly user specific stuff.
     * Can increase the granularity of the privacy later if required
     * {@inheritDoc}
     */
    @Column(name = "HIDE_PRIVATE_DETAILS", nullable = false)
    public boolean isHidePrivateDetails() {
        return hidePrivateDetails;
    }
    public void setHidePrivateDetails(boolean hidePrivateDetails) {
        this.hidePrivateDetails = hidePrivateDetails;
    }
    
    /**
     * The minimum role level required to view this map. relies on 
     * hierarchical roles.
     * {@inheritDoc}
     */
    @Column(name = "ROLE_REQUIRED", nullable = false)
    public String getRoleRequired() {
        return roleRequired;
    }
    public void setRoleRequired(String roleRequired) {
        this.roleRequired = roleRequired;
    }
    
    /**
     * whether this map is published or not
     * {@inheritDoc}
     */
    @Column(name = "PUBLISH", nullable = false)
    public boolean isPublish() {
        return publish;
    }
    public void setPublish(boolean publish) {
        this.publish = publish;
    }    
    
    /**
     * Indicates whether the map can be seen without logging in
     */
    @Column(name = "ANONYMOUS_ACCESS", nullable = false) 
    public boolean isAnonymousAccess() {
        return this.anonymousAccess;
    }
    public void setAnonymousAccess(boolean value) {
        this.anonymousAccess = value;
    }
    
    /**
     * Get the owner of the map. 
     * @return the owner of the map.
     */
    @Column(name = "map_owner", nullable=false)
    @Enumerated(EnumType.STRING)
	public MapOwner getOwner() {
		return owner;
	}

    /**
     * Set the owner of the map.
     * @param owner the owner of the map.
     */
	public void setOwner(MapOwner owner) {
		this.owner = owner;
	}
	
	/**
	 * Get the survey this map belongs to. Can be null.
	 * @return survey for map.
	 */
    @ManyToOne
    @JoinColumn(name = "survey_id", nullable = true)
    @ForeignKey(name = "GEO_MAP_TO_SURVEY_FK")
	public Survey getSurvey() {
		return survey;
	}
	
	/**
	 * Sets the survey this map belongs to. Can be null.
	 * @param survey survey to set.
	 */
	public void setSurvey(Survey survey) {
		this.survey = survey;
	}

	/**
	 * Get the center of the map.
	 * @return Point object describing the center of the map.
	 */
    @Column(name = "center", nullable=true)
    @Type(type = "org.hibernatespatial.GeometryUserType")
	public Point getCenter() {
		return center;
	}

	/**
	 * Set the center of the map.
	 * @param center Point object describing the center of the map.
	 */
	public void setCenter(Point center) {
		this.center = center;
	}

	/**
	 * Get the zoom level for the map.
	 * @return Integer describing the zoom level for the map.
	 */
    @Column(name = "zoom", nullable=true)
	public Integer getZoom() {
		return zoom;
	}

	/**
	 * Set the zoom level for the map.
	 * @param zoom Integer describing the zoom level for the map.
	 */
	public void setZoom(Integer zoom) {
		this.zoom = zoom;
	}

	/**
     * Get the SRID for this map.
     * @return the SRID for this map.
     */
    @Transient
	public Integer getSrid() {
		return this.crs.getSrid();
	}
	
    /**
     * Get the CRS for this map.
     * @return CRS for this map.
     */
    @Column(name="crs", nullable=false)
    @Enumerated(EnumType.STRING)
	public BdrsCoordReferenceSystem getCrs() {
		return crs;
	}
    
    public void setCrs(BdrsCoordReferenceSystem crs) {
    	this.crs = crs;
    }
    
    @Transient
    public String getEpsgCode() {
    	return BdrsCoordReferenceSystem.sridToEpsg(getSrid());
    }
}
