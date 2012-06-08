package au.com.gaiaresources.bdrs.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.threshold.Action;
import au.com.gaiaresources.bdrs.model.threshold.ActionEvent;
import au.com.gaiaresources.bdrs.model.threshold.ActionType;
import au.com.gaiaresources.bdrs.model.threshold.Condition;
import au.com.gaiaresources.bdrs.model.threshold.Operator;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;

/**
 * The ModerationUtil creates the default {@link Threshold} for moderation.
 * @author stephanie
 */
public class ModerationUtil {
    public static final String MODERATION_THRESHOLD_NAME = "Moderation Threshold";
    private static final String MODERATION_THRESHOLD_DESCRIPTION = 
        "This threshold will send an email to the moderators when a record is created/updated, " +
        "and an email to the record owner when their record is updated by a moderator.  It will " +
        "also hold a record on creation if it has moderation attributes.";
    
    private static final String SCOPE_PROPERTY_PATH = "survey.attributes.scope";
    
    private ThresholdDAO thresholdDAO;
    
    public ModerationUtil(ThresholdDAO thresholdDAO) {
        this.thresholdDAO = thresholdDAO;
    }
    
    public Threshold createModerationThreshold(Session sesh, Portal portal) {
        Threshold threshold = new Threshold();
        String className = Record.class.getCanonicalName();
        threshold.setClassName(className);
        threshold.setEnabled(true);
        threshold.setName(MODERATION_THRESHOLD_NAME);
        threshold.setDescription(MODERATION_THRESHOLD_DESCRIPTION);
        threshold.setPortal(portal);
        
        List<Condition> conditionList = new ArrayList<Condition>();
        // set the conditions
        Condition condition = new Condition();
        condition.setClassName(className);
        condition.setPropertyPath(SCOPE_PROPERTY_PATH);
        condition.setKeyOperator(null);
        condition.setValueOperator(Operator.CONTAINS);
        condition.setKey(null);
        condition.setValue(new String[]{
                AttributeScope.RECORD_MODERATION.toString(),
                AttributeScope.SURVEY_MODERATION.toString()
        });
        condition.setPortal(portal);
        
        condition = thresholdDAO.save(sesh, condition);
        conditionList.add(condition);
        threshold.setConditions(conditionList);
        
        // set the actions
        List<Action> actionList = new ArrayList<Action>();
        // hold action
        actionList.add(createAction(sesh, portal, ActionType.HOLD_RECORD, ActionEvent.CREATE));
        
        // email action
        actionList.add(createAction(sesh, portal, ActionType.MODERATION_EMAIL_NOTIFICATION, ActionEvent.CREATE_AND_UPDATE));
        
        // moderation history
        actionList.add(createAction(sesh, portal, ActionType.MODERATION_HISTORY, ActionEvent.CREATE_AND_UPDATE));
        
        threshold.setActions(actionList);

        return thresholdDAO.save(sesh, threshold);
    }

    /**
     * Helper method for creating Actions.
     * @param sesh the Session to use when saving the action.
     * @param portal the Portal the Action belongs to.
     * @param actionType the type of action to create.
     * @param actionEvent the event that should trigger the Action.
     * @return the new Action
     */
    private Action createAction(Session sesh, Portal portal, ActionType actionType, ActionEvent actionEvent) {
        Action action = new Action();
        action.setActionType(actionType);
        action.setValue("");
        action.setActionEvent(actionEvent);
        action.setPortal(portal);
        action = thresholdDAO.save(sesh, action);
        return action;
    }
}
