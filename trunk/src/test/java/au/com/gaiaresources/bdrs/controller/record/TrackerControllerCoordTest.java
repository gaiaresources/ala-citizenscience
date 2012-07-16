package au.com.gaiaresources.bdrs.controller.record;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.controller.record.validator.AbstractValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.CrsValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.WktValidator;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

/**
 * Test to save coordinates in different coordinate reference systems.
 * Uses tracker controller as an entry point to RecordDeserializer.
 */
public class TrackerControllerCoordTest extends AbstractControllerTest {


	@Autowired
	private SurveyDAO surveyDAO;
	@Autowired
	private MetadataDAO mdDAO;
	@Autowired
	private RecordDAO recDAO;
	@Autowired
	private PropertyService propService;
	@Autowired
	private GeoMapService geoMapService;
	@Autowired
	private LocationDAO locDAO;
	
	private Survey survey;
	private GeoMap geoMap;
	
	private Location wgsLoc;
	private Location mgaLoc;
	
	private Logger log = Logger.getLogger(getClass());
	
	private static final double TOLERANCE = 0.0001;
	
	
	@Before
	public void setup() {
		survey = new Survey();
		survey.setName("test survey");
		survey.setDescription("test survey description");
		surveyDAO.save(survey);
		for (RecordPropertyType type : RecordPropertyType.values()) {
			RecordProperty p = new RecordProperty(survey, type, mdDAO);
			p.setRequired(false);
		}
		
		geoMap = geoMapService.getForSurvey(survey);
		
		wgsLoc = new Location();
		wgsLoc.setName("wgs loc");
		wgsLoc.setDescription("wgs loc desc");
		wgsLoc.setLocation(new SpatialUtilFactory().getLocationUtil(4326).createPoint(20, 20));
		locDAO.save(wgsLoc);
		
		mgaLoc = new Location();
		mgaLoc.setName("mgaLoc loc");
		mgaLoc.setDescription("mgaLoc loc desc");
		mgaLoc.setLocation(new SpatialUtilFactory().getLocationUtil(28350).createPoint(6500000, 550000));
		locDAO.save(mgaLoc);
		
		request.setRequestURI(TrackerController.EDIT_URL);
		request.setMethod("POST");
		request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
	}
	
	@Test
	public void testWgs84() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.WGS84);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "100");
		request.setParameter(TrackerController.PARAM_LATITUDE, "20");
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_RECORD_ID);
		
		Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
		Record r = recDAO.getRecord(recId);
		
		assertCoord(r, BdrsCoordReferenceSystem.WGS84.getSrid(), 100, 20);
	}
	
	@Test
	public void testMgaWithZone() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA50);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "385346");
		request.setParameter(TrackerController.PARAM_LATITUDE, "6447726");
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_RECORD_ID);
		
		Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
		Record r = recDAO.getRecord(recId);
		
		assertCoord(r, BdrsCoordReferenceSystem.MGA50.getSrid(), 385346, 6447726);
	}
	
	@Test
	public void testMgaNoZone() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "385346");
		request.setParameter(TrackerController.PARAM_LATITUDE, "6447726");
		request.setParameter(BdrsWebConstants.PARAM_SRID, Integer.toString(BdrsCoordReferenceSystem.MGA50.getSrid()));
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_RECORD_ID);
		
		Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
		Record r = recDAO.getRecord(recId);
		
		assertCoord(r, BdrsCoordReferenceSystem.MGA50.getSrid(), 385346, 6447726);
	}
	
	@Test
	public void testMissingZone() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "385346");
		request.setParameter(TrackerController.PARAM_LATITUDE, "6447726");
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, TrackerController.MV_ERROR_MAP);
		
		Map<String, String> errorMap = getErrorMap(mv);
		Assert.assertEquals("wrong msg", propService.getMessage(AbstractValidator.REQUIRED_MESSAGE_KEY), 
				errorMap.get(BdrsWebConstants.PARAM_SRID));
	}
	
	@Test
	public void testInvalidZone() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "385346");
		request.setParameter(TrackerController.PARAM_LATITUDE, "6447726");
		// invalid string
		request.setParameter(BdrsWebConstants.PARAM_SRID, "adfjkashfsdkafdh");
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, TrackerController.MV_ERROR_MAP);
		
		Map<String, String> errorMap = getErrorMap(mv);
		Assert.assertEquals("wrong msg", propService.getMessage(CrsValidator.MESSAGE_KEY_INVALID_CRS), 
				errorMap.get(BdrsWebConstants.PARAM_SRID));
	}
	
	@Test
	public void testZoneWithWkt() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA);
		
		request.setParameter(TrackerController.PARAM_WKT, "POINT(385346 6447726)");		
		request.setParameter(BdrsWebConstants.PARAM_SRID, Integer.toString(BdrsCoordReferenceSystem.MGA50.getSrid()));
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_RECORD_ID);
		
		Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
		Record r = recDAO.getRecord(recId);
		
		assertCoord(r, BdrsCoordReferenceSystem.MGA50.getSrid(), 385346, 6447726);
	}
	
	@Test
	public void testInvalidWkt() throws Exception {
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.MGA);
		
		request.setParameter(TrackerController.PARAM_WKT, "POINT(38asf6 6447726)");		
		request.setParameter(BdrsWebConstants.PARAM_SRID, Integer.toString(BdrsCoordReferenceSystem.MGA50.getSrid()));
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, TrackerController.MV_ERROR_MAP);
		Map<String, String> errorMap = getErrorMap(mv);

		Assert.assertEquals("wrong map size", 1, errorMap.size());
		Assert.assertEquals("wrong msg", propService.getMessage(WktValidator.MESSAGE_KEY_WKT_INVALID), 
				errorMap.get(BdrsWebConstants.PARAM_WKT));
	}
	
	@Test
	public void testNoCrs() throws Exception {
		// should use the survey crs setting
		login("admin", "password", new String[] { Role.ADMIN });
		geoMap.setCrs(BdrsCoordReferenceSystem.WGS84);
		
		request.setParameter(TrackerController.PARAM_LONGITUDE, "115");
		request.setParameter(TrackerController.PARAM_LATITUDE, "-32");
		
		ModelAndView mv = handle(request, response);
		
		ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_RECORD_ID);
		
		Integer recId = (Integer)mv.getModel().get(RecordWebFormContext.MODEL_RECORD_ID);
		Record r = recDAO.getRecord(recId);
		
		assertCoord(r, BdrsCoordReferenceSystem.WGS84.getSrid(), 115, -32);
	}
	
	private Map<String, String> getErrorMap(ModelAndView mv) {
		return (Map<String, String>)mv.getModel().get(TrackerController.MV_ERROR_MAP);
	}
	
	private void assertCoord(Record r, int srid, double x, double y) {
		Assert.assertNotNull("record cant be null", r);
		Assert.assertEquals("wrong srid", srid, r.getGeometry().getSRID());
		Assert.assertEquals("wrong x", x, r.getGeometry().getCentroid().getX(), TOLERANCE);
		Assert.assertEquals("wrong y", y, r.getGeometry().getCentroid().getY(), TOLERANCE);
	}
	
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
    
    // Simulates the controller running in it's own transaction.
    @Override
    protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	sesh.flush();
    	sesh.clear();
    	ModelAndView mv = super.handle(request, response);
    	sesh.flush();
    	sesh.clear();
    	return mv;
    }
}
