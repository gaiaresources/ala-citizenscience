import json

class Report:
    def content(self, json_params, *args, **kwargs):
        response = bdrs.getResponse()
        response.setContentType(response.HTML_CONTENT_TYPE)
        response.setContent("hello world")
        return
