# [r51](https://code.google.com/p/ala-citizenscience/source/detail?r=51) Migrations for Themes and Content #
## Updating Themes (applies to revisions 51 and up) ##
In [revision 51](https://code.google.com/p/ala-citizenscience/source/detail?r=51) of the codebase, major changes to the theming engine were made.  The default look and feel of the site was moved into a downloadable theme which had the unintentional consequence of removing some defaults that themes created before this point may have been inadvertently relying on.  To migrate a theme made prior to [revision 51](https://code.google.com/p/ala-citizenscience/source/detail?r=51) to run on the 51 or greater codebase, the following must be done:
**Log in to your portal.** Go to Admin > Manage Portal > Edit Theme.  On this page, you will see a list of at least two themes: Default BDRS Theme (vanilla) and [theme name](your.md).
**Click on the Download link next to the Default BDRS Theme (vanilla) entry.** Extract the zip file contents from the Default BDRS Theme (vanilla) zip file.
**Rename the file base.css inside the css directory to vanilla.css.** Copy the vanilla.css file to the css directory inside your theme structure.  If you do not have a copy on your local machine, you can download it in the same manner you downloaded the default theme.
**Open the config.json file for your theme and add the path to the vanilla.css file ("css/vanilla.css") to the top of the css\_files list.** Open the config.json file from the downloaded default theme and copy the theme elements into your theme's config.json file.  Check the file for any duplicates and remove the duplicates copied from the vanilla theme.
**Zip the contents of your theme directory.** Upload the file into your portal.
 Log in as admin.
 Go to Admin > Manage Portal > Manage Files
 Click the Add Media button and upload the file.
 After uploading the file, find it in the managed files list and copy the text in the Identifier column.
**Go back to Admin > Manage Portal > Edit Theme** Select your theme from the list and click the Edit link next to it or click the Add Theme button and add a new one.
**In the theme editing interface, paste the identifier you copied into the Managed File UUID text box.****Note that if you are creating a new theme, you need to check the Is Active box in order to activate it.** Click the Save button and your theme should be updated.

After performing the above steps, you will also need to update your content with some changes made there as well.  See [[Content (applies to revisions 457 and up)|Updating Content](#Updating.md)].

## Updating Content (applies to revisions 51 and up) ##
In [revision 457](https://code.google.com/p/ala-citizenscience/source/detail?r=457) of the codebase, major changes to the theming engine were made which also affected the content.  To migrate the content an existing system created prior to [revision 51](https://code.google.com/p/ala-citizenscience/source/detail?r=51) to run on the 51 or greater codebase, the following must be done:
**Update the system.  You will notice that some content is missing and you have ${[variable](variable.md)} in its place.** If you have not edited any content on your system, you can simply go to Admin > Manage Portal > Edit Content and click on the Reset all content to default button.
