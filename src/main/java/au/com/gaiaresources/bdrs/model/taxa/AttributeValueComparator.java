package au.com.gaiaresources.bdrs.model.taxa;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Comparator implementation to help order AttributeValues in AttributeValueUtil
 * @author aaron
 *
 */
public class AttributeValueComparator implements Comparator<AttributeValue>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(AttributeValue arg0, AttributeValue arg1) {
        int weight0 = Integer.MAX_VALUE;
        if(arg0 != null && arg0.getAttribute() != null) {
            weight0 = arg0.getAttribute().getWeight();
        }

        int weight1 = Integer.MAX_VALUE;
        if(arg1 != null && arg1.getAttribute() != null) {
            weight1 = arg1.getAttribute().getWeight();
        }

        return weight0 - weight1;
    }
}
