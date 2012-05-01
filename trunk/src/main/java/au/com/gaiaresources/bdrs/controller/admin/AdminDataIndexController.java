package au.com.gaiaresources.bdrs.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.model.index.IndexSchedule;
import au.com.gaiaresources.bdrs.model.index.IndexScheduleDAO;
import au.com.gaiaresources.bdrs.model.index.IndexTask;
import au.com.gaiaresources.bdrs.model.index.IndexType;
import au.com.gaiaresources.bdrs.model.index.IndexUtil;
import au.com.gaiaresources.bdrs.search.SearchService;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.util.DateFormatter;
import au.com.gaiaresources.bdrs.util.StringUtils;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Controller class for the data index builder scheduler.
 * Provides interfaces for creating/editing/deleting index building schedules 
 * and the ability to delete and create the indexes on demand.
 * 
 * @author stephanie
 *
 */
@RolesAllowed({Role.ADMIN,Role.SUPERVISOR})
@Controller
public class AdminDataIndexController extends AbstractController {
    private Logger log = Logger.getLogger(getClass());
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Autowired
    private IndexScheduleDAO indexScheduleDAO;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private SessionFactory sessionFactory;
    /*
     * URL and view name constants.
     */
    public static final String INDEX_SCHEDULE_URL = "/admin/index/dataIndexSchedule.htm";
    public static final String INDEX_SCHEDULE_VIEW_NAME = "dataIndexSchedule";
    public static final String INDEX_SCHEDULE_LIST_URL = "/admin/index/dataIndexListing.htm";
    public static final String INDEX_SCHEDULE_LIST_VIEW_NAME = "dataIndexScheduleList";
    
    /*
     * Parameter name constants.
     */
    public static final String PARAM_INDEX_SCHEDULE = "indexSchedule";
    public static final String PARAM_INDEX_TYPE = "indexType";
    public static final String PARAM_DELETE_INDEX = "deleteIndexes";
    
    /**
     * Show the list of scheduled index builds.
     * @param request
     * @param response
     * @return A ModelAndView representing the list of scheduled index builds
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT})
    @RequestMapping(value = INDEX_SCHEDULE_LIST_URL, method = RequestMethod.GET)
    public ModelAndView listDataIndexSchedules(HttpServletRequest request, 
                                       HttpServletResponse response) {
        ModelAndView mv = new ModelAndView(INDEX_SCHEDULE_LIST_VIEW_NAME);
        mv.addObject("indexSchedules", indexScheduleDAO.getIndexSchedules());
        return mv;
    }
    
    /**
     * Saves the list of scheduled index builds posted to the form.  Deletes any 
     * that are not present in the list of ids given.
     * @param request
     * @param response
     * @return
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT})
    @RequestMapping(value = INDEX_SCHEDULE_LIST_URL, method = RequestMethod.POST)
    public ModelAndView saveDataIndexSchedules(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       @RequestParam(value="index", required=false) String[] index) {
        List<IndexSchedule> schedules = indexScheduleDAO.getIndexSchedules();
        List<String> ids = index != null ? Arrays.asList(index) : Collections.EMPTY_LIST;
        List<IndexSchedule> returnSchedules = new ArrayList<IndexSchedule>(schedules.size());
        for (IndexSchedule indexSchedule : schedules) {
            if (!ids.contains(String.valueOf(indexSchedule.getId()))) {
                // delete the index schedule
                indexScheduleDAO.delete(indexSchedule);
            } else {
                returnSchedules.add(indexSchedule);
            }
        }
        
        ModelAndView mv = new ModelAndView(INDEX_SCHEDULE_LIST_VIEW_NAME);
        mv.addObject("indexSchedules", returnSchedules);
        return mv;
    }
    
    /**
     * View for creating a new indexing schedule or editing an existing one.
     * @param request
     * @param response
     * @return
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT})
    @RequestMapping(value = INDEX_SCHEDULE_URL, method = RequestMethod.GET)
    public ModelAndView viewDataIndexSchedule(HttpServletRequest request, 
                                       HttpServletResponse response) {
        // get the current schedule, if there is none, default to Server Startup
        IndexSchedule schedule = null;
        if (request.getParameter("indexId") != null) {
            schedule = indexScheduleDAO.getIndexSchedule(Integer.valueOf(request.getParameter("indexId")));
        } else {
            schedule = new IndexSchedule();
            //getRequestContext().getHibernate().evict(schedule);
            schedule.setType(IndexType.SERVER_STARTUP);
        }
        ModelAndView mv = new ModelAndView(INDEX_SCHEDULE_VIEW_NAME);
        mv.addObject(PARAM_INDEX_SCHEDULE, schedule);
        mv.addObject("indexClasses", IndexUtil.getIndexedClasses());
        return mv;
    }
    
    /**
     * Save a new or existing index schedule.
     * @param request
     * @param response
     * @return
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT})
    @RequestMapping(value = INDEX_SCHEDULE_URL, method = RequestMethod.POST)
    public ModelAndView saveDataIndexSchedule(HttpServletRequest request, 
                                       HttpServletResponse response) {
        IndexSchedule schedule = createSchedule(request);
        
        if (!getRequestContext().getMessages().isEmpty()) {
            // add the error messages and return to the view
            ModelAndView mv = new ModelAndView(INDEX_SCHEDULE_VIEW_NAME);
            mv.addObject(PARAM_INDEX_SCHEDULE, schedule);
            mv.addObject("indexClasses", IndexUtil.getIndexedClasses());
            return mv;
        }
        
        // redirect to the listing view on success
        return listDataIndexSchedules(request, response);
    }
    
    /**
     * Creates an new IndexSchedule or edits an existing one.
     * @param request the request to create the indexSchedule
     * @return a new IndexSchedule or the updated existing one
     */
    private IndexSchedule createSchedule(HttpServletRequest request) {
        IndexSchedule thisSchedule = null;
        if (request.getParameter("indexId") != null) {
            thisSchedule = indexScheduleDAO.getIndexSchedule(Integer.valueOf(request.getParameter("indexId")));
        }
        // required parameters
        String paramIndexType = request.getParameter(PARAM_INDEX_TYPE);
        String[] indexClasses = request.getParameterValues("indexClass");
        if (paramIndexType == null) {
            getRequestContext().addMessage("indexSchedule.form.validation", new Object[]{PARAM_INDEX_TYPE});
            return thisSchedule;
        } else if (indexClasses == null || indexClasses.length < 1) {
            getRequestContext().addMessage("indexSchedule.form.validation", new Object[]{"what to index"});
            return thisSchedule;
        }
        
        String deleteIndex = request.getParameter(PARAM_DELETE_INDEX);
        
        IndexType indexType = IndexType.valueOf(paramIndexType.toUpperCase());
        IndexSchedule schedule = null;
        // set up the dates for the schedule
        String date = request.getParameter("date");
        String time = request.getParameter("time");
        Calendar cal = getFirstTime(request, indexType, date, time);
        Calendar periodCal = getNextTime(indexType, cal);
        
        schedule = saveAndScheduleIndexes(thisSchedule, indexClasses, indexType, deleteIndex, cal, periodCal);
        
        return schedule;
    }

    /**
     * Gets the Calendar that contains the next time for calculating the time delay 
     * between index builds.
     * @param indexType the type of the index (used to determine the length of the period)
     * @param cal the first time (used to calculate the next time)
     * @return a Calendar that contains the next time to run the index
     */
    private Calendar getNextTime(IndexType indexType, Calendar cal) {
        Calendar periodCal = Calendar.getInstance();
        switch (indexType) {
            case SERVER_STARTUP:
                // nothing else to be done for server startup
                break;
            case ONCE:
                periodCal.setTime(cal.getTime());
                break;
            case DAILY:
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.HOUR_OF_DAY, 24);
                break;
            case WEEKLY:
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case MONTHLY:
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.MONTH, 1);
                break;
            default:
                break;
        }
        
        return periodCal;
    }

    /**
     * Gets a Calendar that represents the first time to run the IndexSchedule.
     * @param request the request creating an IndexSchedule
     * @param indexType the type of index
     * @param date the Date of the first time in dd MMM yyyy format
     * @param time the time of the first time in HH:mm format
     * @return a Calendar representing the first time the IndexSchedule should be run.
     */
    private Calendar getFirstTime(HttpServletRequest request, IndexType indexType, String date, String time) {
        Calendar cal = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        String today = DateFormatter.format(now.getTime(), DateFormatter.DAY_MONTH_YEAR);
        switch (indexType) {
            case SERVER_STARTUP:
                // nothing else to be done for server startup
                break;
            case ONCE:
                // set the date and time for the IndexSchedule
                if (!StringUtils.nullOrEmpty(date) && !StringUtils.nullOrEmpty(time)) {
                    cal.setTime(DateFormatter.parse(date+" "+time, DateFormatter.DAY_MONTH_YEAR_TIME));
                } else {
                    Object[] missingParams = new String[1];
                    missingParams[0] = StringUtils.nullOrEmpty(date) && StringUtils.nullOrEmpty(time) ? "date and time" :
                        StringUtils.nullOrEmpty(date) ? "date" : StringUtils.nullOrEmpty(time) ? "time" : "";
                    getRequestContext().addMessage("indexSchedule.form.validation", missingParams);
                    return null;
                }
                break;
            case DAILY:
                // set the time for the IndexSchedule
                if (!StringUtils.nullOrEmpty(time)) {
                    cal.setTime(DateFormatter.parse(today+" "+time, DateFormatter.DAY_MONTH_YEAR_TIME));
                } else {
                    Object[] missingParams = new String[1];
                    missingParams[0] = StringUtils.nullOrEmpty(time) ? "time" : "";
                    getRequestContext().addMessage("indexSchedule.form.validation", missingParams);
                    return null;
                }
                break;
            case WEEKLY:
                String weekday = request.getParameter("weeklyDay");
                if (!StringUtils.nullOrEmpty(time) && !StringUtils.nullOrEmpty(weekday)) {
                    cal.setTime(DateFormatter.parse(today+" "+time, DateFormatter.DAY_MONTH_YEAR_TIME));
                    cal.set(Calendar.DAY_OF_WEEK, Integer.valueOf(weekday));
                } else {
                    Object[] missingParams = new String[1];
                    missingParams[0] = StringUtils.nullOrEmpty(weekday) && StringUtils.nullOrEmpty(time) ? "day and time" :
                        StringUtils.nullOrEmpty(weekday) ? "day" : StringUtils.nullOrEmpty(time) ? "time" : "";
                    getRequestContext().addMessage("indexSchedule.form.validation", missingParams);
                    return null;
                }
                break;
            case MONTHLY:
                if (!StringUtils.nullOrEmpty(date) && !StringUtils.nullOrEmpty(time)) {
                    cal.setTime(DateFormatter.parse(date+" "+time, DateFormatter.DAY_MONTH_YEAR_TIME));
                } else {
                    Object[] missingParams = new String[1];
                    missingParams[0] = StringUtils.nullOrEmpty(date) && StringUtils.nullOrEmpty(time) ? "date and time" :
                        StringUtils.nullOrEmpty(date) ? "date" : StringUtils.nullOrEmpty(time) ? "time" : "";
                    getRequestContext().addMessage("indexSchedule.form.validation", missingParams);
                    return null;
                }
                break;
            default:
                break;
        }
        
        return cal;
    }

    /**
     * Saves new IndexSchedules and schedules them with the taskScheduler.
     * @param thisSchedule the schedule from the request
     * @param indexClasses the classes to create schedules for
     * @param indexType the type of the indexes to create
     * @param deleteIndex boolean indicating whether or not to delete the index before building
     * @param cal a Calendar representing the first scheduled time of the index
     * @param periodCal a Calendar representing the second scheduled time of the index
     * @return an IndexSchedule that represents the request
     */
    private IndexSchedule saveAndScheduleIndexes(IndexSchedule thisSchedule,
            String[] indexClasses, IndexType indexType, String deleteIndex, Calendar cal, Calendar periodCal) {
        Session sesh = null;
        Transaction tx = null;
        IndexSchedule schedule = null;
        try {
            sesh = sessionFactory.openSession();
            for (String indexClass : indexClasses) {
                tx = sesh.beginTransaction();
                if (thisSchedule != null && indexClass.equals(thisSchedule.getClassName())) {
                    schedule = thisSchedule;
                } else {
                    schedule = new IndexSchedule();
                }
                schedule.setType(indexType);
                schedule.setFullRebuild(!StringUtils.nullOrEmpty(deleteIndex));
                schedule.setClassName(indexClass);
                if (!IndexType.SERVER_STARTUP.equals(indexType)) {
                    schedule.setDate(cal.getTime());
                    // save the current schedule and commit the transaction
                    schedule = indexScheduleDAO.save(sesh, schedule);
                    // this ensures that in the IndexTask, the schedule can be retrieved
                    tx.commit();
                    
                    IndexTask indexTask = new IndexTask(sessionFactory, searchService, indexScheduleDAO, schedule.getId());
                    // schedule the build, note that if the date is in the past, the build will occur now
                    if (!IndexType.ONCE.equals(indexType)) {
                        taskScheduler.scheduleAtFixedRate(indexTask, cal.getTime(), periodCal.getTimeInMillis() - cal.getTimeInMillis());
                    } else {
                        taskScheduler.schedule(indexTask, cal.getTime());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error occurred saving and scheduling index builds", e);
            if (tx != null) {
                tx.rollback();
            }
        } finally {
            if (sesh != null) {
                sesh.close();
            }
        }
        return schedule;
    }

    /**
     * Run the index now.
     * @param request
     * @param response
     * @throws IOException 
     */
    @RolesAllowed({Role.ADMIN, Role.ROOT})
    @RequestMapping(value = "/admin/index/runIndex.htm", method = RequestMethod.GET)
    public void runDataIndex(HttpServletRequest request, 
                             HttpServletResponse response) throws IOException {
        String deleteIndex = request.getParameter(PARAM_DELETE_INDEX);
        String[] indexClasses = request.getParameterValues("indexClass");

        if (indexClasses == null || indexClasses.length < 1) {
            Set<Class<?>> classes = IndexUtil.getIndexedClasses();
            indexClasses = new String[classes.size()];
            int i = 0;
            for (Class clazz : classes) {
                indexClasses[i] = clazz.getSimpleName();
                i++;
            }
        }
        
        List<IndexSchedule> schedules = indexScheduleDAO.getIndexSchedules();
        Map<String,String> fullClassNames = IndexUtil.getFullNamesForIndexedClasses(indexClasses);
        if (schedules == null || schedules.isEmpty()) {
            schedules = new ArrayList<IndexSchedule>(indexClasses.length);
            for (String string : indexClasses) {
                // create new Once indexes for right now for the indexClasses so it is recorded when they are indexed
                IndexSchedule newSchedule = new IndexSchedule();
                newSchedule.setClassName(string);
                newSchedule.setType(IndexType.ONCE);
                newSchedule.setFullRebuild(!StringUtils.nullOrEmpty(deleteIndex));
                newSchedule.setDate(new Date());
                indexScheduleDAO.save(newSchedule);
            }
            schedules = indexScheduleDAO.getIndexSchedules();
        } 
        JSONArray array = new JSONArray();
        for (IndexSchedule schedule : schedules) {
            Class clazz = null;
            try {
                clazz = Class.forName(fullClassNames.get(schedule.getClassName()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (schedule.isFullRebuild()) {
                if (clazz != null) {
                    searchService.deleteIndex(clazz);
                } else {
                    searchService.deleteIndexes();
                }
            }
            if (clazz != null) {
                searchService.createIndex(clazz);
            } else {
                searchService.createIndexes();
            }
            // update the last run time of the schedule
            schedule.setLastRun(new Date());
            schedule = indexScheduleDAO.saveOrUpdate(schedule);
            array.add(schedule.flatten());
        }
        writeJson(request, response, array.toString());
    } 
}
