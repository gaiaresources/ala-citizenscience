package au.com.gaiaresources.bdrs.service.survey.handler;

import org.springframework.beans.BeanUtils;

import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * A basic implementation of an {@link au.com.gaiaresources.bdrs.service.survey.ImportHandler} that will generally
 * support all subclasses of {@link au.com.gaiaresources.bdrs.db.impl.PersistentImpl} if there are no attached files.
 */
public class SimpleImportHandler extends AbstractImportHandler {

    /**
     * The data type that gets decoded by this handler.
     */
    protected Class<?> klazz;

    /**
     * Creates a new instance of this handler.
     *
     * @param locationService provides facilities to convert WKT strings to Geometry instances.
     * @param klazz           the datatype that shall be decoded by this handler.
     */
    public SimpleImportHandler(SpatialUtilFactory spatialUtilFactory, Class<?> klazz) {
        super(spatialUtilFactory);
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
