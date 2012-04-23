import os
try:
    import json
except ImportError:
    import simplejson as json
from Cheetah.Template import Template
from datetime import datetime
from datetime import timedelta
import time

class Report:
    """Provides a taxon tree change report between 2 dates.
    """

    PARAM_START_DATE = 'startDate'
    PARAM_END_DATE = 'endDate'
    PARAM_SUBMIT_FORM = 'submitReportParams'
    PARAM_ITEMS_PER_PAGE = 'itemsPerPage'
    PARAM_PAGE_NUMBER = 'pageNumber'
    PARAM_GENERATE_REPORT = 'generateReport'
    PARAM_PAGE_COUNT = 'pageCount'
    PARAM_CONTEXT_PATH = 'contextPath'

    def content(self, json_params, *args, **kwargs):

        log = bdrs.getLogger()

        params = json.loads(json_params, strict=False)

        tmpl_params = {}

        tmpl_params[self.PARAM_CONTEXT_PATH] = bdrs.getContextPath()

        if self.PARAM_SUBMIT_FORM in params.keys():
            
            format = '%d %b %Y'

            tmpl_params[self.PARAM_GENERATE_REPORT] = True
            tmpl_params[self.PARAM_START_DATE] = params[self.PARAM_START_DATE][0]
            tmpl_params[self.PARAM_END_DATE] = params[self.PARAM_END_DATE][0]
            itemsPerPage = int(params[self.PARAM_ITEMS_PER_PAGE][0])
            tmpl_params[self.PARAM_ITEMS_PER_PAGE] = itemsPerPage            

            pageNumber = 1
            if self.PARAM_PAGE_NUMBER in params.keys():
                pageNumber = int(params[self.PARAM_PAGE_NUMBER][0])

            tmpl_params[self.PARAM_PAGE_NUMBER] = pageNumber

            startDate = time.strptime(params[self.PARAM_START_DATE][0], format)
            endDate = time.strptime(params[self.PARAM_END_DATE][0], format)

            # convert to datetime objects so we can use isoformat()
            startDate = datetime(*(startDate[0:6]))
            endDate = datetime(*(endDate[0:6]))
            # move the end date so the end date is inclusive
            endDate = endDate + timedelta(days=1)

            context1 = bdrs.getTaxonLibTemporalContext(startDate.isoformat())
            context2 = bdrs.getTaxonLibTemporalContext(endDate.isoformat())
            taxaDAO = bdrs.getTaxaDAO()

            offset = (pageNumber - 1)*itemsPerPage
            pagedResultJson = context1.getJunctionByDate(startDate.isoformat(), endDate.isoformat(), offset, itemsPerPage)

            pagedResult = json.loads(pagedResultJson)
        
            result = []
            for j in pagedResult['list']:
                oldConcept = json.loads(context1.getConceptById(j['oldTaxonConceptId']))
                newConcept = json.loads(context2.getConceptById(j['newTaxonConceptId']))
                j['oldConcept'] = oldConcept
                j['newConcept'] = newConcept
                j['changeDate'] = time.strftime(format, time.localtime(j['changeDate']/1000))
                oldSpecies = json.loads(taxaDAO.getTaxaBySourceId(str(oldConcept['source']), str(oldConcept['name']['id'])))
                newSpecies = json.loads(taxaDAO.getTaxaBySourceId(str(newConcept['source']), str(newConcept['name']['id'])))
                j['oldSpecies'] = oldSpecies
                j['newSpecies'] = newSpecies
                result.append(j)

            tmpl_params['junctions'] = result

            pageCount = (int(pagedResult['count'])/itemsPerPage) + 1
            tmpl_params[self.PARAM_PAGE_COUNT] = pageCount

        else:
            tmpl_params[self.PARAM_ITEMS_PER_PAGE] = 10

        tmpl = Template(file=bdrs.toAbsolutePath('template/taxon_diff.tmpl'), searchList=tmpl_params)
        response = bdrs.getResponse()
        response.setContentType(response.HTML_CONTENT_TYPE)
        response.setContent(str(tmpl))

        return
