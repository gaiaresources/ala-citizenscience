from django.contrib.gis.db import models

from compositekey import db

from core.manager import PortalManager


"""This module contains classes that exist to support m2m relationships."""


class ProjectLocation(models.Model):
    id = db.MultiFieldPK('project_project', 'locations_location')
    project_project = models.ForeignKey('Project')
    locations_location = models.ForeignKey('Location')
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'project_location'
        ordering = ['pos']


class ProjectUsergroup(models.Model):
    id = db.MultiFieldPK('project_project', 'groups_group')
    project_project = models.ForeignKey('Project')
    groups_group = models.ForeignKey('Usergroup')

    objects = PortalManager()

    class Meta:
        db_table = u'project_usergroup'


class ProjectUserDefinition(models.Model):
    id = db.MultiFieldPK('project_project', 'users_user_definition')
    project_project = models.ForeignKey('Project')
    users_user_definition = models.ForeignKey('UserDefinition')

    objects = PortalManager()

    class Meta:
        db_table = u'project_user_definition'


class IndicatorSpeciesTaxonGroup(models.Model):
    indicator_species_indicator_species_id = models.IntegerField()
    secondarygroups_taxon_group_id = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'indicator_species_taxon_group'


class RecordMetadata(models.Model):
    id = db.MultiFieldPK('record_record', 'metadata')
    record_record = models.ForeignKey('Record')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'record_metadata'


class IndicatorSpeciesMetadata(models.Model):
    id = db.MultiFieldPK('indicator_species_indicator_species', 'metadata')
    indicator_species_indicator_species = models.ForeignKey('IndicatorSpecies')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'indicator_species_metadata'


class UsergroupUsergroup(models.Model):
    id = db.MultiFieldPK('usergroup_group', 'groups_group')
    usergroup_group = models.ForeignKey('Usergroup', related_name='usergroup')
    groups_group = models.ForeignKey('Usergroup', related_name='subgroups')

    objects = PortalManager()

    class Meta:
        db_table = u'usergroup_usergroup'


class UserDefinitionMetadata(models.Model):
    id = db.MultiFieldPK('user_definition_user_definition', 'metadata')
    user_definition_user_definition = models.ForeignKey('UserDefinition')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'user_definition_metadata'


class CensusMethodAttribute(models.Model):
    id = db.MultiFieldPK('census_method_census_method', 'attributes_attribute')
    census_method_census_method = models.ForeignKey('CensusMethod')
    attributes_attribute = models.ForeignKey('Attribute', unique=True)
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'census_method_attribute'
        ordering = ['pos']


class GeoMapLayerAttributes(models.Model):
    id = db.MultiFieldPK('geo_map_layer_geo_map_layer', 'attributes_attribute')
    geo_map_layer_geo_map_layer = models.ForeignKey('GeoMapLayer')
    attributes_attribute = models.ForeignKey('Attribute', unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'geo_map_layer_attributes'


class GroupAdmins(models.Model):
    id = db.MultiFieldPK('usergroup_group', 'admins_user_definition')
    usergroup_group = models.ForeignKey('Usergroup')
    admins_user_definition = models.ForeignKey('UserDefinition')

    objects = PortalManager()

    class Meta:
        db_table = u'group_admins'


class GroupUsers(models.Model):
    id = db.MultiFieldPK('usergroup_group', 'users_user_definition')
    usergroup_group = models.ForeignKey('Usergroup')
    users_user_definition = models.ForeignKey('UserDefinition')

    objects = PortalManager()

    class Meta:
        db_table = u'group_users'


class IndicatorSpeciesRegion(models.Model):
    id = db.MultiFieldPK('indicator_species', 'region')
    indicator_species = models.ForeignKey('IndicatorSpecies')
    region = models.ForeignKey('Region')

    objects = PortalManager()

    class Meta:
        db_table = u'indicator_species_region'


class LocationAttributeValue(models.Model):
    id = db.MultiFieldPK('location_location', 'attributes_attribute_value')
    location_location = models.ForeignKey('Location')
    attributes_attribute_value = models.ForeignKey('AttributeValue',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'location_attribute_value'


class GeoMapFeatureAttributeValue(models.Model):
    id = db.MultiFieldPK('geo_map_feature_geo_map_feature', 'attributes_attribute_value')
    geo_map_feature_geo_map_feature = models.ForeignKey('GeoMapFeature')
    attributes_attribute_value = models.ForeignKey('AttributeValue',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'geo_map_feature_attribute_value'


class CensusMethodCensusMethod(models.Model):
    id = db.MultiFieldPK('census_method_census_method', 'censusmethods_census_method')
    census_method_census_method = models.ForeignKey('CensusMethod', related_name='parent_censusmethod')
    censusmethods_census_method = models.ForeignKey('CensusMethod', related_name='child_censusmethods')
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'census_method_census_method'
        ordering = ['pos']


class CensusMethodMetadata(models.Model):
    id = db.MultiFieldPK('census_method_census_method', 'metadata')
    census_method_census_method = models.ForeignKey('CensusMethod')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'census_method_metadata'


class ExpertRegion(models.Model):
    id = db.MultiFieldPK('expert', 'region')
    expert = models.ForeignKey('Expert')
    region = models.ForeignKey('Region')

    objects = PortalManager()

    class Meta:
        db_table = u'expert_region'


class ExpertTaxonGroup(models.Model):
    id = db.MultiFieldPK('expert', 'taxon_group')
    expert = models.ForeignKey('Expert')
    taxon_group = models.ForeignKey('TaxonGroup')

    objects = PortalManager()

    class Meta:
        db_table = u'expert_taxon_group'


class SurveyUserDefinition(models.Model):
    id = db.MultiFieldPK('survey_survey', 'users_user_definition')
    survey_survey = models.ForeignKey('Survey')
    users_user_definition = models.ForeignKey('UserDefinition')

    objects = PortalManager()

    class Meta:
        db_table = u'survey_user_definition'


class LocationMetadata(models.Model):
    id = db.MultiFieldPK('location_location', 'metadata')
    location_location = models.ForeignKey('Location')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'location_metadata'


class SurveyCensusMethod(models.Model):
    id = db.MultiFieldPK('survey_survey', 'censusmethods_census_method')
    survey_survey = models.ForeignKey('Survey')
    censusmethods_census_method = models.ForeignKey('CensusMethod')
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'survey_census_method'
        ordering = ['pos']


class SurveyIndicatorSpecies(models.Model):
    id = db.MultiFieldPK('survey_survey', 'species_indicator_species')
    survey_survey = models.ForeignKey('Survey')
    species_indicator_species = models.ForeignKey('IndicatorSpecies')

    objects = PortalManager()

    class Meta:
        db_table = u'survey_indicator_species'


class SurveyLocation(models.Model):
    id = db.MultiFieldPK('survey_survey', 'locations_location')
    survey_survey = models.ForeignKey('Survey')
    locations_location = models.ForeignKey('Location')
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'survey_location'
        ordering = ['pos']


class SurveyMetadata(models.Model):
    id = db.MultiFieldPK('survey_survey', 'metadata')
    survey_survey = models.ForeignKey('Survey')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'survey_metadata'


class SurveyUsergroup(models.Model):
    id = db.MultiFieldPK('survey_survey', 'groups_group')
    survey_survey = models.ForeignKey('Survey')
    groups_group = models.ForeignKey('Usergroup')

    objects = PortalManager()

    class Meta:
        db_table = u'survey_usergroup'


class LocationRegion(models.Model):
    id = db.MultiFieldPK('location', 'region')
    location = models.ForeignKey('Location')
    region = models.ForeignKey('Region')

    objects = PortalManager()

    class Meta:
        db_table = u'location_region'


class ManagedfileMetadata(models.Model):
    id = db.MultiFieldPK('managedfile_managed_file', 'metadata')
    managedfile_managed_file = models.ForeignKey('Managedfile')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'managedfile_metadata'


class ProjectAttribute(models.Model):
    id = db.MultiFieldPK('project_project', 'attributes_attribute')
    project_project = models.ForeignKey('Project')
    attributes_attribute = models.ForeignKey('Attribute', unique=True)
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'project_attribute'
        ordering = ['pos']


class ProjectIndicatorSpecies(models.Model):
    id = db.MultiFieldPK('project_project', 'species_indicator_species')
    project_project = models.ForeignKey('Project')
    species_indicator_species = models.ForeignKey('IndicatorSpecies',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'project_indicator_species'


class SpeciesProfileMetadata(models.Model):
    id = db.MultiFieldPK('species_profile_species_profile', 'metadata')
    species_profile_species_profile = models.ForeignKey('SpeciesProfile')
    metadata = models.ForeignKey('Metadata')

    objects = PortalManager()

    class Meta:
        db_table = u'species_profile_metadata'


class SurveyAttribute(models.Model):
    id = db.MultiFieldPK('survey_survey', 'attributes_attribute')
    survey_survey = models.ForeignKey('Survey')
    attributes_attribute = models.ForeignKey('Attribute', unique=True)
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'survey_attribute'
        ordering = ['pos']


class ThemeThemeElement(models.Model):
    id = db.MultiFieldPK('theme_theme', 'themeelements_theme_element')
    theme_theme = models.ForeignKey('Theme')
    themeelements_theme_element = models.ForeignKey('ThemeElement',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'theme_theme_element'


class ThresholdAction(models.Model):
    id = db.MultiFieldPK('threshold', 'actions')
    threshold = models.ForeignKey('Threshold')
    actions = models.ForeignKey('Action')

    objects = PortalManager()

    class Meta:
        db_table = u'threshold_action'


class TaxonGroupAttribute(models.Model):
    id = db.MultiFieldPK('taxon_group_taxon_group', 'attributes_attribute')
    taxon_group_taxon_group = models.ForeignKey('TaxonGroup')
    attributes_attribute = models.ForeignKey('Attribute', unique=True)
    pos = models.IntegerField()

    objects = PortalManager()

    class Meta:
        db_table = u'taxon_group_attribute'
        ordering = ['pos']


class ThresholdCondition(models.Model):
    id = db.MultiFieldPK('threshold', 'conditions')
    threshold = models.ForeignKey('Threshold')
    conditions = models.ForeignKey('Condition')

    objects = PortalManager()

    class Meta:
        db_table = u'threshold_condition'


class IndicatorSpeciesAttributeValue(models.Model):
    id = db.MultiFieldPK('indicator_species_indicator_species', 'attributes_attribute_value')
    indicator_species_indicator_species = models.ForeignKey('IndicatorSpecies')
    attributes_attribute_value = models.ForeignKey('AttributeValue',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'indicator_species_attribute_value'


class RecordAttributeValue(models.Model):
    id = db.MultiFieldPK('record_record', 'attributes_attribute_value')
    record_record = models.ForeignKey('Record')
    attributes_attribute_value = models.ForeignKey('AttributeValue',
        unique=True)

    objects = PortalManager()

    class Meta:
        db_table = u'record_attribute_value'
