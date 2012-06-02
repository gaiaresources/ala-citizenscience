package au.com.gaiaresources.bdrs.python;

import au.com.gaiaresources.bdrs.controller.record.*;
import au.com.gaiaresources.bdrs.deserialization.record.*;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.location.LocationDAO;
import au.com.gaiaresources.bdrs.model.location.LocationService;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.portal.PortalDAO;
import au.com.gaiaresources.bdrs.model.python.AbstractPythonRenderable;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOptionDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValueDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.python.deserializer.PyRecordDeserializerResult;
import au.com.gaiaresources.bdrs.python.model.*;
import au.com.gaiaresources.bdrs.python.taxonlib.PyTemporalContext;
import au.com.gaiaresources.bdrs.service.taxonomy.BdrsTaxonLibException;
import au.com.gaiaresources.bdrs.service.taxonomy.TaxonLibSessionFactory;
import au.com.gaiaresources.taxonlib.ITaxonLibSession;
import au.com.gaiaresources.taxonlib.ITemporalContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents the bridge between the Java Virtual Machine and the
 * Python Virtual Machine. It is the responsibility of the bridge to ensure
 * that the Python Report only has read-access to application data. This is
 * generally achieved by ensuring that only JSON encoded strings are passed
 * from the bridge back to the Python report.
 */
public class PyBDRS {
    private Logger log = Logger.getLogger(getClass());

    private HttpServletRequest request;

    private AbstractPythonRenderable renderable;
    private User user;
    private RecordDAO recordDAO;

    private PyResponse response;

    private PySurveyDAO pySurveyDAO;
    private PyTaxaDAO pyTaxaDAO;
    private PyRecordDAO pyRecordDAO;
    private PyCensusMethodDAO pyCensusMethodDAO;
    private PyLocationDAO pyLocationDAO;
    private PyPortalDAO pyPortalDAO;
    private PyAttributeDAO pyAttributeDAO;
    private PyAttributeOptionDAO pyAttributeOptionDAO;
    private PyAttributeValueDAO pyAttributeValueDAO;
    private PyMetadataDAO pyMetadataDAO;

    private FileService fileService;
    private LocationService locationService;

    private TaxonLibSessionFactory taxonLibSessionFactory;
    private ITaxonLibSession taxonLibSession;
    
    private TaxaDAO taxaDAO;

    /**
     * Creates a new instance.
     *
     * @param request           the request from the client.
     * @param locationService   provides facilities to convert WKT strings to Geometry instances
     * @param fileService       retrieves files from the files store.
     * @param renderable        the renderable that will be using this bridge.
     * @param user              the user accessing data.
     * @param surveyDAO         retrieves survey related data.
     * @param censusMethodDAO   retrieves census method related data.
     * @param taxaDAO           retrieves taxon and taxon group related data.
     * @param recordDAO         retrieves record related data.
     * @param portalDAO         retrieves portal related data
     * @param attributeDAO      retrieves attribute related data.
     * @param attributeOptionDAO retrieves attribute option related data.
     * @param attributeValueDAO retrieves attribute value related data
     * @param metadataDAO       retrieves metadata related data
     * @param locationDAO       retrieves location related data.
     * @param taxonLibSessionFactory provides access to taxonlib functionality.
     */
    public PyBDRS(HttpServletRequest request, LocationService locationService,
                  FileService fileService, AbstractPythonRenderable renderable, User user,
                  SurveyDAO surveyDAO, CensusMethodDAO censusMethodDAO,
                  TaxaDAO taxaDAO, RecordDAO recordDAO, PortalDAO portalDAO, AttributeDAO attributeDAO,
                  AttributeOptionDAO attributeOptionDAO, AttributeValueDAO attributeValueDAO,
                  MetadataDAO metadataDAO, LocationDAO locationDAO, TaxonLibSessionFactory taxonLibSessionFactory) {
        this.request = request;
        this.fileService = fileService;
        this.locationService = locationService;
        this.renderable = renderable;

        this.user = user;
        this.recordDAO = recordDAO;
        
        this.taxaDAO = taxaDAO;

        this.response = new PyResponse();

        this.pySurveyDAO = new PySurveyDAO(user, surveyDAO);
        this.pyTaxaDAO = new PyTaxaDAO(user, surveyDAO, taxaDAO);
        this.pyRecordDAO = new PyRecordDAO(user, recordDAO);
        this.pyCensusMethodDAO = new PyCensusMethodDAO(censusMethodDAO);
        this.pyLocationDAO = new PyLocationDAO(locationDAO, surveyDAO);

        this.pyPortalDAO = new PyPortalDAO(portalDAO);
        this.pyAttributeDAO = new PyAttributeDAO(attributeDAO);
        this.pyAttributeOptionDAO = new PyAttributeOptionDAO(attributeOptionDAO);
        this.pyAttributeValueDAO = new PyAttributeValueDAO(attributeValueDAO);
        this.pyMetadataDAO = new PyMetadataDAO(metadataDAO);

        this.taxonLibSessionFactory = taxonLibSessionFactory;
    }

    /**
     * Returns the portion of the request URI that indicates the
     * context of the request.
     *
     * @return the portion of the request URI that indicates the
     *         context of the request.
     */
    public String getContextPath() {
        return this.request.getContextPath();
    }

    /**
     * Returns the host name of the Internet Protocol (IP) interface on which
     * the request was received.
     *
     * @return a <code>String</code> containing the IP address on which the
     *         request was received.
     */
    public String getLocalName() {
        return this.request.getLocalName();
    }

    /**
     * Returns the Internet Protocol (IP) port number of the interface
     * on which the request was received.
     *
     * @return an integer specifying the port number
     */
    public int getLocalPort() {
        return this.request.getLocalPort();
    }

    /**
     * Returns the python wrapped for the {@link SurveyDAO}
     *
     * @return the pySurveyDAO the python wrapped {@link SurveyDAO}
     */
    public PySurveyDAO getSurveyDAO() {
        return pySurveyDAO;
    }

    /**
     * Returns the python wrapped {@link TaxaDAO}
     *
     * @return the pyTaxaDAO the python wrapped {@link TaxaDAO}
     */
    public PyTaxaDAO getTaxaDAO() {
        return pyTaxaDAO;
    }

    /**
     * Returns the python wrapped {@link RecordDAO}
     *
     * @return the pyRecordDAO the python wrapped {@link RecordDAO}
     */
    public PyRecordDAO getRecordDAO() {
        return pyRecordDAO;
    }

    /**
     * Returns the python wrapped {@link CensusMethodDAO}
     *
     * @return the user the python wrapped {@link CensusMethodDAO}
     */
    public PyCensusMethodDAO getCensusMethodDAO() {
        return pyCensusMethodDAO;
    }

    /**
     * Returns the python wrapped {@link LocationDAO}
     *
     * @return the user the python wrapped {@link LocationDAO}
     */
    public PyLocationDAO getLocationDAO() {
        return pyLocationDAO;
    }

    /**
     * Returns the python wrapped {@link PortalDAO}
     *
     * @return the user the python wrapped {@link PortalDAO}
     */
    public PyPortalDAO getPortalDAO() {
        return pyPortalDAO;
    }

    /**
     * Returns the python wrapped {@link MetadataDAO}
     *
     * @return the user the python wrapped {@link MetadataDAO}
     */
    public PyMetadataDAO getMetadataDAO() {
        return pyMetadataDAO;
    }

    /**
     * Returns the python wrapped {@link AttributeDAO}
     *
     * @return the user the python wrapped {@link AttributeDAO}
     */
    public PyAttributeDAO getAttributeDAO() {
        return pyAttributeDAO;
    }

    /**
     * Returns the python wrapped {@link AttributeOptionDAO}
     *
     * @return the user the python wrapped {@link AttributeOptionDAO}
     */
    public PyAttributeOptionDAO getAttributeOptionDAO() {
        return pyAttributeOptionDAO;
    }

    /**
     * Returns the python wrapped {@link AttributeValueDAO}
     *
     * @return the user the python wrapped {@link AttributeValueDAO}
     */
    public PyAttributeValueDAO getAttributeValueDAO() {
        return pyAttributeValueDAO;
    }

    /**
     * Returns the python wrapped {@link ITemporalContext}
     *
     * @param dateString The date of the temporal context in yyyy-MM-dd format.
     * @return the requested wrapped temporal context.
     * @throws Exception
     */
    public PyTemporalContext getTaxonLibTemporalContext(String dateString) throws ParseException, BdrsTaxonLibException {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = isoFormat.parse(dateString);
        ITemporalContext temporalContext = getTaxonLibSession().getTemporalContext(date);
        return new PyTemporalContext(temporalContext);
    }

    /**
     * Returns a json serialized representation of the logged in {@link User}
     *
     * @return the user a json serialized representation of the logged in {@link User}
     */
    public String getUser() {
        if (user == null) {
            return null;
        }

        Map<String, Object> flat = user.flatten();
        flat.put("registrationKey", user.getRegistrationKey());
        return JSONObject.fromMapToString(flat);
    }

    /**
     * Returns a representation of the server response to the client.
     *
     * @return the response a representation of the server response to the browser.
     */
    public PyResponse getResponse() {
        return response;
    }

    /**
     * Returns an absolute path to a file specified by the relative path assuming
     * that the path was relative to the base report directory.
     * <p/>
     * Intended to be used by reports that need to retrieve files from the
     * report directory such as templates.
     *
     * @param relativePath the relative path to the desired file from the report
     *                     directory.
     * @return an absolute path to the file specified by the relative path.
     */
    public String toAbsolutePath(String relativePath) {
        try {
            File reportDir = fileService.getTargetDirectory(renderable, renderable.getContentDir(), false);
            return FilenameUtils.concat(reportDir.getAbsolutePath(), relativePath);
        } catch (IOException ioe) {
            // This cannot happen
            log.error("Unable to resolve absolute path to report.", ioe);
            throw new IllegalStateException(ioe);
        }
    }

    /**
     * Exposes the logger to the python report
     *
     * @return
     */
    public Logger getLogger() {
        return log;
    }

    /**
     * Cleans up any open sessions that may have been open in the lifetime of this
     * PyBDRS object.
     */
    public void close() {
        if (taxonLibSession != null) {
            taxonLibSession.close();
        }
    }

    /**
     * Lazy initialises and returns taxonlib session
     *
     * @return
     * @throws BdrsTaxonLibException Error initialising taxon lib session
     */
    private ITaxonLibSession getTaxonLibSession() throws BdrsTaxonLibException {
        if (taxonLibSession == null) {
            try {
                taxonLibSession = taxonLibSessionFactory.getSession();
            } catch (Exception e) {
                throw new BdrsTaxonLibException("Unable to initialise TaxonLib session : " + e.getMessage(), e);
            }
        }
        return taxonLibSession;
    }

    /**
     * Deserialises records in the POST dictionary of the current request.
     * @return the result of the deserialisation process.
     * @throws IOException thrown if an error has occured.
     * @throws ParseException thrown if an error has occured.
     */
    public PyRecordDeserializerResult deserializeRecord() throws IOException, ParseException {
        MultipartHttpServletRequest multipartRequest;
        if(request instanceof MultipartHttpServletRequest) {
            multipartRequest = (MultipartHttpServletRequest) request;
        } else {
            // It may be possible to detect if the form is not a multipart form and pass an
            // empty file map to the record entry transformer.
            throw new IllegalArgumentException("Deserialization of Records is only available for multipart encoded forms.");
        }
        
        RecordKeyLookup lookup = new TrackerFormRecordKeyLookup();
        SingleSiteFormToRecordEntryTransformer transformer = new SingleSiteFormToRecordEntryTransformer(locationService);
        SingleSiteFormAttributeDictionaryFactory adf = new SingleSiteFormAttributeDictionaryFactory();
        AttributeParser parser = new WebFormAttributeParser(taxaDAO);

        RecordDeserializer rds = new RecordDeserializer(lookup, adf, parser);

        String[] rowIds = multipartRequest.getParameterValues(SingleSiteController.PARAM_ROW_PREFIX);
        if(rowIds == null) {
            rowIds = new String[] {};
        }

        List<RecordEntry> entries = transformer.httpRequestParamToRecordMap(multipartRequest.getParameterMap(), multipartRequest.getFileMap(), rowIds);
        return new PyRecordDeserializerResult(request, recordDAO, rds.deserialize(user, entries));
    }
}
