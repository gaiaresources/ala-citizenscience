package au.com.gaiaresources.bdrs.model.taxa;

import java.io.IOException;
import java.io.Writer;

import au.com.gaiaresources.bdrs.json.JSONEnum;
import au.com.gaiaresources.bdrs.json.JSONEnumUtil;
import au.com.gaiaresources.bdrs.model.record.Record;

/**
 * The <code>AttributeScope</code> defines the suggested extent where this
 * attribute is applicable. For example an attribute may have a scope of
 * {@link #SURVEY} indicating that the value for this attribute applies for all
 * records in a survey. Likewise a scope of {@link Record} indicates that
 * each record contains a different value for the attribute.
 */
public enum AttributeScope implements JSONEnum {
    SURVEY("Survey"),
    RECORD("Record"),
    LOCATION("Location"),
    SURVEY_MODERATION("Survey Moderation"),
    RECORD_MODERATION("Record Moderation");
    
    private String name;
    
    private AttributeScope(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    /**
     * Returns true if the scope is one of SURVEY_MODERATION or RECORD_MODERATION.
     * @param scope the scope to check
     * @return true if the scope is one of SURVEY_MODERATION or RECORD_MODERATION, false otherwise
     */
    public static boolean isModerationScope(AttributeScope scope) {
        return scope == SURVEY_MODERATION || scope == RECORD_MODERATION;
    }
    
    /**
     * Returns true if the scope is one of RECORD or RECORD_MODERATION.
     * @param scope the scope to check
     * @return true if the scope is one of RECORD or RECORD_MODERATION, false otherwise
     */
    public static boolean isRecordScope(AttributeScope scope) {
        return scope == RECORD || scope == RECORD_MODERATION;
    }
    
    /**
     * Returns true if the scope is one of SURVEY or SURVEY_MODERATION.
     * @param scope the scope to check
     * @return true if the scope is one of SURVEY or SURVEY_MODERATION, false otherwise
     */
    public static boolean isSurveyScope(AttributeScope scope) {
        return scope == SURVEY || scope == SURVEY_MODERATION;
    }
    
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONEnumUtil.writeJSONString(out, this);
    }

    @Override
    public String toJSONString() {
        return JSONEnumUtil.toJSONString(this);
    }
}
