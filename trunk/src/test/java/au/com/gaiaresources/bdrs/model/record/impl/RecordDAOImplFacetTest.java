package au.com.gaiaresources.bdrs.model.record.impl;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.Pair;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class tests the methods of the RecordDAOImpl that are used by the faceted search on the
 * "Advanced Review" page.
 */
public class RecordDAOImplFacetTest extends AbstractControllerTest {

    /** The class we are testing */
    @Autowired
    private RecordDAOImpl recordDAO;
    @Autowired
    private SurveyDAO surveyDAO;


    private User adminUser;
    private User moderatorUser;
    private User normalUser;
    
    private Survey s1;
    private Survey s2;
    
    @Before
    public void setup() {
        // we know admin will always exist so...
        adminUser = userDAO.getUser("admin");
        
        moderatorUser = createUser("moderatorUser", Role.SUPERVISOR);
        normalUser = createUser("normalUser", Role.USER);
        
        s1 = createSurvey();
        s2 = createSurvey();
        
        // 5 public records, 2 of which held.
        // 1 owner only, owned by normal user
        // 1 controlled, owned by moderator
        
        Record rec = createSimpleRecord(moderatorUser, s1, RecordVisibility.PUBLIC, false);
        rec = createSimpleRecord(normalUser, s1, RecordVisibility.OWNER_ONLY, false);
        rec = createSimpleRecord(moderatorUser, s2, RecordVisibility.CONTROLLED, false);
        rec = createSimpleRecord(normalUser, s2, RecordVisibility.PUBLIC, false);
        rec = createSimpleRecord(normalUser, s2, RecordVisibility.PUBLIC, false);
        rec = createSimpleRecord(normalUser, s2, RecordVisibility.PUBLIC, true);
        rec = createSimpleRecord(moderatorUser, s2, RecordVisibility.PUBLIC, true);
    }
    
    // unfortunately we have to write to the database to conclude this test works properly...
    @After
    public void teardown() {
        
    }
    
    private Record createSimpleRecord(User u, Survey s, RecordVisibility visibility, boolean hold) {
        Record rec = new Record();
        rec.setUser(u);
        rec.setLastDate(new Date());
        rec.setTime(1000L);
        // made to be nullable...
        rec.setNumber(null);
        rec.setSpecies(null);
        rec.setSurvey(s);
        rec.setRecordVisibility(visibility);
        rec.setHeld(hold);
        return recordDAO.saveRecord(rec);
    }
    
    // dont care about the name
    private Survey createSurvey() {
        Survey survey = new Survey();
        survey.setName("SingleSiteMultiTaxaSurvey 1234");
        survey.setActive(true);
        survey.setStartDate(new Date());
        survey.setDescription("Single Site Multi Taxa Survey Description");
        return surveyDAO.save(survey);
    }
    
    private User createUser(String name, String roleName) {
       return userDAO.createUser(name, name, name, name, name, name, roleName);
    }


    /**
     * Tests the correct set of visibilities/counts are returned when an anonymous user performs the query.
     * Expected results: there are 5 public records, 2 of which are held so the anonymous user can see 3 records.
     */
    @Test 
    public void testAnonymousUserAccessVisibilities() {

        FilterManager.enableRecordFilter(RequestContextHolder.getContext().getHibernate(), null);
        List<Pair<RecordVisibility, Long>> results = recordDAO.getDistinctRecordVisibilities();
        Assert.assertEquals(2, results.size());
        Pair<RecordVisibility, Long> result = results.get(0);
        Assert.assertEquals(RecordVisibility.CONTROLLED, result.getFirst());
        Assert.assertEquals(Long.valueOf(1), result.getSecond());

        result = results.get(1);
        Assert.assertEquals(RecordVisibility.PUBLIC, result.getFirst());
        Assert.assertEquals(Long.valueOf(3), result.getSecond());
    }

    /**
     * Tests the correct set of visibilities/counts are returned when an admin user performs the query.
     * Expected results: there are 7 records in total, 5 public, 1 controlled, 1 owner only.  Admin can see all.
     */
    @Test
    public void testAdminAccessVisibilities() {

        FilterManager.enableRecordFilter(RequestContextHolder.getContext().getHibernate(), adminUser);
        List<Pair<RecordVisibility, Long>> results = recordDAO.getDistinctRecordVisibilities();
        Assert.assertEquals(3, results.size());
        Pair<RecordVisibility, Long> result = getByVisibility(results, RecordVisibility.PUBLIC);
        Assert.assertEquals(Long.valueOf(5), result.getSecond());

        result = getByVisibility(results, RecordVisibility.OWNER_ONLY);
        Assert.assertEquals(Long.valueOf(1), result.getSecond());

        result = getByVisibility(results, RecordVisibility.CONTROLLED);
        Assert.assertEquals(Long.valueOf(1), result.getSecond());

    }


    private Pair<RecordVisibility, Long> getByVisibility(List<Pair<RecordVisibility, Long>> results, RecordVisibility visibility) {
        for (Pair<RecordVisibility, Long> result : results) {
            if (result.getFirst() == visibility) {
                return result;
            }
        }
        
        throw new RuntimeException("No results exist with visibility : "+visibility);
    }

    /**
     * Tests the correct set of visibilities/counts are returned when an admin user performs the query.
     * Expected results: there are 5 public records in total (even though a couple are held the moderator should
     * see those), 1 controlled owned by the moderator so the moderator should be able to see 6.
     */
    @Test
    public void testModeratorAccessVisibilities() {
        FilterManager.enableRecordFilter(RequestContextHolder.getContext().getHibernate(), moderatorUser);
        List<Pair<RecordVisibility, Long>> results = recordDAO.getDistinctRecordVisibilities();
        Assert.assertEquals(2, results.size());
        Pair<RecordVisibility, Long> result = getByVisibility(results, RecordVisibility.PUBLIC);
        Assert.assertEquals(Long.valueOf(5), result.getSecond());

        result = getByVisibility(results, RecordVisibility.CONTROLLED);
        Assert.assertEquals(Long.valueOf(1), result.getSecond());
    }


    /**
     * Tests the correct set of visibilities/counts are returned when a normal user performs the query.
     * Expected results: there are 5 public records in total, 2 are held, but one of the held records is
     * owned by the user.  The user also has an owner only record he can see.
     */
    @Test
    public void testUserAccessVisibilities() {
        FilterManager.enableRecordFilter(RequestContextHolder.getContext().getHibernate(), normalUser);
        List<Pair<RecordVisibility, Long>> results = recordDAO.getDistinctRecordVisibilities();

        Assert.assertEquals(3, results.size());
        Pair<RecordVisibility, Long> result = getByVisibility(results, RecordVisibility.PUBLIC);
        Assert.assertEquals(Long.valueOf(4), result.getSecond());

        result = getByVisibility(results, RecordVisibility.OWNER_ONLY);
        Assert.assertEquals(Long.valueOf(1), result.getSecond());
        
        result = getByVisibility(results, RecordVisibility.CONTROLLED);
        Assert.assertEquals(Long.valueOf(1), result.getSecond());
    }
}
