from django.contrib.gis.db import models

from compositekey import db

from core.manager import PortalManager

"""The following module contains portal agnostic classes."""


class UserRole(models.Model):
    id = db.MultiFieldPK('user_definition', 'role_order')
    user_definition = models.ForeignKey('UserDefinition')
    role_name = models.CharField(max_length=255, blank=True)
    role_order = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'user_role'
        ordering = ['role_order']


class GalleryItems(models.Model):
    id = db.MultiFieldPK('gallery', 'managed_file_order')
    gallery = models.ForeignKey('Gallery')
    managed_file_uuid = models.CharField(max_length=255, blank=True)
    managed_file_order = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'gallery_items'
        ordering = ['managed_file_order']


class ReportReportCapability(models.Model):
    id = db.MultiFieldPK('report', 'capability')
    report = models.ForeignKey('Report')
    capability = models.CharField(max_length=255)

    objects = PortalManager()

    class Meta:
        db_table = u'report_report_capability'


class ReportReportView(models.Model):
    id = db.MultiFieldPK('report', 'view')
    report = models.ForeignKey('Report')
    view = models.CharField(max_length=255)

    objects = PortalManager()

    class Meta:
        db_table = u'report_report_view'


class ThemeCssFile(models.Model):
    id = db.MultiFieldPK('theme_theme', 'array_index')
    theme_theme = models.ForeignKey('Theme')
    css_file = models.CharField(max_length=255, blank=True)
    array_index = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'theme_css_file'
        ordering = ['array_index']


class ThemeJsFile(models.Model):
    id = db.MultiFieldPK('theme_theme', 'array_index')
    theme_theme = models.ForeignKey('Theme')
    js_file = models.CharField(max_length=255, blank=True)
    array_index = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'theme_js_file'
        ordering = ['array_index']
