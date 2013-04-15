<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Edit CSS Layout - '${ filename }'</h1>

<div>
    <p>
        This is where you may edit your <a href="http://en.wikipedia.org/wiki/Cascading_Style_Sheets">cascading style sheet</a> (CSS) 
        to customise the look of your project form. You will need some experience with CSS and HTML in order to style your form.
    </p>
    
    <p>
    Use CSS with the appropriate selectors to style your form. The following HTML will be produced for each form field:
    </p>
    
    <p class="formFieldLayoutExample">
    &lt;div id="ff_(name in database)" class="form_field_pair"&gt;</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="form_field_label"&gt;</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Description of form field</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/div&gt;</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="form_field_input"&gt;</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Input of form field</br>
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/div&gt;</br>
    &lt;/div&gt;</br>
    </p>
</div>

<div class="input_container">
<form method="post" action="${portalContextPath}/bdrs/admin/survey/editCssLayout.htm">
    <input name="surveyId" type="hidden" value="${ survey.id }" />
    <textarea id="markItUp" name="text_to_save">${ text }</textarea>
    <div class="markItUpSubmitButton buttonpanel textright">
        <input id="submitEditContent1" type="submit" class="form_action" value="Save And Continue Editing" name="saveAndContinueEditing" />
        <input id="submitEditContent2" type="submit" class="form_action" value="Save" />
        <input id="submitEditContent3" type="submit" class="form_action" value="Save And Continue" name="saveAndContinue" />
    </div>
    <div class="clear"></div>
</form>

</div>
<script type="text/javascript">

var myHtmlSettings = {
        nameSpace:       "css", // Useful to prevent multi-instances CSS conflict
        onShiftEnter:    {keepDefault:false, replaceWith:'<br />\n'},
        onCtrlEnter:     {keepDefault:false, openWith:'\n<p>', closeWith:'</p>\n'},
        onTab:           {keepDefault:false, openWith:'     '},
        markupSet:  []  // no markup as we are editing CSS, not html! 
};

    jQuery(document).ready(function() {
       // bdrs.admin.adminEditContent.setTextArea('#markItUp');
        $('#markItUp').markItUp(myHtmlSettings);
    });
</script>
