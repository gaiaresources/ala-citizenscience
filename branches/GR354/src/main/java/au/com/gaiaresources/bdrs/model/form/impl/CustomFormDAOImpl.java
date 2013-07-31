package au.com.gaiaresources.bdrs.model.form.impl;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Performs the retrieval of custom forms from the database.
 */
@Repository
public class CustomFormDAOImpl extends AbstractDAOImpl implements CustomFormDAO {

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.form.CustomFormDAO#delete(au.com.gaiaresources.bdrs.model.form.CustomForm)
     */
    @Override
    public void delete(CustomForm form) {
        List<Survey> surveyList = super.find("from Survey where customForm = ?", form);
        for(Survey s : surveyList) {
            s.setCustomForm(null);
            save(s);
        }
        getSession().delete(form);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.form.CustomFormDAO#getCustomForm(int)
     */
    @Override
    public CustomForm getCustomForm(int formId) {
        return super.getByID(CustomForm.class, formId);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.form.CustomFormDAO#getCustomForms()
     */
    @Override
    public List<CustomForm> getCustomForms() {
        return super.find("from CustomForm order by name");
    }

    /* (non-Javadoc)
    * @see au.com.gaiaresources.bdrs.model.form.CustomFormDAO#save(au.com.gaiaresources.bdrs.model.form.CustomForm)
    */
    @Override
    public CustomForm save(CustomForm form) {
        return super.save(form);
    }
}