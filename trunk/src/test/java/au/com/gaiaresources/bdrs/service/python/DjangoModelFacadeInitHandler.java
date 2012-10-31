package au.com.gaiaresources.bdrs.service.python;

import au.com.gaiaresources.bdrs.json.JSONObject;

/**
 *
 */
public interface DjangoModelFacadeInitHandler {
    public JSONObject getModelData(String djangoModelName) throws Exception;
}
