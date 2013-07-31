package au.com.gaiaresources.bdrs.service.taxonomy.nsw;

import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraRow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a SpeciesProfile from the Naturalised status code of a NSW Row.
 */
public class SpeciesProfileNaturalisedStatusBuilder extends SpeciesProfileBuilder {
    /**
     * A mapping between "Informal" codes and the display value.
     */
    public static final Map<String, String> CODE_LOOKUP;
    static {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put("N", "Native");
        temp.put("I", "Introduced");
        temp.put("N/I", "Native/Introduced");

        CODE_LOOKUP = Collections.unmodifiableMap(temp);
    }

    /**
     * Creates a new instance.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileNaturalisedStatusBuilder(String header) {
        super(NswFloraRow.ColumnName.NATIVE_INTRODUCED, header);
    }

    @Override
    protected String getContent(NswFloraRow nameRow) {
        String content = super.getContent(nameRow);
        String displayName = CODE_LOOKUP.get(content);
        return displayName == null ? content : displayName;
    }
}
