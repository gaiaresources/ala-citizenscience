package au.com.gaiaresources.bdrs.service.survey.handler;

import au.com.gaiaresources.bdrs.model.location.LocationService;
import org.springframework.beans.BeanUtils;

/**
 * A basic implementation of an {@link au.com.gaiaresources.bdrs.service.survey.ImportHandler} that will generally
 * support all subclasses of {@link au.com.gaiaresources.bdrs.db.impl.PersistentImpl} if there are no attached files.
 */
public class SimpleImportHandler extends AbstractImportHandler {

    /**
     * The data type that gets decoded by this handler.
     */
    private Class<?> klazz;

    /**
     * Creates a new instance of this handler.
     *
     * @param locationService provides facilities to convert WKT strings to Geometry instances.
     * @param klazz           the datatype that shall be decoded by this handler.
     */
    public SimpleImportHandler(LocationService locationService, Class<?> klazz) {
        super(locationService);
        this.klazz = klazz;
    }

    @Override
    public String getRegistryKey() {
        return this.klazz.getSimpleName();
    }

    @Override
    protected Object createNewInstance() {
        return BeanUtils.instantiate(klazz);
    }
}
