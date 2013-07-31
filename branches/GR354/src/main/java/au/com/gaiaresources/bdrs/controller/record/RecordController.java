package au.com.gaiaresources.bdrs.controller.record;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.attribute.AttributeFormController;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.preference.PreferenceUtil;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Base controller for viewing/editing records.  Contains common code for 
 * determining view type.
 * 
 * @author stephanie
 *
 */
public class RecordController extends AttributeFormController {
    public static final String MAP_TAB = "map";
    public static final String TABLE_TAB = "table";
    public static final String DOWNLOAD_TAB = "download";
    public static final String DEFAULT_TAB = MAP_TAB;

    public static final String PREFIX_TEMPLATE = "%d_";

    public static final int STARTING_SIGHTING_INDEX = 0;
    /**
     * The list of record form field collection objects that may be populated with
     * attribute value data,.
     */
    public static final String MODEL_RECORD_ROW_LIST = "recordFieldCollectionList";
    
    /**
     * The form fields used to create the header of the sightings table
     */
    public static final String MODEL_SIGHTING_ROW_LIST = "sightingRowFormFieldList";
    
    /** Required to get the default view to pass to the messages */
    @Autowired
    private PreferenceDAO preferenceDAO;

    
    /*
     * Returns the default tab (map or table) to display if it has not been specified in the request.
     * The default is determined by the value of the Preference.DEFAULT_TO_MAP_VIEW_KEY preference.
     * @return the default tab to display (MAP_TAB or TABLE_TAB).
     */
    protected String defaultTab() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(preferenceDAO);
        try {
            boolean useMap = preferenceUtil.getBooleanPreference(Preference.MY_SIGHTINGS_DEFAULT_VIEW_KEY);
            return useMap ? MAP_TAB : TABLE_TAB;    
        } catch (NullPointerException npe) {
            // default to table view.
            return TABLE_TAB;
        }
        
    }
    
    protected static String getSightingPrefix(int sightingIndex) {
        return String.format(PREFIX_TEMPLATE, sightingIndex);
    }
    

    protected List<Record> getRecordsForFormInstance(Record rec, User accessor) {
        return null;
    }

    /**
     * Model object "ident" is required for deleting rows from census method 
     * attributes tables which can occur on any record entry form now.  This method
     * is a helper to add the ident object to the ModelAndView when editing a record.
     * Also adds the survey custom css file URL to the model and view.
     * @param mv the ModelAndView to add the ident object to
     * @param accessor the user accessing/editing the record
     * @param survey the survey that will own the created record
     * @return the ModelAndView with the "ident" added
     */
    public ModelAndView addRecord(ModelAndView mv, User accessor, Survey survey) {
        if (accessor != null) {
            mv.addObject("ident", accessor.getRegistrationKey());
        }
        if (survey != null) {
            // add survey specific CSS
            Metadata cssLayoutMetadata = survey.getMetadataByKey(Metadata.SURVEY_CSS);
            if (cssLayoutMetadata != null) {
                mv.addObject(BdrsWebConstants.MV_CSS_FORM_LAYOUT_URL, cssLayoutMetadata.getFileURL());
            }
            // add survey specific JS
            Metadata jsMetadata = survey.getMetadataByKey(Metadata.SURVEY_JS);
            if (jsMetadata != null) {
                mv.addObject(BdrsWebConstants.MV_CUSTOM_JS_URL, jsMetadata.getFileURL());
            }
        }
        return mv;
    }
    
    /**
     * Converts a valueMap of <String,String> back into a <String,String[]> parameter mapping.
     * This is necessary to reuse RecordDeserializer to mock save records for 
     * repopulation of form on error
     * 
     * @param valueMap
     * @return
     */
    protected Map<String, String[]> convertMap(Map<String, String> valueMap) {
        // create a new map of type <String, String[]> and copy the values
        Map<String, String[]> newMap = new LinkedHashMap<String, String[]>(valueMap.size());
        for (Entry<String, String> entry : valueMap.entrySet()) {
            String[] values = new String[] {entry.getValue()};
            if (entry.getValue().contains(",")) {
                values = entry.getValue().split(",");
            }
            newMap.put(entry.getKey(), values);
        }
        return newMap;
    }
}
