package au.com.gaiaresources.bdrs.model.content;

import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;

/**
 * Note we shouldn't be using ContentDAO for reading/writing content in the application.
 * {@link ContentService} handles the lazy initialisation of content so we should use the methods
 * provided there.
 * 
 */
public interface ContentDAO extends TransactionDAO {
    /**
     * Creates or updates a help item in the system.
     * @param sesh 
     * 
     * @param key
     *            The key for the help item
     * @param value
     *            The value returned
     * @return returns the saved help item object
     */
    public Content saveContent(Session sesh, String key, String value);

    /**
     * Creates a help item in the system.
     * @param sesh 
     * 
     * @param key
     *            The key for the help item
     * @param value
     *            The value returned
     * @return returns the saved help item object
     */
    public Content saveNewContent(Session sesh, String key, String value);
    
    public List<String> getAllKeys();

    /**
     * Gets a {@link List} of {@link Content} keys that contain the given string.
     * @param string The string to match the keys with
     * @return A {@link List} of {@link Content} keys that contain the given string.
     */
    public List<String> getKeysLike(String string);
    
    /**
     * Gets a {@link List} of {@link Content} keys that start with the given string.
     * @param string The string to match the beginning of keys with
     * @return A {@link List} of {@link Content} keys that start with the given string.
     */
    public List<String> getKeysStartingWith(String string);

    public String getContentValue(Session sesh, String key, Portal portal);

    /**
     * Gets the content for the key.
     * @param sesh
     * @param key
     * @return
     */
    public Content getContent(Session sesh, String key);
}
