package au.com.gaiaresources.bdrs.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.threshold.Condition;
import au.com.gaiaresources.bdrs.model.threshold.Operator;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.lsid.LSIDService;
import au.com.gaiaresources.bdrs.util.DateFormatter;

/**
 * Sets up complex data for testing.
 * 
 * Be careful when editing this file as you may break other tests. Don't let that deter
 * you from using this class though :)
 * 
 */
public abstract class AbstractGridControllerTest extends AbstractControllerTest {
    
    @Autowired
    protected SurveyDAO surveyDAO;
    @Autowired
    protected RecordDAO recordDAO;
    @Autowired
    protected MetadataDAO metaDAO;
    @Autowired
    protected LocationService locService;
    @Autowired
    protected LocationDAO locationDAO;
    @Autowired
    protected ThresholdDAO thresholdDAO;
    /**
     * username = 'admin'
     */
    protected User user;
    protected User poweruser;
    
    // a user of Role.USER with no access to any surveys
    protected User foreverAlone;
    
    protected Survey survey1;
    protected Survey survey2;
    protected Survey empty_survey;
    
    /**
     * survey with all record properties hidden and no attributes
     */
    protected Survey nullSurvey;
    /**
     * a record for the null survey
     */
    protected Record nullRecord;
    
    protected Survey singleSiteMultiTaxaSurvey;
    
    /**
     * survey with only record scoped attributes
     */
    protected Survey recordScopedSurvey;
    /**
     * survey with only survey scoped attributes
     */
    protected Survey surveyScopedSurvey;
    
    // unused
    protected Survey singleSiteAllTaxaSurvey;
    // unused
    protected Survey atlasSurvey;
    // unused
    protected Survey yearlySightingSurvey;

    protected CensusMethod taxaCm;
    protected CensusMethod nonTaxaCm;
    protected CensusMethod optTaxaCm;
    
    protected Record r1;
    protected Record r2;
    protected Record r3;
    protected Record r4;
    protected Record r5;
    protected Record r6;
    protected Record r7;
    protected Record r8;
    protected Record r9;
    
    protected List<Record> allRecordList;
    protected List<Record> survey1RecordsList;
    protected List<Record> survey2RecordsList;
    
    protected Map<Survey, List<Record>> recordListMap;
    
    protected List<Record> singleSiteAllTaxaSurveyRecordsList;
    protected List<Record> atlasSurveyRecordsList;
    protected List<Record> yearlySightingSurveyRecordsList;
    
    protected TaxonGroup g1;
    
    protected IndicatorSpecies dropBear;
    protected IndicatorSpecies nyanCat;
    protected IndicatorSpecies hoopSnake;
    protected IndicatorSpecies surfingBird;
    
    protected List<Location> locationList;
    
    /**
     * A list of all survey locations added in the setup
     * If you add a new survey location, you MUST add it to this list
     */
    protected List<Location> allSurveyLocationList = new ArrayList<Location>();
        
    protected Date now;
    
    protected Calendar cal = Calendar.getInstance();
    
    protected GeometryBuilder geomBuilder = new GeometryBuilder();
    
    protected Logger log = Logger.getLogger(getClass());
    
    @Autowired
    protected LSIDService lsidService;
    
    @Before
    public void abstractGridControllerTestSetup() throws ParseException, IOException {
        createCensusMethodForAttributes();
        recordListMap = new HashMap<Survey, List<Record>>();
        
        taxaCm = createCm("taxa cm", "taxa cm desc", Taxonomic.TAXONOMIC, true);
        nonTaxaCm = createCm("non taxa cm", "non taxa cm desc", Taxonomic.NONTAXONOMIC, true);
        optTaxaCm = createCm("opt taxa cm", "opt taxa cm desc", Taxonomic.OPTIONALLYTAXONOMIC, true);
        
        now = getDate(2010, 9, 20);
        
        currentUser = userDAO.getUser("admin");
        
        // Additional User
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "abigail.ambrose@example.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        String registrationKey = passwordEncoder.encodePassword(au.com.gaiaresources.bdrs.util.StringUtils.generateRandomString(10, 50), emailAddr);

        user = userDAO.createUser("user", "Abigail", "Ambrose", emailAddr, encodedPassword, registrationKey, new String[] { Role.USER });
        poweruser = userDAO.createUser("poweruser", "Peter", "Pumpkin", "pp@peterpumpkin.com.au", encodedPassword, registrationKey, new String[] { Role.POWERUSER });
        
        g1 = new TaxonGroup();
        g1.setName("fictionus animus");
        taxaDAO.save(g1);
        
        // SPECIES creation start
        dropBear = createIndicatorSpecies(g1, "dropus bearus", "drop bear", "jimmy ricard", "guid1231239a8d");
        nyanCat = createIndicatorSpecies(g1, "nyanatic catup", "nyan cat", null, "lsid:sdklsdff:s39er:sdksdf:");
        hoopSnake = createIndicatorSpecies(g1, "circulom reptile", "hoop snake", null, null);
        surfingBird = createIndicatorSpecies(g1, "orthonological waverider", "surfing bird", "a guy who names stuff", null);
        
        speciesList = new ArrayList<IndicatorSpecies>();
        speciesList.add(dropBear);
        speciesList.add(nyanCat);
        speciesList.add(hoopSnake);
        speciesList.add(surfingBird);
        
        locationList = new ArrayList<Location>();
        locationList.add(createLocationPoint("loc 1", currentUser, -10, -10, BdrsCoordReferenceSystem.DEFAULT_SRID));
        locationList.add(createLocationPoint("loc 2", currentUser, -90, -180, BdrsCoordReferenceSystem.DEFAULT_SRID));
        locationList.add(createLocationPoint("loc 3", currentUser, 90, 180, BdrsCoordReferenceSystem.DEFAULT_SRID));
        locationList.add(createLocationPoint("loc 4", currentUser, 0, 0, BdrsCoordReferenceSystem.MGA53.getSrid()));
        
        List<CensusMethod> survey1_cmList = new ArrayList<CensusMethod>();
        survey1_cmList.add(taxaCm);
        survey1_cmList.add(nonTaxaCm);
        survey1_cmList.add(optTaxaCm);
        survey1 = createSurvey("Fictionay Animal Survey", "A survey of fictionary animals for testing", 
                               now, null, true, survey1_cmList, SurveyFormRendererType.DEFAULT);
        // give survey 1 some locations !
        List<Location> survey1LocList = new ArrayList<Location>();
        survey1LocList.add(createLocationPoint("survey 1 loc 1", currentUser, -20, -20, BdrsCoordReferenceSystem.MGA54.getSrid()));
        survey1LocList.add(createLocationPoint("survey 1 loc 2", currentUser, -21, -21, BdrsCoordReferenceSystem.MGA55.getSrid()));
        survey1LocList.add(createLocationPoint("survey 1 loc 3", currentUser, -22, -22, BdrsCoordReferenceSystem.MGA56.getSrid()));
        survey1LocList.add(createLocationPoint("survey 1 loc 4", currentUser, -23, -23, BdrsCoordReferenceSystem.MGA57.getSrid()));
        survey1.setLocations(survey1LocList);
        allSurveyLocationList.addAll(survey1LocList);
        
        List<CensusMethod> emptyCensusMethodList = Collections.emptyList();
        survey2 = createSurvey("Generic Survey, no CM", "A survey with no census methods for testing", 
                               getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                               SurveyFormRendererType.DEFAULT);
        
        empty_survey = createSurvey("A Survey with no Records", "A survey with no census methods for testing", 
                               getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                               SurveyFormRendererType.DEFAULT);
        
        // Null survey - a survey with no attributes and all record properties hidden
        nullSurvey = new Survey();
        surveyDAO.save(nullSurvey);
        for (RecordPropertyType rpType : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(nullSurvey, rpType, metaDAO);
            rp.setHidden(true);
        }
        
        nullRecord = new Record();
        nullRecord.setSurvey(nullSurvey);
        nullRecord.setRecordVisibility(RecordVisibility.PUBLIC);
        nullRecord.setUser(currentUser);
        recordDAO.saveRecord(nullRecord);
        
        // survey scoped attributes
        singleSiteMultiTaxaSurvey = createScopedSurvey("single site multi taxa survey", "uses the single site multi taxa form", 
                                    getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                    SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA,
                                    new AttributeScope[] { AttributeScope.RECORD, AttributeScope.SURVEY, 
                                                           AttributeScope.RECORD_MODERATION, 
                                                           AttributeScope.SURVEY_MODERATION}, false);
        
        recordScopedSurvey = createScopedSurvey("single site multi taxa survey", "uses the single site multi taxa form", 
                                     getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                     SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA,
                                     new AttributeScope[] { AttributeScope.RECORD, AttributeScope.RECORD_MODERATION });
        for (RecordPropertyType rpType : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(recordScopedSurvey, rpType, metaDAO);
            if (AttributeScope.SURVEY.equals(rp.getScope())) {
                rp.setHidden(true);
            }
        }

        surveyScopedSurvey = createScopedSurvey("single site multi taxa survey", "uses the single site multi taxa form", 
                                    getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                    SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA,
                                    new AttributeScope[] { AttributeScope.SURVEY, AttributeScope.SURVEY_MODERATION });
        for (RecordPropertyType rpType : RecordPropertyType.values()) {
            RecordProperty rp = new RecordProperty(surveyScopedSurvey, rpType, metaDAO);
            if (AttributeScope.RECORD.equals(rp.getScope())) {
                rp.setHidden(true);
            }
        }
        
        // survey scoped attributes
        singleSiteAllTaxaSurvey = createScopedSurvey("single site all taxa survey", "uses the single site all taxa form", 
                                                 getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                                 SurveyFormRendererType.SINGLE_SITE_ALL_TAXA,
                                                 new AttributeScope[] { AttributeScope.RECORD, AttributeScope.SURVEY, 
                                                                        AttributeScope.RECORD_MODERATION, 
                                                                        AttributeScope.SURVEY_MODERATION }, false);
        
        atlasSurvey = createSurvey("atlas survey", "uses the atlas form", 
                                    getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                    SurveyFormRendererType.ATLAS);
        
        yearlySightingSurvey = createSurvey("A Survey with no Records", "A survey with no census methods for testing", 
                                               getDate(1980, 1, 1), getDate(2000, 1, 1), false, emptyCensusMethodList, 
                                               SurveyFormRendererType.YEARLY_SIGHTINGS);
        
        r1 = createRecord(survey1, null, dropBear, currentUser, 1, getDate(2010, 9, 21), 1, 1, BdrsCoordReferenceSystem.DEFAULT_SRID, RecordVisibility.PUBLIC, true);
        r2 = createRecord(survey1, taxaCm, nyanCat, currentUser, 2, getDate(2010, 9, 22), 2.123456789d, 22.123456789d, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.PUBLIC, true);
        r3 = createRecord(survey1, nonTaxaCm, null, currentUser, null, getDate(2010, 9, 23), 3, 3, BdrsCoordReferenceSystem.MGA52.getSrid(),RecordVisibility.PUBLIC, true);
        r4 = createRecord(survey1, optTaxaCm, surfingBird, currentUser, 4, getDate(2010, 9, 24), 4, 4, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.PUBLIC, true);
        
        // optional items *without* taxonomic information
        r5 = createRecord(survey1, null, null, currentUser, null, getDate(2010, 9, 25), 5, 5, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.PUBLIC, true);
        r6 = createRecord(survey1, optTaxaCm, null, currentUser, null, getDate(2010, 9, 26), 6, 6, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.PUBLIC, true);
        
        r7 = createRecord(survey1, null, hoopSnake, currentUser, 1, getDate(2010, 9, 27), 1, 1, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.CONTROLLED, true);
        r8 = createRecord(survey1, null, dropBear, currentUser, 1, getDate(2010, 9, 28), 1, 1, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.OWNER_ONLY, true);
        
        r9 = createRecord(survey2, null, hoopSnake, currentUser, 10, getDate(2010, 9, 29), -1, -1, BdrsCoordReferenceSystem.DEFAULT_SRID,RecordVisibility.PUBLIC, false);
        
        survey1RecordsList = new ArrayList<Record>(8);
        survey1RecordsList.add(r1);
        survey1RecordsList.add(r2);
        survey1RecordsList.add(r3);
        survey1RecordsList.add(r4);
        survey1RecordsList.add(r5);
        survey1RecordsList.add(r6);
        survey1RecordsList.add(r7);
        survey1RecordsList.add(r8);
        
        survey2RecordsList = new ArrayList<Record>();
        survey2RecordsList.add(r9);
        
        
        recordListMap.put(singleSiteMultiTaxaSurvey, createRecordSet(singleSiteMultiTaxaSurvey, RecordVisibility.PUBLIC));
        recordListMap.put(surveyScopedSurvey, createRecordSet(surveyScopedSurvey, RecordVisibility.PUBLIC));
        recordListMap.put(recordScopedSurvey, createRecordSet(recordScopedSurvey, RecordVisibility.PUBLIC));
        recordListMap.put(survey1, survey1RecordsList);
        recordListMap.put(survey2, survey2RecordsList);
        
        allRecordList = new ArrayList<Record>();
        allRecordList.addAll(survey1RecordsList);
        allRecordList.addAll(survey2RecordsList);
        
        
        // a user who is segregated. and lives alone in the private survey
        foreverAlone = userDAO.createUser("foreverAlone", "faFirst", "faLast", "fa@foreveralone.com.au", "password", "regkey", Role.USER);
        
        // create a private survey
        Survey privateSurvey = new Survey();
        Set<User> privateSurveyUsers = new HashSet<User>();
        privateSurveyUsers.add(user);
        privateSurvey.setUsers(privateSurveyUsers);
        privateSurvey.setPublic(false);
        privateSurvey.setActive(true);
        privateSurvey.setName("Private Survey");
        privateSurvey.setDescription("Private survey description wooo");
        
        Location privSurveyLocation = createLocationPoint("private survey location", user, 80, 80, BdrsCoordReferenceSystem.MGA50.getSrid());
        
        List<Location> privateSurveyLocations = new ArrayList<Location>();
        privateSurveyLocations.add(privSurveyLocation);
        privateSurvey.setLocations(privateSurveyLocations);
        allSurveyLocationList.addAll(privateSurveyLocations);
        
        surveyDAO.save(privateSurvey);
    }
    
    protected List<Record> getInitialRecordList(Survey survey) {
        List<Record> result = this.recordListMap.get(survey);
        if (result == null) {
            throw new IllegalArgumentException("There is no record list for the requested survey");
        }
        return result;
    }
    
    private Location createLocationPoint(String name, User owner, double lat, double lon, int srid) {
        Location loc = new Location();
        loc.setName(name);
        loc.setDescription(name + " description blah blah");
        loc.setUser(owner);
        loc.setLocation(geomBuilder.createPoint(lon, lat));
        locationDAO.save(loc);
        return loc;
    }
    
    /**
     * Originally intended to create a record set for single site tests - can be reused elsewhere.
     * Note that changing this method may cause single site tests to break in confusing ways, especially
     * with how the 'ref items' are set up.
     * 
     * @param survey - the survey to create the test record set for
     * @param recVis - the record visibility to assign to the created records
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    private List<Record> createRecordSet(Survey survey, RecordVisibility recVis) throws ParseException, IOException {
        
        Date date1 = getDate(2010, 8, 8);
        // using a different date to mark these items as from a different single site multi taxa form.
        Date date2 = getDate(2010, 8, 9);
        
        // THE ORDER OF THE INSERT MATTERS, DON'T CHANGE IT!
        List<Record> result = new ArrayList<Record>();
        
        // ref item 1
        result.add(createRecord(survey, null, hoopSnake, currentUser, 2, date1, 30, 30, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, true));
        // ref item 2
        result.add(createRecord(survey, null, dropBear, currentUser, 3, date1, 30, 30, 28350, recVis, true));
        
        result.add(createRecord(survey, null, dropBear, currentUser, 4, date1, 31, 31, 28351, recVis, true));
        result.add(createRecord(survey, null, nyanCat, currentUser, 5, date1, 30, 30, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, false));
        result.add(createRecord(survey, null, hoopSnake, currentUser, 6, date2, 30, 30, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, true));
        result.add(createRecord(survey, null, dropBear, currentUser, 7, date2, 30, 30, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, true));
        result.add(createRecord(survey, null, dropBear, currentUser, 8, date2, 31, 31, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, true));
        result.add(createRecord(survey, null, nyanCat, currentUser, 9, date2, 30, 30, BdrsCoordReferenceSystem.DEFAULT_SRID, recVis, false));
        return result;
    }
    
    private IndicatorSpecies createIndicatorSpecies(TaxonGroup group, String sciName, String commonName, String author, String guid) {
        IndicatorSpecies species = new IndicatorSpecies();
        species.setTaxonGroup(group);
        species.setScientificName(sciName);
        species.setCommonName(commonName);
        
        if (author != null) {
            species.setScientificNameAndAuthor(sciName + " - " + author);
        }
        if (guid != null) {
            species.setSourceId(guid);
        }
        
        species.setRunThreshold(false);
        
        return taxaDAO.save(species);
    }
    
    protected Date overlayDate(Date src, Date target, int[] fieldsToCopy) {
        Calendar srcCal = Calendar.getInstance();
        Calendar targetCal = Calendar.getInstance();
        srcCal.setTime(src);
        targetCal.setTime(target);
        for (int fieldCode : fieldsToCopy) {
            targetCal.set(fieldCode, srcCal.get(fieldCode));
        }
        return targetCal.getTime();
    }
    
    private Record createRecord(Survey survey, CensusMethod cm, IndicatorSpecies sp, 
            User u, Integer count, Date now, double lon, double lat, int srid, RecordVisibility vis, boolean generateAv) throws ParseException, IOException {
        Record r = new Record();
        r.setSurvey(survey);
        r.setCensusMethod(cm);
        
        if (!getRecordProperty(survey, RecordPropertyType.SPECIES).isHidden()) {
            r.setSpecies(sp);    
        }
        
        r.setUser(u);
        
        if (!getRecordProperty(survey, RecordPropertyType.WHEN).isHidden()) {
            r.setWhen(now);
        }
        
        r.setLastDate(now);
        
        if (!getRecordProperty(survey, RecordPropertyType.NUMBER).isHidden()) {
            r.setNumber(count);
        }
        
        r.setRecordVisibility(vis);
        
        if (!getRecordProperty(survey, RecordPropertyType.POINT).isHidden()) {
            r.setGeometry(geomBuilder.createPoint(lon, lat));    
        }
        
        r.setRunThreshold(false);
        
        Set<AttributeValue> avSet = new HashSet<AttributeValue>();
        
        int seed = 0;
        for (Attribute a : survey.getAttributes()) {
            avSet.add(generateAttributeValue(a, seed++, generateAv, true, null, "", null));
        }
        
        if (cm != null) {
            for (Attribute a : cm.getAttributes()) {
                avSet.add(generateAttributeValue(a, seed++, generateAv, true, null, "", null));
            }
        }

        r.setAttributes(avSet);
        
        return recordDAO.saveRecord(r);
    }
    
    private RecordProperty getRecordProperty(Survey s, RecordPropertyType rpType) {
        return new RecordProperty(s, rpType, metaDAO);
    }
    
    private Survey createSurvey(String name, String desc, Date startDate, Date endDate, boolean attrRequired, List<CensusMethod> cmList, SurveyFormRendererType renderType) {
        Survey surv = new Survey();
        surv.setName(name);
        surv.setDescription(desc);
        surv.setStartDate(now);
        surv.setActive(true);
        surv.setPublic(true);
        
        List<Attribute> attrList = createAttrList("sattr", attrRequired, AttributeScope.RECORD);
        surv.setAttributes(attrList);

        surv.setCensusMethods(cmList);
        
        surv.setRunThreshold(false);
        
        Metadata rendererTypeMetadata = surv.setFormRendererType(renderType);
        metaDAO.save(rendererTypeMetadata);
        
        return surveyDAO.save(surv);
    }

    /**
     * Creates a survey with survey scoped attributes
     * 
     * @param name
     * @param desc
     * @param startDate
     * @param endDate
     * @param attrRequired
     * @param cmList
     * @param renderType
     * @return
     */
    private Survey createScopedSurvey(String name, String desc, Date startDate, Date endDate, boolean attrRequired, List<CensusMethod> cmList, SurveyFormRendererType renderType, AttributeScope[] scopeArray) {
        return createScopedSurvey(name, desc, startDate, endDate, attrRequired, cmList, renderType, scopeArray, true);
    }
    
    private Survey createScopedSurvey(String name, String desc, Date startDate, Date endDate, boolean attrRequired, List<CensusMethod> cmList, SurveyFormRendererType renderType, AttributeScope[] scopeArray, boolean includeCMAtts) {
        
        Survey surv = new Survey();
        surv.setName(name);
        surv.setDescription(desc);
        surv.setStartDate(now);
        surv.setActive(true);
        surv.setPublic(true);
        
        List<Attribute> attrList = new ArrayList<Attribute>();
        
        int suffix = 1;
        
        for (AttributeScope scope : scopeArray) {
            attrList.addAll(createAttrList(String.format(scope.toString()+"_attribute_%d", suffix++), attrRequired, scope, includeCMAtts, false));
        }
        
        surv.setAttributes(attrList);

        surv.setCensusMethods(cmList);
        
        surv.setRunThreshold(false);
        
        Metadata rendererTypeMetadata = surv.setFormRendererType(renderType);
        metaDAO.save(rendererTypeMetadata);
        
        return surveyDAO.save(surv);
    }
    protected void assertRecordAttributeValue(Record rec, Map<Attribute, Object> attributeValueMap) {
        for (Entry<Attribute, Object> entry : attributeValueMap.entrySet()) {
            AttributeValue av = this.getAttributeValueByAttributeId(rec.getAttributes(), entry.getKey().getId());
            Assert.assertNotNull("An attribute value with attribute id = " + entry.getKey().getId() + " should exist in ref item", av);
            this.assertAttributeValue(av, entry.getValue());
        }
    }
    
    /**
     * Used to generate test data for record properties...
     * 
     * @param rec - the record to gen data for. It may not be used if assign is false
     * @param survey - the survey for which to create data for - used to work out RecordProperty settings
     * @param type - the record property type
     * @param seed - seed for random data
     * @param assign - bool, whether to assign the generated data to the rec parameter
     * @return
     */
    protected String genRandomRecordPropertyValue(Record rec, Survey survey, RecordPropertyType type, int seed, boolean assign) {
        
        RecordProperty rp = this.getRecordProperty(survey, type);
        if (rp.isHidden()) {
            return "";
        }
        
        switch (type) {
        case NUMBER:
            if (assign) {
                rec.setNumber(seed);
            }
            return Integer.toString(seed);
        case SPECIES:
        {
            IndicatorSpecies sp = speciesList.get(seed%speciesList.size());
            while (sp.equals(rec.getSpecies())) {
                sp = speciesList.get(++seed%speciesList.size());
            }
            
            if (assign) {
                rec.setSpecies(sp);
            }
            // returns the species id as a string...
            return sp.getId().toString();
        }
            
        case LOCATION:
        {
            Location loc = locationList.get(seed%locationList.size());
            while (loc.equals(rec.getLocation())) {
                loc = locationList.get(++seed%locationList.size());
            }
            
            if (assign) {
                rec.setLocation(loc);
            }
            // returns the species id as a string...
            return loc.getId().toString();
        }
        case POINT:
        {
            // return string, comma delimited lat,lon
            int lat = seed++%90;
            int lon = seed++%180;
            
            if (assign) {
                rec.setGeometry(geomBuilder.createPoint(lon, lat));
            }
            // not handled, return nothing for now 
            return String.format("%d,%d", lat, lon);
        }
        case ACCURACY:
            if (assign) {
                rec.setAccuracyInMeters(new Double(seed));
            }
            return Integer.toString(seed);
        case WHEN:
        {
            Date d = getDate(2010, 10, seed%30);
            if (assign) {
                if (rec.getWhen() == null) {
                    rec.setWhen(d);    
                } else {
                    Date newDate = overlayDate(d, rec.getWhen(), new int[] { Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH });
                    rec.setWhen(newDate);
                }
            }
            return DateFormatter.format(d, DateFormatter.DAY_MONTH_YEAR);
        }
        case TIME:
        {
            int hour = seed++%24;
            int minute = seed++%60;
            
            // not sure what we're meant to do with assign ...
            if (assign) {
                if (rec.getWhen() == null) {
                    rec.setWhen(this.getDate(0, 0, 0, hour, minute));
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(rec.getWhen());
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    rec.setWhen(cal.getTime());
                }
            }
            return String.format("%d:%d", hour, minute);
        }
        case NOTES:
        {
            String notes = String.format("notes seed = %d", seed);
            if (assign) {
                rec.setNotes(notes);    
            }
            return notes;
        }
        case CREATED:
        case UPDATED:
        {
             return "I don't get used";
        }
        default:
                Assert.fail("Record property type : " + type + " not handled, fix it!");
                return null;
        }
    }
    
    private CensusMethod createCm(String name, String desc, Taxonomic tax, boolean attrRequired) {
        CensusMethod cm = new CensusMethod();
        cm.setName(name);
        cm.setDescription(desc);
        cm.setTaxonomic(tax);
        
        List<Attribute> attrList = createAttrList("cattr", attrRequired, AttributeScope.RECORD, false, false);
        cm.setAttributes(attrList);
        
        cm.setRunThreshold(false);
        
        return cmDAO.save(cm);
    }
    
    /**
     * assert whether the record has the expected values.
     * 
     * if param expectedValue is not of the desired type, exceptions will be thrown
     * 
     * @param rec - the record to assert on
     * @param type - the record property type to assert on
     * @param expectedValue - the string representation of the expected value
     */
    protected void assertRecordPropertyValue(Record rec, Map<RecordPropertyType, String> expectedValueMap) {
        
        if (rec == null) {
            Assert.fail("record cannot be null");
        }
        
        for (Entry<RecordPropertyType, String> entry : expectedValueMap.entrySet()) {
            RecordPropertyType type = entry.getKey();
            String expectedValue = entry.getValue();
            
            switch (type) {
            case NUMBER:
                if (expectedValue == null) {
                    Assert.assertNull("record.number expected to be null", rec.getNumber());
                } else {
                    Assert.assertNotNull("record.number c", rec.getNumber());
                    Assert.assertEquals("record.number field does not match", rec.getNumber().intValue(), Integer.parseInt(expectedValue));
                }
                break;
            case SPECIES:
                if (expectedValue == null) {
                    Assert.assertNull("record.species expected to be null", rec.getSpecies());
                } else {
                    Assert.assertNotNull("record.species cannot be null", rec.getSpecies());
                    Assert.assertEquals("record.species id does not match", rec.getSpecies().getId().intValue(), Integer.parseInt(expectedValue));
                }
                break;
            case LOCATION:
                if (expectedValue == null) {
                    Assert.assertNull("record.location expected to be null", rec.getLocation());
                } else {
                    Assert.assertNotNull("record.location cannot be null", rec.getLocation());
                    Assert.assertEquals("record.location id does not match", rec.getLocation().getId().intValue(), Integer.parseInt(expectedValue));
                }
                break;
                
            case POINT:
            
                if (expectedValue == null) {
                    Assert.assertNull("record.geometry expected to be null", rec.getGeometry());
                } else {
                    Assert.assertNotNull("record.geometry cannot be null", rec.getGeometry());
                    // expected value = lat,lon comma delimited
                    String[] latLonSplit = expectedValue.split(",");
                    Double lat = Double.parseDouble(latLonSplit[0]);
                    Double lon = Double.parseDouble(latLonSplit[1]);
                    Assert.assertEquals("record.latitude does not match", lat, rec.getLatitude(), DEFAULT_TOLERANCE);
                    Assert.assertEquals("record.longitude does not match", lon, rec.getLongitude(), DEFAULT_TOLERANCE);
                }
                break;
            
            case ACCURACY:
                if (expectedValue == null) {
                    Assert.assertNull("record.accuracy expected to be null", rec.getAccuracyInMeters());
                } else {
                    Assert.assertNotNull("record.accracy cannot be null", rec.getAccuracyInMeters());
                    Assert.assertEquals("record.accuracy does not match", Double.parseDouble(expectedValue), rec.getAccuracyInMeters(), DEFAULT_TOLERANCE);
                }
                break;
            case WHEN:
            {
                String dateExpectedValue = expectedValueMap.get(RecordPropertyType.WHEN);
                String timeExpectedValue = expectedValueMap.get(RecordPropertyType.TIME);
                
                if (dateExpectedValue == null && timeExpectedValue == null) {
                    Assert.assertNull("record.when expected to be null", rec.getWhen());
                } else if (dateExpectedValue != null && timeExpectedValue == null) {
                    Assert.assertNotNull("record.when cannot be null", rec.getWhen());
                    Assert.assertEquals("record.when does not match", DateFormatter.parse(expectedValue, DateFormatter.DAY_MONTH_YEAR), rec.getWhen());
                } else if (dateExpectedValue != null && timeExpectedValue == null) {
                    Assert.assertNotNull("record.when cannot be null", rec.getWhen());
                    // just make sure hour and minute parts of the when field matches.
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(rec.getWhen());
                    String[] timeSplit = timeExpectedValue.split(":");
                    Assert.assertEquals("hour does not match", timeSplit[0], Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
                    Assert.assertEquals("minute does not match", timeSplit[1], Integer.toString(cal.get(Calendar.MINUTE)));
                } else if (dateExpectedValue != null && timeExpectedValue != null) {
                    Assert.assertNotNull("record.when cannot be null", rec.getWhen());
                    // 'concat' the time fields and compare...
                    String[] timeSplit = timeExpectedValue.split(":");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(DateFormatter.parse(expectedValue, DateFormatter.DAY_MONTH_YEAR));
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
                    Assert.assertEquals("when date does not match", cal.getTime(), rec.getWhen());
                }
                break;
            }
            case TIME:
            {
                // do nothing
                break;
            }
            case NOTES:
            {
                if (expectedValue == null) {
                    Assert.assertNull("record.notes expected to be null", rec.getNotes());
                } else {
                    Assert.assertNotNull("record.notes cannot be null", rec.getNotes());
                    Assert.assertEquals("record.notes does not match", expectedValue, rec.getNotes());
                }
                break;
            }
            case CREATED:
            case UPDATED:
                // These are read only values.
                break;

                default:
                    Assert.fail("Record property type : " + type + " not handled, fix it!");
                    break;
            }
        }
    }
    
    /**
     * pass in a string representation of the attribute id
     * 
     * @param avSet - the attribute set to search over
     * @param strId - the string representation for the attribute id
     * @return
     */
    protected AttributeValue getAttributeValueByAttributeId(Set<AttributeValue> avSet, String strId) {
        Integer attrId = Integer.parseInt(strId);
        return getAttributeValueByAttributeId(avSet, attrId);
    }
    
    /**
     * pass in the integer representation of the attribute id
     * 
     * @param avSet - the attribute value set to search over
     * @param attrId - the id for the attribute 
     * @return
     */
    protected AttributeValue getAttributeValueByAttributeId(Set<AttributeValue> avSet, Integer attrId) {
        for (AttributeValue av : avSet) {
            if (av.getAttribute().getId().equals(attrId)) {
                return av;
            }
        }
        return null;
    }
    
    protected List<AttributeValue> getAttributeValuesByScope(Record rec, AttributeScope scope) {
        List<AttributeValue> result = new ArrayList<AttributeValue>();
        for (AttributeValue av : rec.getAttributes()) {
            if (scope == null && av.getAttribute().getScope() == null || 
                    (scope != null && scope.equals(av.getAttribute().getScope()))) {
                result.add(av);
            }
        }
        return result;
    }
    
    protected List<RecordProperty> getRecordProperty(Survey survey, AttributeScope scope) {
        List<RecordProperty> result = new ArrayList<RecordProperty>();
        // survey can be null for census method attribute value records
        if (survey != null) {
            for (RecordPropertyType type : RecordPropertyType.values()) {
                RecordProperty recordProperty = new RecordProperty(survey, type,
                        metaDAO);
                if (scope == null || scope.equals(recordProperty.getScope())) {
                    result.add(recordProperty);
                }
            }
        }
        return result;
    }
    
    protected List<Attribute> getAttributeByScope(Survey survey, AttributeScope scope) {
        List<Attribute> result = new ArrayList<Attribute>();
        for (Attribute a : survey.getAttributes()) {
            if (a.getScope().equals(scope)) {
                result.add(a);
            }
        }
        return result;
    }
    
    /**
     * Helper method to create {@link Threshold} for tests.
     * @param className Record, Survey, Taxa, or CensusMethod
     * @param index Number to apply to the end of the name string for differentiation of {@link Threshold}
     * @param propertyPath the property path for the {@link Threshold}
     * @param operator the {@link Operator} for the {@link Threshold}
     * @param values the values to assign to the {@link Threshold}
     * @return a newly created {@link Threshold}
     */
    public Threshold createThreshold(String className, int index, String propertyPath, Operator operator, String[] values) {
        Threshold threshold = new Threshold();
        Portal portal = getRequestContext().getPortal();
        threshold.setClassName(className);
        threshold.setEnabled(true);
        threshold.setName("Test Threshold "+index);
        threshold.setDescription("Threshold for testing.");
        threshold.setPortal(portal);
        
        List<Condition> conditionList = new ArrayList<Condition>();
        // set the conditions
        Condition condition = new Condition();
        condition.setClassName(className);
        condition.setPropertyPath(propertyPath);
        condition.setKeyOperator(null);
        condition.setValueOperator(operator);
        condition.setKey(null);
        condition.setValue(values);
        condition.setPortal(portal);
        
        condition = thresholdDAO.save(sesh, condition);
        conditionList.add(condition);
        threshold.setConditions(conditionList);
        
        // don't need to set threshold actions as only conditions are used 
        // for testing if a threshold is present on an attribute
        
        return thresholdDAO.save(sesh, threshold);
    }
}
