package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.taxonlib.importer.max.MaxFamilyRow;

public class FamilyProfileBuilder {

    private String header;
    private MaxFamilyRow.ColumnName columnName;

    /**
     * Creates a new instance.
     * @param columnName the name of the column that this builder will store in the Species Profile.
     * @param header the header (species profile description) of the profiles created.
     */
    public FamilyProfileBuilder(MaxFamilyRow.ColumnName columnName, String header) {
        this.columnName = columnName;
        this.header = header;
    }

    /**
     * Creates a new Species Profile from the name row provided.
     * @param nameRow the row of Max data to be stored.
     * @return a newly created Species Profile for the column, or null if there is no data.
     */
    public SpeciesProfile createProfile(MaxFamilyRow nameRow) {
        String content = getContent(nameRow);
        if(content.isEmpty()) {
            return null;
        }

        SpeciesProfile sp = new SpeciesProfile();
        sp.setType(SpeciesProfile.SPECIES_PROFILE_TEXT);
        sp.setHeader(this.columnName.toString());
        sp.setDescription(this.header);
        sp.setContent(content);

        return sp;
    }

    /**
     * For the column represented by this builder, retrieve the content and if necessary process the raw value
     * into a displayable format.
     * @param nameRow the row of source Max data.
     * @return the post processed content value. This value should not be null but may be empty string if
     * there is no value.
     */
    protected String getContent(MaxFamilyRow nameRow) {
        String content = nameRow.getValue(this.columnName);
        return content == null ? "" : content;
    }
}
