package au.com.gaiaresources.bdrs.service.survey.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Handler for importing {@link User Users}. Will not import accounts that already exist.
 * 
 * @author stephanie
 *
 */
public class UserImportHandler extends SimpleImportHandler {
    
    private UserDAO userDAO;
    
    public UserImportHandler(SpatialUtilFactory spatialUtilFactory, UserDAO userDAO) {
        super(spatialUtilFactory, User.class);
        this.userDAO = userDAO;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.handler.AbstractImportHandler#importData(org.hibernate.Session, au.com.gaiaresources.bdrs.json.JSONObject, java.util.Map, au.com.gaiaresources.bdrs.json.JSONObject)
     */
    @Override
    public Object importData(Session sesh, JSONObject importData, Map<Class,
            Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {

        // Remove the representation from the registry of instances to be imported
        removeJSONPersistent(importData, jsonPersistent);
        Object bean = createBean(sesh, importData, persistentLookup, jsonPersistent);

        // check if the user already exists, if so, do not import it
        List<User> users = getUsers();
        if (!users.contains(bean)) {
            User newUser = removeInvalidRoles(bean);
            // Notify all listeners that we are about to save the instance.
            firePreSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newUser);

            // Save the instance and add it to the registry of saved data.
            sesh.save(newUser);
            addToPersistentLookup(persistentLookup, jsonPersistent, newUser);

            // Notify all listeners that the instance has been saved.
            firePostSaveEvent(sesh, importData, persistentLookup, jsonPersistent, newUser);
            
            return newUser;
        } else {
            messages.put("bdrs.user.import.duplicate."+messages.size(), new Object[]{ ((User) bean).getName() });
            return null;
        }
    }

    private User removeInvalidRoles(Object bean) {
        User currentUser = RequestContextHolder.getContext().getUser();
        User newUser = (User) bean;
        // remove any roles that are higher than the current user
        List<String> newRoles = new ArrayList<String>(newUser.getRoles().length);
        String highestRole = Role.getHighestRole(currentUser.getRoles());
        for (String role : newUser.getRoles()) {
            if (Role.isRoleHigherThanOrEqualTo(highestRole, role)) {
                newRoles.add(role);
            }
        }
        newUser.setRoles(newRoles.toArray(new String[newRoles.size()]));
        return newUser;
    }

    private List<User> getUsers() {
        if (userDAO == null) {
            return Collections.EMPTY_LIST;
        } else {
            return userDAO.getUsers();
        }
    }
}
