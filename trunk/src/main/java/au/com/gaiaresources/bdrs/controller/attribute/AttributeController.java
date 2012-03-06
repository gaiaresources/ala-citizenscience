package au.com.gaiaresources.bdrs.controller.attribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.threshold.ThresholdService;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

@Controller
public class AttributeController extends AbstractController {
    private Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private ThresholdService thresholdService;
    
    @Autowired
    private SurveyDAO surveyDAO;
    
    private AttributeFormFieldFactory formFieldFactory = new AttributeFormFieldFactory();

    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/attribute/ajaxAddAttribute.htm", method = RequestMethod.GET)
    public ModelAndView ajaxAddAttribute(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="index", required=true) int index,
            @RequestParam(value="showScope", required=false, defaultValue="true") boolean showScope,
            @RequestParam(value="isTag", required=false, defaultValue="false") boolean isTag) {
       
        ModelAndView mv = new ModelAndView("attributeRow");
        mv.addObject("formField", formFieldFactory.createAttributeFormField(index));
        mv.addObject("showScope", showScope);
        mv.addObject("isTag", isTag);
        mv.addObject("index", index);
        return mv;
    }
    
    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/attribute/ajaxCheckThresholdForAttribute.htm", method = RequestMethod.GET)
    public void ajaxCheckAttribute(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value="description", required=false) String description,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="typeCode", required=false) String typeCode,
            @RequestParam(value="scopeCode", required=false) String scopeCode,
            @RequestParam(value="options", required=false) String options,
            @RequestParam(value="surveyId", required=true) int surveyId) {

        Session sesh = RequestContextHolder.getContext().getHibernate();
        // create a mock attribute for testing against the thresholds
        Attribute attribute = new Attribute();
        attribute.setDescription(description);
        attribute.setName(name);
        attribute.setTypeCode(typeCode);
        if (scopeCode != null) {
            attribute.setScope(AttributeScope.valueOf(scopeCode));
        }
        if (options != null) {
            attribute.setOptions(createAttributeOptions(sesh, options));
        }
        sesh.evict(attribute);
        Survey survey = surveyDAO.get(surveyId);
        try {
            response.getOutputStream().write(String.valueOf(thresholdService.isActiveThresholdForAttribute(survey, attribute)).getBytes());
        } catch (IOException e) {
            log.warn("Error occurred writing to response stream", e);
        }
    }

    private List<AttributeOption> createAttributeOptions(Session sesh, String options) {
        String[] optionArray = options.split(",");
        List<AttributeOption> attrOpts = new ArrayList<AttributeOption>(optionArray.length);
        for (int i = 0; i < optionArray.length; i++) {
            AttributeOption opt = new AttributeOption();
            opt.setValue(optionArray[i]);
            sesh.evict(opt);
        }
        return attrOpts;
    }
}
