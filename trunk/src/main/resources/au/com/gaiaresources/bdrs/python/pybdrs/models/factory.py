import json

from models import *

class PersistentFactory:
    """Performs the lazy fetching, cacheing and instantiation of model objects"""

    def __init__(self, bdrs):
        """Creates a new instance"""

        self._bdrs = bdrs
        # { klazz : { pk: instance, ...}, ...}
        self._cache = {}
        self._lazy_load_handler_map = {
            Survey : self.lazy_load_survey,
            Portal: self.lazy_load_portal,
            Attribute: self.lazy_load_attribute,
            AttributeOption: self.lazy_load_attribute_option,
            AttributeValue: self.lazy_load_attribute_value,
            Metadata: self.lazy_load_metadata,
            Record: self.lazy_load_record,
            Taxon: self.lazy_load_taxon,
        }

    def lazy_load_list(self, model_klazz, pk_list):
        """Lazy loads a list of primary keys of the specified type."""
        instance_list = []
        for pk in pk_list:
            instance_list.append(self.lazy_load(model_klazz, pk))
        return instance_list

    def lazy_load(self, model_klazz, pk):
        """Lazy loads the model with the specified type and primary key."""
        instance = self._cache.get(model_klazz, {}).get(pk)
        if instance is None:
            try: 
                instance = self._lazy_load_handler_map[model_klazz](pk)
                self._cache.setdefault(model_klazz, {})[pk] = instance
            except KeyError, ke:
                msg = 'No lazy load handler available for %s' % model_klazz
                self._bdrs.getLogger().error(msg)
                raise KeyError, msg
        return instance

    def lazy_load_survey(self, pk):
        """Lazy loads the Survey with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getSurveyDAO().getById(pk)
        return Survey(self, json.loads(data, strict=False))

    def lazy_load_portal(self, pk):
        """Lazy loads the Portal with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getPortalDAO().getById(pk)
        return Portal(self, json.loads(data, strict=False))

    def lazy_load_attribute(self, pk):
        """Lazy loads the Attribute with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getAttributeDAO().getById(pk)
        return Attribute(self, json.loads(data, strict=False))

    def lazy_load_attribute_option(self, pk):
        """Lazy loads the Attribute Option with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getAttributeOptionDAO().getById(pk)
        return AttributeOption(self, json.loads(data, strict=False))

    def lazy_load_attribute_value(self, pk):
        """Lazy loads the AttributeValue with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getAttributeValueDAO().getById(pk)
        return AttributeValue(self, json.loads(data, strict=False))

    def lazy_load_metadata(self, pk):
        """Lazy loads the Metadata with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getMetadataDAO().getById(pk)
        return Metadata(self, json.loads(data, strict=False))        

    def lazy_load_record(self, pk):
        """Lazy loads the Record with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getRecordDAO().getById(pk)
        return Record(self, json.loads(data, strict=False))        

    def lazy_load_taxon(self, pk):
        """Lazy loads the Taxon (Indicator Species) with the specified primary key."""
        pk = int(pk, 10) if type(pk) == str or type(pk) == unicode else pk
        data = self._bdrs.getTaxaDAO().getById(pk)
        return Taxon(self, json.loads(data, strict=False))   