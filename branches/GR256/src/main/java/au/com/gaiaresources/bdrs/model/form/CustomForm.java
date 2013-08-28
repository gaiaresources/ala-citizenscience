package au.com.gaiaresources.bdrs.model.form;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.python.AbstractPythonRenderable;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Describes a readonly view of the data in the system.
 */
@Entity
@FilterDef(name = PortalPersistentImpl.PORTAL_FILTER_NAME, parameters = @ParamDef(name = "portalId", type = "integer"))
@Filter(name = PortalPersistentImpl.PORTAL_FILTER_NAME, condition = ":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "CUSTOMFORM")
@AttributeOverride(name = "id", column = @Column(name = "CUSTOMFORM_ID"))
public class CustomForm extends AbstractPythonRenderable {
    /**
     * The target directory that will contain the form after extraction.
     */
    public static final String CUSTOM_FORM_DIR = "form";
    /**
     * The name of the custom form.
     */
    private String name;
    /**
     * The description of this form.
     */
    private String description;

    /**
     * Creates a new blank (and invalid) form.
     */
    public CustomForm() {
    }

    /**
     * Creates a new custom form.
     *
     * @param name the name of the new form.
     */
    public CustomForm(String name) {
        super();
        this.name = name;
    }

    /**
     * @return the name
     */
    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    @Column(name = "DESCRIPTION", nullable = false)
    @Lob
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The target directory that will contain the form after extraction.
     */
    @Transient
    @Override
    public String getContentDir() {
        return CustomForm.CUSTOM_FORM_DIR;
    }
}
