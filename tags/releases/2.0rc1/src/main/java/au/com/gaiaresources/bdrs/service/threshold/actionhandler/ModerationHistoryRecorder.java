package au.com.gaiaresources.bdrs.service.threshold.actionhandler;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceUtil;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.threshold.Action;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.service.threshold.ActionHandler;
import au.com.gaiaresources.bdrs.util.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

/**
 * Records moderation specific values of a Record as a Comment on the Record.
 * The values that are saved are:
 * <ul>
 *     <li>The user making the change</li>
 *     <li>The time the change was made</li>
 *     <li>The value of the species property</li>
 *     <li>The values of any moderator scoped attributes.</li>
 * </ul>
 */
public class ModerationHistoryRecorder implements ActionHandler {

    private final Logger log = Logger.getLogger(getClass());

    /** used to retrieve the configuration of the Species Record Property */
    private MetadataDAO metadataDAO;
    /** used to determine whether to show the scientific or common name of the species */
    private PreferenceUtil preferenceUtil;

    /**
     * Creates a new ModerationHistoryRecorder.
     * @param metadataDAO used to retrieve the configuration of the Species Record Property.
     * @param preferenceDAO used to determine whether to show the scientific or common name of the species.
     */
    public ModerationHistoryRecorder(MetadataDAO metadataDAO, PreferenceDAO preferenceDAO) {
        this.metadataDAO = metadataDAO;
        this.preferenceUtil = new PreferenceUtil(preferenceDAO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAction(Session sesh, Threshold threshold, Object entity, Action action) {
        if(!(entity instanceof Record)) {
            log.error("Cannot record changes to an entity of type: "+entity.getClass().getCanonicalName());
            return;
        }
        
        Record record = (Record) entity;
        if (!record.getSurvey().getRecordCommentsEnabled()) {
            log.warn("Recording Record state changes as a Comment but commenting is not enabled.");
        }
        
        String commentText = createHistoryComment(sesh, record);
        if (StringUtils.notEmpty(commentText)) {
            record.addComment(commentText);
        }
    }

    /**
     * Creates a String that records the value of the species and any moderation scoped attributes of the Record.
     * @param session the Hibernate Session to use for database access.
     * @param record the Record to be commented on.
     * @return a String containing the comment text.
     */
    private String createHistoryComment(Session session, Record record) {
        
        StringBuilder comment = new StringBuilder();

        addSpeciesToComment(session, comment, record);

        for (AttributeValue attributeValue : record.getOrderedAttributes()) {
            Attribute attribute = attributeValue.getAttribute();
            if (AttributeScope.isModerationScope(attribute.getScope())) {
                String value = attributeValue.toString();
                if (StringUtils.notEmpty(value)) {
                    addCommentLine(comment, attribute.getDescription(), value);
                }
            }
        }
        
        return comment.toString();
    }

    /**
     * Adds the current species to the record comment.  The value of the taxon.showScientificName preference is
     * preserved.
     * @param session the Hibernate Session to use for database access.
     * @param comment the StringBuilder that is being used to create the Comment.
     * @param record the Record the comment is about.
     */
    private void addSpeciesToComment(Session session, StringBuilder comment, Record record) {

        IndicatorSpecies species = record.getSpecies();
        if (species != null) {
            RecordProperty speciesConfiguration = new RecordProperty(record.getSurvey(), RecordPropertyType.SPECIES, metadataDAO);
            if (!speciesConfiguration.isHidden()) {

                boolean showScientificName = preferenceUtil.getBooleanPreference(session, Preference.SHOW_SCIENTIFIC_NAME_KEY, Preference.DEFAULT_SHOW_SCIENTIFIC_NAME);
                String speciesName = showScientificName ? species.getScientificName() : species.getCommonName();
                String description = speciesConfiguration.getDescription();

                addCommentLine(comment, description, speciesName);
            }
        }
    }

    /**
     * Adds a single line to the comment of the form "description: value"
     * @param comment The line is appended to this StringBuilder.
     * @param description the description of the field to add.
     * @param value the value of the field to add.
     */
    private void addCommentLine(StringBuilder comment, String description, String value) {
        comment.append(description).append(": ").append(value).append("\n");
    }
}
