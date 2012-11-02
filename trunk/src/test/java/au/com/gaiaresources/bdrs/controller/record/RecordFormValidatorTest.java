package au.com.gaiaresources.bdrs.controller.record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.record.validator.DateValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.DoubleRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.HistoricalDateValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.IntRangeValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.StringValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.TaxonValidator;
import au.com.gaiaresources.bdrs.controller.record.validator.Validator;
import au.com.gaiaresources.bdrs.model.map.GeoMap;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.service.property.PropertyService;

public class RecordFormValidatorTest extends AbstractControllerTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private TaxaDAO taxaDAO;
    
    @Autowired
    private SurveyDAO surveyDAO;
    
    @Autowired
    private TaxaService taxaService;

    private Map<String, String[]> paramMap;
    private Map<String, String> errorMap;

    @Before
    public void setUp() throws Exception {
        paramMap = new HashMap<String, String[]>();
        errorMap = new HashMap<String, String>();
    }

    @Test
    public void testDateValidator() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        Calendar cal = new GregorianCalendar();

        Date valid = dateFormat.parse("14 Dec 2010");

        cal.setTime(valid);
        cal.add(Calendar.DAY_OF_WEEK, -7);
        Date earliest = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, -1);
        Date invalidEarly = cal.getTime();

        cal.setTime(valid);
        cal.add(Calendar.DAY_OF_WEEK, 7);
        Date latest = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, 1);
        Date invalidLate = cal.getTime();

        String key = "test";
        Validator validator = new DateValidator(propertyService, true, false,
                earliest, latest);

        // Bounadary Test
        paramMap.put(key, new String[] { dateFormat.format(valid) });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { dateFormat.format(earliest) });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { dateFormat.format(latest) });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { dateFormat.format(invalidEarly) });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        paramMap.put(key, new String[] { dateFormat.format(invalidLate) });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        // Parse Error
        paramMap.put(key, new String[] { "Spam" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testDoubleRangeValidator() throws Exception {

        Double valid = new Double(0.0);
        Double min = new Double(-100.0);
        Double invalidMin = new Double(-100.1);
        Double max = new Double(100.0);
        Double invalidMax = new Double(100.1);

        String key = "test";
        Validator validator = new DoubleRangeValidator(propertyService, true,
                false, min, max);

        // Boundary Test
        paramMap.put(key, new String[] { valid.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null, errorMap));

        paramMap.put(key, new String[] { min.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null, errorMap));

        paramMap.put(key, new String[] { max.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null, errorMap));

        paramMap.put(key, new String[] { invalidMin.toString() });
        Assert.assertFalse(validator.validate(paramMap, key, null, errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        paramMap.put(key, new String[] { invalidMax.toString() });
        Assert.assertFalse(validator.validate(paramMap, key, null, errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testDoubleValidator() throws Exception {

        String key = "test";
        Validator validator = new DoubleRangeValidator(propertyService, true,
                false);

        paramMap.put(key, new String[] { "1.0" });
        Assert.assertTrue(validator.validate(paramMap, key, null, errorMap));

        // Parse Error
        paramMap.put(key, new String[] { "Spam" });
        Assert.assertFalse(validator.validate(paramMap, key, null, errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testHistoricalDateValidator() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        Calendar cal = new GregorianCalendar();

        cal.setTimeInMillis(System.currentTimeMillis());
        Date valid = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, -1);
        Date past = cal.getTime();

        cal.setTime(valid);
        cal.add(Calendar.DAY_OF_WEEK, 1);
        Date future = cal.getTime();

        String key = "test";
        Validator validator = new HistoricalDateValidator(propertyService,
                true, false);

        // Bounadary Test
        paramMap.put(key, new String[] { dateFormat.format(valid) });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { dateFormat.format(past) });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { dateFormat.format(future) });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        // Parse Error
        paramMap.put(key, new String[] { "Spam" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testIntRangeValidator() throws Exception {

        Integer valid = new Integer(0);
        Integer min = new Integer(-100);
        Integer invalidMin = new Integer(-101);
        Integer max = new Integer(100);
        Integer invalidMax = new Integer(101);

        String key = "test";
        Validator validator = new IntRangeValidator(propertyService, true,
                false, min, max);

        // Boundary Test
        paramMap.put(key, new String[] { valid.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { min.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { max.toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { invalidMin.toString() });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        paramMap.put(key, new String[] { invalidMax.toString() });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testIntValidator() throws Exception {

        String key = "test";
        Validator validator = new IntRangeValidator(propertyService, true,
                false);

        paramMap.put(key, new String[] { "1" });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        // Parse Error
        paramMap.put(key, new String[] { "Spam" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();
    }

    @Test
    public void testStringValidator() throws Exception {

        String key = "test";
        Validator reqValidator = new StringValidator(propertyService, true,
                false);

        // Must be required
        Assert.assertFalse(reqValidator.validate(paramMap, key, null,  errorMap));
        Assert.assertTrue(errorMap.containsKey(key));
        errorMap.clear();

        paramMap.put(key, new String[] { "Spam" });
        Assert.assertTrue(reqValidator.validate(paramMap, key, null,  errorMap));

        Validator blankValidator = new StringValidator(propertyService, false,
                true);

        // Is not required
        paramMap.clear();
        Assert.assertTrue(blankValidator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { "Spam" });
        Assert.assertTrue(blankValidator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { "" });
        Assert.assertTrue(blankValidator.validate(paramMap, key, null,  errorMap));
    }

    @Test
    public void testTaxonValidator() throws Exception {
        TaxonGroup taxonGroup = new TaxonGroup();
        taxonGroup.setName("Birds");
        taxonGroup = taxaDAO.save(taxonGroup);

        IndicatorSpecies species = new IndicatorSpecies();
        species.setCommonName("Indicator Species A");
        species.setScientificName("Indicator Species A");
        species.setTaxonGroup(taxonGroup);
        species = taxaDAO.save(species);
        
        IndicatorSpecies species2 = new IndicatorSpecies();
        species2.setCommonName("Indicator Species B");
        species2.setScientificName("Indicator Species B");
        species2.setTaxonGroup(taxonGroup);
        species2 = taxaDAO.save(species2);
        
        Survey survey = new Survey();
        survey.setName("survey");
        survey.setDescription("survey desc");
        Set<IndicatorSpecies> speciesSet = new HashSet<IndicatorSpecies>();
        speciesSet.add(species);
        survey.setSpecies(speciesSet);
        survey = surveyDAO.save(survey);
        
        String key = "test";
        Validator validator = new TaxonValidator(propertyService, true, false,
                taxaDAO, survey, taxaService);

        String valid = species.getScientificName();
        String allLower = valid.toLowerCase();
        String allUpper = valid.toUpperCase();
        String shortenedName = valid.substring(1, valid.length());

        paramMap.put(key, new String[] { valid });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));

        paramMap.put(key, new String[] { allLower });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));
        errorMap.clear();

        paramMap.put(key, new String[] { allUpper });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));
        errorMap.clear();

        // shortened names now fail.
        paramMap.put(key, new String[] { shortenedName });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        // id should override search string.
        paramMap.put(key, new String[] { "random text string", species.getId().toString() });
        Assert.assertTrue(validator.validate(paramMap, key, null,  errorMap));
        errorMap.clear();
        
        paramMap.put(key, new String[] { allUpper, Integer.toString(0) });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        errorMap.clear();
        
        // space padded string for id
        paramMap.put(key, new String[] { "random text string", "     " });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        // empty string for id
        paramMap.put(key, new String[] { "random text string", "" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        paramMap.put(key, new String[] { "random text string" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        paramMap.put(key, new String[] { "random text string" });
        Assert.assertFalse(validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        paramMap.put(key,  new String[] { species2.getScientificName() });
        Assert.assertFalse("expected failure", validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_INVALID_FOR_SURVEY_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
        
        paramMap.put(key,  new String[] { "blah blah blah", species2.getId().toString() });
        Assert.assertFalse("expected failure", validator.validate(paramMap, key, null,  errorMap));
        Assert.assertEquals("wrong message", propertyService.getMessage(TaxonValidator.TAXON_INVALID_FOR_SURVEY_MESSAGE_KEY), errorMap.get(key));
        errorMap.clear();
    }
    
    @Test
    public void testValidateDateRange() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        Calendar cal = new GregorianCalendar();

        Date valid = dateFormat.parse("14 Dec 2010");

        cal.setTime(valid);
        cal.add(Calendar.DAY_OF_WEEK, -7);
        Date earliest = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, -1);
        Date invalidEarly = cal.getTime();

        cal.setTime(valid);
        cal.add(Calendar.DAY_OF_WEEK, 7);
        Date latest = cal.getTime();

        cal.add(Calendar.DAY_OF_WEEK, 1);
        Date invalidLate = cal.getTime();

        String key = "date";
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);

        // Boundary Test
        paramMap.put(key, new String[] { dateFormat.format(valid) });
        paramMap.put("dateRange", new String[] { dateFormat.format(earliest) , dateFormat.format(latest) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(earliest) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(latest) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(invalidEarly) });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();

        paramMap.put(key, new String[] { dateFormat.format(invalidLate) });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();

        // Test with no lower bound
        paramMap.put(key, new String[] { dateFormat.format(valid) });
        paramMap.put("dateRange", new String[] { dateFormat.format(earliest) , null });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(earliest) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(latest) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));

        paramMap.put(key, new String[] { dateFormat.format(invalidEarly) });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();

        paramMap.put(key, new String[] { dateFormat.format(invalidLate) });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));
        
        // Parse Error
        paramMap.put(key, new String[] { "Spam" });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.DATE_WITHIN_RANGE, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
    }
    
    @Test
    public void testValidateRequiredTime() throws Exception {
        String key = "time";
        String value = "";
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);

        // Boundary Test
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REQUIRED_TIME, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();

        value = "12:00";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REQUIRED_TIME, key, null));

        value = "anyString";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REQUIRED_TIME, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
    }
    
    @Test
    public void testValidateTime() throws Exception {
        String key = "time";
        String value = "";
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);

        // Boundary Test
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.TIME, key, null));

        value = "12:00";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.TIME, key, null));
        
        value = "32:00";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.TIME, key, null));

        value = "anyString";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.TIME, key, null));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
    }
    
    @Test
    public void testValidateHtml() throws Exception {
        String key = "test";
        String value = "";
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);

        // Boundary Test
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.HTML, key, null));

        value = "<html><body></body></html>";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.HTML, key, null));

        value = "<br>";
        paramMap.put(key, new String[] { value });
        boolean validated = validator.validate(paramMap, ValidationType.HTML, key, null);
        Assert.assertTrue(validated);
        
        value = "</html";
        paramMap.put(key, new String[] { value });
        validated = validator.validate(paramMap, ValidationType.HTML, key, null);
        Assert.assertFalse(validated);
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
    }
    
    @Test
    public void testValidateRegex() {
        String key = "test";
        String value = "";
        String regex = "\\d+(\\.?\\d+)?"; // regex for numbers
        RecordFormValidator validator = new RecordFormValidator(propertyService, taxaDAO, null, taxaService);

        Attribute att = new Attribute();
        att.setName(key+"_attribute");
        List<AttributeOption> options = new ArrayList<AttributeOption>(1);
        AttributeOption opt = new AttributeOption();
        opt.setValue(regex);
        options.add(opt);
        att.setOptions(options);
        
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REQUIRED_REGEX, key, att));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
        
        value = ".45";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REGEX, key, att));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
        
        value = "0.45";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));

        value = "1,200";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REGEX, key, att));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
        
        value = "1";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));

        // test word characters
        regex = "[A-Z](\\w*\\s*)*\\."; // matches a sentence
        att.getOptions().remove(opt);
        opt.setValue(regex);
        att.getOptions().add(opt);
        att.setOptions(options);
        
        value = "I";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REGEX, key, att));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
        
        value = "I.";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));
        
        value = "I think this should be valid.";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));
        
        value = "this will not be valid.";
        paramMap.put(key, new String[] { value });
        Assert.assertFalse(validator.validate(paramMap, ValidationType.REGEX, key, att));
        Assert.assertTrue(validator.getErrorMap().containsKey(key));
        validator.getErrorMap().clear();
        
        value = "I can even use numb3r5 and _und3r5c0r35 and CAPITALS.";
        paramMap.put(key, new String[] { value });
        Assert.assertTrue(validator.validate(paramMap, ValidationType.REGEX, key, att));
    }
    
    @Test
    public void testCoordValidator() {
    	Survey surveyWgs = new Survey();
    	surveyWgs.setMap(new GeoMap());
    	surveyWgs.getMap().setCrs(BdrsCoordReferenceSystem.WGS84);
    	Survey surveyMga = new Survey();
    	surveyMga.setMap(new GeoMap());
    	surveyMga.getMap().setCrs(BdrsCoordReferenceSystem.MGA50);
    	
    	RecordFormValidator validatorWgs = new RecordFormValidator(propertyService, taxaDAO, surveyWgs, taxaService);
    	
    	assertValid(validatorWgs, ValidationType.COORD_Y, "90", true, null);
    	assertValid(validatorWgs, ValidationType.COORD_Y, "90.1", false, null);
    	assertValid(validatorWgs, ValidationType.COORD_Y, "-90", true, null);
    	assertValid(validatorWgs, ValidationType.COORD_Y, "-90.1", false, null);
    	assertValid(validatorWgs, ValidationType.COORD_X, "180", true, null);
    	assertValid(validatorWgs, ValidationType.COORD_X, "-180", true, null);
    	assertValid(validatorWgs, ValidationType.COORD_X, "180.1", false, null);
    	assertValid(validatorWgs, ValidationType.COORD_X, "-180.1", false, null);
    	assertValid(validatorWgs, ValidationType.COORD_X, "", true, null);
    	assertValid(validatorWgs, ValidationType.COORD_Y, "", true, null);
    	
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_Y, "90", true, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_Y, "90.1", false, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_Y, "-90", true, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_Y, "-90.1", false, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_X, "180", true, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_X, "-180", true, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_X, "180.1", false, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_X, "-180.1", false, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_X, "", false, null);
    	assertValid(validatorWgs, ValidationType.REQUIRED_COORD_Y, "", false, null);
    	
    	RecordFormValidator validatorMga = new RecordFormValidator(propertyService, taxaDAO, surveyMga, taxaService);
    	
    	assertValid(validatorMga, ValidationType.COORD_Y, "90", true, null);
    	assertValid(validatorMga, ValidationType.COORD_Y, "90.1", true, null);
    	assertValid(validatorMga, ValidationType.COORD_Y, "-90", true, null);
    	assertValid(validatorMga, ValidationType.COORD_Y, "-90.1", true, null);
    	assertValid(validatorMga, ValidationType.COORD_X, "180", true, null);
    	assertValid(validatorMga, ValidationType.COORD_X, "-180", true, null);
    	assertValid(validatorMga, ValidationType.COORD_X, "180.1", true, null);
    	assertValid(validatorMga, ValidationType.COORD_X, "-180.1", true, null);
    	assertValid(validatorMga, ValidationType.COORD_X, "", true, null);
    	assertValid(validatorMga, ValidationType.COORD_Y, "", true, null);
    	
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_Y, "90", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_Y, "90.1", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_Y, "-90", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_Y, "-90.1", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_X, "180", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_X, "-180", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_X, "180.1", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_X, "-180.1", true, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_X, "", false, null);
    	assertValid(validatorMga, ValidationType.REQUIRED_COORD_Y, "", false, null);
    }
    
    private void assertValid(RecordFormValidator rfv, ValidationType type, 
    		String value, boolean expectedValid, String expectedMsg) {
    	String key = "dummyKey";
    	rfv.getErrorMap().clear();
    	Map<String, String[]> pMap = new HashMap<String, String[]>(1);
    	pMap.put(key, new String[] { value });
    	boolean result = rfv.validate(pMap, type, key, null);
    	Assert.assertEquals("wrong valid result", expectedValid, result);
    	if (expectedMsg != null) {
    		Assert.assertEquals("wrong msg", expectedMsg, rfv.getErrorMap().get(key));
    	}
    }
}