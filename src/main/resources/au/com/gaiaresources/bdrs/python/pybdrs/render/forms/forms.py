from pybdrs.models import *
from pybdrs.render.widgets import *

class Form(object):
    def __init__(self, bdrs, render_factory, *args, **kwargs):
        self._bdrs = bdrs
        self._render_factory = render_factory

class RecordForm(Form):
    """A form that represents a row in the Record table."""
    DEFAULT_PROPERTY_WIDGET_MAP = {
        'notes': 'textarea',
        'number': 'integer',
        'accuracyInMeters': 'number',
        'gpsAltitude': 'number',
        'when': 'historical_date',
        'time': 'time',
        'species': 'taxon',
        'geometry': 'lat_lon_map'
    }

    RECORD_PREFIX_TMPL = "%d_"

    def __init__(self, bdrs, render_factory, value_map, error_map, record, record_index, *args, **kwargs):
        """Creates a new instance. Note that an additional keyword argument 'survey' containing the 
        Survey that owns the record is mandatory."""
        super(RecordForm, self).__init__(bdrs, render_factory, *args, **kwargs)
        self._record = record
        self._value_map = value_map
        self._error_map = error_map
        self._record_index = record_index
        try: 
            self._survey = kwargs['survey']
        except ValueError, ve:
            # You need the survey to work out which record properties are hidden/required and label text.
            raise Exception, 'Missing named parameter "survey" that is required when creating RecordForms'

    def _record_prefix(self):
        return self.RECORD_PREFIX_TMPL % self._record_index

    def record_prefix_widget(self):
        """Returns a hidden widget containing the prefix appended to each of the current records fields.
        This widget is necessary for the correct deserialization of records."""
        return WIDGET_TYPE_MAP['hidden'](self._bdrs,
                                            widget_id=None, 
                                            name="rowPrefix", 
                                            initial=None,
                                            value=self._record_prefix(), 
                                            label_text=None, 
                                            required=False, 
                                            hidden=True, 
                                            error_text=None)
    def record_id_widget(self):
        """Returns a hidden widget containing the primary key of the record being represented.
        This widget is necessary for the correct deserialization of records."""
        return WIDGET_TYPE_MAP['hidden'](self._bdrs,
                                            widget_id=None, 
                                            name="%srecordId" % self._record_prefix(), 
                                            initial=None,
                                            value=self._record.id(), 
                                            label_text=None, 
                                            required=False, 
                                            hidden=True, 
                                            error_text=None)
        
    def widget(self, property_name, *args, **kwargs):
        """Returns a widget for the specified record property (e.g notes, accuracyInMeters, ...)"""
        try:
            widget_type = kwargs.get('widget_type', RecordForm.DEFAULT_PROPERTY_WIDGET_MAP[property_name])
        except KeyError, ve:
            raise Exception, 'No widget available for Record property with name "%s"' % property_name

        try:
            widget = WIDGET_TYPE_MAP[widget_type]
        except KeyError, ve:
            raise Exception, 'Cannot find a widget named "%s"' % widget_type

        record_prefix = self._record_prefix()
        scope = self._survey.get_record_property_scope(property_name)

        widget_id = property_name
        if scope in [ATTRIBUTE_SCOPE_RECORD, ATTRIBUTE_SCOPE_RECORD_MODERATION]:
            name = "%s%s" % (record_prefix, property_name)
        else:
            name = property_name
            # There always has to be someone
            if name == 'when':
                name = 'date'
                widget_id = 'date'

        value = '' if self._record is None else getattr(self._record, property_name)
        label_text = self._survey.get_record_property_description(property_name)
        required = self._survey.get_record_property_required(property_name)
        hidden = self._survey.get_record_property_hidden(property_name)
        initial = self._value_map.get(name, None)
        if property_name == 'when':
            error_text = self._error_map.get('date', None)
        else:
            error_text = self._error_map.get(property_name, None)
        widget = widget(self._bdrs,
                        widget_id=widget_id, 
                        name=name, 
                        initial=initial,
                        value=value, 
                        label_text=label_text, 
                        required=required, 
                        error_text=error_text,
                        form = self,
                        value_map = self._value_map,
                        error_map = self._error_map,
                        # The above are 'normal' widget parameters.
                        # The ones below are specific to widgets used only by report forms.
                        hidden=hidden, 
                        survey=self._survey, 
                        record=self._record, 
                        property_name=property_name,
                        **kwargs)
        return widget

class AttributeForm(Form):
    """A form representing a single Attribute for a Record."""
    DEFAULT_ATTRIBUTE_TYPE_WIDGET_MAP = {
        Attribute.ATTRIBUTE_TYPE_INTEGER : 'integer',
        Attribute.ATTRIBUTE_TYPE_INTEGER_RANGE : 'integer_range',
        Attribute.ATTRIBUTE_TYPE_DECIMAL : 'number',
        
        Attribute.ATTRIBUTE_TYPE_SHORT_TEXT : 'text',
        Attribute.ATTRIBUTE_TYPE_LONG_TEXT : 'textarea',
        
        Attribute.ATTRIBUTE_TYPE_DATE : 'date',
        Attribute.ATTRIBUTE_TYPE_TIME : 'time',
        
        Attribute.ATTRIBUTE_TYPE_SELECTION : 'select',
        Attribute.ATTRIBUTE_TYPE_CHECKBOX : 'checkbox',
        
        Attribute.ATTRIBUTE_TYPE_MULTI_CHECKBOX : 'multi_checkbox',
        Attribute.ATTRIBUTE_TYPE_MULTI_SELECT : 'multi_select',

        Attribute.ATTRIBUTE_TYPE_SPECIES : 'taxon_attribute',
    }

    def __init__(self, bdrs, render_factory, value_map, error_map, attribute, record_form, *args, **kwargs):
        """Creates a new instance."""
        super(AttributeForm, self).__init__(bdrs, render_factory, *args, **kwargs)
        self._value_map = value_map
        self._error_map = error_map
        self._attribute = attribute
        self._record_form = record_form
        self._widget_arg_handler_map = {
            IntegerRange : self.insert_integer_range_args,
            Select: self.insert_multi_options_args,
            MultiSelect: self.insert_multi_options_args,
            Checkbox: self.insert_checkbox_args,
            MultiCheckbox: self.insert_multi_options_args,
            TaxonAttribute: self.insert_taxon_attribute_args,
        }
        try: 
            self._survey = kwargs['survey']
        except ValueError, ve:
            # You need the survey to work out which species to show on attribute species fields.
            raise Exception, 'Missing named parameter "survey" that is required when creating AttributeForms'

    def widget(self, *args, **kwargs):
        """Creates a widget for the Attribute specified by this form."""
        attr = self._attribute
        attr_type = attr.typeCode()
        try:
            widget_type = kwargs.get('widget_type', AttributeForm.DEFAULT_ATTRIBUTE_TYPE_WIDGET_MAP[attr_type])
        except KeyError, ve:
            #raise Exception, 'No widget available for Attribute with type code "%s"' % attr_type
            self._bdrs.getLogger().warn(str('No custom form widget available for Attribute with type code "%s". Defaulting to "hidden".' % attr_type))
            widget_type = 'hidden'

        try:
            widget = WIDGET_TYPE_MAP[widget_type]
        except KeyError, ve:
            raise Exception, 'Cannot find a widget named "%s"' % widget_type

        name = 'attribute_%d' % attr.id()
        scope = attr.scope()
        if scope in [ATTRIBUTE_SCOPE_RECORD, ATTRIBUTE_SCOPE_RECORD_MODERATION]:
            record_prefix = self._record_form._record_prefix()
            name = '%s%s' % (record_prefix, name)
        
        widget_kwargs = dict(kwargs)
        widget_kwargs['widget_id'] = name
        widget_kwargs['name'] = name

        record = self._record_form._record
        attr_val =  record.get_attribute_value_by_attribute(attr)

        widget_kwargs['value'] = None if attr_val is None else attr_val.get_value()
        widget_kwargs['label_text'] = attr.description()
        widget_kwargs['required'] = attr.required()
        widget_kwargs['initial'] = self._value_map.get(name, None)
        widget_kwargs['error_text'] = self._error_map.get(name, None)
        # add the actual attribute value object

        # Populate with additional widget args as appropriate
        handler = self._widget_arg_handler_map.get(widget, None)
        if callable(handler):
            handler(attr_val, widget_kwargs)

        return widget(self._bdrs, **widget_kwargs)

    def insert_integer_range_args(self, attr_val, widget_kwargs):
        """Inserts the min and max value argument for the integer range widget."""
        vals = []
        for opt in self._attribute.options():
            vals.append(int(opt.value(), 10))

        widget_kwargs[IntegerRange.MAX_INT_KEY] = max(vals)
        widget_kwargs[IntegerRange.MIN_INT_KEY] = min(vals)

    def insert_multi_options_args(self, attr_val, widget_kwargs):
        """Inserts the options argument for the widget."""
        options = []
        for opt in self._attribute.options():
            options.append(opt.value())
        widget_kwargs[MultiOptions.OPTIONS_KEY] = options

    def insert_checkbox_args(self, attr_val, widget_kwargs):
        val = widget_kwargs['value']
        widget_kwargs[Checkbox.CHECKED_KEY] = val == True
        widget_kwargs['value'] = 'true'

    def insert_taxon_attribute_args(self, attr_val, widget_kwargs):
        if attr_val is not None:
            widget_kwargs[TaxonAttribute.SPECIES_KEY] = attr_val.species()
        widget_kwargs[TaxonAttribute.SURVEY_KEY] = self._survey

