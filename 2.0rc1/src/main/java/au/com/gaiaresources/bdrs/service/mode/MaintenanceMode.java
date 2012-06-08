package au.com.gaiaresources.bdrs.service.mode;

import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * The system enters maintenance mode when application wide changes are being undertaken. All users
 * except the root user are prevented from using the application.
 */
public class MaintenanceMode extends AbstractApplicationMode {

    /**
     * The formatting of the starting datetime that is displayed in a message for the root user.
     */
    public static final String DATE_FORMAT_TMPL = "dd MMM yyyy HH:mm";

    /**
     * The list of roles that are affected by maintenance mode.
     */
    static final String[] AFFECTED_ROLES;
    static {
        // Affects all roles except for root
        AFFECTED_ROLES = new String[] {
            Role.ADMIN, Role.ANONYMOUS, Role.POWERUSER,  Role.SUPERVISOR, Role.USER,
        };
        Arrays.sort(AFFECTED_ROLES);
    }

    /**
     * Creates a new instance.
     */
    public MaintenanceMode() {
        super(ApplicationModeType.MAINTENANCE_MODE, AFFECTED_ROLES);
    }

    @Override
    public void apply(ModelAndView modelAndView) {
        RequestContext context = RequestContextHolder.getContext();
        String highestRole;
        if(context.isAuthenticated()) {
            highestRole = Role.getHighestRole(context.getRoles());
        } else {
            highestRole = Role.ANONYMOUS;
        }

        if(Arrays.binarySearch(AFFECTED_ROLES, highestRole) > -1) {
            modelAndView.addObject("disableMenu", true);
            modelAndView.addObject("disableContent", true);

            addMessage(context, getAffectedRoleMessageKey(), null);

        } else {
            DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_TMPL);
            String formattedDate = dateFormat.format(new Date(getStartTime()));

            addMessage(context, getRootRoleMessageKey(), new Object[]{ formattedDate });
        }
    }

    /**
     * Adds a new message to the request context, if the context does not already contain
     * a message with the same key.
     *
     * @param context the instance where session messages can be posted.
     * @param messageKey the message code to be added
     * @param params parameters accompanying the message
     */
    private void addMessage(RequestContext context, String messageKey, Object[] params) {
        if(!context.getMessageCodes().contains(messageKey)) {
            context.addMessage(messageKey, params);
        }
    }

    /**
     * @return the key for the message to be displayed to affected users.
     */
    protected String getAffectedRoleMessageKey() {
        return "bdrs.mode.maintenance.other";
    }

    /**
     * @return the key to be displayed to the root user.
     */
    protected String getRootRoleMessageKey() {
        return "bdrs.mode.maintenance.root";
    }
}
