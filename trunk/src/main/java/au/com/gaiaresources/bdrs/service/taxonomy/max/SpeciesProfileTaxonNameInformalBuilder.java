package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a SpeciesProfile from the "Informal" code of a Max Name Row.
 */
public class SpeciesProfileTaxonNameInformalBuilder extends SpeciesProfileBuilder {
    /**
     * A mapping between "Informal" codes and the display value.
     */
    public static final Map<String, String> CODE_LOOKUP;
    static {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put("MS", "Manuscript Name");
        temp.put("PN", "Phrase Name");
        temp.put("", "Published Name");

        CODE_LOOKUP = Collections.unmodifiableMap(temp);
    }

    /**
     * Creates a new instance.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileTaxonNameInformalBuilder(String header) {
        super(MaxNameRow.ColumnName.INFORMAL, header);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        String displayName = CODE_LOOKUP.get(content);
        return displayName == null ? content : displayName;
    }
}
