try:
    import json
except ImportError:
    import simplejson as json

from decimal import Decimal
from datetime import datetime
import time

from django.db.models import Model
from django.db.models.fields.related import ForeignKey
from core import models


class Report:
    """This is a report using by the unit test framework to test integration
    between Django and the BDRS.
    """
    def datetime_to_epoch_ms(self, dt):
        """Converts the Python datetime.datetime object to the number of ms since January 1, 1970 UTC."""
        seconds = time.mktime(dt.timetuple())
        seconds += (dt.microsecond / 1000000.0)
        return int(round(seconds * 1000.0))

    def flatten(self, entity):
        flat = {'pk': entity.pk}
        for field in entity._meta.fields:
            if isinstance(field, ForeignKey):
                field_name = field.column
            else:
                field_name = field.name

            value = getattr(entity, field.name)
            if isinstance(value, datetime):
                value = self.datetime_to_epoch_ms(value)
            if isinstance(value, Decimal):
                value = str(value)
            if hasattr(value, 'wkt'):
                value = value.wkt
            if isinstance(value, Model):
                value = value.pk

            flat[field_name] = value

        for m2m in entity._meta.many_to_many:
            flat[m2m.name] = list(m2m.value_from_object(entity).values_list('pk', flat=True))

        return flat

    def content(self, json_params, *args, **kwargs):
        params = json.loads(json_params, strict=False)

        model = getattr(models, params['model_name'][0])
        columns = []
        for field in model._meta.fields:
            columns.append(field.column)

        objects = {}
        for entity in model.objects.all():
            objects[entity.pk] = self.flatten(entity)
        data = {
            'columns': columns,
            'objects': objects,
        }

        response = bdrs.getResponse()
        response.setContentType('application/json')
        response.setContent(json.dumps(data))
        return
