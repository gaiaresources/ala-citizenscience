package au.com.gaiaresources.bdrs.model.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.annotations.Indexed;
import org.reflections.Reflections;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Utility class for common Indexing helper methods.
 * 
 * @author stephanie
 *
 */
public class IndexUtil {

    /**
     * Gets a set of classes that can be indexed.  Classes that can be indexed 
     * will be part of the model package and have an @Indexed annotation.
     */
    public static Set<Class<?>> getIndexedClasses() {
        // find every class in the model package with @Indexed annotation
        Reflections ref = new Reflections("au.com.gaiaresources.bdrs.model");
        return ref.getTypesAnnotatedWith(Indexed.class);
    }

    /**
     * Gets a mapping of simple class names to full class names.
     */
    public static Map<String, String> getFullNamesForIndexedClasses(
            String[] indexClasses) {
        Map<String, String> fullNames = new HashMap<String, String>();
        Set<Class<?>> classes = getIndexedClasses();
        List<String> shortNames = indexClasses == null ? null : Arrays.asList(indexClasses);
        for (Class<?> clazz : classes) {
            if (shortNames == null || shortNames.contains(clazz.getSimpleName())) {
                fullNames.put(clazz.getSimpleName(), clazz.getCanonicalName());
            }
        }
        return fullNames;
    }
}
