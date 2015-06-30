# Introduction #

This page includes a range of administrator documentation, aimed at showing you how to administrate BDRS.

# Setup #

## Default Passwords ##

After installing and starting up the system, two user accounts will be automatically created for you: "root" and "admin" with default password "password".  You will want to change these passwords before your system goes live.  To change the password, log in with the given credentials and go to Profile > My Profile.  From this page, you can enter a new password.

## Adding the Google Maps API Key ##

When setting up a new system or a new portal on the system, you must add a google maps API key or you will get an error on all pages with a map.  Set up the key preference using the following steps:

  1. Get a Google Maps API key from http://code.google.com/apis/maps/signup.html.
  1. Sign in to your BDRS instance/portal with an admin account.
  1. Go to the Admin > Manage Portal > Edit Preferences menu.
  1. Scroll down the page to the heading "API Keys"
  1. Create the google maps API key according to the directions on the page.  For example, if your server is running on www.example.com, enter a description like "Google Map API key for www.example.com", a key of "google.map.key.example" and value of "www.example.com, `[key from google maps api page]`".

## Privacy Notice & Terms and Conditions ##
Placeholder pages have been added to the BDRS for a Privacy Notice and Terms and Conditions. It is up to the you to insert the appropriate content for your situation.

To do this,

  1. Log into the BDRS using your administrator account.
  1. Go to the Admin > Manage Portal > Edit Content menu.
  1. Update the content for `public/privacyStatement` and `public/termsAndConditions`

## Setting up the Atlas Tracker Form ##
When creating a new project, one of the available form types is called the `Atlas Tracker`. This is a special recording form that was designed specifically for the Atlas of Living Australia. This form is not available to users via the normal menu system. To access this form you must go directly to the URL,

<pre>
http://<domain>:<port>/bdrs-core/bdrs/user/atlas.htm?surveyId=<survey primary key>&taxonSearch=<Scientific name of an existing taxon><br>
- or -<br>
http://<domain>:<port>/bdrs-core/bdrs/user/atlas.htm?surveyId=<survey primary key>&guid=<ALA LSID><br>
</pre>

For example,
<pre>
http://<domain>:<port>/bdrs-core/bdrs/user/atlas.htm?surveyId=1&taxonSearch=macropus%20rufus<br>
- or -<br>
http://<domain>:<port>/bdrs-core/bdrs/user/atlas.htm?surveyId=1&guid=urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537<br>
</pre>

Note that this form has some limitations.
  * The form will not display any survey, record or location attributes except exactly one data file attribute. All moderation attributes are displayed as normal.
  * This form does not support the hiding of darwin core attributes.

## Viewing all portals ##
To view all `Portals` in a BDRS instance, navigate to
<pre>
http://<domain>:<port>/bdrs-core/index.html<br>
</pre>