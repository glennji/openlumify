# OpenLumify Features

Many featues within OpenLumify are developed and run as plugins to the system. There are two basic types of plugins, web plugins and graph property worker plugins. Please see the [extension point documentation](extension-points/index.md) if you'd like to learn how to build your own OpenLumify plugins.

## Web Plugins

The following web plugins come with OpenLumify, but not all are [installed](#viewing-active-plugins) by default.

| Feature | Description |
| ------- | -------------------|
| auth-username-only | authentication via username only; useful during development |
| auth-username-password | standard username password authentication |
| change-email | UI plugin allowing users to change their email address |
| change-password | UI plugin allowing users to change their password |
| admin-user-tools | UI plugin allowing users to add/modify/delete users |

## Graph Property Worker Plugins

The following features are executed as graph property workers that run within the OpenLumify web application.

| Feature | Description |
| ------- | ----------- |
| email-extractor | identification of e-mail address in text |
| phone-number-extractor | identification of phone numbers in text |
| tika-text-extractor | extract text from supported document filetypes using [Tika](http://tika.apache.org/) |
| zipcode-extractor | identification of postal codes in text (currently US only) |
| tika-mime-type | sets MIME type metadata property of "raw" properties (e.g. file content) using [Tika](http://tika.apache.org/) |
| mime-type-<span class="no-glossary">ontology</span>-mapper | sets the concept type property of vertices based on their MIME type |


## Viewing Active Plugins

You can view a list of all active plugins as well as registered UI extensions under the **Plugin** section of the Admin panel

