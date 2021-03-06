package au.com.gaiaresources.bdrs.service.threshold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.service.threshold.actionhandler.ModerationHistoryRecorder;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.gaiaresources.bdrs.service.content.ContentService;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.service.threshold.actionhandler.EmailActionHandler;
import au.com.gaiaresources.bdrs.service.threshold.actionhandler.HoldRecordHandler;
import au.com.gaiaresources.bdrs.service.threshold.actionhandler.ModerationEmailActionHandler;
import au.com.gaiaresources.bdrs.service.threshold.operatorhandler.ContainsHandler;
import au.com.gaiaresources.bdrs.service.threshold.operatorhandler.EqualsHandler;
import au.com.gaiaresources.bdrs.service.threshold.operatorhandler.RecordAttributeHandler;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordProperty;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.email.EmailService;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.threshold.Action;
import au.com.gaiaresources.bdrs.model.threshold.ActionType;
import au.com.gaiaresources.bdrs.model.threshold.Condition;
import au.com.gaiaresources.bdrs.model.threshold.Operator;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;

@SuppressWarnings("serial")
@Service
/**
 * <p>Thresholding is the establishment of certain criteria known as 
 * {@link Condition}s that, when matched, result in one to many {@link Action}s
 * being applied.</p>
 * 
 * <p>An example of a threshold is a record that when created created
 * for a survey with the name "Wiggle", and the record contains an attribute
 * with the key of "behaviour" whose value is "nesting", then hold the record
 * and email someone.</p>
 * 
 * <p>The <code>ThresholdService</code> is the stateful repository where 
 * threshold enabled classes are registered with action handlers and operators 
 * are mapped to datatypes.</p>
 */
public class ThresholdService implements ConditionOperatorHandler {
    
    @Autowired
    private ThresholdDAO thresholdDAO;
    
    /**
     * The list of classes where theresholding may be applied.
     */
    public static final List<Class<?>> THRESHOLD_CLASSES;
    static {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Record.class);
        list.add(Survey.class);
        list.add(IndicatorSpecies.class);
        list.add(User.class);
        THRESHOLD_CLASSES = Collections.unmodifiableList(list);
    } 

    /**
     * A mapping of simple datatypes and the possible operations that may be
     * applied to perform a comparison. Simple datatypes are defined as those
     * that only involve a single value such as a string or a number.
     */
    public static final Map<Class<?>, Operator[]> SIMPLE_TYPE_TO_OPERATOR_MAP = Collections.unmodifiableMap(new HashMap<Class<?>, Operator[]>() {
        {
            put(String.class, new Operator[] { Operator.EQUALS, Operator.CONTAINS });
            put(Integer.class, new Operator[] { Operator.EQUALS });
            put(Long.class, new Operator[] { Operator.EQUALS });
            put(Date.class, new Operator[] { Operator.EQUALS });
            put(Boolean.class, new Operator[] { Operator.EQUALS });
            put(AttributeScope.class, new Operator[] { Operator.CONTAINS });
        }
    });

    /**
     * A mapping of complex datatypes and the possible operations that may be
     * applied to perform the comparison. Complex datatypes are defined as those
     * that involve more than one value such as a key/value pair and hence
     * require a more intricate comparison operation.
     */
    public static final Map<Class<?>, ComplexTypeOperator> COMPLEX_TYPE_TO_OPERATOR_MAP = Collections.unmodifiableMap(new HashMap<Class<?>, ComplexTypeOperator>() {
        {
            put(AttributeValue.class, new RecordAttributeOperator());
        }
    });

    /**
     * A mapping of threshold enabled classes to the possible actions that may
     * be taken if the conditions are met. All classes in the
     * {@link #THRESHOLD_CLASSES} must be specified as keys in this map.
     */
    public static final Map<Class<?>, ActionType[]> CLASS_TO_ACTION_MAP = Collections.unmodifiableMap(new HashMap<Class<?>, ActionType[]>() {
        {
            put(IndicatorSpecies.class, new ActionType[] { ActionType.EMAIL_NOTIFICATION });
            put(Record.class, new ActionType[] { ActionType.EMAIL_NOTIFICATION, 
                ActionType.MODERATION_EMAIL_NOTIFICATION, ActionType.HOLD_RECORD, ActionType.MODERATION_HISTORY });
            put(User.class, new ActionType[] { ActionType.EMAIL_NOTIFICATION });
            put(Survey.class, new ActionType[] { ActionType.EMAIL_NOTIFICATION });
        }
    });

    private Logger log = Logger.getLogger(getClass());

    /**
     * A map of complex type operators to their comparison handler (object that
     * performs the actual comparison).
     */
    private Map<ComplexTypeOperator, OperatorHandler> operatorHandlerMap;
    /**
     * A map of simple type operators to their comparison handler.
     */
    private Map<SimpleTypeOperator, SimpleOperatorHandler> simpleOperatorHandlerMap;

    /**
     * A map of all possible action types and their handler which performs the
     * action.
     */
    private Map<ActionType, ActionHandler> actionHandlerMap;

    /**
     * A set of all instances that are currently being processed by the
     * thresholding framework. By recording all instances being processed,
     * actions that cause further hibernate events do not cause further actions
     * thereby preventing cascading or repeating sequences of actions. All items
     * in the set take the form of
     * <code>{canonical class name}.{primary key}</code>
     */
    private Set<String> referenceCountSet = new HashSet<String>();

    @Autowired
    private EmailService emailService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ContentService contentService;
    
    @Autowired
    private RedirectionService redirService;
    
    @Autowired
    private UserDAO userDAO;

    /** The ModerationHistoryRecorder needs this to retrieve the description of the species property */
    @Autowired
    private MetadataDAO metadataDAO;

    /** The ModerationHistoryRecorder uses this to determine whether to record a species common or scientific name */
    @Autowired
    private PreferenceDAO preferenceDAO;
    
    @PostConstruct
    public void init() {
        populateOperatorHandlers();
        populateActionHandlers();
    }

    private void populateActionHandlers() {
        actionHandlerMap = new HashMap<ActionType, ActionHandler>(
                ActionType.values().length);
        actionHandlerMap.put(ActionType.EMAIL_NOTIFICATION, new EmailActionHandler(
                emailService, propertyService));
        actionHandlerMap.put(ActionType.MODERATION_EMAIL_NOTIFICATION, 
                             new ModerationEmailActionHandler(emailService, propertyService, 
                                                              contentService, redirService, userDAO));
        actionHandlerMap.put(ActionType.HOLD_RECORD, new HoldRecordHandler());
        actionHandlerMap.put(ActionType.MODERATION_HISTORY, new ModerationHistoryRecorder(metadataDAO, preferenceDAO));
    }

    private void populateOperatorHandlers() {
        operatorHandlerMap = new HashMap<ComplexTypeOperator, OperatorHandler>(
                COMPLEX_TYPE_TO_OPERATOR_MAP.size());
        operatorHandlerMap.put(COMPLEX_TYPE_TO_OPERATOR_MAP.get(AttributeValue.class), new RecordAttributeHandler());

        simpleOperatorHandlerMap = new HashMap<SimpleTypeOperator, SimpleOperatorHandler>(
                Operator.values().length);
        simpleOperatorHandlerMap.put(Operator.CONTAINS, new ContainsHandler());
        simpleOperatorHandlerMap.put(Operator.EQUALS, new EqualsHandler());
    }

    /**
     * Applies the specified <code>action</code> contained by the
     * <code>threshold</code> to the <code>entity</code> using the given
     * <code>session</code>, if necessary.
     * 
     * @param sesh
     *            the session to use if database access is required.
     * @param threshold
     *            the threshold that contains the <code>action</code> and is
     *            currently being processed.
     * @param entity
     *            the entity to which the threshold and action applies.
     * @param action
     *            the action to execute.
     */
    public void applyAction(Session sesh, Threshold threshold, Object entity,
            Action action) {
        ActionHandler handler = actionHandlerMap.get(action.getActionType());
        if (handler == null) {
            log.error("No action handler for action type: "
                    + action.getActionType());
        } else {
            try {
                handler.executeAction(sesh, threshold, entity, action);
            } catch (ClassNotFoundException cfe) {
                log.error("Unable to execute threshold action", cfe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Session sesh, Object entity, Condition condition) {
        boolean match = true;
        try {

            ConditionOperator operator;
            OperatorHandler handler;
            if (condition.isSimplePropertyType()) {
                operator = condition.getValueOperator();
                handler = simpleOperatorHandlerMap.get(operator);
            } else {
                operator = condition.getComplexTypeOperator();
                handler = operatorHandlerMap.get(operator);
            }

            if (handler == null) {
                log.error("No operator handler found for operator: "
                        + operator.toString());
                System.err.println("No operator handler found");
                match = false;
            } else {
                match = match && handler.match(sesh, this, entity, condition);
            }

        } catch (Exception e) {
            log.warn("Error matching condition on property "+condition.getPropertyPath(), e);
            match = false;
        }

        return match;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(SimpleTypeOperator operator, Object objA, Object objB) {
        SimpleOperatorHandler handler = simpleOperatorHandlerMap.get(operator);
        if (handler == null) {
            log.warn("No operator handler found for operator: "
                    + operator.toString());
            return false;
        } else {
            return handler.match(objA, objB);
        }
    }

    private String getReferenceKey(PersistentImpl entity) {
        if(entity == null) {
            throw new NullPointerException();
        }
        return String.format("%s.%d", entity.getClass().getCanonicalName(), entity.getId());
    }

    /**
     * Registers the instance that is being worked on by the thresholding framework. 
     * @param entity the instance being processed.
     * @see #referenceCountSet
     */
    public void registerReference(PersistentImpl entity) {
        if(entity == null) {
            throw new NullPointerException();
        }
        referenceCountSet.add(getReferenceKey(entity));
    }

    /**
     * Removes the specified instance from the set of instances being processed
     * by the thresholding framework.
     * @param entity the instance to be removed.
     * * @see #referenceCountSet
     */
    public void deregisterReference(PersistentImpl entity) {
        if(entity != null) {
            referenceCountSet.remove(getReferenceKey(entity));
        }
    }

    /**
     * Returns true if the specified instance is currently being processed
     * by the thresholding framework, false otherwise.
     * @param entity the entity that may require testing for threshold matches.  
     * @return true if the specified instance is currently being processed
     * by the thresholding framework, false otherwise.
     */
    public boolean isRegisteredReference(PersistentImpl entity) {
        return entity != null && referenceCountSet.contains(getReferenceKey(entity));
    }
    
    /**
     * Checks the active {@link Threshold Thresholds} to determine if there is one
     * with a {@link Condition} matching on this {@link Attribute}.
     * @param attribute the {@link Attribute} to search for thresholds on
     * @return true if there is an active {@linkThreshold Thresholds} with a 
     * {@link Condition} on this {@link Attribute}, false otherwise
     */
    public boolean isActiveThresholdForAttribute(Attribute attribute) {

        // get all the active thresholds
        List<Threshold> thresholds = thresholdDAO.getEnabledThresholdByClassName(Record.class.getCanonicalName());
        thresholds.addAll(thresholdDAO.getEnabledThresholdByClassName(Survey.class.getCanonicalName()));
        List<Attribute> attributes = new ArrayList<Attribute>(1);
        attributes.add(attribute);
        for (Threshold threshold : thresholds) {
            if (matchesThreshold(threshold, attributes)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the supplied List of Attributes to see if they have the potential to trigger an action of the
     * supplied type.
     * @param actionType the type of action to check for.
     * @param attributes the Attributes to check.
     * @return true if the supplied Attributes have the potential to trigger the specified action.
     */
    public boolean canTriggerAction(ActionType actionType, List<Attribute> attributes) {
        List<Threshold> thresholds = thresholdDAO.getEnabledThresholdByClassName(Record.class.getCanonicalName());
        thresholds.addAll(thresholdDAO.getEnabledThresholdByClassName(Survey.class.getCanonicalName()));
        for (Threshold threshold : thresholds) {
            for (Action action : threshold.getActions()) {
                if (action.getActionType() == actionType) {
                    if (matchesThreshold(threshold, attributes)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks the supplied List of Attributes to see if they have the potential to trigger the supplied Threshold
     * @param threshold the threshold to check.
     * @param attributes the Attributes to check.
     * @return true if the supplied Attributes have the potential to trigger the specified threshold.
     */
    private boolean matchesThreshold(Threshold threshold, List<Attribute> attributes) {

        Session sesh = RequestContextHolder.getContext().getHibernate();

        List<Condition> conditions = threshold.getConditions();
        for (Condition condition : conditions) {
            String[] propertyPath = condition.getPropertyPath().split("\\.");
            Survey survey = new Survey();
            if (propertyPath.length >= 1) {
                String attrPath = propertyPath[0];
                if (attrPath.equals("survey") && propertyPath.length >= 2) {
                    if (propertyPath[1].equals("attributes")) {
                        // create a dummy record for testing this condition
                        Record rec = new Record();

                        // must remove all other attributes from the survey so that the
                        // thresholds only match on the current attribute not all survey attributes
                        survey.setAttributes(attributes);
                        rec.setSurvey(survey);
                        for (Attribute attribute : attributes) {
                            AttributeValue val = new AttributeValue();
                            val.setAttribute(attribute);
                            rec.getAttributes().add(val);
                        }
                        boolean isThold = condition.applyCondition(sesh, rec, this);
                        // only return if it is true so that we check all conditions until a match is found
                        if (isThold) {
                            return isThold;
                        }
                    }
                } else if (threshold.getClassName().equals(Survey.class.getCanonicalName())) {
                    // must remove all other attributes from the survey so that the
                    // thresholds only match on the current attribute not all survey attributes
                    survey.setAttributes(attributes);
                    boolean isThold = condition.applyCondition(sesh, survey, this);
                    // only return if it is true so that we check all conditions until a match is found
                    if (isThold) {
                        return isThold;
                    }
                } else {
                    if (attrPath.equals("location") && propertyPath.length >= 2) {
                        attrPath = propertyPath[1];
                    }
                    if (attrPath.equals("attributes")) {
                        boolean isThold = false;
                        for (Attribute attribute: attributes) {
                            if (condition.getKeyOperator().equals(Operator.EQUALS)) {
                                isThold = attribute.getName().equals(condition.getKey());
                            } else if (condition.getKeyOperator().equals(Operator.CONTAINS)) {
                                isThold = attribute.getName().contains(condition.getKey());
                            }
                        }
                        // only return if it is true so that we check all conditions until a match is found
                        if (isThold) {
                            return isThold;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks the active {@link Threshold Thresholds} to determine if there is one
     * with a {@link Condition} matching on this {@link RecordProperty}.
     * @param prop the {@link RecordProperty} to search for thresholds on
     * @return true if there is an active {@linkThreshold Thresholds} with a 
     * {@link Condition} on this {@link RecordProperty}, false otherwise
     */
    public boolean isActiveThresholdForRecordProperty(RecordProperty prop) {
        // get all the active thresholds
        List<Threshold> thresholds = thresholdDAO.getEnabledThresholdByClassName(Record.class.getCanonicalName());
        for (Threshold threshold : thresholds) {
            List<Condition> conditions = threshold.getConditions();
            for (Condition condition : conditions) {
                String[] propertyPath = condition.getPropertyPath().split("\\.");
                if (propertyPath.length >= 1) {
                    if (propertyPath[0].equals(prop.getRecordPropertyType().getName().toLowerCase())) {
                        // the first element in the property path matches this property type,
                        // therefore the condition will match on this property
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
