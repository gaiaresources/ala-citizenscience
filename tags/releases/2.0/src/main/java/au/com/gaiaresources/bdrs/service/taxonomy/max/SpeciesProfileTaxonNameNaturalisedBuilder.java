package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

/**
 * Creates a SpeciesProfile from the Naturalised column of a Max Name Row.
 */
public class SpeciesProfileTaxonNameNaturalisedBuilder extends SpeciesProfileBuilder {
    /**
     * The character stored in the Max Name Row if the taxon is naturalised.
     */
    public static final String IS_NATURALISED = "*";

    /**
     * Creates a new instance.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileTaxonNameNaturalisedBuilder(String header) {
        super(MaxNameRow.ColumnName.NATURALISED, header);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        if(IS_NATURALISED.equals(content)) {
            return "Yes";
        } else {
            return content;
        }
    }
}
