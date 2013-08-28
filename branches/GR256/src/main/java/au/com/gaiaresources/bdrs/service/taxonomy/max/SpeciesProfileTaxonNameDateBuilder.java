package au.com.gaiaresources.bdrs.service.taxonomy.max;

import au.com.gaiaresources.taxonlib.importer.max.MaxNameRow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates a SpeciesProfile from a date column in a Max Name Row.
 */
public class SpeciesProfileTaxonNameDateBuilder extends SpeciesProfileBuilder {
    /**
     * The format of a date value in the Max Name Row.
     */
    public static final String SOURCE_FORMAT_PATTERN = "dd/MM/yyyy";
    /**
     * The desired date format in the Species Profile.
     */
    public static final String TARGET_FORMAT_PATTERN = "dd MMM yyyy";

    private SimpleDateFormat source_formatter;
    private SimpleDateFormat target_formatter;

    /**
     * Creates a new instance.
     * @param columnName the name of the column that this builder will store in the Species Profile.
     * @param header the header (species profile description) of the profiles created).
     */
    public SpeciesProfileTaxonNameDateBuilder(MaxNameRow.ColumnName columnName, String header) {
        super(columnName, header);

        source_formatter = new SimpleDateFormat(SOURCE_FORMAT_PATTERN);
        target_formatter = new SimpleDateFormat(TARGET_FORMAT_PATTERN);
    }

    @Override
    protected String getContent(MaxNameRow nameRow) {
        String content = super.getContent(nameRow);
        try {
            Date d = source_formatter.parse(content);
            return target_formatter.format(d);
        } catch(ParseException pe) {
            return content;
        }
    }
}
