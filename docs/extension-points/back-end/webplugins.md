# Web Plugins

<div class="alert alert-warning">
    Since web-plugins are both front end and back end, it is difficult to know under which section to put their descriptions.  For now, the documentation exists as back end documentation even though there are <a href='../front-end/index.html'>Front end</a> components to most web plugins.
</div>

## Overview

OpenLumify is designed to be a single page application, but requests must be made back to the server to retrieve information, process data, and manage many other back-end services.  Since OpenLumify was designed to be extensible with many different types of plugins, web plugins fit naturally into OpenLumify's architecture.

Web plugins are deployed alongside OpenLumify inside of the web server and are used to both add custom functionality and override existing components.

## Development

Creating a web app plugin can range from creating some javascript that can execute inside of OpenLumify all the way to overriding the current functionality of an endpoint inside of OpenLumify.  That means that web app plugins will always have some sort of mix of front-end and back-end components that provide functionality.  To start working with a web plugin, you must implement the interface WebAppPlugin and register all of your helper files in there.  Since OpenLumify is built on top of Webster for its web layer, the documentation of [Webster](https://github.com/openlumify/webster) might be helpful to read to understand more about how it works.

## Deployment

To ensure that your web plugin is deployed, it needs to be loaded onto the classpath by adding it's fully qualified class name into the META-INF/services/org.openlumify.web.WebAppPlugin file in a jar that is on the classpath.

## Further Reading

[Making your first web plugin](../../tutorials/webplugin.md)
