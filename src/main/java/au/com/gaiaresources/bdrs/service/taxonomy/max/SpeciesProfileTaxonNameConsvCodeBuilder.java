package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a SpeciesProfile from the conservation code of a Max Name Row.
 */
public class SpeciesProfileTaxonNameConsvCodeBuilder extends SpeciesProfileBuilder {

    /**
     * A mapping between conservation codes and the display value.
     */
    public static final Map<String, String> CODE_LOOKUP;
    static {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put("T", "T: Threatened Flora (Declared Rare Flora - Extant)");
        temp.put("X", "X: Presumed Extinct Flora (Declared Rare Flora - Extinct)");
        temp.put("1", "1: Priority One: Poorly-known taxa");
        temp.put("2", "2: Priority Two: Poorly-known taxa");
        temp.put("3", "3: Priority Three: Poorly-known taxa");
        temp.put("4", "4: Priority Four: Rare, Near Threatened and other taxa in need of monitoring");
        temp.put("5", "5: Priority Five: Conservation Dependent taxa");

        CODE_LOOKUP = Collections.unmodifiableMap(temp);
    }

    /**
     * Creates a new instance.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileTaxonNameConsvCodeBuilder(String header) {
        super(MaxNameRow.ColumnName.CONSV_CODE, header);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        String displayName = CODE_LOOKUP.get(content);
        return displayName == null ? content : displayName;
    }
}
