package au.com.gaiaresources.bdrs.service.taxonomy.nsw;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.taxonlib.importer.nswflora.NswFloraRow;

/**
 * Creates a SpeciesProfile from a Name Row by storing the column value as text.
 */
public class SpeciesProfileBuilder {

    private String header;
    private NswFloraRow.ColumnName columnName;

    /**
     * Creates a new instance.
     * @param columnName the name of the column that this builder will store in the Species Profile.
     * @param header the header (species profile description) of the profiles created.
     */
    public SpeciesProfileBuilder(NswFloraRow.ColumnName columnName, String header) {
        this.columnName = columnName;
        this.header = header;
    }

    /**
     * Creates a new Species Profile from the name row provided.
     * @param nameRow the row of data to be stored.
     * @return a newly created Species Profile for the column, or null if there is no data.
     */
    public SpeciesProfile createProfile(NswFloraRow nameRow) {
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
     * @param nameRow the row of source data.
     * @return the post processed content value. This value should not be null but may be empty string if
     * there is no value.
     */
    protected String getContent(NswFloraRow nameRow) {
        String content = nameRow.getValue(this.columnName);
        return content == null ? "" : content;
    }
}
