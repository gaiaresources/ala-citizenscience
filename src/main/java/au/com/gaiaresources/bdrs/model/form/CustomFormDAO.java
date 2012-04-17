package au.com.gaiaresources.bdrs.model.form;

import java.util.List;

/**
 * Performs all database access for <code>CustomForm</code>s
 */
public interface CustomFormDAO {

    /**
     * Returns all custom forms for the current portal.
     *
     * @return all custom forms for the current portal.
     */
    public List<CustomForm> getCustomForms();

    /**
     * Gets the custom form with the specified ID.
     *
     * @param formId the primary key of the custom form.
     * @return the custom form specified by the id or null if one does not exist.
     */
    public CustomForm getCustomForm(int formId);


    /**
     * Saves the specified custom form to the database.
     *
     * @param form the custom form to be saved.
     * @return the persisted instance.
     */
    public CustomForm save(CustomForm form);

    /**
     * Deletes the specified <code>CustomForm</code>.
     *
     * @param form the custom form to be deleted.
     */
    public void delete(CustomForm form);
}
