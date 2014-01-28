package au.com.gaiaresources.bdrs.controller.webservice;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;

public class ApplicationServiceUploadSridTest extends AbstractControllerTest {

	@Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private CensusMethodDAO censusMethodDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private FileService fileService;
    @Autowired
    private MetadataDAO metadataDAO;
    
    private Survey survey;
    
    @Before
    public void setup() {
    	Date startDate = getDate(2000, 1, 1);
    	
    	survey = new Survey();
    	survey.setName("my survey");
    	survey.setDescription("my survey description");
    	survey.setStartDate(startDate);
    	
    	surveyDAO.save(survey);
    	
    	for (RecordPropertyType rpt : RecordPropertyType.values()) {
    		RecordProperty rp = new RecordProperty(survey, rpt, metadataDAO);
    		rp.setRequired(false);
    	}
    }
    
	@Test
    public void testSaveSrid() throws Exception {
    	login("admin", "password", new String[] { Role.ADMIN });
    	
    	double x = 600000;
    	double y = 7000000;
    	int srid = 28350;
    	
    	String jsonUpload = getRecordJsonUpload(x, y, srid);
    	
    	request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", jsonUpload.toString());
        request.setParameter("inFrame", "false");
        
        ModelAndView mv = this.handle(request, response);
        Assert.assertNull("model and view should be null", mv);
        
        String syncResponseJson = response.getContentAsString();
        logger.debug("sync response : " + syncResponseJson);
        JSONObject syncResponse = JSONObject.fromStringToJSONObject(syncResponseJson);
        
        Assert.assertEquals("wrong status code", 200, syncResponse.getInt("status"));
        JSONObject recJson = (JSONObject)syncResponse.getJSONObject("200").getJSONArray("sync_result").get(0);
        
        Record rec = recordDAO.getRecord(recJson.getInt("server_id"));
        
        Assert.assertNotNull("record cant be null", rec);
        
        Assert.assertEquals("wrong x", x, rec.getLongitude(), 0.1);
        Assert.assertEquals("wrong y", y, rec.getLatitude(), 0.1);
        Assert.assertEquals("wrong srid", srid, rec.getGeometry().getSRID());
    }
	
	private String getRecordJsonUpload(double x, double y, int srid) {
		return "[{\"taxon_id\":null,\"id\":\"1\",\"server_id\":0,"
    			+"\"attributeValues\":[]"  // no attribute values
    			+",\"scientificName\":\"Eucalyptus ovata\",\"when\":1336437300000,"
    			+"\"number\":10,\"srid\":\""+srid+"\",\"longitude\":\""+x+"\",\"latitude\":\""+y+"\","
    			+"\"notes\":\"qqqqq\",\"survey_id\":"+survey.getId()+",\"gpsAltitude\":\"5.0\",\"accuracy\":\"25.0\"}]";
	}
}
