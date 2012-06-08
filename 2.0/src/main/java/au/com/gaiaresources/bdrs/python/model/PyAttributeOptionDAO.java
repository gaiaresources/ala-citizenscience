package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOptionDAO;

/**
 * Represents a facade over the {@link AttributeOptionDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyAttributeOptionDAO extends AbstractPyDAO {
    private AttributeOptionDAO attributeOptionDAO;

    /**
     * Creates a new instance.
     *
     * @param attributeOptionDAO retrieves related data.
     */
    public PyAttributeOptionDAO(AttributeOptionDAO attributeOptionDAO) {
        this.attributeOptionDAO = attributeOptionDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(attributeOptionDAO, AttributeOption.class, pk);
    }
}
