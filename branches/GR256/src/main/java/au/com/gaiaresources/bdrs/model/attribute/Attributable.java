package au.com.gaiaresources.bdrs.model.attribute;

import java.util.Set;

import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;


/**
 * Interface that defines an object as Attributable or able to have attribute values.
 * 
 * @author stephanie
 */
public interface Attributable<T extends TypedAttributeValue> {
    /**
     * @return a {@link Set} of <code><T extends TypedAttributeValue></code>s
     */
    public Set<T> getAttributes();
    /**
     * @return an instance of <code><T extends TypedAttributeValue></code>
     */
    public T createAttribute();
    /**
     * Set the {@link Set} of <code><T extends TypedAttributeValue></code>s
     * @param attributes
     */
    public void setAttributes(Set<T> attributes);
    /**
     * Get the id of this Attributable
     * @return the id
     */
    public Integer getId();
}
