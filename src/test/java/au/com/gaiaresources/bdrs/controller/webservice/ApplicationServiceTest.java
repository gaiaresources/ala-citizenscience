package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.survey.SurveyImportExportService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplicationServiceTest extends AbstractControllerTest {

    private Logger log = Logger.getLogger(getClass());
    private Survey frogSurveyInDb, birdAndFrogSurveyInDb, allSurvey;

    private IndicatorSpecies frog1, frog2, bird1, bird2, bird3;

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private LocationDAO locationDAO;

    @Autowired
    private TaxaDAO taxaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AttributeDAO attributeDAO;

    @Autowired
    private MetadataDAO metadataDAO;

    @Autowired
    private SurveyImportExportService surveyImportExportService;

    @Autowired
    private GeoMapService geoMapService;

    @Autowired
    private SpeciesProfileDAO speciesProfileDAO;

    @Autowired
    private ManagedFileDAO managedFileDAO;

    private ManagedFile testFile;

    @Before
    public void setup() {

        //create a user
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "user@mailinator.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        User u = userDAO.createUser("user", "fn", "ln", emailAddr, encodedPassword, "usersIdent", new String[]{"ROLE_USER"});
        Set<User> users = new HashSet<User>();
        users.add(u);

        testFile = new ManagedFile();
        testFile.setContentType("image/jpeg");
        testFile.setFilename("testfile.jpeg");
        testFile.setCredit("test credit");
        testFile.setLicense("test license");
        testFile.setDescription("test description");
        managedFileDAO.save(testFile);

        //create taxa for species
        TaxonGroup frogTaxon = new TaxonGroup();
        TaxonGroup birdTaxon = new TaxonGroup();
        TaxonGroup allGroup = new TaxonGroup();
        frogTaxon.setName("frogs");
        frogTaxon.setImage("frog image.jpg");
        frogTaxon.setThumbNail("frogthumb.jpg");
        birdTaxon.setName("birds");
        birdTaxon.setImage("bird_image.jpg");
        birdTaxon.setThumbNail("birdthumb.jpg");
        allGroup.setName("all");
        allGroup.setImage("all_group.jpeg");
        allGroup.setThumbNail("allThumb.jpg");
        taxaDAO.save(frogTaxon);
        taxaDAO.save(birdTaxon);
        taxaDAO.save(allGroup);

        //create species
        Set<IndicatorSpecies> frogSet = new HashSet<IndicatorSpecies>();
        Set<IndicatorSpecies> birdSet = new HashSet<IndicatorSpecies>();
        frog1 = new IndicatorSpecies();
        frog2 = new IndicatorSpecies();
        bird1 = new IndicatorSpecies();
        bird2 = new IndicatorSpecies();
        bird3 = new IndicatorSpecies();
        frog1.setCommonName("commonNameFrog1");
        frog2.setCommonName("commonNameFrog2");
        bird1.setCommonName("commonNamebird1");
        bird2.setCommonName("commonNamebird2");
        bird3.setCommonName("commonNamebird3");
        frog1.setScientificName("scientificNameFrog1");
        frog2.setScientificName("scientificNameFrog2");
        bird1.setScientificName("scientificNamebird1");
        bird2.setScientificName("scientificNamebird2");
        bird3.setScientificName("scientificNamebird3");
        frog1.setTaxonGroup(taxaDAO.getTaxonGroup("frogs"));
        frog2.setTaxonGroup(taxaDAO.getTaxonGroup("frogs"));
        bird1.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        bird2.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        bird3.setTaxonGroup(taxaDAO.getTaxonGroup("birds"));
        frogSet.add(taxaDAO.save(frog1));
        frogSet.add(taxaDAO.save(frog2));
        birdSet.add(taxaDAO.save(bird1));
        birdSet.add(taxaDAO.save(bird2));
        birdSet.add(taxaDAO.save(bird3));

        frog1.addSecondaryGroup(allGroup);
        frog2.addSecondaryGroup(allGroup);
        bird1.addSecondaryGroup(allGroup);
        bird2.addSecondaryGroup(allGroup);
        bird3.addSecondaryGroup(allGroup);

        addProfileItem(frog1);
        addProfileItem(frog2);
        addProfileItem(bird1);
        addProfileItem(bird2);
        addProfileItem(bird3);

        //create attributes for survey
        List<Attribute> attributes = new ArrayList<Attribute>();
        List<Attribute> birdAndFrogAttributes = new ArrayList<Attribute>();
        Attribute frogSurveyAttribute1 = new Attribute();
        Attribute frogSurveyAttribute2 = new Attribute();
        Attribute birdAndFrogSurveyAttribute1 = new Attribute();
        Attribute birdAndFrogSurveyAttribute2 = new Attribute();
        Attribute birdAndFrogSurveyAttribute3 = new Attribute();
        frogSurveyAttribute1.setName("surveyAttribute1");
        frogSurveyAttribute2.setName("surveyAttribute2");
        birdAndFrogSurveyAttribute1.setName("surveyAttribute1");
        birdAndFrogSurveyAttribute2.setName("surveyAttribute2");
        birdAndFrogSurveyAttribute3.setName("surveyAttribute3");
        frogSurveyAttribute1.setRequired(true);
        frogSurveyAttribute2.setRequired(true);
        birdAndFrogSurveyAttribute1.setRequired(true);
        birdAndFrogSurveyAttribute2.setRequired(true);
        birdAndFrogSurveyAttribute3.setRequired(true);
        frogSurveyAttribute1.setTypeCode("SV");
        frogSurveyAttribute2.setTypeCode("SV");
        birdAndFrogSurveyAttribute1.setTypeCode("SV");
        birdAndFrogSurveyAttribute2.setTypeCode("SV");
        birdAndFrogSurveyAttribute3.setTypeCode("SV");
        frogSurveyAttribute1.setTag(false);
        frogSurveyAttribute2.setTag(false);
        birdAndFrogSurveyAttribute1.setTag(false);
        birdAndFrogSurveyAttribute2.setTag(false);
        birdAndFrogSurveyAttribute3.setTag(false);
        attributes.add(attributeDAO.save(frogSurveyAttribute1));
        attributes.add(attributeDAO.save(frogSurveyAttribute2));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute1));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute2));
        birdAndFrogAttributes.add(attributeDAO.save(birdAndFrogSurveyAttribute3));

        //create locations for survey
        List<Location> locations = new ArrayList<Location>();
        List<Location> birdSurveyLocations = new ArrayList<Location>();
        Location l1 = new Location();
        Location l2 = new Location();
        Location birdSurveyLocation1 = new Location();
        Location birdSurveyLocation2 = new Location();
        Location birdSurveyLocation3 = new Location();
        l1.setName("location1");
        l2.setName("location2");
        birdSurveyLocation1.setName("location1");
        birdSurveyLocation2.setName("location2");
        birdSurveyLocation3.setName("location3");

        //create surveys
        Survey frogSurvey = new Survey();
        Survey birdAndFrogSurvey = new Survey();

        frogSurvey.setActive(true);
        birdAndFrogSurvey.setActive(true);
        frogSurvey.setName("frogSurvey");
        birdAndFrogSurvey.setName("birdAndFrogSurvey");
        frogSurvey.setDescription("This is a frog survey used for testing");
        birdAndFrogSurvey.setDescription("This is a birdsAndFrogs survey used for testing");
        frogSurvey.setUsers(users);
        birdAndFrogSurvey.setUsers(users);
        //add attributes
        frogSurvey.setAttributes(attributes);
        birdAndFrogSurvey.setAttributes(birdAndFrogAttributes);
        //add locations
        locations.add(locationDAO.save(l1));
        locations.add(locationDAO.save(l2));
        frogSurvey.setLocations(locations);
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation1));
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation2));
        birdSurveyLocations.add(locationDAO.save(birdSurveyLocation3));
        birdAndFrogSurvey.setLocations(birdSurveyLocations);
        //add species
        frogSurvey.setSpecies(frogSet);
        birdAndFrogSurvey.setSpecies(birdSet);
        birdAndFrogSurvey.getSpecies().addAll(frogSet);

        Metadata md = frogSurvey.setFormRendererType(SurveyFormRendererType.DEFAULT);
        metadataDAO.save(md);

        //save the survey
        frogSurveyInDb = surveyDAO.save(frogSurvey);
        birdAndFrogSurveyInDb = surveyDAO.save(birdAndFrogSurvey);

        geoMapService.getForSurvey(frogSurveyInDb);

        allSurvey = new Survey();
        allSurvey.setActive(true);
        allSurvey.setName("All survey");
        allSurvey.setDescription("This survey has all the species");
        surveyDAO.save(allSurvey);
    }

    private void addProfileItem(IndicatorSpecies sp) {
        SpeciesProfile profile = new SpeciesProfile();
        profile.setHeader("item_header");
        profile.setDescription("item description");
        profile.setType(SpeciesProfile.SPECIES_PROFILE_IMAGE);
        profile.setContent(testFile.getUuid());
        speciesProfileDAO.save(profile);
        List<SpeciesProfile> profileList = new ArrayList<SpeciesProfile>(sp.getInfoItems());
        profileList.add(profile);
        sp.setInfoItems(profileList);
    }

    /**
     * Tests getting survey related data from a particular survey.
     * There is no survey on the device yet.
     *
     * @throws Exception
     */
    private void testGetSurvey(boolean includeSpecies) throws Exception {
        request.setMethod("GET");
        request.setRequestURI(this.getDownloadSurveyUrl(includeSpecies));
        request.setParameter("ident", userDAO.getUser("user").getRegistrationKey());
        request.setParameter("sid", frogSurveyInDb.getId().toString());

        handle(request, response);

        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());

        //count the survey attributes
        JSONArray attributes = JSONArray.fromString(responseContent.get("attributesAndOptions").toString());
        int expectedAttributeCount = 0;
        for (Attribute a : frogSurveyInDb.getAttributes()) {
            if (!AttributeScope.LOCATION.equals(a.getScope())) {
                expectedAttributeCount++;
            }
        }
        Assert.assertEquals("Expected the attributes size to be " + attributes.size() + " but it was " + expectedAttributeCount, attributes.size(), expectedAttributeCount);
        //count the survey locations
        JSONArray locations = JSONArray.fromString(responseContent.get("locations").toString());
        Assert.assertEquals("Expected the locations size to be " + locations.size() + " but it was " + frogSurveyInDb.getLocations().size(), locations.size(), frogSurveyInDb.getLocations().size());

        if (includeSpecies) {
            //count the species in the survey
            JSONArray indicatorSpecies = JSONArray.fromString(responseContent.get("indicatorSpecies").toString());
            Assert.assertEquals("Expected the indicatorSpecies size to be " + indicatorSpecies.size() + " but it was " + frogSurveyInDb.getSpecies().size(), indicatorSpecies.size(), frogSurveyInDb.getSpecies().size());
        }

        // Make sure the json export object is correctly returned
        Assert.assertTrue("should contain survey template", responseContent.containsKey(ApplicationService.JSON_KEY_SURVEY_TEMPLATE));
        JSONObject surveyTemplate = responseContent.getJSONObject(ApplicationService.JSON_KEY_SURVEY_TEMPLATE);
        Assert.assertEquals("wrong survey template name",
                frogSurveyInDb.getName(),
                surveyTemplate.getJSONObject("Survey").getJSONObject(frogSurveyInDb.getId().toString()).getString("name"));
    }

    /**
     * Tests getting survey related data from a particular survey.
     * There is already a survey on the device
     *
     * @throws Exception
     */
    private void testGetSurveyHasSurveyOnDevice(boolean includeSpecies) throws Exception {
        request.setMethod("GET");
        request.setRequestURI(this.getDownloadSurveyUrl(includeSpecies));
        request.addParameter("ident", userDAO.getUser("user").getRegistrationKey());
        request.addParameter("sid", birdAndFrogSurveyInDb.getId().toString());
        if (includeSpecies) {
            JSONArray surveysOnDevice = new JSONArray();
            surveysOnDevice.add(frogSurveyInDb.getId());
            request.addParameter("surveysOnDevice", surveysOnDevice.toString());
        }

        handle(request, response);

        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());

        //count the survey attributes
        int expectedAttributeCount = 0;
        for (Attribute a : birdAndFrogSurveyInDb.getAttributes()) {
            if (!AttributeScope.LOCATION.equals(a.getScope())) {
                expectedAttributeCount++;
            }
        }
        JSONArray attributes = JSONArray.fromString(responseContent.get("attributesAndOptions").toString());
        Assert.assertEquals("Expected the attributes size to be " + attributes.size(), attributes.size(), expectedAttributeCount);
        //count the survey locations
        JSONArray locations = JSONArray.fromString(responseContent.get("locations").toString());
        Assert.assertEquals("Expected the locations size to be " + locations.size(), locations.size(), birdAndFrogSurveyInDb.getLocations().size());

        if (includeSpecies) {
            //count the species in the survey
            JSONArray indicatorSpecies = JSONArray.fromString(responseContent.get("indicatorSpecies").toString());
            Assert.assertEquals("IndicatorSpecies size does not match", indicatorSpecies.size(), (birdAndFrogSurveyInDb.getSpecies().size() - frogSurveyInDb.getSpecies().size()));
        }

    }

    /**
     * Test getting recordProperties from survey.
     * Default settings for recordProperties.
     *
     * @throws Exception
     */
    private void testGetSurveyCheckRecordProperties(boolean includeSpecies) throws Exception {
        request.setMethod("GET");
        request.setRequestURI(this.getDownloadSurveyUrl(includeSpecies));
        request.addParameter("ident", userDAO.getUser("user").getRegistrationKey());
        request.addParameter("sid", birdAndFrogSurveyInDb.getId().toString());
        JSONArray surveysOnDevice = new JSONArray();
        if (includeSpecies) {
            surveysOnDevice.add(frogSurveyInDb.getId());
            request.addParameter("surveysOnDevice", surveysOnDevice.toString());
        }

        handle(request, response);

        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());

        //count the record properties
        int expectedRecordPropertiesCount = RecordPropertyType.values().length;

        JSONArray recordProperties = JSONArray.fromString(responseContent.get("recordProperties").toString());
        Assert.assertEquals("Expected the recordProperties size to be " + expectedRecordPropertiesCount, expectedRecordPropertiesCount, recordProperties.size());
    }

    /**
     * Test getting recordProperties from survey.
     * Two recordProperties are set to hidden in the survey.
     *
     * @throws Exception
     */
    private void testGetSurveyCheckRecordPropertiesHiddenFields(boolean includeSpecies) throws Exception {

        RecordProperty speciesProperty = new RecordProperty(birdAndFrogSurveyInDb, RecordPropertyType.SPECIES, metadataDAO);
        speciesProperty.setHidden(true);
        RecordProperty locationProperty = new RecordProperty(birdAndFrogSurveyInDb, RecordPropertyType.LOCATION, metadataDAO);
        locationProperty.setHidden(true);

        request.setMethod("GET");
        request.setRequestURI(getDownloadSurveyUrl(includeSpecies));
        request.addParameter("ident", userDAO.getUser("user").getRegistrationKey());
        request.addParameter("sid", birdAndFrogSurveyInDb.getId().toString());
        JSONArray surveysOnDevice = new JSONArray();
        surveysOnDevice.add(frogSurveyInDb.getId());
        request.addParameter("surveysOnDevice", surveysOnDevice.toString());

        handle(request, response);

        JSONObject responseContent = JSONObject.fromStringToJSONObject(response.getContentAsString());

        //count the record properties
        int expectedRecordPropertiesCount = RecordPropertyType.values().length - 2;

        JSONArray recordProperties = JSONArray.fromString(responseContent.get("recordProperties").toString());
        Assert.assertEquals("Expected the recordProperties size to be " + expectedRecordPropertiesCount, expectedRecordPropertiesCount, recordProperties.size());
    }

    private String getDownloadSurveyUrl(boolean includeSpecies) {
        return includeSpecies ? ApplicationService.LEGACY_DOWNLOAD_SURVEY_URL : ApplicationService.DOWNLOAD_SURVEY_NO_SPECIES_URL;
    }


    /**
     * Tests getting survey related data from a particular survey.
     * There is no survey on the device yet.
     *
     * @throws Exception
     */
    @Test
    public void testGetSurveyWithSpecies() throws Exception {
        this.testGetSurvey(true);
    }

    @Test
    public void testGetSurveyNoSpecies() throws Exception {
        this.testGetSurvey(false);
    }

    /**
     * Tests getting survey related data from a particular survey.
     * There is already a survey on the device
     *
     * @throws Exception
     */
    @Test
    public void testGetSurveyHasSurveyOnDeviceIncludeSpecies() throws Exception {
        this.testGetSurveyHasSurveyOnDevice(true);
    }

    @Test
    public void testGetSurveyHasSurveyOnDeviceExcludeSpecies() throws Exception {
        this.testGetSurveyHasSurveyOnDevice(false);
    }

    /**
     * Test getting recordProperties from survey.
     * Default settings for recordProperties.
     *
     * @throws Exception
     */
    @Test
    public void testGetSurveyCheckRecordPropertiesIncludeSpecies() throws Exception {
        this.testGetSurveyCheckRecordProperties(true);
    }

    @Test
    public void testGetSurveyCheckRecordPropertiesExcludeSpecies() throws Exception {
        this.testGetSurveyCheckRecordProperties(false);
    }

    /**
     * Test getting recordProperties from survey.
     * Two recordProperties are set to hidden in the survey.
     *
     * @throws Exception
     */
    @Test
    public void testGetSurveyCheckRecordPropertiesHiddenFieldIncludeSpecies() throws Exception {
        this.testGetSurveyCheckRecordPropertiesHiddenFields(true);
    }

    @Test
    public void testGetSurveyCheckRecordPropertiesHiddenFieldExcludeSpecies() throws Exception {
        this.testGetSurveyCheckRecordPropertiesHiddenFields(false);
    }

    @Test
    public void testGetSpeciesForSurveyWithSetSpecies() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(ApplicationService.DOWNLOAD_SURVEY_SPECIES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, frogSurveyInDb.getId().toString());
        request.setParameter(ApplicationService.PARAM_FIRST, "1");
        request.setParameter(ApplicationService.PARAM_MAX_RESULTS, "1");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertEquals("wrong count in json", 2, json.getInt("count"));
        JSONArray jsonSpeciesArray = json.getJSONArray("list");
        Assert.assertEquals("wrong list size", 1, jsonSpeciesArray.size());
        JSONObject jsonSpecies = jsonSpeciesArray.getJSONObject(0);
        Assert.assertEquals("wrong species id", frog2.getId().intValue(), jsonSpecies.getInt("server_id"));

        // assert info items
        for (int i = 0; i < jsonSpeciesArray.size(); ++i) {
            assertInfoItems(jsonSpeciesArray.getJSONObject(i));
        }

        assertTaxonGroups(json);
    }

    @Test
    public void testGetSpeciesForSurveyWithAllSpecies() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(ApplicationService.DOWNLOAD_SURVEY_SPECIES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, allSurvey.getId().toString());
        request.setParameter(ApplicationService.PARAM_FIRST, "1");
        request.setParameter(ApplicationService.PARAM_MAX_RESULTS, "1");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertEquals("wrong count in json", 5, json.getInt("count"));
        JSONArray jsonSpeciesArray = json.getJSONArray("list");
        Assert.assertEquals("wrong list size", 1, jsonSpeciesArray.size());
        JSONObject jsonSpecies = jsonSpeciesArray.getJSONObject(0);
        Assert.assertEquals("wrong species id", frog2.getId().intValue(), jsonSpecies.getInt("server_id"));

        // assert info items
        for (int i = 0; i < jsonSpeciesArray.size(); ++i) {
            assertInfoItems(jsonSpeciesArray.getJSONObject(i));
        }

        assertTaxonGroups(json);
    }

    @Test
    public void testGetSpeciesExcludeSelf() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(ApplicationService.DOWNLOAD_SURVEY_SPECIES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, frogSurveyInDb.getId().toString());
        request.setParameter(ApplicationService.PARAM_FIRST, "1");
        request.setParameter(ApplicationService.PARAM_MAX_RESULTS, "1");
        request.setParameter(ApplicationService.PARAM_SURVEYS_ON_DEVICE, "[" + this.birdAndFrogSurveyInDb.getId().toString() + "]");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertEquals("wrong count in json", 0, json.getInt("count"));
        JSONArray jsonSpeciesArray = json.getJSONArray("list");
        Assert.assertEquals("wrong list size", 0, jsonSpeciesArray.size());

        // assert info items
        for (int i = 0; i < jsonSpeciesArray.size(); ++i) {
            assertInfoItems(jsonSpeciesArray.getJSONObject(i));
        }

        assertTaxonGroups(json);
    }

    @Test
    public void testGetSpeciesExcludeSurveyWithAllSpecies() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(ApplicationService.DOWNLOAD_SURVEY_SPECIES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, frogSurveyInDb.getId().toString());
        request.setParameter(ApplicationService.PARAM_FIRST, "1");
        request.setParameter(ApplicationService.PARAM_MAX_RESULTS, "1");
        request.setParameter(ApplicationService.PARAM_SURVEYS_ON_DEVICE, "[" + allSurvey.getId().toString() + "]");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertEquals("wrong count in json", 0, json.getInt("count"));
        JSONArray jsonSpeciesArray = json.getJSONArray("list");
        Assert.assertEquals("wrong list size", 0, jsonSpeciesArray.size());

        // assert info items
        for (int i = 0; i < jsonSpeciesArray.size(); ++i) {
            assertInfoItems(jsonSpeciesArray.getJSONObject(i));
        }

        assertTaxonGroups(json);
    }

    @Test
    public void testGetSpeciesForSurveyWithAllExcludeSurveyWithSome() throws Exception {
        request.setMethod("GET");
        request.setRequestURI(ApplicationService.DOWNLOAD_SURVEY_SPECIES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, allSurvey.getId().toString());
        request.setParameter(ApplicationService.PARAM_FIRST, "1");
        request.setParameter(ApplicationService.PARAM_MAX_RESULTS, "1");
        request.setParameter(ApplicationService.PARAM_SURVEYS_ON_DEVICE, "[" + frogSurveyInDb.getId().toString() + "]");

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertEquals("wrong count in json", 3, json.getInt("count"));
        JSONArray jsonSpeciesArray = json.getJSONArray("list");
        Assert.assertEquals("wrong list size", 1, jsonSpeciesArray.size());
        JSONObject jsonSpecies = jsonSpeciesArray.getJSONObject(0);
        Assert.assertEquals("wrong species id", bird2.getId().intValue(), jsonSpecies.getInt("server_id"));

        // assert info items
        for (int i = 0; i < jsonSpeciesArray.size(); ++i) {
            assertInfoItems(jsonSpeciesArray.getJSONObject(i));
        }

        assertTaxonGroups(json);
    }

    @Test
    public void testCreateSurveySetName() throws Exception {
        User u = userDAO.getUser("user");
        frogSurveyInDb.setPublic(true);
        JSONObject importData = surveyImportExportService.exportObject(frogSurveyInDb);
        String testname = "hello world survey";

        Survey s = surveyDAO.getSurveyByName(testname);
        Assert.assertNull("survey should not exist", s);

        request.setMethod("POST");
        request.setRequestURI(ApplicationService.CREATE_SURVEY_URL);
        request.setParameter(ApplicationService.PARAM_NAME, testname);
        request.setParameter(ApplicationService.PARAM_SURVEY_TEMPLATE, importData.toString());
        request.setParameter(ApplicationService.PARAM_IDENT, u.getRegistrationKey());

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());
        Assert.assertTrue("expect success to be true", json.getBoolean(ApplicationService.JSON_KEY_SUCCESS));

        s = surveyDAO.getSurveyByName(testname);
        Assert.assertNotNull("survey should exist", s);

        Assert.assertEquals("wrong name", testname, s.getName());

        Assert.assertTrue("user set should be empty", s.getUsers().isEmpty());
    }

    @Test
    public void testCreateSurveySetNameNonPublic() throws Exception {
        User u = userDAO.getUser("user");
        frogSurveyInDb.setPublic(false);
        JSONObject importData = surveyImportExportService.exportObject(frogSurveyInDb);
        String testname = "hello world survey";

        Survey s = surveyDAO.getSurveyByName(testname);
        Assert.assertNull("survey should not exist", s);

        request.setMethod("POST");
        request.setRequestURI(ApplicationService.CREATE_SURVEY_URL);
        request.setParameter(ApplicationService.PARAM_NAME, testname);
        request.setParameter(ApplicationService.PARAM_SURVEY_TEMPLATE, importData.toString());
        request.setParameter(ApplicationService.PARAM_IDENT, u.getRegistrationKey());

        this.handle(request, response);

        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());
        Assert.assertTrue("expect success to be true", json.getBoolean(ApplicationService.JSON_KEY_SUCCESS));

        s = surveyDAO.getSurveyByName(testname);
        Assert.assertNotNull("survey should exist", s);

        Assert.assertEquals("wrong name", testname, s.getName());

        Assert.assertEquals("wrong user set size", 1, s.getUsers().size());
        Assert.assertEquals("wrong user id", u.getId(), s.getUsers().iterator().next().getId());
    }

    @Test
    public void testCreateSurveySetBadIdent() throws Exception {
        User u = userDAO.getUser("user");
        frogSurveyInDb.setPublic(false);
        JSONObject importData = surveyImportExportService.exportObject(frogSurveyInDb);
        String testname = "hello world survey";

        Survey s = surveyDAO.getSurveyByName(testname);
        Assert.assertNull("survey should not exist", s);

        request.setMethod("POST");
        request.setRequestURI(ApplicationService.CREATE_SURVEY_URL);
        request.setParameter(ApplicationService.PARAM_NAME, testname);
        request.setParameter(ApplicationService.PARAM_SURVEY_TEMPLATE, importData.toString());
        request.setParameter(ApplicationService.PARAM_IDENT, "bogusident");

        this.handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testCreateSurveySetNoIdent() throws Exception {
        User u = userDAO.getUser("user");
        frogSurveyInDb.setPublic(false);
        JSONObject importData = surveyImportExportService.exportObject(frogSurveyInDb);
        String testname = "hello world survey";

        Survey s = surveyDAO.getSurveyByName(testname);
        Assert.assertNull("survey should not exist", s);

        request.setMethod("POST");
        request.setRequestURI(ApplicationService.CREATE_SURVEY_URL);
        request.setParameter(ApplicationService.PARAM_NAME, testname);
        request.setParameter(ApplicationService.PARAM_SURVEY_TEMPLATE, importData.toString());

        this.handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    public void assertInfoItems(JSONObject speciesJson) {
        JSONArray infoItemArray = speciesJson.getJSONArray(ApplicationService.JSON_KEY_SPECIES_INFO_ITEMS);
        for (int i = 0; i < infoItemArray.size(); ++i) {
            JSONObject obj = infoItemArray.getJSONObject(i);

            Assert.assertTrue("needs id field", obj.has("id"));
            Integer profileId = obj.getInt("id");
            SpeciesProfile sp = speciesProfileDAO.getById(profileId);
            Assert.assertNotNull("profile should be non null", sp);

            Assert.assertEquals("wrong header string", sp.getHeader(), obj.getString("header"));
            Assert.assertEquals("wrong description string", sp.getDescription(), obj.getString("description"));
            Assert.assertEquals("wrong content string", testFile.getUuid(), obj.getString("content"));
            Assert.assertEquals("wrong type string", SpeciesProfile.SPECIES_PROFILE_IMAGE, obj.getString("type"));

            Assert.assertTrue("has weight", obj.has("weight"));
            Assert.assertEquals("wrong weight", sp.getWeight().intValue(), obj.getInt("weight"));

            // All of these should have a managed file with no content attached.
            // we are using flatten - well tested, so assuming this works to save a little time writing assertions.
            Assert.assertTrue("needs managed file", obj.has(ApplicationService.JSON_KEY_MANAGED_FILE));
        }
    }

    public void assertTaxonGroups(JSONObject responseObj) {
        Set<Integer> expectedTaxonGroupId = new HashSet<Integer>();
        JSONArray speciesListArray = responseObj.getJSONArray("list");
        for (int speciesIdx = 0; speciesIdx < speciesListArray.size(); ++speciesIdx) {
            JSONObject speciesJson = speciesListArray.getJSONObject(speciesIdx);
            Integer speciesId = speciesJson.getInt("server_id");
            IndicatorSpecies indicatorSpecies = taxaDAO.getIndicatorSpecies(speciesId);
            Assert.assertNotNull("species should be non null", indicatorSpecies);

            Assert.assertEquals("wrong taxon group id", indicatorSpecies.getTaxonGroup().getId().intValue()
                    , speciesJson.getInt(ApplicationService.JSON_KEY_TAXON_GROUP_ID));

            Set<Integer> expectedSecondaryGroupIds = new HashSet<Integer>();
            for (TaxonGroup secondaryGroup : indicatorSpecies.getSecondaryGroups()) {
                expectedSecondaryGroupIds.add(secondaryGroup.getId());
            }

            expectedTaxonGroupId.add(speciesJson.getInt(ApplicationService.JSON_KEY_TAXON_GROUP_ID));
            JSONArray secondaryGroupArray = speciesJson.getJSONArray(ApplicationService.JSON_KEY_SECONDARY_TAXON_GROUPS);

            Assert.assertEquals("wrong array size", expectedSecondaryGroupIds.size(), secondaryGroupArray.size());

            for (int groupIdx = 0; groupIdx < secondaryGroupArray.size(); ++groupIdx) {
                Integer groupId = secondaryGroupArray.getInt(groupIdx);
                expectedTaxonGroupId.add(groupId);
                expectedSecondaryGroupIds.remove(groupId);
            }

            Assert.assertEquals("set size should be 0", 0, expectedSecondaryGroupIds.size());
        }

        JSONArray taxonGroupJsonArray = responseObj.getJSONArray(ApplicationService.JSON_KEY_TAXON_GROUPS);

        Assert.assertEquals("wrong array size", expectedTaxonGroupId.size(), taxonGroupJsonArray.size());

        for (int i = 0; i < taxonGroupJsonArray.size(); ++i) {
            JSONObject taxonGroupJson = taxonGroupJsonArray.getJSONObject(i);
            Integer taxonGroupId = taxonGroupJson.getInt("id");
            TaxonGroup tg = taxaDAO.getTaxonGroup(taxonGroupId);
            Assert.assertNotNull("taxon group should be non null", tg);
            Assert.assertEquals("wrong name", tg.getName(), taxonGroupJson.getString("name"));
            Assert.assertEquals("wrong image file name", tg.getImage(), taxonGroupJson.getString("image"));
            Assert.assertEquals("wrong thumb nail image name", tg.getThumbNail(), taxonGroupJson.getString("thumbNail"));
            expectedTaxonGroupId.remove(taxonGroupId);
        }
        Assert.assertEquals("set size should be 0", 0, expectedTaxonGroupId.size());
    }
}
