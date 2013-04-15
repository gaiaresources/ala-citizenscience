package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

/**
 * Creates a SpeciesProfile from a yes/no column in a Max Name Row.
 */
public class SpeciesProfileTaxonNameYesNoBuilder extends SpeciesProfileBuilder {
    /**
     * The character expected in the Max Name Row for 'Yes'.
     */
    public static final String Y = "Y";
    /**
     * The character expected in the Max Name Row for 'No'.
     */
    public static final String N = "N";

    /**
     * Creates a new instance.
     * @param columnName the name of the column that this builder will store in the Species Profile.
     * @param header the header (species profile description) of the profiles created).
     */
    public SpeciesProfileTaxonNameYesNoBuilder(MaxNameRow.ColumnName columnName, String header) {
        super(columnName, header);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        if(Y.equals(content)) {
            return "Yes";
        } else if(N.equals(content)) {
            return "No";
        } else {
            return content;
        }
    }
}
