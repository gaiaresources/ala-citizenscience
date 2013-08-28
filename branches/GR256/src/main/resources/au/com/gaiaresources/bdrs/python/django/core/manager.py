from django.contrib.gis.db.models import GeoManager
from django.db.models.fields.related import ForeignKey
from django.conf import settings


class PortalManager(GeoManager):

    def _get_portal_id(self):
        portal_id = None
        if hasattr(settings, 'PORTAL_ID'):
            portal_id = settings.PORTAL_ID

        return portal_id

    def _has_field(self, model_klass, field_name):
        try:
            model_klass._meta.get_field(field_name)
            return True
        except:
            return False

    def _has_portal(self, model_klass):
        return self._has_field(model_klass, 'portal')

    def _has_weight(self, model_klass):
        return self._has_field(model_klass, 'weight')

    def _insert_portal_filter(self, query_set, portal_id):
        if self._has_portal(query_set.model):
            query_set = query_set.filter(portal__pk=portal_id)
        else:
            query_set = self._insert_m2m_portal_filter(query_set, portal_id)

        return query_set

    def _insert_fk_portal_filter(self, query_set, portal_id, fk_field):
        if self._has_portal(fk_field.rel.to):
            query_set = query_set.filter(**{
                '%s__portal__pk' % fk_field.name: portal_id
            })

        return query_set

    def _insert_m2m_portal_filter(self, query_set, portal_id):
        for field in query_set.model._meta.fields:
            if isinstance(field, ForeignKey):
                query_set = self._insert_fk_portal_filter(
                    query_set, portal_id, field)

        return query_set

    def _insert_weight_ordering(self, query_set):
        # Cannot perform distinct if it does not match the order_by so if you
        # try to use distinct, the manager will not give you free
        # order by support.
        query = query_set.query
        if query.distinct and len(query.distinct_fields) > 0:
            return

        if self._has_weight(query_set.model):
            query_set = query_set.order_by('weight')
        return query_set

    def get_query_set(self):
        qs = super(PortalManager, self).get_query_set()
        portal_id = self._get_portal_id()
        if portal_id is not None:
            qs = self._insert_portal_filter(qs, portal_id)

        qs = self._insert_weight_ordering(qs)

        return qs
