package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;

import java.util.*;

public class SyncResponse {

    // [{ Class: { klass: ..., id: ..., server_id: ...}}, ... ]
    private List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();

    // { Class : { clientId: server instance }}
    private Map<Class<?>, Map<String, PersistentImpl>> clientIdLookup = new HashMap<Class<?>, Map<String, PersistentImpl>>();

    public void add(Class<?> klazz, String clientId, PersistentImpl pi) {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("id", clientId);
        map.put("server_id", pi.getId());
        map.put("klass", klazz.getSimpleName());
        response.add(Collections.unmodifiableMap(map));

        Map<String, PersistentImpl> lookup = clientIdLookup.get(klazz);
        if(lookup == null) {
            lookup = new HashMap<String, PersistentImpl>();
            clientIdLookup.put(klazz, lookup);
        }
        lookup.put(clientId, pi);
    }

    public List<Map<String, Object>> getResponse() {
        return Collections.unmodifiableList(response);
    }

    public PersistentImpl getPersistentForClientId(Class<?> klazz, String clientId) {
        Map<String, PersistentImpl> lookup = this.clientIdLookup.get(klazz);
        return lookup == null ? null : lookup.get(clientId);
    }
}
