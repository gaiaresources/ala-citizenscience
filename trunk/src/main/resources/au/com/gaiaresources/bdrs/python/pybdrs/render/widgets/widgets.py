import os.path
from datetime import datetime
from urllib import urlencode

from Cheetah.Template import Template
from Cheetah.Filters import WebSafe

def get_template_dir():
    import pybdrs
    import inspect
    head, tail = os.path.split(inspect.getsourcefile(pybdrs))
    return os.path.join(head, 'templates')

TEMPLATE_DIR = get_template_dir()

class Widget(dict):
    """Represents common behaviours for all form elements (inputs, selects, textareas, radios etc).
    
    Widgets extend dictionary which are then passed to the template. This facility allows 
    keyword arguments to be transparently passed between forms and the template.

    """
    WIDGET_CLASS_KEY = 'widget_class'
    WIDGET_VALIDATION_CLASS_KEY = 'widget_validation_class'
    
    VALUE_REQUIRED_KEY = 'required'
    VALUE_REQUIRED_DEFAULT = False
    VALUE_REQUIRED_CLASS_NAME = 'required'

    WIDGET_HIDDEN_DEFAULT = False
    WIDGET_READONLY_KEY = 'readonly'
    WIDGET_READONLY_DEFAULT = False

    # The WIDGET_VALUE_KEY typically provides the value associated with the
    # underlying model.
    WIDGET_VALUE_KEY = 'value'

    # The WIDGET_VALUE_OVERRIDE_KEY typically provides the value for this
    # widget if there has been a server side validation error. This is the 
    # value that was previously entered on the form but has not yet been 
    # persisted.
    WIDGET_VALUE_OVERRIDE_KEY = 'initial'

    WIDGET_NAME_KEY = 'name'
    WIDGET_STYLE_KEY = 'style'

    def __init__(self, bdrs, **kwargs):
        super(Widget, self).__init__(kwargs)
        self._bdrs = bdrs
        self.setdefault('widget_id', None)
        self.setdefault('name', None)
        self.setdefault('style', None)
        self.setdefault('initial', None)
        self.setdefault('value', None) 
        self.setdefault('label_text', None) 
        self.setdefault('required', Widget.VALUE_REQUIRED_DEFAULT)
        self.setdefault('hidden', Widget.WIDGET_HIDDEN_DEFAULT)
        self.setdefault('error_text', None)
        self.setdefault(self.WIDGET_READONLY_KEY, Widget.WIDGET_READONLY_DEFAULT)

        self._form = self.pop('form', None)
        self._value_map = self.pop('value_map', {})
        self._error_map = self.pop('error_map', {})
       
        self.setdefault(Widget.WIDGET_CLASS_KEY, [])
        self.setdefault(Widget.WIDGET_VALIDATION_CLASS_KEY, [])
        self.init_class_names()
        
        self.widget_value()

    def init_class_names(self):
        """Initialises the classname for the widget. The base implementation of this function
        will add the 'required' class if the required parameter is set otherwise it will
        use the default value, Widget.VALUE_REQUIRED_DEFAULT.
        """
        required = self.get(Widget.VALUE_REQUIRED_KEY, Widget.VALUE_REQUIRED_DEFAULT)
        if required:
            self.add_widget_validation_class(Widget.VALUE_REQUIRED_CLASS_NAME)

    def label(self):
        """Returns the associated HTML label snippet for this widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/label.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

    def error(self):
        """Returns the associated HTML error snippet for this widget if there is one."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/error.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

    def script(self):
        """Returns any javascript necessary for the correct operation of this widget."""
        return None

    def widget_name(self):
        """Returns the name of this widget."""
        return self.get(Widget.WIDGET_NAME_KEY, None)

    def is_readonly(self):
        """Returns the readonly state of this widget."""
        return self.get(Widget.WIDGET_READONLY_KEY, self.WIDGET_READONLY_DEFAULT)

    def widget_style(self):
        """Returns the style of this widget."""
        return self.get(Widget.WIDGET_STYLE_KEY, None)

    def widget_value(self):
        """Returns the value of this widget. This function will attempt to first return an override value
        if it has been specified. This is usually the case if server validation has failed and the corresponding
        override value is the value entered on the form but not yet persisted. If an override value is not specified
        the function will attempt to return the underlying model value if present, otherwise None.
        """
        override = self.get(Widget.WIDGET_VALUE_OVERRIDE_KEY, None)
        if override is not None:
            # the override value is a list because it gets populated via the post dictionary
            val = override[0] if len(override) == 1 else override
        else:
            # the value is a callable because it is populated via the model accessor
            val = self.get(Widget.WIDGET_VALUE_KEY, None)
            if callable(val):
                val = val()
        return val

    def add_widget_class(self, class_name):
        """Adds a HTML class name to the widget."""
        if class_name is not None and len(class_name) > 0:
            self[Widget.WIDGET_CLASS_KEY].append(class_name)

    def add_widget_validation_class(self, class_name):
        """Adds a HTML class name used for validation to the widget"""
        if class_name is not None and len(class_name) > 0:
            self[Widget.WIDGET_VALIDATION_CLASS_KEY].append(class_name)

class Input(Widget):
    """Represents all HTML input elements."""

    INPUT_TYPE_KEY = 'widget_input_type'

    def __init__(self, bdrs, **kwargs):
        """Creates a new Input if the type specified by kwarg[Input.INPUT_TYPE_KEY] or 'text'
        if the input type is not specified."""
        super(Input, self).__init__(bdrs, **kwargs)
        self.setdefault(Input.INPUT_TYPE_KEY, 'text')

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/input.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

class Text(Input):
    """Represents an <input type="text"/>"""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'text')
        super(Text, self).__init__(bdrs, **kwargs)

class Hidden(Text):
    """Represents an <input type="hidden"/>"""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'hidden')
        super(Hidden, self).__init__(bdrs, **kwargs)

class Integer(Text):
    """Represents an <input type="text"/> that only accepts integers"""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'text')
        super(Integer, self).__init__(bdrs, **kwargs)
        self.add_widget_validation_class('integerOrBlank')

class IntegerRange(Integer):
    MAX_INT_KEY = 'max_int'
    MAX_INT_DEFAULT = 10
    
    MIN_INT_KEY = 'min_int'
    MIN_INT_DEFAULT = 0

    """Represents an <input type="text"/> that only accepts integers within a range."""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'text')
        super(IntegerRange, self).__init__(bdrs, **kwargs)
        kwargs.setdefault(IntegerRange.MAX_INT_KEY, IntegerRange.MAX_INT_DEFAULT)
        kwargs.setdefault(IntegerRange.MIN_INT_KEY, IntegerRange.MIN_INT_DEFAULT)

        self.add_widget_validation_class('rangeOrBlank(%d, %d)' % (self[IntegerRange.MIN_INT_KEY], self[IntegerRange.MAX_INT_KEY],))

class Number(Text):
    """Represents an <input type="text"/> that only accepts decimal values"""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'text')
        super(Number, self).__init__(bdrs, **kwargs)
        self.add_widget_validation_class('numberOrBlank')

class Checkbox(Input):
    ONCHANGE_KEY = 'onchange'
    CHECKED_KEY = 'checked'
    CHECKED_DEFAULT = False

    """Represents a single <input type="checkbox"/>"""
    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        kwargs.setdefault(Input.INPUT_TYPE_KEY, 'checkbox')
        kwargs.setdefault(Checkbox.CHECKED_KEY, Checkbox.CHECKED_DEFAULT)
        kwargs.setdefault(Checkbox.ONCHANGE_KEY, None)
        super(Checkbox, self).__init__(bdrs, **kwargs)

    def widget(self):   
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/checkbox.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

class FormattedDateTime(Text):
    """Represents an <input type="text"/> that shows a datetime value."""
    DATETIME_FORMAT_KEY = 'datetime_format'
    DEFAULT_DATETIME_FORMAT = '%x %X'

    def __init__(self, bdrs, **kwargs):
        super(FormattedDateTime, self).__init__(bdrs, **kwargs)
        self.datetime_format = kwargs.get(FormattedDateTime.DATETIME_FORMAT_KEY, FormattedDateTime.DEFAULT_DATETIME_FORMAT)
        self._post_process_value()

    def _post_process_value(self):
        """Post processing of the datetime value to the appropriate string format (dd mmm yy)"""
        try:
            v = self['value']
            value = v() if callable(v) else v
            if value is not None:
                if type(value) == datetime:
                    self['value'] = value.strftime(self.datetime_format)
                else:
                    raise Exception, 'Date widget value must be a datetime.datetime object'
        except KeyError, ke:
            # do nothing, no value supplied
            pass

class Date(FormattedDateTime):
    """Represents an <input type="text"/> with a date picker popup when focussed."""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(FormattedDateTime.DATETIME_FORMAT_KEY, '%d %b %Y')
        super(Date, self).__init__(bdrs, **kwargs)

    def init_class_names(self):
        """Initialises the classes for this widget including a special "datepicker" class name
        that will cause an event handler to be attached."""
        super(Date, self).init_class_names()
        self.add_widget_class('datepicker')


class HistoricalDate(FormattedDateTime):
    """Represents an <input type="text"/> with a date picker popup for before and including today."""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(FormattedDateTime.DATETIME_FORMAT_KEY, '%d %b %Y')
        super(HistoricalDate, self).__init__(bdrs, **kwargs)

    def init_class_names(self):
        """Initialises the classes for this widget including a special "datepicker_historical" class name
        that will cause an event handler to be attached."""
        super(HistoricalDate, self).init_class_names()
        self.add_widget_class('datepicker_historical')

class Time(FormattedDateTime):
    """Represents an <input type="text"/> with a time picker popup when focussed."""
    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(FormattedDateTime.DATETIME_FORMAT_KEY, '%H:%M')
        super(Time, self).__init__(bdrs, **kwargs)

    def init_class_names(self):
        """Initialises the classes for this widget including a special "timepicker" class name
        that will cause an event handler to be attached."""
        super(Time, self).init_class_names()
        self.add_widget_class('timepicker')

class TextArea(Widget):
    """Represents an <textarea></textarea>"""

    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        super(TextArea, self).__init__(bdrs, **kwargs)

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/textarea.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

class MultiOptions(Widget):
    OPTIONS_KEY = 'options'
    OPTIONS_DEFAULT = []

    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        kwargs.setdefault(MultiOptions.OPTIONS_KEY, MultiOptions.OPTIONS_DEFAULT)
        super(MultiOptions, self).__init__(bdrs, **kwargs)

    def widget(self):
        raise NotImplementedError

class Select(MultiOptions):
    """Represents an <select></select>"""

    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        super(Select, self).__init__(bdrs, **kwargs)

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/select.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

class MultiSelect(Select):
    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        super(MultiSelect, self).__init__(bdrs, **kwargs)

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/select_multiple.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

class MultiCheckbox(MultiOptions):
    """Represents a series of <input type="checkbox"/> elements"""

    # Arguably a bad idea, the script is here because the alternative would be to put it in the template,
    # however it doesn't have any substitutions. Could put it in a css file, however that would be (yet another)
    # dependency.
    VALIDATOR_STYLE = 'visibility:hidden;height:0em;width:0em;margin:0px;padding:0px;'

    def __init__(self, bdrs, **kwargs):
        """Creates a new instance."""
        super(MultiCheckbox, self).__init__(bdrs, **kwargs)

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        #filter=WebSafe, 
        tmpl = Template(file=os.path.join(TEMPLATE_DIR, 'widgets/multi_checkbox.tmpl'), searchList={ 'widget': self })
        return str(tmpl)
        
    def validator(self):
        # encode the existing selection
        selected_opts = [] if self.widget_value() is None  else self.widget_value()
        to_encode = []
        name = self['name']
        for sel_opt in selected_opts:
            to_encode.append((name, sel_opt,))
        encoded_value = urlencode(to_encode)

        return Text(    self._bdrs,
                        widget_id=name,
                        name=None,
                        initial=None,
                        value=encoded_value, 
                        label_text=None, 
                        required=self['required'], 
                        hidden=self['hidden'], 
                        error_text=self['error_text'],
                        form = self._form,
                        value_map = self._value_map,
                        style=MultiCheckbox.VALIDATOR_STYLE,)

    def widget_list(self):
        widgets = []
        onchange = str(Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/multi_checkbox_onchange.tmpl'), searchList={ 'widget': self }))
        options = self[MultiOptions.OPTIONS_KEY]
        selected_opts = [] if self.widget_value() is None else self.widget_value()
        for index in range(0, len(options), 1):
            opt = options[index]
            widgets.append(Checkbox(self._bdrs,
                                    widget_id="%s_%d" % (self['name'], index,), 
                                    name=self['name'], 
                                    initial=None,
                                    value=opt,
                                    checked=opt in selected_opts, 
                                    label_text=opt, 
                                    # Required validation handled by the outer widget.
                                    onchange=onchange,
                                    required=False, 
                                    hidden=False, 
                                    error_text=None,
                                    form = self._form,
                                    value_map = self._value_map,))
        return widgets


#########################################################################
# Complex Widget Types
#########################################################################
       
class Taxon(Widget):
    """Represents an autocomplete field for taxonomic selection."""
    SHOW_SCIENTIFIC_NAME_KEY = 'show_scientific_name'
    SHOW_SCIENTIFIC_NAME_DEFAULT = True

    def __init__(self, bdrs, **kwargs):
        kwargs.setdefault(Taxon.SHOW_SCIENTIFIC_NAME_KEY, Taxon.SHOW_SCIENTIFIC_NAME_DEFAULT)
        super(Taxon, self).__init__(bdrs, **kwargs)
        self.add_widget_class('speciesIdInput')

    def widget(self):
        """Returns the HTML snippet representing the widget."""
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/taxon.tmpl'), searchList={ 'widget': self })
        return str(tmpl)

    def script(self):
        """Returns the Javascript block (without script tags) that accompanies the widget. """
        tmpl = Template(filter=WebSafe, file=os.path.join(TEMPLATE_DIR, 'widgets/taxon_script.tmpl'), searchList={ 'widget': self })
        return str(tmpl)        

    def widget_value(self):
        """Returns the primary key of the taxon if there is one"""
        # val is either a Taxon or a String if there was a server side validation error
        val = super(Taxon, self).widget_value()
        try:
            return val.id()
        except AttributeError:
            return val

    def survey_species_search_name(self):
        """Returns the name of the widget where the user types in the search text."""
        return '%ssurvey_species_search' % self._form._record_prefix()

    def survey_species_search_value(self):
        """Returns the value that goes in the search widget."""
        if self._value_map.has_key(self.survey_species_search_name()):
            return self._value_map[self.survey_species_search_name()][0]
        else:
            # In this case, the widget_value is a Taxon instance because
            # if this was a server validation error workflow, the survey_species_search_value 
            # will be set in the value_map
            taxon = super(Taxon, self).widget_value()
            return self.taxon_name(taxon)

    def taxon_name(self, taxon):
        """Returns the name of the specified taxon, depending if the widget is configured
        to show scientific names or common names."""
        if taxon is None:
            return None

        try:
            if self[Taxon.SHOW_SCIENTIFIC_NAME_KEY]:
                return taxon.scientificName()
            else:
                return taxon.commonName()
        except AttributeError:
            return str(taxon)

class LatLonMap(Widget):
    """Represents a latitude input, longitude input and a map for the geometry."""

    def __init__(self, bdrs, **kwargs):
        super(LatLonMap, self).__init__(bdrs, **kwargs)
        # Having a record and survey are not optional for this widget
        self._record = kwargs['record']
        self._survey = kwargs['survey']

    def _coordinate(self, label_text, property_name, range_min, range_max):
        value = '' if self._record is None else getattr(self._record, property_name)
        #label_text = self._survey.get_record_property_description(property_name)
        label_text = label_text
        required = self._survey.get_record_property_required('point')
        hidden = self._survey.get_record_property_hidden('point')

        name = property_name
        initial = self._value_map.get(name, None)
        error_text = self._error_map.get(property_name, None)

        widget = Text(  self._bdrs,
                        widget_id=property_name, 
                        name=name, 
                        initial=initial,
                        value=value, 
                        label_text=label_text, 
                        required=required, 
                        hidden=hidden, 
                        error_text=error_text,
                        form = self._form,
                        value_map = self._value_map,
                        # The above are 'normal' widget parameters.
                        # The ones below are specific to widgets used only by report forms.
                        survey=self._survey, 
                        record=self._record, 
                        property_name=property_name)

        widget.add_widget_validation_class('rangeOrBlank(%d,%d)' % (range_min, range_max,))
        widget.add_widget_validation_class('numberOrBlank')
        return widget

    def latitude(self):
        return self._coordinate('Latitude', 'latitude', -90, 90)

    def longitude(self):
        return self._coordinate('Longitude', 'longitude', -180, 180)

    def map(self):
        """Returns the HTML snippet representing the map."""
        raise NotImplementedError

    def map_script(self):
        """Returns the Javascript (without script tags) that accompanies the map."""
        raise NotImplementedError