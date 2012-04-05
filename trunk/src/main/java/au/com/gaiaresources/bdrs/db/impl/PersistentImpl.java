package au.com.gaiaresources.bdrs.db.impl;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.search.annotations.Field;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.annotation.MobileField;
import au.com.gaiaresources.bdrs.annotation.NoThreshold;
import au.com.gaiaresources.bdrs.db.Persistent;
import au.com.gaiaresources.bdrs.serialization.DataInterchangeSerializable;

@MappedSuperclass
public abstract class PersistentImpl implements Persistent,
        DataInterchangeSerializable {
    public static final int DEFAULT_WEIGHT = 0;

    /**
     * The key name indicating the class of the flattened instance.
     */
    public static final String FLATTEN_KEY_CLASS = "_class";

    private Logger log = Logger.getLogger(getClass());

    @Field(index=org.hibernate.search.annotations.Index.UN_TOKENIZED)
    private Integer id;
    private Date createdAt;
    private Date updatedAt;
    private Integer createdBy;
    private Integer updatedBy;
    
    private boolean runThreshold = true;
    
    private Integer weight = DEFAULT_WEIGHT;

    /**
     * {@inheritDoc}
     */
    @Id
    @NoThreshold
    @MobileField(name = "server_id")
    @CompactAttribute
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PERSISTENT_ID")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @NoThreshold
    @Column(name = "CREATED_AT")
    @Override
    public Date getCreatedAt() {
        return createdAt != null ? new Date(createdAt.getTime()) : null;
    }

    protected void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt != null ? new Date(createdAt.getTime()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @NoThreshold
    @Column(name = "UPDATED_AT")
    @Override
    public Date getUpdatedAt() {
        return updatedAt != null ? new Date(updatedAt.getTime()) : null;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt != null ? (Date) updatedAt.clone() : null;
    }

    /**
     * {@inheritDoc}
     */
    @NoThreshold
    @Column(name = "CREATED_BY")
    @Override
    public Integer getCreatedBy() {
        return createdBy;
    }

    protected void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * {@inheritDoc}
     */
    @NoThreshold
    @Column(name = "UPDATED_BY")
    @Override
    public Integer getUpdatedBy() {
        return updatedBy;
    }

    protected void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Equals. Check if this and the other object are representations of the
     * same instance in the database. If <code>other</code> is of the same class
     * then compare IDs.
     * 
     * @param other
     *            The <code>Object</code> to compare to.
     * @return <code>boolean</code>.
     */
    public boolean equals(Object other) {
        if(other != null && (this.getClass().isAssignableFrom(other.getClass()) ||
                other.getClass().isAssignableFrom(this.getClass()))) {
            Persistent that = (Persistent) other;
            if ((this.getId() != null) && (that.getId() != null)) {
                return this.getId().equals(that.getId());
            } else {
                return super.equals(other);
            }
        }
        return false;
    }

    /**
     * Returns the hashCode of the class name concatenated with the id.
     * 
     * @return <code>int</code>.
     */
    public int hashCode() {
        return (getClass().getName() + getId()).hashCode();
    }

    @Override
    @Transient
    public Map<String, Object> flatten() {
        return this.flatten(0, false, false);
    }
    
    @Override
    @Transient
    public Map<String, Object> flatten(int depth) {
    	return this.flatten(depth, false, false);
    }
    
    @Override
    @Transient
    public Map<String, Object> flatten(boolean compact, boolean mobileFields) {
        return this.flatten(0, compact, mobileFields);
    }
    
    @Override
    @Transient
    public Map<String, Object> flatten(int depth, boolean compact, boolean mobileFields) {
    	return au.com.gaiaresources.bdrs.util.BeanUtils.flatten(PersistentImpl.class, this, depth, compact, mobileFields);
    }

    /**
     * {@inheritDoc}
     */
    @Column(name = "WEIGHT")
    @CompactAttribute
    public Integer getWeight() {
        return weight;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    @Transient
    public boolean isRunThreshold() {
        return runThreshold;
    }
    
    public void setRunThreshold(boolean value) {
        runThreshold = value;
    }
}
