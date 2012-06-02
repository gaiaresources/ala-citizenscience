from widgets import *

# { widget_name : widget_klazz }
WIDGET_TYPE_MAP = {
    'textarea' : TextArea,
    
    'text': Text,
    'hidden': Hidden,

    'date': Date,
    'historical_date': HistoricalDate,

    'time': Time,

    'taxon': Taxon,
    'taxon_attribute': TaxonAttribute,

    'lat_lon_map': LatLonMap,

    'integer' : Integer,
    'integer_range': IntegerRange,
    'number': Number,

    'select': Select,
    'checkbox': Checkbox,

    'multi_checkbox': MultiCheckbox,
    'multi_select': MultiSelect,
}