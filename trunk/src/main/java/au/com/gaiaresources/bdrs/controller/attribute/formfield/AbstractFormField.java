package au.com.gaiaresources.bdrs.controller.attribute.formfield;

public abstract class AbstractFormField  implements FormField  {

    private String prefix;
    private String category;
    
    public AbstractFormField(String prefix, String category) {
        this.prefix = prefix;
        this.category = category;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAttributeFormField() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyFormField() {
        return false;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    /**
         * {@inheritDoc}
         */
    @Override
    public boolean isDisplayFormField() {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField#getCategory()
     */
    @Override
    public String getCategory() {
        return category;
    }
}