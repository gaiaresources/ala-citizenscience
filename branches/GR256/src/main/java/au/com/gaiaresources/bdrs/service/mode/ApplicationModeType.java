package au.com.gaiaresources.bdrs.service.mode;

/**
 * The type of a particular application mode, e.g "Maintenance mode".
 */
public enum ApplicationModeType {
    /**
     * Application wide changes are being undertaken. All users except the root user are prevented from
     * using the system.
     */
    MAINTENANCE_MODE("Maintenance Mode");

    /**
     * The human readable name of this mode type.
     */
    private String name;

    /**
     * Creates a new instance.
     * @param name the human readable name of this mode type.
     */
    private ApplicationModeType(String name) {
        if(name == null) {
            throw new NullPointerException("Application mode type cannot have a null name");
        }
        this.name = name;
    }

    /**
     * @return the human readable name of this mode type.
     */
    public String getName() {
        return this.name;
    }
}
