package au.com.gaiaresources.bdrs.model.location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
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
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.attribute.Attributable;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.user.User;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * A User defined location for where they are collecting information.
 * @author Tim Carpenter
 *
 */
@Entity

@FilterDefs({
    @FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" )),
    @FilterDef(name=FilterManager.LOCATION_USER_ACCESS_FILTER, parameters=@ParamDef( name=FilterManager.USER_ID, type="integer" ))
})

@Filters({
    @Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID"),
    @Filter(name=FilterManager.LOCATION_USER_ACCESS_FILTER, condition="(:userId = user_id or (user_id is null and (" +
                " location_id in (select sl1.locations_location_id from survey_location sl1 join survey_user_definition sud1 on sl1.survey_survey_id=sud1.survey_survey_id where sud1.users_user_definition_id = :userId) or" +
                " location_id in (select sl2.locations_location_id from survey_location sl2 join survey_usergroup sg2 on sl2.survey_survey_id=sg2.survey_survey_id join group_users gu2 on sg2.groups_group_id=gu2.usergroup_group_id where gu2.users_user_definition_id = :userId) or" +
                " location_id in (select sl3.locations_location_id from survey_location sl3 join survey surv3 on sl3.survey_survey_id=surv3.survey_id where surv3.public_read_access or surv3.public))))")
})
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
    
    /**
     * Get the set of attributes that were recorded for the location for the survey given.
     * Note that modifying this set will have no effect on persisted data
     * @return {@link Set} of {@link RecordAttribute}
     */
    @Transient
    public Set<AttributeValue> getAttributes(Survey survey) {
        Set<AttributeValue> surveyValues = new LinkedHashSet<AttributeValue>();
        // create an Attribute-indexed map of the location attributes
        // so we can get the attribute value by attribute later
        Map<Attribute, AttributeValue> typedAttrMap = new HashMap<Attribute, AttributeValue>();
        for (AttributeValue attr : getAttributes()) {
            typedAttrMap.put(attr.getAttribute(), attr);
        }
        for (Attribute attribute : survey.getAttributes()) {
            if (typedAttrMap.containsKey(attribute)) {
                surveyValues.add(typedAttrMap.get(attribute));
            }
        }
        return surveyValues;
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
    
    /**
     * Get the SRID for the geometry contained in this location.
     * If geometry is null this method will return null.
     * @return SRID for the contained geometry.
     */
    @Transient
    public Integer getSrid() {
    	return this.location != null ? this.location.getSRID() : null;
    }
}
