package au.com.gaiaresources.bdrs.model.taxa;

import au.com.gaiaresources.bdrs.controller.attribute.DisplayContext;

/**
 * An AttributeVisibility defines the contexts in which an Attribute is visible.
 * The allowed values are:
 * <ul>
 *     <li>ALWAYS - the attribute always visible if the normal access rules allows it.</li>
 *     <li>READ - the attribute is only visible when the Record is being viewed in read only mode.</li>
 *     <li>EDIT - the attribute is only visible while the Record is being edited (or created)</li>
 * </ul>
 */
public enum AttributeVisibility {
    ALWAYS("Always"), READ("Read"), EDIT("Edit");

    /** A description of this enum value.  Used when rendering select options associated with an AttributeVisibility */
    private String description;

    /**
     * Creates a new AttributeVisibility with the supplied description.
     *
     * @param description a description of the visibility.
     */
    private AttributeVisibility(String description) {
        this.description = description;
    }


    /**
     * @return a String description of this AttributeVisibility.
     */
    public String getDescription() {
        return description;
    }


    /**
     * Returns true if this Attribute should be visible in the supplied DisplayContext.
     * @param context the context to check the visibility in.
     * @return true if this Attribute should be visible, false otherwise.
     */
    public boolean isVisible(DisplayContext context) {
        boolean visible;
        switch (this) {
            case ALWAYS:
                visible = true;
                break;
            case READ:
                visible = (context == DisplayContext.VIEW);
                break;
            case EDIT:
                visible = (context == DisplayContext.EDIT || context == DisplayContext.CREATE);
                break;
            default:
                throw new IllegalStateException("Invalid state of the visibility property: "+this);
        }
        return visible;
    }
}
