package au.com.gaiaresources.bdrs.controller.service.threshold.actionhandler;

import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.record.Comment;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.service.threshold.actionhandler.ModerationHistoryRecorder;
import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ModerationHistoryRecorder class.
 */
@RunWith(JMock.class)
public class ModerationHistoryRecorderTest {

    private Mockery context = new JUnit4Mockery();

    private MetadataDAO metadataDAO = context.mock(MetadataDAO.class);
    private PreferenceDAO preferenceDAO = context.mock(PreferenceDAO.class);

    private ModerationHistoryRecorder moderationHistoryRecorder;

    @Before
    public void setUp() {
        moderationHistoryRecorder = new ModerationHistoryRecorder(metadataDAO, preferenceDAO);

        final Preference preference = new Preference();
        preference.setValue("True");
        context.checking(new Expectations(){
            {allowing(preferenceDAO).getPreferenceByKey(null, Preference.SHOW_SCIENTIFIC_NAME_KEY);
                will(returnValue(preference)); }
        });
    }

    /**
     * Tests that a record comment is generated as expected.
     */
    @Test
    public void testAddRecordComment() {

        Record record = createRecord();
        addAttribute(record, "Attribute1", "Value1", AttributeScope.SURVEY_MODERATION);
        addAttribute(record, "Attribute2", "Value2", AttributeScope.RECORD_MODERATION);
        addAttribute(record, "Attribute3", "Value3", AttributeScope.RECORD);
        moderationHistoryRecorder.executeAction(null, null, record, null);

        Comment comment = record.getComments().get(0);
        String expectedComment =
                "Species: Frogus Greenus\n" +
                "Attribute1: Value1\n" +
                "Attribute2: Value2\n";
        Assert.assertEquals(expectedComment, comment.getCommentText());
    }

    /**
     * Tests that a record comment is generated as expected when there are no moderator scoped attributes.
     */
    @Test
    public void testAddRecordCommentNoModeratorAttributes() {

        Record record = createRecord();
        addAttribute(record, "Attribute1", "Value1", AttributeScope.RECORD);
        addAttribute(record, "Attribute2", "Value2", AttributeScope.RECORD);
        addAttribute(record, "Attribute3", "Value3", AttributeScope.RECORD);


        moderationHistoryRecorder.executeAction(null, null, record, null);

        Comment comment = record.getComments().get(0);
        String expectedComment =
                "Species: Frogus Greenus\n";

        Assert.assertEquals(expectedComment, comment.getCommentText());
    }

    /**
     * Tests the comment handles the case that the species is marked as Hidden on the survey.
     */
    @Test
    public void testAddRecordCommentForSurveyWithSpeciesHidden() {
        // Hide the species field on the survey.
        Record record = createRecord();
        Metadata hidden = record.getSurvey().getMetadataByKey("RECORD.Species.HIDDEN");
        hidden.setValue(Boolean.toString(true));
        
        addAttribute(record, "Attribute1", "Value1", AttributeScope.RECORD_MODERATION);
        moderationHistoryRecorder.executeAction(null, null, record, null);

        Comment comment = record.getComments().get(0);
        String expectedComment =
                "Attribute1: Value1\n";

        Assert.assertEquals(expectedComment, comment.getCommentText());
    }

    /**
     * Tests that no comment is created if there is nothing to log.
     */
    @Test
    public void testAddRecordCommentForSurveyWithSpeciesHiddenAndNoModeratorAttributes() {
        // Hide the species field on the survey.
        Record record = createRecord();
        Metadata hidden = record.getSurvey().getMetadataByKey("RECORD.Species.HIDDEN");
        hidden.setValue(Boolean.toString(true));

        addAttribute(record, "Attribute1", "Value1", AttributeScope.RECORD);
        moderationHistoryRecorder.executeAction(null, null, record, null);

        Assert.assertEquals(0, record.getComments().size());
    }

    /**
     * Creates a new Record with the minimum configuration required to run the tests.
     * @return the new Record.
     */
    private Record createRecord() {
        Record record = new Record();

        IndicatorSpecies frog = new IndicatorSpecies();
        frog.setCommonName("Green Frog");
        frog.setScientificName("Frogus Greenus");

        Survey survey = createSurvey();

        record.setSpecies(frog);
        record.setSurvey(survey);
        
        return record;
    }
    
    private void addAttribute(Record record, String attributeDescription, String attributeValue, AttributeScope scope) {
        Attribute attribute = new Attribute();
        attribute.setDescription(attributeDescription);
        attribute.setScope(scope);
        attribute.setTypeCode(AttributeType.TEXT.getCode());
        attribute.setWeight(record.getAttributes().size());

        AttributeValue value = new AttributeValue();
        value.setStringValue(attributeValue);
        value.setAttribute(attribute);

        
        record.getAttributes().add(value);
    }

    /**
     * Creates a new Survey with the minimum configuration required to run the tests.
     * @return a new Survey.
     */
    private Survey createSurvey() {
        Survey survey = new Survey();
        Metadata commentsAllowed = new Metadata();
        commentsAllowed.setKey(Metadata.COMMENTS_ENABLED_FOR_SURVEY);
        commentsAllowed.setValue("True");
        survey.getMetadata().add(commentsAllowed);

        Metadata metadata = new Metadata();
        metadata.setValue("Species");
        metadata.setKey("RECORD.Species.DESCRIPTION");
        survey.getMetadata().add(metadata);
        Metadata hideSpeciesMetadata = new Metadata();
        hideSpeciesMetadata.setKey("RECORD.Species.HIDDEN");
        hideSpeciesMetadata.setValue(Boolean.toString(false));
        survey.getMetadata().add(hideSpeciesMetadata);
        return survey;
    }
}
