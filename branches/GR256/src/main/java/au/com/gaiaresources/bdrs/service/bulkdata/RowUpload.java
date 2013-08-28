package au.com.gaiaresources.bdrs.service.bulkdata;

public class RowUpload {

    private boolean error = false;
    private String errorMessage;
    
    private Integer rowNumber;
    private Integer colNumber;
    private String sheetName;
    
	public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isError() {
        return error;
    }
	
    private void setError(boolean error) {
    	this.error = error;
    }

    public void setErrorMessage(String errorMessage) {
    	this.setError(true);
        this.errorMessage = errorMessage;
    }
    
    public void setErrorMessage(String sheetName, String colNum, int rowNum, String value, String className, String msg) {
    	this.setError(true);
    	this.errorMessage = String.format("Cell %s!%s%d[ value=\"%s\" ]: %s %s", sheetName, colNum, rowNum, value, className, msg);
    }
    
    public void setErrorMessage(String value, String msg) {
    	this.setError(true);
    	this.errorMessage = String.format("Cell %s!row:%d[ value=\"%s\" ]: %s", sheetName, rowNumber, value, msg);
    }
    
    public Integer getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(Integer val) {
        rowNumber = val;
    }
    
    public Integer getColNumber() {
        return colNumber;
    }
    
    public void setColNumber(Integer val) {
        colNumber = val;
    }

	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}
}
