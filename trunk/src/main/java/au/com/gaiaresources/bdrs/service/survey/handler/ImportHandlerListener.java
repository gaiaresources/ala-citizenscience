package au.com.gaiaresources.bdrs.service.survey.handler;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import org.hibernate.classic.Session;

import java.util.Map;

/**
 * To be implemented by {@link au.com.gaiaresources.bdrs.service.survey.ImportHandler} implementations that need to
 * receive callbacks during various stages of the decoding process.
 */
public interface ImportHandlerListener {

    /**
     * Invoke after a bean has been instantiated and populated but not yet saved.
     *
     * @param sesh             the session that shall be used to save the bean.
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved beans.
     * @param jsonPersistent   the encoded representation of the bean that will be saved.
     * @param bean             the bean that will be saved.
     */
    public void preSave(Session sesh, JSONObject importData, Map<Class, Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent, Object bean);

    /**
     * Invoked just after a bean has been saved.
     *
     * @param sesh             the session that has been used to save the bean.
     * @param importData       the registry of encoded data that has not yet been saved.
     * @param persistentLookup the registry of saved beans.
     * @param jsonPersistent   the encoded representation of the bean that has been saved.
     * @param bean             the bean that was saved.
     */
    public void postSave(Session sesh, JSONObject importData, Map<Class, Map<Integer, PersistentImpl>> persistentLookup, JSONObject jsonPersistent, Object bean);
}
