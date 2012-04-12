package au.com.gaiaresources.bdrs.model.index;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.util.DateFormatter;

/**
 * Represents a schedule on which to build indexes.
 * 
 * @author stephanie
 *
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "INDEX_SCHEDULE")
@AttributeOverride(name = "id", column = @Column(name = "INDEX_ID"))
public class IndexSchedule extends PortalPersistentImpl {
    private IndexType type;
    private Date date;
    private boolean fullRebuild = false;
    private String className;
    
    /**
     * Default constructor.
     */
    public IndexSchedule() {
        
    }
    
    public IndexSchedule(String className, IndexType type) {
        this.className = className;
        this.type = type;
    }
    
    public IndexSchedule(String className, IndexType type, boolean fullRebuild) {
        this(className, type);
        this.fullRebuild = fullRebuild;
    }
    
    public IndexSchedule(String className, IndexType type, boolean fullRebuild, Date date) {
        this(className, type, fullRebuild);
        this.date = (Date) date.clone();
    }
    
    /**
     * @return the type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "INDEX_TYPE", nullable=false)
    public IndexType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(IndexType type) {
        this.type = type;
    }
    /**
     * @return the date
     */
    @Column(name = "INDEX_DATE", nullable=true)
    public Date getDate() {
        return (Date) date.clone();
    }
    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = (Date) date.clone();
    }
    /**
     * @return the fullRebuild
     */
    @Column(name = "FULL_REBUILD", nullable=false)
    public boolean isFullRebuild() {
        return fullRebuild;
    }
    /**
     * @param fullRebuild the fullRebuild to set
     */
    public void setFullRebuild(boolean fullRebuild) {
        this.fullRebuild = fullRebuild;
    }
    /**
     * @return the className
     */
    @Column(name = "CLASS_NAME", nullable=false)
    public String getClassName() {
        return className;
    }
    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
    /**
     * Gets the interval between which to run this index build.  Returns -1 if the 
     * index is only scheduled to run once or on server startup.
     * @return
     */
    @Transient
    public long getPeriod() {
        long period = -1;
        Calendar cal = Calendar.getInstance();
        Calendar periodCal = Calendar.getInstance();
        
        switch (getType()) {
            case SERVER_STARTUP:
            case ONCE:
                // no period for server startup or once
                break;
            case DAILY:
                cal.setTime(getDate());
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.HOUR_OF_DAY, 24);
                period = periodCal.getTimeInMillis() - cal.getTimeInMillis();
                break;
            case WEEKLY:
                cal.setTime(getDate());
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.DAY_OF_YEAR, 7);
                period = periodCal.getTimeInMillis() - cal.getTimeInMillis();
                break;
            case MONTHLY:
                cal.setTime(getDate());
                periodCal.setTime(cal.getTime());
                periodCal.add(Calendar.MONTH, 1);
                period = periodCal.getTimeInMillis() - cal.getTimeInMillis();
                break;
            default:
                break;
        }
        return period;
    }
    
    /**
     * Returns the date in String format specified by {@link DateFormatter::$DAY_MONTH_YEAR}.
     */
    @Transient
    public String getDateString() {
        if (getDate() == null) {
            return "";
        }
        return DateFormatter.format(getDate(), DateFormatter.DAY_MONTH_YEAR);
    }
    
    /**
     * Returns the time in String format specified by {@link DateFormatter::$TIME}.
     */
    @Transient
    public String getTimeString() {
        if (getDate() == null) {
            return "";
        }
        return DateFormatter.format(getDate(), DateFormatter.TIME);
    }
    
    /**
     * Returns the day of the week as specified by {@link Calendar::$DAY_OF_WEEK}.
     */
    @Transient
    public int getDayOfWeek() {
        if (getDate() == null) {
            return 1;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(getDate());
        return cal.get(Calendar.DAY_OF_WEEK);
    }
}
