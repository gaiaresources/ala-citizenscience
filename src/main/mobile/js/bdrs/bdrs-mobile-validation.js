
bdrs.mobile.validation = {};
bdrs.mobile.validation.POPUP_DELAY = 1200;
bdrs.mobile.validation.FADEOUT_DELAY = 400;

// The following is a template that all validators must implement.
//
//bdrs.mobile.validation.XXXXValidator = function() {
//    this.isValid = function(elem) {
//        return false;
//    };
//    
//    this.getHeader = function() {
//        return 'Example Header';
//    };
//    
//    this.getMessage = function() {
//        return '${label} Example Message';  
//    };
//
//    return this;
//};

bdrs.mobile.validation.IntegerValidator = function() {

    this.isValid = function(elem) {
        var val = elem.val();
        return (/^\-?\d*$/).test(val);
    };
    
    this.getHeader = function() {
        return 'Must be a number';
    };
    
    this.getMessage = function() {
        return '${label} must be a number';  
    };

    return this;
};

/**
 * Returns true if within range or empty
 */
bdrs.mobile.validation.IntegerWithRangeValidator = function() {
	
	this.isValid = function(elem) {
		
		this.getHeader = function() {
			return 'Invalid';
		};
		    
	    this.getMessage = function() {
	        return '${label} must be a number in the range of ' + elem.attr('min') + ' and ' + elem.attr('max');  
	    };
		
	    var minRange = parseInt(elem.attr('min'));
	    var maxRange = parseInt(elem.attr('max'));
	    var stringValue = elem.val();
	    var theValue;
	    if (stringValue === "") {
	    	return true;
	    } else if (isNaN(parseInt(stringValue))) {
	    	elem.val("");
	    	return true;
	    } else {
	    	 theValue = parseInt(elem.val());
	    	 return minRange <= theValue && theValue <= maxRange ? true : false;
	    }

	};
	
};

bdrs.mobile.validation.DecimalWithRangeValidator = function() {
	
	this.isValid = function(elem) {
		
		var rangeAttributes = elem.attr('range').split(' ');
		
		this.getHeader = function() {
			return 'Invalid';
		};
		    
	    this.getMessage = function() {
	        return '${label} must be a decimal in the range of ' + rangeAttributes[0] + ' and ' + rangeAttributes[1];  
	    };
		
	    var minRange = parseFloat(rangeAttributes[0]);
	    var maxRange = parseFloat(rangeAttributes[1]);
	    var theValue = parseFloat(elem.val());
	    
		return minRange <= theValue && theValue <= maxRange ? true : false;
		
	};
	
};


bdrs.mobile.validation.DecimalValidator = function() {

    this.isValid = function(elem) {
        var val = elem.val();
        return (/^\-?(\d*)(.?)(\d*)$/).test(val);
    };
    
    this.getHeader = function() {
        return 'Must be a decimal';
    };
    
    this.getMessage = function() {
        return '${label} must be a decimal';  
    };

    return this;
};

bdrs.mobile.validation.RequiredValidator = function() {
    this.isValid = function(elem) {
        var val = elem.val();
        return val.length > 0;
    };
    
    this.getHeader = function() {
        return 'Field is required';  
    };
    
    this.getMessage = function() {
        return '${label} is required.';
    };

    return this;
};

bdrs.mobile.validation.DateValidator = function() {
    this.isValid = function(elem) {
        var val = jQuery.trim(elem.val());
        if(val.length === 0) {
            // Blank is allowed.
            return true;
        }
    
        return bdrs.mobile.parseDate(val) !== null;
    };
    
    this.getHeader = function() {
        return 'Date is not valid';  
    };
    
    this.getMessage = function() {
        return '${label} must be a valid date of the form dd mmm yyyy.';
    };

    return this;
};

/**
 * Returns true if [-90,90] or if empty
 */
bdrs.mobile.validation.LatitudeValidator = function() {

    this.isValid = function(elem) {
        var val = elem.val();
        if (val === "") {
        	return true;
        } else if (isNaN(val)) {
        	elem.val("");
        	return true;
        } else {
            var value = parseFloat(val);
	    	return value !== NaN && value >= -90 && value <= 90;
        }
    };
    
    this.getHeader = function() {
        return 'Latitude is not valid';
    };
    
    this.getMessage = function() {
        return '${label} must be a decimal between -90 and +90';  
    };

    return this;
};

/**
 * Returns true if [-180,180;] or if empty
 */
bdrs.mobile.validation.LongitudeValidator = function() {

    this.isValid = function(elem) {
        var val = elem.val();
        if (val === "") {
        	return true;
        } else if (isNaN(val)) {
        	elem.val("");
        	return true;
        } else {
            var value = parseFloat(val);
	    	return value !== NaN && value >= -180 && value <= 180;
	    }
    };
    
    this.getHeader = function() {
        return 'Longitude is not valid';
    };
    
    this.getMessage = function() {
        return '${label} must be a decimal between -180 and +180';  
    };

    return this;
};

bdrs.mobile.validation.RegExpValidator = function() {
	var regExp ="";
	
	this.isValid = function(elem) {
		// blank is allowed unless required which will be caught by the required validator
		var val = jQuery.trim(elem.val());
        if(val.length === 0) {
            // Blank is allowed.
            return true;
        }
		this.regExp = elem.attr('regexp');
		var pattern = new RegExp(elem.attr('regexp'));
		return pattern.test(elem.val());
	};
	
    this.getHeader = function() {
        return 'Value does not match regExp';
    };
    
    this.getMessage = function() {
        return '${label} must match format ' + this.regExp;  
    };
    
    return this;
};

bdrs.mobile.validation.MultiCheckboxRequiredValidator = function() {

    this.isValid = function(elem) {
        return elem.parents("fieldset").find(":checked").length > 0;
    };

    this.getHeader = function() {
        return 'Field is required';  
    };

    this.getMessage = function() {
        return '${label} is required.';
    };

    return this;
};

// Validators
bdrs.mobile.validation.validators = {};
bdrs.mobile.validation.validators.required = new bdrs.mobile.validation.RequiredValidator();
bdrs.mobile.validation.validators.integer = new bdrs.mobile.validation.IntegerValidator();
bdrs.mobile.validation.validators.integerRange = new bdrs.mobile.validation.IntegerWithRangeValidator();
bdrs.mobile.validation.validators.decimal = new bdrs.mobile.validation.DecimalValidator();
bdrs.mobile.validation.validators.date = new bdrs.mobile.validation.DateValidator();
bdrs.mobile.validation.validators.latitude = new bdrs.mobile.validation.LatitudeValidator();
bdrs.mobile.validation.validators.longitude = new bdrs.mobile.validation.LongitudeValidator();
bdrs.mobile.validation.validators.regExp = new bdrs.mobile.validation.RegExpValidator();
bdrs.mobile.validation.validators.multiCheckboxRequired = new bdrs.mobile.validation.MultiCheckboxRequiredValidator();


/**
 * Retrieves the label for the specified input. Returns the label element if
 * one can be found, or null if no label can be located.
 * @param the input that for the label to be retrieved.
 */
bdrs.mobile.validation.getLabelForInput = function(inputElement) {
    if(inputElement.attr("type") === "checkbox") {
        var heading = inputElement.parents("fieldset").find("[role=heading]");
        return heading.length > 0 ? heading : null;
    } else {
        var elemId = inputElement.attr('id');
        var label = jQuery('label[for='+elemId+']');
        return label.length > 0 ? label : null;
    }
};
    
/**
 * Displays the validation error popup dialog using the specified validator
 * to provide the error header and error message content.
 * @param validator the validator that flagged the input as invalid.
 * @param inputElement the input that failed validation.
 */
bdrs.mobile.validation.showValidationErrorDialog = function(validator, inputElement) {

    var label = bdrs.mobile.validation.getLabelForInput(inputElement);
    var labelText = label !== null ? label.text() : '';

    var model = {};
    model.top = jQuery(window).scrollTop() + 100;
    model.header = validator.getHeader();
    model.message = jQuery.tmpl(validator.getMessage(), { label:labelText }).text();
    
    bdrs.template.renderCallback('validationError', model, jQuery.mobile.pageContainer, function() {
        var popup = jQuery('.validation-error-popup');
        popup.delay(bdrs.mobile.validation.POPUP_DELAY);
        popup.fadeOut(bdrs.mobile.validation.FADEOUT_DELAY, function() {
            jQuery(this).remove();
        });        
    });
};

/**
 * Validates the specified jQuery input element. This function returns true
 * if the input is valid, false otherwise. If the input is invalid, a 
 * validation error dialog shall be displayed.
 *
 * @param inputElement the input (input, select or textarea) to be validated.
 * @return true if the input is valid, false otherwise. 
 */
bdrs.mobile.validation.isValidInput = function(inputElement) {
    var classAttributes = inputElement.attr('class').split(' ');
	var validator;
	for(var i=0; i<classAttributes.length; i++) {
	    
	    validator = bdrs.mobile.validation.validators[classAttributes[i]];
	    if(validator !== undefined && !validator.isValid(inputElement)) {
	        
	        bdrs.mobile.validation.showValidationErrorDialog(validator, inputElement);
	        
	        return false;
	    }
	}
	
	return true;
};

/**
 * Validates the form specified by the jQuery formSelector. This function will
 * return true if the form is valid, false otherwise.
 * @param formSelector jQuery selector to the form to be validated.
 * @return true if the form is valid, false otherwise.
 */
bdrs.mobile.validation.isValidForm = function(formSelector) {
    var formElem = jQuery(formSelector);
    var inputElems = formElem.find('input.validate, textarea.validate, select.validate');
    for(var i=0; i<inputElems.length; i++) {
        if(!bdrs.mobile.validation.isValidInput(jQuery(inputElems[i]))) {
            return false;
        }
    }
    return true;
};