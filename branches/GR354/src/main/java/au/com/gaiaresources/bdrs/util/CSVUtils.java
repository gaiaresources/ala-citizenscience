package au.com.gaiaresources.bdrs.util;

import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import edu.emory.mathcs.backport.java.util.Arrays;

public class CSVUtils {
    
	/**
	 * Splits a csv string.
	 * @param csvStr string to split.
	 * @return String array. If null is passed in will return empty array.
	 */
    public static String[] fromCSVString(String csvStr) {
        return CSVUtils.fromCSVString(csvStr, CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER);
    }
    
	/**
	 * Splits a csv string.
	 * @param csvStr string to split.
	 * @param separator separator character for csv
	 * @param quotechar quote character for csv
	 * @return String array. If null is passed in will return empty array.
	 */
    public static String[] fromCSVString(String csvStr, char separator, char quotechar) {
    	
    	if (csvStr == null) {
    		return new String[]{};
    	}
    	
        String[] split = null;
        try {
            CSVReader csvReader = new CSVReader(new StringReader(csvStr), separator, quotechar);
            split = csvReader.readNext();
            csvReader.close();
        } catch(IOException ioe) {
            // This can't happen because we are not doing any file or stream IO.
            throw new IOError(ioe);
        }
                
        if(split == null) {
            split = new String[]{};
        }
        return split;
    }
    
    public static String toCSVString(String[] values, boolean sortValues) {
        return CSVUtils.toCSVString(values, CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, sortValues);
    }
    
    public static String toCSVString(String[] values, char separator, char quotechar, boolean sortValues) {
        return toCSVString(values, separator, quotechar, CSVWriter.DEFAULT_LINE_END, sortValues);
    }
    
    /**
     * Converts the string array values into a delimited, quoted string.
     * 
     * @param values       the values to write to a string
     * @param separator    the separator to use between the values
     * @param quoteEntries boolean indicating if each entry should be quoted or not
     * @param useLineEnd   boolean indicating if a line end character should be appended at the end of each line string
     * @param sortValues   boolean indicating if the values should be sorted
     * @return A string representation of the array, delimited by the separator and quoted according to quoteEntries
     */
    public static String toCSVString(String[] values, char separator, boolean quoteEntries, boolean useLineEnd, boolean sortValues) {
        return toCSVString(values, separator, 
                           quoteEntries ? CSVParser.DEFAULT_QUOTE_CHARACTER : CSVWriter.NO_QUOTE_CHARACTER, 
                           useLineEnd ? CSVWriter.DEFAULT_LINE_END : "", sortValues);
    }
    
    /**
     * Converts the string array values into a delimited, quoted string.
     * 
     * @param values     the values to write to a string
     * @param separator  the separator to use between the values
     * @param quotechar  the quote character to use when quoting values
     *                   use CSVWriter.NO_QUOTE_CHARACTER if no quotes are desired
     * @param lineEnd    the line end character to append to the end of each line
     * @param sortValues boolean indicating if the values should be sorted
     * @return A string representation of the array, delimited by the separator, quoted with the quotechar, 
     *         ending with the lineEnd char and sorted if sortValues is true
     */
    public static String toCSVString(String[] values, char separator, char quotechar, String lineEnd, boolean sortValues) {
        String stringValue;
        if(values == null) {
                stringValue = "";
        } else {
            try {
                String[] copy = new String[values.length];
                System.arraycopy(values, 0, copy, 0, values.length);
                if (sortValues) {
                    Arrays.sort(copy);
                }
                
                StringWriter writer = new StringWriter();
                CSVWriter csvWriter = new CSVWriter(writer, separator, quotechar, lineEnd);
                csvWriter.writeNext(copy);
                stringValue = writer.toString();
                
                csvWriter.close();
                writer.close();
            } catch(IOException ioe) {
                // This cannot happen
                throw new IOError(ioe);
            }
        }
        return stringValue;
    }
    
    public static boolean hasValue(String[] csvValues, String strToFind) {
        Arrays.sort(csvValues);
        return Arrays.binarySearch(csvValues, strToFind) >= 0;
    }
    
    public static boolean hasValue(String csv, String strToFind) {
        String[] strArray = fromCSVString(csv);
        return hasValue(strArray, strToFind);
    }
}
