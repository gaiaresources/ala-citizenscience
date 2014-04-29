package au.com.gaiaresources.bdrs.controller.webservice;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.method.CensusMethod;
import au.com.gaiaresources.bdrs.model.method.CensusMethodDAO;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.serialization.RecordCsvWriter;
import au.com.gaiaresources.bdrs.service.content.ContentService;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Exports Records in CSV format
 */
@SuppressWarnings("UnusedDeclaration")  // class called by spring url dispatcher
@Controller
public class RecordCsvWebService extends AbstractController {

    public static final String RECORD_CSV_URL = "exportCSV.htm";

    @Autowired
    private SurveyDAO surveyDAO;

    @Autowired
    private CensusMethodDAO cmDAO;

    @Autowired
    private RecordDAO recordDAO;

    @Autowired
    private MetadataDAO metadataDAO;

    private Logger log = Logger.getLogger(getClass());

    @RequestMapping(method = RequestMethod.GET, value = RECORD_CSV_URL)
    public void exportRecordCsv(
            @RequestParam(value=BdrsWebConstants.PARAM_SURVEY_ID, required = false) Integer surveyId,
            @RequestParam(value=BdrsWebConstants.PARAM_CENSUS_METHOD_ID, required = false, defaultValue = "0") Integer cmId,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (surveyId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeText(response, "parameter surveyId is required");
            return;
        }

        Survey survey = surveyDAO.getSurvey(surveyId);

        if (survey == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeText(response,
                    String.format("Survey ID %d does not match a survey in the database", surveyId));
            return;
        }

        CensusMethod cm = cmDAO.get(cmId);

        RedirectionService redirectionService = new RedirectionService(ContentService.getRequestURL(request));
        RecordCsvWriter csvWriter = new RecordCsvWriter(recordDAO, metadataDAO, redirectionService, survey, cm);

        try {
            response.setContentType("text/csv");
            csvWriter.writeRecords(response.getWriter());
        } catch (IOException ioe) {
            log.error("Error writing CSV to response stream", ioe);
            writeText(response, "IO Error writing CSV to response stream");
        }
    }
}
