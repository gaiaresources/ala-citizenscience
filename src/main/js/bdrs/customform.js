if(bdrs === undefined) {
    window.bdrs = {};
}

if(bdrs.customform === undefined) {
    bdrs.customform = {};
}

bdrs.customform.listing = {};

bdrs.customform.listing.ADD_CUSTOM_FORM_BUTTON_SELECTOR = '#add_customform_button';
bdrs.customform.listing.ADD_CUSTOM_FORM_FILE_SELECTOR = '#add_customform_file';

bdrs.customform.listing.DELETE_CUSTOM_FORM_ACTION_SELECTOR = ".delete_customform";

bdrs.customform.listing.init = function() {

    // Submits the custom form when the file selection changes.
    jQuery(bdrs.customform.listing.ADD_CUSTOM_FORM_FILE_SELECTOR).change(function(event) {
        var file_elem = jQuery(event.currentTarget);
        file_elem.parents('form').trigger('submit');
    });
    // Displays a file selection dialog when the button is clicked.
    jQuery(bdrs.customform.listing.ADD_CUSTOM_FORM_BUTTON_SELECTOR).click(function() {
        jQuery(bdrs.customform.listing.ADD_CUSTOM_FORM_FILE_SELECTOR).trigger('click');
    });

    // Performs a form submission when the admin clicks the delete button
    jQuery(bdrs.customform.listing.DELETE_CUSTOM_FORM_ACTION_SELECTOR).click(function(event) {
        var confirm_delete = confirm("Are you sure you wish to delete this Custom Form?");
        if(confirm_delete) {
            jQuery(event.currentTarget).parents("form").submit();
        }
    });
};