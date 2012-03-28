if(bdrs === undefined) {
    window.bdrs = {};
}

if(bdrs.survey === undefined) {
    bdrs.survey = {};
}

bdrs.survey.listing = {};

bdrs.survey.listing.IMPORT_SURVEY_BUTTON_SELECTOR = '#import_survey_button';
bdrs.survey.listing.IMPORT_SURVEY_FILE_SELECTOR = '#import_survey_file';

/**
 *  Initialises the survey listing page.
 *  This function will attach handlers to the "Import Survey" button in order to trigger a file input dialog.
 */
bdrs.survey.listing.init = function() {
    // Submits the add report form when the file selection changes.
    jQuery(bdrs.survey.listing.IMPORT_SURVEY_FILE_SELECTOR).change(function(event) {
        var file_elem = jQuery(event.currentTarget);
        file_elem.parents('form').trigger('submit');
    });
    // Displays a file selection dialog when the button is clicked.
    jQuery(bdrs.survey.listing.IMPORT_SURVEY_BUTTON_SELECTOR).click(function() {
        jQuery(bdrs.survey.listing.IMPORT_SURVEY_FILE_SELECTOR).trigger('click');
    });
};