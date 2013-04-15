package au.com.gaiaresources.bdrs.model.record;

import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.test.TestUtil;

import com.ibm.icu.util.Calendar;
import com.vividsolutions.jts.geom.Geometry;

public class RecordDAOImplDuplicateRecordTest extends AbstractControllerTest {

	@Autowired
	private SurveyDAO surveyDAO;
	@Autowired
	private RecordDAO recDAO;
	@Autowired
	private UserDAO userDAO;
	
	private User admin;
	
	private Date d1;
	private Date d2;
	
	private Record r1;
	private Record r2;
	private Record r3;
	private Record r4;
	
	private GeometryBuilder wgsBuilder = new GeometryBuilder(4326);
	private GeometryBuilder mgaBuilder = new GeometryBuilder(28350);
	
	@Before
	public void setup() {
		admin = userDAO.getUser("admin");
		
		Survey survey = new Survey();
		survey.setName("survey name");
		survey.setDescription("survey description");
		survey = surveyDAO.save(survey);
		
		d1 = TestUtil.getDate(2000, 12, 1);
		Geometry geom = wgsBuilder.createPoint(20, 20);
		d2 = TestUtil.getDate(2000, 12, 1, 1, 0);
	
		r1 = createTestRecord(d1, geom, survey, admin);
		r2 = createTestRecord(d1, geom, survey, admin);
		r3 = createTestRecord(d1, geom, survey, admin);
		r4 = createTestRecord(d2, geom, survey, admin);
	}
	
	@Test
	public void testDuplicateRecord() {
		{
			Set<Record> result = recDAO.getDuplicateRecords(r1, 1, 0, 0, new Integer[] {}, new Integer[] {});
			Assert.assertEquals("wrong set size", 2, result.size());
		}
		{
			Set<Record> result = recDAO.getDuplicateRecords(r1, 1, 0, 0, new Integer[] { r2.getId() }, new Integer[] {});
			Assert.assertEquals("wrong set size", 1, result.size());
		}
		{
			Set<Record> result = recDAO.getDuplicateRecords(r1, 1, 0, 0, new Integer[] { r2.getId() }, new Integer[] { r1.getId() });
			Assert.assertEquals("wrong set size", 0, result.size());
		}
		{
			Set<Record> result = recDAO.getDuplicateRecords(r1, 1, Calendar.HOUR_OF_DAY, 1, new Integer[] {}, new Integer[] {});
			Assert.assertEquals("wrong set size", 3, result.size());
		}
	}
	
	private Record createTestRecord(Date now, Geometry geom, Survey survey, User owner) {
        Record rec = new Record();
        rec.setUser(owner);
        rec.setCensusMethod(null);
        rec.setSurvey(survey);
        rec.setSpecies(null);
        rec.setWhen(now);
        rec.setLastDate(now);
        rec.setGeometry(geom);   
        rec.setRecordVisibility(RecordVisibility.PUBLIC);

        return recDAO.saveRecord(rec);
    }
}
