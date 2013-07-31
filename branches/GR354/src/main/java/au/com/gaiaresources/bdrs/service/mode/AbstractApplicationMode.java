package au.com.gaiaresources.bdrs.service.mode;

import org.springframework.web.servlet.ModelAndView;

/**
 * An application mode represents a long running task that places the BDRS in a certain "mode"
 * such as "Maintenance Mode".
 */
public abstract class AbstractApplicationMode {

    /**
     * The specific type of this mode.
     */
    private ApplicationModeType modeType;

    /**
     * The time when this mode was created.
     */
    private long startTime;

    /**
     * Creates a new instance.
     * @param modeType the new mode of the application.
     * @param affectedRoles the array of user roles that will be affected by the activation of this mode.
     */
    public AbstractApplicationMode(ApplicationModeType modeType, String[] affectedRoles) {
        if(modeType == null) {
            throw new NullPointerException("Application mode type cannot be null.");
        }

        if(affectedRoles == null) {
            throw new NullPointerException("Application mode roles cannot be null");
        }

        if(affectedRoles.length == 0) {
            throw new IllegalArgumentException("Application mode must affect at least one role.");
        }


        this.modeType = modeType;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * @return the type of this mode.
     */
    public ApplicationModeType getModeType() {
        return modeType;
    }

    /**
     * @return the time when the mode was created.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Invoked by the interceptor before rendering a view.
     * @param modelAndView the view and model that shall be rendered.
     */
    public abstract void apply(ModelAndView modelAndView);
}
