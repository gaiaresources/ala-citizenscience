package au.com.gaiaresources.bdrs.serialization;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedCountRecordFilter;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedRecordFilter;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Geometry;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tests RecordCsvWriter
 */
public class RecordCsvWriterTest extends AbstractGridControllerTest {

    private static final double ASSERT_COORD_TOLERANCE = 0.000001;

    private static final String TEST_CONTEXT = "http://test.gaiaresources.com.au/BDRS";

    private RedirectionService redirectionService;

    private SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();

    @Autowired
    private GeoMapService geoMapService;

    private CsvReader csvReader = null;

    @Before
    public void setup() {
        redirectionService = new RedirectionService(TEST_CONTEXT);
        // create the geo map object...
        geoMapService.getForSurvey(survey1);
    }

    @After
    public void tearDown() {
        if (csvReader != null) {
            csvReader.close();
        }
    }

    @Test
    public void testCsv() throws IOException {
        runTest();
    }

    @Test
    public void testCsvAttrNameCollision() throws IOException {

        // lets create the worse case scenario and name all of the survey
        // attributes the same...
        for (Attribute attribute : survey1.getAttributes()) {
            attribute.setName("test_attr_name");
            attributeDAO.save(attribute);
        }
        runTest();
    }

    @Test
    public void testEastingNorthingNoZone() throws IOException {

        survey1.getMap().setCrs(BdrsCoordReferenceSystem.MGA);
        runTest();
    }

    @Test
    public void testEastingNorthingWithZone() throws IOException {
        survey1.getMap().setCrs(BdrsCoordReferenceSystem.MGA50);

        // we also need to set new lat/lon coords that lie within mga 50 zone

        AdvancedRecordFilter filter = new AdvancedRecordFilter();
        filter.setSurveyPk(survey1.getId());
        ScrollableRecords sr = recDAO.getScrollableRecords(filter);

        SpatialUtil spatialUtil = spatialUtilFactory.
                getLocationUtil(BdrsCoordReferenceSystem.DEFAULT_SRID);

        double lat = -31.123456;
        double lon = 115.123456;
        double increment = 0.000001;

        while (sr.hasMoreElements()) {
            Record r = sr.nextElement();

            r.setGeometry(spatialUtil.createPoint(lat, lon));

            lat += increment;
            lon += increment;
            recDAO.save(r);
        }

        runTest();
    }

    @Test
    public void testHeld() throws IOException {

        // we also need to set new lat/lon coords that lie within mga 50 zone

        AdvancedRecordFilter filter = new AdvancedRecordFilter();
        filter.setSurveyPk(survey1.getId());
        ScrollableRecords sr = recDAO.getScrollableRecords(filter);


        while (sr.hasMoreElements()) {
            Record r = sr.nextElement();
            r.setHeld(true);
            recDAO.save(r);
        }

        int csvRecCount = 0;

        csvRecCount += assertCensusMethodRecords(null);
        for (CensusMethod cm : survey1.getCensusMethods()) {
            csvRecCount += assertCensusMethodRecords(cm);
        }

        // expect no records as everything is held
        Assert.assertEquals("mismatch record count", csvRecCount, 0);
    }

    @Test
    public void testMixedScope() throws IOException {

        int i = 0;
        for (Attribute a : survey1.getAttributes()) {
            if (++i % 2 == 0) {
                a.setScope(AttributeScope.RECORD);
            } else {
                a.setScope(AttributeScope.SURVEY);
            }
            attributeDAO.save(a);
        }
        runTest();
    }

    private void runTest() throws IOException {
        AdvancedCountRecordFilter recFilter = new AdvancedCountRecordFilter();
        recFilter.setSurveyPk(survey1.getId());
        recFilter.setRecordVisibility(RecordVisibility.PUBLIC);
        Integer count = recordDAO.countRecords(recFilter);
        Assert.assertTrue("no records to write", count > 0);

        int csvRecCount = 0;

        csvRecCount += assertCensusMethodRecords(null);
        for (CensusMethod cm : survey1.getCensusMethods()) {
            csvRecCount += assertCensusMethodRecords(cm);
        }

        Assert.assertEquals("mismatch record count", csvRecCount, count.intValue());

    }

    private int assertCensusMethodRecords(CensusMethod cm) throws IOException {

        Survey survey = survey1;
        RecordCsvWriter csvWriter = new RecordCsvWriter(recordDAO, metaDAO, redirectionService, survey, cm);

        File f = File.createTempFile("testexportrecord", ".csv");
        FileWriter writer = new FileWriter(f);

        try {
            csvWriter.writeRecords(writer);
        } finally {
            writer.close();
        }

        Assert.assertNotNull("File should be non null", f);
        FileReader fileReader = new FileReader(f);
        csvReader = new CsvReader(fileReader);

        boolean readHeaders = csvReader.readHeaders();
        Assert.assertTrue("failed to read headers", readHeaders);

        String[] headerArray = csvReader.getHeaders();
        @SuppressWarnings("unchecked") List<String> headerList = Arrays.asList(headerArray);

        assertHeader(headerList, RecordCsvWriter.HEADER_RECORD_ID);
        assertHeader(headerList, RecordCsvWriter.HEADER_OWNER_FIRST);
        assertHeader(headerList, RecordCsvWriter.HEADER_OWNER_LAST);

        int csvRecCount = 0;
        // iterate over all records
        while (csvReader.readRecord()) {
            assertRecord(csvReader, csvWriter.getHeaderMap());
            ++csvRecCount;
        }
        return csvRecCount;
    }

    private void assertHeader(List<String> headerList, String targetHeader) {
        Assert.assertTrue("missing header: " + targetHeader,
                headerList.contains(targetHeader));
    }

    private void assertRecord(CsvReader csvReader,
                              Map<Attribute, String> attrHeaderMap) throws IOException {

        Assert.assertEquals("column count should be the same as header count",
                csvReader.getColumnCount(), csvReader.getHeaderCount());

        Integer recId = Integer.valueOf(csvReader.get(RecordCsvWriter.HEADER_RECORD_ID));
        Record rec = recordDAO.getRecord(recId);
        Assert.assertNotNull("record must exist", rec);

        String ownerFirst = csvReader.get(RecordCsvWriter.HEADER_OWNER_FIRST);
        String ownerLast = csvReader.get(RecordCsvWriter.HEADER_OWNER_LAST);

        Assert.assertEquals("wrong owner first name", rec.getUser().getFirstName(), ownerFirst);
        Assert.assertEquals("Wrong owner last name", rec.getUser().getLastName(), ownerLast);

        BdrsCoordReferenceSystem crs = rec.getSurvey().getCrs();

        int srid = crs.getSrid();
        if (srid == BdrsCoordReferenceSystem.NO_SPECIFIED_ZONE) {
            srid = BdrsCoordReferenceSystem.DEFAULT_SRID;
        }
        SpatialUtil spatialUtil = spatialUtilFactory.
                getLocationUtil(srid);

        Double xCoord = Double.valueOf(csvReader.get(crs.getXname()));
        Double yCoord = Double.valueOf(csvReader.get(crs.getYname()));

        // Transform the record geom into the projection of the survey.
        Geometry geom = spatialUtil.transform(rec.getGeometry());

        Assert.assertEquals("wrong x coord", geom.getCentroid().getX(),
                xCoord, ASSERT_COORD_TOLERANCE);
        Assert.assertEquals("wrong y xoord", geom.getCentroid().getY(),
                yCoord, ASSERT_COORD_TOLERANCE);

        for (AttributeValue av : rec.getAttributes()) {
            Attribute attr = av.getAttribute();
            AttributeType attrType = attr.getType();

            if (!RecordCsvWriter.IGNORE_ATTR_TYPES.contains(attrType)) {
                String csvHeaderName = attrHeaderMap.get(av.getAttribute());
                String csvAvValue = csvReader.get(csvHeaderName);
                String expectedValue = null;

                switch (attrType) {
                    case FILE:
                    case AUDIO:
                    case VIDEO:
                    case IMAGE:
                        expectedValue = redirectionService.getFileDownloadUrl(av, true);
                    break;
                    case SPECIES:
                        if (av.getSpecies() != null) {
                            expectedValue = av.getSpecies().getScientificName();
                        }
                      break;
                    default:
                        expectedValue = av.getValue();
                }
                if (expectedValue == null) {
                    expectedValue = "";
                }
                Assert.assertEquals("wrong value for attr : "
                                + attr.getName() + ", header name : " + csvHeaderName
                                + ", type : " + av.getAttribute().getTypeCode(),
                        expectedValue, csvAvValue);
            }

        }
    }
}
