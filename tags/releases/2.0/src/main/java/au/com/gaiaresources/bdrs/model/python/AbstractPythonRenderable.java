package au.com.gaiaresources.bdrs.model.python;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;

/**
 * Implementations of this class support a facility to be rendered using the Java to Python rendering framework.
 */
public abstract class AbstractPythonRenderable extends PortalPersistentImpl {
    /**
     * @return the name of the directory containing the renderable content.
     */
    public abstract String getContentDir();
}
