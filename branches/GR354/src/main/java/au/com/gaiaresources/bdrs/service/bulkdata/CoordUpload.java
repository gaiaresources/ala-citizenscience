package au.com.gaiaresources.bdrs.service.bulkdata;

public class CoordUpload extends RowUpload {

    private double latitude;
    private double longitude;
    private String epsg;
    
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getEpsg() {
		return epsg;
	}

	public void setEpsg(String epsg) {
		this.epsg = epsg;
	}
}
