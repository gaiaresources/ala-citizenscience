package au.com.gaiaresources.bdrs.controller.survey;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.FileDataSource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.LocationAttributeSurveyCreator;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.FileUtils;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.ZipUtils;

public class SurveyImportExportTest extends AbstractGridControllerTest {

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private RecordDAO recordDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private CensusMethodDAO methodDAO;
    @Autowired
    private GeoMapService geoMapService;
    /**
     * Used to change the default view returned by the controller
     */
    @Autowired
    private PreferenceDAO preferenceDAO;
    @Autowired
    private FileService fileService;

    @Test
    public void testSurveyLocationAttributes() throws Exception {
        requestDropDatabase();

        LocationAttributeSurveyCreator surveyCreator = new LocationAttributeSurveyCreator(surveyDAO, locationDAO,
                new SpatialUtilFactory().getLocationUtil(), methodDAO, userDAO, taxaDAO, recordDAO, metadataDAO, preferenceDAO, fileService,
                geoMapService);
        surveyCreator.create(false);

        RequestContext c = new RequestContext(request, applicationContext);
        RequestContextHolder.set(c);
        c.setHibernate(sessionFactory.getCurrentSession());

        for (Survey s : surveyDAO.getSurveys(surveyCreator.getSpeciesA())) {
            testSurveyImportExport(s);
        }
    }

    @Test
    public void testSurveyExportWithCensusMethod() throws Exception {
        requestDropDatabase();
        testSurveyImportExport(survey1);
    }

    @Test
    public void testSurveyExportWithNoCensusMethod() throws Exception {
        requestDropDatabase();
        testSurveyImportExport(survey2);
    }
    
    @Test
    public void testSurveyExportWithNonDefaultCrs() throws Exception {
    	requestDropDatabase();
        geoMapService.getForSurvey(survey1).setCrs(BdrsCoordReferenceSystem.MGA);
        for (Location loc : survey1.getLocations()) {
        	loc.getLocation().setSRID(28350);
        }
        sessionFactory.getCurrentSession().flush();
        testSurveyImportExport(survey1);
    }
    
    @Test
    public void testSurveyExportWithCss() throws Exception {
        requestDropDatabase();
        String filename = "testfile.css";
        String fileContents = "blah";
        Metadata md = survey1.addMetadata(Metadata.SURVEY_CSS, filename);
        metaDAO.save(md);
        
        MockMultipartFile testFile = new MockMultipartFile("file", filename, "text/css", fileContents.getBytes());
        fileService.createFile(md, testFile);
        
        Survey s = testSurveyImportExport(survey1);
        
        Metadata cssMeta = s.getMetadataByKey(Metadata.SURVEY_CSS);
        Assert.assertNotNull("Metadata for survey css should not be null", cssMeta);
        FileDataSource fds = fileService.getFile(cssMeta, cssMeta.getValue());
        Assert.assertNotNull("file data source cannot be null", fds);
        Assert.assertEquals("wrong contents", fileContents, FileUtils.readFile(fds.getFile().getAbsolutePath()));
    }

    private void createNewRequest() {
        request = createMockHttpServletRequest();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
    }

    private Survey testSurveyImportExport(Survey survey) throws Exception {
    	
    	// create default map
    	geoMapService.getForSurvey(survey);
    	
        login("admin", "password", new String[]{Role.ADMIN});
        int initialSurveyCount = surveyDAO.getSurveys(currentUser).size();

        response = new MockHttpServletResponse();
        createNewRequest();
        request.setMethod("GET");
        request.setRequestURI(SurveyBaseController.SURVEY_EXPORT_URL);
        request.addParameter(SurveyBaseController.QUERY_PARAM_SURVEY_ID, String.valueOf(survey.getId()));
        login("admin", "password", new String[]{Role.ADMIN});
        
        handle(request, response);
        Assert.assertEquals(ZipUtils.ZIP_CONTENT_TYPE, response.getContentType());
        byte[] exportContent = response.getContentAsByteArray();

        // Uncomment if you want to capture the test file
        //BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/tmp/test_survey_export_" + survey.getId() + ".zip"));
        //bos.write(exportContent);
        //bos.flush();
        //bos.close();

        response = new MockHttpServletResponse();
        createNewRequest();
        Assert.assertTrue(response.getContentAsByteArray().length == 0);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.setMethod("POST");
        req.setRequestURI(SurveyBaseController.SURVEY_IMPORT_URL);
        req.addFile(new MockMultipartFile(SurveyBaseController.POST_KEY_SURVEY_IMPORT_FILE, exportContent));
        
        login("admin", "password", new String[]{Role.ADMIN});
        
        handle(request, response);
        
        assertMessageCode("bdrs.survey.import.success");

        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());

        // Test we have an additional survey.
        Assert.assertEquals(initialSurveyCount + 1, surveyDAO.getSurveys(currentUser).size());

        // Find the new survey.
        Survey actualSurvey = null;
        for (Survey s : surveyDAO.getSurveys(currentUser)) {
            if (actualSurvey == null && s.getName() != null && s.getName().equals(survey.getName()) && !s.getId().equals(survey.getId())) {
                actualSurvey = s;
            }
        }
        Assert.assertNotNull("Cannot find imported survey", actualSurvey);

        Assert.assertEquals(survey.getName(), actualSurvey.getName());
        Assert.assertEquals(survey.getDescription(), actualSurvey.getDescription());
        Assert.assertEquals(survey.getEndDate(), actualSurvey.getEndDate());
        Assert.assertEquals(survey.getStartDate(), actualSurvey.getStartDate());
        Assert.assertEquals("wrong survey crs", geoMapService.getForSurvey(survey).getCrs(), 
        		geoMapService.getForSurvey(actualSurvey).getCrs());

        assertMetadata(survey.getMetadata(), actualSurvey.getMetadata());
        assertAttributes(survey.getAttributes(), actualSurvey.getAttributes());
        assertCensusMethod(survey.getCensusMethods(), actualSurvey.getCensusMethods());
        assertLocations(survey.getLocations(), actualSurvey.getLocations());

        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
        
        return actualSurvey;
    }

    private void assertLocations(Collection<Location> expected, Collection<Location> actual) {
        Map<String, Location> locMap = new HashMap<String, Location>(expected.size());
        for (Location loc : expected) {
            locMap.put(loc.getName(), loc);
        }
        for (Location actualLoc : actual) {
            Location expectedLoc = locMap.get(actualLoc.getName());
            Assert.assertNotNull("Expected Location not found: " + actualLoc.getName(), expectedLoc);

            Assert.assertEquals(expectedLoc.getName(), actualLoc.getName());
            Assert.assertEquals(expectedLoc.getWeight(), actualLoc.getWeight());
            Assert.assertEquals(expectedLoc.getDescription(), actualLoc.getDescription());
            Assert.assertEquals(expectedLoc.getLocation().toText(), actualLoc.getLocation().toText());
            Assert.assertEquals("wrong srid", expectedLoc.getLocation().getSRID(), actualLoc.getLocation().getSRID());

            assertMetadata(expectedLoc.getMetadata(), actualLoc.getMetadata());
            assertAttrValue(expectedLoc.getAttributes(), actualLoc.getAttributes());
        }
    }

    private void assertAttrValue(Collection<AttributeValue> expected, Collection<AttributeValue> actual) {
        Map<String, AttributeValue> attrValMap = new HashMap<String, AttributeValue>(expected.size());
        for (AttributeValue attrVal : expected) {
            attrValMap.put(attrVal.getAttribute().getName(), attrVal);
        }

        for (AttributeValue actualAttrVal : actual) {
            AttributeValue expectedAttrVal = attrValMap.get(actualAttrVal.getAttribute().getName());
            Assert.assertNotNull(
                    String.format("Expected AttributeValue not found: (Parent Attribute name: %s)", actualAttrVal.getAttribute().getName()),
                    expectedAttrVal);
            
            Assert.assertEquals("String value for attribute "+actualAttrVal.getAttribute().getName()+" does not match.", 
                                expectedAttrVal.getStringValue(), actualAttrVal.getStringValue());
            Assert.assertEquals(expectedAttrVal.getWeight(), actualAttrVal.getWeight());
            Assert.assertEquals("Date value for attribute "+actualAttrVal.getAttribute().getName()+" does not match.", 
                                expectedAttrVal.getDateValue(), actualAttrVal.getDateValue());
            if(expectedAttrVal.getNumericValue() == null) {
                Assert.assertEquals(expectedAttrVal.getNumericValue(), actualAttrVal.getNumericValue());
            } else {
                Assert.assertTrue("Numeric value for attribute "+actualAttrVal.getAttribute().getName()+" does not match. " +
                                  "Expected "+expectedAttrVal.getNumericValue()+" but was "+actualAttrVal.getNumericValue(), 
                                  expectedAttrVal.getNumericValue().compareTo(actualAttrVal.getNumericValue()) == 0);
            }
            Assert.assertEquals("Boolean value for attribute "+actualAttrVal.getAttribute().getName()+" does not match.", 
                                expectedAttrVal.getBooleanValue(), actualAttrVal.getBooleanValue());
            Assert.assertArrayEquals("MultiCheckbox value for attribute "+actualAttrVal.getAttribute().getName()+" does not match.", 
                                     expectedAttrVal.getMultiCheckboxValue(), actualAttrVal.getMultiCheckboxValue());
            Assert.assertArrayEquals("MultiSelect value for attribute "+actualAttrVal.getAttribute().getName()+" does not match.", 
                                     expectedAttrVal.getMultiSelectValue(), actualAttrVal.getMultiSelectValue());
        }
    }

    private void assertCensusMethod(Collection<CensusMethod> expected, Collection<CensusMethod> actual) {
        Map<String, CensusMethod> methodMap = new HashMap<String, CensusMethod>(expected.size());
        for (CensusMethod method : expected) {
            if (method != null) {
                methodMap.put(method.getName(), method);
            }
        }

        for (CensusMethod actualMethod : actual) {
            if (actualMethod != null) {
                CensusMethod expectedMethod = methodMap.get(actualMethod.getName());
                Assert.assertNotNull("Expected CensusMethod not found: " + actualMethod.getName(), expectedMethod);

                Assert.assertEquals(expectedMethod.getName(), actualMethod.getName());
                Assert.assertEquals(expectedMethod.getDescription(), actualMethod.getDescription());
                Assert.assertEquals(expectedMethod.getTaxonomic(), actualMethod.getTaxonomic());
                Assert.assertEquals(expectedMethod.getType(), actualMethod.getType());
                Assert.assertEquals(expectedMethod.getWeight(), actualMethod.getWeight());

                assertAttributes(expectedMethod.getAttributes(), actualMethod.getAttributes());
                assertMetadata(expectedMethod.getMetadata(), actualMethod.getMetadata());
                assertCensusMethod(expectedMethod.getCensusMethods(), actualMethod.getCensusMethods());
            }
        }

    }

    private void assertAttributes(Collection<Attribute> expected, Collection<Attribute> actual) {
        // Test the attributes match
        Map<String, Attribute> attributeMap = new HashMap<String, Attribute>(expected.size());
        for (Attribute attr : expected) {
            attributeMap.put(attr.getName(), attr);
        }
        
        Assert.assertEquals(expected.size(), actual.size());

        for (Attribute actualAttr : actual) {
            Attribute expectedAttr = attributeMap.remove(actualAttr.getName());
            Assert.assertNotNull("Expected Metadata not found: " + actualAttr.getName(), expectedAttr);

            Assert.assertEquals(expectedAttr.getName(), actualAttr.getName());
            Assert.assertEquals(expectedAttr.getDescription(), actualAttr.getDescription());
            Assert.assertEquals(expectedAttr.getScope(), actualAttr.getScope());
            Assert.assertEquals(expectedAttr.getType(), actualAttr.getType());
            Assert.assertEquals(expectedAttr.isRequired(), actualAttr.isRequired());
            Assert.assertEquals(expectedAttr.isTag(), actualAttr.isTag());
            Assert.assertEquals(expectedAttr.getWeight(), actualAttr.getWeight());

            assertAttributeOptions(expectedAttr.getOptions(), actualAttr.getOptions());
        }
    }

    private void assertAttributeOptions(Collection<AttributeOption> expected, Collection<AttributeOption> actual) {
        // Test the attribute options match
        Set<String> optionsSet = new HashSet<String>(expected.size());
        for (AttributeOption opt : expected) {
            optionsSet.add(opt.getValue());
        }
        for (AttributeOption actualOpt : actual) {
            boolean removeSuccess = optionsSet.remove(actualOpt.getValue());
            Assert.assertTrue("Failed to remove: " + actualOpt.getValue(), removeSuccess);
        }
        Assert.assertTrue("There are more actual AttributeOptions than expected AttributeOptions.", optionsSet.isEmpty());
    }

    private void assertMetadata(Collection<Metadata> expected, Collection<Metadata> actual) {
        // Test that the metadata match
        Map<String, Metadata> metadataMap = new HashMap<String, Metadata>(expected.size());
        for (Metadata md : expected) {
            metadataMap.put(md.getKey(), md);
        }
        for (Metadata actualMd : actual) {
            Metadata expectedMd = metadataMap.remove(actualMd.getKey());
            Assert.assertNotNull("Expected Metadata not found: " + actualMd.getKey(), expectedMd);
            Assert.assertEquals(expectedMd.getValue(), actualMd.getValue());
            Assert.assertEquals(expectedMd.getWeight(), actualMd.getWeight());
        }
        Assert.assertTrue("Excess Metadata found. Not all metadata may have been imported.", metadataMap.isEmpty());
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
}
