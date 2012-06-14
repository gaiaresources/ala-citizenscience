package au.com.gaiaresources.bdrs.model.location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.attribute.Attributable;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * A User defined location for where they are collecting information.
 * @author Tim Carpenter
 *
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "LOCATION")
@AttributeOverride(name = "id", column = @Column(name = "LOCATION_ID"))
@Indexed
@AnalyzerDef(name = IndexingConstants.FULL_TEXT_ANALYZER,
             tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
             filters = {@TokenFilterDef(factory = LowerCaseFilterFactory.class),
                        @TokenFilterDef(factory = SnowballPorterFilterFactory.class, 
                                        params = {@Parameter(name="language", value="English")})
             })
public class Location extends PortalPersistentImpl implements Attributable<AttributeValue> {
    private Geometry location;
    private User user;
    private String name;
    private Set<Region> regions = new HashSet<Region>();
    private Set<AttributeValue> attributes = new HashSet<AttributeValue>();
    private String description;
    
    private Set<Metadata> metadata = new HashSet<Metadata>();
    
    private List<Survey> surveys = new ArrayList<Survey>();
    
    /**
     * Get the coordinate of the <code>Location</code>.
     * @return Java Topology Suite <code>Point</code>.
     */
    @CompactAttribute
    @Column(name = "LOCATION")
    @Type(type = "org.hibernatespatial.GeometryUserType")
    public Geometry getLocation() {
        return location;
    }
    public void setLocation(Geometry location) {
        this.location = location;
    }

    /**
     * Get the <code>User</code> that owns this <code>Location</code>.
     * @return <code>User</code>.
     */
    @CompactAttribute
    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = true)
    @ForeignKey(name = "LOCATION_USER_FK")
    @IndexedEmbedded
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Get the name of this <code>Location</code>.
     * @return <code>String</code>.
     */
    @CompactAttribute
    @Column(name = "NAME", nullable = false)
    @Fields( {
        @Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.YES, analyzer=@Analyzer(definition=IndexingConstants.FULL_TEXT_ANALYZER)),
        @Field(name = "name_sort", index = org.hibernate.search.annotations.Index.UN_TOKENIZED, store = Store.YES)
        } )
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description of this <code>Location</code>.
     * @return <code>String</code>.
     */
    @CompactAttribute
    @Column(name = "DESCRIPTION", nullable = true)
    @Lob  // makes a 'text' type in the database
    @Fields( {
        @Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.YES, analyzer=@Analyzer(definition=IndexingConstants.FULL_TEXT_ANALYZER)),
        @Field(name = "description_sort", index = org.hibernate.search.annotations.Index.UN_TOKENIZED, store = Store.YES)
        } )
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the regions that this location is within. Most of the time
     * there will only be one. But in the case of border locations
     * there might be more than one.
     * @return <code>Set</code> of <code>Region</code>.
     */
    @ManyToMany
    @JoinTable(name = "LOCATION_REGION",
               joinColumns = { @JoinColumn(name = "LOCATION_ID") },
               inverseJoinColumns = { @JoinColumn(name = "REGION_ID") })
    @ForeignKey(name = "LOCATION_REGION_LOC_FK",
                inverseName = "LOCATION_REGION_REGION_FK")
    @Fetch(FetchMode.JOIN)
    public Set<Region> getRegions() {
        // Changing to FetchMode.JOIN as FetchMode.SUBSELECT breaks when:
        // survey A has a record in it, no survey locations or user locations used.
        // survey B has a record in it that uses a user location. Survey B also
        // has survey locations.
        // Attempting to load the table view in the advanced review screen and selecting
        // viewing records for survey A. It causes a 500 error with the following exception: 
        // org.hibernate.exception.DataException: could not load collection by subselect: [au.com.gaiaresources.bdrs.model.location.Location.regions#<22, 28>]
        // 
        // where 22 and 28 are id's of locations used in survey B.
        // 
        // This may not be the minimum test case.
        return regions;
    }
    
    public void setRegions(Set<Region> regions) {
        this.regions = regions;
    }

    /**
     * Get the set of attributes that were recorded for the species.
     * @return {@link Set} of {@link RecordAttribute}
     */
    @CompactAttribute
    @OneToMany
    @Override
    @IndexedEmbedded
    public Set<AttributeValue> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<AttributeValue> attributes) {
        this.attributes = attributes;
    }
    
    @Transient
    public Point getPoint() {
        if (getLocation() == null) {
            return null;
        }
        return getLocation().getCentroid();
    }

    @Transient
    public Double getX() {
        if (getPoint() == null) {
            return null;
        }
        return getPoint().getX();
    }

    @Transient
    public Double getY() {
        if (getPoint() == null) {
            return null;
        }
        return getPoint().getY();
    }

    @Transient
    public Double getArea() {
        if (getLocation() == null) {
            return null;
        }
        return getLocation().getArea();
    }
    
    @Override
    @Transient
    public AttributeValue createAttribute() {
        return new AttributeValue();
    }
    
    // Many to many is a work around (read hack) to prevent a unique
    // constraint being applied on the metadata id.
    /**
     * Returns all metadata stored against this {@link Location}
     * @return all metdata stored against this {@link Location}
     */
    @ManyToMany
    @IndexedEmbedded
    public Set<Metadata> getMetadata() {
        return metadata;
    }

    /**
     * Updates all metadata stored against this {@link Location}
     * @param metadata the updated set of metadata about this {@link Location}
     */
    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Returns the value of the metadata with the specified key or null if a
     * matching metadata item cannot be found. 
     * 
     * @param key the key of the metadata item value to be returned.
     * @return the value of the metadata item with the specified key.
     */
    @Transient
    public String getMetadataValue(String key) {
        Metadata md = getMetadataForKey(key);
        return md == null ? null : md.getValue();
    }
    
    /**
     * Returns the metadata with the specified key or null if a
     * matching metadata item cannot be found. 
     * 
     * @param key the key of the metadata item to be returned.
     * @return the metadata item with the specified key.
     */
    @Transient
    public Metadata getMetadataForKey(String key) {
        if(key == null) {
            throw new NullPointerException();
        }

        for(Metadata md : this.getMetadata()) {
            if(md.getKey().equals(key)) {
                return md;
            }
        }
     return null;
    }
    
    /**
     * Returns a list of surveys which this location has been added to.
     * This is a bidirectional link between {@link Survey} and {@link Location}.
     * @return a list of surveys
     */
    @ManyToMany(mappedBy="locations", fetch = FetchType.LAZY)
    @IndexedEmbedded
    public List<Survey> getSurveys() {
        return this.surveys;
    }
    
    public void setSurveys(List<Survey> surveys) {
        this.surveys = surveys;
    }
    
    /**
     * Returns a list of the AttributeValues ordered by Attribute weight
     * @return
     */
    @Transient
    public List<AttributeValue> getOrderedAttributes() {
        return AttributeValueUtil.orderAttributeValues(attributes);
    }
    
    /**
     * Convenience method for getting the latitude of a Location.
     * @return the latitude of the location
     */
    @Transient
    public Double getLatitude() {
        if (this.getPoint() != null) {
            return this.getPoint().getY();
        } else if (getY() != null) {
            return getY();
        } else {
            return null;
        }
    }

    /**
     * Convenience method for getting the longitude of a Location.
     * @return the longitude of the location
     */
    @Transient
    public Double getLongitude() {
        if (this.getPoint() != null) {
            return this.getPoint().getX();
        } else if (getX() != null) {
            return getX();
        } else {
            return null;
        }
    }
}