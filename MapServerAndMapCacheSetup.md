# Installation steps for Ubuntu (standard cgi script) #

1. Install MapServer using apt-get
```
sudo apt-get install cgi-mapserver mapserver-bin mapserver-doc php5-mapscript python-mapscript
```
The default install directory is **/usr/lib/cgi-bin**.
Verify installation
```
/usr/lib/cgi-bin/mapserv -v
```
Verify apache can run the cgi script
```
http://localhost/cgi-bin/mapserv
```

2. Put your MapServer config file into your cgi-lib directory. By default it is in /usr/lib/cgi-bin. The mapserv executable cgi script should also be in this directory.
3. Not always required but google projection is a very common projection that isn't part of the standard epsg file. Add the google projection to your epsg file in /usr/share/proj/epsg
```
# Google projection
<900913> +proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs <>
```

4. Your MapServer should now be able to serve up layers defined in your config file.

If you have any problems getting this working turn on debug in your map config file. http://mapserver.org/optimization/debugging.html#step-1-set-the-ms-errorfile-variable

# Installing MapCache for Ubuntu #
See http://mapserver.org/trunk/mapcache/install.html

find out which apache mpm you are running:
```
/usr/sbin/apache2 -V
```
> if threaded:
```
    sudo apt-get install apache2-threaded-dev
```
> otherwise:
```
    sudo apt-get install apache2-prefork-dev
```
install other dependencies from page:
```
sudo apt-get install libpng12-dev
sudo apt-get install libjpeg62-dev
sudo apt-get install libcurl4-gnutls-dev
```
recommended:
```
sudo apt-get install libpcre3-dev
sudo apt-get install libpixman-1-dev
sudo apt-get install libfcgi-dev
```
After installation is complete
```
sudo ldconfig
sudo service apache2 restart
```

add mapping to httpd.conf:
```
<IfModule mapcache_module>
   <Directory /path/to/directory>
      Order Allow,Deny
      Allow from all
   </Directory>
   MapCacheAlias /mapcache "/path/to/directory/mapcache.xml"
</IfModule>
```

Make sure that the directory that you have specified for mapcache to use as the actual cache has write permissions.

Restart apache
```
sudo apachectl restart
```

Add fast cgi
```
sudo apt-get install libapache2-mod-fcgid
edit httpd.conf with module info
```

If you have any problems getting mapcache working look at the apache error logs.