package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;

/**
 * Represents a facade over the {@link au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyAttributeValueDAO extends AbstractPyDAO {
    private AttributeValueDAO attributeValueDAO;

    /**
     * Creates a new instance.
     *
     * @param attributeValueDAO retrieves related data.
     */
    public PyAttributeValueDAO(AttributeValueDAO attributeValueDAO) {
        this.attributeValueDAO = attributeValueDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(attributeValueDAO, AttributeValue.class, pk);
    }
}
