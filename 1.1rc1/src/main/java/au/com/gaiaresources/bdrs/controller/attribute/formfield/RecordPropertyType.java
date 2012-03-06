package au.com.gaiaresources.bdrs.controller.attribute.formfield;

import java.io.IOException;
import java.io.Writer;

import au.com.gaiaresources.bdrs.json.JSONEnum;
import au.com.gaiaresources.bdrs.json.JSONEnumUtil;

/**
 * The Darwin Core Fields
 * @author timo
 *
 */
public enum RecordPropertyType implements JSONEnum {
    SPECIES("Species"),
    NUMBER("Number", "Individual Count"),
    LOCATION("Location"),
    POINT("Point"),
    ACCURACY("AccuracyInMeters","Accuracy (meters)"),
    WHEN("When","Date"),
    TIME("Time"),
    NOTES("Notes"),
    CREATED("Created", "Creation Date and User", true),
    UPDATED("Updated", "Last Updated Date and User", true);
     
    String name;
    String defaultDescription;

    boolean readOnly;
     
    private RecordPropertyType(String name, String description) {
        this(name, description, false);
    }
    
    private RecordPropertyType(String name, String description, boolean readOnly) {
        this.name = name;
        this.defaultDescription = description;
        this.readOnly = readOnly;
    }
     
    private RecordPropertyType(String name) {
        this(name, name);
    }
     
    public String getName(){
        return this.name;
    }
     
    public String getDefaultDescription(){
        return this.defaultDescription;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
     
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONEnumUtil.writeJSONString(out, this);
    }

    @Override
    public String toJSONString() {
        return JSONEnumUtil.toJSONString(this);
    }
}
