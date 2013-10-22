package au.com.gaiaresources.bdrs.controller.location;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.attribute.AttributeDictionaryFactory;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormFieldFactory;
import au.com.gaiaresources.bdrs.controller.map.WebMap;
import au.com.gaiaresources.bdrs.controller.record.RecordController;
import au.com.gaiaresources.bdrs.controller.record.RecordWebFormContext;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.controller.survey.SurveyBaseController;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataBuilder;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataHelper;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataRow;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.deserialization.attribute.AttributeDeserializer;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.model.user.UserDAO;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.map.GeoMapService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.util.SpatialUtil;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

@Controller
public class LocationBaseController extends RecordController {
    
    public static final String GET_SURVEY_LOCATIONS_FOR_USER = "/bdrs/location/getSurveyLocationsForUser.htm";
    
    public static final String PARAM_SURVEY_ID = BdrsWebConstants.PARAM_SURVEY_ID;

    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    
    @Autowired
    private LocationDAO locationDAO;
    @Autowired
    private MetadataDAO metadataDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private TaxaDAO taxaDAO;
    @Autowired
    private GeoMapService geoMapService;
    @Autowired
    private SessionFactory sessionFactory;
    
    private AttributeDictionaryFactory attrDictFact;
    private WebFormAttributeParser attributeParser;
    
    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/location/editUserLocations.htm", method = RequestMethod.GET)
    public ModelAndView editUserLocations(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value="redirect", defaultValue="/bdrs/location/editUserLocations.htm") String redirect) {

        User user = getRequestContext().getUser();
        
        Metadata defaultLocId = user.getMetadataObj(Metadata.DEFAULT_LOCATION_ID);
        Location defaultLocation;
        if(defaultLocId == null) {
            defaultLocation = null;
        } else {
            int defaultLocPk = Integer.parseInt(defaultLocId.getValue());
            defaultLocation = locationDAO.getLocation(defaultLocPk);
        }

        Integer defaultLocationId = -1;
        if(defaultLocation != null) {
            defaultLocationId = defaultLocation.getId();
        }
        
        ModelAndView mv = new ModelAndView("userEditLocations");
        mv.addObject("locations", locationDAO.getUserLocations(user));
        mv.addObject("defaultLocationId", defaultLocationId);
        mv.addObject("redirect", redirect);
        return mv;
    }

    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/location/editUserLocations.htm", method = RequestMethod.POST)
    public ModelAndView submitUserLocations(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value="add_location", required=false) int[] addLocationIndexes,
            @RequestParam(value="location", required=false) int[] locationIds,
            @RequestParam(value="defaultLocationId", required=false) String defaultLocationId,
            @RequestParam(value="redirect", defaultValue="/bdrs/location/editUserLocations.htm") String redirect) {
        
        addLocationIndexes = addLocationIndexes == null ? new int[]{} : addLocationIndexes;
        locationIds = locationIds == null ? new int[]{} : locationIds;
        User user = getRequestContext().getUser();

        // This map represents all locations for a user.
        // As locations are updated, they will be removed from this map.
        // At the end of this method, any locations still in this map will
        // be deleted.
        Map<Integer, Location> locationMap = new HashMap<Integer, Location>();
        for(Location loc : locationDAO.getUserLocations(user)) {
            locationMap.put(loc.getId(), loc);
        }

        // Added Locations
        Map<Integer, Location> addedLocationMap = new HashMap<Integer, Location>();
        for(int rawIndex : addLocationIndexes) {
            Location location = createNewLocation(request, String.valueOf(rawIndex), BdrsCoordReferenceSystem.DEFAULT_SRID);
            location.setUser(user);
            location = locationDAO.save(location);
            
            addedLocationMap.put(rawIndex, location);
        }

        // Updated Locations
        for(int pk : locationIds) {
            Location location = locationMap.remove(pk);
            location = updateLocation(request, String.valueOf(pk), location, BdrsCoordReferenceSystem.DEFAULT_SRID);
            location.setUser(user);
            locationDAO.save(location);
        }

        // Location to be Deleted
        // We cannot actually delete the location object because it may be
        // connected to an Record. Instead we are going to unlink it from the
        // User. This means that it is possible for orphan locations to be 
        // created.
        for(Map.Entry<Integer, Location> tuple : locationMap.entrySet()) {
            //locationDAO.delete(tuple.getValue());
        	Location loc = tuple.getValue();
        	loc.setUser(null);
        	loc = locationDAO.save(loc);
        }
        
        try{
            if(defaultLocationId != null) {
                Metadata defaultLocMD = user.getMetadataObj(Metadata.DEFAULT_LOCATION_ID);
                if(defaultLocMD == null) {
                    defaultLocMD = new Metadata();
                    defaultLocMD.setKey(Metadata.DEFAULT_LOCATION_ID);
                }
                
                String[] split = defaultLocationId.split("_");
                if(split.length == 2) {
                    Integer val = new Integer(split[1]);
                    if(defaultLocationId.startsWith("id_")) {
                        defaultLocMD.setValue(val.toString());
                    } else if(addedLocationMap.containsKey(val)) {
                        defaultLocMD.setValue(addedLocationMap.get(val).getId().toString());
                    } else {
                        throw new IllegalArgumentException("Unable to match default location with an id or an index."+defaultLocationId);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid default location id format received: "+ defaultLocationId);
                }
                
                metadataDAO.save(defaultLocMD);
                
                user.getMetadata().add(defaultLocMD);
                userDAO.updateUser(user);
            }
        } catch(NumberFormatException nfe) {
            // Do nothing. Bad data.
            log.error("Invalid location PK or index received: "+defaultLocationId, nfe);
        } catch(IllegalArgumentException iae) {
            log.error(iae.getMessage(), iae);
        }

        ModelAndView mv = new ModelAndView(new PortalRedirectView(redirect, true));
        return mv;
    }

    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/location/ajaxAddUserLocationRow.htm", method = RequestMethod.GET)
    public ModelAndView ajaxAddUserLocationRow(HttpServletRequest request, HttpServletResponse response) {
        
        Location defaultLocation = null;
        User user = getRequestContext().getUser();
        Metadata defaultLocId = user.getMetadataObj(Metadata.DEFAULT_LOCATION_ID);
        if(defaultLocId == null) {
            defaultLocation = null;
        } else {
            int defaultLocPk = Integer.parseInt(defaultLocId.getValue());
            defaultLocation = locationDAO.getLocation(defaultLocPk);
        }
        
        ModelAndView mv = new ModelAndView("userLocationRow");
        mv.addObject("index", Integer.valueOf(request.getParameter("index")));
        mv.addObject("defaultLocationId", defaultLocation == null ? Integer.valueOf(-1) : defaultLocation.getId());
        return mv;
    }
    
    
    // ----------------------------------------
    // Admin Functionality
    // ----------------------------------------
    
    @RolesAllowed( {Role.USER,Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/location/ajaxAddSurveyLocationRow.htm", method = RequestMethod.GET)
    public ModelAndView ajaxAddSurveyLocationRow(HttpServletRequest request, HttpServletResponse response) {
        
        ModelAndView mv = new ModelAndView("surveyLocationRow");
        mv.addObject("index", Integer.parseInt(request.getParameter("index")));
        return mv;
    }
    
    @RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/survey/locationListing.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyLocationListing(
			HttpServletRequest request, 
			HttpServletResponse response,
			@RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, 
			required = true) int surveyId) {
        Survey survey = getSurvey(surveyId);
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        ModelAndView mv = new ModelAndView("locationListing");
        mv.addObject("survey", survey);
        return mv;
    }

    @RolesAllowed( {Role.POWERUSER, Role.SUPERVISOR, Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/survey/locationUpload.htm", method = RequestMethod.POST)
    public ModelAndView locationUpload(
    		MultipartHttpServletRequest req,
            HttpServletResponse res,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required=true) int surveyId) {
    	User user = getRequestContext().getUser();
    	Survey survey = getSurvey(surveyId);
                
    	ModelAndView view = new ModelAndView("locationListing");
    	
    	String message = "";

    	try {
    		MultipartFile uploadedFile = req.getFile("input_csv");

    		if (uploadedFile != null) {
    			if ("application/vnd.ms-excel".equals(uploadedFile.getContentType())) {
    				InputStream inp = uploadedFile.getInputStream();
    				try {
    					Workbook wb = WorkbookFactory.create(inp);
    		            Sheet locationSheet = wb.getSheetAt(0);

    		            for (int i = 1; i <= locationSheet.getLastRowNum(); i++) {
    		            	Row row = locationSheet.getRow(i);

    		            	Location location = new Location(); 
    		            	location.setName(row.getCell(0).getStringCellValue());
    		            	
    		            	PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
    		            	
    		            	int srid = (int)row.getCell(3).getNumericCellValue();
    		            	
    		            	GeometryFactory geometryFactory = new GeometryFactory(precisionModel, srid);
    		            	Point point = geometryFactory.createPoint(
    		            			new Coordinate(
    		            					row.getCell(1).getNumericCellValue(),
    		            					row.getCell(2).getNumericCellValue()));

    		            	location.setLocation(point);
    		            	location.setSurveys(Arrays.asList(survey));
    		            	location.setUser(user);

    		            	location = locationDAO.save(location);
    		            	
    		            	List<Location> locations = survey.getLocations();
    		            	locations.add(location);
    		            	survey.setLocations(locations);
    		            	surveyDAO.save(survey);

    		            	message = "Import completed successfully.";
    		            }
    		        }
    				catch (InvalidFormatException ife) {
    		            throw new IllegalArgumentException(ife);
    		        }
    			}
    			else {
                    // Failed to have the right content type.
                    message = "The uploaded file was not an XLS file.";
                    log.warn(message);
                }
            }
    		else {
                // Failed to upload a file.
    			message = "Spreadsheet file is required.";
                log.error(message);
            }
    		
    	}
    	catch (Exception e) {
    		System.out.println("Unhandled exception: " + e.toString());
    	}

    	view.addObject("survey", survey);
    	view.addObject("message", message);
        return view;
    }
    
    @RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/survey/locationListing.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyLocationListing(
    		HttpServletRequest request, 
    		HttpServletResponse response,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required = true) int surveyId) {
        
        Survey survey = getSurvey(surveyId);
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        List<Location> locationList = new ArrayList<Location>();

        // Updated Locations
        if (request.getParameter("location") != null ) {
            for(String rawPK : request.getParameterValues("location")) {
                int pk = Integer.parseInt(rawPK);
                Location location = locationDAO.getLocation(pk);
                locationList.add(location);
            }
        }

        survey.setLocations(locationList);

        boolean predefined_locations_only = request.getParameter("restrict_locations") != null;
        Metadata predefinedLocMetadataData = survey.getMetadataByKey(Metadata.PREDEFINED_LOCATIONS_ONLY);
        
        if(predefinedLocMetadataData == null) {
            predefinedLocMetadataData = new Metadata();
            predefinedLocMetadataData.setKey(Metadata.PREDEFINED_LOCATIONS_ONLY);
        }
        predefinedLocMetadataData.setValue(String.valueOf(predefined_locations_only));
        metadataDAO.save(predefinedLocMetadataData);
        survey.getMetadata().add(predefinedLocMetadataData);
        
        // Update the form rendering type given the new criteria
        SurveyFormRendererType formRenderType = survey.getFormRendererType();
        if(formRenderType == null || (formRenderType != null && !formRenderType.isEligible(survey))) {
            Metadata md = survey.setFormRendererType(SurveyFormRendererType.DEFAULT);
            metadataDAO.save(md);
        }
        surveyDAO.save(survey);

        getRequestContext().addMessage("bdrs.survey.locations.success", new Object[]{survey.getName()});

        ModelAndView mv;
        if(request.getParameter("saveAndContinue") != null) {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/editUsers.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
        }
        else {
            mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/listing.htm", true));
        }
        return mv;
    }

    /**
     * Renderer for the editLocation page. 
     * This method creates the location attribute form fields.
     * @param request
     * @param response
     * @param surveyId The id of the survey for the location.
     * @param locationId (optional) The id of the location to edit.  If null, a new location will be created.
     * @return
     */
    @RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/survey/editLocation.htm", method = RequestMethod.GET)
    public ModelAndView editSurveyLocation(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required = true) int surveyId,
            @RequestParam(value=BdrsWebConstants.PARAM_LOCATION_ID, required = false) Integer locationId) {
        Survey survey = getSurvey(surveyId);
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        FormFieldFactory formFieldFactory = new FormFieldFactory();
        Location location = null;
        List<FormField> surveyFormFieldList = new ArrayList<FormField>();
        List<Attribute> surveyAttributeList = new ArrayList<Attribute>(survey.getAttributes());
        User loggedInUser = getRequestContext().getUser();
        Set<AttributeValue> locationAttributes = null;
        RecordWebFormContext context = new RecordWebFormContext(null, loggedInUser);
        if (locationId != null) {
            location = getLocation(locationId);
            locationAttributes = location.getAttributes();
            // add the location attribute form fields
            for (AttributeValue attr : locationAttributes) {
                if (surveyAttributeList.remove(attr.getAttribute())) {
                    if (AttributeType.isCensusMethodType(attr.getAttribute().getType())) {
                        FormField ff = createCensusMethodFormField(survey, null, location, 
                                                                   attr.getAttribute(), 
                                                                   loggedInUser, 
                                                                   AttributeParser.DEFAULT_PREFIX, 
                                                                   context, 
                                                                   BdrsWebConstants.LOCATION_ATTR_CATEGORY);
                        if (ff != null) {
                            surveyFormFieldList.add(ff);
                        }
                    } else {
                        surveyFormFieldList.add(formFieldFactory.createLocationFormField(attr.getAttribute(), attr, survey));
                    }
                }
            }
        } else {
            location = new Location();
        }
        
        for (Attribute surveyAttr : surveyAttributeList) {
            if(AttributeScope.LOCATION.equals(surveyAttr.getScope())) {
                if (AttributeType.isCensusMethodType(surveyAttr.getType())) {
                    FormField ff = createCensusMethodFormField(survey, null, surveyAttr, 
                                                               loggedInUser, 
                                                               AttributeParser.DEFAULT_PREFIX, 
                                                               context, 
                                                               BdrsWebConstants.LOCATION_ATTR_CATEGORY);
                    if (ff != null) {
                        surveyFormFieldList.add(ff);
                    }
                } else {
                    surveyFormFieldList.add(formFieldFactory.createLocationFormField(surveyAttr, survey));
                }
            }
        }
        
        Collections.sort(surveyFormFieldList);
        
        ModelAndView mv = new ModelAndView("surveyEditLocation");
        mv.addObject("survey", survey);
        mv.addObject("locationFormFieldList", surveyFormFieldList);
        mv.addObject("location", location);
        mv.addObject("description", location.getDescription());
        mv.addObject("ident", loggedInUser.getRegistrationKey());
        mv.addObject(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT, context);
        // location scoped attributes are always editable on the edit location page...
        mv.addObject(RecordWebFormContext.MODEL_EDIT, true);
        mv.addObject(BdrsWebConstants.MV_WEB_MAP, new WebMap(geoMapService.getForSurvey(survey)));
        
        return mv;
    }

    /**
     * This method creates a new location or updates an existing one, including its attributes.
     * @param request
     * @param response
     * @param surveyId The id of the survey for the location.
     * @param locationId (optional) The id of the location to edit.  If null, a new location will be created.
     * @return
     */
    @RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = "/bdrs/admin/survey/editLocation.htm", method = RequestMethod.POST)
    public ModelAndView submitSurveyLocation(MultipartHttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required = true) int surveyId,
            @RequestParam(value=BdrsWebConstants.PARAM_LOCATION_ID, required = false) Integer locationId,
            @RequestParam(value=BdrsWebConstants.PARAM_SRID, required=true) int srid) {
        Survey survey = getSurvey(surveyId);
        if (survey == null) {
            return SurveyBaseController.nullSurveyRedirect(getRequestContext());
        }
        
        // disable the partial record filter to allow records for attribute values to be retrieved
        // for census method attribute types
        FilterManager.disablePartialRecordCountFilter(getRequestContext().getHibernate());
        try {
            if (request.getParameter("goback") == null) {
                List<Location> locationList = survey.getLocations();
    
                Location location = null;
                // Added Locations
                if(locationId == null) {
                    location = createNewLocation(request, srid);
                } 
                else {
                    location = locationDAO.getLocation(locationId);
                    // remove the location before updating and re-adding it
                    locationList.remove(location);
                    location = updateLocation(request, location, srid);
                }
                // save the location attributes
                try {
                    Set locAtts = saveAttributes(request, survey, location);
                    location.setAttributes(locAtts);
                } catch (Exception e) {
                    log.error("Error setting the location attributes: ", e);
                }
                locationDAO.save(location);
                locationList.add(location);
                
                survey.setLocations(locationList);
                surveyDAO.save(survey);
        
                getRequestContext().addMessage("bdrs.survey.locations.success", new Object[]{survey.getName()});
            }
            
            ModelAndView mv = new ModelAndView(new PortalRedirectView("/bdrs/admin/survey/locationListing.htm", true));
            mv.addObject(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId());
            return mv;
        } finally {
            // enable the partial record filter to prevent records for attribute values to be retrieved
            FilterManager.setPartialRecordCountFilter(sessionFactory.getCurrentSession());
        }
    }
    
    @RolesAllowed( {Role.POWERUSER,Role.SUPERVISOR,Role.ADMIN} )
    @RequestMapping(value = GET_SURVEY_LOCATIONS_FOR_USER, method = RequestMethod.GET)
    public void getSurveyLocationsForUser(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_SURVEY_ID, required=true) int surveyId) throws Exception {
        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);       
        PaginationFilter filter = jqGridHelper.createFilter(request);
        
        User currentUser = getRequestContext().getUser();
        PagedQueryResult<Location> queryResult = locationDAO.getSurveylocations(filter, currentUser, surveyId);
        
        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());

        if (queryResult.getCount() > 0) {
            for (Location loc : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(loc.getId());
                row
                .addValue("name", loc.getName())
                .addValue("description", loc.getDescription());
                builder.addRow(row);
            }
        }
        writeJson(request, response, builder.toJson());
    }
    
    @SuppressWarnings("unchecked")
    private Set<TypedAttributeValue> saveAttributes(
            MultipartHttpServletRequest request, Survey survey, Location location) throws ParseException, IOException {
        User currentUser = getRequestContext().getUser();
        // set up the attribute deserializer
        attributeParser = new WebFormAttributeParser(taxaDAO);
        attrDictFact = new LocationAttributeDictionaryFactory();
        // must include all null and LOCATION or any census method attributes sub attributes (Scope null) will not be saved
        Set<AttributeScope> scope = new HashSet<AttributeScope>(AttributeScope.values().length+1);
        scope.add(AttributeScope.LOCATION);
        scope.add(null);
        Map<Attribute, Object> attrNameMap = attrDictFact.createNameKeyDictionary(null, survey, location, null, null, scope, request.getParameterMap());
        Map<Attribute, Object> attrFilenameMap = attrDictFact.createFileKeyDictionary(null, survey, location, null, null, scope, request.getParameterMap());
        AttributeDeserializer attrDeserializer = new AttributeDeserializer(attributeParser);

        Set recAtts = location.getAttributes();
        List<TypedAttributeValue> attrValuesToDelete = new ArrayList<TypedAttributeValue>();
        // get only the location attributes for deserializing
        List<Attribute> surveyLocationAtts = new ArrayList<Attribute>();
        for (Attribute att : survey.getAttributes()) {
            if (AttributeScope.LOCATION.equals(att.getScope())) {
                surveyLocationAtts.add(att);
            }
        }
        attrDeserializer.deserializeAttributes(surveyLocationAtts,  
                                               attrValuesToDelete, recAtts, "", 
                                               attrNameMap, attrFilenameMap, location, 
                                               request.getParameterMap(), request.getFileMap(), currentUser, false, scope, true);
        
        for(TypedAttributeValue ta : attrValuesToDelete) {
            // Must do a save here to sever the link in the join table.
            attributeDAO.save(ta);
            // And then delete.
            attributeDAO.delete(ta);
        }
        return recAtts;
    }
    
    /**
     * Convenience method for updating a location from an HTTP request.
     * @param request The HTTP request
     * @param id The unique identifier of the location in the request
     * @param location The existing location object from the DAO
     * @return The modified Location object
     */
    private Location updateLocation(HttpServletRequest request, String id,
            Location location, int srid) {
        return updateLocation(location, request.getParameter("location_WKT_"+id), request.getParameter("name_"+id), srid);
    }

    /**
     * Convenience method for updating a location from an HTTP request.
     * @param request The HTTP request
     * @param id The unique identifier of the location in the request
     * @param location The existing location object from the DAO
     * @return The modified Location object
     */
    private Location updateLocation(HttpServletRequest request, Location location, int srid) {
        return updateLocation(location, request.getParameter("location_WKT"), 
                              request.getParameter("locationName"),
                              request.getParameter("locationDescription"), srid);
    }

    /**
     * Convenience method for setting the name and location of a Location object
     * to the given wktString/name
     * @param location The Location to update
     * @param wktString The WKT string of the Geometry to set as the location
     * @param name The name of the location
     * @return The modified Location object
     */
    private Location updateLocation(Location location, String wktString, String name, int srid) {
        return updateLocation(location, wktString, name, null, srid);
    }
    
    /**
     * Convenience method for setting the name and location of a Location object
     * to the given wktString/name
     * @param location The Location to update
     * @param wktString The WKT string of the Geometry to set as the location
     * @param name The name of the location
     * @return The modified Location object
     */
    private Location updateLocation(Location location, String wktString, String name, String description, int srid) {
    	SpatialUtil spatialUtil = new SpatialUtilFactory().getLocationUtil(srid);
        Geometry geometry = spatialUtil.createGeometryFromWKT(wktString);
        location.setName(name);
        location.setLocation(geometry);
        location.setDescription(description);
        return location;
    }

    /**
     * Convenience method for creating a new location from an HTTP request
     * @param request The request object
     * @param id The unique identifier of the location in the request
     * @return The newly created Location object
     */
    private Location createNewLocation(HttpServletRequest request, String id, int srid) {
        Location location = new Location();
        return updateLocation(location, request.getParameter("add_location_WKT_"+id), request.getParameter("add_name_"+id), srid);
    }

    /**
     * Convenience method for creating a new location from an HTTP request
     * @param request The request object
     * @return The newly created Location object
     */
    private Location createNewLocation(HttpServletRequest request, int srid) {
        Location location = new Location();
        return updateLocation(location, request.getParameter("location_WKT"), 
                              request.getParameter("locationName"),
                              request.getParameter("locationDescription"), srid);
    }
    
    private Survey getSurvey(Integer rawSurveyId) {
        if(rawSurveyId == null){
            // Do not know which survey to deal with. Bail out.
            throw new HTTPException(HttpServletResponse.SC_NOT_FOUND);
        }
        return surveyDAO.getSurvey(rawSurveyId);
    }

    private Location getLocation(Integer rawLocationId) {
        if(rawLocationId == null){
            // Do not know which survey to deal with. Bail out.
            throw new HTTPException(HttpServletResponse.SC_NOT_FOUND);
        }
        return locationDAO.getLocation(rawLocationId);
    }
    
    @InitBinder
    public void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(
                dateFormat, true));
    }
}
