//--------------------------------------
// Attributes
//--------------------------------------

bdrs.attribute.addAttributeCount = 1;

// map for option tool tips
bdrs.attribute.OPTION_TOOLTIP = new Hashtable();

// Note not all attribute types have a tooltip for their options.
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.INTEGER_WITH_RANGE, "Enter two numbers, separated by a comma");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.BARCODE, "This is designed for use on mobile devices");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.REGEX, "Enter a Java regular expression");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.HTML, "Enter valid HTML into this option field to have it display on the form");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.HTML_NO_VALIDATION, "Enter valid HTML into this option field to have it display on the form. This HTML will not be validated and could potentially prevent the form from displaying.");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.STRING_WITH_VALID_VALUES, "Enter your choices, separated by a comma");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.MULTI_CHECKBOX, "Enter your choices, separated by a comma");
bdrs.attribute.OPTION_TOOLTIP.put(bdrs.model.taxa.attributeType.MULTI_SELECT, "Enter your choices, separated by a comma");

// map for validation ketchup class.
bdrs.attribute.VALIDATION_CLASS = new Hashtable();
bdrs.attribute.VALIDATION_CLASS.put(bdrs.model.taxa.attributeType.INTEGER_WITH_RANGE, "validate(attrOptionIntWithRange, required)");
bdrs.attribute.VALIDATION_CLASS.put(bdrs.model.taxa.attributeType.STRING_WITH_VALID_VALUES, "validate(required)");
bdrs.attribute.VALIDATION_CLASS.put(bdrs.model.taxa.attributeType.MULTI_CHECKBOX, "validate(attrOptionCommaSeparated, required)");
bdrs.attribute.VALIDATION_CLASS.put(bdrs.model.taxa.attributeType.MULTI_SELECT, "validate(attrOptionCommaSeparated, required)");
bdrs.attribute.VALIDATION_CLASS.put(bdrs.model.taxa.attributeType.STRING_WITH_VALID_VALUES, "validate(attrOptionCommaSeparated, required)");

/**
 * Adds a row to the attribute table.
 */
bdrs.attribute.addAttributeRow = function(tableSelector, showScope, isTag, showVisibility) {
    var index = bdrs.attribute.addAttributeCount++;

    jQuery.get(bdrs.portalContextPath+'/bdrs/admin/attribute/ajaxAddAttribute.htm',
            {'index': index, 'showScope': showScope, 'isTag': isTag, 'showVisibility': showVisibility}, function(data) {

        var table = jQuery(tableSelector); 
        var row = jQuery(data);
        table.find("tbody").append(row);
        bdrs.dnd.attachTableDnD(tableSelector);
        bdrs.dnd.tableDnDDropHandler(table[0], row[0]); 
        jQuery('form').ketchup();
        
        bdrs.attribute.setAttributeWeights(tableSelector);
    });
};

/**
 * Sets the attribute weights from 100 upwards in increments of 100
 * 
 * @param {Object} tableSelector - the selector for the form table
 */
bdrs.attribute.setAttributeWeights = function(tableSelector) {
    // this code relies on the fact that jQuery returns nodes in document order.
    // jQuery does this as of 1.3.2, http://docs.jquery.com/Release%3AjQuery_1.3.2
    var weight = 100;
    jQuery(tableSelector).find(".sort_weight").each(function(index, element) {
        jQuery(element).val(weight);
        weight += 100;
    });
};

bdrs.attribute.rowTypeChanged = function(event) {
    var index = event.data.index;
    var bNewRow = event.data.bNewRow;
    
    var prefix = bNewRow ? 'add_' : '';
    var newTypeCode = jQuery("[name=" + prefix + "typeCode_" + index + "]").val();
    var attrType = bdrs.model.taxa.attributeType.code[newTypeCode];
    var tooltip = bdrs.attribute.OPTION_TOOLTIP.get(attrType);
    var validation = bdrs.attribute.VALIDATION_CLASS.get(attrType);
    
    bdrs.attribute.enableInput(
        (bdrs.model.taxa.attributeType.STRING_WITH_VALID_VALUES.code === newTypeCode) ||
        (bdrs.model.taxa.attributeType.INTEGER_WITH_RANGE.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.BARCODE.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.REGEX.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.HTML.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.HTML_NO_VALIDATION.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.MULTI_CHECKBOX.code  === newTypeCode) ||
        (bdrs.model.taxa.attributeType.MULTI_SELECT.code  === newTypeCode),
        '[name=' + prefix + 'option_'+index+']', tooltip, validation);


    bdrs.attribute.updateMandatoryFieldState(event);
    
    var descriptionSelector = '[name=' + prefix + 'description_'+index+']';
    if(bdrs.model.taxa.attributeType.HTML_HORIZONTAL_RULE.code  === newTypeCode) { 
        jQuery(descriptionSelector).val('');
        jQuery(descriptionSelector).attr('disabled','disabled');
    } else { 
        jQuery(descriptionSelector).removeAttr('disabled');
    }
    
    // remove the hidden census method required input
    var attVal = jQuery('[name="'+prefix+'attribute_census_method_'+index+'"]');
    
    if(bdrs.model.taxa.attributeType.CENSUS_METHOD_ROW.code === newTypeCode ||
            bdrs.model.taxa.attributeType.CENSUS_METHOD_COL.code === newTypeCode) { 
      jQuery(descriptionSelector).attr('onfocus','bdrs.attribute.showCensusMethodAttributeEditor(jQuery(\'#censusMethodAttributeDialog\'), this, \''+newTypeCode+'\', \''+index+'\', \''+bNewRow+'\', \'[name='+prefix+'attribute_census_method_'+index+']\')');
      if (!attVal || attVal === undefined || attVal.length <= 0) {
      	  attVal = jQuery('<input type="text" name="'+prefix+'attribute_census_method_'+index+'"/>');
          jQuery(descriptionSelector).closest("td").find("span").append(attVal);
      }
      attVal.removeClass();
      attVal.addClass("hiddenKetchup validate(required)");
    } else {
	    if(bdrs.model.taxa.attributeType.HTML.code  === newTypeCode || 
	       bdrs.model.taxa.attributeType.HTML_NO_VALIDATION.code  === newTypeCode) { 
	        jQuery(descriptionSelector).attr('onfocus','bdrs.attribute.showHtmlEditor(jQuery(\'#htmlEditorDialog\'), jQuery(\'#markItUp\')[0], this)');
	    } else { 
	        jQuery(descriptionSelector).removeAttr('onfocus');
	    }
	    if (attVal && attVal.length > 0) {
	    	attVal.removeClass();
	        attVal.closest("tr").ketchup();
	        attVal.val("").trigger("blur");
	        attVal.addClass("hiddenKetchup");
	    } 
	}
    
    // name in database
    var nameSelector = '[name=' + prefix + 'name_'+index+']';
    if (bdrs.model.taxa.attributeType.HTML_HORIZONTAL_RULE.code === newTypeCode) {
        jQuery(nameSelector).val("");
        bdrs.attribute.enableInput(false, nameSelector, "", null);
    } else {
        bdrs.attribute.enableInput(true, nameSelector, "The name used to store this attribute in the database", "validate(uniqueAndRequired(.uniqueName))");
    }
};

bdrs.attribute.rowScopeChanged = function(event) {
        var index = event.data.index;
        var bNewRow = event.data.bNewRow;
        
        var changedElement = jQuery(event.currentTarget);
        var newScopeCode = changedElement.val();
        var attrScope = bdrs.model.taxa.attributeScope.value[newScopeCode];
        
        if (attrScope.isModerationScope()) {
            // show the moderation email settings div
            jQuery("#moderationSettingsLink").css("display","block");
            var isVisible = jQuery("#moderationSettings").css('display') !== 'none';
            if (!isVisible) {
                toggleModerationSettings("#moderationSettings", "#moderationSettingsLink a");
            }
        } else {
            var isOneModScope = false;
            // if there are no moderation attributes, hide the email settings
            changedElement.parents("table").find(".attrScopeSelect").each(function() {
                var scope = bdrs.model.taxa.attributeScope.value[jQuery(this).val()];
                isOneModScope |= scope.isModerationScope();
            });
            
            if (!isOneModScope) {
                // hide the moderation email settings div
                jQuery("#moderationSettingsLink").css("display","none");
                var isVisible = jQuery("#moderationSettings").css('display') !== 'none';
                if (isVisible) {
                    toggleModerationSettings("#moderationSettings", "#moderationSettingsLink a");
                }
            }
        }

        var visibilitySelector = bdrs.attribute.buildSelector(event, 'visibility');
        if (attrScope === bdrs.model.taxa.attributeScope.LOCATION) {
            jQuery(visibilitySelector).val('ALWAYS');
            jQuery(visibilitySelector).attr('disabled', 'disabled');
        }
        else {
            jQuery(visibilitySelector).removeAttr('disabled');
        }
        bdrs.attribute.updateMandatoryFieldState(event);
    };

/**
 * Called when the value in the Visibility column changes.
 * @param event the event associated with the change.
 */
bdrs.attribute.visibilityChanged = function(event) {

   bdrs.attribute.updateMandatoryFieldState(event);
};

/**
 * The state of the mandatory / required checkbox depends on two things:
 * 1. The state of the visibility attribute.  Attributes visible only in read mode cannot be mandatory.
 * 2. Read only attributes cannot be mandatory.
 * @param event the event that triggered this update.
 */
bdrs.attribute.updateMandatoryFieldState = function(event) {
    var mandatorySelector = bdrs.attribute.buildSelector(event, 'required');
    var typeCodeSelector = bdrs.attribute.buildSelector(event, 'typeCode');
    var typeCode = jQuery(typeCodeSelector).val();
    var visibilitySelector = bdrs.attribute.buildSelector(event, 'visibility');
    var visibility = jQuery(visibilitySelector).val();

    if (visibility === 'READ') {
        jQuery(mandatorySelector).attr('checked',false);
        jQuery(mandatorySelector).attr('disabled', 'disabled');
    }
    else {
        if(bdrs.model.taxa.attributeType.SINGLE_CHECKBOX.code  === typeCode ||
            bdrs.model.taxa.attributeType.HTML.code  === typeCode ||
            bdrs.model.taxa.attributeType.HTML_NO_VALIDATION.code  === typeCode ||
            bdrs.model.taxa.attributeType.HTML_COMMENT.code  === typeCode ||
            bdrs.model.taxa.attributeType.HTML_HORIZONTAL_RULE.code  === typeCode ||
            bdrs.model.taxa.attributeType.CENSUS_METHOD_ROW.code === typeCode ||
            bdrs.model.taxa.attributeType.CENSUS_METHOD_COL.code === typeCode) {
            jQuery(mandatorySelector).attr('checked',false);
            jQuery(mandatorySelector).attr('disabled','disabled');
        } else {
            jQuery(mandatorySelector).removeAttr('disabled');
        }
    }
};

/**
 * Builds a jQuery selector based on the naming convention used by the elements in the attribute form.
 * @param event the change event that is being processed, contains data about the row that has changed.
 * @param name the name of the form element to be selected, excluding any prefix and suffix.
 * @return {String} a jQuery selector that can be used to identify an element in a changed row.
 */
bdrs.attribute.buildSelector = function(event, name) {
    var index = event.data.index;
    var bNewRow = event.data.bNewRow;

    var prefix = bNewRow ? 'add_' : '';
    var selector = '[name=' + prefix + name + '_' + index +']';

    return selector;
};

/**
 * Returns a function to be attached to the onchanged event of the field type
 * input.
 * 
 * @param {Object} index the row - used to give each row a unique
 * identity.
 * @param {Object} bNewRow signals whether this row is a new row or an existing row.
 * @return a function to be triggered by the field type select control onchange.
 */
bdrs.attribute.getRowTypeChangedFunc = function() {
    return bdrs.attribute.rowTypeChanged;
};

bdrs.attribute.getRowScopeChangedFunc = function() {
    return bdrs.attribute.rowScopeChanged;
};

bdrs.attribute.validateClassRegex = /validate\([\w,\s]+\)/;

/**
 * Enables or disables the element(s) specified by the selector.
 * @param enableOption the enabled/disabled state
 * @param inputSelector jQuery selector for elements to modify.
 * @param tooltip text to apply to the title attribute for the input.
 * @param validationClass ketchup validation class to add to the class attribute for the input.
 */
bdrs.attribute.enableInput = function(enableOption, inputSelector, tooltip, validationClass) {
    var elem = jQuery(inputSelector);
    
    //var oldClass = elem.attr("class");
    // we always want to remove the current validation so...
    //var newClass = oldClass ? elem.attr("class").replace(bdrs.attribute.validateClassRegex, "") : null;
    var newClass = bdrs.attribute.removeValidationClass(elem.attr("class"));
    elem.attr("class", newClass);

    if(enableOption) {
        elem.removeAttr("disabled");
        elem.attr("title", tooltip);
        elem.addClass(validationClass);
    } else {
        // clear the options before disabling
        elem.val('');
        elem.attr("disabled", "disabled");
        elem.attr("title", null);
        elem.removeClass("hasKetchup");
    }
    
    // rebind ketchup
    elem.parents('form').ketchup();
};


bdrs.attribute.KETCHUP_VALIDATE_CLASS_PREFIX = "validate(";

/**
 * @param classStr the contents of the class attribute
 * @return the class string minus the validation class if it exists. null otherwise. 
 */
bdrs.attribute.removeValidationClass = function(classStr) {
    if (!classStr) {
        return null;
    }
    var startIdx = classStr.indexOf(bdrs.attribute.KETCHUP_VALIDATE_CLASS_PREFIX);
    if (startIdx > 0) {
       var currentIdx = startIdx + bdrs.attribute.KETCHUP_VALIDATE_CLASS_PREFIX.length;
       var bracketCount = 1;
       
       var currentChar;
       while (currentIdx < classStr.length - 1) {
              
           currentChar = classStr.charAt(currentIdx);
           if (currentChar === "(") {
              ++bracketCount;
           } else if (currentChar === ")") {
                 --bracketCount;
           }
           if (bracketCount === 0) {
              break;
           }
           ++currentIdx; 
       }
       
       if (bracketCount !== 0) {
           // error, brackets not matched.
           return null;    
       }
       // else brackets are closed properly...
       var validateClassString = classStr.substr(startIdx, currentIdx - startIdx + 1);
       var result = classStr.replace(validateClassString, "");
       return result;
       
    } else {
        return null;
    }
};

bdrs.attribute.htmlInput = "";

bdrs.attribute.saveAndUpdateContent = function(textEditor) {
    bdrs.attribute.htmlInput.value = textEditor.value;
};

/**
 * Shows a popup dialog with an HTML editor to allow the user to edit an HTML
 * attribute more easily.
 * @param popup the popup dialog on the page that you want to interact with
 * @param input the input that originated the dialog
 */
bdrs.attribute.showHtmlEditor = function(popup, textEditor, input) {
    textEditor.value = input.value;
    bdrs.attribute.htmlInput = input;
    popup.dialog('open');
};

bdrs.attribute.closeHtmlEditor = function(popup) {
    popup.dialog('close');
};

bdrs.attribute.createAttributeDisplayDiv = function(attributes, attributeSelector) {
    if (!attributeSelector) {
        return;
    }
    var attDiv = jQuery(attributeSelector);
    if (!attDiv) {
        return;
    }
    // remove any existing attributes
    jQuery(".attributeElement").remove();
    
    if (!attributes) {
        return;
    }
    
    var i;
    for (i = 0; i < attributes.length; i++) {
        var att = attributes[i];
        var attElem = jQuery('<div class="attributeElement" ></div>');
        var attDescElem = jQuery('<div class="attributeDescription" >' + 
                att.attribute.description + '</div>');
        var attValueElem;
        var attr_type = bdrs.model.taxa.attributeType.code[att.attribute.typeCode];
        // If this is a file attribute, create a link.
        if(attr_type.isFileType()) {
            if (att.attribute.type === "FILE" || att.attribute.type === "AUDIO" ) {
                // make a link to download the file
                attValueElem = jQuery('<div class="attributeValue" >' + 
                        '<a href="'+bdrs.portalContextPath+'/files/download.htm?'+att.fileURL+'">' +
                        att.value+'</a></div>');
            } else if (att.attribute.type === "IMAGE") {
                // make a link to download the file
                attValueElem = jQuery('<div class="attributeValue" >' + 
                        '<a href="'+bdrs.portalContextPath+'/files/download.htm?'+att.fileURL+'">' +
                        '<img width="250"'+
                            'src="'+bdrs.portalContextPath+'/files/download.htm?'+att.fileURL+'"' +
                            'alt="Missing Image"/></a></div>');
            } 
        } else if (attr_type.isHtmlType()) {
            // display the html in one single div instead of two
            attDescElem = null;
            var valString = "";
            if (bdrs.model.taxa.attributeType.value.HTML_HORIZONTAL_RULE === attr_type) {
                valString = "<hr/>";
            } else {
                valString = att.attribute.description;
            }
            attValueElem = jQuery('<div class="attributeValue" >' + valString + '</div>');
        } else if (attr_type.isCensusMethodType()) {
            // show the census method types as a table
            attDescElem = bdrs.attribute.createCensusMethodTable(attr_type, att, true);
        } else {
            attValueElem = jQuery('<div class="attributeValue" >' + 
                    att.value + '</div>');
        }
        if (attDescElem) {
            attElem.append(attDescElem);
        }
        if (attValueElem) {
            attElem.append(attValueElem);
        }
        attDiv.append(attElem);
    }
};

bdrs.attribute.createCensusMethodTable = function(attr_type, att, showCaption) {
    if (attr_type === bdrs.model.taxa.attributeType.CENSUS_METHOD_COL) {
        return bdrs.attribute.createCensusMethodColTable(attr_type, att, showCaption);
    } else if (attr_type === bdrs.model.taxa.attributeType.CENSUS_METHOD_ROW) {
        return bdrs.attribute.createCensusMethodRowTable(attr_type, att, showCaption);
    }
    
    var error = jQuery('<div class="attributeValue" >Unable to retrieve locations for attribute</div>');
    return error;
}

bdrs.attribute.createCensusMethodColTable = function(attr_type, att, showCaption) {
    var element = jQuery('<div class="attributeValue" ></div>');
    var table = jQuery('<table class="censusMethodAttributeTable"></table>');
    if (showCaption) {
        table.append(jQuery('<caption>'+att.attribute.description+'</caption>'));
    }
    
    table.addClass("datatable");
    element.addClass("scrollable");
    var thead = jQuery("<thead></thead>");
    for (var i = 0; i < att.attribute.censusMethod.attributes.length; i++) {
        var cmAtt = att.attribute.censusMethod.attributes[i];
        thead.append('<th>'+cmAtt.description+'</th>');
    }
    table.append(thead);

    var tbody = jQuery('<tbody></tbody>');
    for (var i = 0; i < att.records.length; i++) {
        var record = att.records[i];
        var row = jQuery('<tr></tr>');
        for (var j = 0; j < att.attribute.censusMethod.attributes.length; j++) {
            var cmAtt = att.attribute.censusMethod.attributes[j];
            var cell = jQuery('<td></td>');
            
            var cmAttType = bdrs.model.taxa.attributeType.code[cmAtt.typeCode];
            
            for (var k = 0; k < record.attributes.length; k++) {
                var value = record.attributes[k];
                if (value.attribute.id === cmAtt.id) {
                    if (cmAttType.isCensusMethodType()) {
                        // don't create nested attribute tables here because the server cannot flatten the object enough
                        // to show them due to memory limitations
                        //cell.append(bdrs.attribute.createCensusMethodTable(cmAttType, value, true));
                    } else {
                        cell.text(value.value);
                    }
                }
            }
            row.append(cell);
        }
        tbody.append(row);
    }
    table.append(tbody);
    element.append(table);
    
    return element;
};

bdrs.attribute.createCensusMethodRowTable = function(attr_type, att, showCaption) {
    var element = jQuery('<div class="attributeValue" ></div>');
    var table = jQuery('<table class="censusMethodAttributeTable"></table>');
    if (showCaption) {
        table.append(jQuery('<caption>'+att.attribute.description+'</caption>'));
    }
    
    var tbody = jQuery('<tbody></tbody>');
    for (var i = 0; i < att.records.length; i++) {
        var record = att.records[i];
        for (var j = 0; j < att.attribute.censusMethod.attributes.length; j++) {
            var row = jQuery('<tr></tr>');
            var cmAtt = att.attribute.censusMethod.attributes[j];
            var cell = jQuery("<td></td>");
            for (var k = 0; k < record.attributes.length; k++) {
                var value = record.attributes[k];
                if (value.attribute.id === cmAtt.id) {
                    cell.text(value.value);
                }
            }
            row.append('<th>'+cmAtt.description+'</th>');
            row.append(cell);
            tbody.append(row);
        }
    }
    table.append(tbody);
    element.append(table);
    
    return element;
};

bdrs.attribute.getRowOptions = function(row) {
    return {
        'description': jQuery(row).find(".attrDesc").val(),
        'name': row.find(".attrName").val(),
        'typeCode': row.find(".attrTypeSelect option:selected").val(),
        'scopeCode': row.find(".attrScopeSelect option:selected").val(),
        'options': row.find(".attrOpt").val()
    };
};

bdrs.attribute.checkForThreshold = function(row, surveyId) {
    var options = bdrs.attribute.getRowOptions(row);
    jQuery.extend(options, {"surveyId": surveyId});
    jQuery.get(bdrs.portalContextPath+'/bdrs/admin/attribute/ajaxCheckThresholdForAttribute.htm',
            options, 
            function(data) {
                if (data === 'true') {
                    // highlight the row
                    row.addClass("ui-state-highlight");
                    row.addClass("ui-widget-content");
                } else {
                    // un-highlight the row if it is highlighted
                    row.removeClass("ui-state-highlight");
                    row.removeClass("ui-widget-content");
                }
            }
    );
};

/**
 * Shows a popup dialog with a Census Method attribute editing form.
 * @param popup the popup dialog on the page that you want to interact with
 * @param input the input that originated the dialog
 */
bdrs.attribute.showCensusMethodAttributeEditor = function(popup, input, typeCode, index, bNewRow, cmSelector) {
    // todo: need to get attribute cm info and populate dialog if existing attribute
    popup.data('input', input);
    popup.data('typeCode', typeCode);
    popup.data('index', index);
    popup.data('bNewRow', bNewRow);
    var selectedCM = jQuery(input).closest("td").find(cmSelector);
    popup.data('selectedCM', selectedCM != undefined && selectedCM.val() != undefined ? selectedCM.val() : '');
    popup.dialog('open');
};

bdrs.attribute.openCMAttributeDialog = function(dialog) {
    var input = jQuery(dialog).data('input');
    var typeCode = jQuery(dialog).data('typeCode');
    var selectedCM = jQuery(dialog).data('selectedCM');
    
    // populate the form with the selected census method attribute options
    jQuery(dialog).find('#attrDesc').val(jQuery(input).val());
    
    // populate the census methods
    var censusMethods = jQuery(dialog).find("#censusMethod");
    censusMethods.empty();
    jQuery.getJSON(bdrs.contextPath + '/bdrs/admin/censusMethod/search.htm', {rows: 100}, function(data) {
        // create the options from the rows
        var rows = data.rows;
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var cmOpt = jQuery('<option value="'+row.id+'" '+(row.id === selectedCM ? 'selected' : '')+'>'+row.name+'</option>');
            censusMethods.append(cmOpt);
        }
    });
};

bdrs.attribute.saveCMAttributeDialog = function(dialog) {
    // set the form data from the dialog form data
    var input = jQuery(dialog).data('input');
    var index = jQuery(dialog).data('index');
    var bNewRow = jQuery(dialog).data('bNewRow');
    var prefix = '';
    if (bNewRow === 'true') {
        prefix = 'add_';
    }
    // get the parent row
    var parentRow = jQuery(input).parents('tr');
    // name goes in the Description column
    jQuery(input).val(jQuery(dialog).find('#attrDesc').val());
    
    // add census method to the form as a hidden input
    var cm = jQuery(dialog).find('#censusMethod').val();
    var attVal = jQuery('[name="'+prefix+'attribute_census_method_'+index+'"]');
    if (attVal && attVal.length > 0) {
    } else {
        attVal = jQuery('<input type="text" name="'+prefix+'attribute_census_method_'+index+'" value="'+cm+'"/>');
        jQuery(input).closest("td").append(attVal);
    }
    attVal.val(cm);
    attVal.removeClass();
    attVal.addClass("hiddenKetchup validate(required)");
    attVal.closest("tr").ketchup();
    attVal.trigger("blur");
};
