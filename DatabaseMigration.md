#These are the database changes you'll have to make to support new versions of the software.

# Introduction #

From version to version, it is sometimes necessary to update your database, this page tracks the SQL script that is needed between versions.

# [r24](https://code.google.com/p/ala-citizenscience/source/detail?r=24) #

This was the first opensource release. You should just generate the database using the hibernate3:hbm2ddl target.

# [r26](https://code.google.com/p/ala-citizenscience/source/detail?r=26) #
```sql

create table CENSUS_METHOD (CENSUS_METHOD_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, name varchar(255) not null, taxonomic bool, PORTAL_ID int4, primary key (CENSUS_METHOD_ID));
create table CENSUS_METHOD_ATTRIBUTE (CENSUS_METHOD_CENSUS_METHOD_ID int4 not null, attributes_ATTRIBUTE_ID int4 not null, pos int4 not null, primary key (CENSUS_METHOD_CENSUS_METHOD_ID, pos), unique (attributes_ATTRIBUTE_ID));
create table CENSUS_METHOD_CENSUS_METHOD (CENSUS_METHOD_CENSUS_METHOD_ID int4 not null, censusMethods_CENSUS_METHOD_ID int4 not null, pos int4 not null, primary key (CENSUS_METHOD_CENSUS_METHOD_ID, pos));

alter table CENSUS_METHOD add constraint FK222D93DBC136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table CENSUS_METHOD_ATTRIBUTE add constraint FKE57731871755D68 foreign key (CENSUS_METHOD_CENSUS_METHOD_ID) references CENSUS_METHOD;
alter table CENSUS_METHOD_ATTRIBUTE add constraint FKE5773188EFC338 foreign key (attributes_ATTRIBUTE_ID) references ATTRIBUTE;
alter table CENSUS_METHOD_CENSUS_METHOD add constraint FK1F9F9E7771755D68 foreign key (CENSUS_METHOD_CENSUS_METHOD_ID) references CENSUS_METHOD;
alter table CENSUS_METHOD_CENSUS_METHOD add constraint FK1F9F9E77364FB9F6 foreign key (censusMethods_CENSUS_METHOD_ID) references CENSUS_METHOD;

-- Remove null constraints on DwC fields
alter table record alter column INDICATOR_SPECIES_ID drop not null;
alter table record alter column NUMBER_SEEN drop not null;

-- add reference to census method
alter table record add INDICATOR_CENSUSMETHOD_ID int4;
alter table RECORD add constraint RECORD_CENSUSMETHOD_FK foreign key (INDICATOR_CENSUSMETHOD_ID) references CENSUS_METHOD;

-- so I heard you like records...
alter table record add PARENT_RECORD_ID int4;
alter table RECORD add constraint PARENT_RECORD_TO_RECORD_FK foreign key (PARENT_RECORD_ID) references RECORD;

-- surveys and census methods...
create table SURVEY_CENSUS_METHOD (SURVEY_SURVEY_ID int4 not null, censusMethods_CENSUS_METHOD_ID int4 not null, pos int4 not null, primary key (SURVEY_SURVEY_ID, pos));
alter table SURVEY_CENSUS_METHOD add constraint FK6FEA1BB66B93B13B foreign key (SURVEY_SURVEY_ID) references SURVEY;
alter table SURVEY_CENSUS_METHOD add constraint FK6FEA1BB6364FB9F6 foreign key (censusMethods_CENSUS_METHOD_ID) references CENSUS_METHOD;

alter table CENSUS_METHOD add type varchar(255);
alter table CENSUS_METHOD add description varchar(1023);

create table GEO_MAP (GEO_MAP_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255) not null, DESCRIPTION varchar(1023) not null, HIDE_PRIVATE_DETAILS bool not null, ROLE_REQUIRED varchar(255) not null, PUBLISH bool not null, ANONYMOUS_ACCESS bool not null, PORTAL_ID int4, primary key (GEO_MAP_ID));
create table GEO_MAP_GEO_MAP_LAYER (GEO_MAP_GEO_MAP_ID int4 not null, mapLayers_GEO_MAP_LAYER_ID int4 not null, pos int4 not null, primary key (GEO_MAP_GEO_MAP_ID, pos));
create table GEO_MAP_LAYER (GEO_MAP_LAYER_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255) not null, DESCRIPTION varchar(1023) not null, MANAGED_FILE_UUID varchar(255), HIDE_PRIVATE_DETAILS bool not null, ROLE_REQUIRED varchar(255), PUBLISH bool not null, SURVEY_ID int4, PORTAL_ID int4, primary key (GEO_MAP_LAYER_ID));

alter table GEO_MAP add constraint FK261E6B2EC136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table GEO_MAP_GEO_MAP_LAYER add constraint FKBFED258F33EF01C1 foreign key (mapLayers_GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;
alter table GEO_MAP_GEO_MAP_LAYER add constraint FKBFED258F6138E048 foreign key (GEO_MAP_GEO_MAP_ID) references GEO_MAP;
alter table GEO_MAP_LAYER add constraint FKFA2B10E0C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table GEO_MAP_LAYER add constraint GEO_MAP_LAYER_TO_SURVEY_FK foreign key (SURVEY_ID) references SURVEY;

-- As of bdrs-core rev 205 and mobile rev 91, you will need to increase
-- the maximum POST data size so that the mobile tool can post records
-- to the server.

-- 1) Locate and edit 'server.xml'. On gaiaweb01, this file can be found at '/usr/local/apache-tomcat-6.0.14/conf/server.xml'.
-- 2) Locate the line that looks like,
-- '<Connector port="8009" protocol="AJP/1.3" redirectPort="8443"/>'
-- 3) Modify the line to look like,
-- <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" maxPostSize="-1" maxSavePostSize="-1"/>
-- 4) Save and Exit
-- 5) Profit

create table DEVICE_CAPABILITY (WURFLDEVICE_DEVICE_ID int4 not null, capabilities_CAPABILITY_ID int4 not null, primary key (WURFLDEVICE_DEVICE_ID, capabilities_CAPABILITY_ID), unique (capabilities_CAPABILITY_ID));

create table WURFLCAPABILITY (CAPABILITY_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255), VALUE varchar(255), CAPABILITY_GROUP varchar(255), PORTAL_ID int4, primary key (CAPABILITY_ID));

create table WURFLDEVICE (DEVICE_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, USERAGENT varchar(255), DEVICEIDSTRING varchar(255), PORTAL_ID int4, FALLBACK_ID int4, primary key (DEVICE_ID));

alter table DEVICE_CAPABILITY add constraint FK7FA2A2A1D5F1186B foreign key (capabilities_CAPABILITY_ID) references WURFLCAPABILITY;
alter table DEVICE_CAPABILITY add constraint FK7FA2A2A17971CFE5 foreign key (WURFLDEVICE_DEVICE_ID) references WURFLDEVICE;

alter table WURFLCAPABILITY add constraint FK2AD97D92C136AD4E foreign key (PORTAL_ID) references PORTAL;

alter table WURFLDEVICE add constraint FKC7F0CBD01EA2E568 foreign key (FALLBACK_ID) references WURFLDEVICE;
alter table WURFLDEVICE add constraint FKC7F0CBD0C136AD4E foreign key (PORTAL_ID) references PORTAL;

ALTER TABLE device_capability DROP CONSTRAINT device_capability_capabilities_capability_id_key;

create table GALLERY (GALLERY_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255) not null, DESCRIPTION varchar(1023) not null, PORTAL_ID int4, primary key (GALLERY_ID));
create table GALLERY_ITEMS (GALLERY_ID int4 not null, MANAGED_FILE_UUID varchar(255), MANAGED_FILE_ORDER int4 not null, primary key (GALLERY_ID, MANAGED_FILE_ORDER));

alter table GALLERY add constraint FK1F180332C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table GALLERY_ITEMS add constraint GALLERY_GALLERY_ITEMS_PK foreign key (GALLERY_ID) references GALLERY;

-- create the join tables
create table GEO_MAP_LAYER_ATTRIBUTES (GEO_MAP_LAYER_GEO_MAP_LAYER_ID int4 not null, attributes_ATTRIBUTE_ID int4 not null, unique (attributes_ATTRIBUTE_ID));
create table GEO_MAP_LAYER_RECORDS (GEO_MAP_LAYER_GEO_MAP_LAYER_ID int4 not null, records_RECORD_ID int4 not null, unique (records_RECORD_ID));

alter table GEO_MAP_LAYER_ATTRIBUTES add constraint FKD8B18E168EFC338 foreign key (attributes_ATTRIBUTE_ID) references ATTRIBUTE;
alter table GEO_MAP_LAYER_ATTRIBUTES add constraint FKD8B18E162CF3BE5F foreign key (GEO_MAP_LAYER_GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;
alter table GEO_MAP_LAYER_RECORDS add constraint FK99DE81C3C26275EC foreign key (records_RECORD_ID) references RECORD;
alter table GEO_MAP_LAYER_RECORDS add constraint FK99DE81C32CF3BE5F foreign key (GEO_MAP_LAYER_GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;

-- add layer source column to geo map layer.
-- all default values should be overwritten after the updates
alter table GEO_MAP_LAYER add LAYER_SOURCE varchar(255) not null default 'defaultvalue';
update GEO_MAP_LAYER set LAYER_SOURCE='SURVEY' where survey_id is not null;
update GEO_MAP_LAYER set LAYER_SOURCE='KML' where survey_id is null;

--updated to be compatible with older versions of postgres

BEGIN;
alter table record add column accuracy float8;
alter table census_method rename column taxonomic to taxonomic_boolean;
alter table census_method add column taxonomic varchar(255);
update census_method set taxonomic = 'TAXONOMIC' where taxonomic_boolean = true;
update census_method set taxonomic = 'NONTAXONOMIC' where taxonomic_boolean = false;
alter table census_method drop column taxonomic_boolean;

-- in addition to doing the database migration run
-- migrateRecordAttributeToAttributeValue.sh
-- which has been attached to this file. There are instructions
-- inside the script and it has a few checks in it to make sure
-- you can't screw up too bad.
-- The script will move your uploaded files which used to be
-- attached to a RecordAttribute object and will place them
-- under a AttributeValue object, keeping the ID the same.


-- start, GEO_MAP_FEATURE

-- renaming column
alter table RECORD rename column POINT to GEOM;

alter table GEO_MAP_LAYER_RECORDS drop constraint FK99DE81C3C26275EC;
alter table GEO_MAP_LAYER_RECORDS drop constraint FK99DE81C32CF3BE5F;
drop table GEO_MAP_LAYER_RECORDS;

create table GEO_MAP_FEATURE (GEO_MAP_FEATURE_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, GEOM geometry, GEO_MAP_LAYER_ID int4 not null, PORTAL_ID int4, primary key (GEO_MAP_FEATURE_ID));
create table GEO_MAP_FEATURE_RECORD_ATTRIBUTE (GEO_MAP_FEATURE_GEO_MAP_FEATURE_ID int4 not null, attributes_RECORD_ATTRIBUTE_ID int4 not null, primary key (GEO_MAP_FEATURE_GEO_MAP_FEATURE_ID, attributes_RECORD_ATTRIBUTE_ID), unique (attributes_RECORD_ATTRIBUTE_ID));

alter table GEO_MAP_FEATURE add constraint GEO_MAP_FEATURE_TO_GEO_MAP_LAYER_FK foreign key (GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;
alter table GEO_MAP_FEATURE add constraint FKE3CD0AC5C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table GEO_MAP_FEATURE_RECORD_ATTRIBUTE add constraint FKF8133E48AEC460FA foreign key (GEO_MAP_FEATURE_GEO_MAP_FEATURE_ID) references GEO_MAP_FEATURE;
alter table GEO_MAP_FEATURE_RECORD_ATTRIBUTE add constraint FKF8133E48B2F71680 foreign key (attributes_RECORD_ATTRIBUTE_ID) references RECORD_ATTRIBUTE;

-- end geo map feature section


-- start renaming RecordAttribute to AttributeValue

-- drop the old join tables contraints since hibernate changes the key
alter table GEO_MAP_FEATURE_RECORD_ATTRIBUTE drop constraint FKF8133E48AEC460FA;
alter table GEO_MAP_FEATURE_RECORD_ATTRIBUTE drop constraint FKF8133E48B2F71680;

alter table RECORD_ATTRIBUTE drop constraint FK821EA40EC136AD4E;
alter table RECORD_ATTRIBUTE drop constraint FK821EA40EE3DD50;

alter table RECORD_RECORD_ATTRIBUTE drop constraint FK5563323C4F3EBD3B;
alter table RECORD_RECORD_ATTRIBUTE drop constraint FK5563323CB2F71680;

-- rename tables
alter table GEO_MAP_FEATURE_RECORD_ATTRIBUTE rename to GEO_MAP_FEATURE_ATTRIBUTE_VALUE;
alter table RECORD_ATTRIBUTE rename to ATTRIBUTE_VALUE;
alter table RECORD_RECORD_ATTRIBUTE rename to RECORD_ATTRIBUTE_VALUE;
-- rename primary key column in attribute_value table...
alter table ATTRIBUTE_VALUE rename column record_attribute_id to attribute_value_id;
-- rename column in join tables
alter table GEO_MAP_FEATURE_ATTRIBUTE_VALUE rename column attributes_RECORD_ATTRIBUTE_ID to attributes_ATTRIBUTE_VALUE_ID;
alter table RECORD_ATTRIBUTE_VALUE rename column attributes_RECORD_ATTRIBUTE_ID to attributes_ATTRIBUTE_VALUE_ID;

-- recreate index
drop index record_attribute_string_value_index;
create index attribute_value_string_value_index on ATTRIBUTE_VALUE (STRING_VALUE);

-- rename sequence
alter table record_attribute_record_attribute_id_seq rename to attribute_value_attribute_value_id_seq;

-- recreate constraints
alter table ATTRIBUTE_VALUE add constraint FK5127E9CEC136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table ATTRIBUTE_VALUE add constraint FK5127E9CEE3DD50 foreign key (ATTRIBUTE_ID) references ATTRIBUTE;

alter table GEO_MAP_FEATURE_ATTRIBUTE_VALUE add constraint FK657A2054AEC460FA foreign key (GEO_MAP_FEATURE_GEO_MAP_FEATURE_ID) references GEO_MAP_FEATURE;
alter table GEO_MAP_FEATURE_ATTRIBUTE_VALUE add constraint FK657A20547C59E1B3 foreign key (attributes_ATTRIBUTE_VALUE_ID) references ATTRIBUTE_VALUE;

alter table RECORD_ATTRIBUTE_VALUE add constraint FKFD21DDE04F3EBD3B foreign key (RECORD_RECORD_ID) references RECORD;
alter table RECORD_ATTRIBUTE_VALUE add constraint FKFD21DDE07C59E1B3 foreign key (attributes_ATTRIBUTE_VALUE_ID) references ATTRIBUTE_VALUE;

-- end record attribute rename section

--<strong>Read This

Unknown end tag for &lt;/strong&gt;


--If the above constraint deletions fail then you may want to use the --following lines instead

-- Change:
-- alter table RECORD_ATTRIBUTE drop constraint FK821EA40EE3DD50;
-- alter table RECORD_RECORD_ATTRIBUTE drop constraint FK5563323C4F3EBD3B;
-- alter table RECORD_RECORD_ATTRIBUTE drop constraint FK5563323CB2F71680;

-- to:
-- alter table RECORD_ATTRIBUTE drop constraint fk821ea40e8c85a7a6;
-- alter table RECORD_RECORD_ATTRIBUTE drop constraint fk5563323c11af5025;
-- alter table RECORD_RECORD_ATTRIBUTE drop constraint fk5563323cdc2c2ad6;

-- START
-- create new table
create table ASSIGNED_GEO_MAP_LAYER (ASSIGNED_GEO_MAP_LAYER_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, visible bool not null, PORTAL_ID int4, GEO_MAP_ID int4 not null, GEO_MAP_LAYER_ID int4 not null, primary key (ASSIGNED_GEO_MAP_LAYER_ID));
alter table ASSIGNED_GEO_MAP_LAYER add constraint ASSIGNED_GEO_MAP_LAYER_TO_GEO_MAP_FK foreign key (GEO_MAP_ID) references GEO_MAP;
alter table ASSIGNED_GEO_MAP_LAYER add constraint ASSIGNED_GEO_MAP_LAYER_TO_GEO_MAP_LAYER_FK foreign key (GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;
alter table ASSIGNED_GEO_MAP_LAYER add constraint FK8AE3A02FC136AD4E foreign key (PORTAL_ID) references PORTAL;

-- copy data from old to new table. For every row in the old join table create a row in the new table
-- with map and layer ids filled out appropriately. set visible and weight to 0
insert into ASSIGNED_GEO_MAP_LAYER(weight, visible, GEO_MAP_ID, GEO_MAP_LAYER_ID)
select pos, true, GEO_MAP_GEO_MAP_ID, mapLayers_GEO_MAP_LAYER_ID from GEO_MAP_GEO_MAP_LAYER;

-- update the portal ids...
update ASSIGNED_GEO_MAP_LAYER as a set portal_id = (select portal_id from GEO_MAP_LAYER as g where a.geo_map_layer_id=g.geo_map_layer_id);

-- drop old join table...
alter table GEO_MAP_GEO_MAP_LAYER drop constraint FKBFED258F33EF01C1;
alter table GEO_MAP_GEO_MAP_LAYER drop constraint FKBFED258F6138E048;
drop table GEO_MAP_GEO_MAP_LAYER;

-- END


alter TABLE wurflcapability drop CONSTRAINT fk2ad97d92c136ad4e;
alter TABLE wurflcapability drop COLUMN portal_id;

alter TABLE wurfldevice drop CONSTRAINT fkc7f0cbd0c136ad4e;
alter table wurfldevice drop COLUMN portal_id;


alter table geo_map_layer add column STROKE_COLOR varchar(15) not null default '#EE9900';
alter table geo_map_layer add column FILL_COLOR varchar(15) not null default '#EE9900';
alter table geo_map_layer add column SYMBOL_SIZE int4 not null default '5';
alter table geo_map_layer add column STROKE_WIDTH int4 not null default '1';

ALTER TABLE survey ADD COLUMN surveyenddate timestamp without time zone;

--rev 295 core
```
# [r33](https://code.google.com/p/ala-citizenscience/source/detail?r=33) #
```sql

begin;

alter table ASSIGNED_GEO_MAP_LAYER add column upperZoomLimit int4;
alter table ASSIGNED_GEO_MAP_LAYER add column lowerZoomLimit int4;

-- START --------------------------------------

-- add record visibility property
-- note that some sites will NOT want to default to owner only! Take this into
-- account when doing database migrations on prod!
alter table record add column record_visibility varchar(50) default 'OWNER_ONLY';

-- we could use a longer description for our surveys.
alter table survey alter column description type varchar(1023);

-- add census method meta data
create table CENSUS_METHOD_METADATA (CENSUS_METHOD_CENSUS_METHOD_ID int4 not null, metadata_ID int4 not null, primary key (CENSUS_METHOD_CENSUS_METHOD_ID, metadata_ID));
alter table CENSUS_METHOD_METADATA add constraint FK40742F1371755D68 foreign key (CENSUS_METHOD_CENSUS_METHOD_ID) references CENSUS_METHOD;
alter table CENSUS_METHOD_METADATA add constraint FK40742F13510B318B foreign key (metadata_ID) references METADATA;

-- END ---------------------------------------

commit;
--rev 360 core
```


# [r35](https://code.google.com/p/ala-citizenscience/source/detail?r=35) #
```sql

-- to help speed up map server stuff. will help in general with spatial queries.

begin;

create index geo_map_feature_geom on geo_map_feature using GIST (geom);
cluster geo_map_feature_geom on geo_map_feature;

create index record_geom on record using GIST (geom);
cluster record_geom on record;

create table LOCATION_ATTRIBUTE_VALUE (LOCATION_LOCATION_ID int4 not null, attributes_ATTRIBUTE_VALUE_ID int4 not null, primary key (LOCATION_LOCATION_ID, attributes_ATTRIBUTE_VALUE_ID), unique (attributes_ATTRIBUTE_VALUE_ID));

alter table LOCATION_ATTRIBUTE_VALUE add constraint FK75AC5445446447B foreign key (LOCATION_LOCATION_ID) references LOCATION;
alter table LOCATION_ATTRIBUTE_VALUE add constraint FK75AC5447C59E1B3 foreign key (attributes_ATTRIBUTE_VALUE_ID) references ATTRIBUTE_VALUE;


create table THEME_PAGE (theme_page_id serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, KEY varchar(255) not null, DESCRIPTION varchar(2047), TITLE varchar(255), PORTAL_ID int4, THEME_ID int4, primary key (theme_page_id));

alter table THEME_PAGE add constraint THEME_THEME_PAGE_FK foreign key (THEME_ID) references THEME;
alter table THEME_PAGE add constraint FK3F67B765C136AD4E foreign key (PORTAL_ID) references PORTAL;

commit;
-- rev 399 core
```

# [r37](https://code.google.com/p/ala-citizenscience/source/detail?r=37) #
```sql


begin;
alter table location add column description character varying(255);

alter table Threshold add column name varchar(255);
alter table Threshold add column description varchar(1023);
commit;

-- rev 417 core
```

# [r51](https://code.google.com/p/ala-citizenscience/source/detail?r=51) #
```sql


begin;

alter table theme add column isdefault boolean NOT NULL default false;
alter table theme alter column theme_file_uuid drop not null;

alter table attribute_value alter column string_value type text;

--rev 470 core

alter table portal add constraint portal_name_key unique(name);

--rev 475 core

update metadata set key = replace(key, 'Record.','RECORD.') || '.WEIGHT' where key like 'Record.%';
alter TABLE record ALTER COLUMN when_date DROP NOT NULL;
alter TABLE record ALTER COLUMN last_date DROP NOT NULL;


--rev 494 core
ALTER TABLE preference ALTER COLUMN description TYPE text;

-- rev 496 core
commit;
```

# [r58](https://code.google.com/p/ala-citizenscience/source/detail?r=58) #
```sql

BEGIN;
update record set held = FALSE where held is null;
alter table record alter column held set default FALSE;
alter table record alter column held set not null;

alter table action add column actionevent character varying(255) not null default 'CREATE_AND_UPDATE';
update action set actionevent='CREATE_AND_UPDATE';

create table REPORT (REPORT_ID serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255) not null, ACTIVE bool not null, DESCRIPTION text not null, ICONFILENAME varchar(255) not null, PORTAL_ID int4, primary key (REPORT_ID));
alter table REPORT add constraint FK8FDF4934C136AD4E foreign key (PORTAL_ID) references PORTAL;

COMMIT;
--rev 526 core
```

# [r97](https://code.google.com/p/ala-citizenscience/source/detail?r=97) #
```sql

BEGIN;
ALTER TABLE portal ADD COLUMN isactive boolean not null default true;

COMMIT;
-- rev 567 core
```


# [r120](https://code.google.com/p/ala-citizenscience/source/detail?r=120) #
```sql

begin;

alter table indicator_species add column source text;
alter table indicator_species add column source_id text;

create index indicator_species_source_id_index on indicator_species(source_id);

update indicator_species myspecies set source_id=(select m.value from indicator_species s join indicator_species_metadata s_m on s.indicator_species_id=s_m.indicator_species_indicator_species_id join metadata m on s_m.metadata_id=m.id where m.key='TaxonSourceDataID' and s.indicator_species_id=myspecies.indicator_species_id);

delete from indicator_species_metadata where metadata_id in (select m.id from metadata m where key='TaxonSourceDataID');
delete from metadata where key='TaxonSourceDataID';

commit;
```

# [r123](https://code.google.com/p/ala-citizenscience/source/detail?r=123) #
```sql

begin;

alter table attribute_value alter column numeric_value TYPE numeric(19,10);

commit;
```

# [r134](https://code.google.com/p/ala-citizenscience/source/detail?r=134) #
```sql

BEGIN;

create table LOCATION_METADATA (LOCATION_LOCATION_ID int4 not null, metadata_ID int4 not null, primary key (LOCATION_LOCATION_ID, metadata_ID));
alter table LOCATION_METADATA add constraint FK5B00E8795446447B foreign key (LOCATION_LOCATION_ID) references LOCATION;
alter table LOCATION_METADATA add constraint FK5B00E879510B318B foreign key (metadata_ID) references METADATA;

alter table attribute_value alter column numeric_value TYPE numeric(24,12);
alter table location alter column description type text;

ROLLBACK;
```

# [r168](https://code.google.com/p/ala-citizenscience/source/detail?r=168) #
```sql

BEGIN;

create table REPORT_REPORT_CAPABILITY (REPORT_ID int4 not null, CAPABILITY varchar(255) not null, primary key (REPORT_ID, CAPABILITY));
alter table REPORT_REPORT_CAPABILITY add constraint FKCACEE2B824DFDA86 foreign key (REPORT_ID) references REPORT;

create table REPORT_REPORT_VIEW (REPORT_ID int4 not null, VIEW varchar(255) not null, primary key (REPORT_ID, VIEW));
alter table REPORT_REPORT_VIEW add constraint FKD728BC4524DFDA86 foreign key (REPORT_ID) references REPORT;

-- Need to add a migration for exiting reports to display them on the report listing page.
INSERT INTO report_report_view (report_id, view)
SELECT report_id, 'REPORT_LISTING' FROM Report;

ROLLBACK;
```

# [r195](https://code.google.com/p/ala-citizenscience/source/detail?r=195) #
```sql

BEGIN;

create table BASE_MAP_LAYER
(
BASE_MAP_LAYER_ID  serial not null,
CREATED_AT timestamp,
UPDATED_AT timestamp,
CREATED_BY integer,
UPDATED_BY integer,
WEIGHT integer,
LAYER_SOURCE character varying(255),
DEFAULT_LAYER bool not null,
SURVEY_ID integer not null,
PORTAL_ID integer,
primary key (BASE_MAP_LAYER_ID)
);

alter table BASE_MAP_LAYER add constraint FK390EF480C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table BASE_MAP_LAYER add constraint BASE_MAP_LAYER_TO_SURVEY_FK foreign key (SURVEY_ID) references SURVEY;

create table SURVEY_GEO_MAP_LAYER (
SURVEY_GEO_MAP_LAYER_ID serial not null,
WEIGHT integer,
CREATED_AT timestamp,
UPDATED_AT timestamp,
CREATED_BY integer,
UPDATED_BY integer,
survey_SURVEY_ID integer,
PORTAL_ID integer,
layer_GEO_MAP_LAYER_ID integer,
primary key (SURVEY_GEO_MAP_LAYER_ID)
);
alter table SURVEY_GEO_MAP_LAYER add constraint FK47E798BB42522E6E foreign key (layer_GEO_MAP_LAYER_ID) references GEO_MAP_LAYER;
alter table SURVEY_GEO_MAP_LAYER add constraint FK47E798BBC136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table SURVEY_GEO_MAP_LAYER add constraint FK47E798BB6B93B13B foreign key (survey_SURVEY_ID) references SURVEY;

INSERT INTO base_map_layer SELECT nextval('base_map_layer_base_map_layer_id_seq'::regclass), now() as created_at, now() as updated_at, s.created_by, s.updated_by, '0' as weight, 'G_HYBRID_MAP' as layer_source, 'TRUE' as default_layer, s.survey_id, s.portal_id from survey s;

INSERT INTO base_map_layer SELECT nextval('base_map_layer_base_map_layer_id_seq'::regclass), now() as created_at, now() as updated_at, s.created_by, s.updated_by, '0' as weight, 'G_PHYSICAL_MAP' as layer_source, 'FALSE' as default_layer, s.survey_id, s.portal_id from survey s;

INSERT INTO base_map_layer SELECT nextval('base_map_layer_base_map_layer_id_seq'::regclass), now() as created_at, now() as updated_at, s.created_by, s.updated_by, '0' as weight, 'G_NORMAL_MAP' as layer_source, 'FALSE' as default_layer, s.survey_id, s.portal_id from survey s;

INSERT INTO base_map_layer SELECT nextval('base_map_layer_base_map_layer_id_seq'::regclass), now() as created_at, now() as updated_at, s.created_by, s.updated_by, '0' as weight, 'G_SATELLITE_MAP' as layer_source, 'FALSE' as default_layer, s.survey_id, s.portal_id from survey s;

ROLLBACK;

```

# [r202](https://code.google.com/p/ala-citizenscience/source/detail?r=202) #
```sql

BEGIN;
create table RECORD_COMMENT (COMMENT_ID  serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, DELETED bool, COMMENT_TEXT varchar(1024), RECORD_ID int4, PARENT_COMMENT int4, PORTAL_ID int4, primary key (COMMENT_ID));

alter table RECORD_COMMENT add constraint FK2A21A0519ADDB07A foreign key (PARENT_COMMENT) references RECORD_COMMENT;
alter table RECORD_COMMENT add constraint FK2A21A051C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table RECORD_COMMENT add constraint FK2A21A0516771C7A9 foreign key (RECORD_ID) references RECORD;

ROLLBACK;

```

# [r230](https://code.google.com/p/ala-citizenscience/source/detail?r=230) #
```sql

begin;

alter table indicator_species add column is_current boolean;
update indicator_species set is_current=true;
alter table indicator_species alter is_current set default true;
alter table indicator_species alter is_current set not null;

rollback;
```

# [r236](https://code.google.com/p/ala-citizenscience/source/detail?r=236) #
```sql

begin;

alter table attribute add column VISIBILITY varchar(8);
update attribute set VISIBILITY='ALWAYS';

rollback;
```

# [r240](https://code.google.com/p/ala-citizenscience/source/detail?r=240) #
```sql

begin;

create table INDEX_SCHEDULE (
INDEX_ID  serial not null,
WEIGHT integer,
CREATED_AT timestamp,
UPDATED_AT timestamp,
CREATED_BY integer,
UPDATED_BY integer,
INDEX_TYPE varchar(255) not null,
CLASS_NAME varchar(255) not null,
INDEX_DATE timestamp,
FULL_REBUILD bool not null default false,
PORTAL_ID integer,
primary key (INDEX_ID)
);
alter table INDEX_SCHEDULE add constraint FK44AF2B84C136AD4E foreign key (PORTAL_ID) references PORTAL;

rollback;
```

> # [r247](https://code.google.com/p/ala-citizenscience/source/detail?r=247) #
```sql

begin;
create table INDICATOR_SPECIES_TAXON_GROUP (INDICATOR_SPECIES_INDICATOR_SPECIES_ID int4 not null, secondaryGroups_TAXON_GROUP_ID int4 not null, primary key (INDICATOR_SPECIES_INDICATOR_SPECIES_ID, secondaryGroups_TAXON_GROUP_ID));
rollback;
```

> # [r250](https://code.google.com/p/ala-citizenscience/source/detail?r=250) #
```sql

begin;

create table CUSTOMFORM (CUSTOMFORM_ID  serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, NAME varchar(255) not null, DESCRIPTION text not null, PORTAL_ID int4, primary key (CUSTOMFORM_ID));
alter table CUSTOMFORM add constraint FKBEAB63B5C136AD4E foreign key (PORTAL_ID) references PORTAL;

alter table SURVEY add column CUSTOMFORM_ID int4 default NULL;
alter table SURVEY add constraint FK92769B5AE1E4FFF6 foreign key (CUSTOMFORM_ID) references CUSTOMFORM;

alter table attribute alter visibility set not null;

rollback;
```

# [r271](https://code.google.com/p/ala-citizenscience/source/detail?r=271) #
```sql

begin;
update preference set description = substring(description from 0 for char_length(description)-4) || '<dd><code>optionCount</code> - the number of facet options visible by default. The remaining options can be made visible by the user. Default = 10

Unknown end tag for &lt;/dd&gt;



Unknown end tag for &lt;/dl&gt;

' where key like 'au.com.gaiaresources.bdrs.service.facet%';

update preference set value = substring(value from 0 for char_length(value) -1) || ',"optionCount":10}]' where key like 'au.com.gaiaresources.bdrs.service.facet%';

rollback;

```

# [r278](https://code.google.com/p/ala-citizenscience/source/detail?r=278) #
```sql

begin;

alter table index_schedule add column last_run timestamp without time zone;

rollback;

```

# [r294](https://code.google.com/p/ala-citizenscience/source/detail?r=294) #
```sql

begin;

update species_profile set "header"='' where "header" is null;
update species_profile set description='' where description is null;

alter table species_profile alter column description set not null;
alter table species_profile alter column "header" set not null;

rollback;
```

# [r304](https://code.google.com/p/ala-citizenscience/source/detail?r=304) #
```sql

CREATE INDEX indicator_species_source_index on indicator_species(source);
```

# [r316](https://code.google.com/p/ala-citizenscience/source/detail?r=316) #
```sql

begin;

alter table portal add column url_prefix varchar(16) unique;

rollback;
```

# [r338](https://code.google.com/p/ala-citizenscience/source/detail?r=338) #
```sql

begin;

alter table attribute_value add column indicator_species_id int;
alter table attribute_value add constraint attribute_value_indicator_species_fk foreign key (indicator_species_id) references indicator_species;

alter table indicator_species_attribute add column indicator_species_id int;
alter table indicator_species_attribute add constraint indicator_species_attribute_indicator_species_fk foreign key (indicator_species_id) references indicator_species;

rollback;
```

# [r346](https://code.google.com/p/ala-citizenscience/source/detail?r=346) #
```sql


begin;

-- Add extra column for geo map layer to store server url for wms servers
alter table geo_map_layer add column server_url text;

-- alter geo_map
alter table geo_map add column survey_id int;
alter table geo_map add constraint GEO_MAP_TO_SURVEY_FK foreign key (survey_id) references survey;
alter table geo_map add column map_owner text;
update geo_map set map_owner='NONE';
alter table geo_map alter column map_owner set not null;
alter table geo_map add column zoom int;
-- add column 'center' to table 'geo_map' with SRID=4326, type=POINT and 2 dimensions.
select AddGeometryColumn('geo_map', 'center', 4326, 'POINT', 2);

-- database migration...

-- create new maps for each survey
insert into geo_map (name, description, hide_private_details, role_required, publish, anonymous_access, portal_id, map_owner, survey_id)
select
'survey map',
'map description',
false,
'',
true,
true,
s.portal_id,
'SURVEY',
s.survey_id
from survey s;

-- ***************************************************
-- Note the below statements use postgresql functions!
-- ***************************************************
-- update zoom of geomap from survey metadata
update geo_map gm set zoom=(select to_number(m.value, '99') from survey s join survey_metadata sm on s.survey_id=sm.survey_survey_id join metadata m on sm.metadata_id=m.id where s.survey_id=gm.survey_id and m.key='Survey.MapZoom' and char_length(m.value) > 0);
-- update center of geomap from survey metadata
update geo_map gm set center=(select ST_PointFromText(m.value, 4326) from survey s join survey_metadata sm on s.survey_id=sm.survey_survey_id join metadata m on sm.metadata_id=m.id where s.survey_id=gm.survey_id and m.key='Survey.MapCenter' and char_length(m.value) > 0);

-- add geo map to base map layer
alter table base_map_layer add column geo_map_id int;
alter table base_map_layer add constraint BASE_MAP_LAYER_TO_GEO_MAP_FK foreign key (geo_map_id) references geo_map;

-- assign geo_map_ids
update base_map_layer bml set geo_map_id=(select m.geo_map_id from geo_map m where bml.survey_id=m.survey_id);

alter table base_map_layer drop constraint BASE_MAP_LAYER_TO_SURVEY_FK;
alter table base_map_layer drop column survey_id;

-- copy survey_geo_map_layer table over to assigned_geo_map_layer table
insert into assigned_geo_map_layer (weight, created_at, updated_at, visible, geo_map_layer_id, portal_id, geo_map_id)
select
sgml.weight,
sgml.created_at,
sgml.updated_at,
true,
sgml.layer_geo_map_layer_id,
sgml.portal_id,
gm.geo_map_id
from survey_geo_map_layer sgml join survey s on sgml.survey_survey_id=s.survey_id join geo_map gm on gm.survey_id=s.survey_id;

-- drop unused table.
drop table survey_geo_map_layer;

rollback;

```

# [r360](https://code.google.com/p/ala-citizenscience/source/detail?r=360) #
```sql


begin;
-- add a census method to attributes for data matrix attribute types
alter table attribute add column censusmethod_census_method_id integer;
alter table attribute add CONSTRAINT fka6dfba7cfaedbcfd FOREIGN KEY (censusmethod_census_method_id) REFERENCES census_method (census_method_id);

-- add an attribute_value reference to record to allow for storage
-- of data matrix attribute sub attributes in records linked to attribute values
alter table record add column parent_attribute_value integer;
alter table record add CONSTRAINT attribute_value_fk FOREIGN KEY (parent_attribute_value) REFERENCES attribute_value (attribute_value_id);

-- merge indicator_species_attribute into attribute_value

-- set up the table
alter table attribute_value add column description character varying(255);
alter table attribute_value add column indicator_species_attribute_id integer;
-- copy the values
insert into attribute_value (weight, created_at, updated_at, created_by,
updated_by, numeric_value, description, date_value,
string_value, attribute_id, portal_id,
indicator_species_attribute_id)
(select weight, created_at, updated_at, created_by,
updated_by, numeric_value, description, date_value, string_value,
attribute_id, portal_id, indicator_species_attribute_id
from indicator_species_attribute);
alter table indicator_species_indicator_species_attribute drop constraint fk47c73dd6dd53c9de;

-- update the join table
update indicator_species_indicator_species_attribute set attributes_indicator_species_attribute_id =
attribute_value_id from attribute_value
where indicator_species_attribute_id = attributes_indicator_species_attribute_id;

-- create a new join table
create table INDICATOR_SPECIES_ATTRIBUTE_VALUE (INDICATOR_SPECIES_INDICATOR_SPECIES_ID int4 not null, attributes_ATTRIBUTE_VALUE_ID int4 not null, primary key (INDICATOR_SPECIES_INDICATOR_SPECIES_ID, attributes_ATTRIBUTE_VALUE_ID), unique (attributes_ATTRIBUTE_VALUE_ID));
alter table INDICATOR_SPECIES_ATTRIBUTE_VALUE add constraint FK3CB50A5B544A68D8 foreign key (INDICATOR_SPECIES_INDICATOR_SPECIES_ID) references INDICATOR_SPECIES;
alter table INDICATOR_SPECIES_ATTRIBUTE_VALUE add constraint FK3CB50A5B7C59E1B3 foreign key (attributes_ATTRIBUTE_VALUE_ID) references ATTRIBUTE_VALUE;
insert into indicator_species_attribute_value (INDICATOR_SPECIES_INDICATOR_SPECIES_ID, attributes_ATTRIBUTE_VALUE_ID)
select INDICATOR_SPECIES_INDICATOR_SPECIES_ID, attributes_indicator_species_attribute_id from indicator_species_indicator_species_attribute;

-- drop the unused column used for copying
alter table attribute_value drop column indicator_species_attribute_id;
-- drop the old tables
drop table indicator_species_attribute;
drop table indicator_species_indicator_species_attribute;

rollback;

```


# [r366](https://code.google.com/p/ala-citizenscience/source/detail?r=366) #
```sql


begin;

-- Add column, set default value, set not null!

alter table geo_map add column crs text;
update geo_map set crs='WGS84';
alter table geo_map alter crs set not null;

rollback;
```


# [r390](https://code.google.com/p/ala-citizenscience/source/detail?r=390) #
```sql


begin;

alter table survey add column public_read_access boolean not null default true;

rollback;
```

# [r426](https://code.google.com/p/ala-citizenscience/source/detail?r=426) #
```sql

begin;
create index attribute_name_index on ATTRIBUTE (NAME);
rollback;
```

## Future revision (needs to be documented somewhere) ##
```sql

begin;
alter table INDEX_SCHEDULE add column TASK_TYPE varchar(8) unique;
create index task_type_index on INDEX_SCHEDULE (TASK_TYPE);
update INDEX_SCHEDULE set TASK_TYPE='INDEX';
rollback;
```

# [r500](https://code.google.com/p/ala-citizenscience/source/detail?r=500) #
adding record\_group table
```sql


-- Adding record group table
create table RECORD_GROUP (RECORD_GROUP_ID  serial not null, WEIGHT int4, CREATED_AT timestamp, UPDATED_AT timestamp, CREATED_BY int4, UPDATED_BY int4, type text, START_DATE timestamp, END_DATE timestamp, USER_ID int4, PORTAL_ID int4, SURVEY_ID int4, primary key (RECORD_GROUP_ID));

alter table RECORD add column RECORD_GROUP_ID int4;

alter table RECORD add constraint RECORD_RECORD_GROUP_FK foreign key (RECORD_GROUP_ID) references RECORD_GROUP;
create index record_group_type_index on RECORD_GROUP (type);

alter table RECORD_GROUP add constraint FK425580F1C136AD4E foreign key (PORTAL_ID) references PORTAL;
alter table RECORD_GROUP add constraint RECORD_GROUP_USER_FK foreign key (USER_ID) references USER_DEFINITION;
alter table RECORD_GROUP add constraint RECORD_GROUP_SURVEY_FK foreign key (SURVEY_ID) references SURVEY;

create table RECORD_GROUP_METADATA (RECORD_GROUP_RECORD_GROUP_ID int4 not null, metadata_ID int4 not null, primary key (RECORD_GROUP_RECORD_GROUP_ID, metadata_ID));

alter table RECORD_GROUP_METADATA add constraint FK58369ABD510B318B foreign key (metadata_ID) references METADATA;
alter table RECORD_GROUP_METADATA add constraint FK58369ABD490DA25A foreign key (RECORD_GROUP_RECORD_GROUP_ID) references RECORD_GROUP;


```


# [r544](https://code.google.com/p/ala-citizenscience/source/detail?r=544) #
Adding GPS Altitude
```sql

BEGIN;
alter table record add column gps_altitude float8;
ROLLBACK;
```

# [r558](https://code.google.com/p/ala-citizenscience/source/detail?r=558) #
Removed Wurfl
```sql

BEGIN;
drop TABLE device_capability ;
drop TABLE wurfldevice ;
drop TABLE wurflcapability ;
ROLLBACK;
```

# [r588](https://code.google.com/p/ala-citizenscience/source/detail?r=588) #
Report role settings

```sql

BEGIN;
alter table report add column user_role text;
update report set user_role = 'ROLE_USER';
ROLLBACK;
```