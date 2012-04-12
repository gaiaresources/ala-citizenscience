package au.com.gaiaresources.bdrs.controller.admin;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import com.ibm.icu.util.Calendar;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.index.IndexSchedule;
import au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO;
import au.com.gaiaresources.bdrs.model.index.IndexType;
import au.com.gaiaresources.bdrs.model.index.IndexUtil;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.util.DateFormatter;

/**
 * @author stephanie
 *
 */
public class AdminDataIndexControllerTest extends AbstractControllerTest {

    @Autowired
    private IndexScheduleDAO indexScheduleDAO;
    
    @Test
    public final void testlistDataIndexSchedules() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(AdminDataIndexController.INDEX_SCHEDULE_LIST_URL);

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, AdminDataIndexController.INDEX_SCHEDULE_LIST_VIEW_NAME);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "indexSchedules");
    }
    
    @Test
    public final void testsaveDataIndexSchedules() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        // create two data index schedules so we can test that one is deleted
        Set<Class<?>> classes = IndexUtil.getIndexedClasses();
        IndexSchedule s = null;
        for (Class<?> class1 : classes) {
            s = new IndexSchedule(class1.getName(), IndexType.SERVER_STARTUP);
            indexScheduleDAO.save(s);
        }
        
        request.setMethod("POST");
        request.setRequestURI(AdminDataIndexController.INDEX_SCHEDULE_LIST_URL);
        
        List<IndexSchedule> schedules = indexScheduleDAO.getIndexSchedules();
        String[] values = new String[schedules.size()-1];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.valueOf(schedules.get(i).getId());
        }
        request.addParameter("index", values);

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, AdminDataIndexController.INDEX_SCHEDULE_LIST_VIEW_NAME);
        ModelAndViewAssert.assertCompareListModelAttribute(mv, "indexSchedules", indexScheduleDAO.getIndexSchedules());
    }

    @Test
    public final void testviewDataIndexSchedule() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        request.setMethod("GET");
        request.setRequestURI(AdminDataIndexController.INDEX_SCHEDULE_URL);

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, AdminDataIndexController.INDEX_SCHEDULE_VIEW_NAME);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, AdminDataIndexController.PARAM_INDEX_SCHEDULE);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "indexClasses");
    }

    @Test
    public final void testsaveDataIndexSchedule() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        Set<Class<?>> indexClasses = IndexUtil.getIndexedClasses();
        Class<?> clazz = indexClasses.iterator().next();
        
        for (IndexType indexType : IndexType.values()) {
            request.setMethod("POST");
            request.setRequestURI(AdminDataIndexController.INDEX_SCHEDULE_URL);
            request.addParameter(AdminDataIndexController.PARAM_DELETE_INDEX, "true");
            request.addParameter(AdminDataIndexController.PARAM_INDEX_TYPE, indexType.toString());
            request.addParameter("date", DateFormatter.format(new Date(), DateFormatter.DAY_MONTH_YEAR));
            request.addParameter("time", DateFormatter.format(new Date(), DateFormatter.TIME));
            request.addParameter("indexClass", clazz.getName());
            
            if (IndexType.WEEKLY.equals(indexType)) {
                request.addParameter("weeklyDay", String.valueOf(Calendar.MONDAY));
            }
            
            ModelAndView mv = handle(request, response);
            ModelAndViewAssert.assertViewName(mv, AdminDataIndexController.INDEX_SCHEDULE_LIST_VIEW_NAME);
            ModelAndViewAssert.assertCompareListModelAttribute(mv, "indexSchedules", indexScheduleDAO.getIndexSchedules());
        }
    }
}
