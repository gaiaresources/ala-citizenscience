import json
from datetime import datetime
from copy import deepcopy
from Cheetah.Template import Template

from pybdrs.models.factory import PersistentFactory
from pybdrs.models import *

from pybdrs.render.factory import RenderFactory


class Form:
    FORM_SUBMIT_QUERY_PARAM_KEY = 'submit'
    SURVEY_ID_QUERY_PARAM_KEY = 'surveyId'
    RECORD_ID_QUERY_PARAM_KEY = 'recordId'

    def render(self, json_params, *args, **kwargs):
        query_params = json.loads(json_params, strict=False)
        persistent_factory = PersistentFactory(bdrs)
        survey_pk = query_params[self.SURVEY_ID_QUERY_PARAM_KEY][0]
        survey = persistent_factory.lazy_load(Survey, survey_pk)

        deserialize_result = None
        deserialize_result_list = []
        if query_params.has_key(self.FORM_SUBMIT_QUERY_PARAM_KEY):
            deserialize_result = bdrs.deserializeRecord()
            deserialize_result_list = json.loads(deserialize_result.getResults())
            if not json.loads(deserialize_result.hasError()):
                # Save was successful, redirect to listing page if necessary
                if survey.get_form_submit_action() == SURVEYFORMSUBMITACTION_MY_SIGHTINGS:
                    redirect_params = {'survey_id' : survey_pk}
                    response = bdrs.getResponse()
                    response.setRedirect('/map/mySightings.htm', json.dumps(redirect_params))
                    return

        record = None
        if len(query_params.get(self.RECORD_ID_QUERY_PARAM_KEY, [])) > 0:
            record_pk = query_params[self.RECORD_ID_QUERY_PARAM_KEY][0]
            record = persistent_factory.lazy_load(Record, record_pk)
        else:
            record = Record(persistent_factory)

        # Render the data
        tmpl_params = {
            'element_tmpl_path' : bdrs.toAbsolutePath('template/element.tmpl'),
            'survey' : survey,
            'rec' : record,
            'rf' : RenderFactory(bdrs, query_params, deserialize_result_list),
        }

        tmpl = Template(file=bdrs.toAbsolutePath('template/simple.tmpl'), searchList=tmpl_params)
        response = bdrs.getResponse()
        response.setContentType(response.HTML_CONTENT_TYPE)
        response.setContent(str(tmpl))

        return