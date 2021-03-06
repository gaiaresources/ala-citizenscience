package au.com.gaiaresources.bdrs.service.bulkdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.sasl.AuthenticationException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.util.StringUtils;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.record.impl.ScrollableRecordsList;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.lsid.LSIDService;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

import com.vividsolutions.jts.geom.Point;

public class BulkDataServiceTest extends AbstractControllerTest {
    
    private static final int HEADER_ROW_IDX = 2;

    @Autowired
    private BulkDataService bulkDataService;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private CensusMethodDAO cmDAO;
    @Autowired
    private RecordDAO recDAO;
    @Autowired
    private AttributeDAO attrDAO;
    @Autowired
    private AttributeValueDAO avDAO;
    @Autowired
    private BulkDataReadWriteService bdrws;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private LSIDService lsidService;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private LocationDAO locationDAO;
	@Autowired
	private GeoMapService geoMapService;
	
	private SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil();

    Logger log = Logger.getLogger(getClass());

    Survey survey, uploadSurvey;
    CensusMethod cm;
    Attribute testAttr1;
    Attribute testAttr2;
    CensusMethod cm2;
    Attribute testAttr3;
    Attribute testAttr4;
    Attribute orphanAttr;

    Attribute surveyAttr1;
    Attribute surveyAttr2;
    Attribute surveyAttr4;
    Attribute surveyAttr3;

    User user;
    List<User> userList;

    TaxonGroup taxongroup;
    IndicatorSpecies species;
    IndicatorSpecies species2;
    IndicatorSpecies species3;
    IndicatorSpecies species4;
	private Location loc, uploadLoc;
	private AttributeValue locAttrVal;
	public static final String CUSTOM_PREFIX = "custom_";
	
	private static final String EPSG_HEADER = "EPSG Code";
	
	
	/**
	 * Used for asserting exceptions
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		user = userDAO.getUser("admin");
		survey = surveyDAO.createSurvey("my super survey");
		
		GeoMap map1 = geoMapService.getForSurvey(survey);
		map1.setCrs(BdrsCoordReferenceSystem.WGS84);
		
		survey.setDescription("a really great survey");
		survey.setDefaultRecordVisibility(RecordVisibility.CONTROLLED,
				metadataDAO);

        cm = createCensusMethod("c:m:1:", Taxonomic.NONTAXONOMIC);
        testAttr1 = createAttribute("attribute1", "desc1", true,
                AttributeScope.RECORD, false, "IN");
        testAttr2 = createAttribute("attribute2", "desc2", true,
                AttributeScope.RECORD, false, "ST");
        cm.getAttributes().add(testAttr1);
        cm.getAttributes().add(testAttr2);
        survey.getCensusMethods().add(cm);

        cm2 = createCensusMethod("cm2", Taxonomic.NONTAXONOMIC);
        testAttr3 = createAttribute("attribute3", "desc3", true,
                AttributeScope.RECORD, false, "IN");
        testAttr4 = createAttribute("attribute4", "desc4", true,
                AttributeScope.RECORD, false, "ST");
        cm2.getAttributes().add(testAttr3);
        cm2.getAttributes().add(testAttr4);
        survey.getCensusMethods().add(cm2);

        survey.getCensusMethods().add(cm2);
        
        // Attribute values made from this attribute should not be changed during imports.
        orphanAttr = createAttribute("orphanattr", "orphan", true, AttributeScope.RECORD, false, "ST");

        // make cm1 and cm2 do the recursion thing
        cm.getCensusMethods().add(cm2);
        cm2.getCensusMethods().add(cm);

        cmDAO.update(cm);
        cmDAO.update(cm2);

        surveyAttr1 = createAttribute("surv1", "sdesc1", true,
                AttributeScope.RECORD, false, AttributeType.INTEGER.getCode());
        surveyAttr2 = createAttribute("surv2", "sdesc2", true,
                AttributeScope.RECORD, false, AttributeType.STRING.getCode());
        surveyAttr3 = createAttribute("surv3", "sdesc3", false,
                AttributeScope.RECORD, false, AttributeType.HTML.getCode());
        surveyAttr4 = createAttribute("surv4", "sdesc4", true,
                AttributeScope.LOCATION, false, AttributeType.STRING.getCode());

        survey.getAttributes().add(surveyAttr1);
        survey.getAttributes().add(surveyAttr2);
        survey.getAttributes().add(surveyAttr3);
        survey.getAttributes().add(surveyAttr4);

		loc = new Location();
		loc.setName("Test Location");
		loc.setLocation(spatialUtil.createPoint(10.0, 10.0));
		locAttrVal = new AttributeValue();
		locAttrVal.setAttribute(surveyAttr4);
		locAttrVal.setStringValue("location attribute value");
		locAttrVal = attrDAO.save(locAttrVal);
		loc.getAttributes().add(locAttrVal);
		List<Survey> surveyList = new ArrayList<Survey>(1);
		surveyList.add(survey);
		loc.setSurveys(surveyList);
		loc = locationDAO.save(loc);
		survey.getLocations().add(loc);
		//survey = surveyDAO.save(survey);
		
		taxongroup = taxaDAO.createTaxonGroup("a taxon group", false, false,
				false, false, false, false);
		species = taxaDAO.createIndicatorSpecies("hectus workus",
				"argh pirate", taxongroup, new ArrayList<Region>(),
				new ArrayList<SpeciesProfile>());
		
		species2 = taxaDAO.createIndicatorSpecies("species two", 
				"common two", 
				taxongroup, new ArrayList<Region>(),
				new ArrayList<SpeciesProfile>());

        species3 = taxaDAO.createIndicatorSpecies("species three",
                "",
                taxongroup, new ArrayList<Region>(),
                new ArrayList<SpeciesProfile>());

        species4 = taxaDAO.createIndicatorSpecies("species four",
                "",
                taxongroup, new ArrayList<Region>(),
                new ArrayList<SpeciesProfile>());
        
        Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(species);
        speciesSet.add(species2);
        speciesSet.add(species3);
        speciesSet.add(species4);
        survey.setSpecies(speciesSet);

        surveyDAO.updateSurvey(survey);

        requestDropDatabase();

        userList = createTestUsers();

		uploadSurvey = surveyDAO.createSurvey("MyUploadSurvey");
		
		uploadSurvey.setDescription("a survey for testing uploading records");
		uploadSurvey.setDefaultRecordVisibility(RecordVisibility.CONTROLLED,
				metadataDAO);

		uploadLoc = new Location();
		uploadLoc.setName("MySite");
		uploadLoc.setLocation(spatialUtil.createPoint(-29.499304258328,
				139.92773437486));
		uploadLoc = locationDAO.save(uploadLoc);
		uploadSurvey.getLocations().add(uploadLoc);


        RecordProperty numberProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.NUMBER, metadataDAO);
        numberProperty.setRequired(true);
        numberProperty.setHidden(true);
        RecordProperty speciesProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.SPECIES, metadataDAO);
        speciesProperty.setRequired(false);
        speciesProperty.setHidden(false);
        RecordProperty locationProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.LOCATION, metadataDAO);
        locationProperty.setRequired(false);
        locationProperty.setHidden(false);
        RecordProperty pointProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.POINT, metadataDAO);
        pointProperty.setRequired(false);
        pointProperty.setHidden(true);
        RecordProperty accuracyProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.ACCURACY, metadataDAO);
        accuracyProperty.setRequired(false);
        accuracyProperty.setHidden(false);
        RecordProperty gpsAltitudeProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.GPS_ALTITUDE, metadataDAO);
        gpsAltitudeProperty.setRequired(false);
        gpsAltitudeProperty.setHidden(false);
        RecordProperty whenProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.WHEN, metadataDAO);
        whenProperty.setRequired(true);
        whenProperty.setHidden(false);
        RecordProperty timeProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.TIME, metadataDAO);
        timeProperty.setRequired(true);
        timeProperty.setHidden(false);
        RecordProperty notesProperty = new RecordProperty(uploadSurvey,
                RecordPropertyType.NOTES, metadataDAO);
        notesProperty.setRequired(true);
        notesProperty.setHidden(false);
    }

    @Test
    public void testImportSurvey() throws Exception, ParseException {
        InputStream stream = getClass().getResourceAsStream("basic_upload.xls");
        Survey survey = new Survey();
        setHidden(survey, false);
        setRequired(survey, false);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey, stream);

        Assert.assertEquals(1, bulkUpload.getRecordUploadList().size());
        RecordUpload recUpload = bulkUpload.getRecordUploadList().get(0);

        Calendar cal = Calendar.getInstance();
        cal.set(2011, 2, 27, 14, 42, 0);

        // Assert.assertEquals(cal.getTime(), recUpload.getWhen());
        // there's some messed up rounding going on here... accurate to within
        // the second (1000 ms)
        Assert.assertTrue(Math.abs(cal.getTime().getTime()
                - recUpload.getWhen().getTime()) < 1000);
    }

    @Test
    public void testExportSurveyTemplateCensusMethod() throws IOException {
        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testExportSurveyTemplateCensusMethod",
                ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        Assert.assertNotNull(obSheet);

        // The title
        Assert.assertEquals("my super survey: a really great survey", obSheet
                .getRow(0).getCell(0).getStringCellValue());

        // The header - contains the attribute names
        Assert.assertNotNull(obSheet.getRow(HEADER_ROW_IDX));

		int colIdx = 0;
		Assert.assertEquals("ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Parent ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Census Method ID", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Census Method", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Scientific Name", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Common Name", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location Name", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Latitude/Northings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Longitude/Eastings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("wrong header", EPSG_HEADER, obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey, RecordPropertyType.WHEN,
				metadataDAO).getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey, RecordPropertyType.TIME,
				metadataDAO).getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey,
				RecordPropertyType.NUMBER, metadataDAO).getDescription(),
				obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey,
				RecordPropertyType.NOTES, metadataDAO).getDescription(),
				obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++).getStringCellValue());

        Assert.assertEquals("sdesc1", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());
        Assert.assertEquals("sdesc2", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        // cm1
        Assert.assertEquals(bdrws.formatCensusMethodNameId(cm), obSheet.getRow(
                1).getCell(colIdx).getStringCellValue());
        Assert.assertEquals("desc1", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        Assert.assertEquals("desc2", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        // cm2
        Assert.assertEquals(bdrws.formatCensusMethodNameId(cm2), obSheet
                .getRow(1).getCell(colIdx).getStringCellValue());
        Assert.assertEquals("desc3", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        Assert.assertEquals("desc4", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        // Location Sheet
        Sheet locSheet = wb
                .getSheet(AbstractBulkDataService.LOCATION_SHEET_NAME);
        Assert.assertNotNull(locSheet);

		// Header
		int locColIndex = 0;
		Assert.assertEquals("Location ID", locSheet.getRow(0).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals("Type", locSheet.getRow(0).getCell(locColIndex++)
				.getStringCellValue());
		Assert.assertEquals("Location Name", locSheet.getRow(0).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals("Latitude/Northings", locSheet.getRow(0).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals("Longitude/Eastings", locSheet.getRow(0).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals("EPSG Code", locSheet.getRow(0).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals("sdesc4", locSheet.getRow(0).getCell(locColIndex++)
				.getStringCellValue());

		// Content - Should have one one row there.
		locColIndex = 0;
		Assert.assertEquals(loc.getId().intValue(), new Double(XlsCellUtil
				.cellToDouble(locSheet.getRow(1).getCell(locColIndex++)))
				.intValue());
		Assert.assertEquals(
				AbstractBulkDataService.LOCATION_SHEET_SURVEY_LOCATION,
				locSheet.getRow(1).getCell(locColIndex++).getStringCellValue());
		Assert.assertEquals(loc.getName(), locSheet.getRow(1).getCell(
				locColIndex++).getStringCellValue());
		Assert.assertEquals(loc.getLocation().getCentroid().getY(), XlsCellUtil
				.cellToDouble(locSheet.getRow(1).getCell(locColIndex++)));
		Assert.assertEquals(loc.getLocation().getCentroid().getX(), XlsCellUtil
				.cellToDouble(locSheet.getRow(1).getCell(locColIndex++)));
		Assert.assertEquals("wrong epsg code", "EPSG:"+loc.getLocation().getSRID(),
				XlsCellUtil.cellToString(locSheet.getRow(1).getCell(locColIndex++)));
		Assert.assertEquals(locAttrVal.getStringValue(), XlsCellUtil
				.cellToString(locSheet.getRow(1).getCell(locColIndex++)));
	}

    @Test
    public void testImportLocations() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {

        surveyDAO.updateSurvey(survey);

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testImportLocations", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);

        // Location Sheet
        Sheet locSheet = wb
                .getSheet(AbstractBulkDataService.LOCATION_SHEET_NAME);
        Assert.assertNotNull(locSheet);

		Row row = locSheet.createRow(locSheet.getLastRowNum() + 1);
		int colIndex = 0;
		row.createCell(colIndex++); // Location ID
		// Intentionally setting everything as strings to stress the format
		// coercion
		row.createCell(colIndex++).setCellValue(
				AbstractBulkDataService.LOCATION_SHEET_SURVEY_LOCATION); // Type
		row.createCell(colIndex++).setCellValue("I am a new location"); // Location
																		// Name
		row.createCell(colIndex++).setCellValue("7000000"); // Latitude
		row.createCell(colIndex++).setCellValue("500000"); // Longitude
		// EPSG:28352 (case insensitive)
		row.createCell(colIndex++).setCellValue("EpSg:"+BdrsCoordReferenceSystem.MGA52.getSrid());
		row.createCell(colIndex++).setCellValue("I am a little teapot"); // Attribute
																			// Values
        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream2);
        wb.write(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        int initialLocCount = survey.getLocations().size();
        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals("expect no errors", 0, bulkUpload.getErrorLocationUploadList().size());

        Assert.assertEquals(2, bulkUpload.getLocationUploads().size());

        commit();
        
        bulkDataService.saveRecords(user, bulkUpload, true);
                
        Survey other = surveyDAO.getSurvey(survey.getId());
        
        int actualLocCount = other.getLocations().size();
        Assert.assertEquals(initialLocCount + 1, actualLocCount);

		boolean otherLocationFound = false;
		for (Location otherLoc : other.getLocations()) {
			if (!otherLoc.getId().equals(loc.getId())) {
				otherLocationFound = true;
				Assert.assertNull(otherLoc.getUser());
				Assert.assertEquals(7000000d, otherLoc.getLocation().getCentroid()
						.getY());
				Assert.assertEquals(500000d, otherLoc.getLocation().getCentroid()
						.getX());
				Assert.assertEquals("wrong srid", 28352, otherLoc.getLocation().getSRID());
				Assert.assertEquals("I am a new location", otherLoc.getName());

                Assert.assertEquals(1, loc.getAttributes().size());
                Assert.assertEquals("I am a little teapot", otherLoc
                        .getAttributes().iterator().next().getStringValue());
            }
        }

        Assert.assertTrue(otherLocationFound);
    }

    @Test
    public void testExportSurveyTemplateNoCensusMethod() throws IOException {
        survey.setCensusMethods(new ArrayList<CensusMethod>());
        surveyDAO.updateSurvey(survey);

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testExportSurveyTemplateNoCensusMethod",
                ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        Assert.assertNotNull(obSheet);

        // The title
        Assert.assertEquals("my super survey: a really great survey", obSheet
                .getRow(0).getCell(0).getStringCellValue());

        // The header - contains the attribute names
        Assert.assertNotNull(obSheet.getRow(HEADER_ROW_IDX));

		int colIdx = 0;
		Assert.assertEquals("ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Parent ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Census Method ID", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Census Method", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Scientific Name", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Common Name", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location Name", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Latitude/Northings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Longitude/Eastings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("wrong header", EPSG_HEADER, obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey, RecordPropertyType.WHEN,
				metadataDAO).getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey, RecordPropertyType.TIME,
				metadataDAO).getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey,
				RecordPropertyType.NUMBER, metadataDAO).getDescription(),
				obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++).getStringCellValue());
		Assert.assertEquals(new RecordProperty(survey,
				RecordPropertyType.NOTES, metadataDAO).getDescription(),
				obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++).getStringCellValue());

        Assert.assertEquals("sdesc1", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());
        Assert.assertEquals("sdesc2", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());

        Assert.assertNull(obSheet.getRow(1).getCell(colIdx));
        Assert.assertNull(obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++));
    }

    /**
     * Test that checks if changes to the descriptions of the Darwin Core Fields
     * end up in the downloaded spreadsheet template.
     * 
     * @throws Exception
     */
    @Test
    public void downloadSpreadsheetTemplateCustomDescriptions()
            throws Exception {

        HashMap<RecordPropertyType, RecordProperty> recordProperties = new HashMap<RecordPropertyType, RecordProperty>();
        // change default descriptions to custom ones
        for (RecordPropertyType type : RecordPropertyType.values()) {
            RecordProperty recordProperty = new RecordProperty(survey, type,
                    metadataDAO);
            recordProperty.setDescription(CUSTOM_PREFIX
                    + recordProperty.getDescription());
            recordProperties.put(type, recordProperty);
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        registerStream(outStream);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        ByteArrayInputStream inStream = new ByteArrayInputStream(outStream
                .toByteArray());
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);
        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        Assert.assertNotNull(obSheet);

        // The title
        Assert.assertEquals("my super survey: a really great survey", obSheet
                .getRow(0).getCell(0).getStringCellValue());

        // The header - contains the attribute names
        Assert.assertNotNull(obSheet.getRow(HEADER_ROW_IDX));

		int colIdx = 0;
		Assert.assertEquals("ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Parent ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Census Method ID", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Census Method", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Scientific Name", obSheet.getRow(HEADER_ROW_IDX).getCell(
				colIdx++).getStringCellValue());
		Assert.assertEquals("Common Name", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location ID", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Location Name", obSheet.getRow(HEADER_ROW_IDX)
				.getCell(colIdx++).getStringCellValue());
		Assert.assertEquals("Latitude/Northings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("Longitude/Eastings", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals("wrong header", EPSG_HEADER, obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(recordProperties.get(RecordPropertyType.WHEN)
				.getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(recordProperties.get(RecordPropertyType.TIME)
				.getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(recordProperties.get(RecordPropertyType.NUMBER)
				.getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());
		Assert.assertEquals(recordProperties.get(RecordPropertyType.NOTES)
				.getDescription(), obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
				.getStringCellValue());

        Assert.assertEquals("sdesc1", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());
        Assert.assertEquals("sdesc2", obSheet.getRow(HEADER_ROW_IDX).getCell(colIdx++)
                .getStringCellValue());
    }

    @Test
    public void testImportSurveyNumericIds() throws IOException,
            ParseException, MissingDataException,
            InvalidSurveySpeciesException, DataReferenceException, AmbiguousDataException {
        surveyDAO.updateSurvey(survey);
        setRequired(survey, false);

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testImportSurveyNoCensusMethod", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        Assert.assertNotNull(obSheet);

        // The title
        Assert.assertEquals("my super survey: a really great survey", obSheet
                .getRow(0).getCell(0).getStringCellValue());

        String ssParentId = "1";

		MyTestRow parentRow = new MyTestRow();
		parentRow.setIdAsNumber(true);
		// test with leading / trailing white space
		parentRow.setId(ssParentId);
		parentRow.setCmId(cm.getId().toString());
		parentRow.setCmName(cm.getName());
		parentRow.setScientificName("hectus workus");
		parentRow.setCommonName("common workus");
		parentRow.setSurveyAttr1(1);
		parentRow.setSurveyAttr2("4");
		parentRow.setCm1Attr1(2);
		parentRow.setCm1Attr2("5");
		parentRow.setCm2Attr1(3);
		parentRow.setCm2Attr2("6");
		parentRow.setLatitude(7000000);
		parentRow.setLongitude(550000);
		parentRow.setEpsg(BdrsCoordReferenceSystem.MGA50.getDisplayName());
		parentRow.createRow(obSheet, 3);

		MyTestRow childRow = new MyTestRow();
		childRow.setIdAsNumber(true);
		childRow.setId("");
		childRow.setParentId(ssParentId);
		childRow.setCmId(cm2.getId().toString());
		childRow.setCmName(cm2.getName());
		childRow.setScientificName("hectus workus");
		childRow.setCommonName("common workus");
		childRow.setSurveyAttr1(11);
		childRow.setNumberSeen(1);
		childRow.setNotes("7");
		childRow.setSurveyAttr2("8");
		childRow.setCm1Attr1(2);
		childRow.setCm1Attr2("9");
		childRow.setCm2Attr1(33);
		childRow.setCm2Attr2("10");
		childRow.setLatitude(6800000);
		childRow.setLongitude(560000);
		childRow.setEpsg(Integer.toString(BdrsCoordReferenceSystem.MGA51.getSrid()));
		childRow.createRow(obSheet, 4);

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream2);
        wb.write(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(2, bulkUpload.getRecordUploadList().size());

        commit();
        
        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(2, recDAO.countAllRecords().intValue());

        List<Record> recList = recDAO.getRecords(user);

        for (Record rec : recList) {
            Assert
                    .assertEquals(
                            "record visibility should be set to the survey's default record visibility",
                            survey.getDefaultRecordVisibility(), rec
                                    .getRecordVisibility());
        }

        {
            Record r = getRecordByCensusMethod(recList, cm);

            Assert.assertNotNull(r);
            Assert.assertEquals(cm.getId(), r.getCensusMethod().getId());

            // Assert the parent is correct
            Assert.assertNotNull(getRecAttr(r, survey, "sdesc1"));
            Assert.assertEquals(1, getRecAttr(r, survey, "sdesc1")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(r, survey, "sdesc2"));
            Assert.assertEquals("4", getRecAttr(r, survey, "sdesc2")
                    .getStringValue());

            Assert.assertNotNull(getRecAttr(r, cm, "desc1"));
            Assert.assertEquals(2, getRecAttr(r, cm, "desc1").getNumericValue()
                    .intValue());

            Assert.assertNotNull(getRecAttr(r, cm, "desc2"));
            Assert.assertEquals("5", getRecAttr(r, cm, "desc2")
                    .getStringValue());

			Assert.assertNull(getRecAttr(r, cm2, "desc3"));
			Assert.assertNull(getRecAttr(r, cm2, "desc4"));
			
			Assert.assertEquals("wrong lat", 7000000, r.getLatitude(), 0.1);
			Assert.assertEquals("wrong lon", 550000, r.getLongitude(), 0.1);
			Assert.assertEquals("wrong srid", 28350, r.getGeometry().getSRID());
		}

        // Assert the child is correct
        {
            Record parentRecord = getRecordByCensusMethod(recList, cm);
            Record rChild = getRecordByCensusMethod(recList, cm2);
            Assert.assertNotNull(rChild);
            Assert.assertEquals(cm2.getId(), rChild.getCensusMethod().getId());

            Assert.assertEquals(parentRecord, rChild.getParentRecord());

            Assert.assertNotNull(getRecAttr(rChild, survey, "sdesc1"));
            Assert.assertEquals(11, getRecAttr(rChild, survey, "sdesc1")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(rChild, survey, "sdesc2"));
            Assert.assertEquals("8", getRecAttr(rChild, survey, "sdesc2")
                    .getStringValue());

            Assert.assertNull(getRecAttr(rChild, cm, "desc1"));
            Assert.assertNull(getRecAttr(rChild, cm, "desc2"));

            Assert.assertNotNull(getRecAttr(rChild, cm2, "desc3"));
            Assert.assertEquals(33, getRecAttr(rChild, cm2, "desc3")
                    .getNumericValue().intValue());

			Assert.assertNotNull(getRecAttr(rChild, cm2, "desc4"));
			Assert.assertEquals("10", getRecAttr(rChild, cm2, "desc4")
					.getStringValue());
			
			Assert.assertEquals("wrong lat", 6800000, rChild.getLatitude(), 0.1);
			Assert.assertEquals("wrong lon", 560000, rChild.getLongitude(), 0.1);
			Assert.assertEquals("wrong srid", 28351, rChild.getGeometry().getSRID());
		}
	}

    @Test(expected = DataReferenceException.class)
    public void testImportSurveyNoCensusMethod() throws IOException,
            ParseException, MissingDataException,
            InvalidSurveySpeciesException, DataReferenceException, AmbiguousDataException {
        // Will throw an exception
        survey.setCensusMethods(new ArrayList<CensusMethod>());
        surveyDAO.updateSurvey(survey);
        
        setRequired(survey, false);

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testImportSurveyNoCensusMethod", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);

        Workbook wb = new HSSFWorkbook(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        Assert.assertNotNull(obSheet);

        // The title
        Assert.assertEquals("my super survey: a really great survey", obSheet
                .getRow(0).getCell(0).getStringCellValue());

        String ssParentId = "parentid";

        MyTestRow parentRow = new MyTestRow();
        // test with leading / trailing white space
        parentRow.setId(" " + ssParentId + " ");
        // parentRow.setCmId("");
        parentRow.setScientificName("hectus workus");
        parentRow.setSurveyAttr1(1);
        parentRow.setSurveyAttr2("string1");
        parentRow.setCm1Attr1(2);
        parentRow.setCm1Attr2("string2");
        parentRow.setCm2Attr1(3);
        parentRow.setCm2Attr2("string3");
        parentRow.createRow(obSheet, 3);

        MyTestRow childRow = new MyTestRow();
        childRow.setId("");
        childRow.setParentId(ssParentId);
        // childRow.setCmId("");
        childRow.setScientificName("hectus workus");
        childRow.setSurveyAttr1(11);
        childRow.setNumberSeen(1);
        childRow.setNotes("child entry notes");
        childRow.setSurveyAttr2("survey attr string");
        childRow.setCm1Attr1(2);
        childRow.setCm1Attr2("string2");
        childRow.setCm2Attr1(33);
        childRow.setCm2Attr2("choo choo");
        childRow.createRow(obSheet, 4);

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream2);
        wb.write(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(2, bulkUpload.getRecordUploadList().size());

        commit();
        
        bulkDataService.saveRecords(user, bulkUpload, true);
    }

    private void createCensusMethodRecordAttributes(Record record)
            throws ParseException {
        if (record.getCensusMethod() == null) {
            return;
        }
        for (Attribute cmAttr : record.getCensusMethod().getAttributes()) {
            AttributeValue recAttr = new AttributeValue();
            recAttr.setAttribute(cmAttr);
            switch (cmAttr.getType()) {
            case INTEGER:
            case DECIMAL:
                recAttr.setNumericValue(new BigDecimal(1000));
                break;
            case DATE:
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "dd MMM yyyy");
                dateFormat.setLenient(false);
                recAttr.setDateValue(dateFormat.parse("11 may 2011"));
                break;
            case IMAGE:
            case AUDIO:
            case VIDEO:
            case FILE:
                throw new UnsupportedOperationException(
                        "Spreadsheet upload of file data is not supported.");
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                throw new UnsupportedOperationException(
                        "Spreadsheet upload of census method data is not supported.");
            case TEXT:
            case STRING_WITH_VALID_VALUES:
            case STRING:
            default:
                recAttr.setStringValue("cm attr");
                break;
            }
            recDAO.saveAttributeValue(recAttr);
            record.getAttributes().add(recAttr);
        }
    }
    
    private void addOrphanAttr(Record rec, String value) {
        AttributeValue av = new AttributeValue();
        av.setAttribute(orphanAttr);
        av.setStringValue(value);
        avDAO.save(av);
        rec.getAttributes().add(av);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testExportEditXls() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {

		setRequired(survey, false);
		Calendar cal = Calendar.getInstance();
		cal.set(2011, 2, 27, 14, 42, 0);
		Point point = spatialUtil.createPoint(-40, 120);
		Record rec = createRecord(survey, point, user, species, cal
                .getTime(), cal.getTime().getTime(), null, null, "", false, false, "", "",
                200, new HashMap<Attribute, Object>());
		rec.setCensusMethod(cm);
		// setting record visibility here so records will be visible anonymously

        // alternately, we could add a login to the test
        rec.setRecordVisibility(RecordVisibility.PUBLIC);
        createCensusMethodRecordAttributes(rec);
        addOrphanAttr(rec, "parent");

        Record recChild = createRecord(survey, point, user, species, cal
                .getTime(), cal.getTime().getTime(), null, null, "", false, false, "", "",
                200, new HashMap<Attribute, Object>());
        recChild.setParentRecord(rec);
        recChild.setCensusMethod(cm2);
        // setting record visibility here so records will be visible anonymously
        // alternately, we could add a login to the test
        recChild.setRecordVisibility(RecordVisibility.PUBLIC);
        createCensusMethodRecordAttributes(recChild);
        addOrphanAttr(recChild, "child");

        List<Record> recListToDownload = new ArrayList<Record>();
        recListToDownload.add(rec);
        recListToDownload.add(recChild);

        ScrollableRecords sc = new ScrollableRecordsList(recListToDownload);

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testExportEditXls", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);
        
        // exportSurveyRecords evicts objects from the hibernate session interally.
        // this is the only test in this test suite that relies on items created
        // before the call the exportSurveyRecords, so we flush here to make sure
        // the items are properly persisted
        sessionFactory.getCurrentSession().flush();
        
        bulkDataService.exportSurveyRecords(sessionFactory.getCurrentSession(), survey, sc, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);
        Workbook wb = new HSSFWorkbook(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        
        List<String> headerList = this.getHeaderList(obSheet, HEADER_ROW_IDX);
        Assert.assertFalse("must not contain orphan attr desc", headerList.contains(orphanAttr.getDescription()));

        {
            Row parentXlsRow = obSheet.getRow(3);
            int colIdx = 0;
            Assert.assertEquals(lsidService.toLSID(rec).toString(),
                    parentXlsRow.getCell(colIdx++).getStringCellValue()); // ID
            Assert.assertFalse(StringUtils.hasLength(parentXlsRow.getCell(
                    colIdx++).getStringCellValue())); // parentId

            Assert.assertEquals(rec.getCensusMethod().getId().intValue(),
                    new Double(parentXlsRow.getCell(colIdx++)
                            .getNumericCellValue()).intValue()); // Census
                                                                    // method id
            Assert.assertEquals(rec.getCensusMethod().getName(), XlsCellUtil
                    .cellToString(parentXlsRow.getCell(colIdx++))); // Census
                                                                    // method
                                                                    // name

            colIdx++; // Scientific Name
            colIdx++; // Common Name

            colIdx++; // Location ID
            colIdx++; // Location Name

            Assert.assertEquals(-40, new Double(XlsCellUtil
                    .cellToDouble(parentXlsRow.getCell(colIdx++))).intValue());
            Assert.assertEquals(120, new Double(XlsCellUtil
                    .cellToDouble(parentXlsRow.getCell(colIdx++))).intValue());

			colIdx++; // epsg
		
			colIdx++; // Date
			colIdx++; // Time

            colIdx++; // Number
            colIdx++; // Notes

            colIdx++; // sdesc1
            colIdx++; // sdesc2

            // Yes I know this makes for a fragile and hard to read test, at
            // least you get to have fun looking
            // at the code now that you've broken it
            // p.s. pls don't remove these asserts, move them to whatever cell
            // index is correct for your
            // new column arrangement!
            Assert.assertEquals(1000, ((Double) parentXlsRow.getCell(colIdx++)
                    .getNumericCellValue()).intValue());
            Assert.assertEquals("cm attr", parentXlsRow.getCell(colIdx++)
                    .getStringCellValue());

            Assert.assertFalse(StringUtils.hasLength(parentXlsRow.getCell(
                    colIdx++).getStringCellValue()));
            Assert.assertFalse(StringUtils.hasLength(parentXlsRow.getCell(
                    colIdx++).getStringCellValue()));
        }

        {
            Row childXlsRow = obSheet.getRow(4);
            int colIdx = 0;
            // Child
            Assert.assertEquals(lsidService.toLSID(recChild).toString(),
                    childXlsRow.getCell(colIdx++).getStringCellValue());
            // Parent
            Assert.assertEquals(lsidService.toLSID(rec).toString(), childXlsRow
                    .getCell(colIdx++).getStringCellValue());

            // Census Method ID
            Assert.assertEquals(recChild.getCensusMethod().getId().intValue(),
                    new Double(XlsCellUtil.cellToDouble(childXlsRow
                            .getCell(colIdx++))).intValue());
            // Census Method Name
            Assert.assertEquals(recChild.getCensusMethod().getName(),
                    XlsCellUtil.cellToString(childXlsRow.getCell(colIdx++)));

            colIdx++; // Scientific Name
            colIdx++; // Common Name

            colIdx++; // Location ID
            colIdx++; // Location Name

            colIdx++; // Latitude
            colIdx++; // Longitude
			
			colIdx++; // epsg

            colIdx++; // Date
            colIdx++; // Time

            colIdx++; // Number
            colIdx++; // Notes

            colIdx++; // sdesc1
            colIdx++; // sdesc2

            Assert.assertFalse(StringUtils.hasLength(childXlsRow.getCell(
                    colIdx++).getStringCellValue()));
            Assert.assertFalse(StringUtils.hasLength(childXlsRow.getCell(
                    colIdx++).getStringCellValue()));

            Assert.assertEquals(1000, ((Double) childXlsRow.getCell(colIdx++)
                    .getNumericCellValue()).intValue());
            Assert.assertEquals("cm attr", childXlsRow.getCell(colIdx++)
                    .getStringCellValue());
        }

        // now that it came out right, lets edit it!
        {
            Row childXlsRow = obSheet.getRow(4);
            childXlsRow.getCell(1).setCellValue("");
            
            // clearing species...
            // scientific name
            childXlsRow.getCell(4).setCellValue("");
            // common name
            childXlsRow.getCell(5).setCellValue("");

			childXlsRow.getCell(17).setCellValue(10);
			childXlsRow.getCell(18).setCellValue("new string");
		}

        // add a new row, make it a child of our original parent record
        {
            MyTestRow childRow = new MyTestRow();
            childRow.setId("weeeee");
            childRow.setParentId(lsidService.toLSID(rec).toString());
            childRow.setCmId(cm2.getId().toString());
            childRow.setSurveyAttr1(11);
            childRow.setNumberSeen(1);
            childRow.setNotes("child entry notes");
            childRow.setSurveyAttr2("survey attr string");
            childRow.setCm1Attr1(2);
            childRow.setCm1Attr2("string2");
            childRow.setCm2Attr1(33);
            childRow.setCm2Attr2("choo choo");
            childRow.createRow(obSheet, 5);
        }

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream2);
        wb.write(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(3, bulkUpload.getRecordUploadList().size());

        commit();
        
        bulkDataService.saveRecords(user, bulkUpload, true);
        
        Assert.assertEquals(3, recDAO.countAllRecords().intValue());

        rec = recDAO.getRecord(rec.getId());
        recChild = recDAO.getRecord(recChild.getId());
        
        // child's species field should be cleared.
        Assert.assertNull("child's species field should be null", recChild.getSpecies());

        Assert.assertNull(recChild.getParentRecord());
        Assert.assertEquals(1, rec.getChildRecords().size());
        
        // assert orphan attribute has not changed.
        Assert.assertEquals("wrong parent orphan attribute value", 
                            getAttributeValueByAttrId(rec, orphanAttr.getId()).getStringValue(), "parent");
        Assert.assertEquals("wrong child orphan attribute value", 
                            getAttributeValueByAttrId(recChild, orphanAttr.getId()).getStringValue(), "child");
    }

    @Test
    public void testImportSurveyCensusMethodRecordInRecord()
            throws IOException, ParseException, MissingDataException,
            InvalidSurveySpeciesException, DataReferenceException, AmbiguousDataException {
        setRequired(survey, false);
        File spreadSheetTmp = File
                .createTempFile(
                        "BulkDataServiceTest.testImportSurveyCensusMethodRecordInRecord",
                        ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        Calendar cal = Calendar.getInstance();
        cal.set(2011, 2, 27, 14, 42, 0);

        // parent id can be any string so...
        String ssParentId = "yodawg!";

        MyTestRow parentRow = new MyTestRow();
        // test with leading / trailing white space
        parentRow.setId(" " + ssParentId + " ");
        parentRow.setCmId(cm.getId().toString());
        parentRow.setSurveyAttr1(1);
        parentRow.setSurveyAttr2("string1");
        parentRow.setCm1Attr1(2);
        parentRow.setCm1Attr2("string2");
        parentRow.setCm2Attr1(3);
        parentRow.setCm2Attr2("string3");
        parentRow.createRow(obSheet, 3);

        MyTestRow childRow = new MyTestRow();
        childRow.setId("");
        childRow.setParentId(ssParentId);
        childRow.setCmId(cm2.getId().toString());
        childRow.setSurveyAttr1(11);
        childRow.setNumberSeen(1);
        childRow.setNotes("child entry notes");
        childRow.setSurveyAttr2("survey attr string");
        childRow.setCm1Attr1(2);
        childRow.setCm1Attr2("string2");
        childRow.setCm2Attr1(33);
        childRow.setCm2Attr2("choo choo");
        childRow.createRow(obSheet, 4);

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(2, bulkUpload.getRecordUploadList().size());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(2, recDAO.countAllRecords().intValue());

        List<Record> recList = recDAO.getRecords(user);

        for (Record rec : recList) {
            Assert
                    .assertEquals(
                            "record visibility should be set to the survey's default record visibility",
                            survey.getDefaultRecordVisibility(), rec
                                    .getRecordVisibility());
        }

        {
            Record r = getRecordByCensusMethod(recList, cm);

            Assert.assertNotNull(r);
            Assert.assertEquals(cm.getId(), r.getCensusMethod().getId());

            // Assert the parent is correct
            Assert.assertNotNull(getRecAttr(r, survey, "sdesc1"));
            Assert.assertEquals(1, getRecAttr(r, survey, "sdesc1")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(r, survey, "sdesc2"));
            Assert.assertEquals("string1", getRecAttr(r, survey, "sdesc2")
                    .getStringValue());

            Assert.assertNotNull(getRecAttr(r, cm, "desc1"));
            Assert.assertEquals(2, getRecAttr(r, cm, "desc1").getNumericValue()
                    .intValue());

            Assert.assertNotNull(getRecAttr(r, cm, "desc2"));
            Assert.assertEquals("string2", getRecAttr(r, cm, "desc2")
                    .getStringValue());

            Assert.assertNull(getRecAttr(r, cm2, "desc3"));
            Assert.assertNull(getRecAttr(r, cm2, "desc4"));
        }

        // Assert the child is correct
        {
            Record parentRecord = getRecordByCensusMethod(recList, cm);
            Record rChild = getRecordByCensusMethod(recList, cm2);
            Assert.assertNotNull(rChild);
            Assert.assertEquals(cm2.getId(), rChild.getCensusMethod().getId());

            Assert.assertEquals(parentRecord, rChild.getParentRecord());

            Assert.assertNotNull(getRecAttr(rChild, survey, "sdesc1"));
            Assert.assertEquals(11, getRecAttr(rChild, survey, "sdesc1")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(rChild, survey, "sdesc2"));
            Assert.assertEquals("survey attr string", getRecAttr(rChild,
                    survey, "sdesc2").getStringValue());

            Assert.assertNull(getRecAttr(rChild, cm, "desc1"));
            Assert.assertNull(getRecAttr(rChild, cm, "desc2"));

            Assert.assertNotNull(getRecAttr(rChild, cm2, "desc3"));
            Assert.assertEquals(33, getRecAttr(rChild, cm2, "desc3")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(rChild, cm2, "desc4"));
            Assert.assertEquals("choo choo", getRecAttr(rChild, cm2, "desc4")
                    .getStringValue());
        }
    }

    @Test
    public void testImportSurveyInvalidDateRange() throws Exception,
            ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "invalid_daterange.xls");
        this.registerStream(stream);
        Survey survey = new Survey();
        
        setRequired(survey, false);

        // set the survey start date and end date after the one in the file
        Calendar cal = Calendar.getInstance();
        cal.set(2011, 6, 5, 0, 0, 0);
        survey.setStartDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 1);
        survey.setEndDate(cal.getTime());

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey, stream);

        // should be one correct and one incorrect
        Assert.assertEquals(1, bulkUpload.getRecordUploadList().size());
        Assert.assertEquals(1, bulkUpload.getErrorRecordUploadList().size());

    }

    @Test
    public void testImportSurveyValidDateRange() throws Exception,
            ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "invalid_daterange.xls");
        this.registerStream(stream);
        Survey survey = new Survey();
        setRequired(survey, false);

        // set the survey start date and end date before and after the ones in
        // the file
        Calendar cal = Calendar.getInstance();
        cal.set(2011, 6, 1, 0, 0, 0);
        survey.setStartDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 7);
        survey.setEndDate(cal.getTime());

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey, stream);
        List<RecordUpload> errors = bulkUpload.getErrorRecordUploadList();

        Assert.assertEquals(0, errors.size());
        Assert.assertEquals(2, bulkUpload.getRecordUploadList().size());
    }
    
    @Test
    public void testImportRecordAndMissingLocation() throws IOException,
            ParseException, MissingDataException,
            InvalidSurveySpeciesException, DataReferenceException, AmbiguousDataException {
        setRequired(survey, false);
        File spreadSheetTmp = File.createTempFile("BulkDataServiceTest.testImportRecordAndMissingLocation", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        String locationName = "I am a new location";
        
        MyTestRow row = new MyTestRow();
        row.setLocationName(locationName);
        row.createRow(obSheet, 3);
        
        // Location Sheet
        Sheet locSheet = wb
                        .getSheet(AbstractBulkDataService.LOCATION_SHEET_NAME);
        Assert.assertNotNull(locSheet);

        Row locRow = locSheet.createRow(locSheet.getLastRowNum() + 1);
        int colIndex = 0;
        locRow.createCell(colIndex++); // Location ID
        // Intentionally setting everything as strings to stress the format
        // coercion
        locRow.createCell(colIndex++).setCellValue(
                        AbstractBulkDataService.LOCATION_SHEET_SURVEY_LOCATION); // Type
        locRow.createCell(colIndex++).setCellValue(locationName); // Location
                                                                                                                                        // Name
        locRow.createCell(colIndex++).setCellValue("-20"); // Latitude
        locRow.createCell(colIndex++).setCellValue("-20"); // Longitude
        locRow.createCell(colIndex++).setCellValue("EPSG:4326"); // epsg code
        locRow.createCell(colIndex++).setCellValue("I am a little teapot"); // Attribute

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey, inStream2);

        Assert.assertEquals("wrong number of errors", 0, bulkUpload.getErrorCount());
        Assert.assertEquals(1, bulkUpload.getRecordUploadList().size());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(1, recDAO.countAllRecords().intValue());
        Assert.assertEquals(1, survey.getLocations().size());
        
        Location loc = locationDAO.getLocationByName(survey.getName(), locationName);
        
        List<Record> recList = recDAO.getRecords(loc);
        Assert.assertEquals(1, recList.size());
    }

    @Test
    public void testImportRecordDateTime() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        setRequired(survey, false);
        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testImportRecordDateTime", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2011, 2, 27, 14, 42, 0);

        MyTestRow row = new MyTestRow();
        // test with leading / trailing white space
        row.setSurveyAttr1(1);
        row.setSurveyAttr2("string1");
        row.setDate(getDate(2011, 2, 27, 0, 0));
        row.setTime(getDate(0, 0, 0, 14, 42));
        row.createRow(obSheet, 3);

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(1, bulkUpload.getRecordUploadList().size());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(1, recDAO.countAllRecords().intValue());

        List<Record> recList = recDAO.getRecords(user);

        {
            Record r = recList.get(0);

            Assert.assertNotNull(r);

            // Assert the parent is correct
            Assert.assertNotNull(getRecAttr(r, survey, "sdesc1"));
            Assert.assertEquals(1, getRecAttr(r, survey, "sdesc1")
                    .getNumericValue().intValue());

            Assert.assertNotNull(getRecAttr(r, survey, "sdesc2"));
            Assert.assertEquals("string1", getRecAttr(r, survey, "sdesc2")
                    .getStringValue());

            Assert.assertEquals("dates should be equal", cal.getTime()
                    .getTime(), r.getWhen().getTime());
        }
    }

    @Test(expected = AmbiguousDataException.class)
    public void testAmbiguousDataImport() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        TaxonGroup taxongroup2 = taxaDAO.createTaxonGroup("a different taxon group", false, false,
                false, false, false, false);
        IndicatorSpecies species3_1 = taxaDAO.createIndicatorSpecies(species3.getScientificName(),
                species3.getCommonName(), taxongroup2, new ArrayList<Region>(),
                new ArrayList<SpeciesProfile>());
        IndicatorSpecies species4_1 = taxaDAO.createIndicatorSpecies(species4.getScientificName(),
                species4.getCommonName(), taxongroup2, new ArrayList<Region>(),
                new ArrayList<SpeciesProfile>());
        survey.setSpecies(new HashSet<IndicatorSpecies>());

        setRequired(survey, false);
        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testAmbiguousDataImport", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2011, 2, 27, 14, 42, 0);

        IndicatorSpecies[] speciesArray = new IndicatorSpecies[]{ species, species2,
                species3, species3_1,
                species4, species4_1 };
        for(int i=0; i<speciesArray.length; i++) {
            IndicatorSpecies species = speciesArray[i];
            MyTestRow row = new MyTestRow();
            row.setScientificName(species.getScientificName());
            row.setCommonName(species.getCommonName());
            row.setLatitude(0);
            row.setLongitude(0);

            row.setSurveyAttr1(1);
            row.setSurveyAttr2("string1");
            row.createRow(obSheet, 3 + i);
        }

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(speciesArray.length, bulkUpload.getRecordUploadList().size());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);
    }

    @Test
    public void testNonMandatoryTaxonomicImport() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        setRequired(survey, false);
        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testNonMandatoryTaxonomicImport", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2011, 2, 27, 14, 42, 0);

        Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(species);
        speciesSet.add(species2);
        speciesSet.add(species3);
        speciesSet.add(species4);
        int index = 0;
        for(IndicatorSpecies species : speciesSet) {
            MyTestRow row = new MyTestRow();
            row.setScientificName(species.getScientificName());
            row.setCommonName(species.getCommonName());
            row.setLatitude(0);
            row.setLongitude(0);

            row.setSurveyAttr1(1);
            row.setSurveyAttr2("string1");
            row.createRow(obSheet, 3 + index);

            index += 1;
        }

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(speciesSet.size(), bulkUpload.getRecordUploadList().size());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(speciesSet.size(), recDAO.countAllRecords().intValue());
        List<Record> recList = recDAO.getRecords(user);
        for(Record rec : recList) {
            Assert.assertNotNull(rec.getSpecies());
            // Expect 4 unique species.
            Assert.assertTrue(speciesSet.remove(rec.getSpecies()));
        }

        // At this point, we have added 4 species and removed 4 species. Therefore the set is empty.
        Assert.assertTrue(speciesSet.isEmpty());
    }

    @Test
    public void testSpeciesAttributeImport_ok() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        testSpeciesAttributeImport(species2.getScientificName(), species2.getCommonName(), species2);
    }
    
    @Test
    public void testSpeciesAttributeImport_badName() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        thrown.expect(AuthenticationException.class);
        testSpeciesAttributeImport("blahblahblahrandomtext", "asdfjke89sduh", species2);
    }
    
    @Test
    public void testSpeciesAttributeImport_nothing() throws IOException, ParseException,
            MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        // use empty white space
        testSpeciesAttributeImport("   ", "   ", null);
    }
    
    private void testSpeciesAttributeImport(String scientificName, String commonName, IndicatorSpecies expectedSpecies)
            throws IOException, ParseException, MissingDataException, InvalidSurveySpeciesException,
            DataReferenceException, AmbiguousDataException {
        setRequired(survey, false);
        
        // add a species attribute.
        Attribute speciesAttr = new Attribute();
        speciesAttr.setName("species_attr");
        speciesAttr.setDescription("species attr");
        speciesAttr.setTypeCode(AttributeType.SPECIES.getCode());
        taxaDAO.save(speciesAttr);
        
        // we are going to replace the 'string' survey scoped attribute with
        // our new species attribute.
        List<Attribute> attrList = new ArrayList<Attribute>(2);
        attrList.add(surveyAttr1);
        // **********************
        // replacing the string type attribute!
        attrList.add(speciesAttr);
        // **********************
        survey.setAttributes(attrList);
        
        surveyDAO.save(survey);
        
        // edit the second attribute of the the first test cm.
        testAttr2.setTypeCode(AttributeType.SPECIES.getCode());
        taxaDAO.save(testAttr2);
        
        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testImportSpeciesAttribute", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        bulkDataService.exportSurveyTemplate(getSession(), survey, outStream);
        registerStream(outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        Workbook wb = new HSSFWorkbook(inStream);
        registerStream(inStream);

        Sheet obSheet = wb.getSheet(AbstractBulkDataService.RECORD_SHEET_NAME);
        // enter records starting at row 3

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2011, 2, 27, 14, 42, 0);

        MyTestRow row = new MyTestRow();
        // test with leading / trailing white space
        row.setSurveyAttr1(1);
        // ****************************************************
        // set the string type attribute to the sci name of our 
        // desired species.
        row.setSurveyAttr2(scientificName);
        // ****************************************************
        row.setDate(getDate(2011, 2, 27, 0, 0));
        row.setTime(getDate(0, 0, 0, 14, 42));
        
        row.setCmId(cm.getId().toString());
        row.setCm1Attr2(commonName);
        
        row.createRow(obSheet, 3);

        FileOutputStream outStream2 = new FileOutputStream(spreadSheetTmp);
        wb.write(outStream2);
        registerStream(outStream2);

        FileInputStream inStream2 = new FileInputStream(spreadSheetTmp);
        registerStream(inStream2);

        BulkUpload bulkUpload = bulkDataService.importBulkData(survey,
                inStream2);

        Assert.assertEquals(1, bulkUpload.getRecordUploadList().size());
		
		Assert.assertEquals("wrong number of errors", 0, bulkUpload.getErrorCount());

        commit();

        bulkDataService.saveRecords(user, bulkUpload, true);

        Assert.assertEquals(1, recDAO.countAllRecords().intValue());

        List<Record> recList = recDAO.getRecords(user);

        {
            Record r = recList.get(0);

            Assert.assertNotNull(r);

            // Assert the parent is correct
            Assert.assertNotNull(getRecAttr(r, survey, "sdesc1"));
            Assert.assertEquals(1, getRecAttr(r, survey, "sdesc1")
                    .getNumericValue().intValue());
            
            // 'sdesc2' has been replaced by the species attribute...
            AttributeValue speciesAttrVal = getRecAttr(r, survey, speciesAttr.getDescription());
            Assert.assertNotNull("attr value should not be null", speciesAttrVal);
            if (expectedSpecies == null) {
                Assert.assertNull("species should be null", speciesAttrVal.getSpecies());
            } else {
                Assert.assertNotNull("species should not be null", speciesAttrVal.getSpecies());
                Assert.assertEquals("wrong species", expectedSpecies.getId(), speciesAttrVal.getSpecies().getId());    
            }
            
            // check the census method species attr type...
            AttributeValue cmSpeciesAttrVal = getRecAttr(r, cm, testAttr2.getDescription());
            Assert.assertNotNull("attr value should not be null", cmSpeciesAttrVal);
            if (expectedSpecies == null) {
                Assert.assertNull("species should be null", cmSpeciesAttrVal.getSpecies());
            } else {
                Assert.assertNotNull("species should not be null", cmSpeciesAttrVal.getSpecies());
                Assert.assertEquals("wrong species", expectedSpecies.getId(), cmSpeciesAttrVal.getSpecies().getId());    
            }
            Assert.assertEquals("dates should be equal", cal.getTime()
                    .getTime(), r.getWhen().getTime());
        }
    }

    @Test
    public void testExportUsers() throws Exception {

        File spreadSheetTmp = File.createTempFile(
                "BulkDataServiceTest.testExportUsers", ".xls");
        FileOutputStream outStream = new FileOutputStream(spreadSheetTmp);
        registerStream(outStream);

        bulkDataService.exportUsers(userList, outStream);

        InputStream inStream = new FileInputStream(spreadSheetTmp);
        registerStream(inStream);
        Workbook wb = new HSSFWorkbook(inStream);

        Sheet usersSheet = wb
                .getSheet(AbstractBulkDataService.USERS_SHEET_NAME);
        Assert.assertNotNull(usersSheet);
        Assert.assertEquals(2, usersSheet.getLastRowNum());
        // The Headers
        Assert.assertEquals("Login", usersSheet.getRow(0).getCell(0)
                .getStringCellValue());
        Assert.assertEquals("Given Name", usersSheet.getRow(0).getCell(1)
                .getStringCellValue());
        Assert.assertEquals("Surname", usersSheet.getRow(0).getCell(2)
                .getStringCellValue());
        Assert.assertEquals("Email Address", usersSheet.getRow(0).getCell(3)
                .getStringCellValue());
        // The users
        Assert.assertEquals("user", usersSheet.getRow(1).getCell(0)
                .getStringCellValue());
        Assert.assertEquals("fn", usersSheet.getRow(1).getCell(1)
                .getStringCellValue());
        Assert.assertEquals("ln", usersSheet.getRow(1).getCell(2)
                .getStringCellValue());
        Assert.assertEquals("user@mailinator.com", usersSheet.getRow(1)
                .getCell(3).getStringCellValue());
        Assert.assertEquals("admin", usersSheet.getRow(2).getCell(0)
                .getStringCellValue());
        Assert.assertEquals("firstn", usersSheet.getRow(2).getCell(1)
                .getStringCellValue());
        Assert.assertEquals("lastn", usersSheet.getRow(2).getCell(2)
                .getStringCellValue());
        Assert.assertEquals("admin@mailinator.com", usersSheet.getRow(2)
                .getCell(3).getStringCellValue());

    }

    /**
     * The xls file has all fields populated.
     */
    @Test
    public void testAllFieldsPopulated() throws Exception, ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "allFieldsPopulated_upload.xls");
        BulkUpload bulkUpload = bulkDataService.importBulkData(uploadSurvey,
                stream);
        Assert.assertEquals(0, bulkUpload.getErrorCount());
    }

    /**
     * The xls file has no fields populated.
     */
    @Test
    public void testAllFieldsEmpty() throws Exception, ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "allFieldsEmpty_upload.xls");
        BulkUpload bulkUpload = bulkDataService.importBulkData(uploadSurvey,
                stream);
        Assert.assertEquals(0, bulkUpload.getErrorCount());
    }

    /**
     * The xls file has all required fields populated. The non required fields
     * are left blank.
     */
    @Test
    public void testNonRequiredFieldsEmpty() throws Exception, ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "nonRequiredFieldsEmpty_upload.xls");
        BulkUpload bulkUpload = bulkDataService.importBulkData(uploadSurvey,
                stream);
        Assert.assertEquals(0, bulkUpload.getErrorCount());
    }

    /**
     * The xls file has all the not required fields populated, the required
     * fields are left blank.
     */
    @Test
    public void testRequiredFieldsEmpty() throws Exception, ParseException {
        InputStream stream = getClass().getResourceAsStream(
                "requiredFieldsEmpty_upload.xls");
        BulkUpload bulkUpload = bulkDataService.importBulkData(uploadSurvey,
                stream);
        Assert.assertEquals(1, bulkUpload.getErrorCount());
    }
	
	/**
	 * The xls file has all the not required fields populated, the required
	 * fields are left blank.
	 */
	@Test
	public void testLocationCrsFieldEmpty() throws Exception, ParseException {
		InputStream stream = getClass().getResourceAsStream(
				"requiredLocationCrsEmpty_upload.xls");
		BulkUpload bulkUpload = bulkDataService.importBulkData(uploadSurvey,
				stream);
		Assert.assertEquals(1, bulkUpload.getErrorCount());
		Assert.assertEquals("wrong num location upload in error", 1, bulkUpload.getErrorLocationUploadList().size());
	}

    private class MyTestRow {

        boolean idAsNumber = false;
        String id = "";
        String parentId = "";
        String cmId = null;
        String cmName = null;
        String scientificName = "";
        String commonName = "";
        String locationId = null;
        String locationName = null;
        double latitude = -31;
        double longitude = 115;
        Date date = new Date();
        Date time = new Date();
        double numberSeen = 100;
        String notes = "notes";
        double surveyAttr1 = 1;
        String surveyAttr2 = "string1";
        double cm1Attr1 = 2;
        String cm1Attr2 = "string2";
        double cm2Attr1 = 3;
        String cm2Attr2 = "string3";
		String epsg = null;

        public Row createRow(Sheet sheet, int rowIndex) {
            Row result = sheet.createRow(rowIndex);
            int colIdx = 0;

            Calendar cal = Calendar.getInstance();
            cal.setTime(time);
            String timeString = String.format("%02d", cal
                    .get(Calendar.HOUR_OF_DAY))
                    + ":" + String.format("%02d", cal.get(Calendar.MINUTE));

			if (!idAsNumber) {
				result.createCell(colIdx++).setCellValue(id); // id
				result.createCell(colIdx++).setCellValue(parentId); // parent id
				result.createCell(colIdx++).setCellValue(cmId); // census method
				result.createCell(colIdx++).setCellValue(cmName); // census
																	// method
				result.createCell(colIdx++).setCellValue(scientificName); // scientific
																			// name
				result.createCell(colIdx++).setCellValue(commonName); // common
																		// name
				result.createCell(colIdx++).setCellValue(locationId); // location
																		// name
				result.createCell(colIdx++).setCellValue(locationName); // location
																		// name
				result.createCell(colIdx++).setCellValue(latitude); // latitude
				result.createCell(colIdx++).setCellValue(longitude); // longitude
				result.createCell(colIdx++).setCellValue(epsg);
				result.createCell(colIdx++).setCellValue(date); // date
				result.createCell(colIdx++).setCellValue(timeString); // time
				result.createCell(colIdx++).setCellValue(numberSeen); // number
																		// seen
				result.createCell(colIdx++).setCellValue(notes); // notes
				result.createCell(colIdx++).setCellValue(surveyAttr1); // sdesc1
				result.createCell(colIdx++).setCellValue(surveyAttr2); // sdesc2
				result.createCell(colIdx++).setCellValue(cm1Attr1); // cm1 -
																	// desc1
				result.createCell(colIdx++).setCellValue(cm1Attr2); // cm1 -
																	// desc2
				result.createCell(colIdx++).setCellValue(cm2Attr1); // cm2 -
																	// desc3
				result.createCell(colIdx++).setCellValue(cm2Attr2); // cm2 -
																	// desc4
			} else {
				if (StringUtils.hasLength(id)) {
					result.createCell(colIdx++).setCellValue(
							Double.parseDouble(id)); // id
				} else {
					result.createCell(colIdx++).setCellValue(""); // id
				}
				if (StringUtils.hasLength(parentId)) {
					result.createCell(colIdx++).setCellValue(
							Double.parseDouble(parentId)); // parent id
				} else {
					result.createCell(colIdx++).setCellValue(""); // parent id
				}
				// result.createCell(colIdx++).setCellValue(StringUtils.hasLength(id)
				// ? Double.parseDouble(id) : ""); // id
				// result.createCell(colIdx++).setCellValue(StringUtils.hasLength(parentId)
				// ? Double.parseDouble(parentId) : ""); // parent id
				result.createCell(colIdx++).setCellValue(cmId); // census method
				result.createCell(colIdx++).setCellValue(cmName); // census
																	// method
				result.createCell(colIdx++).setCellValue(scientificName); // scientific
																			// name
				result.createCell(colIdx++).setCellValue(commonName); // common
																		// name
				result.createCell(colIdx++).setCellValue(locationId); // location
																		// name
				result.createCell(colIdx++).setCellValue(locationName); // location
																		// name
				result.createCell(colIdx++).setCellValue(latitude); // latitude
				result.createCell(colIdx++).setCellValue(longitude); // longitude
				result.createCell(colIdx++).setCellValue(epsg);
				result.createCell(colIdx++).setCellValue(date); // date
				result.createCell(colIdx++).setCellValue(timeString); // time
				result.createCell(colIdx++).setCellValue(numberSeen); // number
																		// seen
				result.createCell(colIdx++).setCellValue(notes); // notes
				result.createCell(colIdx++).setCellValue(surveyAttr1); // sdesc1
				result.createCell(colIdx++).setCellValue(surveyAttr2); // sdesc2
				result.createCell(colIdx++).setCellValue(cm1Attr1); // cm1 -
																	// desc1
				result.createCell(colIdx++).setCellValue(cm1Attr2); // cm1 -
																	// desc2
				result.createCell(colIdx++).setCellValue(cm2Attr1); // cm2 -
																	// desc3
				result.createCell(colIdx++).setCellValue(cm2Attr2); // cm2 -
																	// desc4
			}
			return result;
		}

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getCmId() {
            return cmId;
        }

        public void setCmId(String cmId) {
            this.cmId = cmId;
        }

        public String getCmName() {
            return cmName;
        }

        public void setCmName(String cmName) {
            this.cmName = cmName;
        }

        public String getLocationId() {
            return locationId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }

        public String getScientificName() {
            return scientificName;
        }

        public void setScientificName(String scientificName) {
            this.scientificName = scientificName;
        }

        public String getCommonName() {
            return commonName;
        }

        public void setCommonName(String commonName) {
            this.commonName = commonName;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
		
		public void setEpsg(String epsg) {
			this.epsg = epsg;
		}
		
		public String getEpsg() {
			return this.epsg;
		}

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Date getTime() {
            return time;
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public double getNumberSeen() {
            return numberSeen;
        }

        public void setNumberSeen(double numberSeen) {
            this.numberSeen = numberSeen;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public double getSurveyAttr1() {
            return surveyAttr1;
        }

        public void setSurveyAttr1(double surveyAttr1) {
            this.surveyAttr1 = surveyAttr1;
        }

        public String getSurveyAttr2() {
            return surveyAttr2;
        }

        public void setSurveyAttr2(String surveyAttr2) {
            this.surveyAttr2 = surveyAttr2;
        }

        public double getCm1Attr1() {
            return cm1Attr1;
        }

        public void setCm1Attr1(double cm1Attr1) {
            this.cm1Attr1 = cm1Attr1;
        }

        public String getCm1Attr2() {
            return cm1Attr2;
        }

        public void setCm1Attr2(String val) {
            this.cm1Attr2 = val;
        }

        public double getCm2Attr1() {
            return cm2Attr1;
        }

        public void setCm2Attr1(double cm2Attr1) {
            this.cm2Attr1 = cm2Attr1;
        }

        public String getCm2Attr2() {
            return cm2Attr2;
        }

        public void setCm2Attr2(String cm2Attr) {
            this.cm2Attr2 = cm2Attr;
        }

        public boolean getIdAsNumber() {
            return this.idAsNumber;
        }

        public void setIdAsNumber(boolean value) {
            this.idAsNumber = value;
        }
    }

    // for debugging obviously...
    private void logRecordAttributes(List<AttributeValue> recAttrList) {
        log.debug("actual record attributes:");
        for (AttributeValue recAttr : recAttrList) {
            Attribute attr = recAttr.getAttribute();
            switch (attr.getType()) {
            case INTEGER:
            case DECIMAL:
                // recAttr.setNumericValue(new BigDecimal(attributeValue));
                log.debug(recAttr.getAttribute().getDescription() + " : "
                        + recAttr.getNumericValue());
                break;
            case DATE:
                // SimpleDateFormat dateFormat = new SimpleDateFormat(
                // "dd MMM yyyy");
                // dateFormat.setLenient(false);
                // recAttr.setDateValue(dateFormat.parse(attributeValue));
                log.debug(recAttr.getAttribute().getDescription() + " : "
                        + recAttr.getDateValue());
                break;
            case IMAGE:
            case AUDIO:
            case VIDEO:
            case FILE:
                throw new UnsupportedOperationException(
                        "Spreadsheet upload of file data is not supported.");
            case CENSUS_METHOD_ROW:
            case CENSUS_METHOD_COL:
                throw new UnsupportedOperationException(
                        "Spreadsheet upload of census method data is not supported.");
            case TEXT:
            case STRING_WITH_VALID_VALUES:
            case STRING:
            default:
                // recAttr.setStringValue(attributeValue);
                log.debug(recAttr.getAttribute().getDescription() + " : "
                        + recAttr.getStringValue());
                break;
            }
        }
        log.debug("end actual record attributes");
    }

    private Record getRecordByCensusMethod(List<Record> recList, CensusMethod cm) {
        for (Record r : recList) {
            if (r.getCensusMethod().equals(cm)) {
                return r;
            }
        }
        return null;
    }

    private AttributeValue getRecAttr(Record rec, CensusMethod cm, String desc) {
        return getRecAttr(rec, getAttributeByDesc(cm, desc));
    }

    private AttributeValue getRecAttr(Record rec, Survey survey, String desc) {
        return getRecAttr(rec, getAttributeByDesc(survey, desc));
    }

    private AttributeValue getRecAttr(Record rec, Attribute attr) {
        for (AttributeValue ra : rec.getAttributes()) {
            if (ra.getAttribute().equals(attr)) {
                return ra;
            }
        }
        return null;
    }

    private Attribute getAttributeByDesc(CensusMethod cm, String desc) {
        return getAttributeByDesc(cm.getAttributes(), desc);
    }

    private Attribute getAttributeByDesc(Survey survey, String desc) {
        return getAttributeByDesc(survey.getAttributes(), desc);
    }

    private Attribute getAttributeByDesc(List<Attribute> attrList, String desc) {
        for (Attribute a : attrList) {
            if (desc.equals(a.getDescription())) {
                return a;
            }
        }
        return null;
    }

    private CensusMethod createCensusMethod(String name, Taxonomic taxonomic) {
        CensusMethod cm = new CensusMethod();
        cm.setName(name);
        cm.setTaxonomic(taxonomic);
        cm.setDescription("census method description");
        cm.setType("census method type");
        return cmDAO.save(cm);
    }

    private Attribute createAttribute(String name, String desc,
            boolean required, AttributeScope scope, boolean tag, String typecode) {
        Attribute a = new Attribute();
        a.setName(name);
        a.setDescription(desc);
        a.setRequired(required);
        a.setScope(scope);
        a.setTag(tag);
        a.setTypeCode(typecode);
        return attrDAO.save(a);
    }

    private List<User> createTestUsers() {
        List<User> users = new ArrayList<User>();
        PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
        String encodedPassword = passwordEncoder.encodePassword("password",
                null);
        User user = userDAO.createUser("user", "fn", "ln",
                "user@mailinator.com", encodedPassword, "usersIdent",
                new String[] { "ROLE_USER" });
        users.add(user);
        User admin = userDAO.createUser("admin", "firstn", "lastn",
                "admin@mailinator.com", encodedPassword, "adminIdent",
                new String[] { "ROLE_ADMIN" });
        users.add(admin);
        return users;

    }

    private void setRequired(Survey s, boolean b) {
        for (RecordPropertyType type : RecordPropertyType.values()) {
            new RecordProperty(s, type, metadataDAO).setRequired(b);
        }
    }

    private void setHidden(Survey s, boolean b) {
        for (RecordPropertyType type : RecordPropertyType.values()) {
            new RecordProperty(s, type, metadataDAO).setHidden(b);
        }
    }

    public Record createRecord(Survey survey, Point location, User user,
            IndicatorSpecies species, Date when, Long time, Date lastDate,
            Long lastTime, String notes, Boolean firstAppearance,
            Boolean lastAppearance, String behaviour, String habitat,
            Integer number, Map<Attribute, Object> attributes) {
        Record r = new Record();
        r.setSurvey(survey);
        r.setPoint(location);
        r.setUser(user);
        r.setSpecies(species);
        r.setWhen(when);
        r.setTime(time);
        r.setLastDate(lastDate);
        r.setLastTime(lastTime);
        r.setNotes(notes);
        r.setFirstAppearance(firstAppearance);
        r.setLastAppearance(lastAppearance);
        r.setBehaviour(behaviour);
        r.setHabitat(habitat);
        r.setNumber(number);
        Set<AttributeValue> atts = this.saveAttributeValues(r, attributes);
        r.setAttributes(atts);
        r = recDAO.save(r);
        return r;
    }
    
    public  Set<AttributeValue> saveAttributeValues(Record r, Map<Attribute, Object> attributeMap){
        Set<AttributeValue> atts = new HashSet<AttributeValue>();
        for (Map.Entry<Attribute, Object> attValue : attributeMap
                .entrySet()) {
            if (attValue != null) {
                AttributeValue attribute = new AttributeValue();
                attribute.setAttribute(attValue
                        .getKey());
                // attribute.setRecord(r);
                switch (attValue.getKey().getType()) {
                case IMAGE:
                case AUDIO:
                case VIDEO:
                case FILE:
//                    attrFile = fileAttributeMap.get(attValue.getKey());
//                    // attrFile will always have size zero unless the file
//                    // is changed. If there is already a file, but the
//                    // record is updated, without changing the file input,
//                    // addAttribute will be true but attrFile will
//                    // have size zero.
//                    boolean addAttribute;
//                    addAttribute =  attValue.getValue() != null;
//                    if(addAttribute && attrFile != null && attrFile.getSize() > 0) {
//                        attribute.setStringValue(attrFile.getOriginalFilename());
//                        
//                    }
                case HTML:
                case HTML_NO_VALIDATION:
                case HTML_COMMENT:
                case HTML_HORIZONTAL_RULE:
                case STRING:
                case STRING_WITH_VALID_VALUES:
                case BARCODE:
                case REGEX:
                case TEXT:
                    attribute.setStringValue((String) attValue.getValue());
                    break;
                case DATE:
                    attribute.setDateValue((Date) attValue.getValue());
                    break;
                case DECIMAL:
                    attribute.setNumericValue((BigDecimal) attValue.getValue());
                    break;
                case INTEGER:
                case INTEGER_WITH_RANGE:
                    attribute.setNumericValue(new BigDecimal((Integer) attValue
                            .getValue()));
                    break;
                case SPECIES:
                {
                        IndicatorSpecies species = (IndicatorSpecies)attValue.getValue();
                        attribute.setSpecies(species);
                        attribute.setStringValue(species != null ? species.getScientificName() : "");
                }
                        break;
                case CENSUS_METHOD_ROW:
                case CENSUS_METHOD_COL:
                    // don't save a string value for census method attributes
                    // their value is a collection of records containing the 
                    // attribute values of the census method
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid data type for attribute "
                                    + attValue.getKey());
                }
                attribute = recDAO.save(attribute);
                atts.add(attribute);
            }
        }
        return atts;
        
    }
    
    private AttributeValue getAttributeValueByAttrId(Record rec, Integer attrId) {
        for (AttributeValue av : rec.getAttributes()) {
            if (av.getAttribute().getId().equals(attrId)) {
                return av;
            }
        }
        return null;
    }
    
    private List<String> getHeaderList(Sheet sheet, int headerRowIdx) {
        int colIdx = 0;
        List<String> result = new ArrayList<String>();
        Row headerRow = sheet.getRow(headerRowIdx);
        Cell c = headerRow.getCell(colIdx, Row.CREATE_NULL_AS_BLANK);
        while (StringUtils.hasLength(c.getStringCellValue())) {
            c = headerRow.getCell(colIdx, Row.CREATE_NULL_AS_BLANK);
            result.add(c.getStringCellValue());
            log.debug("cell value : " + c.getStringCellValue());
            colIdx += 1;
        }
        return result;
    }
}
