package au.com.gaiaresources.bdrs.model.preference;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;

/**
 * A logical grouping of {@link Preference}s.
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "PREFERENCECATEGORY")
@AttributeOverride(name = "id", column = @Column(name = "CATEGORY_ID"))
public class PreferenceCategory extends PortalPersistentImpl implements Comparable<PreferenceCategory> {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    private String name;
    private String displayName;
    private String description;

    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name = "DISPLAY_NAME", nullable = false)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    

    @Column(name = "DESCRIPTION", nullable = false)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(PreferenceCategory o) {
        if (o == null) {
            return 1;
        }
        // first sort by weight, this should be done in the super class
        int compare = this.getWeight() - o.getWeight();
        if (compare == 0) {
            // if the weights are equal, compare by name
            compare = this.getName().compareTo(o.getName());
        }
        return compare;
    }
}
