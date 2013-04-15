package au.com.gaiaresources.bdrs.service.bulkdata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.com.gaiaresources.bdrs.model.taxa.Attribute;

/**
 * Represents an uploaded <code>Location</code>. The Location may or may not 
 * have been saved. If the Location is unsaved, the primary key of this object
 * will be {@link LocationUpload#DEFAULT_PK}.
 */
public class LocationUpload extends CoordUpload {
    
    public static final int DEFAULT_PK = 0;

    private int pk = DEFAULT_PK;
    private String surveyName;
    private String locationName;
    
    private Map<Attribute, String> attrMap = new HashMap<Attribute, String>();

    public LocationUpload() {
    }

    public LocationUpload(String surveyName, String locationName, double latitude, double longitude) {
        this(DEFAULT_PK, surveyName, locationName, latitude, longitude);
    }
    
    public LocationUpload(int pk, String surveyName, String locationName, double latitude, double longitude) {
        this.pk = pk;
        this.surveyName = surveyName;
        this.locationName = locationName;
        setLatitude(latitude);
        setLongitude(longitude);
    }

    public String getSurveyName() {
        return surveyName;
    }

    public void setSurveyName(String surveyName) {
        this.surveyName = surveyName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public void setAttributeValue(Attribute attr, String value) {
        attrMap.put(attr, value);
    }
    
    public String getAttributeValue(Attribute attr) {
        return attrMap.get(attr);
    }
    
    public int getPk() {
        return pk;
    }

    public void setPk(int pk) {
        this.pk = pk;
    }

    @Override
    public String toString() {
        final int maxLen = 100;
        return "LocationUpload [attrMap="
                + (attrMap != null ? toString(attrMap.entrySet(), maxLen)
                        : null) + ", error=" + isError() + ", errorMessage="
                + getErrorMessage() + ", latitude=" + getLatitude() + ", locationName="
                + locationName + ", longitude=" + getLongitude() + ", pk=" + pk
                + ", surveyName=" + surveyName + "]";
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext()
                && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }
}
