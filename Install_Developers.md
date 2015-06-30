# Introduction #

This document is a guide for developers to setup a development environment for the BDRS, run a development server, and deploy a BDRS instance to production.

This document will assume that you are using a debian based system such as Ubuntu however the BDRS can be run on other operating systems.

# Dependencies #

To setup a BDRS development environment you will need to install the following dependencies.

  * Subversion – A software versioning and revision control system. You will need this to check out the source code from our public repository.
  * PostgreSQL – An open source database.
  * PostGIS – Spatial extensions to the Postgres database.
  * Maven2 – Software dependency management and build tool.
  * Eclipse – Integrated Development Environment
On Ubuntu you can install all of these dependencies by running

`$ sudo apt-get install subversion postgresql-8.4 postgresql-8.4-postgis maven2 eclipse`

Under OpenSuSE add the following repository:
`$ http://download.opensuse.org/repositories/Application:/Geo/openSUSE_11.3/

and use Yast to install:

  * maven2
  * postgresql-server
  * postgresql-devel
  * postgis
  * postgis-utils

And on Mac OSX, make sure you have Xcode tools and the Java SDK installed, then install Homebrew and do:

  * brew install maven
  * brew install postgresql
  * brew install postgis

## Native Dependencies ##

In addition to the packages above, the BDRS also requires
  * Jepp - Embdeds CPython in Java

Assuming you are using Ubuntu, you can install Jepp by running:
```
# This indicates which version of Java to use when compiling Jepp
export JAVA_HOME=/usr/lib/jvm/default-java

# Required packages for compiling
sudo apt-get install python-dev libssl-dev

wget http://downloads.sourceforge.net/project/jepp/jep/2.4/jep-2.4.tar.gz
tar -zxvf jep-2.4.tar.gz
cd jep-2.4 

./configure && make 
sudo make install

# This will install Jep to your local maven repository
mvn install:install-file -Dfile=jep.jar -DgroupId=jep -DartifactId=jep -Dversion=2.4 -Dpackaging=jar
```

## Python Dependencies ##
If you wish to create reports, you will also need to install the required Python libraries. Pip handles
the installation of libraries for you by downloading them from the Python Package Index (PyPI)
`sudo apt-get install python-pip`

The precise libraries that you will need will depend on which reports you have installed however the following will be a good start.
```
pip install cheetah
pip install xlrd
pip install xlwt
pip install xlutils
pip install shapely
pip install psycopg2
pip install django==1.4
pip install https://github.com/simone/django-compositekey/tarball/1.4.x
```

## Taxon Lib ##
The taxon lib jar should download automatically from the gaiaresources maven repository. If you do not use any features from taxon lib this is all that you need to do. However if you run all of the unit tests (which is recommended) - you will need to do some additional setup.

  * Create a new database for taxonlib - must be postgresql.
  * Run taxonlib.sql from the taxonlib project to create relations in your new database.

Set the taxonlib test database properties in the dev profile (or another profile of your creation) in the pom file. The tests will fail due being unable to connect to the database.

An example of the settings and the format they are required in are below (Probably url is the only significant one, note there is no protocol specified):
```
username : postgres
password : postgres
url      : localhost:5432/taxonlib
```

If you want to use taxon lib functionality (currently limited to certain reports and taxonomy importing) via the BDRS front end you need to set your taxon lib database preference on the admin -> portal -> preferences page.

Note that you want to make your test database and your production database different as the test database is dropped after every test run!

The taxon lib external repository isn't set up yet so here is the taxonlib.sql file contents:
```sql

CREATE TABLE taxon_concept
(
taxon_concept_id serial NOT NULL,
-- is taxon_concept temporal ??
start_date timestamp,
end_date timestamp,
source_id text NOT NULL, -- aka holo_type id ?
source text,
author text,
year text,
citation text,
PRIMARY KEY(taxon_concept_id)
);

CREATE TABLE taxon_name
(
taxon_name_id serial not null,
taxon_rank text,
taxon_name_type text not null,
name text NOT NULL,
start_date timestamp,
end_date timestamp,
source_id text,
author text,
year text,
citation text,
taxon_concept_id integer not null,
display_name text,
primary key(taxon_name_id)
);
alter table taxon_name add constraint TAXON_NAME_TAXON_CONCEPT_FK foreign key (taxon_concept_id) references taxon_concept;
CREATE INDEX taxon_name_name_lower ON taxon_name ((lower(name)));
CREATE INDEX taxon_name_display_name_lower ON taxon_name ((lower(display_name)));

CREATE TABLE taxon_concept_relation
(
taxon_concept_relation_id serial NOT NULL,
taxon_concept_parent_id integer NOT NULL,
taxon_concept_child_id integer NOT NULL,
start_date timestamp,
end_date timestamp,
author text,
year text,
citation text
);
alter table taxon_concept_relation add constraint TAXON_CONCEPT_PARENT_FK foreign key (taxon_concept_parent_id) references taxon_concept;
alter table taxon_concept_relation add constraint TAXON_CONCEPT_CHILD_FK foreign key (taxon_concept_child_id) references taxon_concept;

CREATE TABLE taxon_concept_junction
(
taxon_concept_junction_id serial not null,
taxon_concept_old_id integer not null,
taxon_concept_new_id integer not null,
change_type text,
change_date timestamp,
author text,
year text,
citation text
);
alter table taxon_concept_junction add constraint TAXON_CONCEPT_JUNCTION_OLD_TAXON_CONCEPT_FK foreign key (taxon_concept_old_id) references taxon_concept;
alter table taxon_concept_junction add constraint TAXON_CONCEPT_JUNCTION_NEW_TAXON_CONCEPT_FK foreign key (taxon_concept_new_id) references taxon_concept;

create index taxon_concept_source_id_index on taxon_concept(source, source_id);
```

# Source Code #

Next we will check out the BDRS source code from the public repository.

`$ svn checkout http://ala-citizenscience.googlecode.com/svn/trunk/ ala-citizenscience-read-only`

After checking out the source code, you will need to create the database and modify the "pom.xml" file with the newly created database settings as specified in the next section.

# Setting up the Database #

First lets create a spatial database template. You will need to execute the commands as a privileged database user. For example you can use the following to become the postgres user.

`$ sudo su – postgres`

Once you are the database super user you may then execute the following commands to create the spatial database template.

`$ createdb template_postgis`

`$ createlang plpgsql template_postgis`

`$ psql -d template_postgis -f /usr/share/postgresql/8.4/contrib/postgis-1.5/postgis.sql`

`$ psql -d template_postgis -f /usr/share/postgresql/8.4/contrib/postgis-1.5/spatial_ref_sys.sql`

Now we will create our project database.

`$ createdb ala-citizenscience-read-only -T template_postgis`

Note:
You can stop using the postgres user at this point and revert back to your normal user account.

At this stage, this is just an empty spatial database. We will need to create tables for the BDRS. Execute the following to generate the SQL creation scripts.

`$ mvn -Pdev -DskipTests clean package hibernate3:hbm2ddl`

Note:
The command above skips the unit tests because we haven't created a test database yet.
Compiling for the first time may take a while as maven downloads all the BDRS dependencies.

In addition to printing the SQL create commands to the console, they are also stored in “ala-citizenscience-read-only/target/hibernate3/sql/bdrs-core.sql”.

Run this script against the spatial database that we created earlier.

`psql -U postgres ala-citizenscience-read-only -f target/hibernate3/sql/bdrs-core.sql`

Now we need to modify the “pom.xml” file located at the base directory with these database settings. Open the file using a text editor and locate the profiles section at the bottom of the file.
At a minimum, you will need to update the

`bdrs.db.driver`

`bdrs.db.url`

`bdrs.db.user.name`

`bdrs.db.user.password`

with the credentials to your postgres database. For example,

`<bdrs.db.driver>org.postgresql.Driver</bdrs.db.driver>`

`<bdrs.db.url>jdbc:postgresql://localhost:5432/ala-citizenscience-read-only</bdrs.db.url>`

`<bdrs.db.user.name>postgres</bdrs.db.user.name>`

`<bdrs.db.user.password>postgres</bdrs.db.user.password>`

In this case, we login in with the username/password postgres/postgres and connect to the database called “ala-citizenscience-read-only”.

We now have a working BDRS database.  Next we will run the unit tests.

# Running the BDRS Unit Tests #

Note:

---

Currently there are some deficiencies with the build setup and this will cause problems when running the unit tests. To work around these problems do the following:
  1. Open the climatewatch-email.xml file and set the host and ports of the email server else you will not be able to run the tests.
  1. Open the climatewatch-hibernate-datasource-test.xml file and set the database details for your test database appropriately. DON'T set your test database to be the same as any production BDRS database used to run a web server because ALL OF YOUR DATA WILL BE ERASED.
  1. Open the file.properties file and set the file store directory for the BDRS. For the unit tests it is ok to use the /tmp directory.

**For all of these changes note that to use the pom settings when creating your WAR file, you must revert your changes else maven will not be able to do its usual filtering.**

---


To run the unit tests, we will need to create a test database. The unit tests are run against this database instead of your development database so that the test environment remains constant for all tests. The BDRS tables in the test database must be completely empty when starting the tests else some tests will fail and further build steps will not be possible.

To create a test database, perform the same procedure as creating the BDRS database except in this case, give the test database a different name e.g. “ala-citizenscience-read-only-test”.

Open “src/main/webapp/WEB-INF/climatewatch-hibernate-datasource-test.xml” in a text editor and update the file with the appropriate settings for your test database.

To run the unit tests execute the following command:

`$ mvn -Pdev test`

Test results can be found in “target/surefire-reports”.

# Running a web server for Development #

Note:

---

Currently there are some deficiencies with the build setup and this will cause problems when running on Jetty. To work around these problems do the following:

  1. Open the climatewatch-email.xml file and set the host and ports of the email server else you will not be able to run the tests.
  1. Open the climatewatch-hibernate-datasource.xml file and set the  database details for your database appropriately.
  1. Open the file.properties file and set the file store directory for the BDRS. For a webserver you do not want to use the /tmp directory as by default your files may be erased when the computer resets.
  1. For email functionality to work (e.g. the user registration signup notifications), you must edit the email.properties file.

**For all of these changes note that to use the pom settings when creating your WAR file, you must revert your changes else maven will not be able to do its usual filtering.**

---


During development you can use a light weight open source web server called Jetty. Jetty has already been mostly configured for you in the POM so that it does not require the Web Application Archive (WAR) file to be packaged. Instead Jetty directly executes the application from the class files in the target directory and the template files in the source directory.

The price that you pay for having this convenience is that maven filtering does not get executed (because we are not building a WAR file) and so we will need to make a manual change to a file that is normally taken care of by maven.

Open “src/main/webapp/WEB-INF/climatewatch-hibernate-datasource.xml” in a text editor.
Locate the following keys and replace them with their equivalents in your POM file.

`${ bdrs.db.driver }`

`${ bdrs.db.url }`

`${ bdrs.db.user.name }`

`${ bdrs.db.user.password }`

It should look something like this when you have made the substitutions.

```
<property name="driverClassName" value="org.postgresql.Driver"/>
<property name="url" value="jdbc:postgresql://localhost:5432/ala-citizenscience-read-only"/>
<property name="username" value="postgres"/>
<property name="password" value="postgres"/>
```

You will also need to tell Jepp where to find the native libraries that it requires.
```
# You will need to update this for your specific system.
# If you are unsure which path to use, please refer to http://jepp.sourceforge.net/usage.html

export LD_LIBRARY_PATH=/usr/local/lib/
export LD_PRELOAD=/usr/lib/libpython2.7.so.1.0
export CLASSPATH=~/.m2/repository/jep/jep/2.4/jep-2.4.jar:$CLASSPATH
```

You can now start Jetty using the command

`$ mvn -Pdev compile jetty:run`

Navigate to http://localhost:8080/BDRS/home.htm to see if you have been successful.

# Creating a Web Application Archive (WAR) File #

Before you can deploy the BDRS to a web server such as Tomcat, we will need to package the application into a WAR file by running the command:

`$ mvn -Pstage clean package`

Note:
In this example, we are packaging the BDRS using the 'stage' profile instead of the 'dev' profile. The staging profile should contain the appropriate settings for your staging environment which may or may not be different to your development environment.
Packaging the BDRS will automatically run the unit tests. If you wish to skip running the unit tests (not recommended) you can do so by adding -DskipTests to the command line parameters.

The generate WAR file can be found in target/bdrs-core.war

## Deploying to Tomcat ##
For deployment, we need to configure Tomcat so that it can correctly locate native libraries required to create reports.

  * Copy the Jepp jar file (jep-2.4.jar) to Tomcat's shared library directory.
  * You can find jep-2.4.jar in the Maven Repository on your build machine
> `~/.m2/repository/jep/jep/2.4`
  * On Ubuntu the Tomcat shared library is
> `/usr/share/tomcat6/lib`
  * Compile and install jep.
  * Configure environment variables for Tomcat
  * Find the tomcat startup script `startup.sh`. On Ubuntu this is in
> `/usr/share/tomcat6/bin`.
  * Create a new file in that directory called `setenv.sh`. Place the following in the file.
_**Don't forget to update the content for that specific machine**_
```
 #!/bin/bash
 export LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH
 export LD_PRELOAD=/usr/lib/libpython2.7.so.1.0:$LD_PRELOAD
```

  * Finally give the file the correct file permissions
`sudo chmod 655 ./setenv.sh`

### Setting the welcome file ###
By default when a user navigates to the root of the web application, namely,

<pre>http://<domain>:<port>/bdrs-core/</pre>

they will be  redirected to a page that lists all `Portal`s running on the BDRS instance.

To alter this default behaviour, edit
```
src/main/index.jsp
```
and modify the line that reads,
```
<meta HTTP-EQUIV="REFRESH" content="0; url=index.html"> 
```

You will need to modify the `index.html` to point at the location in the BDRS to redirect to.

### Tomcat Session Persistence ###
If you are experiencing an issue where if you are logged into the BDRS and restart the Tomcat server, the next browser refresh may cause a redirect loop.

This is caused by Tomcat persisting the session when it stops and restoring the session when it starts again. Unfortunately when Tomcat does this, Spring is not aware of this session and will throw an exception.

The workaround for this is to disable the [Tomcat Session Persistence](http://tomcat.apache.org/tomcat-6.0-doc/config/manager.html#Disable_Session_Persistence). Simply edit
<pre>/etc/tomcat6/context.xml</pre>
and uncomment
```
<Manager pathname=""/> 
```