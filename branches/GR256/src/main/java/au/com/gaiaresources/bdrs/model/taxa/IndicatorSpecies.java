package au.com.gaiaresources.bdrs.model.taxa;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.attribute.Attributable;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.util.CollectionUtils;
import au.com.gaiaresources.taxonlib.TaxonLib;
import au.com.gaiaresources.taxonlib.model.ITaxonName;

import org.hibernate.annotations.*;
import org.hibernate.annotations.Index;
//import org.hibernate.annotations.Table;
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.*;

@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "INDICATOR_SPECIES")
@AttributeOverride(name = "id", column = @Column(name = "INDICATOR_SPECIES_ID"))
@Indexed
public class IndicatorSpecies extends PortalPersistentImpl implements Attributable<AttributeValue> {
    
    public static final String FIELD_SPECIES_NAME = "Field Species";
    
    private String scientificNameAndAuthor;
    private String scientificName;
    private String commonName;
    private TaxonGroup taxonGroup;
    private Set<Region> regions = new HashSet<Region>();
    private Set<String> regionNames = new HashSet<String>();
    private Set<AttributeValue> attributes = new HashSet<AttributeValue>();
    private List<SpeciesProfile> infoItems = new ArrayList<SpeciesProfile>();
    private IndicatorSpecies parent;
    private TaxonRank rank;
    private String author;
    private String year;
    private String source;
    private String sourceId;
    private Boolean current = true;
    private List<TaxonGroup> secondaryGroups = new ArrayList<TaxonGroup>();

    private Set<Metadata> metadata = new HashSet<Metadata>();
    // Cache of metadata mapped against the key. This is not a database 
    // column or relation.
    private Map<String, Metadata> metadataLookup = null;

    @CompactAttribute
    @CollectionOfElements(fetch = FetchType.LAZY)
    @JoinColumn(name = "INDICATOR_SPECIES_ID")
    @OrderBy("weight")
    @IndexedEmbedded
    public List<SpeciesProfile> getInfoItems() {
        return infoItems;
    }

    public void setInfoItems(List<SpeciesProfile> infoItems) {
        this.infoItems = infoItems;
    }

    @CompactAttribute
    @OneToMany
    @Override
    public Set<AttributeValue> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AttributeValue> attributes) {
        this.attributes = attributes;
    }
    
    @CompactAttribute
    @Column(name = "SCIENTIFIC_NAME_AND_AUTHOR", nullable = true)
    public String getScientificNameAndAuthor() {
        return scientificNameAndAuthor;
    }

    public void setScientificNameAndAuthor(String scientificNameAndAuthor) {
        this.scientificNameAndAuthor = scientificNameAndAuthor;
    }

    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "SCIENTIFIC_NAME", nullable = false)
    @Index(name="species_scientific_name_index")
    @Fields( {
        @Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.YES, analyzer=@Analyzer(definition=IndexingConstants.FULL_TEXT_ANALYZER)),
        @Field(name = "scientificName_sort", index = org.hibernate.search.annotations.Index.UN_TOKENIZED, store = Store.YES)
        } )
    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "COMMON_NAME", nullable = false)
    @Index(name="species_common_name_index")
    @Fields( {
        @Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.YES, analyzer=@Analyzer(definition=IndexingConstants.FULL_TEXT_ANALYZER)),
        @Field(name = "commonName_sort", index = org.hibernate.search.annotations.Index.UN_TOKENIZED, store = Store.YES)
        } )
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @ManyToOne
    @JoinColumn(name = "TAXON_GROUP_ID", nullable = false)
    @ForeignKey(name = "IND_SPECIES_TAXON_GROUP_FK")
    @IndexedEmbedded
    public TaxonGroup getTaxonGroup() {
        return taxonGroup;
    }

    public void setTaxonGroup(TaxonGroup taxonGroup) {
        this.taxonGroup = taxonGroup;
    }

    /**
     * {@inheritDoc}
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "INDICATOR_SPECIES_REGION", joinColumns = { @JoinColumn(name = "INDICATOR_SPECIES_ID") }, inverseJoinColumns = { @JoinColumn(name = "REGION_ID") })
    @ForeignKey(name = "INDICATOR_SPECIES_REGION_SPEICES_FK", inverseName = "INDICATOR_SPECIES_REGION_REGION_FK")
    // Hibernate FAIL.
    // https://forum.hibernate.org/viewtopic.php?f=1&t=1007658
    // https://forum.hibernate.org/viewtopic.php?f=1&t=998294
    // http://opensource.atlassian.com/projects/hibernate/browse/HHH-530?focusedCommentId=17890&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_17890
    //@Fetch(FetchMode.SUBSELECT)
    public Set<Region> getRegions() {
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = regions;
    }

    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    public IndicatorSpecies getParent() {
        return parent;
    }

    public void setParent(IndicatorSpecies parent) {
        this.parent = parent;
    }

    @CompactAttribute
    @Enumerated(EnumType.STRING)
    @Column(name = "RANK", nullable=true)
    public TaxonRank getTaxonRank() {
        return this.rank;
    }
    public void setTaxonRank(TaxonRank rank) {
        this.rank = rank;
    }

    @CompactAttribute
    @Column(name = "AUTHOR", nullable = true)
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    
    @CompactAttribute
    @Column(name = "YEAR", nullable = true)
    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }  
    
    /**
     * The data source of this taxon. For example, ALA, Max, NSW Flora
     * 
     * @return the name of the data source
     */
    @Column(name="source", nullable=true)
    @Lob
    @Index(name="indicator_species_source_index")
    public String getSource() {
        return source;
    }

    /**
     * The data source of this taxon. For example, ALA, Max, NSW Flora
     * 
     * @param source the name of the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * The identifier for this taxon. It should either be globally unique or
     * locally unique within the data source of this taxon, see get/setSource()
     * If the identifier is locally unique within the data source - it is highly
     * recommended that the source for this taxon is non null!
     * {@link TaxonLib} {@link ITaxonName} taxon_name_id (int) is stored as a String in this field if this
     * taxon comes from TaxonLib
     * 
     * @return identifier for this taxon
     */
    @Column(name="source_id", nullable=true)
    @Lob
    @Index(name="indicator_species_source_id_index")
    public String getSourceId() {
        return sourceId;
    }

    /**
     * The identifier for this taxon. It should either be globally unique or
     * locally unique within the data source of this taxon, see get/setSource()
     * If the identifier is locally unique within the data source - it is highly
     * recommended that the source for this taxon is non null!
     * 
     * @param sourceId identifier for this taxon
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    // Many to many is a work around (read hack) to prevent a unique
    // constraint being applied on the metadata id.
    /**
     * Sets the Metadata associated with this IndicatorSpecies
     * 
     * @return Metadata associated with this IndicatorSpecies
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    public Set<Metadata> getMetadata() {
        return metadata;
    }

    /**
     * Sets the Metadata associated with this IndicatorSpecies
     * 
     * @param metadata Metadata associated with this IndicatorSpecies.
     */
    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Gets a Metadata for this species by key
     * 
     * @param key key used to search for Metadata
     * @return Metadata with the requested key if it exists, null otherwise
     */
    @Transient
    public Metadata getMetadataByKey(String key) {
        if(key == null) {
            return null;
        }
        
        // If it has not been initialised or the metadata has since been changed,
        if(metadataLookup == null || (metadataLookup!= null && metadataLookup.size() != metadata.size())) {
            metadataLookup = new HashMap<String, Metadata>(metadata.size());
            for(Metadata md : metadata) {
                metadataLookup.put(md.getKey(), md);
            }    
        }
        
        return metadataLookup.get(key);
    }

    @Transient
    public Set<String> getRegionNames() {
        // Lazily load the names...
        if (this.regionNames == null) {
            this.regionNames = new HashSet<String>();
            for (Region r : CollectionUtils.nullSafeFor(this.getRegions())) {
                regionNames.add(r.getRegionName());
            }
        }
        return regionNames;
    }

    @Override
    @Transient
    public AttributeValue createAttribute() {
        return new AttributeValue();
    }
    
    @Column(name = "is_current", nullable = false)
	public Boolean getCurrent() {
		return current;
	}

	public void setCurrent(Boolean current) {
		this.current = current;
	}

    /**
     * Returns the secondary TaxonGroups defined for this taxon.
     * The TaxonGroup lifecycle is independent of the IndicatorSpecies so no operations cascade on this
     * relationship and TaxonGroups must be persistent before being added to this Set.
     * Secondary taxon groups are used to present alternative classifications of species in the field guide.
     * @return a Set of TaxonGroups.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @IndexedEmbedded
    public List<TaxonGroup> getSecondaryGroups() {
        return secondaryGroups;
    }

    /**
     * Sets the secondary TaxonGroups of this IndicatorSpecies.  This is intended for use by Hibernate,
     * use addSecondaryGroup instead.
     * @param secondaryGroups the secondary groups this IndicatorSpecies belong to.
     */
    void setSecondaryGroups(List<TaxonGroup> secondaryGroups) {
        this.secondaryGroups = secondaryGroups;
    }


    /**
     * Marks this IndicatorSpecies as being associated with the supplied TaxonGroup.  The TaxonGroup must
     * be associated with the Hibernate Session before this method is invoked.
     * @param taxonGroup the TaxonGroup to add this IndicatorSpecies to.
     */
    public void addSecondaryGroup(TaxonGroup taxonGroup) {
        this.secondaryGroups.add(taxonGroup);
    }
    
}
