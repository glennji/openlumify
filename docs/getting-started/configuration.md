
# Configuration

There are many strategies for locating OpenLumify configuration properties. By default, OpenLumify will use `org.openlumify.core.config.FileConfigurationLoader` to load configuration files and `org.openlumify.core.bootstrap.lib.LibDirectoryLoader` to load additional `.jar` files.

The following directories will be searched in order:

* `/opt/openlumify/` for Linux/OS X
* `c:/opt/openlumify/` for Windows
* `${appdata}/OpenLumify`
* `${user.home}/.openlumify`
* a directory specified with the `VISALLO_DIR` environment variable

All files in `/config` subdirectories with `.properties` extensions will then be loaded alphabetically allowing you to override properties in various places.

All `.jar` files in `/lib` subdirectories will be added to the classpath.

**For the purposes of this guide, `$VISALLO_DIR` will refer to the parent directory of subdirectories where your config and lib files are stored, regardless of which of the above options you choose to use.**
