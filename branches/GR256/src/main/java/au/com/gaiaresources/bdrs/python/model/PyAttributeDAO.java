package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;

/**
 * Represents a facade over the {@link AttributeDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyAttributeDAO extends AbstractPyDAO {
    private AttributeDAO attributeDAO;

    /**
     * Creates a new instance.
     *
     * @param attributeDAO retrieves related data.
     */
    public PyAttributeDAO(AttributeDAO attributeDAO) {
        this.attributeDAO = attributeDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(attributeDAO, Attribute.class, pk);
    }
}
