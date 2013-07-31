package au.com.gaiaresources.bdrs.service.user;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.AbstractImportExportService;
import au.com.gaiaresources.bdrs.service.survey.ImportHandlerRegistry;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Exports and imports user accounts from one BDRS to another.
 * This only includes Users and their roles.  No associated Records, Locations, 
 * or Metadata are exported/imported with user accounts.
 * 
 * @author stephanie
 *
 */
@Service
public class UserImportExportService extends AbstractImportExportService<User> {
    /**
     * Provides access to the BDRS users.
     */
    @Autowired
    private UserDAO userDAO;
    
    /* 
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.survey.ImportExportService#initService()
     */
    @Override
    @PostConstruct
    public void initService() {
        importHandlerRegistry = new ImportHandlerRegistry(new SpatialUtilFactory(), fileService, userDAO, geoMapService);
    }
    
    
    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.ImportExportService#exportObject(au.com.gaiaresources.bdrs.db.impl.PersistentImpl)
     */
    @Override
    public JSONObject exportObject(User user) {
        // { klazz : { id : JSONObject }}
        JSONObject exportData = new JSONObject();

        addToExport(exportData, user);
        addMetadataToExport(exportData, user.getMetadata());
        
        return exportData;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.service.AbstractImportExportService#addToExport(au.com.gaiaresources.bdrs.json.JSONObject, au.com.gaiaresources.bdrs.db.impl.PersistentImpl)
     */
    @Override
    protected void addToExport(JSONObject exportData, PersistentImpl persistent) {
        if (persistent == null) {
            return;
        }
        // this export needs to contain sensitive fields so passwords can be exported/imported
        this.addToExport(exportData, persistent, JSONObject.fromMapToJSONObject(persistent.flatten(false, false, true)));
    }
}
