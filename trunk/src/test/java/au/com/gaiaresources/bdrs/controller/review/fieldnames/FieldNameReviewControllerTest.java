package au.com.gaiaresources.bdrs.controller.review.fieldnames;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueUtil;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

public class FieldNameReviewControllerTest extends AbstractGridControllerTest {

    @Autowired
    private TaxaService taxaService;
    @Autowired
    private AttributeValueDAO avDAO;

    private Record recA;
    private Record recB;
    private Record recC;
    private Record recD;

    private IndicatorSpecies fieldNameSpecies;
    private Attribute fieldNameAttribute;

    private static final String BLACK = "black tree";
    private static final String CARROT = "carrot tree";
    private static final String DOG = "dog ttree";
    private static final String ELEPHANT = "elephant tree";

    private static final String[] EXPECTED_VALUES = new String[] { BLACK,
            CARROT, DOG, ELEPHANT };

    @Before
    public void setup() {
        fieldNameSpecies = taxaService.getFieldSpecies();
        fieldNameAttribute = taxaService.getFieldNameAttribute();

        recA = getBaseRecord();
        addFieldName(recA, ELEPHANT);
        recA = recDAO.save(recA);

        recB = getBaseRecord();
        addFieldName(recB, CARROT);
        recB = recDAO.save(recB);

        recC = getBaseRecord();
        addFieldName(recC, DOG);
        recC = recDAO.save(recC);

        recD = getBaseRecord();
        addFieldName(recD, BLACK);
        recD = recDAO.save(recD);
    }
    
    // must be logged in to access !
    @Test(expected=AccessDeniedException.class)
    public void testUnauthorized() throws Exception {
        request.setMethod(RequestMethod.GET.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey1.getId().toString());
        
        handle(request ,response);
    }
    
    /**
     * Test GET request with no survey ID parameter.
     * @throws Exception
     */
    @Test
    public void testGetNoSurvey() throws Exception {
        login(currentUser.getName(), "password", new String[] { Role.USER });

        request.setMethod(RequestMethod.GET.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);

        ModelAndView mv = handle(request, response);

        Assert.assertEquals("wrong view", FieldNameReviewController.FIELD_NAMES_REVIEW_VIEW, mv.getViewName());

        List<Record> records = (List<Record>) mv.getModel().get(FieldNameReviewController.MV_RECORDS);
        Assert.assertEquals("wrong size", 0, records.size());

        Attribute modelFieldNameAttribute = (Attribute)mv.getModel().get(FieldNameReviewController.MV_FIELD_NAME_ATTR);
        Assert.assertEquals("wrong attribute", fieldNameAttribute, modelFieldNameAttribute);
    }

    /**
     * Test GET request WITh survey ID parameter
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {
        login(currentUser.getName(), "password", new String[] { Role.USER });

        request.setMethod(RequestMethod.GET.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey1.getId().toString());

        ModelAndView mv = handle(request, response);

        Assert.assertEquals("wrong view", FieldNameReviewController.FIELD_NAMES_REVIEW_VIEW, mv.getViewName());

        List<Record> records = (List<Record>) mv.getModel().get(FieldNameReviewController.MV_RECORDS);
        Assert.assertEquals("wrong size", EXPECTED_VALUES.length, records.size());

        Assert.assertTrue("must contain record", records.contains(recA));
        Assert.assertTrue("must contain record", records.contains(recB));
        Assert.assertTrue("must contain record", records.contains(recC));
        Assert.assertTrue("must contain record", records.contains(recD));
        
        Attribute modelFieldNameAttribute = (Attribute)mv.getModel().get(FieldNameReviewController.MV_FIELD_NAME_ATTR);
        Assert.assertEquals("wrong attribute", fieldNameAttribute, modelFieldNameAttribute);

        // make sure records are ordered by alphabetic field name
        for (int i = 0; i < EXPECTED_VALUES.length; ++i) {
            Assert.assertEquals("wrong field name for index "
                    + Integer.toString(i), 
                    EXPECTED_VALUES[i], 
                    AttributeValueUtil.getAttributeValue(fieldNameAttribute, records.get(i)).getStringValue());
        }
    }
    
    private static final String REC_ID_TEMPLATE = FieldNameReviewController.PARAM_RECORD_ID_TEMPLATE;
    private static final String SPECIES_ID_TEMPLATE = FieldNameReviewController.PARAM_SPECIES_ID_TEMPLATE;
    private static final String SCI_NAME_TEMPLATE = FieldNameReviewController.PARAM_SCI_NAME_TEMPLATE;
    
    /**
     * Test scientific names can be saved. Not all rows are filled in.
     * @throws Exception
     */
    @Test
    public void testSave() throws Exception {
        login(currentUser.getName(), "password", new String[] { Role.USER });

        request.setMethod(RequestMethod.POST.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey1.getId().toString());
        
        int rowIdx = 0;
 
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recA.getId().toString());
        request.setParameter(String.format(SPECIES_ID_TEMPLATE, rowIdx), hoopSnake.getId().toString());
        request.setParameter(String.format(SCI_NAME_TEMPLATE, rowIdx), "ignored sci name");
        
        ++rowIdx;
        
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recB.getId().toString());
        request.setParameter(String.format(SCI_NAME_TEMPLATE, rowIdx), hoopSnake.getScientificName());
        
        // only fill out 2 records.
        
        ModelAndView mv = handle(request, response);
        
        String[] expectedFieldNames = new String[] { BLACK, DOG };
        
        assertRedirect(mv, FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        
        List<Record> records = recDAO.getFieldNameRecords(null, currentUser.getId(), survey1.getId(), taxaService);
        Assert.assertEquals("wrong size", expectedFieldNames.length, records.size());

        Assert.assertTrue("must contain record", records.contains(recC));
        Assert.assertTrue("must contain record", records.contains(recD));
        
        // make sure records are ordered by alphabetic field name
        for (int i = 0; i < expectedFieldNames.length; ++i) {
            Assert.assertEquals("wrong field name for index "
                    + Integer.toString(i), 
                    expectedFieldNames[i], 
                    AttributeValueUtil.getAttributeValue(fieldNameAttribute, records.get(i)).getStringValue());
        }
        
        getSession().refresh(recA);
        getSession().refresh(recB);
        
        Assert.assertEquals("wrong species", recA.getSpecies(), hoopSnake);
        Assert.assertEquals("wrong species", recB.getSpecies(), hoopSnake);
        
        Assert.assertNotNull("field name attr value should exist", AttributeValueUtil.getAttributeValue(fieldNameAttribute, recA));
        Assert.assertNotNull("field name attr value should exist", AttributeValueUtil.getAttributeValue(fieldNameAttribute, recB));
    }
    
    /**
     * Test saving an invalid species name.
     * @throws Exception
     */
    @Test
    public void testSaveBadSpeciesName() throws Exception {
        login(currentUser.getName(), "password", new String[] { Role.USER });

        request.setMethod(RequestMethod.POST.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey1.getId().toString());
        
        int rowIdx = 0;
 
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recA.getId().toString());
        String badSciName = "sci name with no match";
        request.setParameter(String.format(SCI_NAME_TEMPLATE, rowIdx), badSciName);
        
        ModelAndView mv = handle(request, response);
        
        this.assertMessageCodeAndArgs(FieldNameReviewController.MSG_KEY_ZERO_SPECIES_FOR_NAME, new Object[] { badSciName });
        
        String[] expectedFieldNames = EXPECTED_VALUES;
        
        assertRedirect(mv, FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);

        //List<Record> records = (List<Record>) mv.getModel().get(FieldNameReviewController.MV_RECORDS);
        List<Record> records = recDAO.getFieldNameRecords(null, currentUser.getId(), survey1.getId(), taxaService);
        Assert.assertEquals("wrong size", expectedFieldNames.length, records.size());

        Assert.assertTrue("must contain record", records.contains(recA));
        Assert.assertTrue("must contain record", records.contains(recB));
        Assert.assertTrue("must contain record", records.contains(recC));
        Assert.assertTrue("must contain record", records.contains(recD));
        
        // make sure records are ordered by alphabetic field name
        for (int i = 0; i < expectedFieldNames.length; ++i) {
            Assert.assertEquals("wrong field name for index "
                    + Integer.toString(i), 
                    expectedFieldNames[i], 
                    AttributeValueUtil.getAttributeValue(fieldNameAttribute, records.get(i)).getStringValue());
        }
        
        getSession().refresh(recA);
        
        Assert.assertEquals("wrong species", recA.getSpecies(), fieldNameSpecies);        
        Assert.assertNotNull("field name attr value should still exist", AttributeValueUtil.getAttributeValue(fieldNameAttribute, recA));
    }
    
    /**
     * Test saving form with no sci names entered.
     * @throws Exception
     */
    @Test
    public void testSaveBlankForm() throws Exception {
        login(currentUser.getName(), "password", new String[] { Role.USER });

        request.setMethod(RequestMethod.POST.toString());
        request.setRequestURI(FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey1.getId().toString());
        
        int rowIdx = 0;
 
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recA.getId().toString());
        
        ++rowIdx;
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recB.getId().toString());
        
        ++rowIdx;
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recC.getId().toString());
        
        ++rowIdx;
        request.addParameter(FieldNameReviewController.PARAM_ROW_IDX, Integer.toString(rowIdx));
        request.setParameter(String.format(REC_ID_TEMPLATE, rowIdx), recD.getId().toString());
        
        ModelAndView mv = handle(request, response);
        
        assertMessageCode(FieldNameReviewController.MSG_KEY_NOTHING_TO_SAVE);
        
        assertRedirect(mv, FieldNameReviewController.GET_RECORD_FIELD_NAMES_URL);
    }

    private Record getBaseRecord() {
        Record r = new Record();
        r.setUser(currentUser);
        r.setSurvey(survey1);
        r.setSpecies(fieldNameSpecies);
        return r;
    }

    private void addFieldName(Record r, String fieldName) {
        Set<AttributeValue> avSet = new HashSet<AttributeValue>();
        AttributeValue av = new AttributeValue();
        av.setAttribute(fieldNameAttribute);
        av.setStringValue(fieldName);
        av = avDAO.save(av);
        avSet.add(av);
        r.setAttributes(avSet);
    }
}
