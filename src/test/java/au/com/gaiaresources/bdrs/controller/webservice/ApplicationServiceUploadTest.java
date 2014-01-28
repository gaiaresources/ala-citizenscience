package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.controller.test.TestDataCreator;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.CSVUtils;
import junit.framework.Assert;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ApplicationServiceUploadTest extends AbstractControllerTest {
    
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
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
    private Random rand = new Random(123456789);
    
    private IndicatorSpecies species1;
    
    @Before
    public void setup() {
    	
    	TaxonGroup taxonGroup = new TaxonGroup();
    	taxonGroup.setName("test taxon group");
    	taxaDAO.save(taxonGroup);
    	
    	species1 = new IndicatorSpecies();
    	species1.setTaxonGroup(taxonGroup);
    	species1.setScientificName("species one");
    	species1.setCommonName("common one");
    	species1.setTaxonRank(TaxonRank.SPECIALFORM);
    	taxaDAO.save(species1);
    }
    
    @Test
    public void testMissingIdent() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        
        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("syncData", "[]");
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");
        
        //System.err.println(mv.getModel().get("message"));
        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(500, json.getInt("status"));
        
        JSONObject errorData = json.getJSONObject("500");
        Assert.assertEquals("NullPointerException", errorData.getString("type"));
    }
    
    @Test
    public void testMissingSyncData() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        
        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");
        
        //System.err.println(mv.getModel().get("message"));
        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(500, json.getInt("status"));
        
        JSONObject errorData = json.getJSONObject("500");
        Assert.assertEquals("NullPointerException", errorData.getString("type"));
    }
    
    @Test
    public void testUnauthorized() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        
        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey()+"abc");
        request.setParameter("syncData", "[]");
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");
        
        //System.err.println(mv.getModel().get("message"));
        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        Assert.assertEquals(401, json.getInt("status"));
        
        JSONObject errorData = json.getJSONObject("401");
        Assert.assertEquals("Unauthorized", errorData.getString("message"));
    }
    
    @Test
    public void testSync() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        String syncData = generateSyncData(false);
        //System.err.println(syncData);
        
        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", syncData);
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");

        RequestContext context = RequestContextHolder.getContext();
        Assert.assertEquals("admin", context.getUser().getName());

        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        //System.err.println(mv.getModel().get("message"));
        Assert.assertEquals(200, json.getInt("status"));
        
        JSONObject data = json.getJSONObject("200");
        JSONArray syncResult = data.getJSONArray("sync_result");
        validate(JSONArray.fromString(syncData), syncResult);
        //System.err.println(data.toString());
    }
    
    @Test
    public void testSyncBlankRecordAttributes() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        String syncData = generateSyncData(true);
        //System.err.println(syncData);
        
        request.setMethod("POST");
        request.setRequestURI("/webservice/application/clientSync.htm");
        request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
        request.setParameter("syncData", syncData);
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "postMessage");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");

        RequestContext context = RequestContextHolder.getContext();
        Assert.assertEquals("admin", context.getUser().getName());

        JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
        //System.err.println(mv.getModel().get("message"));
        Assert.assertEquals(200, json.getInt("status"));
        
        JSONObject data = json.getJSONObject("200");
        JSONArray syncResult = data.getJSONArray("sync_result");
        validate(JSONArray.fromString(syncData), syncResult);
        //System.err.println(data.toString());
    }
    
    @Test
    public void testSyncUpdate() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        {
            String syncData = generateSyncData(false);
            MockHttpServletRequest preRequest = new MockHttpServletRequest();
            preRequest.setMethod("POST");
            preRequest.setRequestURI("/webservice/application/clientSync.htm");
            preRequest.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
            preRequest.setParameter("syncData", syncData);
            MockHttpServletResponse preResponse = new MockHttpServletResponse();
            
            ModelAndView preMV = handle(preRequest, preResponse);
            ModelAndViewAssert.assertViewName(preMV, "postMessage");
            ModelAndViewAssert.assertModelAttributeAvailable(preMV, "message");

            RequestContext context = RequestContextHolder.getContext();
            Assert.assertEquals("admin", context.getUser().getName());
            
            JSONObject preJSON = JSONObject.fromStringToJSONObject(preMV.getModel().get("message").toString());
            //System.err.println(mv.getModel().get("message"));
            Assert.assertEquals(200, preJSON.getInt("status"));
            
            JSONObject preData = preJSON.getJSONObject("200");
            JSONArray preSyncResult = preData.getJSONArray("sync_result");
            validate(JSONArray.fromString(syncData), preSyncResult);
        }
        // --------------------------
        {
            String updateSyncData = generateUpdateSyncData(false);
            request.setMethod("POST");
            request.setRequestURI("/webservice/application/clientSync.htm");
            request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
            request.setParameter("syncData", updateSyncData);
            
            ModelAndView mv = handle(request, response);
            ModelAndViewAssert.assertViewName(mv, "postMessage");
            ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");
            
            JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
            //System.err.println(mv.getModel().get("message"));
            Assert.assertEquals(200, json.getInt("status"));
            
            JSONObject data = json.getJSONObject("200");
            JSONArray syncResult = data.getJSONArray("sync_result");
            validate(JSONArray.fromString(updateSyncData), syncResult);
        }
    }
    
    @Test
    public void testSyncUpdateWithBlankAttributes() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        createTestData();
        {
            String syncData = generateSyncData(false);
            MockHttpServletRequest preRequest = new MockHttpServletRequest();
            preRequest.setMethod("POST");
            preRequest.setRequestURI("/webservice/application/clientSync.htm");
            preRequest.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
            preRequest.setParameter("syncData", syncData);
            MockHttpServletResponse preResponse = new MockHttpServletResponse();
            
            ModelAndView preMV = handle(preRequest, preResponse);
            ModelAndViewAssert.assertViewName(preMV, "postMessage");
            ModelAndViewAssert.assertModelAttributeAvailable(preMV, "message");

            RequestContext context = RequestContextHolder.getContext();
            Assert.assertEquals("admin", context.getUser().getName());
            
            JSONObject preJSON = JSONObject.fromStringToJSONObject(preMV.getModel().get("message").toString());
            //System.err.println(mv.getModel().get("message"));
            Assert.assertEquals(200, preJSON.getInt("status"));
            
            JSONObject preData = preJSON.getJSONObject("200");
            JSONArray preSyncResult = preData.getJSONArray("sync_result");
            validate(JSONArray.fromString(syncData), preSyncResult);
        }
        // --------------------------
        {
            String updateSyncData = generateUpdateSyncData(true);
            request.setMethod("POST");
            request.setRequestURI("/webservice/application/clientSync.htm");
            request.setParameter("ident", getRequestContext().getUser().getRegistrationKey());
            request.setParameter("syncData", updateSyncData);
            
            ModelAndView mv = handle(request, response);
            ModelAndViewAssert.assertViewName(mv, "postMessage");
            ModelAndViewAssert.assertModelAttributeAvailable(mv, "message");
            
            JSONObject json = JSONObject.fromStringToJSONObject(mv.getModel().get("message").toString());
            Assert.assertEquals(200, json.getInt("status"));
            
            JSONObject data = json.getJSONObject("200");
            JSONArray syncResult = data.getJSONArray("sync_result");
            validate(JSONArray.fromString(updateSyncData), syncResult);
        }
    }
    
    // attempting to reproduce the test case... we have been seeing duplicates of the
    // attribute value appearing in the database. The upload string used below is the
    // same as one that has caused a 4x repeat of the attribute values...
    @Test
    public void multiAvSyncTest() throws Exception {
    	
    	Date startDate = getDate(2000, 1, 1);
    	
    	Survey survey = new Survey();
    	survey.setName("my survey");
    	survey.setDescription("my survey description");
    	survey.setStartDate(startDate);
    	
    	List<Attribute> attrList = new ArrayList<Attribute>();
    	
    	Attribute a1 = new Attribute();
    	a1.setName("my int");
    	a1.setDescription("my int");
    	a1.setTypeCode("IN");
    	
    	Attribute a2 = new Attribute();
    	a2.setName("my select");
    	a2.setDescription("my select");
    	a2.setTypeCode("SV");
    	
    	List<AttributeOption> optionsList = new ArrayList<AttributeOption>();
    	AttributeOption opt1 = new AttributeOption();
    	opt1.setValue("apple");
    	AttributeOption opt2 = new AttributeOption();
    	opt2.setValue("banana");
    	AttributeOption opt3 = new AttributeOption();
    	opt3.setValue("carrot");
    	
    	taxaDAO.save(opt1);
    	taxaDAO.save(opt2);
    	taxaDAO.save(opt3);
    	optionsList.add(opt1);
    	optionsList.add(opt2);
    	optionsList.add(opt3);
    	
    	a2.setOptions(optionsList);
    	
    	taxaDAO.save(a1);
    	taxaDAO.save(a2);
    	
    	attrList.add(a1);
    	attrList.add(a2);
    	
    	survey.setAttributes(attrList);
    	surveyDAO.save(survey);
    	
    	TaxonGroup group = new TaxonGroup();
    	group.setName("test group");
    	taxaDAO.save(group);
    	
    	IndicatorSpecies species = new IndicatorSpecies();
    	species.setTaxonGroup(group);
    	species.setScientificName("Eucalyptus ovata");
    	species.setCommonName("");
    	taxaDAO.save(species);
    	
    	login("admin", "password", new String[] { Role.ADMIN });
    	
    	String jsonUpload = "[{\"taxon_id\":"+species.getId()+",\"id\":\"1\",\"server_id\":0,"
    			+"\"attributeValues\":[{\"value\":\"10\",\"id\":1,\"server_id\":0,\"attribute_id\":"+a1.getId()+"},"
    			+"{\"value\":\"banana\",\"id\":2,\"server_id\":0,\"attribute_id\":"+a2.getId()+"}]"
    			+",\"scientificName\":\"Eucalyptus ovata\",\"when\":1336437300000,"
    			+"\"number\":10,\"longitude\":\"115.842424\",\"latitude\":\"-31.938391\","
    			+"\"notes\":\"qqqqq\",\"survey_id\":"+survey.getId()+",\"gpsAltitude\":\"15.0\",\"accuracy\":\"25.0\"}]";
    	
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
        Assert.assertEquals("wrong num of attr values", 2, rec.getAttributes().size());
    }
    
    private void validate(JSONArray syncData, JSONArray syncResult) throws IOException {
        // Preprocess the syncData to map records against their client id
        Map<String, JSONObject> syncDataMap = new HashMap<String, JSONObject>(syncData.size());
        for(int j=0; j<syncData.size(); j++) {
            JSONObject jsonRecord = syncData.getJSONObject(j);
            syncDataMap.put(jsonRecord.getString("id"), jsonRecord);
            
            JSONArray jsonRecAttrs = jsonRecord.getJSONArray("attributeValues");
            for(int k=0; k<jsonRecAttrs.size(); k++) {
                JSONObject jsonRecAttr = jsonRecAttrs.getJSONObject(k);
                syncDataMap.put(jsonRecAttr.getString("id"), jsonRecAttr);
            }
        }
        
        for(int i=0; i<syncResult.size(); i++) {
            /*
             * {
             *     "id": "150c6a59-a0df-48d9-b371-051037a1f6f6",
             *     "server_id": 6393,
             *     "klass": "Record"
             * }
             */
            JSONObject jsonResult = syncResult.getJSONObject(i);
            if(Record.class.getSimpleName().equals(jsonResult.getString("klass"))) {
                Assert.assertTrue(syncDataMap.containsKey(jsonResult.getString("id")));
                JSONObject jsonRecord = syncDataMap.get(jsonResult.getString("id"));
                
                Record rec = recordDAO.getRecord(jsonResult.getInt("server_id"));
                Assert.assertNotNull(rec);
                
                Assert.assertEquals(jsonRecord.getDouble("latitude"), rec.getPoint().getY());
                Assert.assertEquals(jsonRecord.getDouble("longitude"), rec.getPoint().getX());
                Assert.assertEquals(jsonRecord.getLong("when"), rec.getWhen().getTime());
                
                if(jsonRecord.isNull("lastDate")) {
                    Assert.assertEquals(rec.getWhen(), rec.getLastDate());
                } else {
                    Assert.assertEquals(jsonRecord.getLong("lastDate"), rec.getLastDate().getTime());
                }
                Assert.assertEquals(jsonRecord.getString("notes"), rec.getNotes());
                Assert.assertEquals(jsonRecord.getInt("number"), rec.getNumber().intValue());
                Assert.assertEquals(jsonRecord.getInt("survey_id"), rec.getSurvey().getId().intValue());
                if(jsonRecord.isNull("censusMethod_id")) {
                    Assert.assertNull(rec.getCensusMethod());
                } else {
                    Assert.assertEquals(jsonRecord.getInt("censusMethod_id"), rec.getCensusMethod().getId().intValue());
                    
                    if(Taxonomic.TAXONOMIC.equals(rec.getCensusMethod().getTaxonomic()) || 
                    		Taxonomic.OPTIONALLYTAXONOMIC.equals(rec.getCensusMethod().getTaxonomic())) {
                        Assert.assertEquals(jsonRecord.getInt("taxon_id"), rec.getSpecies().getId().intValue());
                    } else {
                        Assert.assertNull(rec.getSpecies());
                    }
                }
            } else if(AttributeValue.class.getSimpleName().equals(jsonResult.getString("klass"))) {
                Assert.assertTrue(syncDataMap.containsKey(jsonResult.getString("id")));
                JSONObject jsonRecAttr = syncDataMap.get(jsonResult.getString("id"));
                
                int serverId = jsonResult.getInt("server_id");
                AttributeValue recAttr = recordDAO.getAttributeValue(serverId);
                Assert.assertNotNull(recAttr);
                
                Attribute attr = recAttr.getAttribute(); 
                switch(attr.getType()) {
                    case INTEGER:
                    case INTEGER_WITH_RANGE:
                        Assert.assertEquals(jsonRecAttr.get("value").toString(),
                                            recAttr.getStringValue());
                        if(!recAttr.getStringValue().isEmpty()) {
                            Assert.assertEquals(jsonRecAttr.getInt("value"), 
                                                recAttr.getNumericValue().intValue());
                        }
                        break;
                    case DECIMAL:
                        Assert.assertEquals(jsonRecAttr.get("value").toString(),
                                            recAttr.getStringValue());
                        if(!recAttr.getStringValue().isEmpty()) {
                            Assert.assertEquals(jsonRecAttr.getDouble("value"), 
                                                recAttr.getNumericValue().doubleValue());
                        }
                        break;
                    case DATE:
                        if((jsonRecAttr.isNull("value")) || 
                                jsonRecAttr.get("value").toString().isEmpty()) {
                            Assert.assertEquals("", recAttr.getStringValue());
                            Assert.assertEquals(null, recAttr.getDateValue());
                        } else {
                            Date date = new Date(jsonRecAttr.getLong(("value")));
                            Assert.assertEquals(dateFormat.format(date), recAttr.getStringValue());
                            Assert.assertEquals(date, recAttr.getDateValue());
                        }
                        break;
                    case STRING:
                    case STRING_AUTOCOMPLETE:
                    case TEXT:
                    case STRING_WITH_VALID_VALUES:
                        if(jsonRecAttr.isNull("value")) {
                            Assert.assertEquals("", recAttr.getStringValue());
                        } else {
                            Assert.assertEquals(jsonRecAttr.get("value").toString(),
                                                recAttr.getStringValue());
                        }
                    	
                    	break;
                    case MULTI_CHECKBOX:
                    	String[] actualMultiCheckboxValues = recAttr.getMultiCheckboxValue(); 
                        Arrays.sort(actualMultiCheckboxValues);
                        for(String expected : CSVUtils.fromCSVString(jsonRecAttr.get("value").toString())) {
                            Assert.assertTrue(Arrays.binarySearch(actualMultiCheckboxValues, expected) > -1);
                        }
                        break;
                    case MULTI_SELECT:
                    	String[] actualMultiSelectValues = recAttr.getMultiSelectValue(); 
                        Arrays.sort(actualMultiSelectValues);
                        for(String expected : CSVUtils.fromCSVString(jsonRecAttr.get("value").toString())) {
                            Assert.assertTrue(Arrays.binarySearch(actualMultiSelectValues, expected) > -1);
                        }
                        break;
                    case SINGLE_CHECKBOX:
                        // If the attribute value is blank, the empty string will be parsed as a boolean
                        // and then toString into the AttributeValue.stringValue.
                        // This means that the boolean value is false and the string value is "false"
                        if(jsonRecAttr.getString("value").isEmpty()) {
                            Assert.assertEquals(Boolean.FALSE.toString(), recAttr.getStringValue());
                            Assert.assertFalse(recAttr.getBooleanValue());
                        } else {
                            Assert.assertEquals(Boolean.valueOf(jsonRecAttr.getBoolean("value")).toString(),
                                                recAttr.getStringValue());
                            Assert.assertEquals(Boolean.valueOf(jsonRecAttr.getBoolean("value")),
                                                recAttr.getBooleanValue());
                        }
                    	break;
                    case REGEX:
                    case BARCODE:
                        Assert.assertEquals(jsonRecAttr.get("value").toString(),
                                            recAttr.getStringValue());
                        break;
                    case TIME:
                    case HTML:
                    case HTML_RAW:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                        break;
                    case IMAGE:
                        if(!recAttr.getStringValue().isEmpty()) {
                            String imgText;
                            if(jsonRecAttr.getInt("server_id") == 0) {
                                imgText = attr.getName();
                            } else {
                                imgText = "Edited "+attr.getName();
                            }
                            String expectedEncodedBase64 = Base64.encodeBytes(createImage(72,72,imgText));
                            File targetFile = fileService.getFile(recAttr, recAttr.getStringValue()).getFile();
                            BufferedImage targetImg = ImageIO.read(targetFile);
                            
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(targetImg, "png", baos);
                            baos.flush();
                            String targetEncodedBase64 = Base64.encodeBytes(baos.toByteArray());
                            baos.close();
                            
                            Assert.assertEquals(expectedEncodedBase64, targetEncodedBase64);
                        }
                        break;
                    case SPECIES:
                        if (!jsonRecAttr.get("value").toString().isEmpty()) {
                                Assert.assertNotNull("species should not be null", recAttr.getSpecies());
                                Assert.assertEquals("wrong species id", species1.getId(), recAttr.getSpecies().getId());
                                Assert.assertEquals("wrong attr string value", species1.getScientificName(), recAttr.getStringValue()); 
                        }
                        break;
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        // census method types should add a record to the attribute value
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private String generateSyncData(boolean blankRecAttr) throws IOException {
        List<Map<String,Object>> syncData = new ArrayList<Map<String, Object>>();
        for(Survey survey : surveyDAO.getActivePublicSurveys(false)) {
            List<IndicatorSpecies> taxaList = new ArrayList<IndicatorSpecies>(survey.getSpecies());
            if(taxaList.isEmpty()) {
                taxaList = taxaDAO.getIndicatorSpecies();
            }
            
            for(CensusMethod method : survey.getCensusMethods()) {
                IndicatorSpecies taxon = taxaList.get(rand.nextInt(taxaList.size()));
                syncData.add(createJSONRecord(survey, method, taxon, null, blankRecAttr));
            }
            IndicatorSpecies taxon = taxaList.get(rand.nextInt(taxaList.size()));
            syncData.add(createJSONRecord(survey, null, taxon, null, blankRecAttr));
        }
        
        return JSONArray.toJSONString(syncData);
    }
    
    private String generateUpdateSyncData(boolean blankRecAttr) throws IOException {
        List<Map<String,Object>> syncData = new ArrayList<Map<String, Object>>();
        for(Survey survey : surveyDAO.getActivePublicSurveys(false)) {
            
            List<IndicatorSpecies> taxaList = new ArrayList<IndicatorSpecies>(survey.getSpecies());
            if(taxaList.isEmpty()) {
                taxaList = taxaDAO.getIndicatorSpecies();
            }
            
            Set<User> users = new HashSet<User>();
            users.add(getRequestContext().getUser());
            for(Record record : recordDAO.getRecords(survey, users)) {
                IndicatorSpecies taxon = taxaList.get(rand.nextInt(taxaList.size()));
                
                syncData.add(createJSONRecord(survey, record.getCensusMethod(), taxon, record, blankRecAttr));
            }
        }
        return JSONArray.fromList(syncData).toString();
    }
    
    private Map<String, Object> createJSONRecord(Survey survey, CensusMethod method, IndicatorSpecies taxon, Record record, boolean blankRecAttr) throws IOException {
        Map<String, Object> rec = new HashMap<String, Object>();
        
        // Mandatory Stuff First.
        rec.put("id", UUID.randomUUID().toString());
        rec.put("server_id", record == null ? "0" : record.getId().toString());
        rec.put("latitude", record == null ? -31.95222 : -32.96233);
        rec.put("longitude", record == null ? 115.85889 : 117.9422);
        rec.put("accuracy", record == null ? 5: 15);
        rec.put("gpsAltitude", record == null ? 10: 25);
        rec.put("when", System.currentTimeMillis());
        rec.put("lastDate", rand.nextBoolean() ? null : System.currentTimeMillis());
        rec.put("notes", record == null ? "notes" : "edited notes");
        rec.put("number", rand.nextInt(5));
        rec.put("survey_id", survey.getId());
        
        if(method != null) {
            rec.put("censusMethod_id", method.getId());
            if(Taxonomic.TAXONOMIC.equals(method.getTaxonomic()) || 
            		Taxonomic.OPTIONALLYTAXONOMIC.equals(method.getTaxonomic())) {
                rec.put("taxon_id", taxon.getId());
            } else {
                rec.put("taxon_id", null);
            }
        } else {
            rec.put("censusMethod_id", null);
        }
        
        List<Map<String,Object>> recAttrs = new ArrayList<Map<String, Object>>();
        if(record == null) {
            for(Attribute attr : survey.getAttributes()) {
                if(!AttributeType.FILE.equals(attr.getType()) && !AttributeScope.LOCATION.equals(attr.getScope())
                		&& !AttributeType.AUDIO.equals(attr.getType()) 
                		&& !AttributeType.VIDEO.equals(attr.getType())) {
                    recAttrs.add(createJSONRecordAttribute(attr, null, blankRecAttr));
                }
            }
            
            if(method != null) {
                for(Attribute attr : method.getAttributes()) {
                    if(!AttributeType.FILE.equals(attr.getType()) 
                            && !AttributeType.AUDIO.equals(attr.getType())
                            && !AttributeType.VIDEO.equals(attr.getType())) {
                        recAttrs.add(createJSONRecordAttribute(attr, null, blankRecAttr));
                    }
                }
                if(Taxonomic.TAXONOMIC.equals(method.getTaxonomic()) || 
                		Taxonomic.OPTIONALLYTAXONOMIC.equals(method.getTaxonomic())) {
                    for(Attribute attr : taxon.getTaxonGroup().getAttributes()) {
                        if(!AttributeType.FILE.equals(attr.getType()) 
                                && !AttributeType.AUDIO.equals(attr.getType())
                                && !AttributeType.VIDEO.equals(attr.getType())) {
                            recAttrs.add(createJSONRecordAttribute(attr, null, blankRecAttr));
                        }
                    }
                }
            }
        } else {
            for(AttributeValue recordAttribute : record.getAttributes()) {
                recAttrs.add(createJSONRecordAttribute(recordAttribute.getAttribute(), recordAttribute, blankRecAttr));
            }
        }
        
        rec.put("attributeValues", recAttrs);

        return rec;
    }

    private Map<String, Object> createJSONRecordAttribute(Attribute attr, 
                                                          AttributeValue recordAttribute,
                                                          boolean blankData) throws IOException {
        
        Map<String, Object> recAttr = new HashMap<String, Object>();
        recAttr.put("id", UUID.randomUUID().toString());
        recAttr.put("server_id", recordAttribute == null ? "0" : recordAttribute.getId().toString());
        recAttr.put("attribute_id", attr.getId());
        
        if(blankData) {
            recAttr.put("value", "");
        } else {
            switch(attr.getType()) {
                case INTEGER:
                case INTEGER_WITH_RANGE:
                    recAttr.put("value", rand.nextInt(5));
                    break;
                case DECIMAL:
                    recAttr.put("value", rand.nextDouble() * 5.0d);
                    break;
                case DATE:
                    recAttr.put("value", rand.nextBoolean() ? System.currentTimeMillis() : "");
                    break;
                case STRING:
                case STRING_AUTOCOMPLETE:
                case TEXT:
                    recAttr.put("value", recordAttribute == null ? attr.getDescription() : "Edited "+attr.getDescription());
                    break;
                case STRING_WITH_VALID_VALUES:
                    // Just get the first option.
                    recAttr.put("value", recordAttribute == null ? attr.getOptions().get(0).getValue() : attr.getOptions().get(1).getValue());
                    break;
                case REGEX:
                case BARCODE:
                    recAttr.put("value", "#123456");
                    break;
                case TIME:
                    recAttr.put("value", "12:34");
                    break;
                case HTML:
                case HTML_RAW:
                case HTML_NO_VALIDATION:
                case HTML_COMMENT:
                case HTML_HORIZONTAL_RULE:
                    recAttr.put("value", "<hr/>");
                    break;
                case MULTI_CHECKBOX:
                case MULTI_SELECT:
                    int offset = recordAttribute == null ? 0 : 1;
                    String value = CSVUtils.toCSVString(new String[] {
                            attr.getOptions().get(0 + offset).getValue(), 
                            attr.getOptions().get(1 + offset).getValue()
                        }, true);
                    recAttr.put("value", value);
                    break;
                case SINGLE_CHECKBOX: 
                    recAttr.put("value", Boolean.toString(recordAttribute == null));
                    break;
                case IMAGE:
                    String imgText = recordAttribute == null ? attr.getName() : "Edited "+attr.getName();
                    String encodedBase64 = Base64.encodeBytes(createImage(72,72,imgText));
                    recAttr.put("value", encodedBase64);
                    break;
                case SPECIES:
                    recAttr.put("value", species1.getScientificName());
                    recAttr.put(ApplicationService.JSON_KEY_TAXON_ID, species1.getId());
                    break;
                case CENSUS_METHOD_ROW:
                case CENSUS_METHOD_COL:
                    // census method types should add a record to the attribute value
                    break;
                default:
                    throw new IllegalArgumentException("Cannot handle attribute with type: "+attr.getType());
            }
        }
        return recAttr;
    }

    private void createTestData() throws Exception {
        TestDataCreator testDataCreator = new TestDataCreator(getRequestContext());
        
        testDataCreator.createTaxonGroups(2, 0, true);
        testDataCreator.createTaxa(3, 0);
        testDataCreator.createTaxonProfile();
        testDataCreator.createSurvey(1, 0);
        
        CensusMethod nonTaxonomicParentMethod = createCensusMethod("Non Taxonomic Parent Method", Taxonomic.NONTAXONOMIC, null);
        createCensusMethod("Non Taxonomic Child Method", Taxonomic.NONTAXONOMIC, nonTaxonomicParentMethod);
        
        CensusMethod taxonomicParentMethod = createCensusMethod("Taxonomic Parent Method", Taxonomic.TAXONOMIC, null);
        createCensusMethod("Taxonomic Child Method", Taxonomic.TAXONOMIC, taxonomicParentMethod);
        
        CensusMethod optionallyTaxonomicParentMethod = createCensusMethod("Optionally Taxonomic Parent Method", Taxonomic.OPTIONALLYTAXONOMIC, null);
        createCensusMethod("OPTIONALLYTaxonomic Child Method", Taxonomic.OPTIONALLYTAXONOMIC, optionallyTaxonomicParentMethod);
        
        CensusMethod basicMethod = createCensusMethod("Basic Method", Taxonomic.TAXONOMIC, null);
        
        // Just created a survey so we know there is exactly one survey.
        Survey survey = surveyDAO.getActivePublicSurveys(false).get(0);
        survey.getCensusMethods().add(nonTaxonomicParentMethod);
        survey.getCensusMethods().add(taxonomicParentMethod);
        survey.getCensusMethods().add(basicMethod);
        
        
        RecordProperty recordProperty = new RecordProperty(survey, RecordPropertyType.NOTES, metadataDAO);
        recordProperty.setHidden(true);
        recordProperty = new RecordProperty(survey, RecordPropertyType.WHEN, metadataDAO);
        recordProperty.setRequired(false);
        surveyDAO.save(survey);
    }
    
    private CensusMethod createCensusMethod(String name, Taxonomic taxonomic, CensusMethod parent) {
        CensusMethod method = new CensusMethod();
        method.setName(name);
        method.setTaxonomic(taxonomic);
        method.setType("General");
        method.setDescription(name + " Description");
        
        List<Attribute> attributeList = method.getAttributes();
        for(AttributeType attrType : AttributeType.values()) {
            Attribute attr = new Attribute();
            attr.setRequired(true);
            attr.setName(attrType.toString());
            attr.setTypeCode(attrType.getCode());
            attr.setScope(null);
            attr.setTag(false);
            
            if(AttributeType.STRING_WITH_VALID_VALUES.equals(attrType) ||
            		AttributeType.MULTI_CHECKBOX.equals(attrType) ||
            		AttributeType.MULTI_SELECT.equals(attrType)) {
                List<AttributeOption> optionList = new ArrayList<AttributeOption>();
                for(int i=0; i<4; i++) {
                    AttributeOption opt = new AttributeOption();
                    opt.setValue(String.format("Option %d", i));
                    opt = taxaDAO.save(opt);
                    optionList.add(opt);
                }
                attr.setOptions(optionList);
            }
            
            attr = taxaDAO.save(attr);
            attributeList.add(attr);
        }
        
        censusMethodDAO.save(method);
        if(parent != null) {
            parent.getCensusMethods().add(method);
            censusMethodDAO.save(parent);
        }
        return method;
    }
    
    private byte[] createImage(int width, int height, String text) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = (Graphics2D)img.getGraphics();
        g2.setBackground(new Color(220,220,220));

        Dimension size;
        float fontSize = g2.getFont().getSize();
        // Make the text as large as possible.
        do {
            g2.setFont(g2.getFont().deriveFont(fontSize));
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            int hgt = metrics.getHeight();
            int adv = metrics.stringWidth(text);
            size = new Dimension(adv+2, hgt+2);
            fontSize = fontSize + 1f;
        } while(size.width < Math.round(0.9*width) && size.height < Math.round(0.9*height));
        
        g2.setColor(Color.DARK_GRAY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString(text, (width-size.width)/2, (height-size.height)/2);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(0,0,width-1,height-1);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(width * height);
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] rawBytes = baos.toByteArray();
        baos.close();
        
        return rawBytes;
    }
}
