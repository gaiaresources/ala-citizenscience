package au.com.gaiaresources.bdrs.model.taxa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.annotation.NoThreshold;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.util.StringUtils;

/**
 * Defines an attribute for in a taxon group.
 * @author Tim Carpenter
 *
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "ATTRIBUTE")
@AttributeOverride(name = "id", column = @Column(name = "ATTRIBUTE_ID"))
public class Attribute extends PortalPersistentImpl {
    
    public static final String FIELD_NAME_NAME = "FIELD_NAME";
    public static final String FIELD_NAME_DESC = "Field Name";
    
    private String typeCode;
    private boolean required;
    private String name;
    private String description;
    private boolean tag = false;
    private List<AttributeOption> options = new ArrayList<AttributeOption>();
    private AttributeScope scope;

    /** Configures the contexts in which this Attribute is visible */
    private AttributeVisibility visibility = AttributeVisibility.ALWAYS;
    /** Census method for census method attribute types */
    private CensusMethod censusMethod;
    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "DESCRIPTION", nullable = true, columnDefinition="TEXT")
    public String getDescription() {
        return description;
    }
    public void setDescription(String desc) {
        this.description = desc;
    }
    
    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "REQUIRED", nullable = false)
    public boolean isRequired() {
        return required;
    }
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "TYPE_CODE", nullable = false)
    public String getTypeCode() {
        return typeCode;
    }
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    @Transient
    public String getPropertyName() {
        return StringUtils.removeNonAlphaNumerics(getName()).toLowerCase();
    }

    @Transient
    public AttributeType getType() {
        return AttributeType.find(getTypeCode(), AttributeType.values());
    }

    @CompactAttribute
    @CollectionOfElements(fetch = FetchType.LAZY)
    @JoinColumn(name = "ATTRIBUTE_ID")
    @IndexColumn(name = "pos")
    public List<AttributeOption> getOptions() {
        return options;
    }
    public void setOptions(List<AttributeOption> options) {
        this.options = options;
    }

    /**
     * @return The options as a comma separated String.
     */
    @Transient
    public String getOptionString() {
        return getOptionString(",");
    }
    
    /**
     * @return The options as a comma separated String.
     */
    @Transient
    public String getOptionString(String delimiter) {
        if(this.options != null && !this.options.isEmpty()) {
            return StringUtils.joinList(this.options, delimiter);
        } else  {
            return "";
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @CompactAttribute
    @Column(name = "TAG", nullable = false)
    public boolean isTag() {
        return tag;
    }
    public void setTag(boolean tag) {
        this.tag = tag;
    }

    /**
     * Note: null attribute scope should default to SURVEY scope. This is the responsibility
     * of consumers of Attribute objects.
     * 
     * @return
     */
    @CompactAttribute
    @Enumerated(EnumType.STRING)
    @Column(name = "SCOPE", nullable=true)
    public AttributeScope getScope() {
        return this.scope;
    }
    public void setScope(AttributeScope scope) {
        this.scope = scope;
    }

    /**
     * This property defines the contexts in which this Attribute should be visible.
     * The valid values are defined by the AttributeVisibility enum and are:
     * <ul>
     *     <li>ALWAYS - the attribute is always visible if the normal access rules allows it.</li>
     *     <li>READ - the attribute is only visible when the Record is being viewed in read only mode.</li>
     *     <li>EDIT - the attribute is only visible while the Record is being edited (or created)</li>
     * </ul>
     * Please note that this setting does not override existing access rules such as the Record visibility and
     * moderation based visibility rules.
     * @return the current AttributeVisibility configured for this attribute.
     */
    @CompactAttribute
    @Enumerated(EnumType.STRING)
    @Column(name = "VISIBILITY", nullable=false)
    public AttributeVisibility getVisibility() {
        return visibility;
    }

    /**
     * Configures the visibility of this Attribute.  See getVisibility for a full description of this property.
     * @param visibility the desired AttributeVisibility for this Attribute.
     */
    public void setVisibility(AttributeVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns true if this Attribute should be visible in the supplied DisplayContext.  This is a convenience
     * method which delegates to AttributeVisibility.isVisible()
     * @param context the context to check the visibility in.
     * @return true if this Attribute should be visible, false otherwise.
     */
    @Transient
    public boolean isVisible(DisplayContext context) {
        return visibility.isVisible(context);
    }
    
    /**
     * @return the censusMethod
     */
    @CompactAttribute
    @OneToOne
    @NoThreshold
    public CensusMethod getCensusMethod() {
        return censusMethod;
    }
    /**
     * @param censusMethod the censusMethod to set
     */
    public void setCensusMethod(CensusMethod censusMethod) {
        this.censusMethod = censusMethod;
    }
}