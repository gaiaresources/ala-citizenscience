package au.com.gaiaresources.bdrs.python.model;

import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;

/**
 * Represents a facade over the {@link MetadataDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyMetadataDAO extends AbstractPyDAO {
    private MetadataDAO metadataDAO;

    /**
     * Creates a new instance.
     *
     * @param metadataDAO retrieves related data.
     */
    public PyMetadataDAO(MetadataDAO metadataDAO) {
        this.metadataDAO = metadataDAO;
    }

    @Override
    public String getById(int pk) {
        return super.getById(metadataDAO, Metadata.class, pk);
    }
}
