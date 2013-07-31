package au.com.gaiaresources.bdrs.model.taxa;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.WeightComparator;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.util.CSVUtils;
import au.com.gaiaresources.bdrs.util.DateFormatter;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.annotations.CollectionOfElements;
import org.springframework.util.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Abstract helper class to implement common behaviour in TypedAttributeValue
 * implementations
 *
 */
@MappedSuperclass
public abstract class AbstractTypedAttributeValue extends PortalPersistentImpl implements TypedAttributeValue {

    public static final String NOT_RECORDED = "Not recorded";
    protected Attribute attribute;
    protected BigDecimal numericValue;
    protected String stringValue = "Not recorded";
    protected Date dateValue;
    protected Set<Record> records;
    
    @Override
    public String toString() {
        Attribute a = getAttribute();
        switch (a.getType()) {
        case INTEGER:
        case INTEGER_WITH_RANGE:
            // must turn into an int or we get decimal points in the string representation
            // e.g. 2.00
            if(this.getNumericValue() == null) {
                return this.attribute.isRequired() ? "NaN" : "";
            } else {
                return Integer.toString(this.getNumericValue().intValue());
            }

        case DECIMAL:
            if(this.getNumericValue() == null) {
                return this.attribute.isRequired() ? "NaN" : "";
            } else {
                return this.getNumericValue().stripTrailingZeros().toPlainString();
            }
        case DATE:
            return this.getDateValue() != null ? DateFormatter.format(this.getDateValue(), DateFormatter.DAY_MONTH_YEAR) : "";
        case TIME:  // time is actually stored as a string hh:mm
        case BARCODE:
        case REGEX:
        case STRING:
        case STRING_AUTOCOMPLETE:
        case TEXT:
        
        case HTML:
        case HTML_NO_VALIDATION:
        case HTML_COMMENT:
        case HTML_HORIZONTAL_RULE:

        case STRING_WITH_VALID_VALUES:
        
        case SINGLE_CHECKBOX:
        case MULTI_CHECKBOX:
        case MULTI_SELECT:

        case IMAGE:
        case AUDIO:
        case VIDEO:
        case FILE:
            return this.getStringValue();
        case SPECIES:
            return this.getSpecies() != null ? this.getSpecies().getScientificName() : "";
        case CENSUS_METHOD_ROW:
        case CENSUS_METHOD_COL:
            return "";
        default:
            throw new IllegalStateException("attribute type not handled : " + a.getTypeCode());
        }
    }
        
    @Transient
    public String getFileURL() {
        if(getStringValue() != null) {
            try {
                return String.format(FileService.FILE_URL_TMPL, URLEncoder.encode(getClass()
                        .getCanonicalName(), "UTF-8"), getId(), URLEncoder.encode(
                        getStringValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return String.format(FileService.FILE_URL_TMPL, StringEscapeUtils
                        .escapeHtml(getClass().getCanonicalName()), getId(),
                        StringEscapeUtils.escapeHtml(getStringValue()));
            }
        } else {
            return "";
        }
    }

    @Transient
    public String[] getMultiSelectValue() {
        return CSVUtils.fromCSVString(this.getStringValue());
    }
    
    @Transient
    public String[] getMultiCheckboxValue() {
        return getMultiSelectValue();
    }
    
    @Transient
    public boolean hasMultiCheckboxValue(String val) {
        return hasMultiSelectValue(val);
    }
    
    @Transient
    public void setMultiCheckboxValue(String[] values) {
        // don't want values to be quoted or contain a line end string
        setStringValue(CSVUtils.toCSVString(values, ',', CSVWriter.NO_QUOTE_CHARACTER, "", true));
    }
    
    @Transient
    public boolean hasMultiSelectValue(String val) {
        String[] values = this.getMultiSelectValue();
        return CSVUtils.hasValue(values, val);
    }
    
    @Transient
    public void setMultiSelectValue(String[] values) {
        this.setMultiCheckboxValue(values);
    }

    @Transient
    public Boolean getBooleanValue() {
        return Boolean.valueOf(getStringValue());
    }

    @Transient
    public void setBooleanValue(String value) {
        this.setStringValue(Boolean.valueOf(value).toString());
    }
    
    /*
     * Does this attribute value have a valid value to return to the user?
     */
    @Override
    @Transient
    public boolean isPopulated() {
        if (attribute == null) {
            throw new IllegalStateException("Cannot tell if this AttributeValue is populated without");
        }

        // Nothing to be done for String, Text, String with Valid Values,
        // Image or File
        AttributeType type = attribute.getType();
        
        switch (type) {
        case INTEGER:
        case INTEGER_WITH_RANGE:
        case DECIMAL:
            return numericValue != null;
        
        case DATE:
            return dateValue != null;
            
        case STRING:
        case STRING_AUTOCOMPLETE:
        case TEXT:
        case BARCODE:
        case REGEX:
        case TIME:
        case HTML:
        case HTML_NO_VALIDATION:
        case HTML_COMMENT:
        case HTML_HORIZONTAL_RULE:  
        case STRING_WITH_VALID_VALUES:
        case SINGLE_CHECKBOX:
        case MULTI_CHECKBOX:
        case MULTI_SELECT:
            return StringUtils.hasLength(stringValue);
        case IMAGE:
        case AUDIO:
        case VIDEO:
        case FILE:
            // don't want to return an empty file name or a 'not recorded' file name
            return StringUtils.hasLength(stringValue) && !NOT_RECORDED.equals(stringValue);
        case SPECIES:
            return this.getSpecies() != null;
        case CENSUS_METHOD_ROW:
        case CENSUS_METHOD_COL:
            return getRecords() != null && !getRecords().isEmpty();
        default:
            throw new IllegalStateException("Unhandled type : " + type);
        }
    }

    /**
     * @return the records
     */
    @Override
    @OneToMany(mappedBy="attributeValue", cascade=CascadeType.ALL)
    @CompactAttribute
    @CollectionOfElements(fetch = FetchType.LAZY)
    public Set<Record> getRecords() {
        return records;
    }

    /**
     * @param records the records to set
     */
    public void setRecords(Set<Record> records) {
        this.records = records;
    }

    @Transient
    public List<Record> getOrderedRecords() {
        if (getRecords() == null) {
            return null;
        }
        List<Record> records = new ArrayList<Record>(getRecords());
        Collections.sort(records, new WeightComparator());
        return records;
    }
}
