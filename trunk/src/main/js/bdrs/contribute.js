//--------------------------------------
// Contribute
//--------------------------------------
bdrs.contribute = {};

// ---------
// Constants
// ---------
/**
 * CSS class for species attribute inputs
 */
bdrs.contribute.SPECIES_ATTRIBUTE_SELECTOR = 'input.species_attribute';
/**
 * CSS class for species attribute id inputs
 */
bdrs.contribute.SPECIES_ATTRIBUTE_ID_CLASS_SELECTOR = ".species_attribute_id";
/**
 * Key used in order to jquery.data the species to the species attribute input.
 */
bdrs.contribute.SPECIES_VALUE_DATA_KEY = "speciesValue";

//--------------------------------------
// Yearly Sightings
//--------------------------------------
bdrs.contribute.yearlysightings = {};

/**
 * Initialise the yearly sightings page
 */
bdrs.contribute.yearlysightings.init = function() {
    var form = jQuery('form');
    form.submit(bdrs.contribute.yearlysightings.submitHandler);

    var locationSelect = jQuery("#location");
    locationSelect.change(bdrs.contribute.yearlysightings.locationSelected);

    var sightingCells = jQuery(".sightingCell");
    sightingCells.change(bdrs.contribute.yearlysightings.validateCellChange);
    sightingCells.blur(bdrs.contribute.yearlysightings.validateCellChange);
};

/**
 * Handler for form submit - blocks if any of the yearly sighting table cells
 * are in error
 * 
 * @param {Object} event
 */
bdrs.contribute.yearlysightings.submitHandler = function(event) {
    var form = jQuery(event.currentTarget);
    return form.find(".errorCell").length === 0;
};

/**
 * Handler for yearly sighting table cell 'on change'. Marks cell as 
 * being in error which will then block the form submitting.
 * 
 * @param {Object} event
 */
bdrs.contribute.yearlysightings.validateCellChange = function(event) {
    var inp = jQuery(event.currentTarget);
    var cell = inp.parent("td");
    
    var isValid = true;    
    
    if (inp.val().length > 0) {
        if(/^\d+$/.test(inp.val())) {
            isValid = parseInt(inp.val(),10) < 1000000;
        } else {
            isValid = false;
        }
    }
    
    if(isValid) {
        cell.removeClass("errorCell");
        var date = new Date(parseInt(inp.attr("name").split("_")[1], 10));
        inp.attr("title", bdrs.util.formatDate(date));
    } else {
        cell.addClass("errorCell");
        inp.attr("title", "Must be a positive integer or blank");
    }
};

/**
 * Event handler for inserting a record attribute
 * @param {Object} recAttr - record attribute to insert
 */
bdrs.contribute.yearlysightings.insertRecordAttribute = function(recAttr) {
    var attrId = recAttr.attribute.id;
    var attrType = bdrs.model.taxa.attributeType.code[recAttr.attribute.typeCode];
    var inp = jQuery("[name=attribute_"+attrId+"]");
    
    if (bdrs.model.taxa.attributeType.SPECIES === attrType) {
        // do nothing.
    } else if (bdrs.model.taxa.attributeType.SINGLE_CHECKBOX === attrType) {
        if (recAttr.booleanValue === true) {
            inp.prop("checked", true);
        }
    } else if (bdrs.model.taxa.attributeType.MULTI_CHECKBOX === attrType) {
        var valueArray = recAttr.multiCheckboxValue;
        if (valueArray && jQuery.isArray(valueArray)) {
            // mark each selected checkbox...as being selected!
            jQuery.each(valueArray, function(index, elem) {
                inp.filter("[value="+elem+"]").prop("checked", true);
            });
        }
    } else if (bdrs.model.taxa.attributeType.MULTI_SELECT === attrType) {
        var valueArray = recAttr.multiSelectValue;
        if (valueArray && jQuery.isArray(valueArray)) {
            // mark each selected checkbox...as being selected!
            jQuery.each(valueArray, function(index, elem) {
                inp.children("[value="+elem+"]").prop("selected", true);
            });
        }
    } else {
        inp.val(recAttr.stringValue);
        
        // Repopulate files
        var fileInput = jQuery("#attribute_file_" + attrId);
        if (fileInput.length > 0) {
            var fileUrl = bdrs.portalContextPath + "/files/download.htm?" + recAttr.fileURL;
            if (fileInput.hasClass("image_file")) {
                // Images
                var img = jQuery("<img/>");
                img.attr({
                    width: 250,
                    src: fileUrl,
                    alt: "Missing Image"
                });
                
                var imgAnchor = jQuery("<a></a>");
                imgAnchor.attr("href", fileUrl);
                
                var imgContainer = jQuery("<div></div>");
                imgContainer.attr("id", "attribute_img_" + attrId);
                
                imgContainer.append(imgAnchor);
                imgAnchor.append(img);
                
                inp.parent().before(imgContainer);
            }
            else if (fileInput.hasClass("data_file")) {
                // Data
                var dataAnchor = jQuery("<a></a>");
                dataAnchor.attr("href", fileUrl);
                dataAnchor.text(recAttr.stringValue);
                
                var dataContainer = jQuery("<div></div>");
                dataContainer.attr("id", "attribute_data_" + attrId);
                
                dataContainer.append(dataAnchor);
                inp.parent().before(dataContainer);
            }
        } // End file repopulation
    }
};

/**
 * Event handler
 * @param {Object} event - event to be handled
 */
bdrs.contribute.yearlysightings.locationSelected = function(event) {

    var selectLocation = jQuery(event.currentTarget);
    var location = jQuery("[name=locationId]");

    var ans;
    if(location.val().length > 0) {
        ans = confirm("Changing the location will replace the data below. Do you wish to proceed?");
    }
    else {
        ans = true;
    }

    if(ans) {
        location.val(selectLocation.val());

        var locationId = selectLocation.val();
        var surveyId = jQuery('#surveyId').val();
        var ident = jQuery('#ident').val();

        bdrs.contribute.yearlysightings.loadCellData(locationId, surveyId, ident, true);
    } // End confirm dialog return check
    else {
        selectLocation.val(location.val());
    }
};

/**
 * Loads the cells in the yearly sightings form. Each cell represents a different
 * day in the year
 * 
 * @param {Object} locationId - the location id
 * @param {Object} surveyId - the survey id
 * @param {Object} ident - the ident of the user
 * @param {Object} editable - whether the form is editable or not
 */
bdrs.contribute.yearlysightings.loadCellData = function(locationId, surveyId, ident, editable) {
    // Clear all cells
    jQuery(".sightingCell").each(function(index, element){
        var inp = jQuery(element);
        var cell = inp.parents("td");
        inp.val('');
        cell.removeClass("errorCell");
        var date = new Date(parseInt(inp.attr("name").split("_")[1],10));
        inp.attr("title", bdrs.util.formatDate(date));
    });
    
    // Clear the survey scope attributes - reset state of form!
    // don't remove the value of checkbox types!
    // Leave species attribute values. The current implementation means they are automatically
    // populated with the species for the current yearly sightings survey. 
    // This makes the species attribute useless for the yearly sightings but we would need
    // to implement a way to indicate what species to allow for species attribute types.
    jQuery("[name^=attribute_]")
        .not(bdrs.contribute.SPECIES_ATTRIBUTE_SELECTOR)
        .not(bdrs.contribute.SPECIES_ATTRIBUTE_ID_CLASS_SELECTOR)
        .not('input[type=checkbox]').val('');
    
    // remove the checked attribute of checkbox types
    jQuery("[name^=attribute_]").filter('input[type=checkbox]').prop('checked', false);
    // remove the selected attribute of selection types
    jQuery("[name^=attribute_]").children('option').prop('selected', false);
    // remove all file values
    jQuery("[id^=attribute_img_], [id^=attribute_data_]").remove();

    if(locationId) {
        var param = {
            locationId: locationId,
            surveyId: surveyId,
            ident: ident
        };
        jQuery.getJSON(bdrs.portalContextPath+'/webservice/record/getRecordsForLocation.htm', param, function(data) {
            var rec;
            for(var i=0; i<data.length; i++) {
                rec = data[i];
                if(rec.number !== null) {
                    if (editable) {
                       jQuery("[name=date_"+rec.when+"]").val(rec.number);    
                    } else {
                       jQuery("#date_"+rec.when).text(rec.number);
                    }
                }
            }
            
            if(typeof(rec) !== 'undefined') {
                // Use the last survey as the prototype to load the 
                // Survey scoped attributes
                for(var j=0; j<rec.attributes.length; j++) {
                    var param = {
                        recordAttributeId: rec.attributes[j],
                        ident: jQuery('#ident').val()
                    };
                    jQuery.getJSON(bdrs.portalContextPath+"/webservice/record/getRecordAttributeById.htm", param, bdrs.contribute.yearlysightings.insertRecordAttribute);
                } // End for-loop request for survey scope attributes
            }
        });
    } // End location update
}

// End of Yearly Sightings -------------
// -------------------------------------

//--------------------------------------
// Single Site Multiple Taxa
//--------------------------------------
bdrs.contribute.singleSiteMultiTaxa = {};

/**
 * @param {Object} sightingIndexSelector - on the form page there is a hidden input, aka the sighting index, that shows
 * how many rows need to be saved. This is the jquery selector for the sighting index element.
 * @param {Object} surveyIdSelector - on the form there is a hidden input for the survey id, this is the selector for
 * the survey id element.
 * @param {Object} sightingTableBodyy - jQuery selector for the sighting table body element
 * @param {Object} speciesRequired - a boolean, sets validation on the species field.
 * @param {Object} numberRequired - a boolean, sets validation on the number field.
 */
bdrs.contribute.singleSiteMultiTaxa.addSighting = function(sightingIndexSelector, surveyIdSelector, sightingTableBody, speciesRequired, numberRequired, showScientificName) {
    var sightingIndexElem = jQuery(sightingIndexSelector);
    var sightingIndex = parseInt(sightingIndexElem.val(), 10);
    sightingIndexElem.val(sightingIndex+1);

    var surveyId = jQuery(surveyIdSelector).val();
    
    var url = bdrs.portalContextPath+"/bdrs/user/singleSiteMultiTaxa/sightingRow.htm";
    var param = {
        sightingIndex: sightingIndex,
        surveyId: surveyId
    };
    jQuery.get(url, param, function(data) {
        var newRow = jQuery(data);
        jQuery(sightingTableBody).append(newRow);
        
        bdrs.contribute.singleSiteMultiTaxa.attachRowControls(sightingIndex, surveyId, speciesRequired, numberRequired, showScientificName);
        newRow.ketchup();
    });
};

/**
 * Helper function for attaching row controls
 * 
 * @param {Object} node - the dom node to search for inputs to attach row controls.
 * @param {Object} sightingIndex - the sighting index of the row to attach the controls on
 * @param {Object} surveyId - the survey id
 * @param {Object} speciesRequired - is the species field mandatory ?
 * @param {Object} numberRequired - is the number field mandatory ?
 * @param {Object} showScientificName - when true will show the scientific name, else will use the common name
 */
bdrs.contribute.singleSiteMultiTaxa.attachRowControls = function(sightingIndex, surveyId, speciesRequired, numberRequired, showScientificName) {

    // Attach the autocomplete
    var search_elem = jQuery("[name="+sightingIndex+"_survey_species_search]");
    search_elem.data("surveyId", surveyId); 
    search_elem.autocomplete({
        source: bdrs.contribute.getAutocompleteSourceFcn(showScientificName),
        select: function(event, ui) {
            var taxon = ui.item.data;
            jQuery("[name="+sightingIndex+"_species]").val(taxon.id).blur();
        },
        html: true,
        minLength: 2,
        delay: 300
    });
    
    // Attach the datepickers
    bdrs.initDatePicker();
    // attach the validation
    var species_elem = jQuery("[name="+sightingIndex+"_species]");
    if (speciesRequired) {
        species_elem.addClass("validate(required)");
    }
    
    var count_elem = jQuery("[name="+sightingIndex+"_number]");
    if (numberRequired) {
        count_elem.addClass("validate(positiveIntegerLessThanOneMillion)");
    } else {
        count_elem.addClass("validate(positiveIntegerLessThanOneMillionOrBlank)");
    }
    
    var row = jQuery("#"+sightingIndex+"_sightingIndexRow");

    search_elem.parents("tr").ketchup();
}

//--------------------------------------
// End Single Site Multiple Taxa -------
// -------------------------------------
//--------------------------------------
// Single Site All Taxa
//--------------------------------------
bdrs.contribute.singleSiteAllTaxa = {};

/**
 * Add a row to the sightings table
 * 
 * @param {Object} sightingIndexSelector - for the input which has the number of existing rows in the sightings table
 * @param {Object} surveyIdSelector - for the input that holds the survey id
 * @param {Object} sightingTableBody - the table body for the sightings table
 */
bdrs.contribute.singleSiteAllTaxa.addSighting = function(sightingIndexSelector, surveyIdSelector, sightingTableBody) {
    var sightingIndexElem = jQuery(sightingIndexSelector);
  var sightingIndex = parseInt(sightingIndexElem.val(), 10);
  sightingIndexElem.val(sightingIndex+1);
  
  var surveyId = jQuery(surveyIdSelector).val();
  
  var url = bdrs.portalContextPath+"/bdrs/user/singleSiteAllTaxa/sightingTableAllTaxa.htm";
  var param = {
      sightingIndex: sightingIndex,
      surveyId: surveyId
  };
 
  jQuery.ajax(url, {
      data: param, 
      success: function(data) {
          jQuery(sightingTableBody).append(data);
          
          // Attach the datepickers
          bdrs.initDatePicker();
        },
        async: false
  });

  jQuery(sightingTableBody).ketchup();
  
  // update the sightingIndex field to match the attribute count
  sightingIndexElem.val(jQuery(sightingTableBody).find('tr').length);
};

// ---------------------------
// Autocomplete initialisation
// ---------------------------

/**
 * Will initialise the auto complete events, see parameters below
 *
 * Initialises auto complete for the PRIMARY species
 *
 * required parameters:
 * surveySpeciesSearchSelector - selector for the text input used to search for the species, autocomplete will be applied to this input
 * speciesIdSelector - the selector for the species ID field. This field is normally hidden
 * taxonAttrRowSelector - the selector for the taxon attr rows (i.e. will probably be a wildcard)
 * surveyId - the survey id
 * recordId - the record id
 * editable - whether the field is editable or not
 * attributeTbodySelector - selector for the table where we will append our taxon group attributes
 * showScientificName - boolean. if true, uses the scientific name for auto complete, else uses the common name
 * 
 * Species auto complete
 * 
 * @param {Object} args - mandatory arguments to do initialisation of species autocomplete. see notes above...
 */
bdrs.contribute.initSpeciesAutocomplete = function(args) {
    
    var surveySpeciesSearchSelector = args.surveySpeciesSearchSelector;
    var speciesIdSelector = args.speciesIdSelector;
    var taxonAttrRowSelector = args.taxonAttrRowSelector;
    var surveyId = args.surveyId;
    var recordId = args.recordId;
    var editable = args.editable;
    var attributeTbodySelector = args.attributeTbodySelector;
    var showScientificName = args.showScientificName;
    
    var addTaxonTableFunc = function(taxon) {

        // Load Taxon Group Attributes
        // Clear the group attribute rows
        if(taxonAttrRowSelector !== undefined && taxonAttrRowSelector !== null) {
            jQuery(taxonAttrRowSelector).parents("tr").remove();
        }
        
        // Make a note of which attribute ids are currently on the form so we don't 
        // double up. The concrete use case is:
        // 1. Record has a field name.
        // 2. Assign a standard species.
        // 3. Edit the record again.
        // 4. Select field species in the species autocomplete.
        // you will have 2 field species items as the web service does not the current
        // state of the web form.
        var attrIds = [];
        var name;
        var id;
        jQuery('[name*="attribute_"]').each(function(index, element) {
            name = $(this).attr("name");
            while (name.indexOf("attribute_") > -1) {
                name = name.substr(name.indexOf("attribute_") + "attribute_".length, name.length - name.indexOf("attribute_") + "attribute_".length);
                // get the first number in the string...
                id = parseInt(name);
                if (!isNaN(id)) {
                    attrIds.push(id);
                }
            }
        });
        
        // Build GET request parameters
        var params = {};
        params.surveyId = surveyId;
        params.taxonId = taxon.id;
        if (recordId) {
            params.recordId = recordId;
        }
        params.editForm = editable;
        params.attrIds = attrIds;

        // Issue Request
        if(attributeTbodySelector !== null && attributeTbodySelector !== undefined) {
            jQuery.get(bdrs.portalContextPath+"/bdrs/user/ajaxTrackerTaxonAttributeTable.htm", jQuery.param(params, true), function(data) {
                var node = jQuery(attributeTbodySelector);
                // append to the main form table, not a sub table
                node.first().append(data);
            });
        }
    };
    
    var onChangeFunc = function(event, ui) {
        if(jQuery(event.target).val().length === 0) {
            jQuery(speciesIdSelector).val("").trigger("blur");
        
            // Clear the group attribute rows
            if(taxonAttrRowSelector !== undefined && taxonAttrRowSelector !== null) {
                jQuery(taxonAttrRowSelector).closest("tr").remove();
            }
        }
    };
    
    bdrs.contribute.initSpeciesAttributeAutocomplete(surveySpeciesSearchSelector, speciesIdSelector, surveyId, showScientificName,
        addTaxonTableFunc, onChangeFunc);
};

/**
 * Initialise the auto complete for species attribute fields.
 * @param {String} selector for taxon name input.
 * @param {String} selector for the taxon id hidden input.
 * @param {int} surveyId The survey id.
 * @param {boolean} showScientificName Shows scientific name if true, otherwise common name.
 * @param {Function} selectCallback callback to run when the taxon id field is populated.
 * @param {Function} changeCallback callback to run when the autocomplete fires its change event.
 */
bdrs.contribute.initSpeciesAttributeAutocomplete = function(taxonNameInputSelector, 
    taxonIdInputSelector, surveyId, showScientificName, selectCallback, changeCallback) {
    // Attach the autocomplete
    
    var speciesNameElem = jQuery(taxonNameInputSelector);
    var speciesIdElem = jQuery(taxonIdInputSelector);
    
    if (surveyId === null || surveyId === undefined) {
        surveyId = 0;
    }
    
    speciesNameElem.data("surveyId", surveyId);
    
    speciesNameElem.keydown(function(event, ui) {
        speciesNameElem.data(bdrs.contribute.SPECIES_VALUE_DATA_KEY, speciesNameElem.val());
    });
    speciesNameElem.keyup(function(event,ui){
        // don't clear the id when hitting the arrow keys which are key codes 37 to 40 inclusive.
        if (typeof event.keyCode !== 'undefined' && event.keyCode !== 37 && event.keyCode !== 38 
            && event.keyCode !== 39 && event.keyCode !== 40) {
            if (speciesNameElem.data(bdrs.contribute.SPECIES_VALUE_DATA_KEY) !== speciesNameElem.val()) {
                speciesIdElem.val("");
            }
        }
    });
    
    var assignIdFunc = function(taxon) {
        speciesIdElem.val(taxon.id).trigger("blur");
        if (jQuery.isFunction(selectCallback)) {
            selectCallback(taxon);
        }
    };
    
    speciesNameElem.autocomplete({
        autoSelect: false,
        source: bdrs.contribute.getAutocompleteSourceFcn(showScientificName),
        focus: function (event, ui) {
            // only set the id field on the focus event when using up/down arrows to focus
            // menu items.
            if (typeof event.keyCode !== 'undefined' && event.keyCode !== 0) {
                var taxon = ui.item.data;
                assignIdFunc(taxon);
            }
        },
        select: function(event, ui) {
            var taxon = ui.item.data;
            assignIdFunc(taxon);
        },
        change: jQuery.isFunction(changeCallback) ? changeCallback : undefined,
        html: true,
        minLength: 2,
        delay: 300
    });
};


/**
 * Get the function to use as the parameter for the jQuery autocomplete 'source'
 * parameter
 * 
 * @param {Object} showScientificName - boolean, when true will use the scientific name
 * for the autocomplete value. Else will use the common name
 */
bdrs.contribute.getAutocompleteSourceFcn = function(showScientificName) {
    var fcn = function(request, callback) {
        var params = {};
        params.q = request.term;
        params.surveyId = jQuery(this.element).data("surveyId");
        jQuery.getJSON(bdrs.portalContextPath + '/webservice/survey/speciesForSurvey.htm', params, function(data, textStatus){
            var label;
            var result;
            var taxon;
            var resultsArray = [];
            for (var i = 0; i < data.length; i++) {
                taxon = data[i];
                
                label = [];
                if (taxon.scientificName !== undefined && taxon.scientificName.length > 0) {
                    label.push("<b><i>" + taxon.scientificName + "</b></i>");
                }
                if (taxon.commonName !== undefined && taxon.commonName.length > 0) {
                    label.push(taxon.commonName);
                }
                
                label = label.join(' ');
                
                resultsArray.push({
                    label: label,
                    value: showScientificName ? taxon.scientificName : taxon.commonName,
                    data: taxon
                });
            }
            
            callback(resultsArray);
        });
    }
    return fcn;
};

/**
 * @param {Object} rowIndexSelector - on the form page there is a hidden input, aka the row index, that shows
 * how many rows need to be saved. This is the jquery selector for the row index element.
 * @param {Object} surveyIdSelector - on the form there is a hidden input for the survey id, this is the selector for
 * the survey id element.
 * @param {Object} sightingTableBodyy - jQuery selector for the sighting table body element
 * @param {Object} speciesRequired - a boolean, sets validation on the species field.
 * @param {Object} numberRequired - a boolean, sets validation on the number field.
 */
bdrs.contribute.addAttributeRecordRow = function(rowIndexSelector, surveyIdSelector, tableBody, attributeId, censusMethodSelector, speciesSelector, speciesRequired, numberRequired, showScientificName) {
    var rowIndexElem = jQuery(rowIndexSelector);
    var rowIndex = parseInt(rowIndexElem.val(), 10);
    if (!rowIndex) {
        rowIndex = 0;
    }
    rowIndexElem.val(rowIndex+1);
    
    var surveyId = jQuery(surveyIdSelector).val();
    var speciesIdElem = jQuery(speciesSelector);
    var speciesId = speciesIdElem ? speciesIdElem.val() : 0;
    
    var url = bdrs.portalContextPath+"/bdrs/user/contribute/attributeRecordRow.htm";
    var param = {
        rowIndex: rowIndex,
        surveyId: surveyId,
        attributeId: attributeId,
        speciesId: speciesId
    };
    
    var table = jQuery(tableBody).first();
    var prefix = table.parentsUntil("div").parent().parent().attr("name");
    if (prefix !== '') {
        param['prefix'] = prefix;
    }
    
    var cmIds = new Array();
    // get the attribute id stack for making sure each census method is only added once per attribute stack
    jQuery(table).parents("table.censusMethodAttributeTable").siblings(censusMethodSelector).each(function(i, parent) {
        var element = jQuery(parent);
        if (element !== undefined && jQuery(element).val() !== undefined) {
            cmIds.push(jQuery(element).val());
        }
    });
    param['cmIds'] = cmIds;
    
    // need to use a traditional request in order for the cmIds to be passed properly
    jQuery.ajax({
        url: url,
        type: "GET",
        data: param,
        success: function(data) {
            table.append(data);
            bdrs.contribute.attachRowControls(rowIndex, surveyId, speciesRequired, showScientificName, table);
        },
        traditional: true
    });
};

/**
 * Helper function for attaching row controls
 * 
 * @param {Object} node - the dom node to search for inputs to attach row controls.
 * @param {Object} sightingIndex - the sighting index of the row to attach the controls on
 * @param {Object} surveyId - the survey id
 * @param {Object} speciesRequired - is the species field mandatory ?
 * @param {Object} numberRequired - is the number field mandatory ?
 * @param {Object} showScientificName - when true will show the scientific name, else will use the common name
 */
bdrs.contribute.attachRowControls = function(sightingIndex, surveyId, speciesRequired, showScientificName, table) {

    // Attach the autocomplete
    var search_elem = jQuery("[name="+sightingIndex+"_survey_species_search]");
    search_elem.data("surveyId", surveyId); 
    search_elem.autocomplete({
        source: bdrs.contribute.getAutocompleteSourceFcn(showScientificName),
        select: function(event, ui) {
            var taxon = ui.item.data;
            jQuery("[name="+sightingIndex+"_species]").val(taxon.id).blur();
        },
        html: true,
        minLength: 2,
        delay: 300
    });
    
    // Attach the datepickers
    bdrs.initDatePicker();
    // attach the validation
    var species_elem = jQuery("[name="+sightingIndex+"_species]");
    if (speciesRequired) {
        species_elem.addClass("validate(required)");
    }
    
    table.last("tr").ketchup();
};