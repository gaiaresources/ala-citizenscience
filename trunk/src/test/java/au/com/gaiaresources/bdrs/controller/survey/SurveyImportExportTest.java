package au.com.gaiaresources.bdrs.controller.survey;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.controller.LocationAttributeSurveyCreator;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContext;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.*;

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
    private PreferenceDAO prefDAO;
    @Autowired
    private CensusMethodDAO methodDAO;
    /**
     * Used to change the default view returned by the controller
     */
    @Autowired
    private PreferenceDAO preferenceDAO;
    @Autowired
    private LocationService locationService;
    @Autowired
    private FileService fileService;

    @Test
    public void testSurveyLocationAttributes() throws Exception {
        requestDropDatabase();

        LocationAttributeSurveyCreator surveyCreator = new LocationAttributeSurveyCreator(surveyDAO, locationDAO,
                locationService, methodDAO, userDAO, taxaDAO, recordDAO, metadataDAO, preferenceDAO, fileService);
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

    private void createNewRequest() {
        request = createMockHttpServletRequest();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
    }

    private void testSurveyImportExport(Survey survey) throws Exception {
        login("admin", "password", new String[]{Role.ADMIN});
        int initialSurveyCount = surveyDAO.getSurveys(currentUser).size();

        response = new MockHttpServletResponse();
        createNewRequest();
        request.setMethod("GET");
        request.setRequestURI(SurveyBaseController.SURVEY_EXPORT_URL);
        request.addParameter(SurveyBaseController.QUERY_PARAM_SURVEY_ID, String.valueOf(survey.getId()));

        handle(request, response);
        Assert.assertEquals(ZipUtils.ZIP_CONTENT_TYPE, response.getContentType());
        byte[] exportContent = response.getContentAsByteArray();

//        // Uncomment if you want to capture the test file
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/tmp/test_survey_export_" + survey.getId() + ".zip"));
//        bos.write(exportContent);
//        bos.flush();
//        bos.close();

        response = new MockHttpServletResponse();
        createNewRequest();
        Assert.assertTrue(response.getContentAsByteArray().length == 0);
        MockMultipartHttpServletRequest req = (MockMultipartHttpServletRequest) request;
        req.setMethod("POST");
        req.setRequestURI(SurveyBaseController.SURVEY_IMPORT_URL);
        req.addFile(new MockMultipartFile(SurveyBaseController.POST_KEY_SURVEY_IMPORT_FILE, exportContent));
        handle(request, response);

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

        assertMetadata(survey.getMetadata(), actualSurvey.getMetadata());
        assertAttributes(survey.getAttributes(), actualSurvey.getAttributes());
        assertCensusMethod(survey.getCensusMethods(), actualSurvey.getCensusMethods());
        assertLocations(survey.getLocations(), actualSurvey.getLocations());

        sessionFactory.getCurrentSession().getTransaction().commit();
        sessionFactory.getCurrentSession().beginTransaction();
        RequestContextHolder.set(new RequestContext(request, applicationContext));
        getRequestContext().setHibernate(sessionFactory.getCurrentSession());
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
            
            Assert.assertEquals(expectedAttrVal.getStringValue(), actualAttrVal.getStringValue());
            Assert.assertEquals(expectedAttrVal.getWeight(), actualAttrVal.getWeight());
            Assert.assertEquals(expectedAttrVal.getDateValue(), actualAttrVal.getDateValue());
            if(expectedAttrVal.getNumericValue() == null) {
                Assert.assertEquals(expectedAttrVal.getNumericValue(), actualAttrVal.getNumericValue());
            } else {
                Assert.assertTrue(expectedAttrVal.getNumericValue().compareTo(actualAttrVal.getNumericValue()) == 0);
            }
            Assert.assertEquals(expectedAttrVal.getBooleanValue(), actualAttrVal.getBooleanValue());
            Assert.assertArrayEquals(expectedAttrVal.getMultiCheckboxValue(), actualAttrVal.getMultiCheckboxValue());
            Assert.assertArrayEquals(expectedAttrVal.getMultiSelectValue(), actualAttrVal.getMultiSelectValue());
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
