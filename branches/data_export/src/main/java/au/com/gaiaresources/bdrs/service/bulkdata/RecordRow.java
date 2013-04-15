package au.com.gaiaresources.bdrs.service.bulkdata;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.service.lsid.LSIDService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.text.ParseException;

public interface RecordRow extends StyledRow {

    public static final String GPS_LOCATION = "GPS LOCATION";
    
    /**
     * Writes core help text to the specified help sheet.
     * @param helpSheet The sheet where help text shall be written.
     * @param rowIndex The row index where help text shall start to be written.
     * @return The next available row after help text has beeen written.
     */
    public int writeCoreHelp(Sheet helpSheet, int rowIndex);

    public void writeHeader(Row superHeaderRow, Row headerRow, Survey survey);

    /**
     * Writes Record data to one or more rows in the supplied Sheet.  Generally a Record will be written to
     * a single row, however Records containing matrix data types may be written over multiple rows.
     * @param lsidService used to create a LSID for the Record.
     * @param observationSheet the Sheet to write the Record to.
     * @param rowIndex the row number (in the supplied Sheet) to start writing the Record at.
     * @param record the Record to write.
     * @return the number of rows written.
     */
    public int writeRecord(LSIDService lsidService, Sheet observationSheet, int rowIndex, Record record);

    public void readHeader(Survey survey, Row superHeaderRow, Row row) throws ParseException;

    public RecordUpload readRow(Survey survey, Row row);
    
    public boolean isHeader(Row row);
}
