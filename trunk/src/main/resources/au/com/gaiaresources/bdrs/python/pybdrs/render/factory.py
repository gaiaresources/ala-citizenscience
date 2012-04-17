from widgets import *
from forms import *

class RenderFactory:
    def __init__(self, bdrs, query_params, deserialized_result_list = []):
        self._bdrs = bdrs
        self._query_params = query_params
        self._deserialized_result_list = deserialized_result_list

        self._record_row_index = 0

        self._form_creation_handler_map = {
            Record : self.create_record_form,
            Attribute: self.create_attribute_form,
        }

    def create_form(self, instance, *args, **kwargs):
        creation_handler_func = self._form_creation_handler_map.get(type(instance), None)
        if creation_handler_func is None:
            raise Exception, 'No form registered for %s' % type(instance)
        else:
            return creation_handler_func(instance, *args, **kwargs)

    def create_record_form(self, instance, *args, **kwargs):
        row_index = self._record_row_index

        deserialized_result = {}
        if len(self._deserialized_result_list) > row_index:
            deserialized_result = self._deserialized_result_list[row_index] 

        error_map = deserialized_result.get('errorMap', {})
        value_map = self._query_params
        self._record_row_index += 1

        return RecordForm(self._bdrs, self, value_map, error_map,
                            instance, row_index, *args, **kwargs)

    def create_attribute_form(self, attribute_instance, record_form, *args, **kwargs):
        row_index = record_form._record_index
        deserialized_result = {}
        if len(self._deserialized_result_list) > row_index:
            deserialized_result = self._deserialized_result_list[row_index] 

        error_map = deserialized_result.get('errorMap', {})
        value_map = self._query_params

        return AttributeForm(self._bdrs, self, value_map, error_map, 
                                attribute_instance, record_form, *args, **kwargs)