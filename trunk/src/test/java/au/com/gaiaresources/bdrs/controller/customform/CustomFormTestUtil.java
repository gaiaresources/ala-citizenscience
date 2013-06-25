package au.com.gaiaresources.bdrs.controller.customform;

import au.com.gaiaresources.bdrs.controller.record.SingleSiteController;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.form.CustomForm;
import au.com.gaiaresources.bdrs.model.form.CustomFormDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.*;
import au.com.gaiaresources.bdrs.python.PythonTestUtil;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Performs test utility functions that assist custom form testing.
 */
public class CustomFormTestUtil {
    private static final int DEFAULT_INT_WITH_RANGE_VALUE = 5;
    private static Logger log = Logger.getLogger(CustomFormTestUtil.class);

    /**
     * A list of all attribute types supported by the default custom form framework.
     */
    public static final List<AttributeType> SUPPORTED_ATTRIBUTE_TYPES;

    static {
        List<AttributeType> supported = new ArrayList<AttributeType>();
        supported.add(AttributeType.INTEGER);
        supported.add(AttributeType.INTEGER_WITH_RANGE);
        supported.add(AttributeType.DECIMAL);
        supported.add(AttributeType.DATE);
        //supported.add(AttributeType.REGEX);
        //supported.add(AttributeType.STRING_AUTOCOMPLETE);
        supported.add(AttributeType.STRING);
        supported.add(AttributeType.TEXT);
        //supported.add(AttributeType.BARCODE);
        supported.add(AttributeType.TIME);
        //supported.add(AttributeType.HTML);
        //supported.add(AttributeType.HTML_NO_VALIDATION);
        //supported.add(AttributeType.HTML_COMMENT);
        //supported.add(AttributeType.HTML_HORIZONTAL_RULE);
        supported.add(AttributeType.STRING_WITH_VALID_VALUES);
        supported.add(AttributeType.MULTI_CHECKBOX);
        supported.add(AttributeType.MULTI_SELECT);
        supported.add(AttributeType.SINGLE_CHECKBOX);
        //supported.add(AttributeType.FILE);
        //supported.add(AttributeType.IMAGE);

        Collections.sort(supported);
        SUPPORTED_ATTRIBUTE_TYPES = Collections.unmodifiableList(supported);
    }

    private CustomFormTestUtil() {
        // Do nothing. Cannot instantiate a utility class.
    }

    /**
     * @see PythonTestUtil#getRenderURL(String, String, au.com.gaiaresources.bdrs.db.impl.PersistentImpl)
     */
    public static String getRenderURL(CustomForm form) {
        return PythonTestUtil.getRenderURL(CustomFormController.FORM_RENDER_URL, "{formId}", form);
    }

    /**
     * Returns the first custom form with the specified name.
     *
     * @param customFormDAO performs database retrieval of custom forms.
     * @param formName      the name of the form to be returned.
     * @return the first custom form with the specified name.
     */
    public static CustomForm getCustomFormByName(CustomFormDAO customFormDAO, String formName) {
        for (CustomForm form : customFormDAO.getCustomForms()) {
            if (form.getName().equals(formName)) {
                return form;
            }
        }
        return null;
    }

    /**
     * @see PythonTestUtil#getTestFile(java.io.File, String, String)
     */
    public static MockMultipartFile getTestForm(File dir, String formName) throws URISyntaxException, IOException {
        return PythonTestUtil.getTestFile(dir, formName, CustomFormController.POST_KEY_ADD_FORM_FILE);
    }

    /**
     * @see PythonTestUtil#getConfigFile(java.io.File, String)
     */
    public static JSONObject getConfigFile(File reportDir) throws IOException, URISyntaxException {
        return PythonTestUtil.getConfigFile(reportDir, CustomFormController.FORM_CONFIG_FILENAME);
    }

    /**
     * Populates the POST dictionary of the provided request with the expected form key,value pairs.
     *
     * @param request the request to be populated.
     * @param survey  the survey containing attributes to be populated.
     * @param taxon   the taxon to be used for the created/edited record.
     * @throws ParseException thrown if there was an error parsing date and times.
     */
    public static void populateCustomFormPOSTParameters(MockHttpServletRequest request, Survey survey, IndicatorSpecies taxon) throws ParseException {
        populateCustomFormPOSTParameters(request, survey, null, taxon, DEFAULT_INT_WITH_RANGE_VALUE);
    }

    /**
     * Populates the POST dictionary of the provided request with the expected form key,value pairs.
     *
     * @param request the request to be populated.
     * @param survey  the survey containing attributes to be populated.
     * @param taxon   the taxon to be used for the created/edited record.
     * @throws ParseException thrown if there was an error parsing date and times.
     */
    public static void populateCustomFormPOSTParameters(MockHttpServletRequest request, Survey survey, Record rec, IndicatorSpecies taxon) throws ParseException {
        populateCustomFormPOSTParameters(request, survey, rec, taxon, DEFAULT_INT_WITH_RANGE_VALUE);
    }

    /**
     * Populates the POST dictionary of the provided request with the expected form key,value pairs.
     *
     * @param request           the request to be populated.
     * @param survey            the survey containing attributes to be populated.
     * @param record            the original record (if the id needs to be included for an editing workflow)
     * @param taxon             the taxon to be used for the created/edited record.
     * @param intWithRangeValue a valid integer for an int with range attribute.
     * @throws ParseException thrown if there was an error parsing date and times.
     */
    public static void populateCustomFormPOSTParameters(MockHttpServletRequest request, Survey survey, Record record, IndicatorSpecies taxon, int intWithRangeValue) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setLenient(false);

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date sightingDate = cal.getTime();

        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        params.put("latitude", "-36.879620605027");
        params.put("longitude", "126.650390625");
        params.put("date", dateFormat.format(sightingDate));
        params.put("time", timeFormat.format(sightingDate));
        params.put("time_hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        params.put("time_minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        params.put("notes", "This is a test record");

        Map<Attribute, Object> surveyScopeAttributeValueMapping = new HashMap<Attribute, Object>();
        Map<IndicatorSpecies, Map<Attribute, Object>> recordScopeAttributeValueMapping = new HashMap<IndicatorSpecies, Map<Attribute, Object>>(
                2);
        Map<Attribute, Object> attributeValueMapping;

        String[] existingRowPrefixes = request.getParameterValues(SingleSiteController.PARAM_ROW_PREFIX);
        int sightingIndex = existingRowPrefixes == null ? 0 : existingRowPrefixes.length;
        String surveyPrefix = "";

        // Infrastructure
        String recordPrefix = String.format("%d_", sightingIndex);
        request.addParameter(SingleSiteController.PARAM_ROW_PREFIX, recordPrefix);

        if (record != null && record.getId() != null) {
            params.put(String.format("%d_recordId", sightingIndex), record.getId().toString());
        }

        // Taxon and Number
        if (taxon != null) {
            params.put(String.format("%d_survey_species_search", sightingIndex), taxon.getScientificName());
            params.put(String.format("%d_species", sightingIndex), taxon.getId().toString());
            params.put(String.format("%d_number", sightingIndex), Integer.valueOf(sightingIndex + 21).toString());
        }

        String prefix;
        String key;
        String value; // The value in the post dict
        attributeValueMapping = new HashMap<Attribute, Object>();
        Map<Attribute, Object> valueMap;
        recordScopeAttributeValueMapping.put(taxon, attributeValueMapping);
        for (Attribute attr : survey.getAttributes()) {
            if (!AttributeScope.LOCATION.equals(attr.getScope())) {
                if (AttributeScope.isRecordScope(attr.getScope())) {
                    prefix = recordPrefix;
                    valueMap = attributeValueMapping;
                } else {
                    prefix = surveyPrefix;
                    valueMap = surveyScopeAttributeValueMapping;
                }

                key = WebFormAttributeParser.getParamKey(prefix,  attr);
                value = "";

                switch (attr.getType()) {
                    case INTEGER:
                        Integer val = Integer.valueOf(sightingIndex + 30);
                        value = val.toString();
                        valueMap.put(attr, val);
                        break;
                    case INTEGER_WITH_RANGE:
                        value = String.valueOf(intWithRangeValue);
                        valueMap.put(attr, Integer.valueOf(value));
                        break;
                    case DECIMAL:
                        value = String.format("50.%d", sightingIndex);
                        valueMap.put(attr, Double.parseDouble(value));
                        break;
                    case DATE:
                        Date date = new Date(System.currentTimeMillis());
                        value = dateFormat.format(date);
                        // Reparsing the date strips out the hours, minutes and seconds
                        valueMap.put(attr, dateFormat.parse(value));
                        break;
                    case TIME:
                        value = timeFormat.format(new Date());
                        valueMap.put(attr, value);
                        break;
                    case REGEX:
                    case STRING_AUTOCOMPLETE:
                    case STRING:
                    case BARCODE:
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                        value = String.format("String %d", sightingIndex);
                        valueMap.put(attr, value);
                        break;
                    case TEXT:
                        value = String.format("Text %d", sightingIndex);
                        valueMap.put(attr, value);
                        break;
                    case STRING_WITH_VALID_VALUES:
                        value = attr.getOptions().get(sightingIndex).getValue();
                        valueMap.put(attr, value);
                        break;
                    case MULTI_CHECKBOX:
                    case MULTI_SELECT:
                        List<AttributeOption> opts = attr.getOptions();
                        request.addParameter(key, opts.get(0).getValue());
                        request.addParameter(key, opts.get(1).getValue());
                        value = null;
                        break;
                    case SINGLE_CHECKBOX:
                        value = String.valueOf(true);
                        valueMap.put(attr, value);
                        break;
                    case AUDIO:
                    case VIDEO:
                    case FILE:
                        String file_filename = String.format("attribute_%d", attr.getId());
                        MockMultipartFile mockFileFile = new MockMultipartFile(key,
                                file_filename, "audio/mpeg",
                                file_filename.getBytes());
                        ((MockMultipartHttpServletRequest) request).addFile(mockFileFile);
                        valueMap.put(attr, mockFileFile);
                        value = file_filename;
                        break;
                    case IMAGE:
                        String image_filename = String.format("attribute_%d", attr.getId());
                        MockMultipartFile mockImageFile = new MockMultipartFile(
                                key, image_filename, "image/png",
                                image_filename.getBytes());
                        ((MockMultipartHttpServletRequest) request).addFile(mockImageFile);
                        valueMap.put(attr, mockImageFile);
                        value = image_filename;
                        break;
                    case SPECIES:
                        if (taxon != null) {
                                value = taxon.getScientificName();
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
                if (value != null) {
                    params.put(key, value);
                }
            }
        }
        sightingIndex += 1;

        request.setParameters(params);
    }

    /**
     * Validates that the record provided matches the submitted POST dictionary.
     *
     * @param request       the original request containing the POST dictionary of expected values.
     * @param survey        the survey that contains the attributes that need to be created.
     * @param taxon         the taxon that the record captures.
     * @param record        the record that was created/edited.
     * @param sightingIndex the index of the record in the POST dictionary.
     */
    public static void validateRecord(MockHttpServletRequest request, Survey survey, IndicatorSpecies taxon, Record record, int sightingIndex) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setLenient(false);

        Assert.assertEquals(survey.getId(), record.getSurvey().getId());
        // Coordinates are truncates to 6 decimal points
        Assert.assertEquals(new Double(request.getParameter("latitude")).doubleValue(),
                record.getPoint().getY(), Math.pow(10, -6));
        Assert.assertEquals(new Double(request.getParameter("longitude")).doubleValue(),
                record.getPoint().getX(), Math.pow(10, -6));
        Assert.assertEquals(request.getParameter("date"), dateFormat.format(record.getWhen()));
        Assert.assertEquals(request.getParameter("time"), timeFormat.format(record.getTimeAsDate()));
        Assert.assertEquals(request.getParameter("notes"), record.getNotes());

        Assert.assertEquals(taxon, record.getSpecies());
        Assert.assertEquals(sightingIndex + 21, record.getNumber().intValue());

        String prefix;
        String surveyPrefix = "";
        String recordPrefix = String.format("%d_", sightingIndex);

        for (TypedAttributeValue recAttr : record.getAttributes()) {
            if (AttributeScope.SURVEY.equals(recAttr.getAttribute().getScope()) ||
                    AttributeScope.SURVEY_MODERATION.equals(recAttr.getAttribute().getScope())) {
                prefix = surveyPrefix;
            } else {
                prefix = recordPrefix;
            }
            String key = WebFormAttributeParser.getParamKey(prefix,  recAttr.getAttribute());

            AttributeType type = recAttr.getAttribute().getType();
            if (SUPPORTED_ATTRIBUTE_TYPES.contains(type)) {
                switch (recAttr.getAttribute().getType()) {
                    case INTEGER:
                    case INTEGER_WITH_RANGE:
                    case DECIMAL:
                    case DATE:
                    case REGEX:
                    case STRING_AUTOCOMPLETE:
                    case STRING:
                    case TEXT:
                    case BARCODE:
                    case TIME:
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                    case STRING_WITH_VALID_VALUES:
                    case MULTI_CHECKBOX:
                    case MULTI_SELECT:
                    case SINGLE_CHECKBOX:
                        String expected = request.getParameter(key);
                        Assert.assertEquals(expected, recAttr.getStringValue());
                        break;
                    case FILE:
                    case AUDIO:
                    case VIDEO:
                    case IMAGE:
                        Assert.assertTrue("File and Image validation not implemented", false);
                        break;
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        // census method types should add a record to the attribute value
                        break;
                    default:
                        Assert.assertTrue("Unknown Attribute Type: "
                                + recAttr.getAttribute().getType().toString(), false);
                        break;
                }
            } else {
                log.warn("Ignoring unsupported custom form attribute type: " + type);
            }
        }
    }
}
