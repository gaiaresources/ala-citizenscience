package au.com.gaiaresources.bdrs.servlet.jsp.tag;

import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * The SpeciesProfileTypeTag writes out some javascript that categorises a Species Profile Type into
 * files, images and text as per the SpeciesProfile class.
 * This has been done so as to not have to maintain this categorisation in both the java class and in
 * javascript.
 *
 * The resulting javascript looks like:
 * <code>
 *     <script type="text/javascript>
 *         bdrs.taxonomy.speciesProfileType = {
 *             imageTypes: [...],
 *             fileTypes: [...],
 *             textTypes: [...],
 *             isImageType: function(type) {...},
 *             isFileType: function(type) {...},
 *             isTextType: function(type) {...}
 *         ;}
 *     </script>
 *
 * </code>
 */
public class SpeciesProfileTypeTag extends TagSupport {

    /** The javascript that will be rendered by this tag. */
    private static String speciesProfileJavascript = buildSpeciesProfileJavaScript();
    /**
     * Creates and caches (in a field) the javascript that will be rendered to the page when the tag is encountered.
     */
    private static String buildSpeciesProfileJavaScript() {
        
        List<String> imageTypes = new ArrayList<String>();
        List<String> fileTypes = new ArrayList<String>();
        List<String> longTextTypes = new ArrayList<String>();
        populateSpeciesProfileTypeLists(imageTypes, fileTypes, longTextTypes);

        StringBuilder script = new StringBuilder();
        script.append("<script type=\"text/javascript\">");
        script.append("    bdrs.taxonomy.speciesProfileType = {");
        script.append("        imageTypes : "+toJavaScriptArray(imageTypes)+",");
        script.append("        fileTypes  : "+toJavaScriptArray(fileTypes)+",");
        script.append("        textTypes  : "+toJavaScriptArray(longTextTypes)+",");
        script.append("        isImageType : function(type) {return (jQuery.inArray(type, this.imageTypes) >= 0);},");
        script.append("        isFileType  : function(type) {return (jQuery.inArray(type, this.fileTypes) >= 0);},");
        script.append("        isTextType  : function(type) {return (jQuery.inArray(type, this.textTypes) >= 0);}");
        script.append("    };");
        script.append("</script>");
        return script.toString();
    }

    /**
     * Loops through each type defined by SpeciesProfile.SPECIES_PROFILE_TYPE_VALUES and categories it
     * as an image, file or text type.
     * @param imageTypes Stores a list of types determined to be image types by the SpeciesProfile.isImgType method
     * @param fileTypes Stores a list of types determined to be file types by the SpeciesProfile.isFileType method
     * @param longTextTypes Stores a list of types determined to be text types by the SpeciesProfile.isTextType method
     */
    private static void populateSpeciesProfileTypeLists(List<String> imageTypes, List<String> fileTypes, List<String> longTextTypes) {
        SpeciesProfile profile = new SpeciesProfile();
        for (String type : SpeciesProfile.SPECIES_PROFILE_TYPE_VALUES.keySet()) {
            profile.setType(type);

            if (profile.isImgType()) {
                imageTypes.add(type);
            }
            if (profile.isFileType()) {
                fileTypes.add(type);
            }
            if (profile.isTextType()) {
                longTextTypes.add(type);
            }
        }

    }

    /**
     * Turns a java array into a String that declares a valid javascript array.
     * @param values the array to represent as javascript.
     * @return a String declaring a javascript array containing the values in the supplied list.
     */
    private static String toJavaScriptArray(List<String> values) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (String value : values) {
            out.append("'").append(value).append("',");
        }
        // remove the last ,
        out.deleteCharAt(out.length()-1);
        out.append("]");

        return out.toString();
    }

    /**
     * Writes the javascript to the page.
     * @return always returns SKIP_BODY
     */
    @Override
    public int doStartTag() throws JspException {

        JspWriter out = pageContext.getOut();
        try {
           out.println(speciesProfileJavascript);

        } catch (Exception e) {
            throw new JspException("Failed to write content.", e);
        }
        return SKIP_BODY;
    }

    /**
     * Does nothing.
     * @return always returns EVAL_PAGE.
     */
    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }


}
