package au.com.gaiaresources.bdrs.controller.record;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Some handy methods for helping to test census method attributes, especially 
 * recursive ones...
 *
 */
public abstract class AbstractCensusMethodAttributeTest extends AbstractControllerTest {

    /**
     * Add parameters to the request object. 
     * 
     * @param entry The ParamEntry tree to add to the request.
     * @param request Mock request to fill with parameters.
     */
    protected void addParams(ParamEntry entry, MockHttpServletRequest request) {
        addParamsRecursive(entry, request, "");
    }
    
    /**
     * Recursively add parameters to the request object.
     * 
     * Reversed engineered from attributeRenderer.jsp and attributesRecordRow.jsp
     * 
     * @param entry The ParamEntry tree to add to the request.
     * @param request Mock request to fill with parameters.
     * @param prefix The prefix to use for request parameter keys.
     */
    private void addParamsRecursive(ParamEntry entry, MockHttpServletRequest request, String prefix) {
        if (request == null) {
            throw new NullPointerException("request cannot be null");
        }
        
        Attribute attr = entry.getAttr();
        Record rec = entry.getRecord();
        String recIdString = rec != null ? (rec.getId().toString() + "_") : "";
        String rowIdxString = entry.getRowIdx() != null ? String.format("%d_", entry.getRowIdx()) : "";
        if (attr.getType() == AttributeType.CENSUS_METHOD_COL) {
            
            String baseParamName2 = prefix+rowIdxString+"attribute_"+attr.getId()+"_";
            String baseParamName = prefix+"attribute_"+attr.getId()+"_";
            request.addParameter(baseParamName2+"recordId", rec != null ? rec.getId().toString() : "0");
            request.addParameter(baseParamName2+"rowIndex", Integer.toString(entry.getRowIdx()));

            // apparently this is just how it works...
            if (rec != null) {
                request.addParameter(baseParamName+"rowPrefix", baseParamName2+"_record_"+recIdString);
            } else {
                request.addParameter(baseParamName+"rowPrefix", rowIdxString);
            }
        } else if (attr.getType() == AttributeType.CENSUS_METHOD_ROW) {
            // record id
            request.addParameter(prefix+"attribute_"+attr.getId()+"_recordId", rec != null ? rec.getId().toString() : "0");

            
        } else {
            // it's a value!
            request.addParameter(prefix+"attribute_"+attr.getId(), entry.getValue());
        }
        
        prefix += rowIdxString + "attribute_"+attr.getId()+"_record_"+ recIdString;
        
        // recurse through children...
        for (ParamEntry child : entry.getChildren()) {
            addParamsRecursive(child, request, prefix);
        }
    }
    
    
    /**
     * Used to build parameter map. Look at existing tests for examples.
     */
    public static class ParamEntry {
        private List<ParamEntry> children = new ArrayList<ParamEntry>();
        
        private Integer rowIdx;
        private Attribute attr;
        private Record rec;
        private String value;
        
        public ParamEntry(Integer rowIdx, Record rec, Attribute attr, String value) {
            
            if (rec != null && value != null) {
                throw new IllegalArgumentException("Only Record OR String can be non null.");
            }
            
            this.rowIdx = rowIdx;
            this.attr = attr;
            this.rec = rec;
            this.value = value;
        }
        
        public Integer getRowIdx() {
            return rowIdx;
        }
        public Attribute getAttr() {
            return attr;
        }
        public Record getRecord() {
            return rec;
        }
        
        public String getValue() {
            return value;
        }
        
        public List<ParamEntry> getChildren() {
            return children;
        }
        
        public void addChild(ParamEntry pe) {
            children.add(pe);
        }
    }

    /**
     * Logging helper.
     * @param request
     */
    protected void logMap(MockHttpServletRequest request) {
        log.debug("------------------------- BEGIN MAP DUMP");
        for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            for (String value : entry.getValue()) {
                log.debug(entry.getKey() + " : " + value);
            }
        }
        log.debug("*************************  END MAP DUMP");
    }
}
