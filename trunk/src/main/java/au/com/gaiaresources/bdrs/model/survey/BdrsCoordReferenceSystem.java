package au.com.gaiaresources.bdrs.model.survey;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.com.gaiaresources.bdrs.json.JSONEnum;
import au.com.gaiaresources.bdrs.json.JSONEnumUtil;
import au.com.gaiaresources.bdrs.util.StringUtils;

/**
 * CRS enum including hints on how to render a coordinate widget
 */
public enum BdrsCoordReferenceSystem implements JSONEnum {

	WGS84(4326, false, "WGS 84, Lat/Lon", "Latitude", "Longitude", "", -180d, -90d, 180d, 90d, 6),
	MGA50(28350, true, "GDA94 Zone 50", "Northings", "Eastings", "50", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA51(28351, true, "GDA94 Zone 51", "Northings", "Eastings", "51", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA52(28352, true, "GDA94 Zone 52", "Northings", "Eastings", "52", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA53(28353, true, "GDA94 Zone 53", "Northings", "Eastings", "53", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA54(28354, true, "GDA94 Zone 54", "Northings", "Eastings", "54", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA55(28355, true, "GDA94 Zone 55", "Northings", "Eastings", "55", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA56(28356, true, "GDA94 Zone 56", "Northings", "Eastings", "56", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA57(28357, true, "GDA94 Zone 57", "Northings", "Eastings", "57", 100000d, 1116915d, 999999d, 9999999d, 3),
	MGA58(28358, true, "GDA94 Zone 58", "Northings", "Eastings", "58", 100000d, 1116915d, 999999d, 9999999d, 3),
	// MGA, zone not defined. Use -1 for all coordinate reference system 'groups'.
	MGA(-1, true,"GDA94", "Northings", "Eastings", "", 100000d, 1116915d, 999999d, 9999999d, 3,
			MGA50, MGA51, MGA52, MGA53, MGA54, MGA55, MGA56, MGA57, MGA58);

	public static final int DEFAULT_SRID = 4326;
	
	private static final int NO_SPECIFIED_ZONE = -1;	

	private String displayName;
	private String zoneName;
	private String yName;
	private String xName;
	private List<BdrsCoordReferenceSystem> zones = Collections.EMPTY_LIST;
	private int srid;
	private Double minX;
	private Double minY;
	private Double maxX;
	private Double maxY;
	private int truncateDecimalPlaces;
	private boolean xfirst;

	/**
	 * Private constructor.
	 * 
	 * @param srid SRID code.
	 * @param xfirst x coordinate is shown first
	 * @param displayName Name to be displayed in user interfaces.
	 * @param yName name of the y coordinate.
	 * @param xName name of the x coordinate.
	 * @param zoneName name of the zone.
	 * @param minX minimum x value, can be null.
	 * @param minY minimum y value, can be null.
	 * @param maxX maximum x value, can be null.
	 * @param maxY maximum y value, can be null.
	 * @param dp decimal places to round to when displaying to user.
	 * @param zones list of children BdrsCoordReferenceSystem enums
	 */
	private BdrsCoordReferenceSystem(int srid, boolean xfirst, String displayName, String yName, String xName, String zoneName, 
			Double minX, Double minY, Double maxX, Double maxY, int dp,
			BdrsCoordReferenceSystem... zones) {
		
		this.srid = srid;
		this.displayName = displayName;
		this.yName = yName;
		this.xName = xName;
		this.zoneName =  zoneName;
		
		this.xfirst = xfirst;
		
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		
		this.truncateDecimalPlaces = dp;
		
		if (zones != null) {
			this.zones = new ArrayList<BdrsCoordReferenceSystem>(zones.length);
			for (BdrsCoordReferenceSystem z : zones) {
				this.zones.add(z);
			}
		}
	}
	
	/**
	 * Get the display name for this CRS.
	 * @return Display name for this CRS.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Get the name for the y coordinate.
	 * @return Name for the y coordinate.
	 */
	public String getYname() {
		return yName;
	}

	/**
	 * Get the name for the x coordinate.
	 * @return Name for the x coordinate.
	 */
	public String getXname() {
		return xName;
	}

	/**
	 * Get the list of valid zones. E.g., MGA50 is a valid zone
	 * for MGA.
	 * @return List of valid zones.
	 */
	public List<BdrsCoordReferenceSystem> getZones() {
		return zones;
	}

	/**
	 * Get the name of the zone.
	 * @return Name of the zone.
	 */
	public String getZoneName() {
		return zoneName;
	}

	/**
	 * Get the SRID for this CRS.
	 * @return SRID for this CRS.
	 */
	public int getSrid() {
		return srid;
	}
	
	/**
	 * Get the minimum valid X value, can be null.
	 * @return The minimum valid X value.
	 */
	public Double getMinX() {
		return minX;
	}

	/**
	 * Get the minimum valid Y value, can be null.
	 * @return The minimum valid Y value.
	 */
	public Double getMinY() {
		return minY;
	}

	/**
	 * Get the maximum valid X value, can be null.
	 * @return The maximum valid X value.
	 */
	public Double getMaxX() {
		return maxX;
	}

	/**
	 * Get the maximum valid Y value, can be null.
	 * @return The maximum valid Y value.
	 */
	public Double getMaxY() {
		return maxY;
	}
	
	/**
	 * Do we need a more specific zone required. E.g. MGA isn't enough
	 * information, at some point we need the actual zone number specified.
	 * @return true if we need more zone information.
	 */
	public boolean isZoneRequired() {
		return this.srid == NO_SPECIFIED_ZONE;
	}
	
	/**
	 * Get a bdrs  supported CRS for a given SRID. If the SRID
	 * is not supported null will be returned.
	 * @param srid SRID to search.
	 * @return CRS object or null if not found.
	 */
	public static BdrsCoordReferenceSystem getBySRID(int srid) {
		for (BdrsCoordReferenceSystem crs : BdrsCoordReferenceSystem.values()) {
			if (crs.getSrid() == srid) {
				return crs;
			}
		}
		return null;
	}
	
	/**
	 * Get a bdrs supported CRS for a given text string. Will first attempt to
	 * parse the text by SRID (i.e. a number), then by display name, then by
	 * epsg code where the epsg code is the SRID prepended by 'epsg:'
	 * @param text Text to examine.
	 * @return CRS object or null if not found.
	 */
	public static BdrsCoordReferenceSystem getByText(String text) {
		if (StringUtils.notEmpty(text)) {
			text = text.trim();
			// try to parse as srid.
			try {
				// parsing to a double as in a spreadsheet '4326' gets turned into
				// 4326.0. As decimal points are not valid in srids anyway this shouldn't
				// pose a problem. If it becomes a problem feel free to make sure that the
				// decimal part is '0'.
				int srid = Double.valueOf(text).intValue();
				for (BdrsCoordReferenceSystem crs : BdrsCoordReferenceSystem.values()) {
					if (crs.getSrid() == srid) {
						return crs;
					}
				}	
			} catch (NumberFormatException nfe) {
			}
			// try to parse as display name (case insensitive).
			for (BdrsCoordReferenceSystem crs : BdrsCoordReferenceSystem.values()) {
				if (crs.getDisplayName().equalsIgnoreCase(text)) {
					return crs;
				}
			}
			// try to parse as epsg code.
			for (BdrsCoordReferenceSystem crs : BdrsCoordReferenceSystem.values()) {
				if (sridToEpsg(crs.getSrid()).equalsIgnoreCase(text)) {
					return crs;
				}
			}
			return null;
		} else {
			return null;
		}
	}
	
	/**
	 * Get the EPSG code
	 * @return EPSG code
	 */
	public String getEpsgCode() {
		return sridToEpsg(srid);
	}
	
	/**
	 * Convert an SRID to an EPSG code
	 * @param srid srid to convert
	 * @return EPSG code
	 */
	public static String sridToEpsg(int srid) {
		return String.format("EPSG:%d", srid);
	}

	/**
	 * Number of decimal places to truncate to.
	 * @return Number of decimal places to truncate to.
	 */
	public int getTruncateDecimalPlaces() {
		return truncateDecimalPlaces;
	}
	
	/**
	 * Should the x coordinate come before the y coordinate?
	 * @return true if x comes before y
	 */
	public boolean isXfirst() {
	    return xfirst;
	}

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONEnumUtil.writeJSONString(out, this);
    }

    @Override
    public String toJSONString() {
        return JSONEnumUtil.toJSONString(this);
    }
}
