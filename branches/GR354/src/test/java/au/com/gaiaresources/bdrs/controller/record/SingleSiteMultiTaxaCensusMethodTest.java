package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.db.WeightComparator;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.method.Taxonomic;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Tests the SingleSiteMulitTaxaController using a form containing nested census method attributes.
 */
public class SingleSiteMultiTaxaCensusMethodTest extends AbstractCensusMethodAttributeTest {

    @Autowired
    CensusMethodDAO cmDAO;
    @Autowired
    AttributeDAO attrDAO;
    @Autowired
    MetadataDAO metadataDAO;
    @Autowired
    SurveyDAO surveyDAO;
    @Autowired
    RecordDAO recordDAO;
    @Autowired
    TaxaDAO taxaDAO;

    private CensusMethod photoPoint;
    private CensusMethod cameraPosition;
    private CensusMethod images;
    private Survey survey;

    private TaxonGroup trees;
    private IndicatorSpecies eucalyptus;
    private IndicatorSpecies wattle;

    private Attribute project;
    private Attribute speciesNotes;
    private Attribute photoPointAttr;
    private Attribute photoPointName;
    private Attribute photoDate;
    private Attribute cameraPositionAttr;
    private Attribute lat;
    private Attribute lon;
    private Attribute imagesAttr;
    private Attribute compassBearing;

    @Before
    public void setup() throws Exception {
        // Species group and species.
        trees = new TaxonGroup();
        trees.setName("Trees");
        trees = taxaDAO.save(trees);

        eucalyptus = new IndicatorSpecies();
        eucalyptus.setCommonName("Red Stringybark");
        eucalyptus.setScientificName("Eucalyptus macrohyncha");
        eucalyptus.setTaxonGroup(trees);
        eucalyptus = taxaDAO.save(eucalyptus);

        wattle = new IndicatorSpecies();
        wattle.setCommonName("Wattle");
        wattle.setScientificName("Acacia leucolboia");
        wattle.setTaxonGroup(trees);
        wattle = taxaDAO.save(wattle);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = dateFormat.parse("2013-03-17");

        // Survey configuration
        survey = new Survey();
        survey.setName("Tree planting");
        survey.setActive(true);
        survey.setStartDate(startDate);
        survey.setDescription("Tree planting information");
        Metadata md = survey.setFormRendererType(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
        metadataDAO.save(md);

        project = addAttribute(survey, "project", "Project", AttributeType.TEXT, AttributeScope.SURVEY);
        speciesNotes = addAttribute(survey, "Species_Notes", "Notes", AttributeType.TEXT,  AttributeScope.RECORD);

        photoPoint = new CensusMethod();
        photoPoint.setName("Photopoint");
        photoPoint.setTaxonomic(Taxonomic.NONTAXONOMIC);
        cmDAO.save(photoPoint);

        photoPointName = addAttribute(photoPoint, "Photopoint_Name", "Photopoint Name", AttributeType.TEXT, null);
        photoDate = addAttribute(photoPoint, "Photo_Date", "Photo Date", AttributeType.DATE, null);
        photoPointAttr = buildAttribute("Photopoint", "Photopoint", AttributeType.CENSUS_METHOD_COL, AttributeScope.SURVEY);
        photoPointAttr.setCensusMethod(photoPoint);
        survey.getAttributes().add(photoPointAttr);

        cameraPosition = new CensusMethod();
        cameraPosition.setName("Camera_Position");
        cameraPosition.setTaxonomic(Taxonomic.NONTAXONOMIC);
        cmDAO.save(cameraPosition);

        lat = addAttribute(cameraPosition, "Latitude", "Latitude", AttributeType.DECIMAL, null);
        lon = addAttribute(cameraPosition, "Longitude", "Longitude", AttributeType.DECIMAL, null);
        cameraPositionAttr = buildAttribute("Camera_Position", "Camera Position", AttributeType.CENSUS_METHOD_ROW, AttributeScope.SURVEY);
        cameraPositionAttr.setCensusMethod(cameraPosition);
        photoPoint.getAttributes().add(cameraPositionAttr);

        images = new CensusMethod();
        images.setName("Photopoint_Images");
        images.setTaxonomic(Taxonomic.NONTAXONOMIC);
        cmDAO.save(images);
        compassBearing = addAttribute(images, "Compass_Bearing", "Compass bearing", AttributeType.DECIMAL, null);
        // Might leave the image out to avoid complications with processing the multipart file upload.
        //addAttribute(images, "Image", "Image", AttributeType.IMAGE, null);
        photoPoint.getCensusMethods().add(images);

        imagesAttr = buildAttribute("Images", "Images", AttributeType.CENSUS_METHOD_COL, AttributeScope.SURVEY);
        imagesAttr.setCensusMethod(images);
        photoPoint.getAttributes().add(imagesAttr);
        survey = surveyDAO.save(survey);

        // all tests in this class are attempting to edit a record
        request.setParameter(RecordWebFormContext.PARAM_EDIT, Boolean.TRUE.toString());

    }

    private Attribute addAttribute(Survey survey, String name, String description, AttributeType type, AttributeScope scope) {
        Attribute attr = buildAttribute(name, description, type, scope);
        survey.getAttributes().add(attr);
        return attr;
    }

    private Attribute addAttribute(CensusMethod censusMethod, String name, String description, AttributeType type, AttributeScope scope) {
        Attribute attr = buildAttribute(name, description, type, scope);
        censusMethod.getAttributes().add(attr);
        return attr;
    }

    private Attribute buildAttribute(String name, String description, AttributeType type, AttributeScope scope) {
        Attribute attr = new Attribute();
        attr.setName(name);
        attr.setDescription(description);
        attr.setTypeCode(type.getCode());
        attr.setScope(scope);
        attrDAO.save(attr);
        return attr;
    }

    private Attribute findAttributeByName(String name) {
        for (Attribute attr : survey.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        CensusMethod[] methods = {photoPoint, cameraPosition, images};
        for (CensusMethod method : methods) {
            for (Attribute attr : method.getAttributes()) {
                if (attr.getName().equals(name)) {
                    return attr;
                }
            }
        }
        throw new IllegalArgumentException("No such attribute name");
    }

    /** The server expects a multipart http request */
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }

    /**
     * Tests adding new records using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testAddTwoSpeciesWithSinglePhotopoint() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // And 2 wattle trees
        addRecordScopedValues(1, wattle, 2);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");

        handle(request, response);

        try {
        // Should be two records, one Eucalpyt and one Wattle.
        List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(2, records.size());

        if (records.get(0).getSpecies().equals(wattle)) {
            Collections.reverse(records);
        }

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());

        String[][] photoPointData = {
            {"Photopoint 1", "18 Mar 2013", "-32.000", "141.000"}
        };
        String [][] imageData = {
            {"333"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);

        Record wattleRecord = records.get(1);
        Assert.assertEquals(Integer.valueOf(2), wattleRecord.getNumber());
        Assert.assertEquals(wattle, wattleRecord.getSpecies());
        Assert.assertEquals("Notes 1", wattleRecord.valueOfAttribute(speciesNotes).getStringValue());

        checkSurveyScopedResults(wattleRecord, photoPointData, imageData);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests adding a new record using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testAddSingleSpeciesWithTwoPhotoPointsWithMultipleImages() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        // Now add another photopoint
        String photoPoint2Prefix = addPhotoPoint(1, "Photopoint 2", "19 Mar 2013");
        addCameraPosition(photoPoint2Prefix, "-32.100", "141.100");
        addImageRow(photoPoint2Prefix, 0, "666");

        handle(request, response);

        // Should be two records, one Eucalpyt and one Wattle.
        final List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(1, records.size());

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1", "18 Mar 2013", "-32.000", "141.000"},
                {"Photopoint 2", "19 Mar 2013", "-32.100", "141.100"}
        };
        String [][] imageData = {
                {"333", "444", "555"},
                {"666"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);
    }

    /**
     * Tests adding new records using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testAddTwoSpeciesWithTwoPhotoPointsWithMultipleImages() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // And 2 wattle trees
        addRecordScopedValues(1, wattle, 2);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        // Now add another photopoint
        String photoPoint2Prefix = addPhotoPoint(1, "Photopoint 2", "19 Mar 2013");
        addCameraPosition(photoPoint2Prefix, "-32.100", "141.100");
        addImageRow(photoPoint2Prefix, 0, "666");

        handle(request, response);

        // Should be two records, one Eucalpyt and one Wattle.
        final List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(2, records.size());

        if (records.get(0).getSpecies().equals(wattle)) {
            Collections.reverse(records);
        }

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1", "18 Mar 2013", "-32.000", "141.000"},
                {"Photopoint 2", "19 Mar 2013", "-32.100", "141.100"}
        };
        String [][] imageData = {
                {"333", "444", "555"},
                {"666"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);

        Record wattleRecord = records.get(1);
        Assert.assertEquals(Integer.valueOf(2), wattleRecord.getNumber());
        Assert.assertEquals(wattle, wattleRecord.getSpecies());
        Assert.assertEquals("Notes 1", wattleRecord.valueOfAttribute(speciesNotes).getStringValue());

        checkSurveyScopedResults(wattleRecord, photoPointData, imageData);
    }


    /**
     * Tests editing records using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testEditSinglePhotopointWithSingleSpecies() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        handle(request, response);

        // Should be a single record.
        final List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(1, records.size());

        // Now edit the photopoint - new request.
        request.removeAllParameters();
        response = new MockHttpServletResponse();

        Record result = records.get(0);
        setupRequestAndRecordProperties(result.getId());
        addRecordScopedValues(0, eucalyptus, 1, result.getId());
        photoPointPrefix = addPhotoPoint(0, "Photopoint 1 - edited", "17 Mar 2013", getRecordId(photoPointAttr, 0, result));
        addCameraPosition(photoPointPrefix, "-33.000", "142.000", getRecordId(cameraPositionAttr, 0, result));

        addImageRow(photoPointPrefix, 0, "3333", getRecordId(imagesAttr, 0, result));
        addImageRow(photoPointPrefix, 1, "4444", getRecordId(imagesAttr, 1, result));
        addImageRow(photoPointPrefix, 2, "5555", getRecordId(imagesAttr, 2, result));

        System.out.println();
        handle(request, response);

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1 - edited", "17 Mar 2013", "-33.000", "142.000"}
        };
        String [][] imageData = {
                {"3333", "4444", "5555"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);
    }

    /**
     * Tests editing records using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testEditTwoPhotopointsWithSingleSpecies() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        // Now add another photopoint
        String photoPoint2Prefix = addPhotoPoint(1, "Photopoint 2", "19 Mar 2013");
        addCameraPosition(photoPoint2Prefix, "-32.100", "141.100");
        addImageRow(photoPoint2Prefix, 0, "666");

        handle(request, response);

        // Should be a single record.
        final List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(1, records.size());

        // Now edit the photopoint - new request.
        request.removeAllParameters();
        response = new MockHttpServletResponse();

        Record result = records.get(0);
        setupRequestAndRecordProperties(result.getId());
        addRecordScopedValues(0, eucalyptus, 1, result.getId());
        photoPointPrefix = addPhotoPoint(0, "Photopoint 1 - edited", "17 Mar 2013", getRecordId(photoPointAttr, 0, 0, result));
        addCameraPosition(photoPointPrefix, "-33.000", "142.000", getRecordId(cameraPositionAttr, 0, result));

        addImageRow(photoPointPrefix, 0, "3333", getRecordId(imagesAttr, 0, 0, result));
        addImageRow(photoPointPrefix, 1, "4444", getRecordId(imagesAttr, 0, 1, result));
        addImageRow(photoPointPrefix, 2, "5555", getRecordId(imagesAttr, 0, 2, result));

        // Now edit the other photopoint
        photoPoint2Prefix = addPhotoPoint(1, "Photopoint 2 - edited", "18 Mar 2013", getRecordId(photoPointAttr, 1, result));
        addCameraPosition(photoPoint2Prefix, "-32.110", "141.110", getRecordId(cameraPositionAttr, 1, 0, result));
        addImageRow(photoPoint2Prefix, 0, "6666", getRecordId(imagesAttr, 1, 0, result));

        System.out.println();
        handle(request, response);

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1 - edited", "17 Mar 2013", "-33.000", "142.000"},
                {"Photopoint 2 - edited", "18 Mar 2013", "-32.110", "141.110"}
        };
        String [][] imageData = {
                {"3333", "4444", "5555"},
                {"6666"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);
    }

    /**
     * Tests editing records using a single site multi taxa survey configured to include nested
     * census method attributes.
     */
    @Test
    public void testEditSinglePhotopointWithTwoSpecies() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // And 2 wattle trees
        addRecordScopedValues(1, wattle, 2);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        handle(request, response);

        // Should be a single record.
        final List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(2, records.size());

        // Now edit the photopoint - new request.
        request.removeAllParameters();
        response = new MockHttpServletResponse();

        Record result = records.get(0);
        setupRequestAndRecordProperties(result.getId());
        addRecordScopedValues(0, eucalyptus, 1, result.getId());
        addRecordScopedValues(1, wattle, 2, records.get(1).getId());

        photoPointPrefix = addPhotoPoint(0, "Photopoint 1 - edited", "17 Mar 2013", getRecordId(photoPointAttr, 0, result));
        addCameraPosition(photoPointPrefix, "-33.000", "142.000", getRecordId(cameraPositionAttr, 0, result));

        addImageRow(photoPointPrefix, 0, "3333", getRecordId(imagesAttr, 0, 0, result));
        addImageRow(photoPointPrefix, 1, "4444", getRecordId(imagesAttr, 0, 1, result));
        addImageRow(photoPointPrefix, 2, "5555", getRecordId(imagesAttr, 0, 2, result));

        System.out.println();
        handle(request, response);

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1 - edited", "17 Mar 2013", "-33.000", "142.000"}
        };
        String [][] imageData = {
                {"3333", "4444", "5555"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);

        Record wattleRecord = records.get(1);
        Assert.assertEquals(Integer.valueOf(2), wattleRecord.getNumber());
        Assert.assertEquals(wattle, wattleRecord.getSpecies());
        Assert.assertEquals("Notes 1", wattleRecord.valueOfAttribute(speciesNotes).getStringValue());

        checkSurveyScopedResults(wattleRecord, photoPointData, imageData);
    }

   /*
    * Tests editing records using a single site multi taxa survey configured to include nested
    * census method attributes.
    */
    @Test
    public void testEditSinglePhotopointAddNewSpecies() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        Assert.assertEquals(0, recordDAO.countRecords(getRequestContext().getUser()).intValue());

        setupRequestAndRecordProperties();

        // Populate record scoped attributes (not including census method attributes)

        // We've planted one Eucalypt tree.
        addRecordScopedValues(0, eucalyptus, 1);

        // We'll start with a single photopoint - it's new so no record id.
        String photoPointPrefix = addPhotoPoint(0, "Photopoint 1", "18 Mar 2013");

        addCameraPosition(photoPointPrefix, "-32.000", "141.000");

        // Now the embedded image census method (row based)
        addImageRow(photoPointPrefix, 0, "333");
        addImageRow(photoPointPrefix, 1, "444");
        addImageRow(photoPointPrefix, 2, "555");

        handle(request, response);

        // Should be a single record.
        List<Record> records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(1, records.size());

        // Now edit the photopoint - new request.
        request.removeAllParameters();
        response = new MockHttpServletResponse();

        Record result = records.get(0);
        setupRequestAndRecordProperties(result.getId());

        addRecordScopedValues(0, eucalyptus, 1, result.getId());

        // Now add a new species
        addRecordScopedValues(1, wattle, 2);

        photoPointPrefix = addPhotoPoint(0, "Photopoint 1 - edited", "17 Mar 2013", getRecordId(photoPointAttr, 0, result));
        addCameraPosition(photoPointPrefix, "-33.000", "142.000", getRecordId(cameraPositionAttr, 0, result));

        addImageRow(photoPointPrefix, 0, "3333", getRecordId(imagesAttr, 0, 0, result));
        addImageRow(photoPointPrefix, 1, "4444", getRecordId(imagesAttr, 0, 1, result));
        addImageRow(photoPointPrefix, 2, "5555", getRecordId(imagesAttr, 0, 2, result));

        System.out.println();
        handle(request, response);

        records = recordDAO.getRecords(getRequestContext().getUser());
        Assert.assertEquals(2, records.size());

        Record eucalyptRecord = records.get(0);
        Assert.assertEquals(Integer.valueOf(1), eucalyptRecord.getNumber());
        Assert.assertEquals(eucalyptus, eucalyptRecord.getSpecies());
        Assert.assertEquals("Notes 0", eucalyptRecord.valueOfAttribute(speciesNotes).getStringValue());


        String[][] photoPointData = {
                {"Photopoint 1 - edited", "17 Mar 2013", "-33.000", "142.000"}
        };
        String [][] imageData = {
                {"3333", "4444", "5555"}
        };
        checkSurveyScopedResults(eucalyptRecord, photoPointData, imageData);

        Record wattleRecord = records.get(1);
        Assert.assertEquals(Integer.valueOf(2), wattleRecord.getNumber());
        Assert.assertEquals(wattle, wattleRecord.getSpecies());
        Assert.assertEquals("Notes 1", wattleRecord.valueOfAttribute(speciesNotes).getStringValue());

        checkSurveyScopedResults(wattleRecord, photoPointData, imageData);
    }


    private void addImageRow(String photoPointPrefix, int rowIndex, String bearingValue) {
        addImageRow(photoPointPrefix, rowIndex, bearingValue, 0);
    }

    private void addImageRow(String photoPointPrefix, int rowIndex, String bearingValue, long recordId) {
        String imagesPrefix = photoPointPrefix+"_"+rowIndex+"_attribute_"+imagesAttr.getId();
        request.setParameter(imagesPrefix+"_rowIndex", Integer.toString(rowIndex));
        if (recordId > 0) {
            imagesPrefix += "_record_"+recordId;
            request.addParameter(imagesPrefix+"_recordId", Long.toString(recordId));

            request.addParameter(photoPointPrefix+"_attribute_"+imagesAttr.getId()+"_rowPrefix",
                    imagesPrefix+"_");
        }
        else {
            request.addParameter(photoPointPrefix+"_attribute_"+imagesAttr.getId()+"_rowPrefix", rowIndex+"_");
            request.addParameter(imagesPrefix+"_recordId", "0");
            imagesPrefix += "_record";
        }
        // Now the values
        request.setParameter(imagesPrefix + "_attribute_" + compassBearing.getId(), bearingValue);

        request.addParameter(photoPointPrefix + "_attribute_" + imagesAttr.getId() + "_rowPrefix", rowIndex + "_");

    }


    private void addCameraPosition(String photoPointPrefix, String latValue, String lonValue) {
        addCameraPosition(photoPointPrefix, latValue, lonValue, 0);
    }

    private void addCameraPosition(String photoPointPrefix, String latValue, String lonValue, long recordId) {
        String cameraPositionPrefix = photoPointPrefix+"_attribute_"+cameraPositionAttr.getId();
        request.addParameter(cameraPositionPrefix + "_rowIndex", "0");
        if (recordId > 0) {
            cameraPositionPrefix += "_record_"+recordId;
            request.setParameter(cameraPositionPrefix+"_recordId", Long.toString(recordId));

        }
        else {
            request.setParameter(cameraPositionPrefix+"_recordId", Long.toString(recordId));
            cameraPositionPrefix += "_record";
        }
        // Now the embedded column based census method attribute (for lat/lon)
        request.setParameter(cameraPositionPrefix+"_attribute_"+lat.getId(), latValue);
        request.setParameter(cameraPositionPrefix+"_attribute_"+lon.getId(), lonValue);
    }

    private String addPhotoPoint(int rowIndex, String name, String photoDateStr) {
        return addPhotoPoint(rowIndex, name, photoDateStr, 0);
    }

    private String addPhotoPoint(int rowIndex, String name, String photoDateStr, long recordId) {

        String photoPointPrefix = rowIndex+"_attribute_"+photoPointAttr.getId();
        request.setParameter(photoPointPrefix+"_rowIndex", Integer.toString(rowIndex));

        if (recordId > 0) {
            request.addParameter("attribute_"+photoPointAttr.getId()+"_rowPrefix",
                    rowIndex+"_attribute_"+photoPointAttr.getId()+"_record_"+recordId+"_");
            request.addParameter(photoPointPrefix+"_recordId", Long.toString(recordId));
            photoPointPrefix += "_record_"+recordId;
            request.addParameter(photoPointPrefix+"_recordId", Long.toString(recordId));


        }
        else {
            request.addParameter("attribute_"+photoPointAttr.getId()+"_rowPrefix", rowIndex+"_");
            request.setParameter(photoPointPrefix+"_recordId", Long.toString(recordId));
            photoPointPrefix += "_record";
        }

        request.setParameter(photoPointPrefix+"_attribute_"+photoPointName.getId(), name);
        request.setParameter(photoPointPrefix+"_attribute_"+photoDate.getId(), photoDateStr);

        return photoPointPrefix;
    }

    private void addRecordScopedValues(int speciesRowIndex, IndicatorSpecies species, int numberSeen) {
        addRecordScopedValues(speciesRowIndex, species, numberSeen, 0);
    }

    private void addRecordScopedValues(int speciesRowIndex, IndicatorSpecies species, int numberSeen, long recordId) {
        if (recordId > 0) {
            request.setParameter(speciesRowIndex+"_recordId", Long.toString(recordId));
        }
        request.addParameter("rowPrefix", speciesRowIndex+"_");
        request.setParameter(speciesRowIndex+"_survey_species_search", species.getScientificName());
        request.setParameter(speciesRowIndex+"_species", Integer.toString(species.getId()));
        request.setParameter(speciesRowIndex+"_number", Integer.toString(numberSeen));
        request.setParameter(speciesRowIndex+"_attribute_"+Integer.toString(speciesNotes.getId()), "Notes "+speciesRowIndex);
    }

    private void setupRequestAndRecordProperties() {
        setupRequestAndRecordProperties(-1);
    }

    private void setupRequestAndRecordProperties(long recordId) {
        request.setMethod("POST");
        request.setRequestURI(request.getContextPath()+"/bdrs/user/singleSiteMultiTaxa.htm");
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        request.setParameter("submit", "Submit");
        request.setParameter("surveyId", Long.toString(survey.getId()));
        if (recordId > 0) {
            request.setParameter("recordId", Long.toString(recordId));
        }

        // Populate survey scoped record properties
        request.setParameter("latitude", "-36.879620605027");
        request.setParameter("longitude", "126.650390625");
        request.setParameter("date", "18 Mar 2013");
        request.setParameter("time", "11:00");
        request.setParameter("notes", "Test notes");

        // Populate survey scoped attributes (not including census method attributes)
        request.setParameter("attribute_"+project.getId(), "Test Project");
    }

    private void checkSurveyScopedResults(Record record, String[][] photoPointAttributes, String[][] imageAttributes) {
        Assert.assertEquals("Test notes", record.getNotes());
        Assert.assertEquals("Test Project", record.valueOfAttribute(project).getStringValue());

        AttributeValue photopointValue = record.valueOfAttribute(photoPointAttr);
        Assert.assertEquals(photoPointAttr, photopointValue.getAttribute());

        // Each record should have a one or more child records containing attribute data for the photopoints.
        Assert.assertEquals(photoPointAttributes.length, photopointValue.getRecords().size());

        List<Record> records = new ArrayList<Record>(photopointValue.getRecords());
        Collections.sort(records, new WeightComparator());
        for (int i=0; i<records.size(); i++) {
            Record photoPointRecord = records.get(i);
            Assert.assertEquals("Record "+i,Integer.valueOf(i), photoPointRecord.getWeight());
            Assert.assertEquals("Record "+i, photoPointAttributes[i][0], photoPointRecord.valueOfAttribute(photoPointName).getStringValue());
            Assert.assertEquals("Record "+i,photoPointAttributes[i][1], photoPointRecord.valueOfAttribute(photoDate).getStringValue());
            AttributeValue cameraValue = photoPointRecord.valueOfAttribute(cameraPositionAttr);
            Assert.assertEquals(cameraPositionAttr, cameraValue.getAttribute());
            Assert.assertEquals(1, cameraValue.getRecords().size());
            Record cameraRecord = cameraValue.getRecords().iterator().next();
            Assert.assertEquals(photoPointAttributes[i][2], cameraRecord.valueOfAttribute(lat).getStringValue());
            Assert.assertEquals(photoPointAttributes[i][3], cameraRecord.valueOfAttribute(lon).getStringValue());

            AttributeValue imagesValue = photoPointRecord.valueOfAttribute(imagesAttr);
            Assert.assertEquals(imagesAttr, imagesValue.getAttribute());

            Assert.assertEquals(imageAttributes[i].length, imagesValue.getRecords().size());
            List<Record> imageRecords = new ArrayList<Record>(imagesValue.getRecords());
            Collections.sort(imageRecords, new WeightComparator());
            for (int j=0; j<imageRecords.size(); j++) {
                Record imageRecord = imageRecords.get(j);
                Assert.assertEquals(imageAttributes[i][j], imageRecord.valueOfAttribute(compassBearing).getStringValue());
            }
        }


    }


    private long getRecordId(Attribute attribute, int rowIndex, Record record) {
        AttributeValue value = record.valueOfAttribute(attribute);
        if (value != null) {
            List<Record> records = new ArrayList<Record>(value.getRecords());
            Collections.sort(records, new WeightComparator());
            return records.get(rowIndex).getId();
        }

        for (AttributeValue tmpVal : record.getAttributes()) {
            if (tmpVal.getRecords() != null) {
                for (Record tmpRecord : tmpVal.getRecords()) {
                    long recordId = getRecordId(attribute, rowIndex, tmpRecord);
                    if (recordId > 0) {
                        return recordId;
                    }
                }
            }
        }
        return -1;
    }

    private long getRecordId(Attribute attribute, int photoPointRowIndex, int attributeRowIndex, Record record) {
        if (attribute == photoPointAttr) {
            return getRecordId(attribute, photoPointRowIndex, record);
        }
        else {
            List<Record> photoPointRecords = new ArrayList<Record>(record.valueOfAttribute(photoPointAttr).getRecords());
            Collections.sort(photoPointRecords, new WeightComparator());
            return getRecordId(attribute, attributeRowIndex, photoPointRecords.get(photoPointRowIndex));
        }

    }
}