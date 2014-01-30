package au.com.gaiaresources.bdrs.service.facet.option;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.db.impl.Predicate;

/**
 * Represents a parameter to the selection criteria represented by the
 * {@link au.com.gaiaresources.bdrs.service.facet.Facet}.
 */
public abstract class FacetOption {

    private String displayName;
    private String value;
    private Long count;
    private boolean selected;
    protected boolean visible = true;
    protected String id;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Creates a new <code>FacetOption</code>
     * @param displayName the name/heading to be displayed to the user.
     * @param value the value that will be applied if the option is selected.
     * @param count the number of records that this query will affect.
     * @param selected true if this option is applied, false otherwise.
     */
    public FacetOption(String displayName, String value, Long count,
            boolean selected) {
        this(displayName, value, value, count, selected);
    }

    /**
     * Creates a new <code>FacetOption</code>
     * @param displayName the name/heading to be displayed to the user.
     * @param value the value that will be applied if the option is selected.
     * @param id this will be used in the view to identify the input field.
     * @param count the number of records that this query will affect.
     * @param selected true if this option is applied, false otherwise.
     */
    public FacetOption(String displayName, String value, String id, Long count,
                       boolean selected) {
        super();
        this.displayName = displayName;
        this.value = value;
        this.id = id;
        this.count = count;
        this.selected = selected;
    }

    /**
     * Returns true if this option is currently selected (ticked), false otherwise.
     * @return true if this option is currently selected (ticked), false otherwise.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets if this option is currently selected (ticked).
     * @param selected true if this option is currently selected (ticked), false otherwise.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns the number of records that match the predicate represented by this option.
     * @return the number of records that match the predicate represented by this option.
     */
    public Long getCount() {
        return count;
    }

    /**
     * Sets the number of records that match the predicate represented by this option.
     * @param count the number of records that match the predicate represented by this option.
     */
    public void setCount(Long count) {
        this.count = count;
    }

    /**
     * Returns the human readable name of this option.
     * @return the human readable name of this option.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human readable name of this option.
     * @param displayName the human readable name of this option.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the value of this option. This is the value that gets passed
     * back by the browser if the option is selected.
     * @return the value of this option.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this option. This is the value that gets passed
     * back by the browser if the option is selected.
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Returns the predicate represented by this option that may be applied to a
     * query.
     * @return the predicate represented by this option that may be applied to a
     * query.
     */
    public abstract Predicate getPredicate();

    /**
     * Returns a query string for querying this option via a lucene-based indexed query.
     * @return
     */
    public String getIndexedQueryString() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getId() {
        return id != null ? id : getValue();
    }

    public void setId(String id) {
        this.id = id;
    }
}
