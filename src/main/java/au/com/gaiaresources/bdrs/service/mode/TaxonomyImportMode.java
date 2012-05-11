package au.com.gaiaresources.bdrs.service.mode;

/**
 * A special kind of maintenance mode that is displayed when large taxonomy sets such as Max are being loaded.
 */
public class TaxonomyImportMode extends MaintenanceMode {
    @Override
    protected String getRootRoleMessageKey() {
        return "bdrs.mode.maintenance.taxonomyImport.root";
    }
}
