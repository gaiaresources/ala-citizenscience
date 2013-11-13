package au.com.gaiaresources.bdrs.model.record;


import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: aaron
 * Date: 8/10/13
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "RECORD_GROUP")
@AttributeOverride(name = "id", column = @Column(name = "RECORD_GROUP_ID"))
public class RecordGroup extends PortalPersistentImpl {

    private String type;
    private Date startDate;
    private Date endDate;
    private Survey survey;
    private User user;

    private Set<Record> records;

    private Set<Metadata> metadata = new HashSet<Metadata>();

    @CompactAttribute
    @Column(name = "type", nullable = true)
    @Index(name="record_group_type_index")
    @Lob  // makes a 'text' type in the database
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @CompactAttribute
    @Column(name = "START_DATE", nullable = true)
    public Date getStartDate() {
        return startDate != null ? new Date(startDate.getTime()) : null;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate != null ? new Date(startDate.getTime()) : null;
    }

    @CompactAttribute
    @Column(name = "END_DATE", nullable = true)
    public Date getEndDate() {
        return endDate != null ? new Date(endDate.getTime()) : null;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate != null ? new Date(endDate.getTime()) : null;
    }

    @OneToMany(mappedBy = "recordGroup", fetch = FetchType.LAZY)
    public Set<Record> getRecords() {
        return records;
    }

    public void setRecords(Set<Record> records) {
        this.records = records;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SURVEY_ID", nullable = true)
    @ForeignKey(name = "RECORD_GROUP_SURVEY_FK")
    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = true)
    @ForeignKey(name = "RECORD_GROUP_USER_FK")
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Many to many is a work around (read hack) to prevent a unique
    // constraint being applied on the metadata id.
    @ManyToMany(fetch = FetchType.LAZY)
    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the metadata value
     * @param key metadata key
     * @return metadata value
     */
    @Transient
    public String getMetadataValue(String key) {
        if (key == null) {
            return "";
        }

        for (Metadata md : this.getMetadata()) {
            if (md.getKey().equals(key)) {
                return md.getValue();
            }
        }

        return "";
    }
}