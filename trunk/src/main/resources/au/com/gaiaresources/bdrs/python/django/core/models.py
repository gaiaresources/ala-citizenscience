# This is an auto-generated Django model module.
# You'll have to do the following manually to clean this up:
#     * Rearrange models' order
#     * Make sure each model has one field with primary_key=True
# Feel free to rename the models, but don't rename db_table values or field names.
#
# Also note: You'll have to insert the output of 'django-admin.py sqlcustom [appname]'
# into your database.

from django.contrib.gis.db import models

from core.manager import PortalManager
from core.nonportal import *
from core.m2m import *


class IndexSchedule(models.Model):
    index_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    index_type = models.CharField(max_length=255)
    class_name = models.CharField(max_length=255)
    index_date = models.DateTimeField(null=True, blank=True)
    full_rebuild = models.BooleanField()
    portal = models.ForeignKey('Portal', null=True, blank=True)
    last_run = models.DateTimeField(null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'index_schedule'


class RecordComment(models.Model):
    comment_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    deleted = models.NullBooleanField(null=True, blank=True)
    comment_text = models.CharField(max_length=1024, blank=True)
    record = models.ForeignKey('Record', null=True, blank=True)
    parent_comment = models.ForeignKey('self', null=True, db_column='parent_comment', blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'record_comment'


class Usergroup(models.Model):
    group_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, blank=True)
    description = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    admins = models.ManyToManyField('UserDefinition',
        through='GroupAdmins', related_name='admin_usergroups')

    users = models.ManyToManyField('UserDefinition',
        through='GroupUsers', related_name='user_usergroups')

    groups = models.ManyToManyField('Usergroup',
        through='UsergroupUsergroup', related_name='usergroups')

    objects = PortalManager()

    class Meta:
        db_table = u'usergroup'


class Customform(models.Model):
    customform_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    description = models.TextField()

    objects = PortalManager()

    class Meta:
        db_table = u'customform'


class UserDefinition(models.Model):
    user_definition_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    active = models.BooleanField()
    password = models.CharField(max_length=255)
    email_address = models.CharField(max_length=255)
    first_name = models.CharField(max_length=255)
    last_name = models.CharField(max_length=255)
    registration_key = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    metadata = models.ManyToManyField('Metadata',
        through='UserDefinitionMetadata', related_name='userdefinitions')

    objects = PortalManager()

    class Meta:
        db_table = u'user_definition'


class Threshold(models.Model):
    id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    classname = models.CharField(max_length=255)
    description = models.CharField(max_length=1023, blank=True)
    enabled = models.BooleanField()
    portal = models.ForeignKey('Portal', null=True, blank=True)

    actions = models.ManyToManyField('Action',
        through='ThresholdAction', related_name='thresholds')

    conditions = models.ManyToManyField('Condition',
        through='ThresholdCondition', related_name='thresholds')

    objects = PortalManager()

    class Meta:
        db_table = u'threshold'


class Theme(models.Model):
    theme_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    active = models.BooleanField()
    isdefault = models.BooleanField()
    theme_file_uuid = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    themeelements = models.ManyToManyField('ThemeElement',
        through='ThemeThemeElement', related_name='themes')

    objects = PortalManager()

    class Meta:
        db_table = u'theme'


class ThemeElement(models.Model):
    theme_element_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    key = models.CharField(max_length=255)
    type = models.CharField(max_length=255)
    default_value = models.CharField(max_length=255)
    custom_value = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'theme_element'


class ThemePage(models.Model):
    theme_page_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    key = models.CharField(max_length=255)
    description = models.CharField(max_length=2047, blank=True)
    title = models.CharField(max_length=255, blank=True)
    theme = models.ForeignKey('Theme')
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'theme_page'


class Preferencecategory(models.Model):
    category_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    display_name = models.CharField(max_length=255)
    description = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'preferencecategory'


class Project(models.Model):
    project_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, blank=True)
    public = models.NullBooleanField(null=True, blank=True)
    description = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    attributes = models.ManyToManyField('Attribute',
        through='ProjectAttribute', related_name='projects')

    indicatorspecies = models.ManyToManyField('IndicatorSpecies',
        through='ProjectIndicatorSpecies', related_name='projects')

    locations = models.ManyToManyField('Location',
        through='ProjectLocation', related_name='projects')

    userdefinitions = models.ManyToManyField('UserDefinition',
        through='ProjectUserDefinition', related_name='projects')

    usergroups = models.ManyToManyField('Usergroup',
        through='ProjectUsergroup', related_name='projects')

    objects = PortalManager()

    class Meta:
        db_table = u'project'


class SpeciesProfile(models.Model):
    species_profile_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    type = models.CharField(max_length=255, blank=True)
    content = models.TextField(blank=True)
    description = models.TextField()
    header = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    indicator_species = models.ForeignKey('IndicatorSpecies', null=True, blank=True)

    metadata = models.ManyToManyField('Metadata',
        through='SpeciesProfileMetadata', related_name='speciesprofiles')

    objects = PortalManager()

    class Meta:
        db_table = u'species_profile'


class TaxonGroup(models.Model):
    taxon_group_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, blank=True)
    image = models.CharField(max_length=255, blank=True)
    thumbnail = models.CharField(max_length=255, blank=True)
    behaviour_included = models.NullBooleanField(null=True, blank=True)
    first_appearance_included = models.NullBooleanField(null=True, blank=True)
    last_appearance_included = models.NullBooleanField(null=True, blank=True)
    habitat_included = models.NullBooleanField(null=True, blank=True)
    weather_included = models.NullBooleanField(null=True, blank=True)
    number_included = models.NullBooleanField(null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    attributes = models.ManyToManyField('Attribute',
        through='TaxonGroupAttribute', related_name='taxongroups')

    objects = PortalManager()

    class Meta:
        db_table = u'taxon_group'


class Location(models.Model):
    location_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    location = models.GeometryField(null=True, blank=True)
    description = models.TextField(blank=True)
    user = models.ForeignKey('UserDefinition', null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    attributevalues = models.ManyToManyField('AttributeValue',
        through='LocationAttributeValue', related_name='locations')

    metadata = models.ManyToManyField('Metadata',
        through='LocationMetadata', related_name='locations')

    regions = models.ManyToManyField('Region',
        through='LocationRegion', related_name='locations')

    objects = PortalManager()

    def __unicode__(self):
        return self.name

    def __str__(self):
        return self.name

    class Meta:
        db_table = u'location'


class Managedfile(models.Model):
    managed_file_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    contenttype = models.TextField()
    description = models.TextField()
    uuid = models.TextField(unique=True)
    filename = models.TextField()
    credit = models.TextField()
    license = models.TextField()
    portal = models.ForeignKey('Portal', null=True, blank=True)

    metadata = models.ManyToManyField('Metadata',
        through='ManagedfileMetadata', related_name='managedfiles')

    objects = PortalManager()

    class Meta:
        db_table = u'managedfile'


class GeoMapLayer(models.Model):
    geo_map_layer_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    description = models.CharField(max_length=1023)
    managed_file_uuid = models.CharField(max_length=255, blank=True)
    layer_source = models.CharField(max_length=255, blank=True)
    hide_private_details = models.BooleanField()
    role_required = models.CharField(max_length=255, blank=True)
    publish = models.BooleanField()
    stroke_color = models.CharField(max_length=15)
    fill_color = models.CharField(max_length=15)
    symbol_size = models.IntegerField()
    stroke_width = models.IntegerField()
    portal = models.ForeignKey('Portal', null=True, blank=True)
    survey = models.ForeignKey('Survey', null=True, blank=True)
    server_url = models.TextField(blank=True)

    attributes = models.ManyToManyField('Attribute',
        through='GeoMapLayerAttributes', related_name='geomaplayers')

    objects = PortalManager()

    class Meta:
        db_table = u'geo_map_layer'


class Survey(models.Model):
    survey_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, blank=True)
    public = models.NullBooleanField(null=True, blank=True)
    active = models.NullBooleanField(null=True, blank=True)
    description = models.CharField(max_length=1023, blank=True)
    surveydate = models.DateTimeField(null=True, blank=True)
    surveyenddate = models.DateTimeField(null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    customform = models.ForeignKey('Customform', null=True, blank=True)
    public_read_access = models.BooleanField()

    objects = PortalManager()

    attributes = models.ManyToManyField('Attribute',
        through='SurveyAttribute', related_name='surveys')

    censusmethods = models.ManyToManyField('CensusMethod',
        through='SurveyCensusMethod', related_name='surveys')

    indicatorspecies = models.ManyToManyField('IndicatorSpecies',
        through='SurveyIndicatorSpecies', related_name='surveys')

    locations = models.ManyToManyField('Location',
        through='SurveyLocation', related_name='surveys')

    metadata = models.ManyToManyField('Metadata',
        through='SurveyMetadata', related_name='surveys')

    userdefinitions = models.ManyToManyField('UserDefinition',
        through='SurveyUserDefinition', related_name='surveys')

    usergroups = models.ManyToManyField('Usergroup',
        through='SurveyUsergroup', related_name='surveys')

    class Meta:
        db_table = u'survey'

    def __unicode__(self):
        return self.name

    def __str__(self):
        return self.name


class Grid(models.Model):
    grid_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    grid_precision = models.DecimalField(max_digits=19, decimal_places=2)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'grid'


class GridEntry(models.Model):
    grid_entry_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    boundary = models.GeometryField(null=True, blank=True)
    number_of_records = models.IntegerField(null=True, blank=True)
    indicator_species = models.ForeignKey('IndicatorSpecies')
    grid = models.ForeignKey('Grid')
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'grid_entry'


class Metadata(models.Model):
    id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.CharField(max_length=255)
    key = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'metadata'


class Portalentrypoint(models.Model):
    portal_entry_point_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    pattern = models.CharField(max_length=255)
    redirect = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'portalentrypoint'


class Preference(models.Model):
    preference_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.CharField(max_length=255)
    key = models.CharField(max_length=255)
    locked = models.BooleanField()
    description = models.TextField()
    isrequired = models.BooleanField()
    category = models.ForeignKey('Preferencecategory')
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'preference'


class Region(models.Model):
    region_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, blank=True)
    boundary = models.GeometryField(null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'region'


class Report(models.Model):
    report_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    active = models.BooleanField()
    description = models.TextField()
    iconfilename = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'report'


class Content(models.Model):
    content_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.TextField(blank=True)
    key = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'content'


class Expert(models.Model):
    expert_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    user = models.ForeignKey('UserDefinition')

    regions = models.ManyToManyField('Region',
        through='ExpertRegion', related_name='experts')

    taxongroups = models.ManyToManyField('TaxonGroup',
        through='ExpertTaxonGroup', related_name='experts')

    objects = PortalManager()

    class Meta:
        db_table = u'expert'


class ExpertReviewRequest(models.Model):
    expert_review_request_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    reason_for_request = models.TextField()
    portal = models.ForeignKey('Portal', null=True, blank=True)
    expert = models.ForeignKey('Expert')
    record = models.ForeignKey('Record')

    objects = PortalManager()

    class Meta:
        db_table = u'expert_review_request'


class Gallery(models.Model):
    gallery_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    description = models.CharField(max_length=1023)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'gallery'


class GeoMapFeature(models.Model):
    geo_map_feature_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    geom = models.GeometryField(null=True, blank=True)
    geo_map_layer = models.ForeignKey('GeoMapLayer')
    portal = models.ForeignKey('Portal', null=True, blank=True)

    attributevalues = models.ManyToManyField('AttributeValue',
        through='GeoMapFeatureAttributeValue', related_name='geomapfeatures')

    objects = PortalManager()

    class Meta:
        db_table = u'geo_map_feature'


class AssignedGeoMapLayer(models.Model):
    assigned_geo_map_layer_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    visible = models.BooleanField()
    upperzoomlimit = models.IntegerField(null=True, blank=True)
    lowerzoomlimit = models.IntegerField(null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    geo_map = models.ForeignKey('GeoMap')
    geo_map_layer = models.ForeignKey('GeoMapLayer')

    objects = PortalManager()

    class Meta:
        db_table = u'assigned_geo_map_layer'


class AttributeOption(models.Model):
    attribute_option_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    attribute = models.ForeignKey('Attribute', null=True, blank=True)
    pos = models.IntegerField(null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'attribute_option'


class Action(models.Model):
    id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.CharField(max_length=255, blank=True)
    actiontype = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    actionevent = models.CharField(max_length=255)

    objects = PortalManager()

    class Meta:
        db_table = u'action'


class Portal(models.Model):
    portal_identifier = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255, unique=True)
    isdefault = models.BooleanField()
    isactive = models.BooleanField()
    url_prefix = models.CharField(max_length=16, unique=True, blank=True)

    class Meta:
        db_table = u'portal'


class Condition(models.Model):
    id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    value = models.CharField(max_length=255)
    key = models.CharField(max_length=255, blank=True)
    classname = models.CharField(max_length=255)
    propertypath = models.CharField(max_length=255)
    keyoperator = models.CharField(max_length=255, blank=True)
    valueoperator = models.CharField(max_length=255)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'condition'


class BaseMapLayer(models.Model):
    base_map_layer_id = models.IntegerField(primary_key=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    weight = models.IntegerField(null=True, blank=True)
    layer_source = models.CharField(max_length=255, blank=True)
    default_layer = models.BooleanField()
    portal_id = models.IntegerField(null=True, blank=True)
    geo_map = models.ForeignKey('Portal', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'base_map_layer'


class CensusMethod(models.Model):
    census_method_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    type = models.CharField(max_length=255, blank=True)
    description = models.CharField(max_length=1023, blank=True)
    taxonomic = models.CharField(max_length=255, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    attributes = models.ManyToManyField('Attribute',
        through='CensusMethodAttribute', related_name='censusmethods')

    sub_censusmethods = models.ManyToManyField('CensusMethod',
        through='CensusMethodCensusMethod', related_name='parent_censusmethods')

    metadata = models.ManyToManyField('Metadata',
        through='CensusMethodMetadata', related_name='censusmethods')

    objects = PortalManager()

    class Meta:
        db_table = u'census_method'


class IndicatorSpecies(models.Model):
    indicator_species_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    year = models.CharField(max_length=255, blank=True)
    author = models.CharField(max_length=255, blank=True)
    common_name = models.CharField(max_length=255)
    scientific_name_and_author = models.CharField(max_length=255, blank=True)
    scientific_name = models.CharField(max_length=255)
    rank = models.CharField(max_length=255, blank=True)
    taxon_group = models.ForeignKey('TaxonGroup')
    portal = models.ForeignKey('Portal', null=True, blank=True)
    parent = models.ForeignKey('self', null=True, blank=True)
    source = models.TextField(blank=True)
    source_id = models.TextField(blank=True)
    is_current = models.BooleanField()

    attributevalues = models.ManyToManyField('AttributeValue',
        through='IndicatorSpeciesAttributeValue', related_name='indicatorspecies')

    metadata = models.ManyToManyField('Metadata',
        through='IndicatorSpeciesMetadata', related_name='indicatorspecies')

    regions = models.ManyToManyField('Region',
        through='IndicatorSpeciesRegion', related_name='indicatorspecies')

    # This m2m is problematic because the relation is not a foreign key but
    # an integer.
    # secondary_groups = models.ManyToManyField('TaxonGroup',
    #     through='IndicatorSpeciesTaxonGroup', related_name='indicatorspecies')

    objects = PortalManager()

    class Meta:
        db_table = u'indicator_species'


class AttributeValue(models.Model):
    attribute_value_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    numeric_value = models.DecimalField(null=True, max_digits=24, decimal_places=12, blank=True)
    date_value = models.DateTimeField(null=True, blank=True)
    string_value = models.TextField(blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    attribute = models.ForeignKey('Attribute')
    indicator_species = models.ForeignKey('IndicatorSpecies', null=True, blank=True)
    description = models.CharField(max_length=255, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'attribute_value'


class Attribute(models.Model):
    attribute_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    type_code = models.CharField(max_length=255)
    required = models.BooleanField()
    description = models.TextField(blank=True)
    scope = models.CharField(max_length=255, blank=True)
    tag = models.BooleanField()
    portal = models.ForeignKey('Portal', null=True, blank=True)
    visibility = models.CharField(max_length=8)
    censusmethod_census_method = models.ForeignKey('CensusMethod', null=True, blank=True)

    objects = PortalManager()

    class Meta:
        db_table = u'attribute'


class Record(models.Model):
    record_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    number_seen = models.IntegerField(null=True, blank=True)
    time = models.BigIntegerField(null=True, blank=True)
    when_date = models.DateTimeField(null=True, blank=True)
    geom = models.GeometryField(null=True, blank=True)
    accuracy = models.FloatField(null=True, blank=True)
    gps_altitude = models.FloatField(null=True, blank=True)
    held = models.NullBooleanField(null=True, blank=True)
    last_date = models.DateTimeField(null=True, blank=True)
    last_time = models.BigIntegerField(null=True, blank=True)
    notes = models.TextField(blank=True)
    first_appearance = models.NullBooleanField(null=True, blank=True)
    last_appearance = models.NullBooleanField(null=True, blank=True)
    behaviour = models.TextField(blank=True)
    habitat = models.TextField(blank=True)
    record_visibility = models.CharField(max_length=255, blank=True)
    indicator_censusmethod = models.ForeignKey('CensusMethod', null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)
    indicator_user = models.ForeignKey('UserDefinition')
    indicator_species = models.ForeignKey('IndicatorSpecies', null=True, blank=True)
    parent_record = models.ForeignKey('self', null=True, blank=True)
    indicator_survey = models.ForeignKey('Survey', null=True, blank=True)
    location = models.ForeignKey('Location', null=True, blank=True)

    record_group = models.ForeignKey('RecordGroup',null=True,
        db_column='record_group_id',blank=True)

    parent_attribute_value = models.ForeignKey('AttributeValue',
        null=True, db_column='parent_attribute_value', blank=True)

    attributevalues = models.ManyToManyField('AttributeValue',
        through='RecordAttributeValue', related_name='records')

    metadata = models.ManyToManyField('Metadata',
        through='RecordMetadata', related_name='records')

    objects = PortalManager()

    class Meta:
        db_table = u'record'

class RecordGroup(models.Model):
    record_group_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    start_date = models.DateTimeField(null=True, blank=True)
    end_date = models.DateTimeField(null=True, blank=True)
    type = models.TextField(null=True, blank=True)
    user = models.ForeignKey('UserDefinition',null=True,blank=True)
    survey = models.ForeignKey('Survey', null=True, blank=True)
    portal = models.ForeignKey('Portal', null=True, blank=True)

    metadata = models.ManyToManyField('Metadata',
            through='RecordGroupMetadata', related_name='recordGroups')

    objects = PortalManager()

    class Meta:
        db_table = u'record_group'

class GeoMap(models.Model):
    geo_map_id = models.IntegerField(primary_key=True)
    weight = models.IntegerField(null=True, blank=True)
    created_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(null=True, blank=True)
    created_by = models.IntegerField(null=True, blank=True)
    updated_by = models.IntegerField(null=True, blank=True)
    name = models.CharField(max_length=255)
    description = models.CharField(max_length=1023)
    hide_private_details = models.BooleanField()
    role_required = models.CharField(max_length=255)
    publish = models.BooleanField()
    anonymous_access = models.BooleanField()
    portal = models.ForeignKey('Portal', null=True, blank=True)
    survey = models.ForeignKey('Survey', null=True, blank=True)
    map_owner = models.TextField()
    zoom = models.IntegerField(null=True, blank=True)
    center = models.GeometryField(null=True, blank=True)
    crs = models.TextField()

    objects = PortalManager()

    class Meta:
        db_table = u'geo_map'
