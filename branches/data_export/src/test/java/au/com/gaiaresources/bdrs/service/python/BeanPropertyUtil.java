package au.com.gaiaresources.bdrs.service.python;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is specialised in retrieving <code>PropertyDescriptors</code> from Beans specifically working around
 * issues getting accessors for boolean types using the 'is' prefix instead of the 'get' prefix and getting the
 * appropriate readMethod for boolean types providing both an 'is' getting and a 'get' getter.
 *
 * https://jira.springsource.org/browse/SPR-8071
 */
public class BeanPropertyUtil {
    private Class<?> bean;
    private Map<String, PropertyDescriptor> pd_map;

    public BeanPropertyUtil(Class<?> bean) throws IntrospectionException {
        this.bean = bean;
        pd_map = new HashMap<String, PropertyDescriptor>();

        for(PropertyDescriptor pd : Introspector.getBeanInfo(bean).getPropertyDescriptors()) {
            String propertyName = pd.getName();

            if(Boolean.class.equals(pd.getPropertyType())) {

                Method readMethod = pd.getReadMethod();
                if(readMethod == null ||
                        readMethod.getAnnotation(Column.class) == null ||
                        readMethod.getAnnotation(JoinColumn.class) == null) {

                    pd.setReadMethod(new PropertyDescriptor(propertyName, bean).getReadMethod());
                }
            }

            if(!isTransient(pd)) {
                pd_map.put(propertyName, pd);
            }
        }
    }

    /**
     * True if the transient annotation is present, false otherwise.
     * @param pd provides the read method for retrieving the transient annotation.
     * @return true if this property is transient, false otherwise.
     */
    public boolean isTransient(PropertyDescriptor pd) {
        return pd.getReadMethod() == null || pd.getReadMethod().getAnnotation(Transient.class) != null;
    }

    /**
     * Returns the descriptor for the named property.
     * @param propertyName the name of the property to be described.
     * @return the descriptor for the named property.
     */
    public PropertyDescriptor getPropertyDescriptor(String propertyName) {
        return pd_map.get(propertyName);
    }
}