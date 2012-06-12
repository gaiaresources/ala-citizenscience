package au.com.gaiaresources.bdrs.model.map;

public enum MapOwner {
	NONE,  // map is a stand alone map.
	REVIEW, // map is for the review screens.
	SURVEY // map is for a survey. survey member of GeoMap should be non null.
}
