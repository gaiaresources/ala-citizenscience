<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<p>Select a census method whose attributes will define the table layout for this attribute and enter a name for the table.</p>
<label for="attrDesc">Enter a name for this attribute (this will show as the title of the table):</label>
<input type="text" name="attrDesc" id="attrDesc"/>
    
    <div id="censusMethodSelector">
    <label for="censusMethod">Select a Census Method:</label>
    <select name="censusMethod" id="censusMethod" >
    </select>
    </div>

<script type="text/javascript">
var addCMAttributeCount = 0;

jQuery(function() {
    jQuery( "#censusMethodAttributeDialog" ).dialog({
        width: 'auto',
        modal: true,
        autoOpen: false,
        zIndex: bdrs.MODAL_DIALOG_Z_INDEX,
        resizable: false,
        buttons: {
            "Save": function() {
                // set the form data from the dialog form data
                // name goes in the Description column
                // census method is saved as an attribute value
                // row headers are saved as attribute options?
                bdrs.attribute.saveCMAttributeDialog(this);
                jQuery( this ).dialog( "close" );
            },
            Cancel: function() {
                jQuery( this ).dialog( "close" );
            }
        },
        open: function(event, ui) {
            // use the attribute type to determine the form to show
            // populate the form with the selected census method attribute options
            bdrs.attribute.openCMAttributeDialog(this);
        }
    });
    bdrs.fixJqDialog("#censusMethodAttributeDialog");
});
</script>