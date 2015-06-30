# BDRS 1.0 Release Notes #

Welcome to BDRS v1.0!

This release represents a lot of work - almost 2 years of development - and a general move towards this software being more of a mature product.  This is our first push out of a versioned release, and will be the start of many under this project.

Behind the scenes, pre-version 1.0, we've been working in a very agile methodology with the Atlas of Living Australia, and this will be seen in the code and the issues below.  This initial period of development has been guiding the direction of the software - initially from a simple recording form for biological data to what is now pretty much an all-singing, all-dancing recording system that can actually handle both biological and non-biological data.

One of the things that you won't find in v1.0 is a pre-packaged mobile system.  While we've been delivering mobile solutions and prototypes, we've found some significant issues with the technologies we are using, and are going to be refactoring mobile in a coming release.  There is mobile code in the trunk of this release, but keep in mind it's something we'll be reworking significantly.

So, despite all of this effort, yes, we have some known issues in the software. We've done our best to remove them as we get to version 1.0, but there are some we simply have had to push back to v1.1 in order to meet the v1.0 deadline.  However, we will be pushing out a new release in the first quarter of 2012 that should address these issues.

The nature of open source projects mean that you could check out our trunk code - which we update here on Google Code regularly from our internal repository where active development occurs - and get some of these fixes early.  We'd only recommend you check out a trunk version if you are a developer; and let us know if you plan to do that.  We would love to hear what you are interested in, and help you become part of the BDRS family.

The BDRS has a bright future.  Gaia Resources is funded to do another six months of development on the core product for the Atlas of Living Australia (ALA), and that will mean more releases.  In addition, we are also putting in place other instances of the software outside of the ALA contract.  We are intending to develop a community site around this code repository in early 2012, and to really establish this software as a useful tool for as many groups as we can.

Please contact us either through this Google Code implementation, or directly at Gaia Resources if you want to know more, get help, or be involved in this project.

Thanks for your interest in the BDRS!

Piers Higgs
Director, Gaia Resources
piers@gaiaresources.com.au

## Content ##
The following items of content have been updated.
<pre>
admin/censusMethodEdit<br>
admin/editProject/editLocations<br>
admin/editProject/editTaxonomy<br>
admin/groupListing<br>
admin/taxonomy/listing<br>
public/about<br>
public/help<br>
public/home<br>
root/portalListing<br>
root/theme/edit/advanced/editFile<br>
user/widgetBuilder<br>
user/locations/edit<br>
</pre>
If you do not have any custom content, you may choose to update the content pages above by
  * Logging into the BDRS as an Administrator
  * Navigate to the `Edit Content` page through the menu using Admin -> Manage Portal -> Edit Content
  * For each of the items in the list above, select the item in the drop down list and click `Reset Current Content Default`

## Known Issues ##
We’ve done our best to make BDRS 1.0 as solid as possible, but as is always the case with software, there are a couple of issues that we know about in this release.

### Creating records with a hidden latitude and longitude ###
When creating or editing a record for a project with hidden latitude and longitude fields, the map will still be displayed. You may click on the map and a point will appear but on submission no spatial data will be saved.

_Note that you will not be able to see your record on the map tab of the `MySightings` page because the record does not have a latitude and longitude._

This bug will be fixed in the next release of the BDRS.

### Creating records with a hidden/non-mandatory when or time field ###
Internally the BDRS stores the darwin core `date` and `time` fields as a single date-time value in the database. This means that if the date or time field is hidden or not set, the BDRS is forced to create a default value for the field so that the value can be stored in the database.

For the moment, we recommend that the `date` and `time` fields are either both mandatory or both hidden to ensure that any dates and times entered are accurate (without the need for default data).

By default the date is January 1, 1970, 00:00:00.

### Project access for child user groups ###
When configuring user group access to a project, only members of the specified group are given access to a project. For example,

  * Group A contains User X, User Y and Group B.
  * Group B contains User Z.

If Group A is added to a project and then only User X and User Y will have access to the project. User Z will not have access to the project.

### Adding taxonomy groups to a project ###
When adding taxonomy groups to a project, only the taxa in the group at the time of submission will be added to the project. If a taxa is added to the group at a later time, this taxa will not be added to the project.

As a workaround you can edit the relevant project and re-save the group selection to add the missing taxa.

### Adding a selection of species to a project ###
If a project is configured to have a selection of species, when the project taxonomy is modified, the page shows all of the taxon groups containing the selected species instead of the original selection of species.

Rest assured that unless the user clicks save, the original selection of species is retained. To modify the selection of species, re-select the `Selection of Species` option and modify the list as appropriate.

### Bulk data menu item ###
Currently it is important that the Bulk Data menu item is not renamed in the `menu.config` file if you wish to ensure that the item appears last on the contribute menu.

### Spreadsheet upload ###
Spreadsheet cells corresponding to `Integer Range` and `Selection` field types are not validated during upload.

### Displaying the last modified record on a map ###
When a record is created or modified, the `MySightings` page will show the position of the record on a map as a red point. Sometimes the record may occupy the same position as another record. In these cases, the modified record will be displayed in red, on top of the other records at the same position. This means that when you click on the point, you will in fact be clicking on the top most record and will only see details for that record in the popup. Specifically, you will not be able to view all the records at that position using the popup.

If you navigate to the `MySightings` view using the menu, then no particular record will be highlighted in red and you can view all records as normal by clicking on the point.

### Gallery and Gallery Widget ###
The Gallery and Gallery Widget has been temporarily removed from the user interface while we improve this feature to meet quality standards.

### Species Auto-completion ###
The species auto-complete widget on record entry forms does not set the species if you paste in the species name.  You must select a value from the auto-complete pop-up list in order for the species to be set.

### Single-Site All Species form type ###
When a `Multi Select`, `Single Checkbox` or `Multi Checkbox` field is added to a project with `Single-Site All Species` form type, a record will be created for each species even if none of the entries were modified on the form.

The reason that all records are saved for these field types is because they are represented on the screen as checkboxes or multi-select lists. By their nature, these widgets contain information when they are ticked and also when they are not ticked. Therefore it is not possible to discern the difference between an intentionally unselected widget and an ignored widget.

### Javascript validation on Single-Site Multi-Species and Single-Site All Species ###
On the `Single-Site Multi-Species` and `Single-Site All Species` projects, if the table is wide enough to require a horizontal scrollbar, javascript errors that are normally displayed using a popup balloon will not be positioned correctly.

### File and Image Attributes on the Yearly Sightings Form ###
File and Image uploads to a `Yearly Sightings` project will be overridden if any records are added or modified.

It is recommended that until this issue is resolved, users should avoid using these field types on a `Yearly Sightings` project.

### Popup dialog width on Internet Explorer 7 ###
Dialog popups in Internet Explorer 7 are incorrectly formatted and are drawn with 100% screen width.

See jQuery UI Ticket [#4437](http://bugs.jqueryui.com/ticket/4437) for more information

### Field Guide `Record Now` button ###
In order to record a taxon directly from the field guide, the administrator must set up a project such that the taxonomy for the project is either

  1. A Single Species,
  1. A Selection of Species, or
  1. A Related Group of Species

For the `Record Now` button to work correctly, please ensure that

  * The project is configured using one of the taxonomy selections above, and
  * There is exactly one project that can be used to record the taxon. Having one more than one project for the taxon will mean that the project selected to record the taxon will be indeterminate.
