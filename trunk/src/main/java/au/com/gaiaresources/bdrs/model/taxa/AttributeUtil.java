/**
 * 
 */
package au.com.gaiaresources.bdrs.model.taxa;

import au.com.gaiaresources.bdrs.model.user.User;

/**
 * @author stephanie
 *
 */
public class AttributeUtil {

    /**
     * Checks if the attribute is modifiable based on its scope and the user trying to modify it.
     * @param attribute the attribute in question
     * @param user the user trying to modify the attribute
     * @return true if the user can modify the attribute, false otherwise
     */
    public static boolean isModifiableByScopeAndUser(Attribute attribute, User user) {
        return (AttributeScope.isModerationScope(attribute.getScope()) && user.isModerator()) ||
                !AttributeScope.isModerationScope(attribute.getScope());
    }

    /**
     * Checks if the attribute is visible based on its scope, the user and if the value is empty or not.
     * @param attr the attribute in question
     * @param loggedInUser the user making the request
     * @param attrVal the value of the attribute in question
     * @return true if the user can view the attribute and it is not empty
     */
    public static boolean isVisibleByScopeAndUser(Attribute attr, User loggedInUser, TypedAttributeValue attrVal) {
        return AttributeScope.isModerationScope(attr.getScope()) && 
                ((loggedInUser != null && loggedInUser.isModerator()) || (attrVal != null && attrVal.isPopulated()));
    }

    /**
     * Checks if the attribute type is an HTML type or not
     * @param attribute the attribute to check
     * @return true if the type of the attribute is an HTML type, false otherwise
     */
    public static boolean isHTMLType(Attribute attribute) {
        AttributeType type = attribute.getType();
        return AttributeType.HTML.equals(type) || AttributeType.HTML_COMMENT.equals(type) ||
               AttributeType.HTML_HORIZONTAL_RULE.equals(type) || 
               AttributeType.HTML_NO_VALIDATION.equals(type);
    }
}
