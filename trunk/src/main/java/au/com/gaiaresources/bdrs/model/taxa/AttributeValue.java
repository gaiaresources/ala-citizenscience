package au.com.gaiaresources.bdrs.model.taxa;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;


/**
 * The value of an attribute attached to a record.
 *
 * @author Tim Carpenter
 *
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "ATTRIBUTE_VALUE")
@AttributeOverride(name = "id", column = @Column(name = "ATTRIBUTE_VALUE_ID"))
public class AttributeValue extends AbstractTypedAttributeValue implements TypedAttributeValue {
	
    private Logger log = Logger.getLogger(getClass());
    
    public static final String NOT_RECORDED = "Not recorded";

    /**
     * Populates the <code>numericValue</code> or <code>dateValue</code> from
     * the contents of the <code>stringValue</code>
     */
    @Transient
    public void populateFromStringValue() throws NumberFormatException,
            ParseException {
        if (attribute == null) {
            return;
        }

        // Nothing to be done for String, Text, String with Valid Values,
        // Image or File

        AttributeType type = attribute.getType();
        if (AttributeType.INTEGER.equals(type)
                || AttributeType.DECIMAL.equals(type)) {
            BigDecimal num = null;
            if (!stringValue.isEmpty()) {
                num = new BigDecimal(stringValue);
            }
            setNumericValue(num);
        } else if (AttributeType.DATE.equals(type)) {
            Date date = null;
            if (!stringValue.isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
                date = dateFormat.parse(stringValue);
            }
            setDateValue(date);
        }
    }

    /**
     * Get the attribute definition that this value is for.
     * @return {@link TaxonGroupAttribute}
     */
    @CompactAttribute
    @ManyToOne
    @JoinColumn(name = "ATTRIBUTE_ID", nullable = false)    
    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }


    /**
     * Get the value as an number, returns a value if and only if the type of
     * the {@link TaxonGroupAttribute} is integer or decimal.
     * @return {@link BigDecimal}
     */
    @CompactAttribute
    // Originally the precision and scale was not defined and the generated SQL
    // set up a numeric precision of 19,2. The scale of 2 was far too small so 
    // this was increased to 12. Why 12? Because it was a nice round number.
    // This setup will support 12 digits before the decimal point, and 12 after.
    @Column(name = "NUMERIC_VALUE", precision=24, scale=12)
    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(BigDecimal numericValue) {
        this.numericValue = numericValue;
    }

    
    /**
     * Get the value as a string, returns a value if and only if the type of
     * the {@link TaxonGroupAttribute} is string.
     * @return {@link String}
     */
    @CompactAttribute
    @Column(name = "STRING_VALUE")
    @Index(name="attribute_value_string_value_index")
    @Lob  // makes a 'text' type in the database
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Get the value as a date, returns a value if and only if the type of
     * the {@link TaxonGroupAttribute} is string.
     * @return {@link Date}
     * @return
     */
    @CompactAttribute
    @Column(name = "DATE_VALUE")
    public Date getDateValue() {
        return dateValue != null ? new Date(dateValue.getTime()) : null;
    }

    public void setDateValue(Date dateValue) {
        if (dateValue != null) {
            this.dateValue = new Date(dateValue.getTime());
        } else {
            this.dateValue = null;
        }
    }
}