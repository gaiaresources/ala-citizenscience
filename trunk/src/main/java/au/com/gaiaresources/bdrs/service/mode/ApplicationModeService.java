package au.com.gaiaresources.bdrs.service.mode;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service is the single web application wide mode lookup. At any given time the application may be in
 * a "mode" such as "maintenance mode". This service provides the single point of call to add, remove or query
 * the mode of the application.
 */
@Service
public class ApplicationModeService {
    /**
     * The repository of active modes keyed against their type.
     */
    private volatile Map<ApplicationModeType, List<AbstractApplicationMode>> activeModeLookup =
            new HashMap<ApplicationModeType, List<AbstractApplicationMode>>();

    /**
     * Creates a new instance.
     */
    public ApplicationModeService() {
        for(ApplicationModeType modeType : ApplicationModeType.values()) {
            activeModeLookup.put(modeType, new ArrayList<AbstractApplicationMode>());
        }
    }

    /**
     * Registers a new mode.
     * @param mode the new mode of the application.
     */
    public synchronized void addMode(AbstractApplicationMode mode) {
        if(mode == null) {
            return;
        }
        activeModeLookup.get(mode.getModeType()).add(mode);
    }

    /**
     * Removes a registered mode.
     * @param mode the mode to be removed.
     */
    public synchronized void removeMode(AbstractApplicationMode mode) {
        if(mode == null) {
            return;
        }
        activeModeLookup.get(mode.getModeType()).remove(mode);
    }

    /**
     * @param modeType the type of mode to test
     * @return returns true if the specified mode is active, false otherwise.
     */
    private synchronized boolean isModeActive(ApplicationModeType modeType) {
        if(modeType == null) {
            return false;
        }
        return !activeModeLookup.get(modeType).isEmpty();
    }

    // -----------------------------------------
    // Maintenance Mode Helpers
    // -----------------------------------------

    /**
     * @return true if the system is currently in maintenance mode, false otherwise.
     */
    public synchronized boolean isMaintenanceModeActive() {
        return isModeActive(ApplicationModeType.MAINTENANCE_MODE);
    }

    /**
     * @return the list of running maintenance modes.
     */
    public synchronized List<AbstractApplicationMode> getMaintenanceModeList() {
        return activeModeLookup.get(ApplicationModeType.MAINTENANCE_MODE);
    }
}
