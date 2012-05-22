// --------------------------------------
// Users
// --------------------------------------
bdrs.user = {};

/**
 * Returns the representation of this users full name plus username in the form,
 *
 * LastName, FirstName (Username) - if both the first name and last name is not
 * null and non blank otherwise, just the username.
 *
 * @param user
 *            the javascript object representation of a User provided by the
 *            UserService (webservice).
 * @return the representation of the users full name plus username. If the
 *            object does not provide a username, this function will return
 *            null.
 */
bdrs.user.getLastFirstUsername = function(user) {
    if(user.name === undefined) {
        return null;
    }

    var firstName = user.firstName === undefined ? "" : user.firstName;
    var lastName = user.lastName === undefined ? "" : user.lastName;
    var name = user.name === undefined ? "" : user.name;

    var parts = [];
    if(firstName !== null && firstName.length > 0 &&
            lastName !== null && lastName.length > 0) {

        parts.push(lastName+',');
        parts.push(firstName);
        parts.push('('+name+')');
    } else {
        parts.push(name);
    }

    return parts.join(' ');
};

/**
 * Creates a new user location with the specified parameters.
 * @param locationName {string} the name of the location
 * @param latitude {float} the latitude in degrees
 * @param longitude {float} the longitude in degrees
 * @param isDefault {boolean} true if the location should be set as the 
 * default location, false otherwise
 * @param callback {function} a callback function that receives a flattened
 * location object sent by the server in JSON.
 */
bdrs.user.bookmarkUserLocation = function(locationName, latitude, longitude, isDefault, callback) {
    var url = bdrs.portalContextPath + "/webservice/location/bookmarkUserLocation.htm";
    var params = {
        ident : bdrs.ident,
        locationName : locationName,
        latitude : latitude,
        longitude : longitude,
        isDefault : isDefault ? "true" : "false"
    };
    jQuery.getJSON(url, params, callback);
};

bdrs.user.listing = {};

bdrs.user.listing.IMPORT_USERS_BUTTON_SELECTOR = '#import_users_button';
bdrs.user.listing.IMPORT_USERS_FILE_SELECTOR = '#import_users_file';

/**
 *  Initialises the user listing page.
 *  This function will attach handlers to the "Import Users" button in order to trigger a file input dialog.
 */
bdrs.user.listing.init = function() {
    // Submits the add report form when the file selection changes.
    jQuery(bdrs.user.listing.IMPORT_USERS_FILE_SELECTOR).change(function(event) {
        var file_elem = jQuery(event.currentTarget);
        file_elem.parents('form').trigger('submit');
    });
    // Displays a file selection dialog when the button is clicked.
    jQuery(bdrs.user.listing.IMPORT_USERS_BUTTON_SELECTOR).click(function() {
        jQuery(bdrs.user.listing.IMPORT_USERS_FILE_SELECTOR).trigger('click');
    });
};