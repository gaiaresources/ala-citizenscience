from time import mktime, strptime
from datetime import datetime
from csv import reader as csv_reader
from decimal import *
from shapely.geometry.base import BaseGeometry
from shapely import wkt

from pybdrs.javautil import epoch_ms_to_datetime

class LazyPersistent(object):
    """Base class representing all BDRS based lazy fetched objects."""
    def __init__(self, factory, data={}):
        self._factory = factory
        self._data = data

    def _get_or_load(self, model_klazz, key):
        """Retrieves the object of the specified type and data
        dictionary key name. If the object was previously fetched, then
        the cached instance is returned, otherwise the object is fetched
        from the BDRS and returned."""
        p = self._data.get(key)
        if type(p) == int:
            p = self._factory.lazy_load(model_klazz, p)
            self._data[key] = p
        elif type(p) == list:
            if len(p) > 0:
                if type(p[0]) == int:
                    p = self._factory.lazy_load_list(model_klazz, p)
                # else the data has been previously loaded
            # else loaded or not, an empty list is just empty.
        # else it has been loaded previously
        return p

def insert_accessors(klazz, mapping):
    """Monkey patches the specified class with a series of functions that make 
    retrieving foreign relations and converting datatypes easier. For example,
    java dates are specified in ms since epoch but the accessor will return a 
    python datetime.
    """
    for key, datatype in mapping.items():
        if not hasattr(klazz, key):
            if issubclass(datatype, LazyPersistent):
                def innerLazyLoad(self, key=key, datatype=datatype):
                    return self._get_or_load(datatype, key)
                func = innerLazyLoad
            elif issubclass(datatype, datetime):
                def innerDatetimeAccessor(self, key=key, datatype=datatype):
                    d = self._data.get(key)
                    if d is not None and not isinstance(d, datetime):
                        d = epoch_ms_to_datetime(d)
                        self._data[key] = d
                    return d
                func = innerDatetimeAccessor
            elif issubclass(datatype, Decimal):
                def innerDecimalAccessor(self, key=key, datatype=datatype):
                    d = self._data.get(key)
                    if d is not None and not isinstance(d, Decimal):
                        self._data[key] = Decimal(d)
                    return d
                func = innerDecimalAccessor
            elif issubclass(datatype, BaseGeometry):
                def innerGeometryAccessor(self, key=key, datatype=datatype):
                    d = self._data.get(key)
                    if d is not None and not isinstance(d, BaseGeometry):
                        d = wkt.loads(d)
                        self._data[key] = d
                    return d
                func = innerGeometryAccessor
            else:
                def innerAccessor(self, key=key, datatype=datatype):
                    return self._data.get(key)
                func = innerAccessor

            setattr(klazz, key, func)

#################################
# BDRS Enumerations
#################################
ATTRIBUTE_SCOPE_RECORD = "RECORD";
ATTRIBUTE_SCOPE_SURVEY = "SURVEY";
ATTRIBUTE_SCOPE_LOCATION = "LOCATION";

ATTRIBUTE_SCOPE_RECORD_MODERATION = "RECORD_MODERATION";
ATTRIBUTE_SCOPE_SURVEY_MODERATION = "SURVEY_MODERATION";

SURVEYFORMSUBMITACTION_MY_SIGHTINGS = "MY_SIGHTINGS"
SURVEYFORMSUBMITACTION_STAY_ON_FORM = "STAY_ON_FORM"

#################################
# BDRS Models
#################################

class Persistent(LazyPersistent):
    def __init__(self, factory, data={}):
        super(Persistent, self).__init__(factory, data)

insert_accessors(Persistent, {
    'id' : int,
    'weight' : int,
})

class Portal(Persistent):
    def __init__(self, factory, data={}):
        super(Portal, self).__init__(factory, data)

    def __unicode__(self):
        return unicode(self.name())

    def __str__(self):
        return str(self.name())

insert_accessors(Portal, {
    'name' : str,
})

class PortalPersistent(Persistent):
    def __init__(self, factory, data={}):
        super(PortalPersistent, self).__init__(factory, data)

insert_accessors(PortalPersistent, {
    'portal' : Portal,
})

class Metadata(PortalPersistent):
    def __init__(self, factory, data={}):
        super(Metadata, self).__init__(factory, data)
        
    def __unicode__(self):
        return unicode(self.key())

    def __str__(self):
        return str(self.key())

insert_accessors(Metadata, {
    'key' : str,
    'value' : str,
})

class AttributeOption(PortalPersistent):
    def __init__(self, factory, data={}):
        super(AttributeOption, self).__init__(factory, data)
        
    def __unicode__(self):
        return unicode(self.value())

    def __str__(self):
        return str(self.value())

insert_accessors(AttributeOption, {
    'value': str,
})

class Attribute(PortalPersistent):

    ATTRIBUTE_TYPE_INTEGER = 'IN'
    ATTRIBUTE_TYPE_INTEGER_RANGE = 'IR'
    ATTRIBUTE_TYPE_DECIMAL = 'DE'
    
    ATTRIBUTE_TYPE_SHORT_TEXT = 'ST'
    ATTRIBUTE_TYPE_LONG_TEXT = 'TA'
    
    ATTRIBUTE_TYPE_DATE = 'DA'
    ATTRIBUTE_TYPE_TIME = 'TM'
    
    ATTRIBUTE_TYPE_SELECTION = 'SV'
    ATTRIBUTE_TYPE_CHECKBOX = 'SC'
    
    ATTRIBUTE_TYPE_MULTI_CHECKBOX = 'MC'
    ATTRIBUTE_TYPE_MULTI_SELECT = 'MS'

    def __init__(self, factory, data={}):
        super(Attribute, self).__init__(factory, data)
        
    def __unicode__(self):
        return unicode(self.name())

    def __str__(self):
        return str(self.name())

insert_accessors(Attribute, {
    'typeCode' : str,
    'required' : bool,
    'name' : str,
    'description' : str,
    'scope': str,
    'options': AttributeOption,
})


class Taxon(PortalPersistent):
    def __init__(self, factory, data={}):
        super(Taxon, self).__init__(factory, data)

    def __unicode__(self):
        return unicode(self.scientificName())

    def __str__(self):
        return str(self.scientificName())

insert_accessors(Taxon, {
    'scientificNameAndAuthor': str,
    'scientificName': str,
    'commonName': str,
    #'taxonGroup': TaxonGroup,
    #'regions': Set<Region>,
    #'regionNames': Set<String>,
    #'attributes': Set<IndicatorSpeciesAttribute>,
    #'infoItems': List<SpeciesProfile>,
    'parent': Taxon,
    #'rank': TaxonRank,
    'author': str,
    'year': str,
    'source': str,
    'sourceId': str,
    'metadata': Metadata,
})

class Survey(PortalPersistent):
    RECORD_PROPERTY_DEFAULTS = {
        'weight' : 0,
        'description' : '',
        'required' : False,
        'scope' : 'SURVEY',
        'hidden' : False,
    }

    def __init__(self, factory, data={}):
        super(Survey, self).__init__(factory, data)
       
    def __unicode__(self):
        return unicode(self.name())

    def __str__(self):
        return str(self.name())

    def get_record_property_required(self, property_name): 
        key = 'RECORD.%s.REQUIRED' % (property_name[0].upper() + property_name[1:])
        md = self._get_metadata_by_key(key)
        return md.value() == 'true' if md is not None else self.RECORD_PROPERTY_DEFAULTS['required']

    def get_record_property_hidden(self, property_name): 
        key = 'RECORD.%s.HIDDEN' % (property_name[0].upper() + property_name[1:])
        md = self._get_metadata_by_key(key)
        return md.value() == 'true' if md is not None else self.RECORD_PROPERTY_DEFAULTS['hidden']

    def get_record_property_weight(self, property_name):      
        key = 'RECORD.%s.WEIGHT' % (property_name[0].upper() + property_name[1:])
        md = self._get_metadata_by_key(key)
        return int(md.value(), 10) if md is not None else self.RECORD_PROPERTY_DEFAULTS['weight']

    def get_record_property_scope(self, property_name): 
        key = 'RECORD.%s.SCOPE' % (property_name[0].upper() + property_name[1:])
        md = self._get_metadata_by_key(key)
        return md.value() if md is not None else self.RECORD_PROPERTY_DEFAULTS['scope']

    def get_record_property_description(self, property_name): 
        key = 'RECORD.%s.DESCRIPTION' % (property_name[0].upper() + property_name[1:])
        md = self._get_metadata_by_key(key)
        return md.value() if md is not None else self.RECORD_PROPERTY_DEFAULTS['description']

    def _get_metadata_by_key(self, key):
        if not hasattr(self, '__metadata_map'):
            md_map = {}
            for md in self.metadata():
                md_map[md.key()] = md
            self.__metadata_map = md_map
        return self.__metadata_map.get(key)

    def get_form_submit_action(self):
        md = self._get_metadata_by_key('FormSubmitAction')
        return SURVEYFORMSUBMITACTION_MY_SIGHTINGS if md is None else md.value()

    def get_attribute_by_name(self, attribute_name):
        if not hasattr(self, '__attribute_map'):
            attr_map = {}
            for attr in self.attributes():
                attr_map[attr.name()] = attr
        return attr_map.get(attribute_name, None)

insert_accessors(Survey, {
    'name' : str,
    'description' : str,
    'active' : bool,
    'startDate' : datetime,
    'endDate' : datetime,
    'publik' : bool,
    #'customForm' : CustomForm,
    #'locations' : List<Location>,
    #'users' : Set<User>,
    #'groups' : Set<Group>,
    'species' : Taxon,
    'attributes' : Attribute,
    #'censusMethods' : List<CensusMethod>,
    #'baseMapLayers' : List<BaseMapLayer>,
    #'geoMapLayers' : List<SurveyGeoMapLayer>,
    'metadata' : Metadata,
})

class AttributeValue(PortalPersistent):
    def __init__(self, factory, data={}):
        super(AttributeValue, self).__init__(factory, data)

    def get_value(self):
        if self.attribute() is None or self.stringValue() is None:
            return None

        typeCode = self.attribute().typeCode()

        if typeCode in [Attribute.ATTRIBUTE_TYPE_INTEGER, 
                        Attribute.ATTRIBUTE_TYPE_INTEGER_RANGE, 
                        Attribute.ATTRIBUTE_TYPE_DECIMAL]:
            try:
                return Decimal(self.stringValue())
            except decimal.InvalidOperation:
                # Thrown if the string is blank which may happen if the attribute is
                # non mandatory
                return None
        elif typeCode == Attribute.ATTRIBUTE_TYPE_DATE:
            # The following line is overcomplicated and it should be datetime.strptime
            # however a weird python bug, means that strptime can (for some reason) only
            # be invoked successfully exactly one time.
            # http://forum.xbmc.org/showthread.php?tid=112916
            try:
                return datetime.fromtimestamp(mktime(strptime(self.stringValue(), '%d %b %Y')))
            except ValueError:
                # Thrown if the string is blank which may happen if the attribute is
                # non mandatory
                return None
        elif typeCode == Attribute.ATTRIBUTE_TYPE_TIME:
            # Overcomplicated. See note attached to Attribute.ATTRIBUTE_TYPE_DATE
            try:
                return datetime.fromtimestamp(mktime(strptime(self.stringValue(), '%H:%M')))
            except ValueError:
                # Thrown if the string is blank which may happen if the attribute is
                # non mandatory
                return None
        elif typeCode == Attribute.ATTRIBUTE_TYPE_CHECKBOX:
            return self.stringValue() == 'true'
        elif typeCode in [Attribute.ATTRIBUTE_TYPE_MULTI_CHECKBOX, Attribute.ATTRIBUTE_TYPE_MULTI_SELECT]:
            return csv_reader([self.stringValue()], skipinitialspace=True).next()
        else:
            return self.stringValue()

insert_accessors(AttributeValue, {
    'attribute' : Attribute,
    'numericValue' : Decimal,
    'stringValue' : str,
    'dateValue' : datetime,
})

class Record(PortalPersistent):
    DEFAULT_RECORD_DATA = {
        'attributes': [],
        'metadata': [],
    }

    def __init__(self, factory, data=DEFAULT_RECORD_DATA):
        super(Record, self).__init__(factory, data)

    def latitude(self):
        return self.geometry().y if self.geometry() is not None else None

    def longitude(self):
        return self.geometry().x if self.geometry() is not None else None

    def get_attribute_value_by_attribute(self, attribute):
        if attribute is None:
            return None

        if not hasattr(self, '__attribute_value_map'):
            attr_val_map = {}
            for attr_val in self.attributes():
                attr_val_map[attr_val.attribute()] = attr_val
        return attr_val_map.get(attribute, None)

insert_accessors(Record, {
    'survey' : Survey,
    'species' : Taxon,
    #'user' : User,
    #'location' : Location,
    'geometry' : BaseGeometry,
    'accuracyInMeters' : float,
    'held' : bool,
    #'recordVisibility' : RecordVisibility,
    'when' : datetime,
    'time' : datetime,
    'lastDate' : datetime,
    'lastTime' : datetime,
    'notes' : str,
    'firstAppearance' : bool,
    'lastAppearance' : bool,
    'behaviour' : str,
    'habitat' : str,
    'number' : int,
    #'censusMethod' : CensusMethod,
    'parentRecord' : Record,
    'childRecords' : Record,
    'attributes' : AttributeValue,
    #'reviewRequests' : ReviewRequest,
    'metadata' : Metadata,
    #'comments' : Comment,
})