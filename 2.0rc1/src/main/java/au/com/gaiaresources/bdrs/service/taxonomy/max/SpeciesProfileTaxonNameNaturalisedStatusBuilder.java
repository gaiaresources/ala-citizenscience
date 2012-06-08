package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a SpeciesProfile from the naturalised status code of a Max Name Row.
 */
public class SpeciesProfileTaxonNameNaturalisedStatusBuilder extends SpeciesProfileBuilder {

    /**
     * A mapping between naturalised status codes and the display value.
     */
    public static final Map<String, String> CODE_LOOKUP;
    static {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put("M", "Mixed (Native in Part of Range, Naturalised Elsewhere)");
        temp.put("A", "Alien to Western Australia");
        temp.put("N", "Native to Western Australia");
        CODE_LOOKUP = Collections.unmodifiableMap(temp);
    }

    /**
     * Creates a new instance.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileTaxonNameNaturalisedStatusBuilder(String header) {
        super(MaxNameRow.ColumnName.NATURALISED_STATUS, header);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        String displayName = CODE_LOOKUP.get(content);
        return displayName == null ? content : displayName;
    }
}
