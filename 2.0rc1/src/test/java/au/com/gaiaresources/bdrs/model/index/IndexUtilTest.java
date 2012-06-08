package au.com.gaiaresources.bdrs.model.index;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import edu.emory.mathcs.backport.java.util.Collections;

public class IndexUtilTest extends AbstractControllerTest {

    private static final Set<Class<?>> INDEXED_CLASSES;
    static {
        Set<Class<?>> tmp = new HashSet<Class<?>>(2);
        tmp.add(IndicatorSpecies.class);
        tmp.add(Location.class);
        
        INDEXED_CLASSES = Collections.unmodifiableSet(tmp);
    }
    
    
    @Test
    public final void testGetIndexedClasses() {
        Set<Class<?>> classes = IndexUtil.getIndexedClasses();
        Assert.assertEquals("Indexed classes collections are not the same size!", INDEXED_CLASSES.size(), classes.size());
        for (Class<?> clazz : classes) {
            Assert.assertTrue("Indexed classes does not contain class: "+clazz, INDEXED_CLASSES.contains(clazz));
        }
    }

}
