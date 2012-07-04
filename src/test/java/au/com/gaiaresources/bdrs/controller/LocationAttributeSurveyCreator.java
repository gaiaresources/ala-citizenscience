package au.com.gaiaresources.bdrs.controller;

import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import junit.framework.Assert;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Creates a series of Surveys, CensusMethods and Taxonomy each containing all attributes and location attributes
 * where appropriate.
 */
public class LocationAttributeSurveyCreator {
    private DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    private TaxonGroup taxonGroupBirds;
    private TaxonGroup taxonGroupFrogs;
    private IndicatorSpecies speciesA;
    private IndicatorSpecies speciesB;
    private IndicatorSpecies speciesC;
    private CensusMethod methodA;
    private CensusMethod methodC;
    private CensusMethod methodB;
    private CensusMethod attrCM;
    private User user;
    private User admin;
    private Date dateA;
    private Date dateB;

    private RecordDAO recordDAO;
    private TaxaDAO taxaDAO;
    private CensusMethodDAO methodDAO;
    private UserDAO userDAO;
    private MetadataDAO metadataDAO;
    private SurveyDAO surveyDAO;
    private PreferenceDAO preferenceDAO;
    private LocationDAO locationDAO;

    private LocationService locationService;
    private FileService fileService;

    /**
     * Total number of records created by setup.
     */
    private int recordCount;
    /**
     * Total number of records created for each survey by setup.
     */
    private int surveyRecordCount;
    private Map<User, Integer> userRecordCount = new HashMap<User, Integer>();
    /**
     * Total number of records created for each census method.
     */
    private int methodRecordCount;
    /**
     * Total number of records created for each indicator species.
     */
    private int taxonRecordCount;

    /**
     * Total number of locations created by setup.
     */
    private int locationCount;
    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;
    private static final int DEFAULT_MAX_IMAGE_HEIGHT = 768;
    private static final int DEFAULT_MIN_IMAGE_WIDTH = 800;
    private static final int DEFAULT_MIN_IMAGE_HEIGHT = 600;

    /**
     * Creates a new instance.
     *
     * @param surveyDAO       provides database access to Surveys
     * @param locationDAO     provides database access to Locations
     * @param locationService provides facilities to convert WKT strings to Geometry instances.
     * @param methodDAO       provides database access to Census Methods.
     * @param userDAO         provides database access to Users.
     * @param taxaDAO         provides database access to Indicator Species.
     * @param recordDAO       provides database access to Records.
     * @param metadataDAO     provides database access to Metadata
     * @param preferenceDAO   provides database access to Preferences.
     * @param fileService     provides access to the application file store.
     */
    public LocationAttributeSurveyCreator(SurveyDAO surveyDAO, LocationDAO locationDAO, LocationService locationService,
                                          CensusMethodDAO methodDAO, UserDAO userDAO, TaxaDAO taxaDAO,
                                          RecordDAO recordDAO, MetadataDAO metadataDAO, PreferenceDAO preferenceDAO,
                                          FileService fileService) {

        this.surveyDAO = surveyDAO;
        this.preferenceDAO = preferenceDAO;
        this.locationDAO = locationDAO;
        this.locationService = locationService;
        this.metadataDAO = metadataDAO;
        this.taxaDAO = taxaDAO;
        this.recordDAO = recordDAO;
        this.methodDAO = methodDAO;
        this.userDAO = userDAO;
        this.fileService = fileService;
    }

    /**
     * Creates the test dataset.
     *
     * @param createRecords true if records should be created, false otherwise.
     * @throws Exception thrown if there has been an error creating the test data.
     */
    public void create(boolean createRecords) throws Exception {
        attrCM = createCensusMethodForAttributes();
        dateA = dateFormat.parse("27 Jun 2004");
        dateB = dateFormat.parse("02 Oct 2005");

        taxonGroupBirds = new TaxonGroup();
        taxonGroupBirds.setName("Birds");
        taxonGroupBirds = taxaDAO.save(taxonGroupBirds);

        taxonGroupFrogs = new TaxonGroup();
        taxonGroupFrogs.setName("Frogs");
        taxonGroupFrogs = taxaDAO.save(taxonGroupFrogs);

        List<Attribute> taxonGroupAttributeList;
        Attribute groupAttr;
        for (TaxonGroup group : new TaxonGroup[]{taxonGroupBirds, taxonGroupFrogs}) {
            taxonGroupAttributeList = new ArrayList<Attribute>();
            for (boolean isTag : new boolean[]{true, false}) {
                for (AttributeType attrType : AttributeType.values()) {
                    groupAttr = new Attribute();
                    groupAttr.setRequired(true);
                    groupAttr.setName(group.getName() + "_"
                            + attrType.toString() + "_isTag" + isTag);
                    groupAttr.setDescription(group.getName() + "_"
                            + attrType.toString() + "_isTag" + isTag);
                    groupAttr.setTypeCode(attrType.getCode());
                    groupAttr.setScope(null);
                    groupAttr.setTag(isTag);

                    if (AttributeType.STRING_WITH_VALID_VALUES.equals(attrType)
                            || AttributeType.MULTI_CHECKBOX.equals(attrType)
                            || AttributeType.MULTI_SELECT.equals(attrType)) {
                        List<AttributeOption> optionList = new ArrayList<AttributeOption>();
                        for (int i = 0; i < 4; i++) {
                            AttributeOption opt = new AttributeOption();
                            opt.setValue(String.format("Option %d", i));
                            opt = taxaDAO.save(opt);
                            optionList.add(opt);
                        }
                        groupAttr.setOptions(optionList);
                    } else if (AttributeType.INTEGER_WITH_RANGE.equals(attrType)) {
                        List<AttributeOption> rangeList = new ArrayList<AttributeOption>();
                        AttributeOption upper = new AttributeOption();
                        AttributeOption lower = new AttributeOption();
                        lower.setValue("100");
                        upper.setValue("200");
                        rangeList.add(taxaDAO.save(lower));
                        rangeList.add(taxaDAO.save(upper));
                        groupAttr.setOptions(rangeList);
                    }

                    groupAttr = taxaDAO.save(groupAttr);
                    taxonGroupAttributeList.add(groupAttr);
                }
            }
            group.setAttributes(taxonGroupAttributeList);
            taxaDAO.save(group);
        }

        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroupBirds);
        speciesA = taxaDAO.save(speciesA);

        speciesB = new IndicatorSpecies();
        speciesB.setCommonName("Indicator Species B");
        speciesB.setScientificName("Indicator Species B");
        speciesB.setTaxonGroup(taxonGroupBirds);
        speciesB = taxaDAO.save(speciesB);

        speciesC = new IndicatorSpecies();
        speciesC.setCommonName("Indicator Species C");
        speciesC.setScientificName("Indicator Species C");
        speciesC.setTaxonGroup(taxonGroupFrogs);
        speciesC = taxaDAO.save(speciesC);

        HashSet<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(speciesA);
        speciesSet.add(speciesB);
        speciesSet.add(speciesC);

        methodA = new CensusMethod();
        methodA.setName("Method A");
        methodA.setTaxonomic(Taxonomic.TAXONOMIC);
        methodA.setType("Type X");
        methodA = methodDAO.save(methodA);

        methodB = new CensusMethod();
        methodB.setName("Method B");
        methodB.setTaxonomic(Taxonomic.OPTIONALLYTAXONOMIC);
        methodB.setType("Type X");
        methodB = methodDAO.save(methodB);

        methodC = new CensusMethod();
        methodC.setName("Method C");
        methodC.setTaxonomic(Taxonomic.NONTAXONOMIC);
        methodC.setType("Type Y");
        methodC = methodDAO.save(methodC);

        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String emailAddr = "abigail.ambrose@example.com";
        String encodedPassword = passwordEncoder.encodePassword("password", null);
        String registrationKey = passwordEncoder.encodePassword(au.com.gaiaresources.bdrs.util.StringUtils.generateRandomString(10, 50), emailAddr);
        locationCount = 0;
        
        user = userDAO.createUser("testuser", "Abigail", "Ambrose", emailAddr, encodedPassword, registrationKey, new String[]{Role.USER});
        createUserLocation(user);
        locationCount++;
        
        admin = userDAO.getUser("admin");
        createUserLocation(admin);
        locationCount++;
        
        int surveyIndex = 1;
        for (CensusMethod method : new CensusMethod[]{methodA, methodB, methodC, null}) {
            List<Attribute> attributeList = new ArrayList<Attribute>();
            Attribute attr;
            for (AttributeType attrType : AttributeType.values()) {
                for (AttributeScope scope : new AttributeScope[]{
                        AttributeScope.RECORD, AttributeScope.SURVEY,
                        AttributeScope.LOCATION,
                        AttributeScope.RECORD_MODERATION,
                        AttributeScope.SURVEY_MODERATION, null}) {

                    attr = new Attribute();
                    String scopeName = scope == null ? "null" : scope.toString();
                    attr.setDescription(scopeName + " " + attrType.toString() + " description");
                    attr.setRequired(true);
                    attr.setName(scopeName + " " + attrType.toString());
                    attr.setTypeCode(attrType.getCode());
                    attr.setScope(scope);
                    attr.setTag(false);

                    if (AttributeType.STRING_WITH_VALID_VALUES.equals(attrType)
                            || AttributeType.MULTI_CHECKBOX.equals(attrType)
                            || AttributeType.MULTI_SELECT.equals(attrType)) {
                        List<AttributeOption> optionList = new ArrayList<AttributeOption>();
                        for (int i = 0; i < 4; i++) {
                            AttributeOption opt = new AttributeOption();
                            opt.setValue(String.format("Option %d", i));
                            opt = taxaDAO.save(opt);
                            optionList.add(opt);
                        }
                        attr.setOptions(optionList);
                    } else if (AttributeType.INTEGER_WITH_RANGE.equals(attrType)) {
                        List<AttributeOption> rangeList = new ArrayList<AttributeOption>();
                        AttributeOption upper = new AttributeOption();
                        AttributeOption lower = new AttributeOption();
                        lower.setValue("100");
                        upper.setValue("200");
                        rangeList.add(taxaDAO.save(lower));
                        rangeList.add(taxaDAO.save(upper));
                        attr.setOptions(rangeList);
                    } else if (AttributeType.isCensusMethodType(attrType)) {
                        attr.setCensusMethod(attrCM);
                    }

                    attr = taxaDAO.save(attr);
                    attributeList.add(attr);
                }
            }

            Survey survey = new Survey();
            survey.setName(String.format("Survey %d", surveyIndex));
            survey.setActive(true);
            survey.setStartDate(new Date());
            survey.setDescription(String.format("Survey %d", surveyIndex)
                    + " Description");

            Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            metadataDAO.save(md);

            survey.setAttributes(attributeList);
            survey.setSpecies(new HashSet<IndicatorSpecies>(speciesSet));
            survey.getCensusMethods().add(method);

            survey = surveyDAO.save(survey);
            survey.setLocations(createLocations(survey));
            survey = surveyDAO.save(survey);
            surveyRecordCount = 0;
            taxonRecordCount = 0;
            methodRecordCount = 0;

            if (createRecords) {
                for (Location loc : survey.getLocations()) {
                    for (IndicatorSpecies species : survey.getSpecies()) {
                        for (CensusMethod cm : survey.getCensusMethods()) {
                            for (User u : new User[]{admin, user}) {
                                createRecord(survey, cm, loc, species, u);

                                recordCount++;
                                surveyRecordCount++;

                                int thisUserRecordCount = 1;
                                if (userRecordCount.containsKey(u)) {
                                    thisUserRecordCount = userRecordCount.get(u) + 1;
                                }
                                userRecordCount.put(u, thisUserRecordCount);

                                methodRecordCount++;
                                taxonRecordCount++;
                            }
                        }
                    }
                }
            }
            surveyIndex += 1;
        }

        RequestContextHolder.getContext().getHibernate().flush();

        Preference mapViewDefault = preferenceDAO.getPreferenceByKey(Preference.ADVANCED_REVIEW_DEFAULT_VIEW_KEY);
        mapViewDefault.setValue(Boolean.toString(true));
    }

    private Location createUserLocation(User user) {
        Location loc = new Location();
        loc.setName(String.format("Location %s", user.getName()));
        loc.setLocation(locationService.createPoint(-40.58, 153.1));
        loc = locationDAO.save(loc);
        return loc;
    }

    /**
     * Creates locations for the specified survey.
     *
     * @param survey the survey that will contain the locations created.
     * @return the locations that were created.
     *         * @Exception thrown if there has been an error saving image or file data for a location
     */
    private List<Location> createLocations(Survey survey) throws IOException {
        List<Location> locList = new ArrayList<Location>();
        for (int i = 0; i < 3; i++) {
            locList.add(createLocation(survey, i));
            locationCount++;
        }
        return locList;
    }

    /**
     * Creates a new location for the specified survey named using the provided index.
     *
     * @param survey the survey that will contain the location.
     * @param index  an index used to assist naming the new location.
     * @return the newly created location.
     * @throws IOException thrown if there was a error storing image or file attributes.
     */
    private Location createLocation(Survey survey, int index) throws IOException {
        Location loc = new Location();
        loc.setName(String.format("Location %d", index));
        loc.setLocation(locationService.createPoint(-40.58, 153.1));
        for (Attribute attr : survey.getAttributes()) {
            if (AttributeScope.LOCATION.equals(attr.getScope())) {
                List<AttributeOption> opts = attr.getOptions();
                byte[] fileData = null;
                AttributeValue attrVal = new AttributeValue();
                attrVal.setAttribute(attr);
                switch (attr.getType()) {
                    case INTEGER:
                        Integer i = Integer.valueOf(123);
                        attrVal.setNumericValue(new BigDecimal(i));
                        attrVal.setStringValue(i.toString());
                        break;
                    case INTEGER_WITH_RANGE:
                        String intStr = attr.getOptions().iterator().next().getValue();
                        attrVal.setNumericValue(new BigDecimal(
                                Integer.parseInt(intStr)));
                        attrVal.setStringValue(intStr);
                        break;
                    case DECIMAL:
                        Double d = new Double(123);
                        attrVal.setNumericValue(new BigDecimal(d));
                        attrVal.setStringValue(d.toString());
                        break;
                    case DATE:
                        Date date = new Date(System.currentTimeMillis());
                        attrVal.setDateValue(date);
                        attrVal.setStringValue(dateFormat.format(date));
                        break;
                    case STRING_AUTOCOMPLETE:
                    case STRING:
                        attrVal.setStringValue("This is a test string record attribute");
                        break;
                    case TEXT:
                        attrVal.setStringValue("This is a test text record attribute");
                        break;
                    case REGEX:
                    case BARCODE:
                        attrVal.setStringValue("#454545");
                        break;
                    case TIME:
                        attrVal.setStringValue("12:34");
                        break;
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                        attrVal.setStringValue("<hr/>");
                        break;
                    case STRING_WITH_VALID_VALUES:
                        attrVal.setStringValue(opts.iterator().next().getValue());
                        break;
                    case MULTI_CHECKBOX:
                        attrVal.setMultiCheckboxValue(new String[]{
                                opts.get(0).getValue(), opts.get(1).getValue()});
                        break;
                    case MULTI_SELECT:
                        attrVal.setMultiCheckboxValue(new String[]{
                                opts.get(0).getValue(), opts.get(1).getValue()});
                        break;
                    case SINGLE_CHECKBOX:
                        attrVal.setBooleanValue(Boolean.TRUE.toString());
                        break;
                    case AUDIO:
                    case FILE:
                        attrVal.setStringValue("testDataFile.dat");
                        fileData = createImage(-1, -1, attrVal.getStringValue());
                        break;
                    case IMAGE:
                        attrVal.setStringValue("testImgFile.png");
                        fileData = createImage(-1, -1, attrVal.getStringValue());
                        break;
                    case SPECIES:
                        attrVal.setSpecies(speciesA);
                        attrVal.setStringValue(speciesA.getScientificName());
                        break;
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        // census method types should add a record to the attribute value
                        break;
                    default:
                        Assert.assertTrue("Unknown Attribute Type: "
                                + attr.getType().toString(), false);
                        break;
                }
                attrVal = recordDAO.saveAttributeValue(attrVal);
                if (fileData != null) {
                    fileService.createFile(attrVal.getClass(), attrVal.getId(), attrVal.getStringValue(), fileData);
                }
                loc.getAttributes().add(attrVal);
            }
        }

        loc = locationDAO.save(loc);
        return loc;
    }

    /**
     * Creates a new Record.
     *
     * @param survey  the survey that owns this record.
     * @param cm      the censusmethod for this record
     * @param loc     the location of this record.
     * @param species the species that was recorded.
     * @param user    the user who created the record.
     * @return the newly created record.
     * @throws IOException thrown if there was an error saving file or image attributes for the record.
     */
    private Record createRecord(Survey survey, CensusMethod cm, Location loc,
                                IndicatorSpecies species, User user) throws IOException {
        return createRecord(survey, cm, loc, species, user, RecordVisibility.PUBLIC);
    }

    /**
     * Creates a new Record.
     *
     * @param survey     the survey that owns this record.
     * @param cm         the censusmethod for this record
     * @param loc        the location of this record.
     * @param species    the species that was recorded.
     * @param user       the user who created the record.
     * @param visibility the visibility of the record to be created.
     * @return the newly created record
     * @throws IOException thrown if there was an error saving file or image attributes for the record.
     */
    public Record createRecord(Survey survey, CensusMethod cm, Location loc,
                               IndicatorSpecies species, User user, RecordVisibility visibility) throws IOException {
        Date recDate = admin.equals(user) ? dateA : dateB;

        Record record = new Record();
        record.setSurvey(survey);
        if (cm != null && Taxonomic.NONTAXONOMIC.equals(cm.getTaxonomic())) {
            record.setSpecies(null);
        } else {
            record.setSpecies(species);
        }
        record.setCensusMethod(cm);
        record.setUser(user);
        record.setLocation(loc);
        record.setPoint(locationService.createPoint(-32.42, 154.15));
        record.setHeld(false);
        record.setWhen(recDate);
        record.setTime(recDate.getTime());
        record.setLastDate(recDate);
        record.setLastTime(recDate.getTime());
        record.setNotes("This is a test record");
        record.setFirstAppearance(false);
        record.setLastAppearance(false);
        record.setBehaviour("Behaviour notes");
        record.setHabitat("Habitat Notes");
        record.setNumber(1);
        record.setRecordVisibility(visibility);

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Set<AttributeValue> attributeList = new HashSet<AttributeValue>();
        Map<Attribute, AttributeValue> expectedRecordAttrMap = new HashMap<Attribute, AttributeValue>();
        for (Attribute attr : survey.getAttributes()) {
            if (!AttributeScope.LOCATION.equals(attr.getScope())) {
                List<AttributeOption> opts = attr.getOptions();
                byte[] fileData = null;
                AttributeValue recAttr = new AttributeValue();
                recAttr.setAttribute(attr);
                switch (attr.getType()) {
                    case INTEGER:
                        Integer i = Integer.valueOf(123);
                        recAttr.setNumericValue(new BigDecimal(i));
                        recAttr.setStringValue(i.toString());
                        break;
                    case INTEGER_WITH_RANGE:
                        String intStr = attr.getOptions().iterator().next().getValue();
                        recAttr.setNumericValue(new BigDecimal(
                                Integer.parseInt(intStr)));
                        recAttr.setStringValue(intStr);
                        break;
                    case DECIMAL:
                        Double d = new Double(123);
                        recAttr.setNumericValue(new BigDecimal(d));
                        recAttr.setStringValue(d.toString());
                        break;
                    case DATE:
                        Date date = new Date(System.currentTimeMillis());
                        recAttr.setDateValue(date);
                        recAttr.setStringValue(dateFormat.format(date));
                        break;
                    case STRING_AUTOCOMPLETE:
                    case STRING:
                        recAttr.setStringValue("This is a test string record attribute");
                        break;
                    case TEXT:
                        recAttr.setStringValue("This is a test text record attribute");
                        break;
                    case REGEX:
                    case BARCODE:
                        recAttr.setStringValue("#454545");
                        break;
                    case TIME:
                        recAttr.setStringValue("12:34");
                        break;
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                        recAttr.setStringValue("<hr/>");
                        break;
                    case STRING_WITH_VALID_VALUES:
                        recAttr.setStringValue(opts.iterator().next().getValue());
                        break;
                    case MULTI_CHECKBOX:
                        recAttr.setMultiCheckboxValue(new String[]{
                                opts.get(0).getValue(), opts.get(1).getValue()});
                        break;
                    case MULTI_SELECT:
                        recAttr.setMultiCheckboxValue(new String[]{
                                opts.get(0).getValue(), opts.get(1).getValue()});
                        break;
                    case SINGLE_CHECKBOX:
                        recAttr.setBooleanValue(Boolean.TRUE.toString());
                        break;
                    case AUDIO:
                    case FILE:
                        recAttr.setStringValue("testDataFile.dat");
                        fileData = createImage(-1, -1, recAttr.getStringValue());
                        break;
                    case IMAGE:
                        recAttr.setStringValue("testImgFile.png");
                        fileData = createImage(-1, -1, recAttr.getStringValue());
                        break;
                    case SPECIES:
                        if (cm == null || !Taxonomic.NONTAXONOMIC.equals(cm.getTaxonomic())) {
                            recAttr.setStringValue(species != null ? species.getScientificName() : "");
                            recAttr.setSpecies(species);
                        }
                        break;
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        // census method types should add a record to the attribute value
                        break;
                    default:
                        Assert.assertTrue("Unknown Attribute Type: "
                                + attr.getType().toString(), false);
                        break;
                }
                recAttr = recordDAO.saveAttributeValue(recAttr);
                if (fileData != null) {
                    fileService.createFile(recAttr.getClass(), recAttr.getId(), recAttr.getStringValue(), fileData);
                }

                attributeList.add(recAttr);
                expectedRecordAttrMap.put(attr, recAttr);
            }
        }

        if (record.getSpecies() != null) {
            for (Attribute attr : record.getSpecies().getTaxonGroup().getAttributes()) {
                if (!attr.isTag()) {
                    byte[] fileData = null;
                    AttributeValue recAttr = new AttributeValue();
                    recAttr.setAttribute(attr);
                    switch (attr.getType()) {
                        case INTEGER:
                            Integer i = new Integer(987);
                            recAttr.setNumericValue(new BigDecimal(i));
                            recAttr.setStringValue(i.toString());
                            break;
                        case INTEGER_WITH_RANGE:
                            String intStr = attr.getOptions().iterator().next().getValue();
                            recAttr.setNumericValue(new BigDecimal(
                                    Integer.parseInt(intStr)));
                            recAttr.setStringValue(intStr);
                            break;
                        case DECIMAL:
                            Double d = new Double(987);
                            recAttr.setNumericValue(new BigDecimal(d));
                            recAttr.setStringValue(d.toString());
                            break;
                        case DATE:
                            Date date = new Date(System.currentTimeMillis());
                            recAttr.setDateValue(date);
                            recAttr.setStringValue(dateFormat.format(date));
                            break;
                        case STRING_AUTOCOMPLETE:
                        case STRING:
                            recAttr.setStringValue("This is a test string record attribute for groups");
                            break;
                        case REGEX:
                        case BARCODE:
                            recAttr.setStringValue("#454545");
                            break;
                        case TIME:
                            recAttr.setStringValue("12:34");
                            break;
                        case HTML:
                        case HTML_NO_VALIDATION:
                        case HTML_COMMENT:
                        case HTML_HORIZONTAL_RULE:
                            recAttr.setStringValue("<hr/>");
                            break;
                        case TEXT:
                            recAttr.setStringValue("This is a test text record attribute for groups");
                            break;
                        case STRING_WITH_VALID_VALUES:
                            recAttr.setStringValue(attr.getOptions().iterator().next().getValue());
                            break;
                        case MULTI_CHECKBOX: {
                            List<AttributeOption> opts = attr.getOptions();
                            recAttr.setMultiCheckboxValue(new String[]{
                                    opts.get(0).getValue(), opts.get(1).getValue()});
                        }
                        break;
                        case MULTI_SELECT: {
                            List<AttributeOption> opts = attr.getOptions();
                            recAttr.setMultiSelectValue(new String[]{
                                    opts.get(0).getValue(), opts.get(1).getValue()});
                        }
                        break;
                        case SINGLE_CHECKBOX:
                            recAttr.setStringValue(Boolean.FALSE.toString());
                            break;
                        case AUDIO:
                        case FILE:
                            recAttr.setStringValue("testGroupDataFile.dat");
                            fileData = createImage(-1, -1, recAttr.getStringValue());
                            break;
                        case IMAGE:
                            recAttr.setStringValue("testGroupImgFile.png");
                            fileData = createImage(-1, -1, recAttr.getStringValue());
                            break;
                        case SPECIES:
                            if (cm == null || !Taxonomic.NONTAXONOMIC.equals(cm.getTaxonomic())) {
                                recAttr.setStringValue(species != null ? species.getScientificName() : "");
                                recAttr.setSpecies(species);
                            }
                            break;
                        case CENSUS_METHOD_ROW:
                        case CENSUS_METHOD_COL:
                            // census method types should add a record to the attribute value
                            break;
                        default:
                            Assert.assertTrue("Unknown Attribute Type: "
                                    + attr.getType().toString(), false);
                            break;
                    }
                    recAttr = recordDAO.saveAttributeValue(recAttr);
                    if (fileData != null) {
                        fileService.createFile(recAttr.getClass(), recAttr.getId(), recAttr.getStringValue(), fileData);
                    }

                    attributeList.add(recAttr);
                    expectedRecordAttrMap.put(attr, recAttr);
                }
            }
        }

        record.setAttributes(attributeList);
        return recordDAO.saveRecord(record);
    }

    /**
     * Creates a new image.
     *
     * @param width  the width of the desired image, or < 0 if the width is not important.
     * @param height the height of the desired image, or < 0 if the height is not important.
     * @param text   the text to be rendered on the image.
     * @return the raw bytes of the created image.
     * @throws IOException thrown if there was an issue creating the image.
     */
    private byte[] createImage(int width, int height, String text) throws IOException {
        Random random = new Random();
        if (width < 0) {
            width = random.nextInt(DEFAULT_MAX_IMAGE_WIDTH - DEFAULT_MIN_IMAGE_WIDTH) + DEFAULT_MIN_IMAGE_WIDTH;
        }
        if (height < 0) {
            height = random.nextInt(DEFAULT_MAX_IMAGE_HEIGHT - DEFAULT_MIN_IMAGE_HEIGHT) + DEFAULT_MIN_IMAGE_HEIGHT;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = (Graphics2D) img.getGraphics();
        g2.setBackground(new Color(220, 220, 220));

        Dimension size;
        float fontSize = g2.getFont().getSize();
        // Make the text as large as possible.
        do {
            g2.setFont(g2.getFont().deriveFont(fontSize));
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            int hgt = metrics.getHeight();
            int adv = metrics.stringWidth(text);
            size = new Dimension(adv + 2, hgt + 2);
            fontSize = fontSize + 1f;
        } while (size.width < Math.round(0.9 * width) && size.height < Math.round(0.9 * height));

        g2.setColor(Color.DARK_GRAY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString(text, (width - size.width) / 2, (height - size.height) / 2);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(0, 0, width - 1, height - 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(width * height);
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] rawBytes = baos.toByteArray();
        baos.close();

        return rawBytes;
    }

    public User getAdmin() {
        return admin;
    }

    public TaxonGroup getTaxonGroupBirds() {
        return taxonGroupBirds;
    }

    public TaxonGroup getTaxonGroupFrogs() {
        return taxonGroupFrogs;
    }

    public IndicatorSpecies getSpeciesA() {
        return speciesA;
    }

    public IndicatorSpecies getSpeciesB() {
        return speciesB;
    }

    public IndicatorSpecies getSpeciesC() {
        return speciesC;
    }

    public CensusMethod getMethodA() {
        return methodA;
    }

    public CensusMethod getMethodC() {
        return methodC;
    }

    public CensusMethod getMethodB() {
        return methodB;
    }

    public Date getDateA() {
        return dateA;
    }

    public Date getDateB() {
        return dateB;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getSurveyRecordCount() {
        return surveyRecordCount;
    }

    public Map<User, Integer> getUserRecordCount() {
        return userRecordCount;
    }

    public int getMethodRecordCount() {
        return methodRecordCount;
    }

    public int getTaxonRecordCount() {
        return taxonRecordCount;
    }

    public User getUser() {
        return user;
    }
    
    public int getLocationCount() {
        return locationCount;
    }
    
    public CensusMethod createCensusMethodForAttributes() {
        CensusMethod cm = new CensusMethod();
        cm.setName("Test Attribute Census Method");
        cm.setDescription("Test Attribute Census Method");
        cm.setTaxonomic(Taxonomic.NONTAXONOMIC);
        
        cm.setRunThreshold(false);
        
        // create at least one attribute for the census method to allow it to 
        // be added to forms
        List<Attribute> attributes = new ArrayList<Attribute>(1);
        AttributeType attrType = AttributeType.STRING;
        Attribute attr = new Attribute();
        attr.setRequired(true);
        String attName = "attribute_" + attrType.toString();
        attr.setName(attName);
        attr.setDescription(attName);
        attr.setTypeCode(attrType.getCode());
        attr.setScope(null);
        attr = taxaDAO.save(attr);
        attributes.add(attr);
        cm.setAttributes(attributes);
        
        return methodDAO.save(cm);
    }
}
