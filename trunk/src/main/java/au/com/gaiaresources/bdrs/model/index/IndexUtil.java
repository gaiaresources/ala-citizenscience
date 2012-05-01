package au.com.gaiaresources.bdrs.model.index;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.search.annotations.Indexed;
import org.reflections.Reflections;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Utility class for common Indexing helper methods.
 * 
 * @author stephanie
 *
 */
public class IndexUtil {
    
    private static Logger log = Logger.getLogger(IndexUtil.class);
    /**
     * Constant for the index directory property.
     */
    public static final String INDEX_DIRECTORY_PROPERTY = "hibernate.search.default.indexBase";

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
                fullNames.put(clazz.getSimpleName(), clazz.getName());
            }
        }
        return fullNames;
    }

    /**
     * Returns the most recent run date of all IndexSchedules for the specified class.
     * @param indexDAO dao for retrieving the IndexSchedules
     * @param class1   the class to retrieve schedules for
     * @return the most recent run date of IndexSchedules for the specified class
     */
    public static Date getLastIndexBuildTime(IndexScheduleDAO indexDAO,
            Class<? extends PersistentImpl> class1) {
        List<IndexSchedule> schedules = indexDAO.getIndexSchedulesForClass(class1);
        Date indexDate = null;
        if (schedules != null) {
            Date tempDate = new Date(0);
            for (IndexSchedule indexSchedule : schedules) {
                tempDate = indexSchedule.getLastRun();
                if (tempDate != null) {
                    if (tempDate.after(indexDate)) {
                        indexDate = tempDate;
                    }
                }
            }
        }
        
        // if there are no scheduled indexes or the last run time of the schedules
        // is null, check the index directory to see if it exists and use the last 
        // modified time of the index directory
        if (indexDate == null) {
            
            String indexDirProp = System.getProperty(INDEX_DIRECTORY_PROPERTY);
            if (StringUtils.hasLength(indexDirProp)) {
                File indexDir = new File(indexDirProp);
                if (indexDir.exists()) {
                    // the last index date will be the last modified time of the 
                    // indexing directory, or the most recently modified date of 
                    // all the files contained within the directory
                    indexDate = new Date(indexDir.lastModified());
                }
            }
        }
        
        return indexDate;
    }
}
