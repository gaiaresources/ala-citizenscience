<!--  DIALOGS -->
<div id="htmlEditorDialog" title="HTML Editor">
    <label>Edit the HTML content that you want to display in the editor below: </label>
    <textarea id="markItUp"></textarea>
</div>

<script type="text/javascript">
jQuery(function() {
// Render the html editor using a jquery dialog.
$( "#htmlEditorDialog" ).dialog({
    width: 'auto',
    modal: true,
    autoOpen: false,
    buttons: {
        Cancel: function() {
            bdrs.attribute.closeHtmlEditor($(this));
        },
        "Clear": function() {
            $('#markItUp')[0].value = "";
        },
        "OK": function() {
            bdrs.attribute.saveAndUpdateContent(jQuery("#markItUp")[0]);
            bdrs.attribute.closeHtmlEditor(jQuery(this));
        }
    },
    zIndex: bdrs.MODAL_DIALOG_Z_INDEX
});

$('#markItUp').markItUp(bdrs.admin.myHtmlSettings);
});
</script>
