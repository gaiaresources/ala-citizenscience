package au.com.gaiaresources.bdrs.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.annotation.MobileField;
import au.com.gaiaresources.bdrs.annotation.Sensitive;

/**
 * 
 * @author Tim Carpenter
 * 
 */
public final class BeanUtils {
	private BeanUtils() {
	}

	public static Object extractProperty(Object bean, String property)
			throws NoSuchFieldException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		if (property.indexOf('.') > 0) {
			int firstDot = property.indexOf('.');
			String firstProperty = property.substring(0, firstDot);
			Object o = extractProperty(bean, firstProperty);
			return extractProperty(o, property.substring(firstDot + 1));
		}
		Method m = ClassUtils.getGetterMethod(bean.getClass(), property);
		m.setAccessible(true);
		return m.invoke(bean);
	}

	public static Object injectProperty(Object bean, String property, Object arg)
			throws NoSuchFieldException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		String methodName = property.substring(0, 1).toUpperCase() + property.substring(1);
		
		Method[] methods = bean.getClass().getMethods();
		for (Method m : methods)
		{
			if (m.getName().equals("set" + methodName))
			{
				m.setAccessible(true);
				Object[] args = new Object[]{arg};
				//args[0] = arg;
				return m.invoke(bean, args);
			}
			
		}
		return null;
	}
	
    /**
     * The key name indicating the class of the flattened instance.
     */
    public static final String FLATTEN_KEY_CLASS = "_class";
	
	private static Logger log = Logger.getLogger(BeanUtils.class);
	
	public static Map<String, Object> flatten(Class<?> clazz, Object objToFlatten, int depth) {
		return flatten(clazz, objToFlatten, depth, false, false);
	}
	
	public static Map<String, Object> flatten(Class<?> clazz, Object objToFlatten, int depth, boolean compact, boolean mobileFields) {
    	Map<String, Object> map = new HashMap<String, Object>();
        try {
            Object value;
            
            Method readMethod;
            
            PropertyDescriptor[] descriptors = org.springframework.beans.BeanUtils.getPropertyDescriptors(objToFlatten.getClass());
            
            for (PropertyDescriptor pd : descriptors) {
                // Skip the attributes marked as sensitive.
                readMethod = pd.getReadMethod();
                String name;
                if (mobileFields) {
                	MobileField mf = readMethod != null ? readMethod.getAnnotation(MobileField.class) : null;
                	name = mf != null ? mf.name() : pd.getName();
                } else {
                	name = pd.getName();
                }
                if (readMethod != null
                        && readMethod.getAnnotation(Sensitive.class) == null
                        && (!compact || readMethod.getAnnotation(CompactAttribute.class) != null)) {

                    Class<?> returnType = readMethod.getReturnType();
                    value = readMethod.invoke(objToFlatten);
                    if (Iterable.class.isAssignableFrom(returnType)) {
                    	log.debug("flatten iterable : " + name);
                    	List<Object> list = new ArrayList<Object>();
                        if (value != null) {
                            Iterator<?> iterator = ((Iterable<?>) value).iterator();
                            Object raw;
                            while (iterator.hasNext()) {
                                raw = iterator.next();
                                if(raw == null) {
                                    list.add(null);
                                    
                                } else if (clazz.isAssignableFrom(raw.getClass())) {
                                    Object val;
                                    if(depth > 0) {
                                    	log.debug("recursive flatten iterable item : " + name);
                                        val = BeanUtils.flatten(clazz, raw, depth-1, compact, mobileFields);
                                    } else {
                                    	log.debug("depth = 0 flatten iterable item : " + name);
                                        val = raw != null ? raw.toString() : null;
                                    }
                                    list.add(val);
                                } else {
                                	log.debug("wrong class to flatten iterable object : " + raw.getClass());
                                    list.add(raw.toString());
                                }
                            }
                        }
                        map.put(name, list);
                        
                    } else if (String.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null : value.toString());
                    } else if (returnType.isArray()) {
                    	List<Object> list = new ArrayList<Object>();
                        if (value != null) {
                        	log.debug("flatten list type : " + name);
                            for (int i = 0; i < Array.getLength(value); i++) {
                                Object raw = Array.get(value, i);
                                if(raw == null) {
                                    list.add(null);
                                } else if (clazz.isAssignableFrom(raw.getClass())) {
                                	log.debug("a");
                                    Object val;
                                    if(depth > 0) {
                                    	log.debug("b");
                                        val = BeanUtils.flatten(clazz, raw, depth-1, compact, mobileFields);
                                    } else {
                                    	log.debug("c");
                                    	val = raw != null ? raw.toString() : null;
                                    }
                                    list.add(val);
                                } else {
                                	log.debug("wrong type to flatten list item");
                                    list.add(raw.toString());
                                }
                            }
                        }
                        map.put(name, list);
                    } else if (clazz.isAssignableFrom(returnType)) {
                    	log.debug("clazz assignable from return type");
                        Object val;
                        if(value == null) {
                        	log.debug("1 " + name);
                            val = null;
                        } else {
                            if(depth > 0) {
                            	log.debug("2 " + name);
                                val = BeanUtils.flatten(clazz, value, depth - 1, compact, mobileFields );
                            } else {
                            	log.debug("3 " + name);
                            	val = value != null ? value.toString() : null;
                            }
                        }
                        
                        map.put(name, val);
                    } else if (Integer.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Integer) value).intValue());
                    } else if (Long.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Long) value).longValue());
                    } else if (Date.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Date) value).getTime());
                    } else if (Byte.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Byte) value).byteValue());
                    } else if (Double.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Double) value).doubleValue());
                    } else if (Float.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Float) value).floatValue());
                    }  else if (Short.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Short) value).shortValue());
                    } else if (Boolean.class.isAssignableFrom(returnType)) {
                        map.put(name, value == null ? null
                                : ((Boolean) value).booleanValue());
                    } else if (returnType.isPrimitive()) {
                        map.put(name, value);
                    } else {
                    	log.debug("catch all go : " + name);
                        map.put(name, value == null ? null : value.toString());
                    }
                }
            }
        } catch (InvocationTargetException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }

        map.put(FLATTEN_KEY_CLASS, objToFlatten.getClass().getSimpleName());
        
        return map;
    }
}
